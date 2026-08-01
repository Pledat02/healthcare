import { useEffect, useState } from 'react'
import api, { unwrap, apiMessage } from '@/shared/lib/api'
import { useAuth } from '@/auth/AuthContext'
import { usePatientProfile } from '@/auth/PatientProfile'
import { formatWorkTime } from '@/shared/lib/format'
import { useToast } from '@/shared/components/Toast'
import { useI18n } from '@/shared/i18n/I18nProvider'
import { Button, Card, Field, Input, Select, Spinner, PageHeader } from '@/shared/ui'
import { UserCircle, Stethoscope, Info } from 'lucide-react'

export default function ProfilePage() {
  const { role, name, username } = useAuth()
  const { t } = useI18n()
  if (role === 'PATIENT') return <PatientProfile />
  if (role === 'DOCTOR') return <DoctorProfile />
  return (
    <>
      <PageHeader title={t('page.profileTitle')} subtitle={t('page.profileSubtitle')} />
      <Card className="max-w-md p-6">
        <div className="flex items-center gap-3">
          <div className="flex h-12 w-12 items-center justify-center rounded-full bg-primary-soft text-primary">
            <UserCircle className="h-6 w-6" />
          </div>
          <div>
            <p className="font-semibold text-text">{name}</p>
            <p className="text-sm text-muted">@{username} · Quản trị viên</p>
          </div>
        </div>
      </Card>
    </>
  )
}

const EMPTY = { fullName: '', phone: '', gender: 'MALE', dateOfBirth: '', email: '', address: '' }

function PatientProfile() {
  const toast = useToast()
  const { t } = useI18n()
  // US-02b: prefill ho ten + email tu token Keycloak (dang nhap Google khong co san ho so)
  const { name: accountName, email: accountEmail } = useAuth()
  const { profile: existing, loading, setProfile } = usePatientProfile()
  const [form, setForm] = useState(() => ({
    ...EMPTY,
    fullName: accountName || '',
    email: accountEmail || '',
  }))
  const [saving, setSaving] = useState(false)

  // Khi context nap xong ho so -> do vao form de sua. Chua co -> giu prefill tu token.
  useEffect(() => {
    if (existing) {
      setForm({
        fullName: existing.fullName || '', phone: existing.phone || '', gender: existing.gender || 'MALE',
        dateOfBirth: existing.dateOfBirth || '', email: existing.email || '', address: existing.address || '',
      })
    }
  }, [existing])

  const set = (k) => (e) => setForm((f) => ({ ...f, [k]: e.target.value }))

  async function submit(e) {
    e.preventDefault()
    setSaving(true)
    try {
      if (existing) {
        const res = await api.put(`/patients/${existing.id}`, form)
        setProfile(unwrap(res)) // dong bo context
        toast.success('Đã cập nhật hồ sơ')
      } else {
        const res = await api.post('/patients/', form)
        setProfile(unwrap(res)) // "mo cong" -> cac chuc nang can ho so dung duoc ngay
        toast.success('Đã tạo hồ sơ, giờ bạn có thể đặt lịch khám')
      }
    } catch (err) {
      toast.error(apiMessage(err))
    } finally {
      setSaving(false)
    }
  }

  if (loading) return <Spinner />

  return (
    <>
      <PageHeader
        title={t('page.profileTitle')}
        subtitle={existing ? 'Cập nhật thông tin cá nhân của bạn' : 'Tạo hồ sơ để bắt đầu đặt lịch khám'}
      />
      {!existing && (
        <div className="mb-4 flex max-w-2xl items-start gap-3 rounded-lg border border-info/30 bg-info/10 p-4 text-sm text-text">
          <Info className="mt-0.5 h-5 w-5 shrink-0 text-info" />
          <p>
            Bạn cần hoàn thiện hồ sơ (bắt buộc <strong>họ tên</strong> và <strong>số điện thoại</strong>)
            trước khi đặt lịch khám, xem lịch hẹn hay hồ sơ khám.
          </p>
        </div>
      )}
      <Card className="max-w-2xl p-6">
        <form onSubmit={submit} className="grid gap-4 sm:grid-cols-2">
          <Field label="Họ và tên" required>
            <Input value={form.fullName} onChange={set('fullName')} required placeholder="Nguyễn Văn A" />
          </Field>
          <Field label="Số điện thoại" required hint="10 số, bắt đầu bằng 0">
            <Input value={form.phone} onChange={set('phone')} required placeholder="0901234567" inputMode="tel" />
          </Field>
          <Field label="Giới tính" required>
            <Select value={form.gender} onChange={set('gender')}>
              <option value="MALE">Nam</option>
              <option value="FEMALE">Nữ</option>
              <option value="OTHER">Khác</option>
            </Select>
          </Field>
          <Field label="Ngày sinh">
            <Input type="date" value={form.dateOfBirth} onChange={set('dateOfBirth')} />
          </Field>
          <Field label="Email" required hint="Dùng để nhận xác nhận và nhắc lịch khám">
            <Input type="email" value={form.email} onChange={set('email')} placeholder="email@example.com" required />
          </Field>
          <Field label="Địa chỉ">
            <Input value={form.address} onChange={set('address')} placeholder="Quận, Thành phố" />
          </Field>
          <div className="sm:col-span-2 flex justify-end pt-2">
            <Button type="submit" loading={saving}>
              {existing ? 'Lưu thay đổi' : 'Tạo hồ sơ'}
            </Button>
          </div>
        </form>
      </Card>
    </>
  )
}

function DoctorProfile() {
  const toast = useToast()
  const { t } = useI18n()
  const [doc, setDoc] = useState(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    api
      .get('/doctors/me')
      .then((res) => setDoc(unwrap(res)))
      .catch((e) => toast.error(apiMessage(e)))
      .finally(() => setLoading(false))
  }, []) // eslint-disable-line

  if (loading) return <Spinner />

  return (
    <>
      <PageHeader title={t('page.profileTitle')} subtitle={t('page.profileSubtitle')} />
      <Card className="max-w-md p-6">
        <div className="flex items-center gap-3">
          <div className="flex h-14 w-14 items-center justify-center rounded-full bg-primary-soft text-primary">
            <Stethoscope className="h-7 w-7" />
          </div>
          <div>
            <p className="text-lg font-semibold text-text">{doc?.fullName}</p>
            <p className="text-sm text-primary">{doc?.specialization}</p>
          </div>
        </div>
        <dl className="mt-5 space-y-3 text-sm">
          <Row label="Giờ làm việc" value={`${formatWorkTime(doc?.workStartTime)} – ${formatWorkTime(doc?.workEndTime)}`} />
          <Row label="Số điện thoại" value={doc?.phone || '—'} />
          <Row label="Email" value={doc?.email || '—'} />
        </dl>
      </Card>
    </>
  )
}

function Row({ label, value }) {
  return (
    <div className="flex justify-between gap-4 border-b border-border pb-2 last:border-0">
      <dt className="text-muted">{label}</dt>
      <dd className="font-medium text-text">{value}</dd>
    </div>
  )
}
