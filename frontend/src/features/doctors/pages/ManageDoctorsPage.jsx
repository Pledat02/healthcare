import { useCallback, useEffect, useState } from 'react'
import api, { unwrap, apiMessage } from '@/shared/lib/api'
import { formatWorkTime } from '@/shared/lib/format'
import { useToast } from '@/shared/components/Toast'
import { useI18n } from '@/shared/i18n/I18nProvider'
import { useConfirm } from '@/shared/components/Confirm'
import { Button, Card, Field, Input, Spinner, EmptyState, PageHeader } from '@/shared/ui'
import Modal from '@/shared/components/Modal'
import Paginator from '@/shared/components/Paginator'
import DoctorAvatar from '@/shared/components/DoctorAvatar'
import {
  Users, Plus, Trash2, Pencil, Search, Eye, Check, X, Camera,
} from 'lucide-react'

const PAGE_SIZE = 10
const EMPTY = {
  username: '', password: '', fullName: '', specialization: '', phone: '', email: '',
  workStartTime: '08:00', workEndTime: '17:00',
}

export default function ManageDoctorsPage() {
  const toast = useToast()
  const { t } = useI18n()
  const confirm = useConfirm()
  const [doctors, setDoctors] = useState([])
  const [loading, setLoading] = useState(true)
  const [editing, setEditing] = useState(null)
  const [adding, setAdding] = useState(false)
  const [query, setQuery] = useState('')
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [totalElements, setTotalElements] = useState(0)
  const [reviewing, setReviewing] = useState(null)
  const [reviewLoading, setReviewLoading] = useState(false)

  const load = useCallback(() => {
    setLoading(true)
    api.get('/doctors', { params: { page, size: PAGE_SIZE, q: query || undefined } })
      .then((res) => {
        const data = unwrap(res) || {}
        setDoctors(data.content || [])
        setTotalPages(data.totalPages || 0)
        setTotalElements(data.totalElements || 0)
      })
      .catch((e) => toast.error(apiMessage(e)))
      .finally(() => setLoading(false))
  }, [page, query]) // eslint-disable-line

  useEffect(() => {
    const timer = setTimeout(load, 250)
    return () => clearTimeout(timer)
  }, [load])

  function search(e) {
    setQuery(e.target.value)
    setPage(0)
  }

  async function remove(doctor) {
    const ok = await confirm({
      title: t('manageDoctors.deleteTitle'),
      message: t('manageDoctors.deleteMsg', { name: doctor.fullName }),
      confirmText: t('manageDoctors.deleteTitle'),
      danger: true,
    })
    if (!ok) return
    try {
      await api.delete(`/doctors/${doctor.id}`)
      toast.success(t('manageDoctors.deleted'))
      if (doctors.length === 1 && page > 0) setPage((value) => value - 1)
      else load()
    } catch (e) {
      toast.error(apiMessage(e))
    }
  }

  async function openAvatarReview(doctor) {
    setReviewing({ doctor, data: null })
    setReviewLoading(true)
    try {
      const data = unwrap(await api.get(`/doctors/${doctor.id}/avatar/review`))
      setReviewing({ doctor, data })
    } catch (error) {
      toast.error(apiMessage(error))
      setReviewing(null)
    } finally {
      setReviewLoading(false)
    }
  }

  async function decideAvatar(decision) {
    if (!reviewing) return
    setReviewLoading(true)
    try {
      await api.patch(`/doctors/${reviewing.doctor.id}/avatar/${decision}`)
      toast.success(t(decision === 'approve' ? 'manageDoctors.avatarApproved' : 'manageDoctors.avatarRejected'))
      setReviewing(null)
      load()
    } catch (error) {
      toast.error(apiMessage(error))
    } finally {
      setReviewLoading(false)
    }
  }

  return (
    <>
      <PageHeader
        title={t('page.manageDoctorsTitle')}
        subtitle={t('manageDoctors.subtitle', { count: totalElements })}
        action={<Button onClick={() => setAdding(true)}><Plus className="h-4 w-4" /> {t('manageDoctors.add')}</Button>}
      />

      <Card className="mb-6 max-w-xl p-4"><div className="relative"><Search className="pointer-events-none absolute left-3.5 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" aria-hidden="true" /><Input className="pl-10" value={query} onChange={search} placeholder={t('manageDoctors.searchPlaceholder')} aria-label={t('manageDoctors.searchAria')} /></div></Card>

      {loading ? <Spinner /> : doctors.length === 0 ? (
        <EmptyState
          icon={Users}
          title={query ? t('manageDoctors.notFound') : t('manageDoctors.none')}
          subtitle={query ? t('manageDoctors.tryOther') : t('manageDoctors.noneSub')}
          action={!query && <Button onClick={() => setAdding(true)}><Plus className="h-4 w-4" /> {t('manageDoctors.add')}</Button>}
        />
      ) : (
        <>
          <Card className="overflow-hidden">
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead className="border-b border-border bg-slate-50 text-left text-xs text-muted">
                  <tr>
                    <th className="min-w-52 px-4 py-3 font-medium">{t('common.doctor')}</th>
                    <th className="px-4 py-3 font-medium">{t('manageDoctors.colSpecialty')}</th>
                    <th className="px-4 py-3 font-medium">{t('manageDoctors.colHours')}</th>
                    <th className="px-4 py-3 font-medium">{t('manageDoctors.colContact')}</th>
                    <th className="px-4 py-3 text-right font-medium">{t('manageDoctors.colActions')}</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-border">
                  {doctors.map((doctor) => (
                    <tr key={doctor.id}>
                      <td className="min-w-52 px-4 py-3">
                        <div className="flex items-center gap-2">
                          <DoctorAvatar doctor={doctor} className="h-9 w-9" rounded="rounded-xl" />
                          <span className="font-medium text-text">{doctor.fullName}</span>
                        </div>
                      </td>
                      <td className="px-4 py-3 text-muted">{doctor.specialization}</td>
                      <td className="whitespace-nowrap px-4 py-3 tabular-nums text-muted">
                        {formatWorkTime(doctor.workStartTime)}–{formatWorkTime(doctor.workEndTime)}
                      </td>
                      <td className="px-4 py-3 text-muted">
                        <span className="block">{doctor.phone || '—'}</span>
                        <span className="block text-xs text-slate-400">{doctor.email || t('manageDoctors.noEmail')}</span>
                      </td>
                      <td className="px-4 py-3">
                        <div className="flex justify-end gap-1">
                          {doctor.avatarStatus === 'PENDING' && (
                            <button onClick={() => openAvatarReview(doctor)} className="rounded-lg bg-amber-50 p-2 text-amber-700 hover:bg-amber-100" aria-label={t('manageDoctors.reviewAvatarAria', { name: doctor.fullName })}>
                              <Eye className="h-4 w-4" />
                            </button>
                          )}
                          <button onClick={() => setEditing(doctor)} className="rounded-lg p-2 text-slate-400 hover:bg-primary-soft hover:text-primary" aria-label={t('manageDoctors.editAria', { name: doctor.fullName })}>
                            <Pencil className="h-4 w-4" />
                          </button>
                          <button onClick={() => remove(doctor)} className="rounded-lg p-2 text-slate-400 hover:bg-red-50 hover:text-danger" aria-label={t('manageDoctors.deleteAria', { name: doctor.fullName })}>
                            <Trash2 className="h-4 w-4" />
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </Card>

          <Paginator page={page} totalPages={totalPages} onPage={setPage}
            info={t('manageDoctors.pageInfo', { page: page + 1, total: totalPages })} />
        </>
      )}

      {adding && <DoctorModal onClose={() => setAdding(false)} onSaved={load} toast={toast} />}
      {editing && <DoctorModal doctor={editing} onClose={() => setEditing(null)} onSaved={load} toast={toast} />}
      {reviewing && (
        <Modal open onClose={() => !reviewLoading && setReviewing(null)} title={t('manageDoctors.reviewAvatarTitle')}>
          {reviewLoading && !reviewing.data ? <Spinner /> : (
            <div className="space-y-5">
              <div className="flex items-center gap-4 rounded-2xl bg-slate-50 p-4">
                {reviewing.data?.pendingAvatarUrl ? (
                  <img src={reviewing.data.pendingAvatarUrl} alt={t('manageDoctors.pendingAvatarAlt', { name: reviewing.doctor.fullName })} className="h-28 w-28 rounded-2xl object-cover shadow-sm" width="112" height="112" />
                ) : <DoctorAvatar doctor={reviewing.doctor} className="h-28 w-28" />}
                <div><p className="font-extrabold text-text">{reviewing.doctor.fullName}</p><p className="mt-1 text-sm text-primary">{reviewing.doctor.specialization}</p><p className="mt-3 text-xs leading-5 text-muted">{t('manageDoctors.reviewAvatarHint')}</p></div>
              </div>
              <div className="flex justify-end gap-2">
                <Button variant="secondary" disabled={reviewLoading} onClick={() => decideAvatar('reject')}><X className="h-4 w-4" />{t('manageDoctors.rejectAvatar')}</Button>
                <Button loading={reviewLoading} onClick={() => decideAvatar('approve')}><Check className="h-4 w-4" />{t('manageDoctors.approveAvatar')}</Button>
              </div>
            </div>
          )}
        </Modal>
      )}
    </>
  )
}

function DoctorModal({ doctor, onClose, onSaved, toast }) {
  const { t } = useI18n()
  const isEditing = Boolean(doctor)
  const [form, setForm] = useState(() => doctor ? {
    fullName: doctor.fullName || '',
    specialization: doctor.specialization || '',
    phone: doctor.phone || '',
    email: doctor.email || '',
    workStartTime: formatWorkTime(doctor.workStartTime),
    workEndTime: formatWorkTime(doctor.workEndTime),
  } : EMPTY)
  const [saving, setSaving] = useState(false)
  const [formError, setFormError] = useState('')
  const [avatarFile, setAvatarFile] = useState(null)
  const [avatarPreview, setAvatarPreview] = useState('')
  const set = (key) => (e) => setForm((value) => ({ ...value, [key]: e.target.value }))

  useEffect(() => {
    if (!avatarFile) { setAvatarPreview(''); return }
    const url = URL.createObjectURL(avatarFile)
    setAvatarPreview(url)
    return () => URL.revokeObjectURL(url)
  }, [avatarFile])

  function pickAvatar(e) {
    const file = e.target.files?.[0]
    e.target.value = ''
    if (!file) return
    if (file.size > 5 * 1024 * 1024) {
      toast.error(t('manageDoctors.avatarTooLarge'))
      return
    }
    setAvatarFile(file)
  }

  async function submit(e) {
    e.preventDefault()
    setFormError('')
    if (form.workStartTime >= form.workEndTime) {
      setFormError(t('manageDoctors.timeError'))
      return
    }
    setSaving(true)
    const payload = {
      ...form,
      workStartTime: `${form.workStartTime}:00`,
      workEndTime: `${form.workEndTime}:00`,
    }
    try {
      let id = doctor?.id
      if (isEditing) await api.put(`/doctors/${doctor.id}`, payload)
      else id = unwrap(await api.post('/doctors', payload))?.id
      let avatarFailed = false
      if (avatarFile && id) {
        try {
          const body = new FormData()
          body.append('file', avatarFile)
          await api.put(`/doctors/${id}/avatar`, body)
        } catch {
          avatarFailed = true
        }
      }
      if (avatarFailed) toast.error(t('manageDoctors.avatarUploadFailed'))
      else toast.success(isEditing ? t('manageDoctors.updated') : t('manageDoctors.added'))
      onClose()
      onSaved()
    } catch (err) {
      toast.error(apiMessage(err))
    } finally {
      setSaving(false)
    }
  }

  return (
    <Modal open onClose={onClose} title={isEditing ? t('manageDoctors.editTitle') : t('manageDoctors.add')}>
      <form onSubmit={submit} className="grid gap-4 sm:grid-cols-2">
        <div className="sm:col-span-2 flex items-center gap-4">
          {avatarPreview ? (
            <img src={avatarPreview} alt="" className="h-16 w-16 shrink-0 rounded-2xl object-cover shadow-sm" width="64" height="64" />
          ) : (
            <DoctorAvatar doctor={isEditing ? doctor : null} className="h-16 w-16" />
          )}
          <div className="min-w-0">
            <p className="text-sm font-medium text-text">{t('manageDoctors.avatarLabel')}</p>
            <div className="mt-1 flex items-center gap-3">
              <label className="inline-flex cursor-pointer items-center gap-1.5 rounded-lg border border-border px-3 py-1.5 text-sm font-medium text-text hover:bg-slate-50">
                <Camera className="h-4 w-4" aria-hidden="true" />
                {avatarPreview ? t('manageDoctors.avatarChange') : t('manageDoctors.avatarChoose')}
                <input type="file" className="sr-only" accept="image/jpeg,image/png" onChange={pickAvatar} disabled={saving} />
              </label>
              {avatarFile && (
                <button type="button" onClick={() => setAvatarFile(null)} className="text-sm text-muted hover:text-danger">
                  {t('manageDoctors.avatarRemove')}
                </button>
              )}
            </div>
            <p className="mt-1 text-xs text-muted">{t('manageDoctors.avatarHint')}</p>
          </div>
        </div>
        {!isEditing && (
          <>
            <div className="sm:col-span-2 rounded-lg bg-primary-soft p-3 text-sm text-text">
              {t('manageDoctors.accountNote')}
            </div>
            <Field label={t('manageDoctors.username')} required hint={t('manageDoctors.usernameHint')}>
              <Input value={form.username} onChange={set('username')} required pattern="[a-zA-Z0-9._-]{3,30}" placeholder="bacsi02" autoComplete="off" />
            </Field>
            <Field label={t('manageDoctors.password')} required hint={t('manageDoctors.passwordHint')}>
              <Input type="password" value={form.password} onChange={set('password')} required minLength={6} placeholder="••••••" autoComplete="new-password" />
            </Field>
          </>
        )}
        <Field label={t('manageDoctors.fullName')} required>
          <Input value={form.fullName} onChange={set('fullName')} required placeholder={t('manageDoctors.fullNamePlaceholder')} />
        </Field>
        <Field label={t('manageDoctors.colSpecialty')} required>
          <Input value={form.specialization} onChange={set('specialization')} required placeholder={t('manageDoctors.specialtyPlaceholder')} />
        </Field>
        <Field label={t('manageDoctors.phone')} required hint={t('manageDoctors.phoneHint')}>
          <Input value={form.phone} onChange={set('phone')} required pattern="0[0-9]{9}" placeholder="0901234567" inputMode="tel" />
        </Field>
        <Field label="Email">
          <Input type="email" value={form.email} onChange={set('email')} placeholder="bacsi@example.com" />
        </Field>
        <Field label={t('manageDoctors.startTime')} required>
          <Input type="time" value={form.workStartTime} onChange={set('workStartTime')} required />
        </Field>
        <Field label={t('manageDoctors.endTime')} required>
          <Input type="time" value={form.workEndTime} onChange={set('workEndTime')} required />
        </Field>
        {formError && <p className="sm:col-span-2 text-sm text-danger" role="alert">{formError}</p>}
        <div className="sm:col-span-2 flex justify-end gap-2 pt-2">
          <Button variant="secondary" type="button" onClick={onClose}>{t('common.cancel')}</Button>
          <Button type="submit" loading={saving}>{isEditing ? t('manageDoctors.saveChanges') : t('manageDoctors.add')}</Button>
        </div>
      </form>
    </Modal>
  )
}
