import { useEffect, useMemo, useState } from 'react'
import api, { unwrap, apiMessage, fetchByIdsMap } from '@/shared/lib/api'
import {
  formatDateTime, formatWorkTime, todayInClinic, clinicDateTimeToIso, instantToClinicHHMM,
} from '@/shared/lib/format'
import { useToast } from '@/shared/components/Toast'
import { useConfirm } from '@/shared/components/Confirm'
import { useI18n } from '@/shared/i18n/I18nProvider'
import { Button, Card, Field, Input, Textarea, Spinner, EmptyState, PageHeader, StatusBadge, StarRating } from '@/shared/ui'
import Modal from '@/shared/components/Modal'
import { CalendarDays, Stethoscope, XCircle, CalendarClock, Star } from 'lucide-react'

export default function MyAppointmentsPage() {
  const toast = useToast()
  const confirm = useConfirm()
  const { t } = useI18n()
  const [items, setItems] = useState([])
  const [doctors, setDoctors] = useState({})
  const [loading, setLoading] = useState(true)
  const [cancelling, setCancelling] = useState(null)
  const [rescheduling, setRescheduling] = useState(null) // lich dang doi (appointment)
  const [rating, setRating] = useState(null)             // lich dang danh gia (appointment)

  async function load() {
    setLoading(true)
    try {
      const appts = await api.get('/appointments/patients/me')
      const list = (unwrap(appts) || []).slice().sort((a, b) => new Date(b.appointmentTime) - new Date(a.appointmentTime))
      setItems(list)
      // Chi lay dung nhung bac si xuat hien trong lich (batch, khong tai het)
      setDoctors(await fetchByIdsMap('doctors', list.map((a) => a.doctorId)))
    } catch (e) {
      toast.error(apiMessage(e))
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { load() }, []) // eslint-disable-line

  async function cancel(id) {
    const ok = await confirm({
      title: 'Hủy lịch hẹn',
      message: 'Bạn chắc chắn muốn hủy lịch hẹn này? Thao tác không thể hoàn tác.',
      confirmText: 'Hủy lịch hẹn',
      cancelText: 'Giữ lại',
      danger: true,
    })
    if (!ok) return
    setCancelling(id)
    try {
      await api.patch(`/appointments/${id}/cancel`)
      toast.success('Đã hủy lịch hẹn')
      load()
    } catch (e) {
      toast.error(apiMessage(e))
    } finally {
      setCancelling(null)
    }
  }

  if (loading) return <Spinner />

  return (
    <>
      <PageHeader title={t('page.myApptTitle')} subtitle={t('page.myApptSubtitle')} />
      {items.length === 0 ? (
        <EmptyState icon={CalendarDays} title="Chưa có lịch hẹn nào" subtitle="Hãy tìm bác sĩ và đặt lịch khám đầu tiên" />
      ) : (
        <div className="space-y-3">
          {items.map((a) => {
            const d = doctors[a.doctorId]
            const canModify = a.status !== 'COMPLETED' && a.status !== 'CANCELLED'
            return (
              <Card key={a.id} className="flex flex-wrap items-center gap-4 p-4">
                <div className="flex h-11 w-11 items-center justify-center rounded-full bg-primary-soft text-primary">
                  <Stethoscope className="h-5 w-5" />
                </div>
                <div className="min-w-0 flex-1">
                  <p className="font-semibold text-text">{d?.fullName || 'Bác sĩ'}</p>
                  <p className="text-sm text-muted">
                    {d?.specialization} · {formatDateTime(a.appointmentTime)}
                  </p>
                  {a.reason && <p className="mt-0.5 truncate text-sm text-slate-400">Lý do: {a.reason}</p>}
                </div>
                <StatusBadge status={a.status} />
                {canModify && (
                  <div className="flex gap-1">
                    <Button
                      variant="ghost"
                      disabled={!d}
                      onClick={() => setRescheduling(a)}
                      title={d ? 'Đổi sang khung giờ khác' : 'Đang tải thông tin bác sĩ…'}
                    >
                      <CalendarClock className="h-4 w-4" /> Đổi lịch
                    </Button>
                    <Button
                      variant="ghost"
                      className="text-danger"
                      loading={cancelling === a.id}
                      onClick={() => cancel(a.id)}
                    >
                      <XCircle className="h-4 w-4" /> Hủy
                    </Button>
                  </div>
                )}
                {a.status === 'COMPLETED' && !a.rated && (
                  <Button variant="ghost" className="text-amber-600" onClick={() => setRating(a)}>
                    <Star className="h-4 w-4" /> Đánh giá
                  </Button>
                )}
                {a.status === 'COMPLETED' && a.rated && (
                  <span className="inline-flex items-center gap-1 text-xs text-muted">
                    <Star className="h-3.5 w-3.5 fill-amber-400 text-amber-400" /> Đã đánh giá
                  </span>
                )}
              </Card>
            )
          })}
        </div>
      )}

      {rescheduling && doctors[rescheduling.doctorId] && (
        <RescheduleModal
          appointment={rescheduling}
          doctor={doctors[rescheduling.doctorId]}
          onClose={() => setRescheduling(null)}
          onDone={() => { setRescheduling(null); load() }}
          toast={toast}
        />
      )}

      {rating && (
        <RatingModal
          appointment={rating}
          doctor={doctors[rating.doctorId]}
          onClose={() => setRating(null)}
          onDone={() => { setRating(null); load() }}
          toast={toast}
        />
      )}
    </>
  )
}

function RatingModal({ appointment, doctor, onClose, onDone, toast }) {
  const [stars, setStars] = useState(5)
  const [comment, setComment] = useState('')
  const [saving, setSaving] = useState(false)

  async function submit(e) {
    e.preventDefault()
    setSaving(true)
    try {
      await api.post(`/appointments/${appointment.id}/rate`, { stars, comment })
      toast.success('Cảm ơn bạn đã đánh giá!')
      onDone()
    } catch (err) {
      toast.error(apiMessage(err))
    } finally {
      setSaving(false)
    }
  }

  return (
    <Modal open onClose={onClose} title={`Đánh giá ${doctor?.fullName || 'bác sĩ'}`}>
      <form onSubmit={submit} className="space-y-4">
        <div className="flex flex-col items-center gap-2 py-2">
          <StarRating value={stars} onChange={setStars} size="h-9 w-9" />
          <span className="text-sm text-muted">{stars}/5 sao</span>
        </div>
        <Field label="Nhận xét (tùy chọn)">
          <Textarea
            value={comment}
            onChange={(e) => setComment(e.target.value)}
            maxLength={1000}
            placeholder="Chia sẻ trải nghiệm của bạn về buổi khám…"
          />
        </Field>
        <div className="flex justify-end gap-2 pt-2">
          <Button variant="secondary" type="button" onClick={onClose}>Hủy</Button>
          <Button type="submit" loading={saving}>Gửi đánh giá</Button>
        </div>
      </form>
    </Modal>
  )
}

function RescheduleModal({ appointment, doctor, onClose, onDone, toast }) {
  const today = todayInClinic()
  // Mac dinh: ngay/gio hien tai cua lich (theo gio phong kham)
  const curDate = instantToClinicYMD(appointment.appointmentTime)
  const [date, setDate] = useState(curDate >= today ? curDate : today)
  const [time, setTime] = useState('')
  const [booked, setBooked] = useState(new Set())
  const [leaveDates, setLeaveDates] = useState(new Set())
  const [loadingSlots, setLoadingSlots] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')

  const slots = useMemo(() => genSlots(doctor.workStartTime, doctor.workEndTime, 30), [doctor])
  const curHHMM = instantToClinicHHMM(appointment.appointmentTime)

  // Ngay nghi cua bac si -> khoa ngay do
  useEffect(() => {
    api.get(`/doctors/${doctor.id}/leaves`)
      .then((res) => setLeaveDates(new Set((unwrap(res) || []).map((l) => l.leaveDate))))
      .catch(() => setLeaveDates(new Set()))
  }, [doctor.id])
  const onLeave = leaveDates.has(date)

  useEffect(() => {
    setTime('')
    setLoadingSlots(true)
    api.get(`/appointments/doctors/${doctor.id}/booked`, { params: { date } })
      .then((res) => setBooked(new Set((unwrap(res) || []).map(instantToClinicHHMM))))
      .catch(() => setBooked(new Set()))
      .finally(() => setLoadingSlots(false))
  }, [date, doctor.id]) // eslint-disable-line

  const now = new Date()
  const isPast = (slot) => new Date(clinicDateTimeToIso(date, slot)) <= now
  // Slot dang la gio cua chinh lich nay (cung ngay) -> khong coi la "da dat" (cho phep giu nguyen/chon lai)
  const isOwnCurrent = (slot) => date === curDate && slot === curHHMM

  async function submit(e) {
    e.preventDefault()
    setError('')
    if (!time) { setError('Vui lòng chọn một khung giờ mới'); return }
    const appointmentTime = clinicDateTimeToIso(date, time)
    setSaving(true)
    try {
      // Giu nguyen bac si, ly do, trang thai; chi doi gio. Gui status hien tai de
      // MapStruct khong set null vao cot status (NOT NULL).
      await api.put(`/appointments/${appointment.id}`, {
        doctorId: appointment.doctorId,
        appointmentTime,
        reason: appointment.reason,
        status: appointment.status,
      })
      toast.success('Đã đổi lịch hẹn. Vui lòng kiểm tra email xác nhận.')
      onDone()
    } catch (err) {
      toast.error(apiMessage(err))
    } finally {
      setSaving(false)
    }
  }

  return (
    <Modal open onClose={onClose} title={`Đổi lịch với ${doctor.fullName}`}>
      <form onSubmit={submit} className="space-y-4">
        <div className="rounded-lg bg-primary-soft p-3 text-sm text-text">
          Lịch hiện tại: <span className="font-medium">{formatDateTime(appointment.appointmentTime)}</span>
          <br />
          {doctor.specialization} · Giờ làm việc {formatWorkTime(doctor.workStartTime)}–{formatWorkTime(doctor.workEndTime)}
        </div>

        <Field label="Ngày khám mới" required>
          <Input type="date" min={today} value={date} onChange={(e) => setDate(e.target.value)} required />
        </Field>

        <div>
          <span className="mb-1.5 block text-sm font-medium text-text">Chọn khung giờ mới (mỗi buổi 30 phút)</span>
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
                  const disabled = (booked.has(s) && !isOwnCurrent(s)) || isPast(s)
                  const selected = time === s
                  return (
                    <button
                      key={s}
                      type="button"
                      disabled={disabled}
                      onClick={() => setTime(s)}
                      title={isOwnCurrent(s) ? 'Giờ hiện tại của lịch' : booked.has(s) ? 'Đã có người đặt' : isPast(s) ? 'Đã qua giờ' : ''}
                      className={[
                        'rounded-lg border px-2 py-2 text-sm font-medium transition',
                        selected
                          ? 'border-primary bg-primary text-white'
                          : disabled
                            ? 'cursor-not-allowed border-border bg-slate-100 text-slate-300 line-through'
                            : isOwnCurrent(s)
                              ? 'border-primary/40 bg-primary-soft text-primary hover:border-primary'
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

        {error && <p className="text-sm text-danger" role="alert">{error}</p>}
        <div className="flex justify-end gap-2 pt-2">
          <Button variant="secondary" type="button" onClick={onClose}>Hủy</Button>
          <Button type="submit" loading={saving} disabled={!time || onLeave}>Xác nhận đổi lịch</Button>
        </div>
      </form>
    </Modal>
  )
}

// Instant ISO -> "YYYY-MM-DD" theo gio phong kham (de so sanh voi input date)
function instantToClinicYMD(iso) {
  // instantToClinicHHMM cho gio; can them ngay -> dung Intl theo Asia/Ho_Chi_Minh
  const d = new Date(iso)
  const parts = new Intl.DateTimeFormat('en-CA', {
    timeZone: 'Asia/Ho_Chi_Minh', year: 'numeric', month: '2-digit', day: '2-digit',
  }).format(d)
  return parts // en-CA -> "YYYY-MM-DD"
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
