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
    <Card className="flex flex-wrap items-center gap-4 p-4">
      <div className="flex min-w-[92px] flex-col items-center rounded-lg bg-primary-soft px-3 py-2 text-primary">
        <span className="text-lg font-bold tabular-nums leading-none">{formatTime(a.appointmentTime)}</span>
        {withDate && (
          <span className="mt-1 text-center text-[11px] leading-tight text-primary/80">
            {dow}<br />{ddmmyyyy(ymd)}
          </span>
        )}
      </div>
      <div className="min-w-0 flex-1">
        <p className="flex items-center gap-1.5 font-semibold text-text">
          <User className="h-4 w-4 text-muted" />{a.patientName || t('schedule.patientFallback')}
        </p>
        <p className="text-sm text-muted">{a.patientPhone} {a.reason && `· ${a.reason}`}</p>
      </div>
      <StatusBadge status={a.status} />
      <div className="flex gap-2">
        {a.status === 'PENDING' && (
          <Button loading={acting === a.id + 'confirm'} onClick={() => act(a.id, 'confirm', t('schedule.confirmed'))}>
            <Check className="h-4 w-4" /> {t('schedule.confirm')}
          </Button>
        )}
        {a.status === 'CONFIRMED' && (
          <Button loading={acting === a.id + 'complete'} disabled={notYet} title={notYet ? t('schedule.tooEarly') : ''} onClick={() => act(a.id, 'complete', t('schedule.completed'))}>
            <ClipboardCheck className="h-4 w-4" /> {t('schedule.complete')}
          </Button>
        )}
        {a.status === 'COMPLETED' && (
          <Button variant="secondary" onClick={() => onRecord(a)}>
            <FileText className="h-4 w-4" /> {t('schedule.writeRecord')}
          </Button>
        )}
      </div>
    </Card>
  )
}
