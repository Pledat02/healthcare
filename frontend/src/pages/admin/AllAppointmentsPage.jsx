import { useEffect, useState, useMemo } from 'react'
import api, { unwrap, apiMessage, fetchByIdsMap } from '../../lib/api'
import { formatDateTime } from '../../lib/format'
import { useToast } from '../../components/Toast'
import { Card, Select, Spinner, EmptyState, PageHeader, StatusBadge } from '../../components/ui'
import { ClipboardList } from 'lucide-react'

const STATUS_OPTIONS = [
  { value: 'PENDING', label: 'Chờ xác nhận' },
  { value: 'CONFIRMED', label: 'Đã xác nhận' },
  { value: 'COMPLETED', label: 'Đã khám' },
  { value: 'CANCELLED', label: 'Đã hủy' },
]

export default function AllAppointmentsPage() {
  const toast = useToast()
  const [items, setItems] = useState([])
  const [doctors, setDoctors] = useState({})
  const [patients, setPatients] = useState({})
  const [loading, setLoading] = useState(true)
  const [filter, setFilter] = useState('')

  useEffect(() => {
    async function load() {
      try {
        const appts = await api.get('/appointments')
        const list = (unwrap(appts) || []).slice().sort((a, b) => new Date(b.appointmentTime) - new Date(a.appointmentTime))
        setItems(list)
        // Batch: lay bac si + benh nhan xuat hien trong lich, moi loai 1 request (bo N+1)
        const [dmap, pmap] = await Promise.all([
          fetchByIdsMap('doctors', list.map((a) => a.doctorId)),
          fetchByIdsMap('patients', list.map((a) => a.patientId)),
        ])
        setDoctors(dmap)
        setPatients(pmap)
      } catch (e) {
        toast.error(apiMessage(e))
      } finally {
        setLoading(false)
      }
    }
    load()
  }, []) // eslint-disable-line

  const filtered = useMemo(
    () => (filter ? items.filter((a) => a.status === filter) : items),
    [items, filter],
  )

  return (
    <>
      <PageHeader
        title="Tất cả lịch hẹn"
        subtitle={`Tổng ${items.length} lịch hẹn trong hệ thống`}
        action={
          <Select value={filter} onChange={(e) => setFilter(e.target.value)} className="w-auto" aria-label="Lọc trạng thái">
            <option value="">Tất cả trạng thái</option>
            {STATUS_OPTIONS.map((s) => <option key={s.value} value={s.value}>{s.label}</option>)}
          </Select>
        }
      />

      {loading ? (
        <Spinner />
      ) : filtered.length === 0 ? (
        <EmptyState icon={ClipboardList} title="Không có lịch hẹn" subtitle="Chưa có lịch hẹn nào khớp bộ lọc" />
      ) : (
        <Card className="overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead className="border-b border-border bg-slate-50 text-left text-xs text-muted">
                <tr>
                  <th className="px-4 py-3 font-medium">Thời gian</th>
                  <th className="px-4 py-3 font-medium">Bệnh nhân</th>
                  <th className="px-4 py-3 font-medium">Bác sĩ</th>
                  <th className="px-4 py-3 font-medium">Lý do</th>
                  <th className="px-4 py-3 font-medium">Trạng thái</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-border">
                {filtered.map((a) => (
                  <tr key={a.id}>
                    <td className="whitespace-nowrap px-4 py-3 tabular-nums text-text">{formatDateTime(a.appointmentTime)}</td>
                    <td className="px-4 py-3 text-text">{patients[a.patientId]?.fullName || '—'}</td>
                    <td className="px-4 py-3 text-muted">
                      {doctors[a.doctorId]?.fullName || '—'}
                      <span className="block text-xs text-slate-400">{doctors[a.doctorId]?.specialization}</span>
                    </td>
                    <td className="max-w-[16rem] truncate px-4 py-3 text-muted">{a.reason || '—'}</td>
                    <td className="px-4 py-3"><StatusBadge status={a.status} /></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </Card>
      )}
    </>
  )
}
