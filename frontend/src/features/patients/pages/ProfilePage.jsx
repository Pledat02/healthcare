import { useEffect, useState } from 'react'
import api, { unwrap, apiMessage } from '@/shared/lib/api'
import { useAuth } from '@/auth/AuthContext'
import { usePatientProfile } from '@/auth/PatientProfile'
import { formatWorkTime } from '@/shared/lib/format'
import { useToast } from '@/shared/components/Toast'
import DoctorAvatar from '@/shared/components/DoctorAvatar'
import { useI18n } from '@/shared/i18n/I18nProvider'
import { Button, Card, Field, Input, Select, Spinner, PageHeader } from '@/shared/ui'
import {
  BadgeCheck, ContactRound, HeartPulse, Info, Mail, MapPin,
  Camera, Phone, Save, ShieldCheck, UserCircle, UserRound,
} from 'lucide-react'

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
            <p className="text-sm text-muted">@{username} · {t('profile.admin')}</p>
          </div>
        </div>
      </Card>
    </>
  )
}

const EMPTY = { fullName: '', phone: '', gender: 'MALE', dateOfBirth: '', email: '', address: '' }

function profileForm(profile = {}) {
  return {
    fullName: profile.fullName || '', phone: profile.phone || '', gender: profile.gender || 'MALE',
    dateOfBirth: profile.dateOfBirth || '', email: profile.email || '', address: profile.address || '',
  }
}

function profileInitials(name) {
  const parts = name?.trim().split(/\s+/).filter(Boolean) || []
  return parts.slice(-2).map((part) => part[0]).join('').toUpperCase() || 'MB'
}

function PatientProfile() {
  const toast = useToast()
  const { t } = useI18n()
  // US-02b: prefill ho ten + email tu token Keycloak (dang nhap Google khong co san ho so)
  const { name: accountName, email: accountEmail } = useAuth()
  const { profile: existing, loading, setProfile } = usePatientProfile()
  const initialForm = {
    ...EMPTY,
    fullName: accountName || '',
    email: accountEmail || '',
  }
  const [form, setForm] = useState(initialForm)
  const [savedForm, setSavedForm] = useState(initialForm)
  const [saving, setSaving] = useState(false)

  // Khi context nap xong ho so -> do vao form de sua. Chua co -> giu prefill tu token.
  useEffect(() => {
    if (existing) {
      const next = profileForm(existing)
      setForm(next)
      setSavedForm(next)
    }
  }, [existing])

  const set = (k) => (e) => setForm((f) => ({ ...f, [k]: e.target.value }))

  async function submit(e) {
    e.preventDefault()
    setSaving(true)
    try {
      if (existing) {
        const res = await api.put(`/patients/${existing.id}`, form)
        const next = unwrap(res)
        setProfile(next) // dong bo context
        setForm(profileForm(next))
        setSavedForm(profileForm(next))
        toast.success(t('profile.updated'))
      } else {
        const res = await api.post('/patients/', form)
        const next = unwrap(res)
        setProfile(next) // "mo cong" -> cac chuc nang can ho so dung duoc ngay
        setForm(profileForm(next))
        setSavedForm(profileForm(next))
        toast.success(t('profile.created'))
      }
    } catch (err) {
      toast.error(apiMessage(err))
    } finally {
      setSaving(false)
    }
  }

  if (loading) return (
    <div className="mx-auto max-w-6xl animate-pulse" role="status" aria-label={t('common.loading')}>
      <div className="h-44 rounded-[2rem] bg-slate-200/70" />
      <div className="mt-6 grid gap-6 lg:grid-cols-[18rem_1fr]"><div className="h-72 rounded-[1.75rem] bg-slate-200/70" /><div className="h-[32rem] rounded-[1.75rem] bg-slate-200/70" /></div>
    </div>
  )

  const completionFields = ['fullName', 'phone', 'gender', 'dateOfBirth', 'email', 'address']
  const completed = completionFields.filter((key) => String(form[key] || '').trim()).length
  const completion = Math.round((completed / completionFields.length) * 100)
  const dirty = JSON.stringify(form) !== JSON.stringify(savedForm)
  const submitDisabled = existing && !dirty
  const displayName = form.fullName || accountName || t('page.profileTitle')
  const today = new Date().toISOString().slice(0, 10)

  return (
    <div className="mx-auto max-w-6xl">
      <section className="profile-hero relative overflow-hidden rounded-[2rem] border border-teal-200/50 bg-gradient-to-br from-teal-50 via-white to-sky-50 px-6 py-8 shadow-sm sm:px-9 sm:py-10" aria-labelledby="profile-title">
        <div className="pointer-events-none absolute -right-12 -top-20 h-64 w-64 rounded-full bg-teal-300/20 blur-3xl" aria-hidden="true" />
        <div className="relative flex flex-col justify-between gap-7 md:flex-row md:items-end">
          <div className="max-w-2xl">
            <p className="flex items-center gap-2 text-xs font-extrabold uppercase tracking-[0.14em] text-teal-700"><HeartPulse className="h-4 w-4" aria-hidden="true" />{t('profile.careEyebrow')}</p>
            <h1 id="profile-title" className="mt-4 text-3xl font-black tracking-tight text-slate-950 sm:text-4xl">{t('page.profileTitle')}</h1>
            <p className="mt-3 max-w-xl text-sm leading-6 text-slate-600 sm:text-base">{existing ? t('profile.editSubtitle') : t('profile.createSubtitle')}</p>
          </div>
          <Button form="patient-profile-form" type="submit" loading={saving} disabled={submitDisabled} className="w-full rounded-xl md:w-auto md:min-w-40">
            <Save className="h-4 w-4" aria-hidden="true" />{existing ? t('profile.save') : t('profile.create')}
          </Button>
        </div>
      </section>

      <div className="mt-6 grid items-start gap-6 lg:grid-cols-[18rem_minmax(0,1fr)]">
        <aside className="space-y-4 lg:sticky lg:top-28">
          <Card className="overflow-hidden rounded-[1.75rem]">
            <div className="bg-gradient-to-br from-slate-950 to-teal-950 px-5 py-5 text-white sm:px-6 sm:py-7">
              <div className="flex min-w-0 items-center gap-4 lg:block">
                <div className="flex h-14 w-14 shrink-0 items-center justify-center rounded-2xl bg-teal-400 text-lg font-black text-slate-950 shadow-lg shadow-teal-950/20 sm:h-16 sm:w-16 sm:text-xl" aria-hidden="true">{profileInitials(displayName)}</div>
                <div className="min-w-0"><h2 className="truncate text-lg font-black lg:mt-5">{displayName}</h2><p className="mt-1 truncate text-sm text-slate-300">{form.email || accountEmail}</p><span className="mt-3 inline-flex items-center gap-1.5 rounded-full bg-white/10 px-3 py-1.5 text-xs font-bold text-teal-200 lg:mt-4"><BadgeCheck className="h-4 w-4" aria-hidden="true" />{existing ? t('profile.completeStatus') : t('profile.inProgressStatus')}</span></div>
              </div>
            </div>
            <div className="p-4 sm:p-5">
              <div className="flex items-center justify-between gap-3"><p className="text-sm font-bold text-text">{t('profile.completion')}</p><p className="text-sm font-black text-primary">{completion}%</p></div>
              <div className="mt-3 h-2 overflow-hidden rounded-full bg-slate-100" role="progressbar" aria-label={t('profile.completion')} aria-valuemin="0" aria-valuemax="100" aria-valuenow={completion}><div className="h-full rounded-full bg-gradient-to-r from-teal-500 to-sky-500 transition-[width] duration-500" style={{ width: `${completion}%` }} /></div>
              <p className="mt-3 text-xs leading-5 text-muted">{t('profile.fieldsCompleted', { completed, total: completionFields.length })}</p>
            </div>
          </Card>

          <div className="profile-privacy hidden items-start gap-3 rounded-2xl border border-teal-200/70 bg-teal-50 p-4 lg:flex">
            <ShieldCheck className="mt-0.5 h-5 w-5 shrink-0 text-teal-700" aria-hidden="true" />
            <div><p className="text-sm font-extrabold text-slate-900">{t('profile.privacyTitle')}</p><p className="mt-1 text-xs leading-5 text-slate-600">{t('profile.privacyDesc')}</p></div>
          </div>
        </aside>

        <Card className="overflow-hidden rounded-[1.75rem] shadow-lg shadow-slate-950/5">
          {!existing && <div className="flex items-start gap-3 border-b border-info/20 bg-info/10 px-5 py-4 text-sm text-text sm:px-7"><Info className="mt-0.5 h-5 w-5 shrink-0 text-info" aria-hidden="true" /><p className="leading-6">{t('profile.requireNote')}</p></div>}
          <form id="patient-profile-form" onSubmit={submit} className="divide-y divide-border">
            <section className="p-5 sm:p-7" aria-labelledby="personal-section-title">
              <div className="mb-6 flex items-start gap-3"><span className="flex h-11 w-11 shrink-0 items-center justify-center rounded-xl bg-primary-soft text-primary"><UserRound className="h-5 w-5" aria-hidden="true" /></span><div><h2 id="personal-section-title" className="font-extrabold text-text">{t('profile.personalTitle')}</h2><p className="mt-1 text-sm text-muted">{t('profile.personalDesc')}</p></div></div>
              <div className="grid gap-5 sm:grid-cols-2">
                <Field label={t('profile.fullName')} required><Input value={form.fullName} onChange={set('fullName')} required placeholder={t('profile.namePlaceholder')} autoComplete="name" /></Field>
                <Field label={t('profile.gender')} required><Select value={form.gender} onChange={set('gender')} autoComplete="sex"><option value="MALE">{t('gender.MALE')}</option><option value="FEMALE">{t('gender.FEMALE')}</option><option value="OTHER">{t('gender.OTHER')}</option></Select></Field>
                <Field label={t('profile.dob')}><Input type="date" max={today} value={form.dateOfBirth} onChange={set('dateOfBirth')} autoComplete="bday" /></Field>
              </div>
            </section>

            <section className="p-5 sm:p-7" aria-labelledby="contact-section-title">
              <div className="mb-6 flex items-start gap-3"><span className="flex h-11 w-11 shrink-0 items-center justify-center rounded-xl bg-primary-soft text-primary"><ContactRound className="h-5 w-5" aria-hidden="true" /></span><div><h2 id="contact-section-title" className="font-extrabold text-text">{t('profile.contactTitle')}</h2><p className="mt-1 text-sm text-muted">{t('profile.contactDesc')}</p></div></div>
              <div className="grid gap-5 sm:grid-cols-2">
                <Field label={t('profile.phone')} required hint={t('profile.phoneHint')}><div className="relative"><Phone className="pointer-events-none absolute left-3.5 top-3.5 h-4 w-4 text-muted" aria-hidden="true" /><Input className="pl-10" value={form.phone} onChange={set('phone')} required placeholder="0901234567" inputMode="tel" autoComplete="tel" pattern="0[0-9]{9}" /></div></Field>
                <Field label="Email" required hint={t('profile.emailHint')}><div className="relative"><Mail className="pointer-events-none absolute left-3.5 top-3.5 h-4 w-4 text-muted" aria-hidden="true" /><Input className="pl-10" type="email" value={form.email} onChange={set('email')} placeholder="email@example.com" autoComplete="email" required /></div></Field>
                <div className="sm:col-span-2"><Field label={t('profile.address')}><div className="relative"><MapPin className="pointer-events-none absolute left-3.5 top-3.5 h-4 w-4 text-muted" aria-hidden="true" /><Input className="pl-10" value={form.address} onChange={set('address')} placeholder={t('profile.addressPlaceholder')} autoComplete="street-address" /></div></Field></div>
              </div>
            </section>

            <footer className="flex flex-col gap-3 bg-slate-50 px-5 py-4 sm:flex-row sm:items-center sm:justify-between sm:px-7" aria-live="polite">
              <p className="text-xs font-semibold text-muted">{dirty ? t('profile.unsavedChanges') : t('profile.allSaved')}</p>
              <Button type="submit" loading={saving} disabled={submitDisabled} className="w-full rounded-xl sm:w-auto sm:min-w-40"><Save className="h-4 w-4" aria-hidden="true" />{existing ? t('profile.save') : t('profile.create')}</Button>
            </footer>
          </form>
        </Card>
      </div>
    </div>
  )
}

function DoctorProfile() {
  const toast = useToast()
  const { t } = useI18n()
  const [doc, setDoc] = useState(null)
  const [loading, setLoading] = useState(true)
  const [uploading, setUploading] = useState(false)

  useEffect(() => {
    api
      .get('/doctors/me')
      .then((res) => setDoc(unwrap(res)))
      .catch((e) => toast.error(apiMessage(e)))
      .finally(() => setLoading(false))
  }, []) // eslint-disable-line

  if (loading) return <Spinner />

  async function uploadAvatar(event) {
    const file = event.target.files?.[0]
    event.target.value = ''
    if (!file) return
    if (file.size > 5 * 1024 * 1024) {
      toast.error('Ảnh không được vượt quá 5 MB')
      return
    }
    const body = new FormData()
    body.append('file', file)
    setUploading(true)
    try {
      const result = unwrap(await api.put('/doctors/me/avatar', body))
      setDoc((value) => ({ ...value, avatarStatus: result?.status || 'PENDING' }))
      toast.success('Ảnh đã được gửi và đang chờ quản trị viên duyệt')
    } catch (error) {
      toast.error(apiMessage(error))
    } finally {
      setUploading(false)
    }
  }

  return (
    <>
      <PageHeader title={t('page.profileTitle')} subtitle={t('page.profileSubtitle')} />
      <Card className="max-w-lg p-6">
        <div className="flex items-center gap-4">
          <div className="relative">
            <DoctorAvatar doctor={doc} className="h-20 w-20" rounded="rounded-3xl" priority />
            <label className="absolute -bottom-2 -right-2 flex h-9 w-9 cursor-pointer items-center justify-center rounded-xl border-2 border-white bg-primary text-white shadow-lg transition hover:-translate-y-0.5" aria-label="Chọn ảnh đại diện mới">
              <Camera className="h-4 w-4" aria-hidden="true" />
              <input type="file" className="sr-only" accept="image/jpeg,image/png" onChange={uploadAvatar} disabled={uploading} />
            </label>
          </div>
          <div>
            <p className="text-lg font-semibold text-text">{doc?.fullName}</p>
            <p className="text-sm text-primary">{doc?.specialization}</p>
            <p className="mt-2 text-xs font-semibold text-muted">
              {uploading ? 'Đang xử lý ảnh…' : doc?.avatarStatus === 'PENDING' ? 'Ảnh mới đang chờ duyệt' : 'JPEG/PNG · tối đa 5 MB'}
            </p>
          </div>
        </div>
        <dl className="mt-5 space-y-3 text-sm">
          <Row label={t('profile.workHours')} value={`${formatWorkTime(doc?.workStartTime)} – ${formatWorkTime(doc?.workEndTime)}`} />
          <Row label={t('profile.phone')} value={doc?.phone || '—'} />
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
