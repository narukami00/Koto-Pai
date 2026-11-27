/*
 * Koto Pai: new / edit outing wizard.
 * One page, plain JavaScript. It talks only to the JSON API:
 *   GET /api/friends, GET/POST /api/guests, POST /api/outings, PUT /api/outings/{id},
 *   POST /api/outings/preview (live split), POST /api/outings/{id}/submit, GET /api/outings/{id} (edit).
 * People are identified by a key: "u<userId>" for a user, "g<guestId>" for a guest.
 */
(function () {
  'use strict';

  var root = document.getElementById('wizard');
  if (!root) { return; }

  var csrf = document.getElementById('csrf-token').value;
  var meId = Number(root.dataset.meId);
  var meName = root.dataset.meName;
  var outingId = root.dataset.outingId ? Number(root.dataset.outingId) : null;
  var ME = 'u' + meId;

  var state = {
    title: '',
    category: 'EATING',
    date: new Date().toLocaleDateString('en-CA'),
    friends: [],            // {key, name}
    guests: [],             // {key, name}
    selected: [ME],         // keys on the outing
    items: [],              // {name, amount, splitType, consumers: [key], pct: {key: text}}
    paid: {}                // key -> text
  };

  // ---------- small helpers ----------

  function $(id) { return document.getElementById(id); }

  function h(tag, attrs, children) {
    var el = document.createElement(tag);
    Object.keys(attrs || {}).forEach(function (k) {
      if (k === 'class') { el.className = attrs[k]; }
      else if (k.indexOf('on') === 0) { el.addEventListener(k.slice(2), attrs[k]); }
      else if (attrs[k] !== false && attrs[k] != null) { el.setAttribute(k, attrs[k] === true ? '' : attrs[k]); }
    });
    [].concat(children || []).forEach(function (c) {
      el.appendChild(typeof c === 'string' ? document.createTextNode(c) : c);
    });
    return el;
  }

  function money(n) { return '৳' + (Math.round(n * 100) / 100).toFixed(2); }
  function num(text) { var n = parseFloat(String(text).replace(/,/g, '')); return isNaN(n) ? NaN : n; }
  function round2(n) { return Math.round(n * 100) / 100; }

  function api(method, url, body) {
    var opts = { method: method, headers: { 'Accept': 'application/json', 'X-CSRF-TOKEN': csrf }, credentials: 'same-origin' };
    if (body !== undefined) {
      opts.headers['Content-Type'] = 'application/json';
      opts.body = JSON.stringify(body);
    }
    return fetch(url, opts).then(function (res) {
      return res.text().then(function (text) {
        var data = text ? JSON.parse(text) : null;
        if (!res.ok) { var err = new Error((data && data.detail) || res.statusText); err.problem = data; err.status = res.status; throw err; }
        return data;
      });
    });
  }

  function nameOf(key) {
    if (key === ME) { return 'You'; }
    var all = state.friends.concat(state.guests);
    for (var i = 0; i < all.length; i++) { if (all[i].key === key) { return all[i].name; } }
    return key;
  }

  function ref(key) { return key.charAt(0) === 'u' ? { userId: Number(key.slice(1)) } : { guestId: Number(key.slice(1)) }; }

  /** Participants in a stable order: me, friends, guests. */
  function people() {
    var order = [ME].concat(state.friends.map(function (f) { return f.key; }), state.guests.map(function (g) { return g.key; }));
    return order.filter(function (k) { return state.selected.indexOf(k) >= 0; });
  }

  // ---------- rendering ----------

  function chip(key, label, on, locked, onToggle) {
    return h('button', { type: 'button', class: 'chip' + (on ? ' on' : '') + (locked ? ' locked' : ''),
      'aria-pressed': on ? 'true' : 'false', disabled: locked, onclick: onToggle }, [label]);
  }

  function renderPeople() {
    var box = $('wiz-people');
    box.textContent = '';
    box.appendChild(chip(ME, meName + ' (you)', true, true));
    state.friends.forEach(function (f) {
      box.appendChild(chip(f.key, f.name, state.selected.indexOf(f.key) >= 0, false, function () { toggle(f.key); }));
    });
    if (!state.friends.length) { box.appendChild(h('span', { class: 'help' }, ['No friends yet. Add some on the Friends page.'])); }
    var gbox = $('wiz-guests');
    gbox.textContent = '';
    state.guests.forEach(function (g) {
      gbox.appendChild(chip(g.key, g.name, state.selected.indexOf(g.key) >= 0, false, function () { toggle(g.key); }));
    });
    if (!state.guests.length) { gbox.appendChild(h('span', { class: 'help' }, ['No guests yet.'])); }
  }

  function toggle(key) {
    var i = state.selected.indexOf(key);
    if (i >= 0) {
      state.selected.splice(i, 1);
      state.items.forEach(function (it) {
        it.consumers = it.consumers.filter(function (k) { return k !== key; });
        delete it.pct[key];
      });
      delete state.paid[key];
    } else {
      addPerson(key);
    }
    renderPeople(); renderItems(); renderPaid(); refresh();
  }

  /** Adds a person to the outing; items that everyone shared so far are shared with them too. */
  function addPerson(key) {
    var before = people().length;
    state.items.forEach(function (it) {
      if (it.splitType !== 'PERSONAL' && it.consumers.length === before) {
        it.consumers.push(key);
        if (it.splitType === 'PERCENTAGE') { evenPercentages(it); }
      }
    });
    state.selected.push(key);
  }

  function newItem() {
    return { name: '', amount: '', splitType: 'EQUAL', consumers: people().slice(), pct: {} };
  }

  function evenPercentages(item) {
    var n = item.consumers.length;
    item.pct = {};
    if (!n) { return; }
    var each = Math.floor(10000 / n) / 100;
    item.consumers.forEach(function (k, i) { item.pct[k] = String(i === 0 ? round2(100 - each * (n - 1)) : each); });
  }

  function renderItems() {
    var box = $('wiz-items');
    box.textContent = '';
    if (!state.items.length) { box.appendChild(h('p', { class: 'help' }, ['No items yet. Add the first thing that was ordered.'])); }
    state.items.forEach(function (item, idx) { box.appendChild(itemCard(item, idx)); });
  }

  function itemCard(item, idx) {
    var kinds = [['PERSONAL', 'One person'], ['EQUAL', 'Split equally'], ['PERCENTAGE', 'By percentage']];
    var select = h('select', { 'aria-label': 'How is it shared', onchange: function (e) {
      item.splitType = e.target.value;
      if (item.splitType === 'PERSONAL') { item.consumers = item.consumers.slice(0, 1); }
      if (item.splitType === 'PERCENTAGE') { evenPercentages(item); }
      renderItems(); refresh();
    } }, kinds.map(function (k) { return h('option', { value: k[0], selected: item.splitType === k[0] }, [k[1]]); }));

    var who = h('div', { class: 'chips' }, people().map(function (key) {
      var on = item.consumers.indexOf(key) >= 0;
      return chip(key, nameOf(key), on, false, function () {
        if (item.splitType === 'PERSONAL') { item.consumers = [key]; }
        else if (on) { item.consumers = item.consumers.filter(function (k) { return k !== key; }); delete item.pct[key]; }
        else { item.consumers.push(key); }
        if (item.splitType === 'PERCENTAGE') { evenPercentages(item); }
        renderItems(); refresh();
      });
    }));

    var parts = [
      h('div', { class: 'wiz-item-top' }, [
        h('input', { type: 'text', class: 'grow', placeholder: 'Item, e.g. Burger', value: item.name, maxlength: '120',
          'aria-label': 'Item name', oninput: function (e) { item.name = e.target.value; refresh(); } }),
        h('input', { type: 'text', inputmode: 'decimal', class: 'amount', placeholder: '0.00', value: item.amount,
          'aria-label': 'Item amount', oninput: function (e) { item.amount = e.target.value; refresh(); } }),
        h('button', { type: 'button', class: 'icon-x', 'aria-label': 'Remove item', onclick: function () {
          state.items.splice(idx, 1); renderItems(); refresh(); } }, ['×'])
      ]),
      h('div', { class: 'wiz-item-mid' }, [select]),
      h('div', { class: 'help' }, [item.splitType === 'PERSONAL' ? 'Who had it?' : 'Who shared it?']),
      who
    ];

    if (item.splitType === 'PERCENTAGE' && item.consumers.length) {
      var total = item.consumers.reduce(function (s, k) { var v = num(item.pct[k]); return s + (isNaN(v) ? 0 : v); }, 0);
      var rows = item.consumers.map(function (key) {
        return h('label', { class: 'wiz-pct' }, [
          h('span', {}, [nameOf(key)]),
          h('input', { type: 'text', inputmode: 'decimal', value: item.pct[key] || '', 'aria-label': 'Percentage for ' + nameOf(key),
            oninput: function (e) { item.pct[key] = e.target.value; refresh(); } }),
          h('span', {}, ['%'])
        ]);
      });
      parts.push(h('div', { class: 'wiz-pcts' }, rows.concat([
        h('div', { class: 'help' }, ['Total ' + round2(total) + '% (must be 100) ']),
        h('button', { type: 'button', class: 'btn sm line', onclick: function () { evenPercentages(item); renderItems(); refresh(); } }, ['Make even'])
      ])));
    }
    return h('div', { class: 'wiz-item' }, parts);
  }

  function renderPaid() {
    var box = $('wiz-paid');
    box.textContent = '';
    people().forEach(function (key) {
      box.appendChild(h('label', { class: 'wiz-paid-row' }, [
        h('span', { class: 'grow' }, [nameOf(key)]),
        h('span', {}, ['৳']),
        h('input', { type: 'text', inputmode: 'decimal', placeholder: '0.00', value: state.paid[key] || '',
          'aria-label': 'Paid by ' + nameOf(key), oninput: function (e) { state.paid[key] = e.target.value; refresh(); } })
      ]));
    });
  }

  // ---------- request body ----------

  function payload(forPreview) {
    var keys = people();
    var items = [];
    state.items.forEach(function (it) {
      var blank = !it.name.trim() && !String(it.amount).trim();
      if (blank || (forPreview && !it.name.trim())) { return; }
      var amount = num(it.amount);
      items.push({
        name: it.name.trim(),
        amount: isNaN(amount) ? 0 : round2(amount),
        splitType: it.splitType,
        shares: it.consumers.filter(function (k) { return keys.indexOf(k) >= 0; }).map(function (k) {
          var p = num(it.pct[k]);
          return { participant: ref(k), percentage: it.splitType === 'PERCENTAGE' && !isNaN(p) ? p : null };
        })
      });
    });
    var contributions = [];
    keys.forEach(function (k) {
      var a = num(state.paid[k]);
      if (!isNaN(a) && a > 0) { contributions.push({ participant: ref(k), amount: round2(a) }); }
    });
    return { participants: keys.map(ref), items: items, contributions: contributions };
  }

  // ---------- live totals and preview ----------

  var previewTimer = null;
  var previewSeq = 0;

  function refresh() {
    var bill = state.items.reduce(function (s, it) { var a = num(it.amount); return s + (isNaN(a) ? 0 : a); }, 0);
    var paid = people().reduce(function (s, k) { var a = num(state.paid[k]); return s + (isNaN(a) ? 0 : a); }, 0);
    var diff = round2(paid - bill);
    $('wiz-bill').textContent = money(bill);
    $('wiz-paidtotal').textContent = money(paid);
    var d = $('wiz-diff');
    d.textContent = diff === 0 ? money(0) : (diff < 0 ? 'short ' + money(-diff) : 'over ' + money(diff));
    d.className = diff === 0 && bill > 0 ? 'get' : (bill > 0 ? 'owe' : '');
    clearTimeout(previewTimer);
    previewTimer = setTimeout(runPreview, 350);
  }

  function runPreview() {
    var seq = ++previewSeq;
    api('POST', '/api/outings/preview', payload(true)).then(function (res) {
      if (seq === previewSeq) { renderPreview(res); }
    }).catch(function (e) {
      if (seq === previewSeq) { $('wiz-preview').textContent = ''; $('wiz-preview').appendChild(h('p', { class: 'help' }, ['Preview unavailable: ' + e.message])); }
    });
  }

  function renderPreview(res) {
    var box = $('wiz-preview');
    box.textContent = '';
    if (!res.valid) {
      box.appendChild(h('p', { class: 'help' }, ['Still to do:']));
      box.appendChild(h('ul', { class: 'wiz-todo' }, res.issues.map(function (i) { return h('li', {}, [i.message]); })));
      return;
    }
    var keyOf = function (b) { return b.userId != null ? 'u' + b.userId : 'g' + b.guestId; };
    res.balances.forEach(function (b) {
      var net = Number(b.net);
      var amount = net > 0 ? h('span', { class: 'get' }, ['+' + money(net)])
        : net < 0 ? h('span', { class: 'owe' }, ['−' + money(-net)]) : h('span', {}, [money(0)]);
      box.appendChild(h('div', { class: 'row' }, [
        h('div', { class: 'grow' }, [h('b', {}, [nameOf(keyOf(b))]),
          h('small', {}, ['ate ' + money(Number(b.consumed)) + ' · paid ' + money(Number(b.paid))])]),
        h('div', { class: 'amt' }, [amount])
      ]));
    });
    box.appendChild(h('p', { class: 'help' }, ['Everything adds up. You can save and split.']));
  }

  // ---------- saving ----------

  function showErrors(e) {
    var box = $('wiz-errors');
    box.textContent = '';
    box.hidden = false;
    var p = e.problem || {};
    box.appendChild(h('b', {}, [e.message || 'Something went wrong']));
    var list = [];
    if (p.errors) { Object.keys(p.errors).forEach(function (f) { list.push(f + ': ' + p.errors[f]); }); }
    if (p.issues) { p.issues.forEach(function (i) { list.push(i.message); }); }
    if (list.length) { box.appendChild(h('ul', {}, list.map(function (m) { return h('li', {}, [m]); }))); }
    box.scrollIntoView({ behavior: 'smooth', block: 'start' });
  }

  function save(submit) {
    $('wiz-errors').hidden = true;
    var buttons = [$('wiz-save'), $('wiz-submit')];
    buttons.forEach(function (b) { b.disabled = true; });
    var title = state.title.trim();
    var chain = Promise.resolve(outingId);
    if (!outingId) {
      chain = api('POST', '/api/outings', { title: title, category: state.category, date: state.date }).then(function (o) {
        outingId = o.id; return o.id;
      });
    }
    chain.then(function (id) { return api('PUT', '/api/outings/' + id, payload(false)); })
      .then(function () { return submit ? api('POST', '/api/outings/' + outingId + '/submit') : null; })
      .then(function () { window.location.href = '/outings/' + outingId; })
      .catch(function (e) { showErrors(e); buttons.forEach(function (b) { b.disabled = false; }); });
  }

  // ---------- loading ----------

  function loadExisting() {
    if (!outingId) { return Promise.resolve(); }
    return api('GET', '/api/outings/' + outingId).then(function (o) {
      state.title = o.title; state.category = o.category; state.date = o.date;
      var keyById = {};
      state.selected = [];
      o.participants.forEach(function (p) {
        var key = p.userId != null ? 'u' + p.userId : 'g' + p.guestId;
        keyById[p.id] = key;
        state.selected.push(key);
        if (p.guestId != null && !state.guests.some(function (g) { return g.key === key; })) {
          state.guests.push({ key: key, name: p.guestName });
        }
      });
      if (state.selected.indexOf(ME) < 0) { state.selected.unshift(ME); }
      state.items = o.items.map(function (it) {
        var item = { name: it.name, amount: String(it.amount), splitType: it.splitType, consumers: [], pct: {} };
        it.shares.forEach(function (s) {
          var key = keyById[s.participantId];
          item.consumers.push(key);
          if (s.percentage != null) { item.pct[key] = String(Number(s.percentage)); }
        });
        return item;
      });
      o.contributions.forEach(function (c) { state.paid[keyById[c.participantId]] = String(c.amountPaid); });
    });
  }

  function mergeFriendsAndGuests(friends, guests) {
    state.friends = friends.map(function (f) { return { key: 'u' + f.id, name: f.displayName }; });
    guests.forEach(function (g) {
      var key = 'g' + g.id;
      var existing = state.guests.filter(function (x) { return x.key === key; })[0];
      if (existing) { existing.name = g.name; } else { state.guests.push({ key: key, name: g.name }); }
    });
    state.guests.sort(function (a, b) { return a.name.localeCompare(b.name); });
  }

  function addGuest() {
    var input = $('wiz-guest-name');
    var name = input.value.trim();
    if (!name) { return; }
    $('wiz-errors').hidden = true;
    api('POST', '/api/guests', { name: name }).then(function (g) {
      input.value = '';
      mergeFriendsAndGuests(state.friends.map(function (f) { return { id: Number(f.key.slice(1)), displayName: f.name }; }), [g]);
      addPerson('g' + g.id);
      renderPeople(); renderItems(); renderPaid(); refresh();
    }).catch(showErrors);
  }

  // ---------- start ----------

  function bind() {
    $('wiz-title').addEventListener('input', function (e) { state.title = e.target.value; });
    $('wiz-category').addEventListener('change', function (e) { state.category = e.target.value; });
    $('wiz-date').addEventListener('change', function (e) { state.date = e.target.value; });
    $('wiz-add-item').addEventListener('click', function () { state.items.push(newItem()); renderItems(); refresh(); });
    $('wiz-add-guest').addEventListener('click', addGuest);
    $('wiz-guest-name').addEventListener('keydown', function (e) { if (e.key === 'Enter') { e.preventDefault(); addGuest(); } });
    $('wiz-save').addEventListener('click', function () { save(false); });
    $('wiz-submit').addEventListener('click', function () { save(true); });
  }

  Promise.all([api('GET', '/api/friends'), api('GET', '/api/guests')])
    .then(function (res) { mergeFriendsAndGuests(res[0], res[1]); return loadExisting(); })
    .then(function () {
      $('wiz-title').value = state.title;
      $('wiz-category').value = state.category;
      $('wiz-date').value = state.date;
      if (!state.items.length && !outingId) { state.items.push(newItem()); }
      bind(); renderPeople(); renderItems(); renderPaid(); refresh();
    })
    .catch(function (e) { bind(); showErrors(e); });
})();
