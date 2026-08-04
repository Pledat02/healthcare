import { useEffect, useState, useCallback } from 'react'
import api, { unwrap, apiMessage } from '@/shared/lib/api'
import { toDateInput } from '@/shared/lib/format'
import { useToast } from '@/shared/components/Toast'
import { useI18n } from '@/shared/i18n/I18nProvider'
import { Button, Card, Field, Input, Spinner, EmptyState, PageHeader } from '@/shared/ui'
import AppointmentRow from '@/features/appointments/components/AppointmentRow'
import RecordModal from '@/features/appointments/components/RecordModal'
import MonthCalendar from '@/features/appointments/components/MonthCalendar'
import { CalendarCheck, Plus, Trash2, CalendarOff } from 'lucide-react'

const CLINIC_TZ = 'Asia/Ho_Chi_Minh'
const p2 = (n) => String(n).padStart(2, '0')
const clinicYMD = (dt) => new Intl.DateTimeFormat('en-CA', { timeZone: CLINIC_TZ, year: 'numeric', month: '2-digit', day: '2-digit' }).format(new Date(dt))
const ymdLocal = (d) => `${d.getFullYear()}-${p2(d.getMonth() + 1)}-${p2(d.getDate())}`
const addDays = (d, n) => { const x = new Date(d); x.setDate(x.getDate() + n); return x }

export default function SchedulePage() {
  const toast = useToast()
  const { t, lang } = useI18n()
  const [cursor, setCursor] = useState(() => { const d = new Date(); return { y: d.getFullYear(), m: d.getMonth() } }) // thang cua lich
  const [monthAppts, setMonthAppts] = useState([])
  const [upcoming, setUpcoming] = useState([])
  const [loading, setLoading] = useState(true)
  const [acting, setActing] = useState(null)
  const [recording, setRecording] = useState(null)

  // Lich thang (calendar)
  const loadMonth = useCallback(async () => {
    const from = `${cursor.y}-${p2(cursor.m + 1)}-01`
    const to = ymdLocal(new Date(cursor.y, cursor.m + 1, 0)) // ngay cuoi thang
    try {
      const res = await api.get('/appointments/doctors/me', { params: { from, to } })
      setMonthAppts(unwrap(res) || [])
    } catch (e) { toast.error(apiMessage(e)) }
  }, [cursor]) // eslint-disable-line

  // Lich hen sap toi 7 ngay
  const loadUpcoming = useCallback(async () => {
    setLoading(true)
    const today = new Date()
    try {
      const res = await api.get('/appointments/doctors/me', {
        params: { from: ymdLocal(today), to: ymdLocal(addDays(today, 6)) },
      })
      const list = (unwrap(res) || []).slice().sort((a, b) => new Date(a.appointmentTime) - new Date(b.appointmentTime))
      setUpcoming(list)
    } catch (e) { toast.error(apiMessage(e)) } finally { setLoading(false) }
  }, [])

  useEffect(() => { loadMonth() }, [loadMonth])
  useEffect(() => { loadUpcoming() }, [loadUpcoming])

  async function act(id, action, okMsg) {
    setActing(id + action)
    try {
      await api.patch(`/appointments/${id}/${action}`)
      toast.success(okMsg)
      loadUpcoming(); loadMonth()
    } catch (e) { toast.error(apiMessage(e)) } finally { setActing(null) }
  }

  // Nhom lich thang theo ngay (cho calendar)
  const apptsByDay = {}
  monthAppts.forEach((a) => { const k = clinicYMD(a.appointmentTime); (apptsByDay[k] ||= []).push(a) })

  const todayYmd = clinicYMD(new Date())
  const shiftMonth = (delta) => setCursor((c) => { const d = new Date(c.y, c.m + delta, 1); return { y: d.getFullYear(), m: d.getMonth() } })
  const rowProps = { acting, act, onRecord: setRecording, t }

  return (
    <>
      <PageHeader title={t('page.scheduleTitle')} subtitle={t('page.scheduleSubtitle')} />

      <LeaveManager toast={toast} />

      <MonthCalendar
        year={cursor.y} month={cursor.m} apptsByDay={apptsByDay} todayYmd={todayYmd}
        onPrev={() => shiftMonth(-1)} onNext={() => shiftMonth(1)}
        onThis={() => { const d = new Date(); setCursor({ y: d.getFullYear(), m: d.getMonth() }) }}
        t={t} lang={lang}
      />

      <h2 className="mb-4 flex items-center gap-3 text-lg font-extrabold tracking-tight text-text">
        <span className="flex h-10 w-10 items-center justify-center rounded-xl bg-primary-soft text-primary"><CalendarCheck className="h-5 w-5" aria-hidden="true" /></span>{t('schedule.upcoming')}
      </h2>
      {loading ? (
        <Spinner />
      ) : upcoming.length === 0 ? (
        <EmptyState icon={CalendarCheck} title={t('schedule.upcomingEmpty')} subtitle={t('schedule.upcomingEmptySub')} />
      ) : (
        <div className="space-y-3">
          {upcoming.map((a) => <AppointmentRow key={a.id} a={a} withDate {...rowProps} />)}
        </div>
      )}

      {recording && (
        <RecordModal appointment={recording} onClose={() => setRecording(null)} onSaved={() => { loadUpcoming(); loadMonth() }} toast={toast} />
      )}
    </>
  )
}

// Bac si tu quan ly ngay nghi (ca ngay). Slot picker cua benh nhan se an cac ngay nay.
function LeaveManager({ toast }) {
  const { t } = useI18n()
  const [leaves, setLeaves] = useState([])
  const [loading, setLoading] = useState(true)
  const [newDate, setNewDate] = useState('')
  const [reason, setReason] = useState('')
  const [saving, setSaving] = useState(false)
  const [removing, setRemoving] = useState(null)

  const load = useCallback(async () => {
    setLoading(true)
    try {
      setLeaves(unwrap(await api.get('/doctors/me/leaves')) || [])
    } catch (e) { toast.error(apiMessage(e)) } finally { setLoading(false) }
  }, []) // eslint-disable-line

  useEffect(() => { load() }, [load])

  async function add(e) {
    e.preventDefault()
    if (!newDate) return
    setSaving(true)
    try {
      await api.post('/doctors/me/leaves', { leaveDate: newDate, reason })
      toast.success(t('leave.added'))
      setNewDate(''); setReason('')
      load()
    } catch (err) { toast.error(apiMessage(err)) } finally { setSaving(false) }
  }

  async function remove(id) {
    setRemoving(id)
    try {
      await api.delete(`/doctors/me/leaves/${id}`)
      toast.success(t('leave.removed'))
      load()
    } catch (err) { toast.error(apiMessage(err)) } finally { setRemoving(null) }
  }

  const fmt = (ymd) => { const [y, m, d] = ymd.split('-'); return `${d}/${m}/${y}` }
  const today = toDateInput()

  return (
    <Card className="mb-6 p-5 sm:p-6">
      <div className="mb-4 flex items-center gap-3 font-extrabold text-text">
        <span className="flex h-10 w-10 items-center justify-center rounded-xl bg-primary-soft text-primary"><CalendarOff className="h-5 w-5" aria-hidden="true" /></span>{t('leave.title')}
      </div>

      {loading ? (
        <Spinner label={t('common.loading')} />
      ) : leaves.length === 0 ? (
        <p className="text-sm text-muted">{t('leave.none')}</p>
      ) : (
        <ul className="mb-3 flex flex-wrap gap-2">
          {leaves.map((l) => (
            <li key={l.id} className="flex items-center gap-2 rounded-full bg-slate-100 py-1 pl-3 pr-1 text-sm">
              <span className="font-medium text-text">{fmt(l.leaveDate)}</span>
              {l.reason && <span className="text-muted">· {l.reason}</span>}
              <button
                type="button"
                onClick={() => remove(l.id)}
                disabled={removing === l.id}
                className="flex h-6 w-6 items-center justify-center rounded-full text-slate-400 hover:bg-white hover:text-danger disabled:opacity-50"
                aria-label={t('leave.removeAria')}
              >
                <Trash2 className="h-3.5 w-3.5" />
              </button>
            </li>
          ))}
        </ul>
      )}

      <form onSubmit={add} className="flex flex-wrap items-end gap-2">
        <Field label={t('leave.dateLabel')}>
          <Input type="date" min={today} value={newDate} onChange={(e) => setNewDate(e.target.value)} className="w-auto" required />
        </Field>
        <div className="flex-1">
          <Field label={t('leave.reasonLabel')}>
            <Input value={reason} onChange={(e) => setReason(e.target.value)} placeholder={t('leave.reasonPlaceholder')} />
          </Field>
        </div>
        <Button type="submit" loading={saving} disabled={!newDate}>
          <Plus className="h-4 w-4" /> {t('leave.add')}
        </Button>
      </form>
    </Card>
  )
}
