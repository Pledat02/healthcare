import { useCallback, useEffect, useMemo, useState } from 'react'
import api, { unwrap, apiMessage } from '../../lib/api'
import { useToast } from '../../components/Toast'
import { useConfirm } from '../../components/Confirm'
import { Button, Card, EmptyState, Field, Input, PageHeader, Select, Spinner } from '../../components/ui'
import Modal from '../../components/Modal'
import { CalendarCheck, Pencil, Search, Trash2, UsersRound } from 'lucide-react'

const GENDERS = {
  MALE: 'Nam',
  FEMALE: 'Nữ',
  OTHER: 'Khác',
}

function shortDate(value) {
  if (!value) return '—'
  return new Intl.DateTimeFormat('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric' }).format(new Date(value))
}

export default function ManagePatientsPage() {
  const toast = useToast()
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
        api.get('/appointments'),
      ])
      setPatients(unwrap(patientRes) || [])
      setAppointments(unwrap(appointmentRes) || [])
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
      title: 'Xóa hồ sơ bệnh nhân',
      message: count
        ? `${patient.fullName} có ${count} lịch hẹn. Hồ sơ bệnh nhân sẽ bị xóa, còn dữ liệu lịch hẹn lịch sử vẫn được giữ lại.`
        : `Xóa hồ sơ của ${patient.fullName} khỏi hệ thống?`,
      confirmText: 'Xóa hồ sơ',
      danger: true,
    })
    if (!ok) return
    try {
      await api.delete(`/patients/${patient.id}`)
      setPatients((items) => items.filter((item) => item.id !== patient.id))
      toast.success('Đã xóa hồ sơ bệnh nhân')
    } catch (e) {
      toast.error(apiMessage(e))
    }
  }

  return (
    <>
      <PageHeader
        title="Quản lý bệnh nhân"
        subtitle={`${patients.length} hồ sơ bệnh nhân · Xem hoạt động và cập nhật thông tin liên hệ`}
      />

      <div className="mb-5 flex flex-col gap-3 sm:flex-row">
        <div className="relative flex-1">
          <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
          <Input
            className="pl-9"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="Tìm theo tên, SĐT, email hoặc địa chỉ…"
            aria-label="Tìm bệnh nhân"
          />
        </div>
        <Select value={gender} onChange={(e) => setGender(e.target.value)} className="sm:w-44" aria-label="Lọc giới tính">
          <option value="">Mọi giới tính</option>
          {Object.entries(GENDERS).map(([value, label]) => <option key={value} value={value}>{label}</option>)}
        </Select>
      </div>

      {loading ? <Spinner /> : filtered.length === 0 ? (
        <EmptyState
          icon={UsersRound}
          title={patients.length ? 'Không tìm thấy bệnh nhân' : 'Chưa có bệnh nhân'}
          subtitle={patients.length ? 'Thử thay đổi từ khóa hoặc bộ lọc' : 'Hồ sơ sẽ xuất hiện khi bệnh nhân hoàn tất đăng ký'}
        />
      ) : (
        <Card className="overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead className="border-b border-border bg-slate-50 text-left text-xs text-muted">
                <tr>
                  <th className="px-4 py-3 font-medium">Bệnh nhân</th>
                  <th className="px-4 py-3 font-medium">Thông tin cá nhân</th>
                  <th className="px-4 py-3 font-medium">Liên hệ</th>
                  <th className="px-4 py-3 font-medium">Hoạt động khám</th>
                  <th className="px-4 py-3 text-right font-medium">Thao tác</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-border">
                {filtered.map((patient) => {
                  const stats = activity[patient.id] || { total: 0, completed: 0, lastVisit: null }
                  return (
                    <tr key={patient.id}>
                      <td className="px-4 py-3">
                        <div className="flex items-center gap-2">
                          <div className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-blue-50 font-semibold text-accent">
                            {(patient.fullName || '?').trim().charAt(0).toUpperCase()}
                          </div>
                          <span className="font-medium text-text">{patient.fullName}</span>
                        </div>
                      </td>
                      <td className="whitespace-nowrap px-4 py-3 text-muted">
                        <span>{GENDERS[patient.gender] || '—'}</span>
                        <span className="block text-xs text-slate-400">Sinh {shortDate(patient.dateOfBirth)}</span>
                      </td>
                      <td className="px-4 py-3 text-muted">
                        <span className="block">{patient.phone || '—'}</span>
                        <span className="block max-w-[15rem] truncate text-xs text-slate-400">{patient.email || patient.address || 'Chưa có thông tin khác'}</span>
                      </td>
                      <td className="whitespace-nowrap px-4 py-3 text-muted">
                        <span className="inline-flex items-center gap-1 font-medium text-text"><CalendarCheck className="h-4 w-4 text-primary" /> {stats.completed} lần đã khám</span>
                        <span className="block text-xs text-slate-400">{stats.total} lịch hẹn · Gần nhất {shortDate(stats.lastVisit)}</span>
                      </td>
                      <td className="px-4 py-3">
                        <div className="flex justify-end gap-1">
                          <button onClick={() => setEditing(patient)} className="rounded-lg p-2 text-slate-400 hover:bg-primary-soft hover:text-primary" aria-label={`Sửa ${patient.fullName}`}>
                            <Pencil className="h-4 w-4" />
                          </button>
                          <button onClick={() => remove(patient)} className="rounded-lg p-2 text-slate-400 hover:bg-red-50 hover:text-danger" aria-label={`Xóa ${patient.fullName}`}>
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
      toast.success('Đã cập nhật hồ sơ bệnh nhân')
      onClose()
      onSaved()
    } catch (err) {
      toast.error(apiMessage(err))
    } finally {
      setSaving(false)
    }
  }

  return (
    <Modal open onClose={onClose} title="Chỉnh sửa bệnh nhân">
      <form onSubmit={submit} className="grid gap-4 sm:grid-cols-2">
        <Field label="Họ và tên" required>
          <Input value={form.fullName} onChange={set('fullName')} required />
        </Field>
        <Field label="Số điện thoại" required hint="10 số, bắt đầu bằng 0">
          <Input value={form.phone} onChange={set('phone')} required pattern="0[0-9]{9}" inputMode="tel" />
        </Field>
        <Field label="Ngày sinh">
          <Input type="date" max={yesterday} value={form.dateOfBirth} onChange={set('dateOfBirth')} />
        </Field>
        <Field label="Giới tính" required>
          <Select value={form.gender} onChange={set('gender')} required>
            {Object.entries(GENDERS).map(([value, label]) => <option key={value} value={value}>{label}</option>)}
          </Select>
        </Field>
        <Field label="Email" required>
          <Input type="email" value={form.email} onChange={set('email')} required />
        </Field>
        <Field label="Địa chỉ">
          <Input value={form.address} onChange={set('address')} />
        </Field>
        <div className="sm:col-span-2 flex justify-end gap-2 pt-2">
          <Button variant="secondary" type="button" onClick={onClose}>Hủy</Button>
          <Button type="submit" loading={saving}>Lưu thay đổi</Button>
        </div>
      </form>
    </Modal>
  )
}
