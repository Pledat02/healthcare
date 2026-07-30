export const CLINIC_TZ = 'Asia/Ho_Chi_Minh'
const TZ = CLINIC_TZ
// Viet Nam co dinh UTC+7, KHONG co DST -> ghep offset thang la an toan, khong phu thuoc mui gio trinh duyet.
const CLINIC_OFFSET = '+07:00'

// Hom nay theo GIO PHONG KHAM -> "YYYY-MM-DD" (en-CA cho dinh dang ISO)
export function todayInClinic() {
  return new Intl.DateTimeFormat('en-CA', { timeZone: CLINIC_TZ }).format(new Date())
}

// Ghep ngay + gio (hieu la GIO PHONG KHAM) -> Instant ISO (UTC). Dung khi dat/doi lich.
export function clinicDateTimeToIso(date, time) {
  return new Date(`${date}T${time}:00${CLINIC_OFFSET}`).toISOString()
}

// Instant ISO -> "HH:MM" theo GIO PHONG KHAM (de so khop voi slot), bat ke mui gio trinh duyet.
export function instantToClinicHHMM(iso) {
  return new Intl.DateTimeFormat('en-GB', {
    timeZone: CLINIC_TZ, hour: '2-digit', minute: '2-digit', hour12: false,
  }).format(new Date(iso))
}

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

// yyyy-MM-dd cho input date / query, theo GIO PHONG KHAM (khong dung UTC/mui gio trinh duyet)
export function toDateInput(d = new Date()) {
  return new Intl.DateTimeFormat('en-CA', { timeZone: CLINIC_TZ }).format(d)
}
