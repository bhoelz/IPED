const KEY_STORAGE = 'runner.apiKey'

export function getApiKey() {
  try { return sessionStorage.getItem(KEY_STORAGE) || '' } catch (_) { return '' }
}

export function setApiKey(key) {
  try {
    if (key) sessionStorage.setItem(KEY_STORAGE, key)
    else sessionStorage.removeItem(KEY_STORAGE)
  } catch (_) {}
}

let _onUnauthorized = null

export function setUnauthorizedHandler(fn) {
  _onUnauthorized = fn
}

/**
 * Drop-in replacement for `fetch` that:
 *  - Attaches `X-Api-Key` when a key is stored in sessionStorage
 *  - Clears the key and calls the unauthorized handler on 401
 *
 * No-op difference in open-access mode (no key configured on the server).
 */
export async function apiFetch(url, options = {}) {
  const key = getApiKey()
  const headers = { ...(options.headers || {}) }
  if (key) headers['X-Api-Key'] = key

  const resp = await fetch(url, { ...options, headers })

  if (resp.status === 401) {
    setApiKey('')
    _onUnauthorized?.()
  }

  return resp
}
