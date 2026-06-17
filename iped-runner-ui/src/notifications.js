/** Thin wrapper around the browser Notification API. */

export const canNotify = () => typeof Notification !== 'undefined'

export function notifyPermission() {
  return canNotify() ? Notification.permission : 'denied'
}

export async function requestPermission() {
  if (!canNotify()) return 'denied'
  if (Notification.permission !== 'default') return Notification.permission
  return Notification.requestPermission()
}

export function sendNotification(title, body) {
  if (!canNotify() || Notification.permission !== 'granted') return
  try {
    new Notification(title, { body, tag: 'iped-runner', icon: '/favicon.ico' })
  } catch (_) {}
}
