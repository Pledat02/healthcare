import { useEffect, useState, useCallback } from 'react'
import api, { unwrap, apiMessage } from '@/shared/lib/api'
import { formatTime, toDateInput } from '@/shared/lib/format'
import { useToast } from '@/shared/components/Toast'
import { useI18n } from '@/shared/i18n/I18nProvider'
import {
  Button, Card, Field, Input, Textarea, Spinner, EmptyState, PageHeader, StatusBadge,
} from '@/shared/ui'
import Modal from '@/shared/components/Modal'
import {
  CalendarCheck, User, Check, ClipboardCheck, FileText, Plus, Trash2, CalendarOff,
  ChevronLeft, ChevronRight, History, CalendarRange,
} from 'lucide-react'

const CLINIC_TZ = 'Asia/Ho_Chi_Minh'
const WEEKDAY_KEYS = ['SUNDAY', 'MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY']

// Date -> "YYYY-MM-DD" theo gio phong kham
function clinicYMD(dt) {
  return new Intl.DateTimeFormat('en-CA', { timeZone: CLINIC_TZ, year: 'numeric', month: '2-digit', day: '2-digit' }).format(new Date(dt))
}
// Thu Hai cua tuan chua 'date' (Date, 00:00 local)
function mondayOf(date) {
  const d = new Date(date); const off = (d.getDay() + 6) % 7 // 0=Mon..6=Sun
  d.setDate(d.getDate() - off); d.setHours(0, 0, 0, 0); return d
}
function addDays(date, n) { const d = new Date(date); d.setDate(d.getDate() + n); return d }
function ymd(d) { const p = (x) => String(x).padStart(2, '0'); return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}` }
function ddmm(ymdStr) { const [, m, dd] = ymdStr.split('-'); return `${dd}/${m}` }

export default function SchedulePage() {
  const toast = useToast()
  const { t } = useI18n()
  const [view, setView] = useState('week')                 // 'week' | 'history'
  const [weekStart, setWeekStart] = useState(() => mondayOf(new Date()))
  const [weekItems, setWeekItems] = useState([])
  const [loadingWeek, setLoadingWeek] = useState(true)
  const [history, setHistory] = useState({ content: [], totalPages: 0 })
  const [histPage, setHistPage] = useState(0)
  const [loadingHist, setLoadingHist] = useState(false)
  const [acting, setActing] = useState(null)
  const [recording, setRecording] = useState(null)

  const weekEnd = addDays(weekStart, 6)

  const loadWeek = useCallback(async () => {
    setLoadingWeek(true)
    try {
      const res = await api.get('/appointments/doctors/me', { params: { from: ymd(weekStart), to: ymd(weekEnd) } })
      setWeekItems(unwrap(res) || [])
    } catch (e) { toast.error(apiMessage(e)) } finally { setLoadingWeek(false) }
  }, [weekStart]) // eslint-disable-line

  const loadHistory = useCallback(async () => {
    setLoadingHist(true)
    try {
      const res = await api.get('/appointments/doctors/me/history', { params: { page: histPage, size: 20 } })
      setHistory(unwrap(res) || { content: [], totalPages: 0 })
    } catch (e) { toast.error(apiMessage(e)) } finally { setLoadingHist(false) }
  }, [histPage]) // eslint-disable-line

  useEffect(() => { if (view === 'week') loadWeek() }, [view, loadWeek])
  useEffect(() => { if (view === 'history') loadHistory() }, [view, loadHistory])

  async function act(id, action, okMsg) {
    setActing(id + action)
    try {
      await api.patch(`/appointments/${id}/${action}`)
      toast.success(okMsg)
      if (view === 'week') loadWeek(); else loadHistory()
    } catch (e) { toast.error(apiMessage(e)) } finally { setActing(null) }
  }

  // Nhom lich tuan theo ngay
  const byDay = {}
  weekItems.forEach((a) => { const k = clinicYMD(a.appointmentTime); (byDay[k] ||= []).push(a) })
  Object.values(byDay).forEach((arr) => arr.sort((a, b) => new Date(a.appointmentTime) - new Date(b.appointmentTime)))
  const days = Array.from({ length: 7 }, (_, i) => ymd(addDays(weekStart, i)))
  const todayY = clinicYMD(new Date())
  const rowProps = { acting, act, onRecord: setRecording, t }

  return (
    <>
      <PageHeader title={t('page.scheduleTitle')} subtitle={t('page.scheduleSubtitle')} />

      <LeaveManager toast={toast} />

      <div className="mb-4 inline-flex rounded-lg border border-border bg-surface p-1">
        <TabBtn active={view === 'week'} onClick={() => setView('week')} icon={CalendarRange} label={t('schedule.tabWeek')} />
        <TabBtn active={view === 'history'} onClick={() => setView('history')} icon={History} label={t('schedule.tabHistory')} />
      </div>

      {view === 'week' ? (
        <>
          <div className="mb-4 flex items-center justify-between gap-3">
            <Button variant="secondary" onClick={() => setWeekStart((d) => addDays(d, -7))} aria-label="prev week"><ChevronLeft className="h-4 w-4" /></Button>
            <div className="text-center">
              <p className="text-sm font-semibold text-text">{ddmm(ymd(weekStart))} – {ddmm(ymd(weekEnd))}</p>
              <button className="text-xs text-primary hover:underline" onClick={() => setWeekStart(mondayOf(new Date()))}>{t('schedule.thisWeek')}</button>
            </div>
            <Button variant="secondary" onClick={() => setWeekStart((d) => addDays(d, 7))} aria-label="next week"><ChevronRight className="h-4 w-4" /></Button>
          </div>

          {loadingWeek ? <Spinner label={t('schedule.weekLoading')} /> : (
            <div className="space-y-4">
              {days.map((dk) => {
                const appts = byDay[dk] || []
                const dow = WEEKDAY_KEYS[new Date(dk + 'T00:00:00').getDay()]
                return (
                  <div key={dk}>
                    <div className={`mb-2 flex items-center gap-2 text-sm font-semibold ${dk === todayY ? 'text-primary' : 'text-text'}`}>
                      {t(`weekday.${dow}`)}, {ddmm(dk)}
                      {dk === todayY && <span className="rounded-full bg-primary-soft px-2 py-0.5 text-xs text-primary">{t('schedule.thisWeek')}</span>}
                    </div>
                    {appts.length === 0 ? (
                      <p className="pl-1 text-sm text-muted">{t('schedule.noApptsDay')}</p>
                    ) : (
                      <div className="space-y-2">{appts.map((a) => <AppointmentRow key={a.id} a={a} {...rowProps} />)}</div>
                    )}
                  </div>
                )
              })}
            </div>
          )}
        </>
      ) : (
        loadingHist ? <Spinner /> : history.content.length === 0 ? (
          <EmptyState icon={History} title={t('schedule.historyEmpty')} subtitle={t('schedule.historyEmptySub')} />
        ) : (
          <>
            <div className="space-y-2">{history.content.map((a) => <AppointmentRow key={a.id} a={a} withDate {...rowProps} />)}</div>
            {history.totalPages > 1 && (
              <div className="mt-4 flex items-center justify-center gap-3">
                <Button variant="secondary" disabled={histPage === 0} onClick={() => setHistPage((p) => p - 1)}><ChevronLeft className="h-4 w-4" /> {t('common.prev')}</Button>
                <span className="text-sm text-muted">{histPage + 1}/{history.totalPages}</span>
                <Button variant="secondary" disabled={histPage >= history.totalPages - 1} onClick={() => setHistPage((p) => p + 1)}>{t('common.next')} <ChevronRight className="h-4 w-4" /></Button>
              </div>
            )}
          </>
        )
      )}

      {recording && (
        <RecordModal appointment={recording} onClose={() => setRecording(null)} onSaved={() => (view === 'week' ? loadWeek() : loadHistory())} toast={toast} />
      )}
    </>
  )
}

function TabBtn({ active, onClick, icon: Icon, label }) {
  return (
    <button onClick={onClick} className={`flex items-center gap-1.5 rounded-md px-3 py-1.5 text-sm font-medium transition-colors ${active ? 'bg-primary text-white' : 'text-muted hover:text-text'}`}>
      <Icon className="h-4 w-4" /> {label}
    </button>
  )
}

// 1 dong lich hen (dung o ca tuan + lich su). withDate: hien them ngay (lich su).
function AppointmentRow({ a, acting, act, onRecord, t, withDate }) {
  const notYet = new Date(a.appointmentTime) > new Date()   // chua toi gio hen -> chua duoc hoan thanh
  return (
    <Card className="flex flex-wrap items-center gap-4 p-4">
      <div className="flex flex-col items-center rounded-lg bg-primary-soft px-3 py-2 text-primary">
        <span className="text-lg font-bold tabular-nums leading-none">{formatTime(a.appointmentTime)}</span>
        {withDate && <span className="mt-0.5 text-xs">{ddmm(clinicYMD(a.appointmentTime))}</span>}
      </div>
      <div className="min-w-0 flex-1">
        <p className="flex items-center gap-1.5 font-semibold text-text"><User className="h-4 w-4 text-muted" />{a.patientName || t('schedule.patientFallback')}</p>
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
    } catch (e) {
      toast.error(apiMessage(e))
    } finally {
      setLoading(false)
    }
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
    } catch (err) {
      toast.error(apiMessage(err))
    } finally {
      setSaving(false)
    }
  }

  async function remove(id) {
    setRemoving(id)
    try {
      await api.delete(`/doctors/me/leaves/${id}`)
      toast.success(t('leave.removed'))
      load()
    } catch (err) {
      toast.error(apiMessage(err))
    } finally {
      setRemoving(null)
    }
  }

  const fmt = (ymd) => { const [y, m, d] = ymd.split('-'); return `${d}/${m}/${y}` }
  const today = toDateInput()

  return (
    <Card className="mb-5 p-4">
      <div className="mb-3 flex items-center gap-2 font-semibold text-text">
        <CalendarOff className="h-4 w-4 text-primary" /> {t('leave.title')}
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

function RecordModal({ appointment, onClose, onSaved, toast }) {
  const { t } = useI18n()
  const [diagnosis, setDiagnosis] = useState('')
  const [notes, setNotes] = useState('')
  const [items, setItems] = useState([{ medicineName: '', dosage: '', quantity: 1, instruction: '' }])
  const [saving, setSaving] = useState(false)

  const setItem = (i, k) => (e) =>
    setItems((arr) => arr.map((it, idx) => (idx === i ? { ...it, [k]: k === 'quantity' ? +e.target.value : e.target.value } : it)))
  const addItem = () => setItems((a) => [...a, { medicineName: '', dosage: '', quantity: 1, instruction: '' }])
  const removeItem = (i) => setItems((a) => a.filter((_, idx) => idx !== i))

  async function submit(e) {
    e.preventDefault()
    setSaving(true)
    try {
      await api.post('/medical-records', {
        appointmentId: appointment.id,
        diagnosis,
        notes,
        prescriptionItems: items.filter((it) => it.medicineName.trim()),
      })
      toast.success(t('record.saved'))
      onClose()
      onSaved()
    } catch (err) {
      toast.error(apiMessage(err))
    } finally {
      setSaving(false)
    }
  }

  return (
    <Modal open onClose={onClose} title={t('record.title')}>
      <form onSubmit={submit} className="space-y-4">
        <Field label={t('record.diagnosis')} required>
          <Textarea value={diagnosis} onChange={(e) => setDiagnosis(e.target.value)} required placeholder={t('record.diagnosisPlaceholder')} />
        </Field>
        <Field label={t('record.notes')}>
          <Textarea value={notes} onChange={(e) => setNotes(e.target.value)} placeholder={t('record.notesPlaceholder')} />
        </Field>
        <div>
          <div className="mb-2 flex items-center justify-between">
            <span className="text-sm font-medium text-text">{t('record.prescription')}</span>
            <Button type="button" variant="ghost" onClick={addItem} className="text-primary">
              <Plus className="h-4 w-4" /> {t('record.addMedicine')}
            </Button>
          </div>
          <div className="space-y-2">
            {items.map((it, i) => (
              <div key={i} className="grid grid-cols-12 gap-2">
                <Input className="col-span-4" placeholder={t('record.medicineName')} value={it.medicineName} onChange={setItem(i, 'medicineName')} />
                <Input className="col-span-3" placeholder={t('record.dose')} value={it.dosage} onChange={setItem(i, 'dosage')} />
                <Input className="col-span-2" type="number" min="1" value={it.quantity} onChange={setItem(i, 'quantity')} aria-label={t('record.qtyAria')} />
                <Input className="col-span-2" placeholder={t('record.usage')} value={it.instruction} onChange={setItem(i, 'instruction')} />
                <button type="button" onClick={() => removeItem(i)} className="col-span-1 flex items-center justify-center text-slate-400 hover:text-danger" aria-label={t('record.removeAria')}>
                  <Trash2 className="h-4 w-4" />
                </button>
              </div>
            ))}
          </div>
        </div>
        <div className="flex justify-end gap-2 pt-2">
          <Button variant="secondary" type="button" onClick={onClose}>{t('common.cancel')}</Button>
          <Button type="submit" loading={saving}>{t('record.save')}</Button>
        </div>
      </form>
    </Modal>
  )
}
