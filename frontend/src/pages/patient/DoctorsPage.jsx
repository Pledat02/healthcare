import { useEffect, useMemo, useState } from 'react'
import api, { unwrap, apiMessage } from '../../lib/api'
import { formatWorkTime } from '../../lib/format'
import { useToast } from '../../components/Toast'
import {
  Button, Card, Field, Input, Select, Textarea, Spinner, EmptyState, PageHeader,
} from '../../components/ui'
import Modal from '../../components/Modal'
import { Stethoscope, Clock, Search } from 'lucide-react'

export default function DoctorsPage() {
  const toast = useToast()
  const [doctors, setDoctors] = useState([])
  const [loading, setLoading] = useState(true)
  const [spec, setSpec] = useState('')
  const [q, setQ] = useState('')
  const [booking, setBooking] = useState(null) // doctor dang dat lich

  useEffect(() => {
    api
      .get('/doctors')
      .then((res) => setDoctors(unwrap(res) || []))
      .catch((e) => toast.error(apiMessage(e)))
      .finally(() => setLoading(false))
  }, []) // eslint-disable-line

  const specializations = useMemo(
    () => [...new Set(doctors.map((d) => d.specialization).filter(Boolean))],
    [doctors],
  )

  const filtered = doctors.filter(
    (d) =>
      (!spec || d.specialization === spec) &&
      (!q || d.fullName?.toLowerCase().includes(q.toLowerCase())),
  )

  if (loading) return <Spinner />

  return (
    <>
      <PageHeader title="Tìm bác sĩ" subtitle="Chọn bác sĩ theo chuyên khoa và đặt lịch khám" />

      <div className="mb-5 flex flex-col gap-3 sm:flex-row">
        <div className="relative flex-1">
          <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
          <Input
            className="pl-9"
            placeholder="Tìm theo tên bác sĩ…"
            value={q}
            onChange={(e) => setQ(e.target.value)}
            aria-label="Tìm bác sĩ"
          />
        </div>
        <Select value={spec} onChange={(e) => setSpec(e.target.value)} className="sm:w-56" aria-label="Chuyên khoa">
          <option value="">Tất cả chuyên khoa</option>
          {specializations.map((s) => (
            <option key={s} value={s}>{s}</option>
          ))}
        </Select>
      </div>

      {filtered.length === 0 ? (
        <EmptyState icon={Stethoscope} title="Không tìm thấy bác sĩ" subtitle="Thử đổi bộ lọc hoặc từ khóa khác" />
      ) : (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {filtered.map((d) => (
            <Card key={d.id} className="flex flex-col p-5">
              <div className="flex items-center gap-3">
                <div className="flex h-12 w-12 items-center justify-center rounded-full bg-[--color-primary-soft] text-[--color-primary]">
                  <Stethoscope className="h-6 w-6" />
                </div>
                <div className="min-w-0">
                  <p className="truncate font-semibold text-[--color-text]">{d.fullName}</p>
                  <p className="truncate text-sm text-[--color-primary]">{d.specialization}</p>
                </div>
              </div>
              <div className="mt-4 flex items-center gap-1.5 text-sm text-[--color-muted]">
                <Clock className="h-4 w-4" />
                {formatWorkTime(d.workStartTime)} – {formatWorkTime(d.workEndTime)}
              </div>
              <Button className="mt-4 w-full" onClick={() => setBooking(d)}>
                Đặt lịch
              </Button>
            </Card>
          ))}
        </div>
      )}

      {booking && (
        <BookingModal doctor={booking} onClose={() => setBooking(null)} toast={toast} />
      )}
    </>
  )
}

function BookingModal({ doctor, onClose, toast }) {
  const today = new Date().toISOString().slice(0, 10)
  const [date, setDate] = useState(today)
  const [time, setTime] = useState('09:00')
  const [reason, setReason] = useState('')
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')

  async function submit(e) {
    e.preventDefault()
    setError('')
    // Ghep ngay + gio (gio dia phuong) -> ISO Instant
    const appointmentTime = new Date(`${date}T${time}:00`).toISOString()
    if (new Date(appointmentTime) <= new Date()) {
      setError('Không thể đặt lịch vào thời điểm trong quá khứ')
      return
    }
    setSaving(true)
    try {
      await api.post('/appointments', { doctorId: doctor.id, appointmentTime, reason })
      toast.success('Đặt lịch thành công! Vui lòng kiểm tra email xác nhận.')
      onClose()
    } catch (err) {
      setError('') // loi nghiep vu hien duoi form
      toast.error(apiMessage(err))
    } finally {
      setSaving(false)
    }
  }

  return (
    <Modal open onClose={onClose} title={`Đặt lịch với ${doctor.fullName}`}>
      <form onSubmit={submit} className="space-y-4">
        <div className="rounded-lg bg-[--color-primary-soft] p-3 text-sm text-[--color-text]">
          <span className="font-medium">{doctor.specialization}</span> · Giờ làm việc{' '}
          {formatWorkTime(doctor.workStartTime)}–{formatWorkTime(doctor.workEndTime)}
        </div>
        <div className="grid grid-cols-2 gap-3">
          <Field label="Ngày khám" required>
            <Input type="date" min={today} value={date} onChange={(e) => setDate(e.target.value)} required />
          </Field>
          <Field label="Giờ khám" required hint="Trong giờ làm việc của bác sĩ">
            <Input type="time" value={time} onChange={(e) => setTime(e.target.value)} required />
          </Field>
        </div>
        <Field label="Lý do khám">
          <Textarea placeholder="Mô tả triệu chứng hoặc lý do khám…" value={reason} onChange={(e) => setReason(e.target.value)} />
        </Field>
        {error && <p className="text-sm text-[--color-danger]" role="alert">{error}</p>}
        <div className="flex justify-end gap-2 pt-2">
          <Button variant="secondary" type="button" onClick={onClose}>Hủy</Button>
          <Button type="submit" loading={saving}>Xác nhận đặt lịch</Button>
        </div>
      </form>
    </Modal>
  )
}
