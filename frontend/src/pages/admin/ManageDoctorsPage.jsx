import { useEffect, useState } from 'react'
import api, { unwrap, apiMessage } from '../../lib/api'
import { formatWorkTime } from '../../lib/format'
import { useToast } from '../../components/Toast'
import { Button, Card, Field, Input, Spinner, EmptyState, PageHeader } from '../../components/ui'
import Modal from '../../components/Modal'
import { Users, Plus, Trash2, Stethoscope } from 'lucide-react'

const EMPTY = {
  username: '', password: '',
  fullName: '', specialization: '', phone: '', email: '',
  workStartTime: '08:00', workEndTime: '17:00',
}

export default function ManageDoctorsPage() {
  const toast = useToast()
  const [doctors, setDoctors] = useState([])
  const [loading, setLoading] = useState(true)
  const [adding, setAdding] = useState(false)

  function load() {
    setLoading(true)
    api
      .get('/doctors')
      .then((res) => setDoctors(unwrap(res) || []))
      .catch((e) => toast.error(apiMessage(e)))
      .finally(() => setLoading(false))
  }
  useEffect(() => { load() }, []) // eslint-disable-line

  async function remove(id) {
    if (!window.confirm('Xóa bác sĩ này khỏi hệ thống?')) return
    try {
      await api.delete(`/doctors/${id}`)
      toast.success('Đã xóa bác sĩ')
      load()
    } catch (e) {
      toast.error(apiMessage(e))
    }
  }

  return (
    <>
      <PageHeader
        title="Quản lý bác sĩ"
        subtitle="Thêm, xem và xóa bác sĩ trong hệ thống"
        action={<Button onClick={() => setAdding(true)}><Plus className="h-4 w-4" /> Thêm bác sĩ</Button>}
      />

      {loading ? (
        <Spinner />
      ) : doctors.length === 0 ? (
        <EmptyState icon={Users} title="Chưa có bác sĩ" subtitle="Thêm bác sĩ đầu tiên để bệnh nhân đặt lịch" action={<Button onClick={() => setAdding(true)}><Plus className="h-4 w-4" /> Thêm bác sĩ</Button>} />
      ) : (
        <Card className="overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead className="border-b border-border bg-slate-50 text-left text-xs text-muted">
                <tr>
                  <th className="px-4 py-3 font-medium">Bác sĩ</th>
                  <th className="px-4 py-3 font-medium">Chuyên khoa</th>
                  <th className="px-4 py-3 font-medium">Giờ làm việc</th>
                  <th className="px-4 py-3 font-medium">Liên hệ</th>
                  <th className="px-4 py-3"></th>
                </tr>
              </thead>
              <tbody className="divide-y divide-border">
                {doctors.map((d) => (
                  <tr key={d.id}>
                    <td className="px-4 py-3">
                      <div className="flex items-center gap-2">
                        <div className="flex h-8 w-8 items-center justify-center rounded-full bg-primary-soft text-primary">
                          <Stethoscope className="h-4 w-4" />
                        </div>
                        <span className="font-medium text-text">{d.fullName}</span>
                      </div>
                    </td>
                    <td className="px-4 py-3 text-muted">{d.specialization}</td>
                    <td className="px-4 py-3 tabular-nums text-muted">
                      {formatWorkTime(d.workStartTime)}–{formatWorkTime(d.workEndTime)}
                    </td>
                    <td className="px-4 py-3 text-muted">{d.phone || d.email || '—'}</td>
                    <td className="px-4 py-3 text-right">
                      <button onClick={() => remove(d.id)} className="text-slate-400 hover:text-danger" aria-label="Xóa bác sĩ">
                        <Trash2 className="h-4 w-4" />
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </Card>
      )}

      {adding && <AddDoctorModal onClose={() => setAdding(false)} onSaved={load} toast={toast} />}
    </>
  )
}

function AddDoctorModal({ onClose, onSaved, toast }) {
  const [form, setForm] = useState(EMPTY)
  const [saving, setSaving] = useState(false)
  const set = (k) => (e) => setForm((f) => ({ ...f, [k]: e.target.value }))

  async function submit(e) {
    e.preventDefault()
    setSaving(true)
    try {
      await api.post('/doctors', {
        ...form,
        workStartTime: form.workStartTime + ':00',
        workEndTime: form.workEndTime + ':00',
      })
      toast.success('Đã thêm bác sĩ')
      onClose()
      onSaved()
    } catch (err) {
      toast.error(apiMessage(err))
    } finally {
      setSaving(false)
    }
  }

  return (
    <Modal open onClose={onClose} title="Thêm bác sĩ">
      <form onSubmit={submit} className="grid gap-4 sm:grid-cols-2">
        <div className="sm:col-span-2 rounded-lg bg-primary-soft p-3 text-sm text-text">
          Tài khoản đăng nhập sẽ được tạo tự động cho bác sĩ.
        </div>
        <Field label="Tên đăng nhập" required hint="3-30 ký tự: chữ, số, . _ -">
          <Input value={form.username} onChange={set('username')} required placeholder="bacsi02" autoComplete="off" />
        </Field>
        <Field label="Mật khẩu" required hint="Tối thiểu 6 ký tự">
          <Input type="password" value={form.password} onChange={set('password')} required placeholder="••••••" autoComplete="new-password" />
        </Field>
        <Field label="Họ và tên" required>
          <Input value={form.fullName} onChange={set('fullName')} required placeholder="BS. Nguyễn Văn A" />
        </Field>
        <Field label="Chuyên khoa" required>
          <Input value={form.specialization} onChange={set('specialization')} required placeholder="Nội tổng quát" />
        </Field>
        <Field label="Số điện thoại">
          <Input value={form.phone} onChange={set('phone')} placeholder="0901234567" inputMode="tel" />
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
        <div className="sm:col-span-2 flex justify-end gap-2 pt-2">
          <Button variant="secondary" type="button" onClick={onClose}>Hủy</Button>
          <Button type="submit" loading={saving}>Thêm bác sĩ</Button>
        </div>
      </form>
    </Modal>
  )
}
