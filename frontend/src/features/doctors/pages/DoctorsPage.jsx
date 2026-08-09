import { useEffect, useMemo, useState } from 'react'
import api, { unwrap, apiMessage } from '@/shared/lib/api'
import { formatWorkTime, todayInClinic, clinicDateTimeToIso, instantToClinicHHMM } from '@/shared/lib/format'
import { useToast } from '@/shared/components/Toast'
import { useI18n } from '@/shared/i18n/I18nProvider'
import {
  Button, Card, Field, Input, Select, Textarea, Spinner, EmptyState, PageHeader, StarRating,
} from '@/shared/ui'
import Modal from '@/shared/components/Modal'
import DoctorAvatar from '@/shared/components/DoctorAvatar'
import Paginator from '@/shared/components/Paginator'
import { Stethoscope, Clock, Search } from 'lucide-react'

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
    const timer = setTimeout(() => {
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
    return () => clearTimeout(timer)
  }, [page, spec, q]) // eslint-disable-line

  return (
    <>
      <PageHeader title={t('page.doctorsTitle')} subtitle={t('page.doctorsSubtitle')} />

      <Card className="mb-6 flex flex-col gap-3 p-4 sm:flex-row sm:items-center">
        <div className="relative flex-1">
          <Search className="pointer-events-none absolute left-3.5 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" aria-hidden="true" />
          <Input
            className="pl-10"
            placeholder={t('doctors.searchPlaceholder')}
            value={q}
            onChange={(e) => setQ(e.target.value)}
            aria-label={t('doctors.searchAria')}
          />
        </div>
        <Select value={spec} onChange={(e) => setSpec(e.target.value)} className="sm:w-56" aria-label={t('doctors.specialtyAria')}>
          <option value="">{t('doctors.allSpecialties')}</option>
          {specializations.map((s) => (
            <option key={s} value={s}>{s}</option>
          ))}
        </Select>
        {!loading && <span className="min-w-11 shrink-0 self-end rounded-xl bg-primary-soft px-3 py-2 text-center text-xs font-extrabold text-primary sm:self-auto">{totalElements}</span>}
      </Card>

      {loading ? (
        <Spinner />
      ) : doctors.length === 0 ? (
        <EmptyState icon={Stethoscope} title={t('doctors.notFound')} subtitle={t('doctors.notFoundSub')} />
      ) : (
        <>
          <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
            {doctors.map((d) => (
              <Card key={d.id} className="interactive-card relative flex flex-col overflow-hidden p-6">
                <div className="pointer-events-none absolute -right-10 -top-12 h-32 w-32 rounded-full bg-teal-100/60 blur-2xl" aria-hidden="true" />
                <div className="flex items-center gap-3">
                  <DoctorAvatar doctor={d} />
                  <div className="relative min-w-0">
                    <p className="truncate text-base font-extrabold text-text">{d.fullName}</p>
                    <p className="mt-0.5 truncate text-sm font-semibold text-primary">{d.specialization}</p>
                  </div>
                </div>
                <div className="mt-5 flex items-center gap-2 rounded-xl bg-slate-50 px-3 py-2.5 text-sm font-medium text-muted">
                  <Clock className="h-4 w-4 text-primary" aria-hidden="true" />
                  {formatWorkTime(d.workStartTime)} – {formatWorkTime(d.workEndTime)}
                </div>
                <div className="mt-3 min-h-[20px]">
                  {d.ratingCount > 0 ? (
                    <StarRating value={d.avgRating || 0} count={d.ratingCount} showNumber />
                  ) : (
                    <span className="text-xs text-muted">{t('doctors.noRating')}</span>
                  )}
                </div>
                <Button className="mt-5 w-full" onClick={() => setBooking(d)}>
                  {t('doctors.book')}
                </Button>
              </Card>
            ))}
          </div>

          <Paginator page={page} totalPages={totalPages} onPage={setPage}
            info={t('doctors.pageInfo', { page: page + 1, total: totalPages, count: totalElements })} />
        </>
      )}

      {booking && (
        <BookingModal doctor={booking} onClose={() => setBooking(null)} toast={toast} />
      )}
    </>
  )
}

function BookingModal({ doctor, onClose, toast }) {
  const { t } = useI18n()
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
        const set = new Set((unwrap(res) || []).map(instantToClinicHHMM))
        setBooked(set)
      })
      .catch(() => setBooked(new Set()))
      .finally(() => setLoadingSlots(false))
  }, [date, doctor.id]) // eslint-disable-line

  const now = new Date()
  const isPast = (slot) => new Date(clinicDateTimeToIso(date, slot)) <= now

  async function submit(e) {
    e.preventDefault()
    setError('')
    if (!time) { setError(t('booking.chooseOne')); return }
    const appointmentTime = clinicDateTimeToIso(date, time)
    setSaving(true)
    try {
      await api.post('/appointments', { doctorId: doctor.id, appointmentTime, reason })
      toast.success(t('booking.success'))
      onClose()
    } catch (err) {
      toast.error(apiMessage(err))
    } finally {
      setSaving(false)
    }
  }

  return (
    <Modal open onClose={onClose} title={t('booking.title', { name: doctor.fullName })}>
      <form onSubmit={submit} className="space-y-4">
        <div className="rounded-lg bg-primary-soft p-3 text-sm text-text">
          <span className="font-medium">{doctor.specialization}</span> · {t('booking.workHours')}{' '}
          {formatWorkTime(doctor.workStartTime)}–{formatWorkTime(doctor.workEndTime)}
        </div>

        <Field label={t('booking.date')} required>
          <Input type="date" min={today} value={date} onChange={(e) => setDate(e.target.value)} required />
        </Field>

        <div>
          <span className="mb-1.5 block text-sm font-medium text-text">{t('booking.chooseSlot')}</span>
          {onLeave ? (
            <p className="rounded-lg bg-amber-50 p-3 text-sm text-amber-700 ring-1 ring-amber-200">
              {t('booking.onLeave')}
            </p>
          ) : loadingSlots ? (
            <Spinner label={t('common.loadingSlots')} />
          ) : slots.length === 0 ? (
            <p className="text-sm text-muted">{t('booking.noWorkHours')}</p>
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
                      title={booked.has(s) ? t('booking.booked') : isPast(s) ? t('booking.past') : ''}
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
              <p className="mt-1.5 text-xs text-muted">{t('booking.legend')}</p>
            </>
          )}
        </div>

        <Field label={t('booking.reason')}>
          <Textarea placeholder={t('booking.reasonPlaceholder')} value={reason} onChange={(e) => setReason(e.target.value)} />
        </Field>
        {error && <p className="text-sm text-danger" role="alert">{error}</p>}
        <div className="flex justify-end gap-2 pt-2">
          <Button variant="secondary" type="button" onClick={onClose}>{t('common.cancel')}</Button>
          <Button type="submit" loading={saving} disabled={!time || onLeave}>{t('booking.submit')}</Button>
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
