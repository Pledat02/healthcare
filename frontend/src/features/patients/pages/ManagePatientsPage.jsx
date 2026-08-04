import { useCallback, useEffect, useMemo, useState } from 'react'
import api, { unwrap, apiMessage } from '@/shared/lib/api'
import { useToast } from '@/shared/components/Toast'
import { useI18n } from '@/shared/i18n/I18nProvider'
import { useConfirm } from '@/shared/components/Confirm'
import { Button, Card, EmptyState, Field, Input, PageHeader, Select, Spinner } from '@/shared/ui'
import Modal from '@/shared/components/Modal'
import { CalendarCheck, Pencil, Search, Trash2, UsersRound } from 'lucide-react'

const GENDER_VALUES = ['MALE', 'FEMALE', 'OTHER']

function shortDate(value) {
  if (!value) return '—'
  return new Intl.DateTimeFormat('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric' }).format(new Date(value))
}

export default function ManagePatientsPage() {
  const toast = useToast()
  const { t } = useI18n()
  const confirm = useConfirm()
  const [patients, setPatients] = useState([])
  const [appointments, setAppointments] = useState([])
  const [loading, setLoading] = useState(true)
  const [query, setQuery] = useState('')
  const [gender, setGender] = useState('')
  const [editing, setEditing] = useState(null)

  const load = useCallback(async () => {
    setLoading(true)
    try {
      const [patientRes, appointmentRes] = await Promise.all([
        api.get('/patients'),
        // /appointments tra ve PageResponse (phan trang) -> lay .content. size 100 de
        // tinh hoat dong kham (gioi han: >100 lich thi thong ke chua day du - can aggregate BE).
        api.get('/appointments', { params: { size: 100 } }),
      ])
      setPatients(unwrap(patientRes) || [])
      const appts = unwrap(appointmentRes)
      setAppointments(Array.isArray(appts) ? appts : (appts?.content || []))
    } catch (e) {
      toast.error(apiMessage(e))
    } finally {
      setLoading(false)
    }
  }, []) // eslint-disable-line

  useEffect(() => { load() }, [load])

  const activity = useMemo(() => {
    const map = {}
    appointments.forEach((appointment) => {
      const current = map[appointment.patientId] || { total: 0, completed: 0, lastVisit: null }
      current.total += 1
      if (appointment.status === 'COMPLETED') {
        current.completed += 1
        if (!current.lastVisit || new Date(appointment.appointmentTime) > new Date(current.lastVisit)) {
          current.lastVisit = appointment.appointmentTime
        }
      }
      map[appointment.patientId] = current
    })
    return map
  }, [appointments])

  const filtered = useMemo(() => {
    const keyword = query.trim().toLowerCase()
    return patients.filter((patient) => {
      const matchesGender = !gender || patient.gender === gender
      const searchable = [patient.fullName, patient.phone, patient.email, patient.address].filter(Boolean).join(' ').toLowerCase()
      return matchesGender && (!keyword || searchable.includes(keyword))
    })
  }, [patients, query, gender])

  async function remove(patient) {
    const count = activity[patient.id]?.total || 0
    const ok = await confirm({
      title: t('managePatients.deleteTitle'),
      message: count
        ? t('managePatients.deleteMsgWithAppts', { name: patient.fullName, count })
        : t('managePatients.deleteMsgNoAppts', { name: patient.fullName }),
      confirmText: t('managePatients.deleteConfirm'),
      danger: true,
    })
    if (!ok) return
    try {
      await api.delete(`/patients/${patient.id}`)
      setPatients((items) => items.filter((item) => item.id !== patient.id))
      toast.success(t('managePatients.deleted'))
    } catch (e) {
      toast.error(apiMessage(e))
    }
  }

  return (
    <>
      <PageHeader
        title={t('page.managePatientsTitle')}
        subtitle={t('managePatients.subtitle', { count: patients.length })}
      />

      <Card className="mb-6 flex flex-col gap-3 p-4 sm:flex-row">
        <div className="relative flex-1">
          <Search className="pointer-events-none absolute left-3.5 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" aria-hidden="true" />
          <Input
            className="pl-10"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder={t('managePatients.searchPlaceholder')}
            aria-label={t('managePatients.searchAria')}
          />
        </div>
        <Select value={gender} onChange={(e) => setGender(e.target.value)} className="sm:w-44" aria-label={t('managePatients.genderAria')}>
          <option value="">{t('managePatients.allGenders')}</option>
          {GENDER_VALUES.map((v) => <option key={v} value={v}>{t(`gender.${v}`)}</option>)}
        </Select>
      </Card>

      {loading ? <Spinner /> : filtered.length === 0 ? (
        <EmptyState
          icon={UsersRound}
          title={patients.length ? t('managePatients.notFound') : t('managePatients.none')}
          subtitle={patients.length ? t('managePatients.tryOther') : t('managePatients.noneSub')}
        />
      ) : (
        <Card className="overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead className="border-b border-border bg-slate-50 text-left text-xs text-muted">
                <tr>
                  <th className="min-w-48 px-4 py-3 font-medium">{t('common.patient')}</th>
                  <th className="px-4 py-3 font-medium">{t('managePatients.colPersonal')}</th>
                  <th className="px-4 py-3 font-medium">{t('managePatients.colContact')}</th>
                  <th className="px-4 py-3 font-medium">{t('managePatients.colActivity')}</th>
                  <th className="px-4 py-3 text-right font-medium">{t('managePatients.colActions')}</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-border">
                {filtered.map((patient) => {
                  const stats = activity[patient.id] || { total: 0, completed: 0, lastVisit: null }
                  return (
                    <tr key={patient.id}>
                      <td className="min-w-48 px-4 py-3">
                        <div className="flex items-center gap-2">
                          <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-gradient-to-br from-blue-500 to-cyan-600 font-extrabold text-white shadow-sm">
                            {(patient.fullName || '?').trim().charAt(0).toUpperCase()}
                          </div>
                          <span className="font-medium text-text">{patient.fullName}</span>
                        </div>
                      </td>
                      <td className="whitespace-nowrap px-4 py-3 text-muted">
                        <span>{patient.gender ? t(`gender.${patient.gender}`) : '—'}</span>
                        <span className="block text-xs text-slate-400">{t('managePatients.born', { date: shortDate(patient.dateOfBirth) })}</span>
                      </td>
                      <td className="px-4 py-3 text-muted">
                        <span className="block">{patient.phone || '—'}</span>
                        <span className="block max-w-[15rem] truncate text-xs text-slate-400">{patient.email || patient.address || t('managePatients.noOtherInfo')}</span>
                      </td>
                      <td className="whitespace-nowrap px-4 py-3 text-muted">
                        <span className="inline-flex items-center gap-1 font-medium text-text"><CalendarCheck className="h-4 w-4 text-primary" /> {t('managePatients.visitsCompleted', { n: stats.completed })}</span>
                        <span className="block text-xs text-slate-400">{t('managePatients.apptSummary', { total: stats.total, last: shortDate(stats.lastVisit) })}</span>
                      </td>
                      <td className="px-4 py-3">
                        <div className="flex justify-end gap-1">
                          <button onClick={() => setEditing(patient)} className="rounded-lg p-2 text-slate-400 hover:bg-primary-soft hover:text-primary" aria-label={t('managePatients.editAria', { name: patient.fullName })}>
                            <Pencil className="h-4 w-4" />
                          </button>
                          <button onClick={() => remove(patient)} className="rounded-lg p-2 text-slate-400 hover:bg-red-50 hover:text-danger" aria-label={t('managePatients.deleteAria', { name: patient.fullName })}>
                            <Trash2 className="h-4 w-4" />
                          </button>
                        </div>
                      </td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          </div>
        </Card>
      )}

      {editing && <EditPatientModal patient={editing} onClose={() => setEditing(null)} onSaved={load} toast={toast} />}
    </>
  )
}

function EditPatientModal({ patient, onClose, onSaved, toast }) {
  const { t } = useI18n()
  const [form, setForm] = useState({
    fullName: patient.fullName || '',
    phone: patient.phone || '',
    dateOfBirth: patient.dateOfBirth?.slice(0, 10) || '',
    gender: patient.gender || 'OTHER',
    email: patient.email || '',
    address: patient.address || '',
  })
  const [saving, setSaving] = useState(false)
  const set = (key) => (e) => setForm((value) => ({ ...value, [key]: e.target.value }))
  const yesterday = new Date(Date.now() - 86400000).toISOString().slice(0, 10)

  async function submit(e) {
    e.preventDefault()
    setSaving(true)
    try {
      await api.put(`/patients/${patient.id}`, {
        ...form,
        dateOfBirth: form.dateOfBirth || null,
      })
      toast.success(t('managePatients.updated'))
      onClose()
      onSaved()
    } catch (err) {
      toast.error(apiMessage(err))
    } finally {
      setSaving(false)
    }
  }

  return (
    <Modal open onClose={onClose} title={t('managePatients.editTitle')}>
      <form onSubmit={submit} className="grid gap-4 sm:grid-cols-2">
        <Field label={t('managePatients.fullName')} required>
          <Input value={form.fullName} onChange={set('fullName')} required />
        </Field>
        <Field label={t('managePatients.phone')} required hint={t('managePatients.phoneHint')}>
          <Input value={form.phone} onChange={set('phone')} required pattern="0[0-9]{9}" inputMode="tel" />
        </Field>
        <Field label={t('managePatients.dob')}>
          <Input type="date" max={yesterday} value={form.dateOfBirth} onChange={set('dateOfBirth')} />
        </Field>
        <Field label={t('managePatients.gender')} required>
          <Select value={form.gender} onChange={set('gender')} required>
            {GENDER_VALUES.map((v) => <option key={v} value={v}>{t(`gender.${v}`)}</option>)}
          </Select>
        </Field>
        <Field label="Email" required>
          <Input type="email" value={form.email} onChange={set('email')} required />
        </Field>
        <Field label={t('managePatients.address')}>
          <Input value={form.address} onChange={set('address')} />
        </Field>
        <div className="sm:col-span-2 flex justify-end gap-2 pt-2">
          <Button variant="secondary" type="button" onClick={onClose}>{t('common.cancel')}</Button>
          <Button type="submit" loading={saving}>{t('managePatients.saveChanges')}</Button>
        </div>
      </form>
    </Modal>
  )
}
