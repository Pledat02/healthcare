import { useEffect, useState, useCallback } from 'react'
import api, { unwrap, apiMessage } from '../../lib/api'
import { formatTime, toDateInput } from '../../lib/format'
import { useToast } from '../../components/Toast'
import {
  Button, Card, Field, Input, Textarea, Spinner, EmptyState, PageHeader, StatusBadge,
} from '../../components/ui'
import Modal from '../../components/Modal'
import { CalendarCheck, User, Check, ClipboardCheck, FileText, Plus, Trash2 } from 'lucide-react'

export default function SchedulePage() {
  const toast = useToast()
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
        title="Lịch khám"
        subtitle="Danh sách bệnh nhân theo ngày"
        action={
          <Input type="date" value={date} onChange={(e) => setDate(e.target.value)} className="w-auto" aria-label="Chọn ngày" />
        }
      />

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
