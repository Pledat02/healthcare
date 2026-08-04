import { formatTime } from '@/shared/lib/format'
import { Button, Card, StatusBadge } from '@/shared/ui'
import { User, Check, ClipboardCheck, FileText } from 'lucide-react'

const CLINIC_TZ = 'Asia/Ho_Chi_Minh'
const WD = ['SUNDAY', 'MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY']

// Instant -> "YYYY-MM-DD" theo gio phong kham
function clinicYMD(dt) {
  return new Intl.DateTimeFormat('en-CA', { timeZone: CLINIC_TZ, year: 'numeric', month: '2-digit', day: '2-digit' }).format(new Date(dt))
}
function ddmmyyyy(ymd) { const [y, m, d] = ymd.split('-'); return `${d}/${m}/${y}` }

/**
 * 1 dong lich hen cho phia BAC SI. Dung o Lich kham (sap toi) + Lich su.
 * withDate: ghi ro NGAY (thu, dd/mm/yyyy) canh gio.
 * Nut Hoan thanh bi disable + tooltip khi chua toi gio hen (notYet).
 */
export default function AppointmentRow({ a, acting, act, onRecord, t, withDate }) {
  const ymd = clinicYMD(a.appointmentTime)
  const dow = t(`weekday.${WD[new Date(ymd + 'T00:00:00').getDay()]}`)
  const notYet = new Date(a.appointmentTime) > new Date()   // chua toi gio hen -> chua duoc hoan thanh
  return (
    <Card className="interactive-card flex flex-wrap items-center gap-4 p-5">
      <div className="flex min-w-[96px] flex-col items-center rounded-2xl bg-gradient-to-br from-teal-500 to-cyan-600 px-3 py-3 text-white shadow-lg shadow-teal-700/15">
        <span className="text-lg font-black tabular-nums leading-none">{formatTime(a.appointmentTime)}</span>
        {withDate && (
          <span className="mt-1.5 text-center text-[11px] font-semibold leading-tight text-white/80">
            {dow}<br />{ddmmyyyy(ymd)}
          </span>
        )}
      </div>
      <div className="min-w-0 flex-1">
        <p className="flex items-center gap-1.5 font-extrabold text-text">
          <User className="h-4 w-4 text-primary" aria-hidden="true" />{a.patientName || t('schedule.patientFallback')}
        </p>
        <p className="mt-1 text-sm text-muted">{a.patientPhone} {a.reason && `· ${a.reason}`}</p>
      </div>
      <StatusBadge status={a.status} />
      <div className="flex w-full gap-2 border-t border-border pt-3 sm:w-auto sm:border-0 sm:pt-0">
        {a.status === 'PENDING' && (
          <Button loading={acting === a.id + 'confirm'} onClick={() => act(a.id, 'confirm', t('schedule.confirmed'))}>
            <Check className="h-4 w-4" aria-hidden="true" /> {t('schedule.confirm')}
          </Button>
        )}
        {a.status === 'CONFIRMED' && (
          <Button loading={acting === a.id + 'complete'} disabled={notYet} title={notYet ? t('schedule.tooEarly') : ''} onClick={() => act(a.id, 'complete', t('schedule.completed'))}>
            <ClipboardCheck className="h-4 w-4" aria-hidden="true" /> {t('schedule.complete')}
          </Button>
        )}
        {a.status === 'COMPLETED' && (
          <Button variant="secondary" onClick={() => onRecord(a)}>
            <FileText className="h-4 w-4" aria-hidden="true" /> {t('schedule.writeRecord')}
          </Button>
        )}
      </div>
    </Card>
  )
}
