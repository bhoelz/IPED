// IPED Processing Dashboard — React island
// Loaded by dashboard.rocker.html; depends on tweaks-panel.js being loaded first.
// Dev mode: Babel compiles JSX in-browser. Prod: Vite bundle (dashboard-island.js).

const { useState, useEffect, useRef } = React;

// ── Helpers ───────────────────────────────────────────────────────────────────

function fmtN(n) {
  if (n == null || n === 0) return '—';
  return Number(n).toLocaleString();
}

function fmtDur(ms) {
  if (!ms) return '—';
  const s = Math.floor(ms / 1000);
  const h = Math.floor(s / 3600);
  const m = Math.floor((s % 3600) / 60);
  const sc = s % 60;
  if (h > 0) return `${h}h ${m}m ${sc}s`;
  if (m > 0) return `${m}m ${sc}s`;
  return `${sc}s`;
}

const STATUS_COLOR = {
  running: 'var(--ok)',
  done:    '#5fc99a',
  error:   'var(--danger)',
  aborted: 'var(--text-muted)',
  queued:  'var(--text-muted)',
};

// ── SVG Components ────────────────────────────────────────────────────────────

function ProgressRing({ pct, size = 46, color = 'var(--accent)' }) {
  const r = (size - 6) / 2;
  const circ = 2 * Math.PI * r;
  const fill = (pct / 100) * circ;
  const cx = size / 2, cy = size / 2;
  return (
    <svg width={size} height={size} viewBox={`0 0 ${size} ${size}`} style={{flexShrink:0}}>
      <circle cx={cx} cy={cy} r={r} fill="none" stroke="var(--bg-inset)" strokeWidth={3} />
      <circle cx={cx} cy={cy} r={r} fill="none" stroke={color} strokeWidth={3}
        strokeDasharray={`${fill} ${circ}`} strokeLinecap="round"
        transform={`rotate(-90 ${cx} ${cy})`}
        style={{transition:'stroke-dasharray 1s ease'}} />
      <text x={cx} y={cy + 4} textAnchor="middle"
        fontSize={10} fontWeight={600} fontFamily="var(--font-mono)" fill={color}>
        {pct}%
      </text>
    </svg>
  );
}

function StatusPill({ status }) {
  const label = status ? status.toUpperCase() : 'UNKNOWN';
  return (
    <span className={`status-pill ${status || ''}`}>
      {status === 'running' && <span className="pulse-dot" />}
      {label}
    </span>
  );
}

// ── Detail panel ──────────────────────────────────────────────────────────────

function OverviewTab({ job }) {
  const pct = job.itemsFound > 0
    ? Math.round((job.itemsProcessed / job.itemsFound) * 100)
    : (job.status === 'done' ? 100 : 0);
  return (
    <div className="stat-strip">
      {[
        ['Duration',         fmtDur(job.durationMs)],
        ['Items Processed',  fmtN(job.itemsProcessed)],
        ['Items Found',      fmtN(job.itemsFound)],
        ['Progress',         pct + '%'],
        ['Current Speed',    job.currentSpeed || '—'],
        ['ETA',              job.eta          || '—'],
        ['Status',           job.status       || '—'],
        ['Source',           job.source       || '—'],
      ].map(([lbl, val]) => (
        <div className="stat-cell" key={lbl}>
          <div className="stat-lbl">{lbl}</div>
          <div className="stat-val" title={val}
               style={{overflow:'hidden',textOverflow:'ellipsis',whiteSpace:'nowrap'}}>{val}</div>
        </div>
      ))}
    </div>
  );
}

function LogTab({ job }) {
  const endRef = useRef(null);
  useEffect(() => {
    endRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [job.recentLines?.length]);

  const lines = job.recentLines || [];
  return (
    <div className="log-panel">
      {lines.length === 0
        ? <span className="log-empty">No log output yet.</span>
        : lines.map((line, i) => <div key={i}>{line}</div>)
      }
      <div ref={endRef} />
    </div>
  );
}

const DETAIL_TABS = [
  { id: 'overview', label: 'Overview' },
  { id: 'log',      label: 'Log' },
];

function DetailPanel({ job }) {
  const [tab, setTab] = useState('overview');
  return (
    <div className="detail-panel">
      <div className="detail-tabs">
        {DETAIL_TABS.map(({ id, label }) => (
          <button key={id}
            className={`detail-tab${tab === id ? ' active' : ''}`}
            onClick={() => setTab(id)}>
            {label}
          </button>
        ))}
      </div>
      <div className="detail-body">
        {tab === 'overview' && <OverviewTab job={job} />}
        {tab === 'log'      && <LogTab job={job} />}
      </div>
    </div>
  );
}

// ── Job card ──────────────────────────────────────────────────────────────────

function JobRow({ job }) {
  const [open, setOpen] = useState(false);
  const toggle = () => setOpen(o => !o);
  const pct   = job.itemsFound > 0
    ? Math.round((job.itemsProcessed / job.itemsFound) * 100)
    : (job.status === 'done' ? 100 : 0);
  const color = STATUS_COLOR[job.status] || 'var(--accent)';

  return (
    <div className={`job-card${open ? ' open' : ''}`}>
      <div className="job-row" onClick={toggle}>
        <ProgressRing pct={pct} size={46} color={color} />

        <div style={{minWidth:0}}>
          <div style={{fontWeight:600,fontSize:13,color:'var(--text)',
                       whiteSpace:'nowrap',overflow:'hidden',textOverflow:'ellipsis'}}>
            {job.name}
          </div>
          <div style={{fontSize:10.5,fontFamily:'var(--font-mono)',color:'var(--text-muted)',
                       marginTop:2,whiteSpace:'nowrap',overflow:'hidden',textOverflow:'ellipsis'}}>
            {job.source}
          </div>
        </div>

        <StatusPill status={job.status} />

        <div>
          <div style={{display:'flex',justifyContent:'space-between',marginBottom:4}}>
            <span style={{fontSize:10.5,fontFamily:'var(--font-mono)',color:'var(--text-muted)'}}>
              {fmtN(job.itemsProcessed)} / {fmtN(job.itemsFound)}
            </span>
            <span style={{fontSize:10.5,fontFamily:'var(--font-mono)',color,fontWeight:600}}>
              {pct}%
            </span>
          </div>
          <div className="prog-bar-wrap">
            <div className="prog-bar-fill" style={{width:`${pct}%`,background:color}} />
          </div>
        </div>

        <div>
          <div className="stat-lbl">ETA</div>
          <div style={{fontSize:11.5,fontFamily:'var(--font-mono)',color:'var(--text-2)'}}>
            {job.eta || '—'}
          </div>
        </div>

        <div>
          <div className="stat-lbl">Speed</div>
          <div style={{fontSize:11.5,fontFamily:'var(--font-mono)',color:'var(--text-2)'}}>
            {job.currentSpeed || '—'}
          </div>
        </div>

        <div onClick={e => e.stopPropagation()}>
          <button className={`view-btn${open ? ' active' : ''}`} onClick={toggle}>
            {open ? '▲ Hide' : '▼ View Details'}
          </button>
        </div>
      </div>

      {open && <DetailPanel job={job} />}
    </div>
  );
}

// ── Page header ───────────────────────────────────────────────────────────────

function PageHeader({ jobs }) {
  const running = jobs.filter(j => j.status === 'running').length;
  const done    = jobs.filter(j => j.status === 'done').length;
  const errors  = jobs.filter(j => j.status === 'error').length;
  const total   = jobs.reduce((a, j) => a + (j.itemsProcessed || 0), 0);
  return (
    <div className="dash-page-header">
      <div style={{flex:1, minWidth:160}}>
        <div className="dash-page-title">Processing Dashboard</div>
        <div className="dash-page-sub">Active indexing &amp; processing jobs</div>
      </div>
      {[
        { lbl:'Running',         val: running,    color:'var(--ok)' },
        { lbl:'Complete',        val: done,        color:'#5fc99a' },
        { lbl:'Errors',          val: errors,      color:'var(--danger)' },
        { lbl:'Items Processed', val: fmtN(total), color:'var(--accent)' },
      ].map(({ lbl, val, color }) => (
        <div className="dash-stat-chip" key={lbl}>
          <div className="dash-stat-val" style={{color}}>{val}</div>
          <div className="dash-stat-lbl">{lbl}</div>
        </div>
      ))}
    </div>
  );
}

// ── App root ──────────────────────────────────────────────────────────────────

function DashboardApp() {
  const [tw, setTweak] = useTweaks({ accent: '#e0a83a' });
  const [jobs, setJobs]       = useState([]);
  const [clock, setClock]     = useState(() => new Date().toLocaleTimeString());
  const [connErr, setConnErr] = useState(null);

  useEffect(() => {
    const root = document.getElementById('dashboard-root');
    if (root) root.style.setProperty('--accent', tw.accent);
  }, [tw.accent]);

  useEffect(() => {
    const id = setInterval(() => setClock(new Date().toLocaleTimeString()), 1000);
    return () => clearInterval(id);
  }, []);

  useEffect(() => {
    fetch('/dashboard/jobs')
      .then(r => r.json())
      .then(setJobs)
      .catch(() => {});

    const es = new EventSource('/dashboard/stream');
    es.addEventListener('jobs-update', e => {
      try { setJobs(JSON.parse(e.data)); } catch (_) {}
      setConnErr(null);
    });
    es.onerror = () => setConnErr('Connection lost — retrying…');
    return () => es.close();
  }, []);

  const running = jobs.filter(j => j.status === 'running').length;
  const done    = jobs.filter(j => j.status === 'done').length;
  const queued  = jobs.filter(j => j.status === 'queued').length;

  return (
    <div className="dash-shell">
      <header className="dash-topbar">
        <div className="dash-brand">A</div>
        <div>
          <div className="dash-brand-name">ACE Framework</div>
          <div className="dash-brand-sub">Processing Monitor</div>
        </div>
        <div className="dash-topbar-sep" />
        <span className="dash-breadcrumb">IPED v4 · Processing Dashboard</span>
      </header>

      <div className="dash-scroll">
        <PageHeader jobs={jobs} />

        {connErr && (
          <div style={{
            background:'color-mix(in srgb,var(--danger) 10%,var(--bg-card))',
            border:'1px solid color-mix(in srgb,var(--danger) 30%,transparent)',
            borderRadius:6, padding:'8px 14px', marginBottom:14,
            color:'var(--danger)', fontSize:12,
          }}>⚠ {connErr}</div>
        )}

        {jobs.length === 0 ? (
          <div className="dash-empty">
            <div className="dash-empty-icon">⏳</div>
            <div>No active jobs — start a processing run in the Runner.</div>
            <a href="/" style={{color:'var(--accent)',fontSize:12}}>Go to Runner →</a>
          </div>
        ) : (
          <div className="job-list">
            {jobs.map(job => <JobRow key={job.id} job={job} />)}
          </div>
        )}
      </div>

      <footer className="dash-statusbar">
        <span className={`dash-statusbar-dot${running > 0 ? ' ok' : ''}`} />
        <span>IPED v4.4.0</span>
        <span>·</span>
        <span>{running} running · {done} complete · {queued} queued</span>
        <span style={{marginLeft:'auto'}}>{clock}</span>
      </footer>

      <TweaksPanel title="Dashboard Tweaks">
        <TweakSection label="Appearance">
          <TweakColor label="Accent" value={tw.accent}
            options={['#e0a83a','#58b6e8','#5fc99a','#a78bfa','#f0707a','#5b8def']}
            onChange={v => setTweak('accent', v)} />
        </TweakSection>
      </TweaksPanel>
    </div>
  );
}

ReactDOM.createRoot(document.getElementById('dashboard-root')).render(<DashboardApp />);
