const TZ = 'Asia/Ho_Chi_Minh'

// Instant/ISO -> "09:30, 15/08/2026"
export function formatDateTime(iso) {
  if (!iso) return '—'
  return new Intl.DateTimeFormat('vi-VN', {
    timeZone: TZ,
    hour: '2-digit',
    minute: '2-digit',
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
  }).format(new Date(iso))
}

export function formatTime(iso) {
  if (!iso) return '—'
  return new Intl.DateTimeFormat('vi-VN', {
    timeZone: TZ,
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(iso))
}

export function formatDate(iso) {
  if (!iso) return '—'
  return new Intl.DateTimeFormat('vi-VN', {
    timeZone: TZ,
    weekday: 'long',
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
  }).format(new Date(iso))
}

// "08:00:00" (LocalTime) -> "08:00"
export function formatWorkTime(t) {
  if (!t) return '—'
  return t.slice(0, 5)
}

// yyyy-MM-dd cho input date / query
export function toDateInput(d = new Date()) {
  return d.toISOString().slice(0, 10)
}
