import { useState } from 'react'
import { Icon } from './icons.jsx'
import { setApiKey } from './api.js'

export function ApiKeyModal({ open, onConnect, t }) {
  const [key, setKey] = useState('')
  const [error, setError] = useState(null)
  const [loading, setLoading] = useState(false)

  const ta = t.auth

  const connect = async () => {
    const k = key.trim()
    if (!k) return
    setLoading(true); setError(null)
    try {
      const resp = await fetch('/metrics', { headers: { 'X-Api-Key': k } })
      if (resp.ok) {
        setApiKey(k)
        setKey('')
        onConnect()
      } else {
        setError(ta.badKey)
      }
    } catch (_) {
      setError(ta.connError)
    }
    setLoading(false)
  }

  if (!open) return null

  return (
    <div className="overlay" style={{ zIndex: 200 }}>
      <div className="modal" onClick={(e) => e.stopPropagation()} style={{ maxWidth: 400 }}>
        <div className="modal-head">
          <span style={{ color: 'var(--accent)' }}><Icon name="lock" size={22} /></span>
          <div style={{ flex: 1 }}>
            <div className="modal-title">{ta.title}</div>
            <div className="modal-sub">{ta.subtitle}</div>
          </div>
        </div>

        <div className="modal-body">
          <div className="field">
            <label className="field-label">{ta.keyLabel}</label>
            <input
              className="inp tech"
              type="password"
              value={key}
              placeholder={ta.keyPh}
              autoFocus
              onChange={(e) => { setKey(e.target.value); setError(null) }}
              onKeyDown={(e) => e.key === 'Enter' && connect()}
            />
          </div>
          {error && (
            <div className="hinttext err" style={{ marginTop: 10 }}>
              <Icon name="alert" size={13} /> {error}
            </div>
          )}
        </div>

        <div className="modal-foot">
          <button
            className="btn btn-primary"
            onClick={connect}
            disabled={loading || !key.trim()}
          >
            {loading
              ? <span className="spinner" style={{ width: 14, height: 14 }} />
              : <Icon name="unlock" size={14} />}
            {ta.connect}
          </button>
        </div>
      </div>
    </div>
  )
}
