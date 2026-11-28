"""Builds presentation/KotoPai_Presentation.pptx (run: python presentation/build_deck.py)."""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from deck_lib import *  # noqa: E402,F401,F403

HERE = os.path.dirname(os.path.abspath(__file__))
SHOTS = os.path.join(os.path.dirname(HERE), 'assets', 'screenshots')
OUT_FILE = os.path.join(HERE, 'KotoPai_Presentation.pptx')


def shot(name):
    return os.path.join(SHOTS, name)


def label(slide, x, yc, w, title, sub=None, size=16, color=TEAL, h=0.9, name='Label'):
    paras = [P(title, size=size, bold=True, color=color)]
    if sub:
        paras.append(P(sub, size=12, color=MUTED))
    return tb(slide, x, yc - h / 2, w, h, paras, align='c', anchor='m', name=name)


def heading(slide, x, y, w, text, name='Heading'):
    return tb(slide, x, y, w, 0.3, [P((text.upper(), dict(spc=1.5)))], size=12, bold=True, color=MUTED, name=name)


d = Deck()

# ======================================================================= 1 cover
s, a = d.slide('Koto Pai?', footer=False, notes=(
    'Koto Pai? is Bengali for "how much do I get?". It is a bill-splitting web application built by three people '
    'for the Software Engineering and Project Management Laboratory. This talk is about how it was designed, '
    'tested, shipped and run, more than about what it looks like.'))
t = s.shapes.title
t.left, t.top, t.width, t.height = Inches(LM), Inches(2.3), Inches(11), Inches(1.5)
t.text_frame.paragraphs[0].runs[0].font.size = Pt(88)
tb(s, LM, 0.95, 8, 0.4, [P(('CSE 3220', dict(spc=3.0)))], size=20, bold=True, color=ORANGE_INK, name='Course code')
tb(s, LM, 1.4, 11, 0.5, ['Software Engineering and Project Management Laboratory'], size=24, name='Course title')
tb(s, LM, 1.92, 8, 0.4, ['Final Project'], size=18, color=MUTED, name='Project type')
tb(s, LM, 4.35, 6, 0.3, [P(('SUBMITTED BY', dict(spc=2.0)))], size=12, bold=True, color=MUTED, name='Submitted by')
tb(s, LM, 4.75, 8, 1.6,
   [P(('Rafsan Riasat', dict(bold=True)), '\t', m('2207006')),
    P(('Adib Raian', dict(bold=True)), '\t', m('2207020')),
    P(('Utsa Roy', dict(bold=True)), '\t', m('2207027'))],
   size=24, after=8, tabs=[(3.6, 'l')], name='Team')
tb(s, LM, 6.65, 6, 0.4, [DATE_TEXT], size=16, color=MUTED, name='Date')

# ======================================================================= 2 problem and idea
s, a = d.slide('Who paid what, and who owes whom', 'Overview', notes=(
    'Friends who share meals remember who paid but not who ate what. Doing it by hand loses small amounts: dividing '
    '100.00 three ways and rounding gives 99.99. The whole design is driven by one example, burger night, which '
    'is also a golden test in the engine: if a single poisha moves, the build fails.'))
bl = bullets(s, LM, 1.95, 6.1, 4.4, [
    'Friends remember who paid, not who ate what',
    'Settling by memory ends in arguments',
    'A calculator loses money: 100.00 split three ways and rounded gives 99.99',
    [b('Koto Pai?'), ' is Bengali for \u201chow much do I get?\u201d']], size=20, after=18)
hd = heading(s, 7.5, 1.95, 4.93, 'Burger night \u00b7 bill Tk 1900', 'Example heading')
rows, y = [], 2.5
for nm, sub, val, col in [('Rafsan', 'consumed 700 \u00b7 paid 1400', '+700', GREEN),
                          ('Tanvir', 'consumed 800 \u00b7 paid 500', '\u2212300', ORANGE_INK),
                          ('Nabila', 'consumed 400 \u00b7 paid 0', '\u2212400', ORANGE_INK)]:
    n = tb(s, 7.5, y, 2.8, 0.9, [P(nm, size=22, bold=True), P(sub, size=13, color=MUTED)], name=nm + ' row')
    v = tb(s, 10.2, y - 0.08, 2.23, 0.9, [P(val, align='r')], size=44, bold=True, color=col, name=nm + ' net')
    rows += [n, v]
    y += 1.0
debts = tb(s, 7.5, 5.55, 4.93, 0.8,
           [P('Two debts follow: Tanvir owes Rafsan Tk 300, Nabila owes Rafsan Tk 400.', bold=True, color=TEAL)],
           size=17, name='Debts')
gold = tb(s, 7.5, 6.3, 4.93, 0.3, ['A golden test: shift one poisha and the build fails.'], size=13, color=MUTED,
          name='Golden test')
for i in range(4):
    a.click(PAR(bl, i))
a.click(IN(hd), *[IN(r) for r in rows])
a.click(IN(debts), IN(gold))

# ======================================================================= 3 what it does
s, a = d.slide('What the application does', 'Overview', notes=(
    'A quick tour with real screenshots. Accounts and friends, then the outing wizard with a live preview that runs '
    'the same engine as submit, then settling up with payments, disputes and delegation, then the activity timeline '
    'and the admin console.'))
bl = bullets(s, LM, 1.95, 4.7, 4.6, [
    'Friends, accounts and a dashboard',
    'Outings with a live split preview',
    'Debts, payments, disputes, delegation',
    'Notifications and an activity timeline',
    'An admin console with user search'], size=20, after=18)
tour = [('dashboard.png', 'Dashboard: net balance, balances per friend, active outings'),
        ('wizard-live-preview.png', 'Wizard: the preview confirms \u201cEverything adds up\u201d'),
        ('settle-up.png', 'Settle up: payments, disputes and delegation'),
        ('activity.png', 'Activity timeline with filters'),
        ('admin.png', 'Admin console: platform statistics and user search')]
SCROLLBAR = 15 / 1440  # two screenshots were taken with a visible scrollbar; crop it out
# Added last-to-first so the first screenshot sits on top: a PDF export or thumbnail shows one clean picture,
# while the slide show swaps them one per click.
shots_ = {}
for f, cap in reversed(tour):
    crop = (0, 0, SCROLLBAR, 0) if f == 'wizard-live-preview.png' else (0, 0, 0, 0)
    shots_[f], _ = pic(s, shot(f), 5.9, 1.95, 6.53, 3.72, crop=crop, alt=cap, name='Screenshot ' + f)
prev = None
for i, (f, cap) in enumerate(tour):
    items = [PAR(bl, i), IN(shots_[f])]
    if prev:
        items.append(OUT(prev))
    a.click(*items)
    prev = shots_[f]

# ======================================================================= 4 design philosophy
s, a = d.slide('Design philosophy', 'Design', notes=(
    'The course requirements were treated as design constraints from the first day: modular design with patterns, '
    'role-based access, a REST API, tests in a pipeline, containers, a public deployment and monitoring. Four goals '
    'follow from them, and the rest of the talk shows each one in the code.'))
tb(s, LM, 1.8, CW, 0.4, ['The course requirements became design constraints from day one, and four goals follow.'],
   size=18, color=MUTED, name='Intro')
y = 2.6
for n, h_, t_ in [('01', 'Exact money', 'BigDecimal, scale 2, half-even, whole poisha; a third decimal is rejected, never rounded'),
                  ('02', 'Three people, one codebase', 'Ports and events between modules, so each teammate builds and tests alone'),
                  ('03', 'Testable at every level', 'A pure-Java engine, unit and integration suites, a coverage gate in the pipeline'),
                  ('04', 'Operable from day one', 'Docker, health checks, metrics, a public deployment, a protected main branch')]:
    num = tb(s, LM, y, 1.1, 0.8, [n], size=40, bold=True, color=ORANGE, name='Goal ' + n)
    txt = tb(s, LM + 1.3, y + 0.03, 10.2, 0.95, [P(h_, size=24, bold=True, color=TEAL), P(t_, size=17)],
             name='Goal ' + n + ' text')
    a.click(IN(num), IN(txt))
    y += 1.05

# ======================================================================= 5 architecture
s, a = d.slide('A modular monolith', 'Design', notes=(
    'One deployable with strict internal boundaries. Calls go down through security to the modules; modules '
    'cooperate through services, ports and events, shown dashed, so any of them could become its own service. '
    'The engine is plain Java with no Spring and no JPA. More than 200 production classes and about 100 test classes.'))
cx = (LM + RM) / 2
r1 = tb(s, LM, 1.9, CW, 0.4, ['Browser (Thymeleaf pages, wizard JavaScript) and REST clients'], size=18, align='c',
        name='Clients')
a1 = arrow(s, cx, 2.38, cx, 2.68)
r2 = tb(s, LM, 2.72, CW, 0.4, [P('Spring Security: session, CSRF, roles, resource-level checks', bold=True, color=TEAL)],
        size=18, align='c', name='Security')
colw, gap = 2.2, 0.9
x0 = LM + (CW - (4 * colw + 3 * gap)) / 2
xs = [x0 + i * (colw + gap) for i in range(4)]
mods = [[P('auth \u00b7 user \u00b7 friend', size=16, bold=True, color=TEAL),
         P('notification \u00b7 admin', size=16, bold=True, color=TEAL),
         P('accounts, friends, alerts, console', size=13, color=MUTED)],
        [P('outing', size=16, bold=True, color=TEAL), P('aggregate, services, REST, pages', size=13, color=MUTED),
         P('engine: pure Java', size=13, bold=True, color=ORANGE_INK)],
        [P('ledger', size=16, bold=True, color=TEAL), P('debts, payments, disputes, delegation', size=13, color=MUTED)],
        [P('activity', size=16, bold=True, color=TEAL), P('timeline and per-friend history', size=13, color=MUTED)]]
mod_shapes = [tb(s, xs[i], 3.75, colw, 1.2, mods[i], align='c', after=2, name='Module %d' % (i + 1)) for i in range(4)]
down1 = [arrow(s, xs[i] + colw / 2, 3.2, xs[i] + colw / 2, 3.68) for i in range(4)]
dashed = [arrow(s, xs[1] - 0.05, 4.2, xs[0] + colw + 0.05, 4.2, color=ORANGE, width=1.75, dashed=True, name='Event'),
          arrow(s, xs[1] + colw + 0.05, 4.2, xs[2] - 0.05, 4.2, color=ORANGE, width=1.75, dashed=True, name='Event'),
          arrow(s, xs[2] + colw + 0.05, 4.2, xs[3] - 0.05, 4.2, color=ORANGE, width=1.75, dashed=True, name='Event')]
db = tb(s, LM, 5.6, CW, 0.4, ['PostgreSQL (Neon, Compose) or H2 (tests and offline runs)'], size=18, align='c',
        name='Database')
down2 = [arrow(s, xs[i] + colw / 2, 5.1, xs[i] + colw / 2, 5.52) for i in range(4)]
tb(s, LM, 6.25, CW, 0.3, ['Java 21 \u00b7 Spring Boot 4.1 \u00b7 Spring Security \u00b7 Spring Data JPA \u00b7 Thymeleaf'],
   size=13, color=MUTED, align='c', name='Stack')
legend = tb(s, LM, 6.52, CW, 0.3, [P(('Solid arrows are calls. ', dict(color=MUTED)),
                                    ('Dashed arrows are domain events.', dict(color=ORANGE_INK, bold=True)))],
            size=12, align='c', name='Legend')
a.click(IN(r1), IN(a1), IN(r2))
a.click(*[IN(x) for x in mod_shapes + down1])
a.click(IN(db), *[IN(x) for x in down2])
a.click(*[IN(x) for x in dashed + [legend]])

# ======================================================================= 6 boundaries
s, a = d.slide('Boundaries that keep modules apart', 'Design', notes=(
    'Four rules and one honest exception. The engine imports no framework. Modules never touch each other\'s '
    'entities; they link by id and react through events. Outing needs friendship and payment facts, so it declares '
    'its own narrow ports that other modules implement. A few read-only views do read other repositories, and that '
    'is listed as future work. Below: what happens when an outing is submitted.'))
bl = bullets(s, LM, 1.9, CW, 2.8, [
    'The engine imports no Spring and no JPA: zero framework imports under outing/engine',
    'Outing, ledger and activity never touch each other\u2019s entities; they link by id',
    'Cross-module reactions go through events: OutingActivated, OutingUpdated, OutingCancelled',
    'Outing declares its own narrow ports; adapters in the owning modules implement them',
    [o('Honest exception: '), 'a few read-only views read other repositories (future work)']], size=18, after=12)
hd = heading(s, LM, 4.65, 6, 'Submitting an outing', 'Flow heading')
flow = ['POST /submit', 'Engine calculates, or lists every issue', 'Status DRAFT \u2192 ACTIVE', 'OutingActivated event',
        'Ledger creates the debts; one commit']
fw, fg = 1.9, (CW - 5 * 1.9) / 4
fshapes = []
for i, t_ in enumerate(flow):
    fx = LM + i * (fw + fg)
    fshapes.append(tb(s, fx, 5.15, fw, 0.9, [P(t_, bold=(i in (0, 4)), color=TEAL if i in (0, 4) else INK)],
                      size=15, align='c', anchor='m', name='Step %d' % (i + 1)))
    if i:
        fshapes.append(arrow(s, fx - fg + 0.06, 5.6, fx - 0.06, 5.6, color=ORANGE, width=1.75))
for i in range(5):
    a.click(PAR(bl, i))
a.click(IN(hd), *[IN(x) for x in fshapes])

# ======================================================================= 7 principles
s, a = d.slide('Design principles, each with an example', 'Design', notes=(
    'SOLID, plus two more that mattered in practice: fail fast and least privilege. Each line names where it is '
    'visible in the code rather than just naming the principle.'))


def principle(name, example):
    return P((name, dict(bold=True, color=TEAL, size=22)), BR, (example, dict(size=18)))


h1 = heading(s, LM, 1.9, 5, 'SOLID', 'SOLID heading')
left = tb(s, LM, 2.35, 5.7, 4.2, [
    principle('Single Responsibility', 'One validation rule per class; thin controllers'),
    principle('Open / Closed', 'A new split type is a new strategy, not an edit'),
    principle('Liskov Substitution', 'Every strategy honours the template contract'),
    principle('Interface Segregation', 'Narrow ports such as areFriends, not whole services')],
    size=18, after=18, name='SOLID principles')
h2 = heading(s, 7.0, 1.9, 5, 'In practice', 'Practice heading')
right = tb(s, 7.0, 2.35, 5.43, 4.2, [
    principle('Dependency Inversion', 'Outing depends on its own ports; the engine on nothing'),
    principle('Fail fast', 'Factory completeness check; Money rejects a third decimal'),
    principle('Least privilege', 'Service and URL checks, non-root container, secrets from the environment')],
    size=18, after=18, name='Practice principles')
a.click(IN(h1), PAR(left, 0))
for i in range(1, 4):
    a.click(PAR(left, i))
a.click(IN(h2), PAR(right, 0))
a.click(PAR(right, 1))
a.click(PAR(right, 2))

# ======================================================================= 8 patterns: split rules
s, a = d.slide('Patterns for the split rules', 'Design', notes=(
    'Strategy, Template Method and Factory work together. The base class owns the checks every split needs, in a '
    'final method no subclass can skip. Subclasses own only the arithmetic. The factory guarantees every split type '
    'has exactly one strategy, so forgetting one breaks start-up and a test, not a customer\'s bill.'))
cxl = LM + 2.3
l1 = label(s, LM, 2.3, 4.6, 'SplitStrategyFactory', 'Factory \u00b7 one mapping, checked at start-up', size=18, h=0.8,
           name='Factory')
l2 = label(s, LM, 3.4, 4.6, 'SplitStrategy', 'Strategy \u00b7 split rules vary independently', size=18, h=0.8,
           name='Strategy')
l3 = label(s, LM, 4.5, 4.6, 'AbstractSplitStrategy', 'Template Method \u00b7 final split() runs the shared checks',
           size=18, h=0.8, name='Template')
ar1 = arrow(s, cxl, 2.72, cxl, 2.98)
ar2 = arrow(s, cxl, 3.82, cxl, 4.08)
leaves, fan = [], []
for i, nm in enumerate(['Personal', 'Equal', 'Percentage']):
    lx = LM + i * 1.55
    leaves.append(tb(s, lx, 5.4, 1.5, 0.4, [nm], size=17, bold=True, color=TEAL, align='c', name='Strategy ' + nm))
    fan.append(arrow(s, cxl, 4.92, lx + 0.75, 5.33))
cd = code(s, 6.7, 1.95, 5.7, 3.1, [
    'public abstract class AbstractSplitStrategy',
    '        implements SplitStrategy {',
    '',
    '    public final Map<ParticipantRef, Money>',
    '            split(ItemLine item) {',
    '        // checks every strategy needs',
    '        return unmodifiableMap(doSplit(item));',
    '    }',
    '',
    '    protected abstract Map<ParticipantRef, Money>',
    '            doSplit(ItemLine item);',
    '}'], size=14, name='Template Method code')
closing = tb(s, 6.7, 5.35, 5.73, 1.2, [
    P('Adding an \u201cexact amounts\u201d split is one new strategy and one enum value. Forgetting the strategy breaks '
      'start-up, not a customer\u2019s bill.')], size=17, name='Why it matters')
a.click(IN(l1))
a.click(IN(ar1), IN(l2))
a.click(IN(ar2), IN(l3), *[IN(x) for x in fan + leaves])
a.click(IN(cd))
a.click(IN(closing))

# ======================================================================= 9 observer and ports
s, a = d.slide('Observer: modules react without coupling', 'Design', notes=(
    'Outing publishes an event and the ledger, notifications and activity listen; outing does not know they exist. '
    'The phase matters. The first version listened AFTER_COMMIT and an integration test caught an active outing with '
    'zero debts, because writes made after the commit were silently lost. BEFORE_COMMIT makes the outing and its '
    'debts commit together.'))
bl = bullets(s, LM, 1.95, 5.7, 2.9, [
    'Outing publishes OutingActivated; ledger, notifications and activity listen',
    'Narrow ports of its own: FriendshipChecker, PaymentLockChecker',
    'Adapters in the owning modules implement them'], size=18, after=14)
cd = code(s, 6.9, 1.95, 5.5, 2.6, [
    '@TransactionalEventListener(',
    '    phase = TransactionPhase.BEFORE_COMMIT,',
    '    fallbackExecution = true)',
    'public void onOutingActivated(',
    '        OutingActivatedEvent event) {',
    '    debts.generateDebts(event.outingId());',
    '}'], size=14, name='Listener code')
wh = heading(s, LM, 4.95, 6, 'What went wrong', 'Story heading')
story = tb(s, LM, 5.3, CW, 1.3, [
    P('The first version listened ', mono('AFTER_COMMIT', color=ORANGE_INK, bold=True),
      '. An integration test found an active outing with zero debts: writes made after the commit were silently lost. '
      'Moving to ', mono('BEFORE_COMMIT', color=TEAL, bold=True), ' makes the outing and its debts commit together.')],
    size=18, name='Story')
for i in range(3):
    a.click(PAR(bl, i))
a.click(IN(cd))
a.click(IN(wh), IN(story))

# ======================================================================= 10 states and other patterns
s, a = d.slide('State machines, and the other patterns', 'Design', notes=(
    'State: each status owns its legal transitions. Anything else throws an exception that the API maps to 409. '
    'Below, the remaining patterns in one place: Value Object for Money, a collecting Chain of Responsibility for the '
    'seven validation rules, Facades as single entry points, the Outing aggregate root, adapters, DTOs so entities '
    'never reach JSON, and a policy object for who may see an outing.'))
Y = 3.1


def node(x, y, w, text, **kw):
    return tb(s, x, y, w, 0.4, [text], size=16, bold=True, color=TEAL, align='c', name='State ' + text, **kw)


def tag(x, y, w, text):
    return tb(s, x, y, w, 0.3, [text], size=12, color=MUTED, align='c', name='Transition ' + text)


o1, o2, o3 = node(0.9, Y, 1.3, 'DRAFT'), node(3.0, Y, 1.3, 'ACTIVE'), node(5.1, Y, 1.3, 'SETTLED')
oc = node(2.55, 4.0, 2.2, 'CANCELLED')
outing = [o1, o2, o3, oc, arrow(s, 2.25, Y + 0.2, 2.95, Y + 0.2), arrow(s, 4.35, Y + 0.2, 5.05, Y + 0.2),
          tag(2.2, Y - 0.32, 0.8, 'submit'), tag(4.3, Y - 0.32, 0.8, 'all paid'),
          arrow(s, 1.55, Y + 0.45, 2.7, 3.95), arrow(s, 3.65, Y + 0.45, 3.65, 3.95)]
d1, d2, d3 = node(6.9, Y, 1.0, 'OPEN'), node(8.6, Y, 1.9, 'PARTIALLY_PAID'), node(11.2, Y, 1.1, 'PAID')
dd = node(7.9, 2.1, 1.6, 'DISPUTED')
debt = [d1, d2, d3, dd, arrow(s, 7.95, Y + 0.2, 8.55, Y + 0.2), arrow(s, 10.55, Y + 0.2, 11.15, Y + 0.2),
        tag(7.9, Y - 0.32, 0.7, 'part'), tag(10.5, Y - 0.32, 0.7, 'rest'),
        arrow(s, 7.4, Y - 0.02, 8.1, 2.55), arrow(s, 9.55, Y - 0.02, 9.2, 2.55)]
cap1 = tb(s, LM, 4.7, 5.6, 0.3, ['Outing: cancelling is allowed from DRAFT and ACTIVE'], size=13, color=MUTED, name='Outing caption')
cap2 = tb(s, 6.9, 4.7, 5.53, 0.5, ['Debt: a resolved dispute returns to OPEN; VOID on cancellation'],
          size=13, color=MUTED, name='Debt caption')
oh = heading(s, LM, 5.2, 6, 'Also in the code', 'Other patterns heading')
others = tb(s, LM, 5.52, CW, 1.2, [
    P(b('Value Object'), ' Money'), P(b('Chain of Responsibility'), ' seven validation rules'),
    P(b('Facade'), ' SplitEngine, OutingApiFacade'), P(b('Aggregate Root'), ' Outing'),
    P(b('Adapter'), ' snapshot mapper, settlement provider'), P(b('DTO'), ' entities never reach JSON'),
    P(b('Policy object'), ' OutingAccessPolicy')], size=14, after=3, cols=2, name='Other patterns')
a.click(*[IN(x) for x in outing], IN(cap1))
a.click(*[IN(x) for x in debt], IN(cap2))
a.click(IN(oh), IN(others))

# ======================================================================= 11 data model
s, a = d.slide('Data model: 14 tables, four groups', 'Design', notes=(
    'Fourteen tables in four module groups. Inside a group, normal JPA relationships with cascade and orphan removal '
    'from the aggregate root. Between groups, only ids with no foreign key (decision D30), shown dashed. The trade-off '
    'is explicit: integrity between modules is enforced in services, and inside an aggregate by JPA.'))
cw, cg = 2.35, 0.7
cxs = [LM + i * (cw + cg) for i in range(4)]
groups = [('Identity', ['users', 'friendships', 'guests']),
          ('Outing aggregate', ['outings', 'outing_participants', 'items', 'item_shares', 'contributions']),
          ('Ledger', ['debts', 'payments', 'delegations', 'guest_debts']),
          ('Notification and activity', ['notifications', 'activity_events'])]
gshapes = []
for i, (gname, tabs_) in enumerate(groups):
    gshapes.append(tb(s, cxs[i], 1.95, cw, 0.7, [P(gname, bold=True, color=TEAL)], size=18, name='Group ' + gname))
    gshapes.append(tb(s, cxs[i], 2.8, cw, 1.7, [P(t_) for t_ in tabs_], size=15, font=MONO, after=3,
                      name='Tables ' + gname))
arrows_ = [arrow(s, cxs[1] - 0.08, 2.2, cxs[0] + cw + 0.08, 2.2, color=ORANGE, width=1.75, dashed=True),
           arrow(s, cxs[2] - 0.08, 2.2, cxs[1] + cw + 0.08, 2.2, color=ORANGE, width=1.75, dashed=True)]
bl = bullets(s, LM, 4.75, CW, 1.9, [
    [b('Inside a module: '), 'JPA relationships, cascade and orphan removal from the aggregate root'],
    [b('Between modules: '), 'ids only, no foreign key (D30), so modules stay independent'],
    [b('Integrity: '), 'NUMERIC(12,2) for money; @Version on Outing and Debt for optimistic locking']],
    size=17, after=10)
a.click(*[IN(x) for x in gshapes[:4] + arrows_[:1]])
a.click(*[IN(x) for x in gshapes[4:] + arrows_[1:]])
for i in range(3):
    a.click(PAR(bl, i))

# ======================================================================= 12 decisions
s, a = d.slide('Five decisions that shaped the system', 'Design', notes=(
    'Every decision in the log records the alternative and the reason. These five shaped the architecture most. '
    'The numbering follows the project\'s decision log.'))
dec = tb(s, LM, 1.95, CW, 4.7, [
    P(o('D1  '), b('Pure-Java calculation engine'), m('  over a Spring service on entities'), BR,
      'Fast, deterministic tests, and one engine for preview, submit and ledger'),
    P(o('D16  '), b('Debts generated BEFORE_COMMIT'), m('  over AFTER_COMMIT'), BR,
      'Writes made after the commit were silently lost'),
    P(o('D30  '), b('Cross-module links are ids'), m('  over @ManyToOne everywhere'), BR,
      'Modules stay independent and replaceable'),
    P(o('D17  '), b('Session login with CSRF'), m('  over JWT'), BR,
      'One security model for pages and API; /api answers 401 instead of redirecting'),
    P(o('D21  '), b('Metrics on their own Basic-auth chain'), m('  over public or form login'), BR,
      'Prometheus cannot fill in a login form; closed unless a password is set')],
    size=18, after=18, name='Decisions')
for i in range(5):
    a.click(PAR(dec, i))

# ======================================================================= 13 REST API
s, a = d.slide('REST API design', 'API', notes=(
    'Thirty-eight endpoints in eleven controllers, with server-rendered pages beside the API. Authentication is the '
    'session cookie from form login, and every state-changing request needs a CSRF token. Errors are RFC 9457 '
    'problem details. A 404 is also used for things a user may not see, so existence never leaks. The API never '
    'accepts a user id from the client.'))
big = tb(s, LM, 1.7, 2.1, 1.4, ['38'], size=88, bold=True, color=TEAL, name='Endpoint count')
bigl = tb(s, 3.0, 2.25, 3.4, 0.9, ['endpoints in', '11 controllers'], size=20, name='Endpoint label')
bl = bullets(s, LM, 3.35, 5.7, 3.4, [
    'Session cookie from form login; CSRF on every state change',
    'Unauthenticated /api calls get 401; pages redirect to login',
    'Errors are RFC 9457 problem details: 400, 403, 404, 409, 422',
    '404 for what you may not see, so existence never leaks',
    'The acting user comes from the session, never from the client'], size=16, after=9)
eh = heading(s, 7.1, 1.95, 5, 'Endpoints by area', 'Endpoints heading')
areas = [('Auth', 'register'), ('Profile', 'me, user lookup'), ('Friends', 'list, requests, remove'),
         ('Guests', 'list, add, remove'), ('Outings', 'CRUD, preview, submit, cancel'),
         ('Ledger', 'ledger, debts'), ('Settlement', 'pay, confirm, dispute'), ('Delegation', 'suggest, execute'),
         ('Notifications', 'list, count, mark read'), ('Activity', 'timeline with filters'),
         ('Admin', 'stats, users?q=, role  (ADMIN)'), ('Operations', 'health, prometheus')]
ep = tb(s, 7.1, 2.35, 5.33, 4.4, [P(b(n_), '\t', t_) for n_, t_ in areas], size=15, after=5, tabs=[(1.75, 'l')],
        name='Endpoint list')
a.click(IN(big), IN(bigl), IN(eh), IN(ep))
for i in range(5):
    a.click(PAR(bl, i))

# ======================================================================= 14 API in action
s, a = d.slide('One call, every problem at once', 'API', notes=(
    'The live preview takes a draft and returns balances without saving anything. Incomplete input is still a normal '
    '200 with valid false and every problem listed, for example NO_ITEMS and PAID_NOT_EQUAL_BILL together. It runs '
    'the same engine as submit, so the numbers on screen are the numbers that will be stored.'))
bl = bullets(s, LM, 1.95, 5.2, 4.5, [
    [mono('POST /api/outings/preview', color=TEAL, bold=True), ' takes a draft and saves nothing'],
    ['Incomplete input is a normal ', mono('200'), ' with ', mono('valid: false')],
    ['Every issue is listed at once, such as ', mono('NO_ITEMS'), ' and ', mono('PAID_NOT_EQUAL_BILL')],
    'The preview runs the same engine as submit, so the screen shows the stored numbers'], size=18, after=14)
cd = code(s, 6.5, 1.95, 5.93, 4.5, [
    ('POST /api/outings/preview', TEAL),
    ('two people, one Tk 800 item split equally,', MUTED),
    ('user 1 paid 800', MUTED),
    '',
    '{ "valid": true,',
    '  "issues": [],',
    '  "billTotal": 800.00,',
    '  "paidTotal": 800.00,',
    '  "balances": [',
    '    { "userId": 1, "consumed": 400.00,',
    '      "paid": 800.00, "net": 400.00 },',
    '    { "userId": 2, "consumed": 400.00,',
    '      "paid": 0.00, "net": -400.00 } ] }'], size=14, name='Preview example')
for i in range(4):
    a.click(PAR(bl, i))
a.click(IN(cd))

# ======================================================================= 15 security
s, a = d.slide('Security and role-based access', 'API', notes=(
    'Two roles, USER and ADMIN. The admin URLs need ROLE_ADMIN, with method security on the admin service. '
    'Everything else is authenticated by default and uses ownership checks instead of roles. Admins deliberately '
    'cannot read anyone\'s outings or debts: the console shows aggregates only. The administrator is a real account '
    'named in configuration, not a demo account with a published password.'))
hr = heading(s, LM, 1.9, 5.5, 'Roles', 'Roles heading')
roles = bullets(s, LM, 2.25, 5.6, 2.3, [
    [mono('USER'), ' and ', mono('ADMIN'), '; admin URLs need ', mono('ROLE_ADMIN')],
    ['@PreAuthorize on the admin service; the last admin cannot be demoted'],
    ['The administrator is a real account named in configuration']], size=16, after=8, name='Roles bullets')
ho = heading(s, 6.9, 1.9, 5.5, 'Ownership checks', 'Ownership heading')
own = bullets(s, 6.9, 2.25, 5.53, 2.3, [
    'Outings: the creator, and participants once submitted; others get 404',
    'Debts: debtor and creditor only; only the creditor confirms a payment',
    'BCrypt, CSRF on every write, Bean Validation, no stack traces'], size=16, after=8, name='Ownership bullets')
SIDEBAR = 250 / 1440  # crop the app's sidebar so the screenshot shows only the page that matters
CROP = (SIDEBAR, 0.09, 0, 0.262)  # same crop for both, so they line up: page content only, no sidebar or empty foot
p1, (_, _, _, h1_) = pic(s, shot('error-403.png'), LM, 4.55, 4.2, 1.9, crop=CROP,
                         alt='A normal user opening /admin sees a friendly 403 page', name='Screenshot 403')
c1 = tb(s, LM, 4.55 + h1_ + 0.08, 4.6, 0.3, ['A normal user opening /admin gets a 403 page'], size=13, color=MUTED,
        name='Caption 403')
p2, (_, _, _, h2_) = pic(s, shot('admin.png'), 6.9, 4.55, 4.2, 1.9, crop=CROP,
                         alt='Admin console with platform statistics and user search', name='Screenshot admin')
c2 = tb(s, 6.9, 4.55 + h2_ + 0.08, 5.0, 0.3, ['Admin console: counts and search only, no private data'], size=13,
        color=MUTED, name='Caption admin')
a.click(IN(hr), PAR(roles, 0))
a.click(PAR(roles, 1))
a.click(PAR(roles, 2))
a.click(IN(p1), IN(c1))
a.click(IN(ho), PAR(own, 0))
a.click(PAR(own, 1))
a.click(PAR(own, 2))
a.click(IN(p2), IN(c2))

# ======================================================================= 16 testing strategy
s, a = d.slide('Testing strategy', 'Testing', notes=(
    'Seven hundred and sixty tests, all green: 658 unit tests run by Surefire and 102 integration tests run by '
    'Failsafe. The requirement was at least 20 and 10. JaCoCo enforces an 80 percent gate in the pipeline, and the '
    'engine itself is at 100 percent line coverage. The wizard JavaScript is the one thing not tested automatically.'))
stats = [('760', 'tests, no failures'), ('658', 'unit tests (Surefire)'), ('102', 'integration tests (Failsafe)'),
         ('80%', 'JaCoCo gate; engine at 100%')]
sshapes = []
for i, (n_, c_) in enumerate(stats):
    sshapes.append(tb(s, LM + i * 3.0, 1.9, 2.8, 1.5, [P(n_, size=56, bold=True, color=TEAL if i else ORANGE_INK),
                                                       P(c_, size=14, color=MUTED)], name='Stat ' + n_))
bl = bullets(s, LM, 3.85, CW, 2.9, [
    [b('Golden-master engine tests: '), 'burger night asserts exact numbers; 200 seeded random cases prove shares sum to the bill'],
    [b('Security matrices: '), 'every ledger and activity route called anonymously and as an unrelated user'],
    [b('End-to-end flows: '), 'create, submit, debts, cancel; dispute to settled; two concurrent payments'],
    [b('Not automated: '), 'the wizard\u2019s JavaScript, covered by the API tests it calls and a manual script']],
    size=17, after=11)
a.click(*[IN(x) for x in sshapes])
for i in range(4):
    a.click(PAR(bl, i))

# ======================================================================= 17 what the tests caught
s, a = d.slide('What the tests caught', 'Testing', notes=(
    'Real defects found by the suite. A BigDecimal.equals bug compares scale, so 100.00 percent was rejected: use '
    'compareTo. A missing cascade meant data was not saved even though the unit test passed: integration tests exist '
    'for a reason. And the AFTER_COMMIT bug. Finally, we proved the pipeline works by introducing the equals bug on '
    'purpose: the unit job went red and integration was skipped, then green after the fix.'))
caught = tb(s, LM, 1.95, 6.7, 4.7, [
    P(o('01  '), b('Valid 100.00 percentages rejected'), BR, 'BigDecimal.equals compares scale. Use compareTo; realistic input finds real bugs.'),
    P(o('02  '), b('Data not saved, unit test green'), BR, 'A missing cascade. Integration tests exist for a reason.'),
    P(o('03  '), b('Active outing with no debts'), BR, 'AFTER_COMMIT writes were lost. Cross-module tests find contract bugs.'),
    P(o('04  '), b('The pipeline proves itself'), BR, 'An intentional equals bug turned unit red and skipped integration; compareTo turned it green.')],
    size=16, after=16, name='Defects')
ci, _ = pic(s, shot('ci-pipeline.jpg'), 8.0, 2.0, 4.43, 3.9, alt='GitHub Actions job graph', name='Screenshot pipeline')
cic = tb(s, 8.0, 6.05, 4.43, 0.4, ['The GitHub Actions job graph'], size=13, color=MUTED, name='Caption pipeline')
for i in range(3):
    a.click(PAR(caught, i))
a.click(PAR(caught, 3), IN(ci), IN(cic))

# ======================================================================= 18 demo testing
s, a = d.slide('Demo testing: a 15-minute click-through', 'Testing', notes=(
    'MANUAL_TEST.md is a click-through script with seeded demo data and the exact expected numbers. Log in as Rafsan: '
    'net balance minus 1450, which is 550 owed by the guest Rahim minus 2000 owed to Tanvir. If you want to run it '
    'live, start the h2 profile and follow the checklist; otherwise this slide stands in for the demo.'))
demo = tb(s, LM, 1.95, 6.5, 4.3, [
    P(b('Login and home'), BR, 'Rafsan sees net \u2212Tk\u00a01450: owed 550 by guest Rahim, owes Tanvir 2000'),
    P(b('Outings'), BR, 'Tanvir sees Burger night but not Rafsan\u2019s draft; Samiha sees only her trip'),
    P(b('New outing wizard'), BR, 'Change one payment by Tk 100 and the preview names the difference'),
    P(b('Rules and security'), BR, 'Cancelling voids the debts; /admin refuses users; /?userId=1 still shows your own page')],
    size=16, after=16, name='Checks')
dp, _ = pic(s, shot('outing-detail.png'), 7.5, 1.95, 4.93, 2.9, crop=(0, 0, 15 / 1440, 0),
            alt='Burger night outing detail', name='Screenshot outing')
dc = tb(s, 7.5, 4.95, 4.93, 0.5, ['Burger night: Rafsan ate 500 and paid 1050'], size=13, color=MUTED, name='Caption outing')
run_it = tb(s, LM, 6.35, CW, 0.35, [P(m('Run it yourself: '), mono('MANUAL_TEST.md', color=TEAL), m('  \u00b7  '),
                                      mono('./mvnw verify', color=TEAL))], size=14, name='Run it')
a.click(PAR(demo, 0), IN(dp), IN(dc))
for i in (1, 2, 3):
    a.click(PAR(demo, i))
a.click(IN(run_it))

# ======================================================================= 19 CI/CD
s, a = d.slide('CI/CD and the Git workflow', 'Delivery', notes=(
    'One task, one branch, one pull request. Feature branches merge into develop with CI; develop merges into main '
    'through a release pull request, and main is protected. The pipeline: unit tests with an 80 percent coverage '
    'gate, then integration tests and a Docker check that the image builds, starts and reports UP, then a deploy '
    'job that runs only for main. Render\'s own auto-deploy is off, so only the pipeline can release.'))
yc = 2.95
pa = label(s, LM, yc, 1.9, 'Push or pull request', name='Trigger')
pu = label(s, 3.3, yc, 2.4, 'Unit tests', '80% coverage gate', name='Unit tests')
pi = label(s, 6.5, yc - 0.65, 2.4, 'Integration tests', name='Integration tests')
pd = label(s, 6.5, yc + 0.65, 2.4, 'Docker image', 'builds, starts, health UP', name='Docker check')
pr = label(s, 9.9, yc, 2.53, 'Deploy to Render', 'main only, then health check', name='Deploy')
flow = [pa, pu, pi, pd, pr,
        arrow(s, 2.85, yc, 3.3, yc), arrow(s, 5.7, yc - 0.1, 6.45, yc - 0.55), arrow(s, 5.7, yc + 0.1, 6.45, yc + 0.55),
        arrow(s, 8.9, yc - 0.55, 9.85, yc - 0.1), arrow(s, 8.9, yc + 0.55, 9.85, yc + 0.1)]
bl = bullets(s, LM, 4.5, CW, 2.3, [
    [b('One task, one branch, one pull request: '), 'feature \u2192 develop with CI; develop \u2192 main by release PR'],
    [b('main is protected: '), 'no direct pushes, one approval, green required checks; merge commits keep history visible'],
    [b('Only the pipeline can release: '), 'Render auto-deploy is off; a deploy hook, then the health check']],
    size=17, after=11)
a.click(*[IN(x) for x in flow])
for i in range(3):
    a.click(PAR(bl, i))

# ======================================================================= 20 Docker
s, a = d.slide('Docker: three stages, one small runtime', 'Delivery', notes=(
    'The Dockerfile has three stages. The builder copies pom.xml first so dependency layers cache, and does not '
    're-run unit tests, which already run in CI. The extractor unpacks the Spring Boot layered jar so rarely changing '
    'dependencies and often changing code sit in different layers. The runner is a Java 21 JRE under a non-root user '
    'with a real health check. Compose adds PostgreSQL and an optional monitoring profile.'))
cols = [('builder', ['Maven with Java 21', 'pom.xml copied first, for layer caching', 'Dependencies from a Maven Central mirror, with retries',
                     'Packages the jar; unit tests already ran in CI']),
        ('extractor', ['Unpacks the Spring Boot layered jar', 'Dependencies and application code land in separate layers',
                       'Rarely changing layers stay cached']),
        ('runner', ['Java 21 JRE, non-root user', 'Layers copied in order of change frequency',
                    'Container-aware JVM memory; honours PORT', 'HEALTHCHECK requires \u201cstatus\u201d:\u201cUP\u201d'])]
cwid, cgap = 3.35, (CW - 3 * 3.35) / 2
for i, (nm, items) in enumerate(cols):
    cx_ = LM + i * (cwid + cgap)
    h_ = tb(s, cx_, 1.95, cwid, 0.5, [nm], size=24, bold=True, color=TEAL, name='Stage ' + nm)
    l_ = bullets(s, cx_, 2.6, cwid, 2.7, items, size=15, after=7, name='Stage %s points' % nm)
    grp = [IN(h_), IN(l_)]
    if i:
        grp.append(IN(arrow(s, cx_ - cgap + 0.1, 2.2, cx_ - 0.1, 2.2, color=ORANGE, width=1.75)))
    a.click(*grp)
comp = tb(s, LM, 5.55, CW, 1.1, [P(b('Docker Compose: '), 'PostgreSQL 16 with a health check and a persistent volume, and the '
                                    'application, which starts once the database is healthy. An optional monitoring '
                                    'profile adds Prometheus and Grafana.')], size=17, name='Compose')
a.click(IN(comp))

# ======================================================================= 21 hosting
s, a = d.slide('Hosting on Render and Neon', 'Delivery', notes=(
    'The application runs as a Docker web service on Render\'s free tier, built from the repository Dockerfile, with '
    'Neon as the managed PostgreSQL in the same region. Configuration comes entirely from environment variables, '
    'with no secrets in the repository. KOTOPAI_ADMIN_USER_CODE names the account that becomes administrator on every '
    'start. Free instances sleep after about 15 idle minutes and need about a minute to wake.'))
steps = ['Merge to main', 'CI is green', 'Deploy hook for the exact commit', 'Render builds the Dockerfile',
         'Health check says UP']
fw, fg = 1.9, (CW - 5 * 1.9) / 4
fl = []
for i, t_ in enumerate(steps):
    fx = LM + i * (fw + fg)
    fl.append(tb(s, fx, 1.9, fw, 0.8, [t_], size=15, bold=True, color=TEAL, align='c', anchor='m', name='Step %d' % (i + 1)))
    if i:
        fl.append(arrow(s, fx - fg + 0.06, 2.3, fx - 0.06, 2.3, color=ORANGE, width=1.75))
bl = bullets(s, LM, 3.2, 6.9, 3.5, [
    [b('Render: '), 'Docker web service on the free tier, built from the Dockerfile'],
    [b('Neon: '), 'managed PostgreSQL, SSL required, same region'],
    [b('Configuration '), 'only from environment variables; no secrets in the repository'],
    [mono('KOTOPAI_ADMIN_USER_CODE', color=TEAL), ' makes a real administrator on every start'],
    ['Free instances sleep after about 15 idle minutes'],
    [('koto-pai-aatd.onrender.com', dict(color=TEAL, bold=True, link='https://koto-pai-aatd.onrender.com'))]],
    size=16, after=8)
rp, _ = pic(s, shot('render-deploys.jpg'), 8.3, 3.15, 4.13, 3.6, alt='Render service and deploys', align='r',
            name='Screenshot Render')
a.click(*[IN(x) for x in fl])
for i in range(3):
    a.click(PAR(bl, i))
a.click(PAR(bl, 3), IN(rp))
a.click(PAR(bl, 4))
a.click(PAR(bl, 5))

# ======================================================================= 22 monitoring
s, a = d.slide('Monitoring with Prometheus and Grafana', 'Delivery', notes=(
    'Spring Boot Actuator exposes health with liveness and readiness probes. Micrometer publishes metrics to '
    '/actuator/prometheus, protected by Basic auth on a separate filter chain, tagged application=kotopai, with p50, '
    'p95 and p99 latency. Grafana Cloud scrapes it once a minute and shows a twelve-panel dashboard versioned as '
    'JSON in the repository, so it stays up without anyone\'s laptop.'))
mflow = ['Actuator and Micrometer', '/actuator/prometheus behind Basic auth', 'Grafana Cloud scrapes every minute',
         '12-panel dashboard']
mw, mg = 2.5, (CW - 4 * 2.5) / 3
ml = []
for i, t_ in enumerate(mflow):
    mx = LM + i * (mw + mg)
    ml.append(tb(s, mx, 1.9, mw, 0.8, [t_], size=15, bold=True, color=TEAL, align='c', anchor='m', name='Step %d' % (i + 1)))
    if i:
        ml.append(arrow(s, mx - mg + 0.06, 2.3, mx - 0.06, 2.3, color=ORANGE, width=1.75))
bl = bullets(s, LM, 3.2, 6.9, 3.5, [
    ['Health with liveness and readiness probes, public'],
    ['Metrics tagged ', mono('application=kotopai'), ' with p50, p95 and p99 latency'],
    ['The dashboard is JSON in the repository: status, traffic, 5xx, latency, JVM, connection pool, CPU'],
    ['Three ways to view it: Compose profile, a laptop stack pointed at the live site, hosted Grafana Cloud']],
    size=16, after=10)
g2, _ = pic(s, shot('grafana-cloud-bottom.jpg'), 8.3, 3.15, 4.13, 3.6, alt='Grafana Cloud dashboard, bottom half',
            align='r', name='Screenshot Grafana bottom')
g1, _ = pic(s, shot('grafana-cloud-top.jpg'), 8.3, 3.15, 4.13, 3.6, alt='Grafana Cloud dashboard, top half',
            align='r', name='Screenshot Grafana top')  # added last so the top half is the one a static export shows
a.click(*[IN(x) for x in ml])
a.click(PAR(bl, 0), IN(g1))
a.click(PAR(bl, 1))
a.click(PAR(bl, 2), OUT(g1), IN(g2))
a.click(PAR(bl, 3))

# ======================================================================= 23 how we worked
s, a = d.slide('How we worked', 'Project management', notes=(
    'The process: a written roadmap of numbered tasks with a definition of done, one task per branch per pull '
    'request, and a per-task work log. Contracts first: cross-module interfaces were agreed in an architecture '
    'document before parallel work, so a module could build against a port with a stub before its neighbour existed. '
    'Each member owned a vertical slice end to end: service, API, pages and tests.'))
bl = bullets(s, LM, 1.95, 5.7, 3.4, [
    'One task, one branch, one pull request',
    'Contracts first: interfaces agreed in an architecture document before parallel work',
    'Integration checkpoints on scheduled days',
    'Quality gates: protected main, required checks, review, coverage gate',
    'A written roadmap and a per-task work log'], size=17, after=9)
own = tb(s, 7.1, 1.95, 5.33, 3.5, [
    P(b('Rafsan Riasat'), m('  2207006'), BR,
      'outing and split engine, dashboard, CI pipeline, Docker, deployment, monitoring, admin bootstrap and search'),
    P(b('Adib Raian'), m('  2207020'), BR,
      'ledger (debts, payments, disputes, delegation, simplifier), activity, and their pages'),
    P(b('Utsa Roy'), m('  2207027'), BR,
      'auth, user, friend, notification, admin, common, base layout, security hardening')],
    size=14, after=12, name='Ownership')
th = heading(s, LM, 5.35, 6, 'Timeline, 2026', 'Timeline heading')
tl = [('Oct 28\u201331', 'Repository, CI, engine core'), ('Nov 5\u201314', 'Ledger, services, REST API, authentication'),
      ('Nov 15\u201325', 'Payments, friends, notifications, wizard'), ('Nov 27', 'Deployment, monitoring, hosted Grafana'),
      ('Nov 28', 'Real administrator, admin search')]
tls = []
for i, (dt, tx) in enumerate(tl):
    tls.append(tb(s, LM + i * 2.33, 5.7, 2.2, 1.0, [P(dt, size=15, bold=True, color=ORANGE_INK), P(tx, size=13)],
                  name='Milestone ' + dt))
for i in range(5):
    a.click(PAR(bl, i))
a.click(*[IN(x) for x in [own]])
a.click(IN(th), *[IN(x) for x in tls])

# ======================================================================= 24 limits and next
s, a = d.slide('Honest limits and what comes next', 'Project management', notes=(
    'The limitations are real and listed in the README. Cross-module ids and a few read-only cross-module views; '
    'Hibernate ddl-auto instead of a migration tool; no login rate limiting, password reset or e-mail verification; a '
    'single instance with in-memory sessions on a free tier that sleeps; and no automated tests for the wizard '
    'JavaScript. A second-platform deployment was not done. Each limit has a natural next step.'))
h1 = heading(s, LM, 1.95, 5, 'Limits today', 'Limits heading')
lim = bullets(s, LM, 2.35, 5.6, 4.2, [
    'Module links are ids; a few read-only views read other repositories',
    'Schema by Hibernate ddl-auto, not Flyway or Liquibase',
    'No login rate limiting, password reset or e-mail verification',
    'One instance, in-memory sessions, a free tier that sleeps',
    'The wizard\u2019s JavaScript has no automated tests'], size=17, after=10, name='Limits')
h2 = heading(s, 7.0, 1.95, 5, 'Next', 'Next heading')
nxt = bullets(s, 7.0, 2.35, 5.43, 4.2, [
    'Small query ports in place of cross-module reads',
    'A migration tool for the schema',
    'Rate limiting, password reset, e-mail verification',
    'An exact-amounts split: the Strategy seam is ready',
    'Receipts, recurring outings, push notifications'], size=17, after=10, name='Next steps')
a.click(IN(h1), PAR(lim, 0))
for i in range(1, 5):
    a.click(PAR(lim, i))
a.click(IN(h2), PAR(nxt, 0))
for i in range(1, 5):
    a.click(PAR(nxt, i))

# ======================================================================= 25 thank you
s, a = d.slide('Thank you', notes=(
    'Thank you. The application is live, the code and the README with diagrams are on GitHub, and the report has the '
    'full write-up. Happy to take questions or run the click-through live.'))
t = s.shapes.title
t.left, t.top, t.width, t.height = Inches(LM), Inches(1.9), Inches(11), Inches(1.4)
t.text_frame.paragraphs[0].runs[0].font.size = Pt(80)
tb(s, LM, 3.45, 8, 0.6, ['Questions?'], size=32, name='Questions')
tb(s, LM, 4.6, 9, 0.9, [
    P(m('Live  '), ('koto-pai-aatd.onrender.com', dict(color=TEAL, bold=True, link='https://koto-pai-aatd.onrender.com'))),
    P(m('Code  '), ('github.com/narukami00/Koto-Pai', dict(color=TEAL, bold=True, link='https://github.com/narukami00/Koto-Pai')))],
   size=20, after=8, name='Links')
tb(s, LM, 5.75, 11, 0.4, [P('Rafsan Riasat 2207006  \u00b7  Adib Raian 2207020  \u00b7  Utsa Roy 2207027')], size=15,
   color=MUTED, name='Team')

d.save(OUT_FILE)
print('saved', OUT_FILE, len(d.prs.slides), 'slides')
