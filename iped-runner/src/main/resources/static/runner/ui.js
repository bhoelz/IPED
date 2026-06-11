// Shared UI atoms for IPED Runner
const { useState, useRef } = React;

function Pill({ children }) {
  return <span className="pill">{children}</span>;
}

function Card({ title, flag, desc, icon, action, bar, children, style }) {
  return (
    <section className="card" style={style}>
      {bar ? (
        <div className="card-head with-bar">
          {icon && <span className="card-icon">{icon}</span>}
          <h3 className="card-title">{title}</h3>
          {flag && <Pill>{flag}</Pill>}
          {action && <div className="card-action">{action}</div>}
        </div>
      ) : (title || desc) ? (
        <div className="card-head">
          <div style={{ flex: 1, minWidth: 0 }}>
            <div className="card-titleline">
              <h3 className="card-title">{title}</h3>
              {flag && <Pill>{flag}</Pill>}
            </div>
            {desc && <p className="card-desc">{desc}</p>}
          </div>
          {action && <div className="card-action">{action}</div>}
        </div>
      ) : null}
      <div className="card-body">{children}</div>
    </section>
  );
}

function Field({ label, flag, required, error, hint, children }) {
  return (
    <div className="field">
      {(label || flag) && (
        <label className="field-label">
          {label}
          {required && <span className="field-req" title="required">*</span>}
          {flag && <Pill>{flag}</Pill>}
        </label>
      )}
      {children}
      {error && <span className="hinttext err"><Icon name="alert" size={13} /> {error}</span>}
      {!error && hint && <span className={`hinttext ${hint.kind || ''}`}>{hint.icon && <Icon name={hint.icon} size={13} />} {hint.text}</span>}
    </div>
  );
}

function TextInput({ value, onChange, placeholder, affix, onAffix, invalid, mono = true, type = 'text' }) {
  const cls = `inp ${mono ? 'tech' : ''} ${affix ? 'has-affix' : ''} ${invalid ? 'invalid' : ''}`;
  if (!affix) {
    return <input className={cls} type={type} value={value} placeholder={placeholder}
      onChange={(e) => onChange(e.target.value)} />;
  }
  return (
    <div className={`inp-wrap ${invalid ? 'invalid' : ''}`}>
      <input className={cls} type={type} value={value} placeholder={placeholder}
        onChange={(e) => onChange(e.target.value)} />
      <button className="inp-affix" type="button" onClick={onAffix} title="Browse">{affix}</button>
    </div>
  );
}

function Select({ value, onChange, options }) {
  return (
    <div className="sel-wrap">
      <select className="sel tech" value={value} onChange={(e) => onChange(e.target.value)}>
        {options.map((o) => (
          <option key={o.value} value={o.value} disabled={o.disabled}>{o.label}</option>
        ))}
      </select>
      <span className="sel-chev"><Icon name="chevronDown" size={16} /></span>
    </div>
  );
}

function FilePicker({ fileName, onPick, onClear, buttonLabel, emptyLabel, accept, invalid }) {
  return (
    <div className={`filepick ${invalid ? 'invalid' : ''}`}>
      <button type="button" className="filepick-btn" onClick={onPick}>
        <Icon name="folder" size={15} /> {buttonLabel}
      </button>
      <span className={`filepick-name ${fileName ? 'set' : ''}`} title={fileName || ''}>
        {fileName || emptyLabel}
      </span>
      {fileName && (
        <button type="button" className="icon-btn danger" style={{ marginRight: 4 }} onClick={onClear} title="Clear">
          <Icon name="x" size={15} />
        </button>
      )}
    </div>
  );
}

function PasswordRow({ value, onChange, onRemove, placeholder }) {
  const [show, setShow] = useState(false);
  return (
    <div className="inp-wrap" style={{ alignItems: 'center', gap: 8 }}>
      <div style={{ position: 'relative', flex: 1, display: 'flex', alignItems: 'center' }}>
        <span style={{ position: 'absolute', left: 11, color: 'var(--text-muted)', display: 'inline-flex', pointerEvents: 'none' }}>
          <Icon name="key" size={15} />
        </span>
        <input className="inp tech" type={show ? 'text' : 'password'} value={value} placeholder={placeholder}
          style={{ paddingLeft: 34, paddingRight: 40 }} onChange={(e) => onChange(e.target.value)} />
        <button type="button" className="icon-btn" style={{ position: 'absolute', right: 2, width: 30, height: 30 }}
          onClick={() => setShow((s) => !s)} title={show ? 'Hide' : 'Show'}>
          <Icon name={show ? 'eyeOff' : 'eye'} size={16} />
        </button>
      </div>
      <button type="button" className="icon-btn danger" onClick={onRemove} title="Remove"><Icon name="trash" size={16} /></button>
    </div>
  );
}

function Switch({ on, onChange }) {
  return <button type="button" className={`switch ${on ? 'on' : ''}`} role="switch" aria-checked={on} onClick={() => onChange(!on)} />;
}

function Checkbox({ on, onChange }) {
  return (
    <button type="button" className={`cbx ${on ? 'on' : ''}`} role="checkbox" aria-checked={on} onClick={() => onChange(!on)}>
      {on && <Icon name="check" size={13} stroke={3} />}
    </button>
  );
}

function FlagSwitch({ name, flag, desc, on, onChange }) {
  return (
    <div className="flag-row">
      <div className="flag-main">
        <div className="flag-name">{name}</div>
        {flag && <div className="flag-flag">{flag}</div>}
        {desc && <div className="flag-sub">{desc}</div>}
      </div>
      <Switch on={on} onChange={onChange} />
    </div>
  );
}

function FlagCheck({ name, flag, desc, on, onChange }) {
  return (
    <div className="flag-row box" style={{ cursor: 'pointer' }} onClick={() => onChange(!on)}>
      <div style={{ paddingTop: 1 }}><Checkbox on={on} onChange={onChange} /></div>
      <div className="flag-main">
        <div className="flag-name">{name} {flag && <Pill>{flag}</Pill>}</div>
        {desc && <div className="flag-sub">{desc}</div>}
      </div>
    </div>
  );
}

function Segmented({ value, onChange, options }) {
  return (
    <div className="seg-track" role="tablist">
      {options.map((o) => (
        <button key={o.value} type="button" role="tab" aria-selected={value === o.value}
          className={`seg-opt ${value === o.value ? 'active' : ''}`} onClick={() => onChange(o.value)}>
          <span className="seg-label">{o.label}</span>
          {o.flag && <span className="seg-flag">{o.flag}</span>}
        </button>
      ))}
    </div>
  );
}

function TagsInput({ tags, onChange, placeholder }) {
  const [draft, setDraft] = useState('');
  const [focus, setFocus] = useState(false);
  const inputRef = useRef(null);
  const commit = () => {
    const v = draft.trim();
    if (v && !tags.includes(v)) onChange([...tags, v]);
    setDraft('');
  };
  const onKey = (e) => {
    if (e.key === 'Enter' || e.key === ',') { e.preventDefault(); commit(); }
    else if (e.key === 'Backspace' && !draft && tags.length) onChange(tags.slice(0, -1));
  };
  return (
    <div className={`tags ${focus ? 'focus' : ''}`} onClick={() => inputRef.current && inputRef.current.focus()}>
      {tags.map((t) => (
        <span className="tag" key={t}>
          {t}
          <button type="button" className="tag-x" onClick={(e) => { e.stopPropagation(); onChange(tags.filter((x) => x !== t)); }}>
            <Icon name="x" size={13} />
          </button>
        </span>
      ))}
      <input ref={inputRef} className="tags-input" value={draft} placeholder={tags.length ? '' : placeholder}
        onChange={(e) => setDraft(e.target.value)} onKeyDown={onKey} onBlur={() => { commit(); setFocus(false); }}
        onFocus={() => setFocus(true)} />
    </div>
  );
}

function NumberPresets({ value, onChange, presets, placeholder }) {
  return (
    <div className="num-presets">
      <input className="inp tech" type="text" value={value} placeholder={placeholder}
        onChange={(e) => onChange(e.target.value.replace(/[^0-9]/g, ''))} />
      {presets.map((p) => (
        <button key={p} type="button" className={`preset-btn ${String(value) === String(p) ? 'active' : ''}`}
          onClick={() => onChange(String(p))}>{p}</button>
      ))}
    </div>
  );
}

function KeyValueGrid({ rows, onChange, headK, headV, phK, phV }) {
  const update = (i, field, val) => {
    const next = rows.map((r, idx) => (idx === i ? { ...r, [field]: val } : r));
    onChange(next);
  };
  const remove = (i) => onChange(rows.filter((_, idx) => idx !== i));
  const display = [...rows];
  const last = display[display.length - 1];
  if (!last || last.k || last.v) display.push({ k: '', v: '', _new: true });

  return (
    <div className="kvgrid">
      <div className="kv-head"><div>{headK}</div><div>{headV}</div><div /></div>
      {display.map((r, i) => (
        <div className="kv-row" key={i}>
          <div className="kv-cell"><input className="kvinp k" value={r.k} placeholder={phK}
            onChange={(e) => {
              if (r._new) onChange([...rows, { k: e.target.value, v: '' }]);
              else update(i, 'k', e.target.value);
            }} /></div>
          <div className="kv-cell"><input className="kvinp" value={r.v} placeholder={phV}
            onChange={(e) => {
              if (r._new) onChange([...rows, { k: '', v: e.target.value }]);
              else update(i, 'v', e.target.value);
            }} /></div>
          <div className="kv-cell">
            {!r._new && <button type="button" className="icon-btn danger" onClick={() => remove(i)}><Icon name="trash" size={15} /></button>}
          </div>
        </div>
      ))}
    </div>
  );
}

function EmptyNote({ children }) {
  return <div className="empty-note">{children}</div>;
}

Object.assign(window, {
  Pill, Card, Field, TextInput, Select, FilePicker, PasswordRow, Switch, Checkbox,
  FlagSwitch, FlagCheck, Segmented, TagsInput, NumberPresets, KeyValueGrid, EmptyNote,
});
