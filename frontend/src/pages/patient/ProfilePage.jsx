import { useEffect, useState } from 'react'
import api, { unwrap, apiMessage } from '../../lib/api'
import { useAuth } from '../../auth/AuthContext'
import { formatWorkTime } from '../../lib/format'
import { useToast } from '../../components/Toast'
import { Button, Card, Field, Input, Select, Spinner, PageHeader } from '../../components/ui'
import { UserCircle, Stethoscope } from 'lucide-react'

export default function ProfilePage() {
  const { role, name, username } = useAuth()
  if (role === 'PATIENT') return <PatientProfile />
  if (role === 'DOCTOR') return <DoctorProfile />
  return (
    <>
      <PageHeader title="Hồ sơ" subtitle="Thông tin tài khoản" />
      <Card className="max-w-md p-6">
        <div className="flex items-center gap-3">
          <div className="flex h-12 w-12 items-center justify-center rounded-full bg-[--color-primary-soft] text-[--color-primary]">
            <UserCircle className="h-6 w-6" />
          </div>
          <div>
            <p className="font-semibold text-[--color-text]">{name}</p>
            <p className="text-sm text-[--color-muted]">@{username} · Quản trị viên</p>
          </div>
        </div>
      </Card>
    </>
  )
}

const EMPTY = { fullName: '', phone: '', gender: 'MALE', dateOfBirth: '', email: '', address: '' }

function PatientProfile() {
  const toast = useToast()
  const [form, setForm] = useState(EMPTY)
  const [existing, setExisting] = useState(null)
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)

  useEffect(() => {
    api
      .get('/patients/me')
      .then((res) => {
        const p = unwrap(res)
        setExisting(p)
        setForm({
          fullName: p.fullName || '', phone: p.phone || '', gender: p.gender || 'MALE',
          dateOfBirth: p.dateOfBirth || '', email: p.email || '', address: p.address || '',
        })
      })
      .catch((e) => { if (e?.response?.status !== 404) toast.error(apiMessage(e)) })
      .finally(() => setLoading(false))
  }, []) // eslint-disable-line

  const set = (k) => (e) => setForm((f) => ({ ...f, [k]: e.target.value }))

  async function submit(e) {
    e.preventDefault()
    setSaving(true)
    try {
      if (existing) {
        await api.put(`/patients/${existing.id}`, form)
        toast.success('Đã cập nhật hồ sơ')
      } else {
        const res = await api.post('/patients/', form)
        setExisting(unwrap(res))
        toast.success('Đã tạo hồ sơ bệnh nhân')
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
        title="Hồ sơ bệnh nhân"
        subtitle={existing ? 'Cập nhật thông tin cá nhân của bạn' : 'Tạo hồ sơ để bắt đầu đặt lịch khám'}
      />
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
          <Field label="Email">
            <Input type="email" value={form.email} onChange={set('email')} placeholder="email@example.com" />
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
      <PageHeader title="Hồ sơ bác sĩ" subtitle="Thông tin và giờ làm việc của bạn" />
      <Card className="max-w-md p-6">
        <div className="flex items-center gap-3">
          <div className="flex h-14 w-14 items-center justify-center rounded-full bg-[--color-primary-soft] text-[--color-primary]">
            <Stethoscope className="h-7 w-7" />
          </div>
          <div>
            <p className="text-lg font-semibold text-[--color-text]">{doc?.fullName}</p>
            <p className="text-sm text-[--color-primary]">{doc?.specialization}</p>
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
    <div className="flex justify-between gap-4 border-b border-[--color-border] pb-2 last:border-0">
      <dt className="text-[--color-muted]">{label}</dt>
      <dd className="font-medium text-[--color-text]">{value}</dd>
    </div>
  )
}
