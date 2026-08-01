import { useCallback, useEffect, useState } from 'react'
import api, { unwrap, apiMessage } from '@/shared/lib/api'
import { formatWorkTime } from '@/shared/lib/format'
import { useToast } from '@/shared/components/Toast'
import { useI18n } from '@/shared/i18n/I18nProvider'
import { useConfirm } from '@/shared/components/Confirm'
import { Button, Card, Field, Input, Spinner, EmptyState, PageHeader } from '@/shared/ui'
import Modal from '@/shared/components/Modal'
import {
  Users, Plus, Trash2, Stethoscope, ChevronLeft, ChevronRight, Pencil, Search,
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
      title: 'Xóa bác sĩ',
      message: `Xóa ${doctor.fullName} khỏi hệ thống? Tài khoản đăng nhập của bác sĩ cũng sẽ bị xóa.`,
      confirmText: 'Xóa bác sĩ',
      danger: true,
    })
    if (!ok) return
    try {
      await api.delete(`/doctors/${doctor.id}`)
      toast.success('Đã xóa bác sĩ')
      if (doctors.length === 1 && page > 0) setPage((value) => value - 1)
      else load()
    } catch (e) {
      toast.error(apiMessage(e))
    }
  }

  return (
    <>
      <PageHeader
        title={t('page.manageDoctorsTitle')}
        subtitle={`${totalElements} bác sĩ trong hệ thống · Có thể cập nhật hồ sơ và giờ làm việc`}
        action={<Button onClick={() => setAdding(true)}><Plus className="h-4 w-4" /> Thêm bác sĩ</Button>}
      />

      <div className="relative mb-5 max-w-md">
        <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
        <Input className="pl-9" value={query} onChange={search} placeholder="Tìm theo tên bác sĩ…" aria-label="Tìm bác sĩ" />
      </div>

      {loading ? <Spinner /> : doctors.length === 0 ? (
        <EmptyState
          icon={Users}
          title={query ? 'Không tìm thấy bác sĩ' : 'Chưa có bác sĩ'}
          subtitle={query ? 'Thử từ khóa khác' : 'Thêm bác sĩ đầu tiên để bệnh nhân đặt lịch'}
          action={!query && <Button onClick={() => setAdding(true)}><Plus className="h-4 w-4" /> Thêm bác sĩ</Button>}
        />
      ) : (
        <>
          <Card className="overflow-hidden">
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead className="border-b border-border bg-slate-50 text-left text-xs text-muted">
                  <tr>
                    <th className="px-4 py-3 font-medium">Bác sĩ</th>
                    <th className="px-4 py-3 font-medium">Chuyên khoa</th>
                    <th className="px-4 py-3 font-medium">Giờ làm việc</th>
                    <th className="px-4 py-3 font-medium">Liên hệ</th>
                    <th className="px-4 py-3 text-right font-medium">Thao tác</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-border">
                  {doctors.map((doctor) => (
                    <tr key={doctor.id}>
                      <td className="px-4 py-3">
                        <div className="flex items-center gap-2">
                          <div className="flex h-8 w-8 items-center justify-center rounded-full bg-primary-soft text-primary">
                            <Stethoscope className="h-4 w-4" />
                          </div>
                          <span className="font-medium text-text">{doctor.fullName}</span>
                        </div>
                      </td>
                      <td className="px-4 py-3 text-muted">{doctor.specialization}</td>
                      <td className="whitespace-nowrap px-4 py-3 tabular-nums text-muted">
                        {formatWorkTime(doctor.workStartTime)}–{formatWorkTime(doctor.workEndTime)}
                      </td>
                      <td className="px-4 py-3 text-muted">
                        <span className="block">{doctor.phone || '—'}</span>
                        <span className="block text-xs text-slate-400">{doctor.email || 'Chưa có email'}</span>
                      </td>
                      <td className="px-4 py-3">
                        <div className="flex justify-end gap-1">
                          <button onClick={() => setEditing(doctor)} className="rounded-lg p-2 text-slate-400 hover:bg-primary-soft hover:text-primary" aria-label={`Sửa ${doctor.fullName}`}>
                            <Pencil className="h-4 w-4" />
                          </button>
                          <button onClick={() => remove(doctor)} className="rounded-lg p-2 text-slate-400 hover:bg-red-50 hover:text-danger" aria-label={`Xóa ${doctor.fullName}`}>
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

          {totalPages > 1 && (
            <div className="mt-4 flex flex-wrap items-center justify-center gap-3">
              <Button variant="secondary" disabled={page === 0} onClick={() => setPage((value) => value - 1)}>
                <ChevronLeft className="h-4 w-4" /> Trước
              </Button>
              <span className="text-sm text-muted">Trang {page + 1}/{totalPages}</span>
              <Button variant="secondary" disabled={page >= totalPages - 1} onClick={() => setPage((value) => value + 1)}>
                Sau <ChevronRight className="h-4 w-4" />
              </Button>
            </div>
          )}
        </>
      )}

      {adding && <DoctorModal onClose={() => setAdding(false)} onSaved={load} toast={toast} />}
      {editing && <DoctorModal doctor={editing} onClose={() => setEditing(null)} onSaved={load} toast={toast} />}
    </>
  )
}

function DoctorModal({ doctor, onClose, onSaved, toast }) {
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
  const set = (key) => (e) => setForm((value) => ({ ...value, [key]: e.target.value }))

  async function submit(e) {
    e.preventDefault()
    setFormError('')
    if (form.workStartTime >= form.workEndTime) {
      setFormError('Giờ kết thúc phải sau giờ bắt đầu')
      return
    }
    setSaving(true)
    const payload = {
      ...form,
      workStartTime: `${form.workStartTime}:00`,
      workEndTime: `${form.workEndTime}:00`,
    }
    try {
      if (isEditing) await api.put(`/doctors/${doctor.id}`, payload)
      else await api.post('/doctors', payload)
      toast.success(isEditing ? 'Đã cập nhật thông tin bác sĩ' : 'Đã thêm bác sĩ')
      onClose()
      onSaved()
    } catch (err) {
      toast.error(apiMessage(err))
    } finally {
      setSaving(false)
    }
  }

  return (
    <Modal open onClose={onClose} title={isEditing ? 'Chỉnh sửa bác sĩ' : 'Thêm bác sĩ'}>
      <form onSubmit={submit} className="grid gap-4 sm:grid-cols-2">
        {!isEditing && (
          <>
            <div className="sm:col-span-2 rounded-lg bg-primary-soft p-3 text-sm text-text">
              Tài khoản đăng nhập sẽ được tạo tự động cho bác sĩ.
            </div>
            <Field label="Tên đăng nhập" required hint="3-30 ký tự: chữ, số, . _ -">
              <Input value={form.username} onChange={set('username')} required pattern="[a-zA-Z0-9._-]{3,30}" placeholder="bacsi02" autoComplete="off" />
            </Field>
            <Field label="Mật khẩu" required hint="Tối thiểu 6 ký tự">
              <Input type="password" value={form.password} onChange={set('password')} required minLength={6} placeholder="••••••" autoComplete="new-password" />
            </Field>
          </>
        )}
        <Field label="Họ và tên" required>
          <Input value={form.fullName} onChange={set('fullName')} required placeholder="BS. Nguyễn Văn A" />
        </Field>
        <Field label="Chuyên khoa" required>
          <Input value={form.specialization} onChange={set('specialization')} required placeholder="Nội tổng quát" />
        </Field>
        <Field label="Số điện thoại" required hint="10 số, bắt đầu bằng 0">
          <Input value={form.phone} onChange={set('phone')} required pattern="0[0-9]{9}" placeholder="0901234567" inputMode="tel" />
        </Field>
        <Field label="Email">
          <Input type="email" value={form.email} onChange={set('email')} placeholder="bacsi@example.com" />
        </Field>
        <Field label="Giờ bắt đầu" required>
          <Input type="time" value={form.workStartTime} onChange={set('workStartTime')} required />
        </Field>
        <Field label="Giờ kết thúc" required>
          <Input type="time" value={form.workEndTime} onChange={set('workEndTime')} required />
        </Field>
        {formError && <p className="sm:col-span-2 text-sm text-danger" role="alert">{formError}</p>}
        <div className="sm:col-span-2 flex justify-end gap-2 pt-2">
          <Button variant="secondary" type="button" onClick={onClose}>Hủy</Button>
          <Button type="submit" loading={saving}>{isEditing ? 'Lưu thay đổi' : 'Thêm bác sĩ'}</Button>
        </div>
      </form>
    </Modal>
  )
}
