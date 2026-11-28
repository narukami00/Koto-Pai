"""Helpers for building the Koto Pai presentation with python-pptx.

Design rules (from the brief): the application's own colours, light background and dark text, no rounded
corners, no cards or tables, no decorative lines. Structure comes from type, colour and white space; the only
lines are the arrows inside diagrams. Animations and the slide transition are written as raw OOXML because
python-pptx does not expose them.
"""
import itertools
import re

from lxml import etree
from PIL import Image
from pptx import Presentation
from pptx.dml.color import RGBColor
from pptx.enum.shapes import MSO_CONNECTOR, PP_PLACEHOLDER
from pptx.enum.text import MSO_ANCHOR, MSO_AUTO_SIZE, PP_ALIGN
from pptx.opc.constants import RELATIONSHIP_TYPE as RT
from pptx.oxml.ns import qn
from pptx.util import Emu, Inches, Pt

# ---------------------------------------------------------------- palette (from static/css/kotopai.css)
INK = '14291F'
TEAL = '18604B'
TEAL_D = '0F4536'
GREEN = '2F7A2A'
ORANGE = 'DB8428'
ORANGE_INK = 'A85F12'
MUTED = '667267'
BG = 'FAF8EE'
LINE = 'ECE8CF'
CREAM = 'EDE7B4'

FONT = 'Segoe UI'
MONO = 'Consolas'
DATE_TEXT = 'December 2026'

SW, SH = 13.333, 7.5
LM, RM = 0.9, 12.43
CW = RM - LM

NS_P = 'http://schemas.openxmlformats.org/presentationml/2006/main'
NS_A = 'http://schemas.openxmlformats.org/drawingml/2006/main'

BR = object()  # line break marker inside a paragraph


def rgb(hexstr):
    return RGBColor.from_string(hexstr)


# ---------------------------------------------------------------- theme and layout
def _theme_xml():
    colors = dict(dk1=INK, lt1=BG, dk2=TEAL_D, lt2=CREAM, accent1=TEAL, accent2=ORANGE, accent3='4C9A41',
                  accent4=ORANGE_INK, accent5=MUTED, accent6=LINE, hlink=TEAL, folHlink=MUTED)
    clr = ''.join('<a:%s><a:srgbClr val="%s"/></a:%s>' % (k, v, k) for k, v in colors.items())
    scheme = '<a:clrScheme name="Koto Pai">%s</a:clrScheme>' % clr
    fonts = ('<a:fontScheme name="Koto Pai"><a:majorFont><a:latin typeface="%s"/><a:ea typeface=""/><a:cs typeface=""/>'
             '</a:majorFont><a:minorFont><a:latin typeface="%s"/><a:ea typeface=""/><a:cs typeface=""/></a:minorFont>'
             '</a:fontScheme>') % (FONT, FONT)
    return scheme, fonts


def _lst_style(size, color, bold=False, align='l'):
    return ('<a:lstStyle xmlns:a="%s"><a:lvl1pPr algn="%s"><a:defRPr sz="%d" b="%d"><a:solidFill><a:srgbClr val="%s"/>'
            '</a:solidFill></a:defRPr></a:lvl1pPr></a:lstStyle>') % (NS_A, align, int(size * 100), 1 if bold else 0, color)


def _replace_lst_style(ph, xml):
    txbody = ph._element.txBody
    old = txbody.find(qn('a:lstStyle'))
    new = etree.fromstring(xml)
    if old is not None:
        txbody.replace(old, new)
    else:
        txbody.insert(1, new)


def _place(shape, x, y, w, h):
    shape.left, shape.top, shape.width, shape.height = Inches(x), Inches(y), Inches(w), Inches(h)


def prepare(prs):
    """16:9 canvas, Koto Pai theme, and a single clean 'Title Only' layout."""
    prs.slide_width, prs.slide_height = Emu(12192000), Emu(6858000)
    master = prs.slide_master

    theme_part = master.part.part_related_by(RT.THEME)
    xml = theme_part.blob.decode('utf-8')
    scheme, fonts = _theme_xml()
    xml = re.sub(r'<a:clrScheme .*?</a:clrScheme>', scheme, xml, flags=re.S)
    xml = re.sub(r'<a:fontScheme .*?</a:fontScheme>', fonts, xml, flags=re.S)
    theme_part._blob = xml.encode('utf-8')

    keep = prs.slide_layouts.get_by_name('Title Only')
    for layout in list(prs.slide_layouts):
        if layout is not keep:
            prs.slide_layouts.remove(layout)

    boxes = {
        PP_PLACEHOLDER.TITLE: (LM, 0.82, CW, 0.9),
        PP_PLACEHOLDER.BODY: (LM, 1.95, CW, 4.7),
        PP_PLACEHOLDER.DATE: (LM, 6.92, 4.0, 0.3),
        PP_PLACEHOLDER.FOOTER: (5.0, 6.92, 3.3, 0.3),
        PP_PLACEHOLDER.SLIDE_NUMBER: (RM - 2.0, 6.92, 2.0, 0.3),
    }
    for ph in master.placeholders:
        t = ph.placeholder_format.type
        if t in boxes:
            _place(ph, *boxes[t])

    for ph in list(keep.placeholders):
        t = ph.placeholder_format.type
        if t == PP_PLACEHOLDER.FOOTER:
            ph._element.getparent().remove(ph._element)
        elif t == PP_PLACEHOLDER.TITLE:
            _place(ph, *boxes[t])
            _replace_lst_style(ph, _lst_style(34, TEAL, bold=True))
            tf = ph.text_frame
            tf.margin_left = tf.margin_right = tf.margin_top = tf.margin_bottom = 0
            tf.vertical_anchor = MSO_ANCHOR.TOP
        elif t == PP_PLACEHOLDER.DATE:
            _place(ph, *boxes[t])
            _replace_lst_style(ph, _lst_style(11, MUTED, align='l'))
            tf = ph.text_frame
            tf.margin_left = tf.margin_right = tf.margin_top = tf.margin_bottom = 0
        elif t == PP_PLACEHOLDER.SLIDE_NUMBER:
            _place(ph, *boxes[t])
            _replace_lst_style(ph, _lst_style(11, MUTED, align='r'))
            tf = ph.text_frame
            tf.margin_left = tf.margin_right = tf.margin_top = tf.margin_bottom = 0
    return keep


# ---------------------------------------------------------------- slide scaffolding
def _add_footer(slide, layout):
    for ph in layout.placeholders:
        t = ph.placeholder_format.type
        idx = ph.placeholder_format.idx
        sid = slide.shapes._next_shape_id
        if t == PP_PLACEHOLDER.DATE:
            xml = ('<p:sp xmlns:p="%s" xmlns:a="%s"><p:nvSpPr><p:cNvPr id="%d" name="Date"/><p:cNvSpPr>'
                   '<a:spLocks noGrp="1"/></p:cNvSpPr><p:nvPr><p:ph type="dt" sz="half" idx="%d"/></p:nvPr></p:nvSpPr>'
                   '<p:spPr/><p:txBody><a:bodyPr/><a:lstStyle/><a:p><a:r><a:rPr lang="en-US"/><a:t>%s</a:t></a:r></a:p>'
                   '</p:txBody></p:sp>') % (NS_P, NS_A, sid, idx, DATE_TEXT)
        elif t == PP_PLACEHOLDER.SLIDE_NUMBER:
            xml = ('<p:sp xmlns:p="%s" xmlns:a="%s"><p:nvSpPr><p:cNvPr id="%d" name="Slide number"/><p:cNvSpPr>'
                   '<a:spLocks noGrp="1"/></p:cNvSpPr><p:nvPr><p:ph type="sldNum" sz="quarter" idx="%d"/></p:nvPr>'
                   '</p:nvSpPr><p:spPr/><p:txBody><a:bodyPr/><a:lstStyle/><a:p>'
                   '<a:fld id="{B6F15528-21DE-4FAA-801E-634DDDAF4B2B}" type="slidenum"><a:rPr lang="en-US"/>'
                   '<a:t>‹#›</a:t></a:fld><a:endParaRPr lang="en-US"/></a:p></p:txBody></p:sp>'
                   ) % (NS_P, NS_A, sid, idx)
        else:
            continue
        slide.shapes._spTree.append(etree.fromstring(xml))


def _add_transition(slide):
    xml = '<p:transition xmlns:p="%s" spd="med"><p:fade/></p:transition>' % NS_P
    slide._element.append(etree.fromstring(xml))


class Deck:
    def __init__(self):
        self.prs = Presentation()
        self.layout = prepare(self.prs)
        self.slides = []

    def slide(self, title, kicker=None, notes=None, footer=True):
        s = self.prs.slides.add_slide(self.layout)
        s.shapes.title.text = title
        if kicker:
            tb(s, LM, 0.5, 8, 0.3, [P((kicker.upper(), dict(spc=2.0)))], size=12, color=ORANGE_INK, bold=True,
               name='Section')
        if footer:
            _add_footer(s, self.layout)
        if notes:
            s.notes_slide.notes_text_frame.text = notes
        a = Anim(s)
        self.slides.append(a)
        return s, a

    def save(self, path):
        for a in self.slides:
            _add_transition(a.slide)  # schema order: transition comes before timing
            a.apply()
        self.prs.save(path)


# ---------------------------------------------------------------- text
def P(*runs, **opts):
    opts['runs'] = list(runs)
    return opts


def b(text):  # bold teal
    return (text, dict(bold=True, color=TEAL))


def m(text):  # muted
    return (text, dict(color=MUTED))


def o(text):  # orange ink, bold
    return (text, dict(bold=True, color=ORANGE_INK))


def mono(text, **kw):
    return (text, dict(font=MONO, **kw))


def _style_run(r, size, bold, italic, color, font, spc):
    f = r.font
    if size:
        f.size = Pt(size)
    if bold is not None:
        f.bold = bold
    if italic is not None:
        f.italic = italic
    if color:
        f.color.rgb = rgb(color)
    if font:
        f.name = font
    if spc is not None:
        r._r.get_or_add_rPr().set('spc', str(int(spc * 100)))


def _bullet(p, color, indent):
    pPr = p._p.get_or_add_pPr()
    pPr.set('marL', str(int(Inches(indent))))
    pPr.set('indent', str(-int(Inches(indent))))
    clr = etree.SubElement(pPr, qn('a:buClr'))
    etree.SubElement(clr, qn('a:srgbClr')).set('val', color)
    etree.SubElement(pPr, qn('a:buFont')).set('typeface', 'Arial')
    etree.SubElement(pPr, qn('a:buChar')).set('char', '•')


def _tabs(p, stops):
    pPr = p._p.get_or_add_pPr()
    lst = etree.SubElement(pPr, qn('a:tabLst'))
    for pos, algn in stops:
        t = etree.SubElement(lst, qn('a:tab'))
        t.set('pos', str(int(Inches(pos))))
        t.set('algn', algn)


_ALIGN = {'l': PP_ALIGN.LEFT, 'c': PP_ALIGN.CENTER, 'r': PP_ALIGN.RIGHT}
_ANCHOR = {'t': MSO_ANCHOR.TOP, 'm': MSO_ANCHOR.MIDDLE, 'b': MSO_ANCHOR.BOTTOM}


def tb(slide, x, y, w, h, paras, size=18, color=INK, bold=None, align='l', anchor='t', line=None, after=0,
       before=0, font=None, name=None, bullet=False, bullet_color=TEAL, indent=0.3, tabs=None, cols=None,
       wrap=True):
    shp = slide.shapes.add_textbox(Inches(x), Inches(y), Inches(w), Inches(h))
    if name:
        shp.name = name
    tf = shp.text_frame
    tf.word_wrap = wrap
    tf.auto_size = MSO_AUTO_SIZE.NONE
    tf.margin_left = tf.margin_right = tf.margin_top = tf.margin_bottom = 0
    tf.vertical_anchor = _ANCHOR[anchor]
    if cols:
        bp = tf._txBody.find(qn('a:bodyPr'))
        bp.set('numCol', str(cols))
        bp.set('spcCol', str(int(Inches(0.4))))
    base = dict(size=size, color=color, bold=bold, align=align, line=line, after=after, before=before, font=font,
                bullet=bullet, tabs=tabs, spc=None, italic=None)
    first = True
    for spec in paras:
        if isinstance(spec, str):
            spec = P(spec)
        o_ = {**base, **{k: v for k, v in spec.items() if k != 'runs'}}
        p = tf.paragraphs[0] if first else tf.add_paragraph()
        first = False
        p.alignment = _ALIGN[o_['align']]
        p.space_after = Pt(o_['after'])
        p.space_before = Pt(o_['before'])
        if o_['line']:
            p.line_spacing = o_['line']
        for r_ in spec['runs']:
            if r_ is BR:
                p.add_line_break()
                br = p._p.findall(qn('a:br'))[-1]
                etree.SubElement(br, qn('a:rPr')).set('sz', str(int(o_['size'] * 100)))
                continue
            text, ro = (r_, {}) if isinstance(r_, str) else r_
            ro = {**dict(size=o_['size'], bold=o_['bold'], italic=o_['italic'], color=o_['color'], font=o_['font'],
                         spc=o_['spc']), **ro}
            run = p.add_run()
            run.text = text
            _style_run(run, ro['size'], ro['bold'], ro['italic'], ro['color'], ro['font'], ro['spc'])
            if ro.get('link'):
                run.hyperlink.address = ro['link']
        if o_['bullet']:
            _bullet(p, bullet_color, indent)
        if o_['tabs']:
            _tabs(p, o_['tabs'])
    return shp


def bullets(slide, x, y, w, h, items, size=20, after=14, color=INK, name='Bullets', **kw):
    return tb(slide, x, y, w, h, [it if isinstance(it, dict) else P(*(it if isinstance(it, list) else [it]))
                                  for it in items],
              size=size, color=color, after=after, bullet=True, name=name, line=1.0, **kw)


def code(slide, x, y, w, h, lines, size=14, name='Code'):
    paras = []
    for ln in lines:
        if isinstance(ln, tuple):  # (text, color)
            paras.append(P((ln[0], dict(color=ln[1]))))
        else:
            paras.append(P(ln if ln else ' '))
    return tb(slide, x, y, w, h, paras, size=size, font=MONO, color=INK, name=name, wrap=False)


# ---------------------------------------------------------------- diagram pieces
def arrow(slide, x1, y1, x2, y2, color=MUTED, width=1.25, dashed=False, head=True, name='Arrow'):
    c = slide.shapes.add_connector(MSO_CONNECTOR.STRAIGHT, Inches(x1), Inches(y1), Inches(x2), Inches(y2))
    c.name = name
    c.line.color.rgb = rgb(color)
    c.line.width = Pt(width)
    ln = c.line._get_or_add_ln()
    if dashed:
        etree.SubElement(ln, qn('a:prstDash')).set('val', 'dash')
    if head:
        te = etree.SubElement(ln, qn('a:tailEnd'))
        te.set('type', 'triangle')
        te.set('w', 'med')
        te.set('len', 'med')
    return c


def pic(slide, path, x, y, w, h, crop=(0, 0, 0, 0), alt='', align='l', name=None):
    """Place a picture inside the (x, y, w, h) box, keeping its aspect ratio.

    crop = (left, top, right, bottom) as fractions of the source image, for trimming scrollbars and side panels.
    """
    im = Image.open(path)
    iw, ih = im.size
    cl, ct, cr, cb = crop
    ratio = (iw * (1 - cl - cr)) / (ih * (1 - ct - cb))
    if w / h > ratio:
        ph, pw = h, h * ratio
    else:
        pw, ph = w, w / ratio
    px = x if align == 'l' else (x + (w - pw) / 2 if align == 'c' else x + w - pw)
    p = slide.shapes.add_picture(path, Inches(px), Inches(y), Inches(pw), Inches(ph))
    p.crop_left, p.crop_top, p.crop_right, p.crop_bottom = cl, ct, cr, cb
    if name:
        p.name = name
    p._element.nvPicPr.cNvPr.set('descr', alt)
    return p, (px, y, pw, ph)


# ---------------------------------------------------------------- animation
def IN(shape):
    return ('in', shape)


def OUT(shape):
    return ('out', shape)


def PAR(shape, i):
    return ('par', shape, i)


class Anim:
    """Click-by-click build. click(*items): the first item starts on the click, the rest start with it."""

    DUR = 450

    def __init__(self, slide):
        self.slide = slide
        self.clicks = []

    def click(self, *items):
        self.clicks.append(items)
        return self

    def apply(self):
        if not self.clicks:
            return
        ids = itertools.count(3)
        bld = {}
        parts = []
        for group in self.clicks:
            outer, inner = next(ids), next(ids)
            effects = []
            for k, item in enumerate(group):
                kind, shp = item[0], item[1]
                spid = shp.shape_id
                is_sp = shp._element.tag == qn('p:sp')
                if kind == 'par':
                    tgt = ('<p:spTgt spid="%d"><p:txEl><p:pRg st="%d" end="%d"/></p:txEl></p:spTgt>'
                           % (spid, item[2], item[2]))
                    bld[spid] = 'p'
                else:
                    tgt = '<p:spTgt spid="%d"/>' % spid
                    if is_sp:
                        bld.setdefault(spid, 'whole')
                grp = ' grpId="0"' if is_sp else ''
                node = 'clickEffect' if k == 0 else 'withEffect'
                exit_ = kind == 'out'
                e, s_, a_ = next(ids), next(ids), next(ids)
                effects.append(
                    '<p:par><p:cTn id="%d" presetID="10" presetClass="%s" presetSubtype="0" fill="hold"%s nodeType="%s">'
                    '<p:stCondLst><p:cond delay="0"/></p:stCondLst><p:childTnLst>'
                    '<p:set><p:cBhvr><p:cTn id="%d" dur="1" fill="hold"><p:stCondLst><p:cond delay="%d"/></p:stCondLst>'
                    '</p:cTn><p:tgtEl>%s</p:tgtEl><p:attrNameLst><p:attrName>style.visibility</p:attrName>'
                    '</p:attrNameLst></p:cBhvr><p:to><p:strVal val="%s"/></p:to></p:set>'
                    '<p:animEffect transition="%s" filter="fade"><p:cBhvr><p:cTn id="%d" dur="%d"/>'
                    '<p:tgtEl>%s</p:tgtEl></p:cBhvr></p:animEffect></p:childTnLst></p:cTn></p:par>'
                    % (e, 'exit' if exit_ else 'entr', grp, node, s_, self.DUR - 1 if exit_ else 0, tgt,
                       'hidden' if exit_ else 'visible', 'out' if exit_ else 'in', a_, self.DUR, tgt))
            parts.append(
                '<p:par><p:cTn id="%d" fill="hold"><p:stCondLst><p:cond delay="indefinite"/></p:stCondLst>'
                '<p:childTnLst><p:par><p:cTn id="%d" fill="hold"><p:stCondLst><p:cond delay="0"/></p:stCondLst>'
                '<p:childTnLst>%s</p:childTnLst></p:cTn></p:par></p:childTnLst></p:cTn></p:par>'
                % (outer, inner, ''.join(effects)))
        bld_xml = ''
        if bld:
            bld_xml = '<p:bldLst>%s</p:bldLst>' % ''.join(
                '<p:bldP spid="%d" grpId="0"%s/>' % (sp, ' build="p"' if kind == 'p' else '')
                for sp, kind in bld.items())
        xml = ('<p:timing xmlns:p="%s"><p:tnLst><p:par><p:cTn id="1" dur="indefinite" restart="never" nodeType="tmRoot">'
               '<p:childTnLst><p:seq concurrent="1" nextAc="seek"><p:cTn id="2" dur="indefinite" nodeType="mainSeq">'
               '<p:childTnLst>%s</p:childTnLst></p:cTn><p:prevCondLst><p:cond evt="onPrev" delay="0"><p:tgtEl><p:sldTgt/>'
               '</p:tgtEl></p:cond></p:prevCondLst><p:nextCondLst><p:cond evt="onNext" delay="0"><p:tgtEl><p:sldTgt/>'
               '</p:tgtEl></p:cond></p:nextCondLst></p:seq></p:childTnLst></p:cTn></p:par></p:tnLst>%s</p:timing>'
               % (NS_P, ''.join(parts), bld_xml))
        self.slide._element.append(etree.fromstring(xml))
