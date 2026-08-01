import { useEffect, useMemo, useState } from 'react'
import api, { unwrap, apiMessage } from '@/shared/lib/api'
import { formatWorkTime, todayInClinic, clinicDateTimeToIso, instantToClinicHHMM } from '@/shared/lib/format'
import { useToast } from '@/shared/components/Toast'
import { useI18n } from '@/shared/i18n/I18nProvider'
import {
  Button, Card, Field, Input, Select, Textarea, Spinner, EmptyState, PageHeader, StarRating,
} from '@/shared/ui'
import Modal from '@/shared/components/Modal'
import { Stethoscope, Clock, Search, ChevronLeft, ChevronRight } from 'lucide-react'

const PAGE_SIZE = 6

export default function DoctorsPage() {
  const toast = useToast()
  const { t } = useI18n()
  const [doctors, setDoctors] = useState([])
  const [specializations, setSpecializations] = useState([])
  const [loading, setLoading] = useState(true)
  const [spec, setSpec] = useState('')
  const [q, setQ] = useState('')
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [totalElements, setTotalElements] = useState(0)
  const [booking, setBooking] = useState(null) // doctor dang dat lich

  // Danh sach chuyen khoa cho dropdown (lay 1 lan, tu cache backend)
  useEffect(() => {
    api.get('/doctors/specializations').then((r) => setSpecializations(unwrap(r) || [])).catch(() => {})
  }, [])

  // Doi bo loc / tu khoa -> ve trang dau
  useEffect(() => { setPage(0) }, [spec, q])

  // Tai trang hien tai tu server (debounce 250ms cho o tim kiem)
  useEffect(() => {
    const t = setTimeout(() => {
      setLoading(true)
      api
        .get('/doctors', { params: { page, size: PAGE_SIZE, specialization: spec || undefined, q: q || undefined } })
        .then((res) => {
          const d = unwrap(res) || {}
          setDoctors(d.content || [])
          setTotalPages(d.totalPages || 0)
          setTotalElements(d.totalElements || 0)
        })
        .catch((e) => toast.error(apiMessage(e)))
        .finally(() => setLoading(false))
    }, 250)
    return () => clearTimeout(t)
  }, [page, spec, q]) // eslint-disable-line

  return (
    <>
      <PageHeader title={t('page.doctorsTitle')} subtitle={t('page.doctorsSubtitle')} />

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

      {loading ? (
        <Spinner />
      ) : doctors.length === 0 ? (
        <EmptyState icon={Stethoscope} title="Không tìm thấy bác sĩ" subtitle="Thử đổi bộ lọc hoặc từ khóa khác" />
      ) : (
        <>
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {doctors.map((d) => (
              <Card key={d.id} className="flex flex-col p-5">
                <div className="flex items-center gap-3">
                  <div className="flex h-12 w-12 items-center justify-center rounded-full bg-primary-soft text-primary">
                    <Stethoscope className="h-6 w-6" />
                  </div>
                  <div className="min-w-0">
                    <p className="truncate font-semibold text-text">{d.fullName}</p>
                    <p className="truncate text-sm text-primary">{d.specialization}</p>
                  </div>
                </div>
                <div className="mt-4 flex items-center gap-1.5 text-sm text-muted">
                  <Clock className="h-4 w-4" />
                  {formatWorkTime(d.workStartTime)} – {formatWorkTime(d.workEndTime)}
                </div>
                <div className="mt-2 min-h-[20px]">
                  {d.ratingCount > 0 ? (
                    <StarRating value={d.avgRating || 0} count={d.ratingCount} showNumber />
                  ) : (
                    <span className="text-xs text-muted">Chưa có đánh giá</span>
                  )}
                </div>
                <Button className="mt-4 w-full" onClick={() => setBooking(d)}>
                  Đặt lịch
                </Button>
              </Card>
            ))}
          </div>

          {totalPages > 1 && (
            <div className="mt-6 flex items-center justify-center gap-3">
              <Button variant="secondary" disabled={page === 0} onClick={() => setPage((p) => p - 1)}>
                <ChevronLeft className="h-4 w-4" /> Trước
              </Button>
              <span className="text-sm text-muted">
                Trang {page + 1}/{totalPages} · {totalElements} bác sĩ
              </span>
              <Button variant="secondary" disabled={page >= totalPages - 1} onClick={() => setPage((p) => p + 1)}>
                Sau <ChevronRight className="h-4 w-4" />
              </Button>
            </div>
          )}
        </>
      )}

      {booking && (
        <BookingModal doctor={booking} onClose={() => setBooking(null)} toast={toast} />
      )}
    </>
  )
}

function BookingModal({ doctor, onClose, toast }) {
  const today = todayInClinic()   // hom nay theo gio phong kham (khong theo mui gio trinh duyet)
  const [date, setDate] = useState(today)
  const [time, setTime] = useState('')            // slot da chon "HH:MM"
  const [reason, setReason] = useState('')
  const [booked, setBooked] = useState(new Set()) // cac gio da co lich (chua huy)
  const [leaveDates, setLeaveDates] = useState(new Set()) // cac ngay bac si nghi (YYYY-MM-DD)
  const [loadingSlots, setLoadingSlots] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')

  // Sinh slot 30' trong gio lam viec cua bac si (chi tinh lai khi doi bac si)
  const slots = useMemo(() => genSlots(doctor.workStartTime, doctor.workEndTime, 30), [doctor])

  // Ngay nghi cua bac si (lay 1 lan) -> khoa ngay do
  useEffect(() => {
    api.get(`/doctors/${doctor.id}/leaves`)
      .then((res) => setLeaveDates(new Set((unwrap(res) || []).map((l) => l.leaveDate))))
      .catch(() => setLeaveDates(new Set()))
  }, [doctor.id])
  const onLeave = leaveDates.has(date)

  // Doi ngay -> lay cac gio da dat cua bac si de lam mo, va bo chon cu
  useEffect(() => {
    setTime('')
    setLoadingSlots(true)
    api.get(`/appointments/doctors/${doctor.id}/booked`, { params: { date } })
      .then((res) => {
        // Doi Instant -> "HH:MM" theo GIO PHONG KHAM de khop voi slot (khong dung gio trinh duyet)
        const set = new Set((unwrap(res) || []).map(instantToClinicHHMM))
        setBooked(set)
      })
      .catch(() => setBooked(new Set()))
      .finally(() => setLoadingSlots(false))
  }, [date, doctor.id]) // eslint-disable-line

  const now = new Date()
  // slot da qua = mốc tuyet doi (gio phong kham) <= bay gio -> so sanh dung bat ke mui gio
  const isPast = (slot) => new Date(clinicDateTimeToIso(date, slot)) <= now

  async function submit(e) {
    e.preventDefault()
    setError('')
    if (!time) { setError('Vui lòng chọn một khung giờ'); return }
    // Ghi nhan gio da chon la GIO PHONG KHAM -> Instant UTG dung, du user o mui gio khac
    const appointmentTime = clinicDateTimeToIso(date, time)
    setSaving(true)
    try {
      await api.post('/appointments', { doctorId: doctor.id, appointmentTime, reason })
      toast.success('Đặt lịch thành công! Vui lòng kiểm tra email xác nhận.')
      onClose()
    } catch (err) {
      toast.error(apiMessage(err))
    } finally {
      setSaving(false)
    }
  }

  return (
    <Modal open onClose={onClose} title={`Đặt lịch với ${doctor.fullName}`}>
      <form onSubmit={submit} className="space-y-4">
        <div className="rounded-lg bg-primary-soft p-3 text-sm text-text">
          <span className="font-medium">{doctor.specialization}</span> · Giờ làm việc{' '}
          {formatWorkTime(doctor.workStartTime)}–{formatWorkTime(doctor.workEndTime)}
        </div>

        <Field label="Ngày khám" required>
          <Input type="date" min={today} value={date} onChange={(e) => setDate(e.target.value)} required />
        </Field>

        <div>
          <span className="mb-1.5 block text-sm font-medium text-text">Chọn khung giờ (mỗi buổi 30 phút)</span>
          {onLeave ? (
            <p className="rounded-lg bg-amber-50 p-3 text-sm text-amber-700 ring-1 ring-amber-200">
              Bác sĩ nghỉ vào ngày này. Vui lòng chọn ngày khác.
            </p>
          ) : loadingSlots ? (
            <Spinner label="Đang tải khung giờ…" />
          ) : slots.length === 0 ? (
            <p className="text-sm text-muted">Bác sĩ chưa khai báo giờ làm việc hợp lệ.</p>
          ) : (
            <>
              <div className="grid grid-cols-4 gap-2 sm:grid-cols-6">
                {slots.map((s) => {
                  const disabled = booked.has(s) || isPast(s)
                  const selected = time === s
                  return (
                    <button
                      key={s}
                      type="button"
                      disabled={disabled}
                      onClick={() => setTime(s)}
                      title={booked.has(s) ? 'Đã có người đặt' : isPast(s) ? 'Đã qua giờ' : ''}
                      className={[
                        'rounded-lg border px-2 py-2 text-sm font-medium transition',
                        selected
                          ? 'border-primary bg-primary text-white'
                          : disabled
                            ? 'cursor-not-allowed border-border bg-slate-100 text-slate-300 line-through'
                            : 'border-border bg-white text-text hover:border-primary hover:text-primary',
                      ].join(' ')}
                    >
                      {s}
                    </button>
                  )
                })}
              </div>
              <p className="mt-1.5 text-xs text-muted">Ô mờ gạch ngang = đã có người đặt hoặc đã qua giờ.</p>
            </>
          )}
        </div>

        <Field label="Lý do khám">
          <Textarea placeholder="Mô tả triệu chứng hoặc lý do khám…" value={reason} onChange={(e) => setReason(e.target.value)} />
        </Field>
        {error && <p className="text-sm text-danger" role="alert">{error}</p>}
        <div className="flex justify-end gap-2 pt-2">
          <Button variant="secondary" type="button" onClick={onClose}>Hủy</Button>
          <Button type="submit" loading={saving} disabled={!time || onLeave}>Xác nhận đặt lịch</Button>
        </div>
      </form>
    </Modal>
  )
}

// Sinh cac gio bat dau, buoc stepMin phut, sao cho buoi kham 30' nam gon trong gio lam viec.
function genSlots(start, end, stepMin) {
  if (!start || !end) return []
  const toMin = (t) => { const [h, m] = t.split(':').map(Number); return h * 60 + m }
  const pad = (n) => String(n).padStart(2, '0')
  const fmt = (mins) => `${pad(Math.floor(mins / 60))}:${pad(mins % 60)}`
  const s = toMin(start), e = toMin(end)
  const out = []
  for (let m = s; m + stepMin <= e; m += stepMin) out.push(fmt(m))
  return out
}
