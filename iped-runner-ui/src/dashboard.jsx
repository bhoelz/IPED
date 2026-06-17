import { useState, useEffect, useRef, useCallback, useMemo } from 'react'
import { Icon } from './icons.jsx'
import { apiFetch } from './api.js'
import { canNotify, notifyPermission, requestPermission, sendNotification } from './notifications.js'

// ── Helpers ──────────────────────────────────────────────────────────────────

function relTime(iso) {
  if (!iso) return ''
  const diff = Date.now() - new Date(iso).getTime()
  const s = Math.floor(diff / 1000)
  if (s < 5)  return 'just now'
  if (s < 60) return `${s}s ago`
  const m = Math.floor(s / 60)
  if (m < 60) return `${m}m ago`
  const h = Math.floor(m / 60)
  if (h < 24) return `${h}h ago`
  return new Date(iso).toLocaleDateString()
}

function elapsed(startIso, endIso) {
  if (!startIso) return ''
  const ms = (endIso ? new Date(endIso) : new Date()) - new Date(startIso)
  const s = Math.floor(ms / 1000)
  if (s < 60) return `${s}s`
  const m = Math.floor(s / 60)
  if (m < 60) return `${m}m ${s % 60}s`
  const h = Math.floor(m / 60)
  return `${h}h ${m % 60}m`
}

function fmtDuration(ms) {
  if (!ms) return '—'
  const s = Math.floor(ms / 1000)
  if (s < 60) return `${s}s`
  const m = Math.floor(s / 60)
  if (m < 60) return `${m}m ${s % 60}s`
  return `${Math.floor(m / 60)}h ${m % 60}m`
}

function fmtUptime(ms) {
  if (!ms) return '—'
  const s = Math.floor(ms / 1000)
  const h = Math.floor(s / 3600)
  const m = Math.floor((s % 3600) / 60)
  if (h >= 24) return `${Math.floor(h / 24)}d ${h % 24}h`
  if (h > 0)   return `${h}h ${m}m`
  return `${m}m`
}

// ── Local toast ───────────────────────────────────────────────────────────────

function useDashToasts() {
  const [toasts, setToasts] = useState([])
  const idRef = useRef(0)
  const toast = useCallback((msg, type = 'ok') => {
    const id = ++idRef.current
    setToasts(p => [...p, { id, msg, type }])
    setTimeout(() => setToasts(p => p.filter(x => x.id !== id)), 2800)
  }, [])
  return [toasts, toast]
}

function DashToasts({ toasts }) {
  if (!toasts.length) return null
  return (
    <div className="dash-toasts">
      {toasts.map(t => (
        <div key={t.id} className={`dash-toast ${t.type}`}>
          <Icon name={t.type === 'err' ? 'alert' : 'check'} size={14} stroke={2.5} />
          {t.msg}
        </div>
      ))}
    </div>
  )
}

// ── Status chip ───────────────────────────────────────────────────────────────

const STATUS_CFG = {
  running:   { color: 'var(--accent)',      bg: 'var(--accent-soft)',                                   icon: 'activity' },
  pending:   { color: 'var(--warn)',        bg: 'color-mix(in srgb, var(--warn) 14%, var(--bg-card))', icon: 'clock'    },
  completed: { color: 'var(--ok)',          bg: 'color-mix(in srgb, var(--ok) 14%, var(--bg-card))',   icon: 'check'    },
  failed:    { color: 'var(--danger)',      bg: 'var(--danger-soft)',                                   icon: 'alert'    },
  cancelled: { color: 'var(--text-muted)', bg: 'var(--bg-chip)',                                        icon: 'x'        },
}

function StatusChip({ status }) {
  const cfg = STATUS_CFG[status] || STATUS_CFG.pending
  return (
    <span className="status-chip" style={{ color: cfg.color, background: cfg.bg }}>
      <Icon name={cfg.icon} size={11} stroke={2.5} /> {status}
    </span>
  )
}

// ── Progress bar ──────────────────────────────────────────────────────────────

function ProgressBar({ progress, status, style }) {
  const base = { height: 4, ...style }
  if (status === 'pending') return null
  if (status === 'running' && progress === 0) {
    return <div className="progress-track" style={base}><div className="progress-fill-pulse" style={{ height: '100%' }} /></div>
  }
  const color = status === 'completed' ? 'var(--ok)' : status === 'failed' ? 'var(--danger)' : status === 'cancelled' ? 'var(--text-muted)' : 'var(--accent)'
  return <div className="progress-track" style={base}><div className="progress-fill" style={{ width: `${Math.max(2, progress)}%`, background: color }} /></div>
}

// ── Job list row ──────────────────────────────────────────────────────────────

function JobRow({ job, selected, onSelect }) {
  const name = job.params?.name || job.id.slice(0, 8)
  return (
    <button type="button" className={`job-row ${selected ? 'selected' : ''}`} onClick={() => onSelect(selected ? null : job.id)}>
      <div className="job-row-main">
        <div className="job-row-name">{name}</div>
        <div className="job-row-meta">
          <span className="job-row-id tech">{job.id.slice(0, 8)}</span>
          <span className="job-row-time">{relTime(job.createdAt)}</span>
        </div>
        <ProgressBar progress={job.progress} status={job.status} style={{ marginTop: 6, height: 3 }} />
      </div>
      <div className="job-row-right">
        <StatusChip status={job.status} />
        {job.status === 'running' && job.progress > 0 && (
          <span className="tech" style={{ fontSize: 10.5, color: 'var(--text-muted)' }}>{job.progress}%</span>
        )}
      </div>
    </button>
  )
}

// ── Job detail pane ───────────────────────────────────────────────────────────

function JobDetail({ job, logs, logsLoading, onCancel, onSaveLog, td }) {
  const logEndRef = useRef(null)

  useEffect(() => {
    if (logEndRef.current) logEndRef.current.scrollIntoView({ behavior: 'smooth' })
  }, [logs])

  if (!job) {
    return (
      <div className="detail-empty">
        <Icon name="layers" size={32} style={{ color: 'var(--text-muted)', marginBottom: 12 }} />
        <div style={{ color: 'var(--text-muted)', fontSize: 13 }}>{td.selectRun}</div>
      </div>
    )
  }

  const { params = {} } = job
  const canCancel = job.status === 'running' || job.status === 'pending'

  return (
    <div className="detail-panel">
      <div className="detail-head">
        <div style={{ flex: 1, minWidth: 0 }}>
          <div className="detail-title" title={params.name || job.id}>{params.name || job.id}</div>
          <div className="tech" style={{ fontSize: 10.5, color: 'var(--text-muted)', marginTop: 2 }}>{job.id}</div>
        </div>
        <StatusChip status={job.status} />
        {logs.length > 0 && (
          <button type="button" className="btn btn-ghost btn-sm" onClick={onSaveLog} title={td.saveLog}>
            <Icon name="download" size={13} /> {td.saveLog}
          </button>
        )}
        {canCancel && (
          <button type="button" className="btn btn-ghost btn-sm"
            style={{ color: 'var(--danger)', borderColor: 'color-mix(in srgb, var(--danger) 40%, transparent)' }}
            onClick={onCancel}>
            <Icon name="stopCircle" size={13} /> {td.cancel}
          </button>
        )}
      </div>

      <div className="detail-body">
        <ProgressBar progress={job.progress} status={job.status} />

        <div className="detail-meta-grid">
          {job.createdAt && <div><span className="dmeta-k">{td.meta.started}</span><span className="dmeta-v">{new Date(job.createdAt).toLocaleString()}</span></div>}
          {job.terminatedAt && <div><span className="dmeta-k">{td.meta.ended}</span><span className="dmeta-v">{new Date(job.terminatedAt).toLocaleString()}</span></div>}
          {job.createdAt && <div><span className="dmeta-k">{td.meta.duration}</span><span className="dmeta-v tech">{elapsed(job.createdAt, job.terminatedAt)}</span></div>}
          {params.source && <div><span className="dmeta-k">{td.meta.source}</span><span className="dmeta-v tech" style={{ wordBreak: 'break-all' }}>{params.source}</span></div>}
          {params.priority && <div><span className="dmeta-k">{td.meta.priority}</span><span className="dmeta-v">{params.priority}</span></div>}
          {params.itemsFound > 0 && (
            <div>
              <span className="dmeta-k">{td.meta.items}</span>
              <span className="dmeta-v tech">{params.itemsProcessed?.toLocaleString()} / {params.itemsFound?.toLocaleString()}</span>
            </div>
          )}
        </div>

        {job.message && job.status !== 'running' && (
          <div style={{
            padding: '9px 12px', borderRadius: 8, fontSize: 12.5, fontFamily: 'var(--font-mono)',
            background: job.status === 'failed' ? 'var(--danger-soft)' : 'var(--bg-inset)',
            color: job.status === 'failed' ? 'var(--danger)' : 'var(--text-muted)',
            border: '1px solid var(--border)',
          }}>{job.message}</div>
        )}

        <div>
          <div className="detail-section-label">{td.logOutput}</div>
          <div className="log-terminal" style={{ height: 240, maxHeight: 'calc(100vh - 480px)' }}>
            {logsLoading && logs.length === 0 && <span style={{ opacity: .5, display: 'flex', alignItems: 'center', gap: 8 }}><span className="spinner" /> {td.connecting}</span>}
            {!logsLoading && logs.length === 0 && <span style={{ opacity: .5 }}>{td.noLog}</span>}
            {logs.map((line, i) => <div className="log-line" key={i}>{line}</div>)}
            <div ref={logEndRef} />
          </div>
        </div>
      </div>
    </div>
  )
}

// ── Agents panel ──────────────────────────────────────────────────────────────

function AgentsPanel({ agents, td }) {
  if (!agents) return <div className="detail-empty"><span className="spinner" /></div>
  if (!agents.enabled) {
    return <div className="detail-empty"><Icon name="wifiOff" size={28} style={{ color: 'var(--text-muted)', marginBottom: 8 }} /><div style={{ color: 'var(--text-muted)', fontSize: 13 }}>{td.kafkaOff}</div></div>
  }
  if (agents.agents.length === 0) {
    return <div className="detail-empty"><Icon name="server" size={28} style={{ color: 'var(--text-muted)', marginBottom: 8 }} /><div style={{ color: 'var(--text-muted)', fontSize: 13 }}>{td.noAgents}</div></div>
  }
  const HEALTH_ICON = { active: 'wifi', stale: 'clock', offline: 'wifiOff' }
  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
      <div className="agents-summary">
        <span style={{ color: 'var(--ok)' }}>{agents.active} active</span>
        {agents.stale  > 0 && <span style={{ color: 'var(--warn)' }}>{agents.stale} stale</span>}
        {agents.offline > 0 && <span style={{ color: 'var(--text-muted)' }}>{agents.offline} offline</span>}
      </div>
      {agents.agents.map((a) => (
        <div key={a.agentId} className="agent-card">
          <div className="agent-card-head">
            <span style={{ display: 'inline-flex', color: a.health === 'active' ? 'var(--ok)' : a.health === 'stale' ? 'var(--warn)' : 'var(--text-muted)' }}>
              <Icon name={HEALTH_ICON[a.health] || 'server'} size={15} />
            </span>
            <span className="agent-hostname">{a.hostname || a.agentId}</span>
            <span className={`agent-health-badge ${a.health}`}>{a.health}</span>
          </div>
          <div className="agent-meta">
            {a.version && <span className="tech">{a.version}</span>}
            <span className="tech">{a.activeTasks}/{a.maxTasks} tasks</span>
            {a.startedAt && <span>up {elapsed(a.startedAt)}</span>}
            <span>seen {relTime(a.lastSeen)}</span>
          </div>
        </div>
      ))}
    </div>
  )
}

// ── Queue panel ───────────────────────────────────────────────────────────────

function QueuePanel({ queue, onCancel, onClearAll, td }) {
  const [confirmClear, setConfirmClear] = useState(false)

  const handleClearAll = async () => {
    if (!confirmClear) { setConfirmClear(true); return }
    setConfirmClear(false)
    await onClearAll()
  }

  if (!queue) return <div className="detail-empty"><span className="spinner" /></div>
  if (queue.length === 0) {
    return <div className="detail-empty"><Icon name="layers" size={28} style={{ color: 'var(--text-muted)', marginBottom: 8 }} /><div style={{ color: 'var(--text-muted)', fontSize: 13 }}>{td.noQueue}</div></div>
  }

  const PRIORITY_COLOR = { HIGH: 'var(--danger)', NORMAL: 'var(--accent-text)', LOW: 'var(--text-muted)' }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
      <div className="queue-panel-head">
        <span className="queue-panel-count">{queue.length} queued</span>
        <button type="button" className="btn btn-ghost btn-sm"
          style={confirmClear ? { color: 'var(--danger)', borderColor: 'color-mix(in srgb, var(--danger) 40%, transparent)' } : {}}
          onClick={handleClearAll}
          onBlur={() => setTimeout(() => setConfirmClear(false), 200)}>
          <Icon name="trash" size={13} />
          {confirmClear ? td.clearQueueConfirm : td.clearQueue}
        </button>
      </div>
      {queue.map((q) => (
        <div key={q.id} className="queue-row">
          <div style={{ flex: 1, minWidth: 0 }}>
            <div style={{ color: 'var(--text)', fontSize: 13.5, fontWeight: 500, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
              {q.name || q.id.slice(0, 8)}
            </div>
            <div style={{ display: 'flex', gap: 10, marginTop: 4, flexWrap: 'wrap', alignItems: 'center' }}>
              <span style={{ fontSize: 11.5, fontWeight: 700, color: PRIORITY_COLOR[q.priority] || 'var(--accent-text)' }}>↑ {q.priority}</span>
              {q.profile && <span className="tech" style={{ fontSize: 11.5, color: 'var(--text-muted)' }}>profile: {q.profile}</span>}
              <span style={{ fontSize: 11.5, color: 'var(--text-muted)' }}>{relTime(q.enqueuedAt)}</span>
            </div>
          </div>
          <button type="button" className="icon-btn danger" onClick={() => onCancel(q.id)} title={td.cancel}>
            <Icon name="x" size={15} />
          </button>
        </div>
      ))}
    </div>
  )
}

// ── Audit item detail ─────────────────────────────────────────────────────────

function AuditItemDetail({ detail, ta }) {
  if (!detail) return null
  const fields = [
    ['itemUuid',      detail.itemUuid],
    ['caseId',        detail.caseId],
    ['taskType',      detail.taskType],
    ['pipelineStage', detail.pipelineStage != null ? String(detail.pipelineStage) : null],
    ['outcome',       detail.outcome],
    ['agentId',       detail.agentId],
    ['processedAt',   detail.processedAt ? new Date(detail.processedAt).toLocaleString() : null],
    ['durationMs',    fmtDuration(detail.durationMs)],
  ].filter(([, v]) => v != null && v !== '')

  return (
    <div className="audit-detail-cell">
      <div style={{ fontSize: 11, fontWeight: 700, color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: .5, marginBottom: 10 }}>
        {ta.itemDetail}
      </div>
      <div className="audit-detail-grid">
        {fields.map(([k, v]) => (
          <div key={k}>
            <div className="audit-detail-key">{k}</div>
            <div className="audit-detail-val">{v}</div>
          </div>
        ))}
      </div>
      {detail.errorMessage && (
        <>
          <div style={{ fontSize: 11, fontWeight: 700, color: 'var(--danger)', textTransform: 'uppercase', letterSpacing: .5, marginBottom: 6, marginTop: 12 }}>
            {ta.fullError}
          </div>
          <div className="audit-detail-error">{detail.errorMessage}</div>
        </>
      )}
    </div>
  )
}

// ── Audit panel ───────────────────────────────────────────────────────────────

function AuditPanel({ td }) {
  const ta = td.audit
  const [caseId,      setCaseId]      = useState('')
  const [records,     setRecords]     = useState(null)
  const [loading,     setLoading]     = useState(false)
  const [error,       setError]       = useState(null)
  const [selectedRow, setSelectedRow] = useState(null)
  const [itemDetail,  setItemDetail]  = useState(null)
  const [itemLoading, setItemLoading] = useState(false)

  const load = async () => {
    const id = caseId.trim()
    if (!id) return
    setLoading(true); setError(null); setRecords(null); setSelectedRow(null); setItemDetail(null)
    try {
      const r = await apiFetch('/api/v1/audit/' + encodeURIComponent(id))
      if (r.status === 404) { setRecords([]) }
      else if (r.ok) { const d = await r.json(); setRecords(d.records || []) }
      else { setError('HTTP ' + r.status) }
    } catch (e) { setError(e.message) }
    setLoading(false)
  }

  const selectRow = async (r, i) => {
    if (selectedRow === i) { setSelectedRow(null); setItemDetail(null); return }
    setSelectedRow(i); setItemDetail(null)
    if (!r.itemUuid) return
    setItemLoading(true)
    try {
      const resp = await apiFetch('/api/v1/audit/' + encodeURIComponent(caseId.trim()) + '/' + encodeURIComponent(r.itemUuid))
      if (resp.ok) setItemDetail(await resp.json())
    } catch (_) {}
    setItemLoading(false)
  }

  const csvHref = caseId.trim() ? '/api/v1/audit/' + encodeURIComponent(caseId.trim()) + '/csv' : '#'

  return (
    <div>
      <div className="audit-search">
        <input className="inp" value={caseId} placeholder={ta.casePh}
          onChange={(e) => setCaseId(e.target.value)} onKeyDown={(e) => e.key === 'Enter' && load()} />
        <button className="btn btn-soft btn-sm" onClick={load} disabled={loading || !caseId.trim()}>
          {loading ? <span className="spinner" style={{ width: 13, height: 13 }} /> : <Icon name="search" size={13} />}
          {ta.load}
        </button>
        {records?.length > 0 && (
          <a className="btn btn-ghost btn-sm" href={csvHref} download={`audit-${caseId.trim()}.csv`}>
            <Icon name="download" size={13} /> {ta.exportCsv}
          </a>
        )}
      </div>

      {error && <div className="hinttext err" style={{ marginBottom: 12 }}><Icon name="alert" size={13} /> {error}</div>}

      {records === null && !loading && (
        <div className="detail-empty"><Icon name="search" size={28} style={{ color: 'var(--text-muted)', marginBottom: 8 }} /><div style={{ color: 'var(--text-muted)', fontSize: 13 }}>{ta.noCaseId}</div></div>
      )}
      {records?.length === 0 && (
        <div className="detail-empty"><Icon name="fileText" size={28} style={{ color: 'var(--text-muted)', marginBottom: 8 }} /><div style={{ color: 'var(--text-muted)', fontSize: 13 }}>{ta.noRecords}</div></div>
      )}

      {records?.length > 0 && (
        <div style={{ overflowX: 'auto' }}>
          <table className="audit-table">
            <thead>
              <tr>
                <th>{ta.cols.time}</th><th>{ta.cols.item}</th><th>{ta.cols.task}</th>
                <th>{ta.cols.stage}</th><th>{ta.cols.duration}</th><th>{ta.cols.outcome}</th>
                <th>{ta.cols.agent}</th><th>{ta.cols.error}</th>
              </tr>
            </thead>
            <tbody>
              {records.map((r, i) => (
                <>
                  <tr key={`row-${i}`} className={`audit-row ${selectedRow === i ? 'audit-selected' : ''}`} onClick={() => selectRow(r, i)}>
                    <td className="tech" style={{ whiteSpace: 'nowrap', fontSize: 11.5 }}>{r.processedAt ? new Date(r.processedAt).toLocaleTimeString() : '—'}</td>
                    <td className="tech" style={{ fontSize: 11, color: 'var(--text-muted)' }} title={r.itemUuid}>{r.itemUuid ? r.itemUuid.slice(0, 8) + '…' : '—'}</td>
                    <td style={{ fontSize: 12 }}>{r.taskType || '—'}</td>
                    <td className="tech" style={{ textAlign: 'center' }}>{r.pipelineStage ?? '—'}</td>
                    <td className="tech" style={{ whiteSpace: 'nowrap' }}>{fmtDuration(r.durationMs)}</td>
                    <td><span className={`audit-outcome ${r.outcome}`}>{r.outcome}</span></td>
                    <td className="tech" style={{ fontSize: 11, color: 'var(--text-muted)' }} title={r.agentId}>{r.agentId ? r.agentId.slice(0, 12) : '—'}</td>
                    <td style={{ fontSize: 11.5, color: 'var(--danger)', maxWidth: 200, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{r.errorMessage || ''}</td>
                  </tr>
                  {selectedRow === i && (
                    <tr key={`detail-${i}`} className="audit-detail-row">
                      <td colSpan={8} style={{ padding: 0, border: 'none' }}>
                        {itemLoading
                          ? <div style={{ padding: '14px 16px', display: 'flex', gap: 10, color: 'var(--text-muted)', fontSize: 13 }}><span className="spinner" /> Loading…</div>
                          : <AuditItemDetail detail={itemDetail} ta={ta} />}
                      </td>
                    </tr>
                  )}
                </>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  )
}

// ── Batch submit modal ────────────────────────────────────────────────────────

const PRIORITIES = ['HIGH', 'NORMAL', 'LOW']

function BatchModal({ open, onClose, profiles, openBrowser, td }) {
  const tb = td.batch
  const [sourceDir, setSourceDir] = useState('')
  const [outputDir, setOutputDir] = useState('')
  const [priority,  setPriority]  = useState('NORMAL')
  const [profile,   setProfile]   = useState('')
  const [loading,   setLoading]   = useState(false)
  const [result,    setResult]    = useState(null)
  const [error,     setError]     = useState(null)

  useEffect(() => {
    if (open) { setSourceDir(''); setOutputDir(''); setPriority('NORMAL'); setProfile(''); setResult(null); setError(null) }
  }, [open])

  const submit = async () => {
    setLoading(true); setError(null)
    try {
      const body = { sourceDir, outputDir, priority }
      if (profile) body.profile = profile
      const resp = await apiFetch('/runs/batch', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body) })
      const data = await resp.json()
      if (!resp.ok) setError(data.error || 'HTTP ' + resp.status)
      else setResult(data)
    } catch (e) { setError(e.message) }
    setLoading(false)
  }

  if (!open) return null
  const canSubmit = sourceDir.trim() && outputDir.trim() && !loading

  return (
    <div className="overlay" onClick={result ? onClose : undefined}>
      <div className="modal" onClick={(e) => e.stopPropagation()} style={{ maxWidth: 520 }}>
        <div className="modal-head">
          <span style={{ color: 'var(--accent)' }}><Icon name="package" size={22} /></span>
          <div style={{ flex: 1 }}><div className="modal-title">{tb.title}</div><div className="modal-sub">{tb.subtitle}</div></div>
          {!loading && <button type="button" className="icon-btn" onClick={onClose}><Icon name="x" size={18} /></button>}
        </div>
        <div className="modal-body">
          {result ? (
            <div className="batch-result">
              <Icon name="check" size={44} style={{ color: 'var(--ok)' }} />
              <div className="batch-result-count">{result.enqueuedCount}</div>
              <div style={{ color: 'var(--text-2)', fontSize: 14 }}>{tb.resultSuffix}</div>
              {result.errors?.length > 0 && (
                <div className="batch-errors" style={{ width: '100%', textAlign: 'left' }}>
                  <div className="batch-errors-title">{tb.errorsTitle} ({result.errors.length})</div>
                  {result.errors.map((e, i) => <div className="batch-error-line" key={i}>{e}</div>)}
                </div>
              )}
            </div>
          ) : (
            <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
              <div className="field">
                <label className="field-label">{tb.sourceDir}</label>
                <div className="inp-wrap">
                  <input className="inp tech has-affix" value={sourceDir} placeholder={tb.sourceDirPh} onChange={(e) => setSourceDir(e.target.value)} />
                  <button className="inp-affix" type="button" onClick={() => openBrowser(sourceDir, 'dir', setSourceDir)}><Icon name="folder" size={16} /></button>
                </div>
              </div>
              <div className="field">
                <label className="field-label">{tb.outputDir}</label>
                <div className="inp-wrap">
                  <input className="inp tech has-affix" value={outputDir} placeholder={tb.outputDirPh} onChange={(e) => setOutputDir(e.target.value)} />
                  <button className="inp-affix" type="button" onClick={() => openBrowser(outputDir, 'dir', setOutputDir)}><Icon name="folder" size={16} /></button>
                </div>
              </div>
              <div className="field">
                <label className="field-label">{td.priority.title}</label>
                <div className="priority-select">
                  {PRIORITIES.map((p) => (
                    <button key={p} type="button" className={`priority-opt ${priority === p ? 'active ' + p : ''}`} onClick={() => setPriority(p)}>
                      {p === 'HIGH' && <Icon name="arrowUp" size={12} />}
                      {p === 'LOW'  && <Icon name="arrowDown" size={12} />}
                      {p === 'HIGH' ? td.priority.high : p === 'LOW' ? td.priority.low : td.priority.normal}
                    </button>
                  ))}
                </div>
              </div>
              {profiles.length > 0 && (
                <div className="field">
                  <label className="field-label">{tb.profile}</label>
                  <div className="sel-wrap">
                    <select className="sel tech" value={profile} onChange={(e) => setProfile(e.target.value)}>
                      <option value="">{tb.profileNone}</option>
                      {profiles.map((p) => <option key={p} value={p}>{p}</option>)}
                    </select>
                    <span className="sel-chev"><Icon name="chevronDown" size={16} /></span>
                  </div>
                </div>
              )}
              {error && <div className="hinttext err"><Icon name="alert" size={13} /> {error}</div>}
            </div>
          )}
        </div>
        <div className="modal-foot">
          {result
            ? <button className="btn btn-primary" onClick={onClose}>{tb.close}</button>
            : <>
                <button className="btn btn-ghost" onClick={onClose} disabled={loading}>{tb.cancel}</button>
                <button className="btn btn-primary" onClick={submit} disabled={!canSubmit}>
                  {loading ? <><span className="spinner" style={{ width: 14, height: 14 }} /> {tb.submitting}</> : <><Icon name="send" size={14} /> {tb.submit}</>}
                </button>
              </>}
        </div>
      </div>
    </div>
  )
}

// ── System health row ─────────────────────────────────────────────────────────

function HealthRow({ metrics, td }) {
  const th = td.health
  const heapUsed = metrics['jvm.heap_used_bytes'] || 0
  const heapMax  = metrics['jvm.heap_max_bytes']  || 0
  const heapMBu  = Math.round(heapUsed / 1024 / 1024)
  const heapMBm  = Math.round(heapMax  / 1024 / 1024)
  const heapPct  = heapMBm > 0 ? Math.round(heapMBu / heapMBm * 100) : 0
  const heapColor = heapPct > 85 ? 'var(--danger)' : heapPct > 65 ? 'var(--warn)' : 'var(--ok)'
  const profiles  = metrics['runner.profiles_available']
  const distCases = metrics['distributed.active_cases']
  const kafkaUp   = metrics['distributed.kafka_available']

  return (
    <div className="health-row">
      <div className="health-chip">
        <span className="health-label">{th.heap}</span>
        <div className="heap-wrap">
          <div className="heap-track"><div className="heap-fill" style={{ width: `${heapPct}%`, background: heapColor }} /></div>
          <span className="tech" style={{ fontSize: 11.5, color: 'var(--text-2)' }}>{heapMBu} / {heapMBm} MB ({heapPct}%)</span>
        </div>
      </div>
      <div className="health-sep" />
      <div className="health-chip">
        <span className="health-label">{th.uptime}</span>
        <span className="tech" style={{ fontSize: 12, color: 'var(--text-2)' }}>{fmtUptime(metrics['jvm.uptime_ms'])}</span>
      </div>
      {profiles != null && (
        <><div className="health-sep" />
        <div className="health-chip">
          <span className="health-label">{th.profiles}</span>
          <span className="tech" style={{ fontSize: 12, color: 'var(--text-2)' }}>{profiles}</span>
        </div></>
      )}
      {kafkaUp && distCases != null && (
        <><div className="health-sep" />
        <div className="health-chip">
          <span className="health-label">{th.distCases}</span>
          <span className="tech" style={{ fontSize: 12, color: distCases > 0 ? 'var(--accent-text)' : 'var(--text-2)' }}>{distCases}</span>
        </div></>
      )}
    </div>
  )
}

// ── Dashboard root ────────────────────────────────────────────────────────────

export function DashboardView({ t, openBrowser, serverProfiles, notify }) {
  const td = t.dashboard
  const [jobs,         setJobs]         = useState(null)
  const [queue,        setQueue]        = useState(null)
  const [agents,       setAgents]       = useState(null)
  const [metrics,      setMetrics]      = useState(null)
  const [selectedId,   setSelectedId]   = useState(null)
  const [logs,         setLogs]         = useState([])
  const [logsLoading,  setLogsLoading]  = useState(false)
  const [activeTab,    setActiveTab]    = useState('runs')
  const [batchOpen,    setBatchOpen]    = useState(false)
  const [healthOpen,   setHealthOpen]   = useState(false)
  const [filterStatus, setFilterStatus] = useState('all')
  const [filterSearch, setFilterSearch] = useState('')
  const [notifPerm,    setNotifPerm]    = useState(() => notifyPermission())
  const [dashToasts,   dashToast]       = useDashToasts()
  const logEsRef = useRef(null)

  // ── Data fetching ─────────────────────────────────────────────────────────

  const fetchJobs = useCallback(async () => {
    try { const r = await apiFetch('/v2/jobs'); if (r.ok) setJobs(await r.json()) } catch (_) {}
  }, [])

  const fetchSupporting = useCallback(async () => {
    await Promise.allSettled([
      apiFetch('/queue').then(r => r.ok && r.json()).then(d => d && setQueue(d)),
      apiFetch('/v2/agents').then(r => r.ok && r.json()).then(d => d && setAgents(d)),
      apiFetch('/metrics').then(r => r.ok && r.json()).then(d => d && setMetrics(d)),
    ])
  }, [])

  useEffect(() => {
    fetchJobs(); fetchSupporting()
    const pollId = setInterval(() => { fetchJobs(); fetchSupporting() }, 10000)
    const es = new EventSource('/dashboard/stream')
    es.addEventListener('jobs-update', () => fetchJobs())
    es.onerror = () => {}
    return () => { clearInterval(pollId); es.close() }
  }, [fetchJobs, fetchSupporting])

  // ── Filter ────────────────────────────────────────────────────────────────

  const filteredJobs = useMemo(() => {
    if (!jobs) return null
    return jobs.filter(job => {
      if (filterStatus !== 'all' && job.status !== filterStatus) return false
      const q = filterSearch.trim().toLowerCase()
      if (!q) return true
      return (job.params?.name || '').toLowerCase().includes(q) || job.id.toLowerCase().includes(q)
    })
  }, [jobs, filterStatus, filterSearch])

  useEffect(() => {
    if (selectedId && filteredJobs && !filteredJobs.find(j => j.id === selectedId)) setSelectedId(null)
  }, [filteredJobs, selectedId])

  // ── Keyboard navigation on runs list ─────────────────────────────────────

  useEffect(() => {
    if (activeTab !== 'runs' || !filteredJobs?.length) return
    const handler = (e) => {
      if (e.target.tagName === 'INPUT' || e.target.tagName === 'SELECT' || e.target.tagName === 'TEXTAREA') return
      if (e.key === 'ArrowDown' || e.key === 'ArrowUp') {
        e.preventDefault()
        const idx = filteredJobs.findIndex(j => j.id === selectedId)
        const next = e.key === 'ArrowDown'
          ? (idx < 0 ? 0 : Math.min(filteredJobs.length - 1, idx + 1))
          : Math.max(0, idx - 1)
        setSelectedId(filteredJobs[next]?.id ?? null)
      }
      if (e.key === 'Escape') setSelectedId(null)
    }
    window.addEventListener('keydown', handler)
    return () => window.removeEventListener('keydown', handler)
  }, [activeTab, filteredJobs, selectedId])

  // ── SSE log stream ────────────────────────────────────────────────────────

  const selectedJob    = jobs?.find(j => j.id === selectedId) ?? null
  const selectedStatus = selectedJob?.status

  useEffect(() => {
    if (logEsRef.current) { logEsRef.current.close(); logEsRef.current = null }
    setLogs([])
    if (!selectedId || selectedStatus !== 'running') { setLogsLoading(false); return }

    const jobName = selectedJob?.params?.name || selectedId.slice(0, 8)
    setLogsLoading(true)
    const es = new EventSource('/run/' + selectedId + '/stream')
    logEsRef.current = es

    es.addEventListener('log', (e) => {
      try { const { line } = JSON.parse(e.data); setLogs(prev => [...prev.slice(-499), line]); setLogsLoading(false) }
      catch (_) {}
    })

    const finish = (outcome) => {
      es.close(); logEsRef.current = null; setLogsLoading(false)
      if (outcome === 'done') {
        sendNotification(td.notifications.jobDone, jobName)
        notify?.(`${jobName} — ${td.notifications.jobDone}`)
      } else if (outcome === 'error') {
        sendNotification(td.notifications.jobFailed, jobName)
        notify?.(`${jobName} — ${td.notifications.jobFailed}`)
      } else {
        sendNotification(td.notifications.jobAborted, jobName)
      }
    }

    es.addEventListener('done',    () => finish('done'))
    es.addEventListener('aborted', () => finish('aborted'))
    es.addEventListener('error',   (e) => {
      try { const d = JSON.parse(e.data); if (d.message) setLogs(prev => [...prev, `[error] ${d.message}`]) } catch (_) {}
      finish('error')
    })
    es.onerror = () => { if (logEsRef.current) { logEsRef.current.close(); logEsRef.current = null; setLogsLoading(false) } }

    return () => es.close()
  }, [selectedId, selectedStatus])   // eslint-disable-line react-hooks/exhaustive-deps

  // ── Actions ───────────────────────────────────────────────────────────────

  const cancelJob = async (id) => {
    try { await apiFetch('/v2/jobs/' + id, { method: 'DELETE' }) } catch (_) {}
    dashToast(td.toasts.cancelled)
    fetchJobs(); fetchSupporting()
  }

  const clearQueue = async () => {
    if (!queue?.length) return
    const count = queue.length
    await Promise.allSettled(queue.map(q => apiFetch('/v2/jobs/' + q.id, { method: 'DELETE' }).catch(() => {})))
    dashToast(`${td.toasts.queueCleared} (${count})`)
    fetchJobs(); fetchSupporting()
  }

  const saveLog = () => {
    if (!logs.length || !selectedJob) return
    const name = (selectedJob.params?.name || selectedJob.id.slice(0, 8)).replace(/[^a-zA-Z0-9-_]/g, '_')
    const blob = new Blob([logs.join('\n')], { type: 'text/plain; charset=utf-8' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url; a.download = `run-${name}-${selectedJob.id.slice(0, 8)}.log`
    document.body.appendChild(a); a.click(); document.body.removeChild(a)
    URL.revokeObjectURL(url)
    dashToast(td.toasts.logSaved)
  }

  const handleNotifBtn = async () => {
    const perm = await requestPermission()
    setNotifPerm(perm)
    if (perm === 'granted') dashToast(td.toasts.notifOn)
  }

  // ── Derived ───────────────────────────────────────────────────────────────

  const activeCount = metrics?.['runner.active_runs'] ?? 0
  const queueCount  = metrics?.['runner.queued_runs']  ?? 0
  const kafkaUp     = metrics?.['distributed.kafka_available'] ?? false

  const TABS = [
    { key: 'runs',   icon: 'activity', label: td.tabs.runs },
    { key: 'queue',  icon: 'layers',   label: `${td.tabs.queue}${queueCount > 0 ? ` (${queueCount})` : ''}` },
    { key: 'agents', icon: 'server',   label: td.tabs.agents },
    { key: 'audit',  icon: 'fileText', label: td.tabs.audit },
  ]

  const tn = td.notifications
  const notifLabel = notifPerm === 'granted' ? tn.granted : notifPerm === 'denied' ? tn.denied : tn.enable
  const notifClass = notifPerm === 'granted' ? 'notif-granted' : notifPerm === 'denied' ? 'notif-denied' : ''

  // ── Render ────────────────────────────────────────────────────────────────

  return (
    <div className="dashboard-root" style={{ position: 'relative' }}>
      {/* Metrics bar */}
      <div className="metrics-bar">
        {metrics ? (
          <>
            <div className="metric-badge">
              <span className="metric-val" style={{ color: activeCount > 0 ? 'var(--accent-text)' : undefined }}>{metrics['runner.active_runs']}</span>
              <span className="metric-label">{td.metrics.active}</span>
            </div>
            <div className="metric-sep" />
            <div className="metric-badge">
              <span className="metric-val" style={{ color: queueCount > 0 ? 'var(--warn)' : undefined }}>{metrics['runner.queued_runs']}</span>
              <span className="metric-label">{td.metrics.queued}</span>
            </div>
            <div className="metric-sep" />
            <div className="metric-badge">
              <span className="metric-val">{metrics['runner.slots_available']}/{metrics['runner.slots_total']}</span>
              <span className="metric-label">{td.metrics.slotsFree}</span>
            </div>
            {kafkaUp && (
              <><div className="metric-sep" />
              <div className="metric-badge">
                <span style={{ color: 'var(--ok)', display: 'inline-flex', alignItems: 'center', gap: 5, fontSize: 12.5, fontWeight: 600 }}>
                  <Icon name="wifi" size={13} /> Kafka
                </span>
              </div></>
            )}
          </>
        ) : <span className="spinner" style={{ marginLeft: 8 }} />}

        <div style={{ marginLeft: 'auto', display: 'flex', gap: 6, alignItems: 'center' }}>
          {canNotify() && (
            <button type="button" className={`btn btn-ghost btn-sm ${notifClass}`}
              onClick={handleNotifBtn} disabled={notifPerm === 'denied'} title={notifLabel}>
              <Icon name={notifPerm === 'granted' ? 'bell' : 'bellOff'} size={13} />
              <span style={{ display: 'none' }}>{notifLabel}</span>
            </button>
          )}
          {metrics && (
            <button type="button" className="btn btn-ghost btn-sm"
              onClick={() => setHealthOpen(o => !o)}
              style={healthOpen ? { color: 'var(--accent-text)', borderColor: 'var(--accent-line)' } : {}}>
              <Icon name="cpu" size={13} /> {td.health.title}
              <Icon name="chevronDown" size={11} style={{ transform: healthOpen ? 'rotate(180deg)' : 'none', transition: 'transform .15s' }} />
            </button>
          )}
          <button type="button" className="btn btn-soft btn-sm" onClick={() => setBatchOpen(true)}>
            <Icon name="package" size={13} /> {td.batch.title}
          </button>
        </div>
      </div>

      {/* System health row */}
      {healthOpen && metrics && <HealthRow metrics={metrics} td={td} />}

      {/* Tab bar */}
      <div className="tab-bar">
        {TABS.map((tab) => (
          <button key={tab.key} type="button" className={`tab ${activeTab === tab.key ? 'active' : ''}`} onClick={() => setActiveTab(tab.key)}>
            <Icon name={tab.icon} size={14} /> {tab.label}
          </button>
        ))}
      </div>

      {/* Runs tab */}
      {activeTab === 'runs' && (
        <div className="runs-layout">
          <div className="runs-list">
            <div className="runs-filter">
              <input className="runs-filter-search" type="search" placeholder={td.filter.search}
                value={filterSearch} onChange={(e) => setFilterSearch(e.target.value)} />
              <select className="runs-filter-status" value={filterStatus} onChange={(e) => setFilterStatus(e.target.value)}>
                <option value="all">{td.filter.all}</option>
                <option value="running">{td.filter.running}</option>
                <option value="pending">{td.filter.pending}</option>
                <option value="completed">{td.filter.completed}</option>
                <option value="failed">{td.filter.failed}</option>
                <option value="cancelled">{td.filter.cancelled}</option>
              </select>
              {jobs && filteredJobs && filteredJobs.length !== jobs.length && (
                <span className="runs-filter-count">{filteredJobs.length}/{jobs.length}</span>
              )}
            </div>
            {!jobs && <div style={{ padding: 20, display: 'flex', gap: 10, color: 'var(--text-muted)', fontSize: 13 }}><span className="spinner" /> Loading…</div>}
            {filteredJobs?.length === 0 && (
              <div className="detail-empty">
                <Icon name="activity" size={28} style={{ color: 'var(--text-muted)', marginBottom: 8 }} />
                <div style={{ color: 'var(--text-muted)', fontSize: 13 }}>{td.noRuns}</div>
              </div>
            )}
            {filteredJobs?.map((job) => (
              <JobRow key={job.id} job={job} selected={selectedId === job.id} onSelect={setSelectedId} />
            ))}
          </div>
          <div className="runs-detail">
            <JobDetail job={selectedJob} logs={logs} logsLoading={logsLoading}
              onCancel={() => cancelJob(selectedId)} onSaveLog={saveLog} td={td} />
          </div>
        </div>
      )}

      {activeTab === 'queue' && (
        <div style={{ padding: '16px 20px', overflowY: 'auto', flex: 1 }}>
          <QueuePanel queue={queue} onCancel={cancelJob} onClearAll={clearQueue} td={td} />
        </div>
      )}
      {activeTab === 'agents' && (
        <div style={{ padding: '16px 20px', overflowY: 'auto', flex: 1 }}>
          <AgentsPanel agents={agents} td={td} />
        </div>
      )}
      {activeTab === 'audit' && (
        <div style={{ padding: '16px 20px', overflowY: 'auto', flex: 1 }}>
          <AuditPanel td={td} />
        </div>
      )}

      <BatchModal open={batchOpen} onClose={() => { setBatchOpen(false); fetchJobs(); fetchSupporting() }}
        profiles={serverProfiles || []} openBrowser={openBrowser} td={td} />

      {/* Local toasts */}
      <DashToasts toasts={dashToasts} />
    </div>
  )
}
