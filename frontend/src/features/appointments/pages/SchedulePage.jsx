import { useEffect, useState, useCallback } from 'react'
import api, { unwrap, apiMessage } from '@/shared/lib/api'
import { formatTime, toDateInput } from '@/shared/lib/format'
import { useToast } from '@/shared/components/Toast'
import { useI18n } from '@/shared/i18n/I18nProvider'
import {
  Button, Card, Field, Input, Textarea, Spinner, EmptyState, PageHeader, StatusBadge,
} from '@/shared/ui'
import Modal from '@/shared/components/Modal'
import { CalendarCheck, User, Check, ClipboardCheck, FileText, Plus, Trash2, CalendarOff } from 'lucide-react'

export default function SchedulePage() {
  const toast = useToast()
  const { t } = useI18n()
  const [date, setDate] = useState(toDateInput())
  const [items, setItems] = useState([])
  const [loading, setLoading] = useState(true)
  const [acting, setActing] = useState(null)
  const [recording, setRecording] = useState(null) // appointment dang ghi ho so

  const load = useCallback(async () => {
    setLoading(true)
    try {
      const res = await api.get('/appointments/doctors/me', { params: { date } })
      const list = (unwrap(res) || []).slice().sort((a, b) => new Date(a.appointmentTime) - new Date(b.appointmentTime))
      // Ten/SDT benh nhan da duoc appointment-service lam giau san (khong goi /patients/batch nua)
      setItems(list)
    } catch (e) {
      toast.error(apiMessage(e))
    } finally {
      setLoading(false)
    }
  }, [date]) // eslint-disable-line

  useEffect(() => { load() }, [load])

  async function act(id, action, okMsg) {
    setActing(id + action)
    try {
      await api.patch(`/appointments/${id}/${action}`)
      toast.success(okMsg)
      load()
    } catch (e) {
      toast.error(apiMessage(e))
    } finally {
      setActing(null)
    }
  }

  return (
    <>
      <PageHeader
        title={t('page.scheduleTitle')}
        subtitle={t('page.scheduleSubtitle')}
        action={
          <Input type="date" value={date} onChange={(e) => setDate(e.target.value)} className="w-auto" aria-label="Chọn ngày" />
        }
      />

      <LeaveManager toast={toast} />

      {loading ? (
        <Spinner />
      ) : items.length === 0 ? (
        <EmptyState icon={CalendarCheck} title="Không có lịch hẹn" subtitle="Chọn ngày khác để xem lịch khám" />
      ) : (
        <div className="space-y-3">
          {items.map((a) => {
            return (
              <Card key={a.id} className="flex flex-wrap items-center gap-4 p-4">
                <div className="flex flex-col items-center rounded-lg bg-primary-soft px-3 py-2 text-primary">
                  <span className="text-lg font-bold tabular-nums leading-none">{formatTime(a.appointmentTime)}</span>
                </div>
                <div className="min-w-0 flex-1">
                  <p className="flex items-center gap-1.5 font-semibold text-text">
                    <User className="h-4 w-4 text-muted" />
                    {a.patientName || 'Bệnh nhân'}
                  </p>
                  <p className="text-sm text-muted">
                    {a.patientPhone} {a.reason && `· ${a.reason}`}
                  </p>
                </div>
                <StatusBadge status={a.status} />
                <div className="flex gap-2">
                  {a.status === 'PENDING' && (
                    <Button loading={acting === a.id + 'confirm'} onClick={() => act(a.id, 'confirm', 'Đã xác nhận lịch')}>
                      <Check className="h-4 w-4" /> Xác nhận
                    </Button>
                  )}
                  {a.status === 'CONFIRMED' && (
                    <Button loading={acting === a.id + 'complete'} onClick={() => act(a.id, 'complete', 'Đã hoàn thành buổi khám')}>
                      <ClipboardCheck className="h-4 w-4" /> Hoàn thành
                    </Button>
                  )}
                  {a.status === 'COMPLETED' && (
                    <Button variant="secondary" onClick={() => setRecording(a)}>
                      <FileText className="h-4 w-4" /> Ghi hồ sơ
                    </Button>
                  )}
                </div>
              </Card>
            )
          })}
        </div>
      )}

      {recording && (
        <RecordModal appointment={recording} onClose={() => setRecording(null)} onSaved={load} toast={toast} />
      )}
    </>
  )
}

// Bac si tu quan ly ngay nghi (ca ngay). Slot picker cua benh nhan se an cac ngay nay.
function LeaveManager({ toast }) {
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
      toast.success('Đã đăng ký ngày nghỉ')
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
      toast.success('Đã xóa ngày nghỉ')
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
        <CalendarOff className="h-4 w-4 text-primary" /> Ngày nghỉ của tôi
      </div>

      {loading ? (
        <Spinner label="Đang tải…" />
      ) : leaves.length === 0 ? (
        <p className="text-sm text-muted">Chưa đăng ký ngày nghỉ nào sắp tới.</p>
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
                aria-label="Xóa ngày nghỉ"
              >
                <Trash2 className="h-3.5 w-3.5" />
              </button>
            </li>
          ))}
        </ul>
      )}

      <form onSubmit={add} className="flex flex-wrap items-end gap-2">
        <Field label="Nghỉ ngày">
          <Input type="date" min={today} value={newDate} onChange={(e) => setNewDate(e.target.value)} className="w-auto" required />
        </Field>
        <div className="flex-1">
          <Field label="Lý do (tùy chọn)">
            <Input value={reason} onChange={(e) => setReason(e.target.value)} placeholder="Ví dụ: nghỉ phép, đi hội thảo…" />
          </Field>
        </div>
        <Button type="submit" loading={saving} disabled={!newDate}>
          <Plus className="h-4 w-4" /> Thêm
        </Button>
      </form>
    </Card>
  )
}

function RecordModal({ appointment, onClose, onSaved, toast }) {
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
      toast.success('Đã lưu hồ sơ khám')
      onClose()
      onSaved()
    } catch (err) {
      toast.error(apiMessage(err))
    } finally {
      setSaving(false)
    }
  }

  return (
    <Modal open onClose={onClose} title="Ghi hồ sơ khám">
      <form onSubmit={submit} className="space-y-4">
        <Field label="Chẩn đoán" required>
          <Textarea value={diagnosis} onChange={(e) => setDiagnosis(e.target.value)} required placeholder="Kết luận chẩn đoán…" />
        </Field>
        <Field label="Ghi chú">
          <Textarea value={notes} onChange={(e) => setNotes(e.target.value)} placeholder="Dặn dò, lời khuyên…" />
        </Field>
        <div>
          <div className="mb-2 flex items-center justify-between">
            <span className="text-sm font-medium text-text">Đơn thuốc</span>
            <Button type="button" variant="ghost" onClick={addItem} className="text-primary">
              <Plus className="h-4 w-4" /> Thêm thuốc
            </Button>
          </div>
          <div className="space-y-2">
            {items.map((it, i) => (
              <div key={i} className="grid grid-cols-12 gap-2">
                <Input className="col-span-4" placeholder="Tên thuốc" value={it.medicineName} onChange={setItem(i, 'medicineName')} />
                <Input className="col-span-3" placeholder="Liều" value={it.dosage} onChange={setItem(i, 'dosage')} />
                <Input className="col-span-2" type="number" min="1" value={it.quantity} onChange={setItem(i, 'quantity')} aria-label="Số lượng" />
                <Input className="col-span-2" placeholder="Cách dùng" value={it.instruction} onChange={setItem(i, 'instruction')} />
                <button type="button" onClick={() => removeItem(i)} className="col-span-1 flex items-center justify-center text-slate-400 hover:text-danger" aria-label="Xóa">
                  <Trash2 className="h-4 w-4" />
                </button>
              </div>
            ))}
          </div>
        </div>
        <div className="flex justify-end gap-2 pt-2">
          <Button variant="secondary" type="button" onClick={onClose}>Hủy</Button>
          <Button type="submit" loading={saving}>Lưu hồ sơ</Button>
        </div>
      </form>
    </Modal>
  )
}
