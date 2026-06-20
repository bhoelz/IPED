import { useState, useEffect, useRef, useMemo } from 'react'
import { Icon } from './icons.jsx'
import { LANGS, STRINGS } from './i18n.js'
import { INITIAL_STATE, buildCommand, validateState } from './command.js'
import { ScreenEntradas, ScreenSaida, ScreenProcessamento, ScreenRelatorio, ScreenAvancado } from './screens.jsx'
import { useTweaks, TweaksPanel, TweakSection, TweakColor, TweakRadio, TweakToggle } from './tweaks-panel.jsx'
import { DashboardView } from './dashboard.jsx'
import { apiFetch, setUnauthorizedHandler, getApiKey, setApiKey } from './api.js'
import { ApiKeyModal } from './auth.jsx'

const TWEAK_DEFAULTS = {
  accent: '#58b6e8',
  techFont: 'mono',
  showFlags: true,
}

const CONFIG_NAV = [
  { key: 'entradas',      icon: 'login',    screen: ScreenEntradas },
  { key: 'saida',         icon: 'terminal', screen: ScreenSaida },
  { key: 'processamento', icon: 'cpu',      screen: ScreenProcessamento },
  { key: 'relatorio',     icon: 'fileText', screen: ScreenRelatorio },
  { key: 'avancado',      icon: 'settings', screen: ScreenAvancado },
]

function persistedConfig() {
  try { const raw = localStorage.getItem('iped.config'); if (raw) return JSON.parse(raw) } catch (e) {}
  return null
}

// ── Toasts ────────────────────────────────────────────────────────────────────

function Toasts({ items }) {
  return (
    <div className="toast-wrap">
      {items.map((to) => (
        <div className="toast" key={to.id}>
          <span className="ico"><Icon name="check" size={16} stroke={2.4} /></span>{to.msg}
        </div>
      ))}
    </div>
  )
}

// ── Command bar ───────────────────────────────────────────────────────────────

function CommandBar({ cmd, issues, t }) {
  const [open, setOpen] = useState(false)
  const [copied, setCopied] = useState(false)
  const errors = issues.filter((i) => i.level === 'error')
  const copy = () => {
    navigator.clipboard && navigator.clipboard.writeText(cmd.str).catch(() => {})
    const ta = document.createElement('textarea'); ta.value = cmd.str; document.body.appendChild(ta); ta.select()
    try { document.execCommand('copy') } catch (e) {} document.body.removeChild(ta)
    setCopied(true); setTimeout(() => setCopied(false), 1600)
  }
  return (
    <div className="cmdbar">
      {open && (
        <div className="cmd-panel">
          <div className="cmd-code">
            <span className="fl">.\start-iped-app.ps1</span>{'\n'}
            {cmd.tokens.map((tk, i) => (
              <span key={i}>{'  '}<span className="fl">{tk.flag}</span>{tk.val !== null ? <span className="vl"> {tk.val}</span> : null}<span className="cont"> \</span>{'\n'}</span>
            ))}
          </div>
        </div>
      )}
      <div className="cmd-line">
        <span className="cmd-prompt">$ .\start-iped-app.ps1</span>
        <div className="cmd-str tech">{cmd.tokens.length === 0 ? <span style={{ opacity: .6 }}>{t.cmd.empty}</span> : cmd.str.replace(/^\.\\start-iped-app\.ps1 /, '')}</div>
        {errors.length > 0
          ? <span className="cmd-status bad"><Icon name="alert" size={13} /> {t.cmd.issues(errors.length)}</span>
          : <span className="cmd-status ok"><Icon name="check" size={13} /> {t.cmd.valid}</span>}
        <button className="btn btn-ghost btn-sm" onClick={copy}>
          <Icon name={copied ? 'check' : 'copy'} size={14} /> {copied ? t.cmd.copied : t.cmd.copy}
        </button>
        <button className="icon-btn cmd-expand" onClick={() => setOpen((o) => !o)} title={open ? t.cmd.collapse : t.cmd.expand}
          style={{ transform: open ? 'rotate(180deg)' : 'none' }}>
          <Icon name="chevronDown" size={16} />
        </button>
      </div>
    </div>
  )
}

// ── File browser modal ────────────────────────────────────────────────────────

function FileBrowserModal({ dir: initialDir, accept, onSelect, onClose, t }) {
  const [dir, setDir] = useState(initialDir || '')
  const [parent, setParent] = useState(null)
  const [entries, setEntries] = useState([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)

  const load = async (d) => {
    setLoading(true); setError(null)
    try {
      const r = await fetch('/browse?dir=' + encodeURIComponent(d || ''))
      if (!r.ok) throw new Error('HTTP ' + r.status)
      const data = await r.json()
      setDir(data.path || '')
      setParent(data.parent)
      const show = accept === 'dir' ? data.entries.filter(e => e.type === 'dir') : data.entries
      setEntries(show)
    } catch (e) {
      setError(e.message)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { load(initialDir || '') }, [])

  const pick = (entry) => {
    if (entry.type === 'dir') return load(entry.path)
    onSelect(entry.path)
    onClose()
  }

  return (
    <div className="overlay" style={{ zIndex: 120 }} onClick={onClose}>
      <div className="modal" onClick={(e) => e.stopPropagation()} style={{ maxWidth: 560 }}>
        <div className="modal-head">
          <span style={{ color: 'var(--accent)' }}><Icon name="folder" size={22} /></span>
          <div style={{ flex: 1, minWidth: 0 }}>
            <div className="modal-title">{t.browse.title}</div>
            <div className="tech" style={{ fontSize: 11.5, color: 'var(--text-muted)', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
              {dir || '/'}
            </div>
          </div>
          <button className="icon-btn" onClick={onClose}><Icon name="x" size={18} /></button>
        </div>

        <div className="modal-body" style={{ padding: '6px 8px', maxHeight: 380, overflowY: 'auto' }}>
          {parent !== null && (
            <button className="browser-entry" onClick={() => load(parent)}>
              <Icon name="chevronLeft" size={14} style={{ color: 'var(--text-muted)' }} />
              <span style={{ color: 'var(--text-muted)', fontFamily: 'var(--font-mono)', fontSize: 13 }}>..</span>
            </button>
          )}
          {loading && (
            <div style={{ padding: '22px 12px', color: 'var(--text-muted)', fontSize: 13, display: 'flex', alignItems: 'center', gap: 10 }}>
              <span className="spinner" style={{ width: 16, height: 16 }} /> Loading…
            </div>
          )}
          {error && <div className="hinttext err" style={{ padding: 12 }}>{error}</div>}
          {!loading && entries.map((e) => (
            <button key={e.path} className="browser-entry" onClick={() => pick(e)}>
              <Icon name={e.type === 'dir' ? 'folder' : 'fileText'} size={15}
                style={{ color: e.type === 'dir' ? 'var(--accent)' : 'var(--text-muted)', flexShrink: 0 }} />
              <span className="tech" style={{ fontSize: 12.5, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{e.name}</span>
            </button>
          ))}
          {!loading && !error && entries.length === 0 && !parent && (
            <div style={{ padding: '22px 12px', color: 'var(--text-muted)', fontSize: 13, textAlign: 'center' }}>Empty</div>
          )}
        </div>

        <div className="modal-foot">
          <button className="btn btn-ghost" onClick={onClose}>{t.run.cancel}</button>
          {accept === 'dir' && dir && (
            <button className="btn btn-primary" onClick={() => { onSelect(dir); onClose() }}>
              <Icon name="folder" size={14} /> {t.browse.useFolder}
            </button>
          )}
        </div>
      </div>
    </div>
  )
}

// ── Run modal ─────────────────────────────────────────────────────────────────

const PRIORITIES = ['HIGH', 'NORMAL', 'LOW']

function RunModal({ open, onClose, cmd, issues, s, t, lang, serverProfiles }) {
  const [phase, setPhase] = useState('review')
  const [logs, setLogs] = useState([])
  const [exitCode, setExitCode] = useState(null)
  const [priority, setPriority] = useState('NORMAL')
  const runIdRef = useRef(null)
  const esRef = useRef(null)
  const logEndRef = useRef(null)
  const errors = issues.filter((i) => i.level === 'error')
  const warns  = issues.filter((i) => i.level === 'warn')

  useEffect(() => {
    if (open) { setPhase('review'); setLogs([]); setExitCode(null); setPriority('NORMAL'); runIdRef.current = null }
    return () => { if (esRef.current) { esRef.current.close(); esRef.current = null } }
  }, [open])

  useEffect(() => {
    if (logEndRef.current) logEndRef.current.scrollIntoView({ behavior: 'auto' })
  }, [logs])

  const start = async () => {
    setPhase('running'); setLogs([]); setExitCode(null)
    const rawTokens = cmd.tokens.map(tk => ({
      flag: tk.flag,
      val: tk.val != null
        ? (tk.val.startsWith('"') && tk.val.endsWith('"') ? tk.val.slice(1, -1) : tk.val)
        : null,
    }))
    try {
      const resp = await apiFetch('/run', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ tokens: rawTokens, priority }),
      })
      if (!resp.ok) throw new Error('Server responded ' + resp.status)
      const body = await resp.json()
      if (!body.id) throw new Error(body.error || 'No run ID returned')
      runIdRef.current = body.id

      const es = new EventSource('/run/' + body.id + '/stream')
      esRef.current = es

      es.addEventListener('log', (e) => {
        const { line } = JSON.parse(e.data)
        setLogs(prev => [...prev.slice(-199), line])
      })

      const finish = (nextPhase, code) => {
        es.close(); esRef.current = null
        setExitCode(code ?? null)
        setPhase(nextPhase)
      }
      es.addEventListener('done',    (e) => finish('done',  JSON.parse(e.data).exitCode))
      es.addEventListener('error',   (e) => finish('error', JSON.parse(e.data).exitCode))
      es.addEventListener('aborted', ()  => finish('error', null))

      es.onerror = () => {
        if (esRef.current) { esRef.current.close(); esRef.current = null }
        setPhase(prev => prev === 'running' ? 'error' : prev)
      }
    } catch (err) {
      setLogs(['Error starting process: ' + err.message])
      setPhase('error')
    }
  }

  const abort = async () => {
    if (esRef.current) { esRef.current.close(); esRef.current = null }
    if (runIdRef.current) {
      try { await apiFetch('/run/' + runIdRef.current, { method: 'DELETE' }) } catch (e) {}
      runIdRef.current = null
    }
    setPhase('error'); setExitCode(null)
  }

  if (!open) return null

  const checkItems = []
  if (errors.length === 0) checkItems.push({ level: 'ok', text: t.cmd.valid })
  errors.forEach((e) => checkItems.push({ level: 'error', text: t.validation[e.key] }))
  warns.forEach((w)  => checkItems.push({ level: 'warn',  text: t.validation[w.key] }))

  const isDone  = phase === 'done'
  const isError = phase === 'error'
  const td = t.dashboard

  return (
    <div className="overlay" onClick={phase === 'running' ? undefined : onClose}>
      <div className="modal" onClick={(e) => e.stopPropagation()}>
        <div className="modal-head">
          <span style={{ color: isDone ? 'var(--ok)' : isError ? 'var(--danger)' : 'var(--accent)' }}>
            <Icon name={isDone ? 'check' : isError ? 'alert' : 'play'} size={22} />
          </span>
          <div style={{ flex: 1 }}>
            <div className="modal-title">
              {isDone ? t.run.done : isError ? (exitCode === null ? t.run.aborted : t.run.failed) : phase === 'running' ? t.run.running : t.run.title}
            </div>
            {phase === 'review' && <div className="modal-sub">{t.run.subtitle}</div>}
          </div>
          {phase !== 'running' && <button className="icon-btn" onClick={onClose}><Icon name="x" size={18} /></button>}
        </div>

        <div className="modal-body">
          {phase === 'review' && (
            <>
              <div style={{ fontSize: 12, fontWeight: 700, letterSpacing: .6, textTransform: 'uppercase', color: 'var(--text-muted)', marginBottom: 10 }}>{t.run.checks}</div>
              <div style={{ display: 'flex', flexDirection: 'column', gap: 6, marginBottom: 20 }}>
                {checkItems.map((c, i) => (
                  <div className={`check-item ${c.level}`} key={i}>
                    <span className={`check-ico ${c.level}`} style={{ display: 'inline-flex', marginTop: 1 }}>
                      <Icon name={c.level === 'ok' ? 'check' : 'alert'} size={15} stroke={2.2} />
                    </span>
                    {c.text}
                  </div>
                ))}
              </div>

              {/* Priority selector */}
              <div style={{ fontSize: 12, fontWeight: 700, letterSpacing: .6, textTransform: 'uppercase', color: 'var(--text-muted)', marginBottom: 10 }}>
                {td.priority.title}
              </div>
              <div className="priority-select" style={{ marginBottom: 20 }}>
                {PRIORITIES.map((p) => (
                  <button
                    key={p}
                    type="button"
                    className={`priority-opt ${priority === p ? 'active ' + p : ''}`}
                    onClick={() => setPriority(p)}
                  >
                    {p === 'HIGH' && <Icon name="arrowUp" size={12} />}
                    {p === 'LOW'  && <Icon name="arrowDown" size={12} />}
                    {p === 'HIGH' ? td.priority.high : p === 'LOW' ? td.priority.low : td.priority.normal}
                  </button>
                ))}
              </div>

              <div style={{ fontSize: 12, fontWeight: 700, letterSpacing: .6, textTransform: 'uppercase', color: 'var(--text-muted)', marginBottom: 10 }}>{t.run.command}</div>
              <div className="cmd-code" style={{ background: '#06090f', border: '1px solid var(--border)', borderRadius: 10, padding: 14 }}>
                <span className="fl">.\start-iped-app.ps1</span>{cmd.tokens.map((tk, i) => (
                  <span key={i}> <span className="fl">{tk.flag}</span>{tk.val !== null ? <span className="vl"> {tk.val}</span> : null}</span>
                ))}
              </div>
            </>
          )}

          {phase === 'running' && (
            <>
              <div style={{ marginBottom: 12 }}>
                <div className="progress-track">
                  <div className="progress-fill-pulse" />
                </div>
              </div>
              <div className="log-terminal">
                {logs.length === 0
                  ? <span style={{ opacity: .5 }}>Waiting for output…</span>
                  : logs.map((line, i) => <div className="log-line" key={i}>{line}</div>)
                }
                <div ref={logEndRef} />
              </div>
            </>
          )}

          {(isDone || isError) && (
            <>
              <div style={{ textAlign: 'center', padding: '14px 0 6px' }}>
                <div style={{
                  width: 64, height: 64, borderRadius: '50%',
                  background: isDone ? 'color-mix(in srgb, var(--ok) 16%, transparent)' : 'color-mix(in srgb, var(--danger) 16%, transparent)',
                  color: isDone ? 'var(--ok)' : 'var(--danger)',
                  display: 'inline-flex', alignItems: 'center', justifyContent: 'center', marginBottom: 14,
                }}>
                  <Icon name={isDone ? 'check' : 'alert'} size={34} stroke={2.5} />
                </div>
                <div style={{ color: 'var(--text)', fontSize: 16, fontWeight: 600 }}>
                  {isDone ? t.run.done : exitCode === null ? t.run.aborted : t.run.failed}
                </div>
                {exitCode != null && (
                  <div className="tech" style={{ color: 'var(--text-muted)', fontSize: 12.5, marginTop: 6 }}>
                    {t.run.exitCode}: {exitCode}
                  </div>
                )}
                {isDone && (
                  <div className="tech" style={{ color: 'var(--text-muted)', fontSize: 12.5, marginTop: 8 }}>
                    {s.output || '?'} · {s.datasources.filter((d) => d.path.trim()).length} {lang === 'en' ? 'sources' : lang === 'es' ? 'fuentes' : 'fontes'} · profile={s.profile}
                  </div>
                )}
              </div>
              {logs.length > 0 && (
                <div className="log-terminal" style={{ marginTop: 16, maxHeight: 160 }}>
                  {logs.slice(-40).map((line, i) => <div className="log-line" key={i}>{line}</div>)}
                </div>
              )}
            </>
          )}
        </div>

        <div className="modal-foot">
          {phase === 'review' && (
            <>
              <button className="btn btn-ghost" onClick={onClose}>{t.run.cancel}</button>
              <button className="btn btn-primary" disabled={errors.length > 0} onClick={start}>
                <Icon name="play" size={15} /> {errors.length > 0 ? t.run.fixFirst : t.run.start}
              </button>
            </>
          )}
          {phase === 'running' && (
            <button className="btn btn-ghost" onClick={abort}>
              <Icon name="x" size={15} /> {t.run.abort}
            </button>
          )}
          {(isDone || isError) && (
            <button className="btn btn-primary" onClick={onClose}>{t.run.close}</button>
          )}
        </div>
      </div>
    </div>
  )
}

// ── Session menu ──────────────────────────────────────────────────────────────

function SessionMenu({ hasKey, onChangeKey, onClearKey, t }) {
  const [open, setOpen] = useState(false)
  const tk = t.keyMenu
  useEffect(() => {
    if (!open) return
    const h = () => setOpen(false)
    window.addEventListener('click', h)
    return () => window.removeEventListener('click', h)
  }, [open])
  return (
    <div className="lang" onClick={(e) => e.stopPropagation()} style={{ position: 'relative' }}>
      <button className="top-icon" style={{ position: 'relative' }} onClick={() => setOpen((o) => !o)}>
        <Icon name="user" size={19} />
        <span className={`key-dot${hasKey ? '' : ' none'}`} />
      </button>
      {open && (
        <div className="lang-menu" style={{ right: 0, left: 'auto', minWidth: 180 }}>
          <div style={{ padding: '8px 14px 6px', fontSize: 11, fontWeight: 700, color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: .5 }}>
            {tk.title}
          </div>
          <div style={{ padding: '4px 14px 8px', fontSize: 12.5, color: hasKey ? 'var(--ok)' : 'var(--text-muted)', display: 'flex', alignItems: 'center', gap: 6 }}>
            <span className={`key-dot${hasKey ? '' : ' none'}`} style={{ position: 'static', flexShrink: 0 }} />
            {hasKey ? tk.keySet : tk.keyNone}
          </div>
          <div style={{ height: 1, background: 'var(--border)', margin: '0 8px 6px' }} />
          <button className="lang-item" onClick={() => { setOpen(false); onChangeKey() }}>
            <Icon name="lock" size={14} /> {tk.change}
          </button>
          {hasKey && (
            <button className="lang-item" style={{ color: 'var(--danger)' }} onClick={() => { setOpen(false); onClearKey() }}>
              <Icon name="unlock" size={14} /> {tk.clear}
            </button>
          )}
        </div>
      )}
    </div>
  )
}

// ── Language menu ─────────────────────────────────────────────────────────────

function LangMenu({ lang, onChange }) {
  const [open, setOpen] = useState(false)
  const cur = LANGS.find((l) => l.code === lang)
  useEffect(() => {
    if (!open) return
    const h = () => setOpen(false)
    window.addEventListener('click', h)
    return () => window.removeEventListener('click', h)
  }, [open])
  return (
    <div className="lang" onClick={(e) => e.stopPropagation()}>
      <button className="lang-btn" onClick={() => setOpen((o) => !o)}>
        <Icon name="globe" size={15} /> {cur.flag} <Icon name="chevronDown" size={13} />
      </button>
      {open && (
        <div className="lang-menu">
          {LANGS.map((l) => (
            <button key={l.code} className={`lang-item ${l.code === lang ? 'active' : ''}`} onClick={() => { onChange(l.code); setOpen(false) }}>
              <span className="lang-code">{l.flag}</span> {l.label}
              {l.code === lang && <span style={{ marginLeft: 'auto', color: 'var(--accent-text)' }}><Icon name="check" size={15} /></span>}
            </button>
          ))}
        </div>
      )}
    </div>
  )
}

// ── App root ──────────────────────────────────────────────────────────────────

export default function App() {
  const [tw, setTweak] = useTweaks(TWEAK_DEFAULTS)
  const [lang, setLang] = useState(() => localStorage.getItem('iped.lang') || 'pt')
  const [view, setView] = useState('assets')          // 'dashboard' | 'assets'
  const [sidebarCollapsed, setSidebarCollapsed] = useState(() => localStorage.getItem('iped.sidebarCollapsed') === '1')
  const [active, setActive] = useState('entradas')    // config sub-screen
  const [s, setS] = useState(() => ({ ...INITIAL_STATE, ...(persistedConfig() || {}) }))
  const [runOpen, setRunOpen] = useState(false)
  const [toasts, setToasts] = useState([])
  const [browser, setBrowser] = useState(null)
  const [serverProfiles, setServerProfiles] = useState([])
  const [authNeeded, setAuthNeeded] = useState(false)
  const [hasKey,     setHasKey]     = useState(() => !!getApiKey())
  const toastId = useRef(0)

  const t = STRINGS[lang]

  // Accent / font tweaks
  useEffect(() => {
    const root = document.documentElement
    root.style.setProperty('--accent', tw.accent)
    root.style.setProperty('--tech-font', tw.techFont === 'sans' ? 'var(--font-sans)' : 'var(--font-mono)')
    document.body.classList.toggle('hide-flags', !tw.showFlags)
  }, [tw.accent, tw.techFont, tw.showFlags])

  useEffect(() => { localStorage.setItem('iped.lang', lang); document.documentElement.lang = lang === 'en' ? 'en' : lang === 'es' ? 'es' : 'pt-BR' }, [lang])
  useEffect(() => { localStorage.setItem('iped.sidebarCollapsed', sidebarCollapsed ? '1' : '0') }, [sidebarCollapsed])
  useEffect(() => { try { localStorage.setItem('iped.config', JSON.stringify(s)) } catch (e) {} }, [s])

  // Register global 401 handler — shows ApiKeyModal whenever a protected request is rejected
  useEffect(() => {
    setUnauthorizedHandler(() => setAuthNeeded(true))
    return () => setUnauthorizedHandler(null)
  }, [])

  // Fetch server profile list
  useEffect(() => {
    apiFetch('/profiles').then(r => r.ok ? r.json() : []).then(list => setServerProfiles(list || [])).catch(() => {})
  }, [])

  const patch  = (partial) => setS((prev) => ({ ...prev, ...partial }))
  const notify = (msg) => {
    const id = ++toastId.current
    setToasts((p) => [...p, { id, msg }])
    setTimeout(() => setToasts((p) => p.filter((x) => x.id !== id)), 2600)
  }

  const openBrowser = (initialDir, accept, onSelect) => setBrowser({ dir: initialDir || '', accept: accept || 'any', onSelect })
  const closeBrowser = () => setBrowser(null)

  const issues = useMemo(() => validateState(s), [s])
  const cmd = useMemo(() => buildCommand(s), [s])
  const errCountByScreen = useMemo(() => {
    const m = {}
    issues.filter((i) => i.level === 'error').forEach((i) => { m[i.screen] = (m[i.screen] || 0) + 1 })
    return m
  }, [issues])

  const getIssue = (key) => issues.find((i) => i.key === key) || null

  const ActiveScreen = CONFIG_NAV.find((n) => n.key === active).screen

  const switchView = (v) => {
    setView(v)
    setRunOpen(false)
  }

  return (
    <div className={`app${sidebarCollapsed ? ' sidebar-collapsed' : ''}`}>
      <aside className="sidebar">
        <div className="brand">
          <div className="brand-icon"><Icon name="shield" size={20} /></div>
          <div className="brand-text">
            <div className="brand-name">{t.appName}</div>
            <div className="brand-sub">{t.appSub}</div>
          </div>
        </div>

        {view === 'assets' && (
          <nav className="nav">
            {CONFIG_NAV.map((n) => (
              <button key={n.key} className={`navitem ${active === n.key ? 'active' : ''}`} onClick={() => setActive(n.key)} title={sidebarCollapsed ? t.nav[n.key] : undefined}>
                <span className="nav-ico"><Icon name={n.icon} size={18} /></span>
                <span className="nav-label">{t.nav[n.key]}</span>
                {errCountByScreen[n.key] ? <span className="nav-badge">{errCountByScreen[n.key]}</span> : null}
              </button>
            ))}
          </nav>
        )}

        {view === 'dashboard' && (
          <nav className="nav">
            <button className="navitem active" title={sidebarCollapsed ? t.topnav.dashboard : undefined}>
              <span className="nav-ico"><Icon name="activity" size={18} /></span>
              <span className="nav-label">{t.topnav.dashboard}</span>
            </button>
          </nav>
        )}

        <div className="sidebar-foot">
          <button className="navitem" title={sidebarCollapsed ? t.support : undefined}>
            <span className="nav-ico"><Icon name="helpCircle" size={18} /></span>
            <span className="nav-label">{t.support}</span>
          </button>
          <button className="navitem" onClick={() => setSidebarCollapsed((c) => !c)}
            title={sidebarCollapsed ? t.expandSidebar : t.collapseSidebar}>
            <span className="nav-ico"><Icon name={sidebarCollapsed ? 'chevronRight' : 'chevronLeft'} size={18} /></span>
            <span className="nav-label">{t.collapseSidebar}</span>
          </button>
        </div>
      </aside>

      <div className="main">
        <header className="topbar">
          <nav className="topnav">
            <button className={`topnav-link ${view === 'dashboard' ? 'active' : ''}`} onClick={() => switchView('dashboard')}>
              {t.topnav.dashboard}
            </button>
            <button className="topnav-link" style={{ opacity: .45, cursor: 'default' }}>
              {t.topnav.cases}
            </button>
            <button className={`topnav-link ${view === 'assets' ? 'active' : ''}`} onClick={() => switchView('assets')}>
              {t.topnav.assets}
            </button>
          </nav>
          <div className="topbar-right">
            <LangMenu lang={lang} onChange={setLang} />
            <button className="top-icon"><Icon name="settings" size={19} /></button>
            <SessionMenu
              hasKey={hasKey}
              onChangeKey={() => setAuthNeeded(true)}
              onClearKey={() => { setApiKey(''); setHasKey(false) }}
              t={t}
            />
            <button className="btn btn-ghost" style={{ marginLeft: 4 }}><Icon name="helpCircle" size={15} /> {t.help}</button>
            <button className="btn btn-primary" onClick={() => setRunOpen(true)}><Icon name="play" size={14} /> {t.runBtn}</button>
          </div>
        </header>

        {view === 'dashboard' ? (
          <DashboardView t={t} openBrowser={openBrowser} serverProfiles={serverProfiles} notify={notify} />
        ) : (
          <>
            <div className="content">
              <div className="content-inner" key={active + lang}>
                <ActiveScreen s={s} patch={patch} t={t} getIssue={getIssue} notify={notify} openBrowser={openBrowser} />
              </div>
            </div>
            <CommandBar cmd={cmd} issues={issues} t={t} />
          </>
        )}
      </div>

      <RunModal
        open={runOpen}
        onClose={() => setRunOpen(false)}
        cmd={cmd}
        issues={issues}
        s={s}
        t={t}
        lang={lang}
        serverProfiles={serverProfiles}
      />
      <ApiKeyModal open={authNeeded} onConnect={() => { setAuthNeeded(false); setHasKey(true) }} t={t} />
      <Toasts items={toasts} />

      {browser && (
        <FileBrowserModal
          dir={browser.dir}
          accept={browser.accept}
          onSelect={browser.onSelect}
          onClose={closeBrowser}
          t={t}
        />
      )}

      <TweaksPanel title={t.tweaks.title}>
        <TweakSection label={t.tweaks.accent} />
        <TweakColor label={t.tweaks.accent} value={tw.accent}
          options={['#58b6e8', '#5fc99a', '#e8b860', '#a78bfa', '#f0707a', '#5b8def']}
          onChange={(v) => setTweak('accent', v)} />
        <TweakSection label={t.tweaks.techFont} />
        <TweakRadio label={t.tweaks.techFont} value={tw.techFont}
          options={[{ value: 'mono', label: t.tweaks.mono }, { value: 'sans', label: t.tweaks.sans }]}
          onChange={(v) => setTweak('techFont', v)} />
        <TweakToggle label={t.tweaks.flagPills} value={tw.showFlags} onChange={(v) => setTweak('showFlags', v)} />
      </TweaksPanel>
    </div>
  )
}
