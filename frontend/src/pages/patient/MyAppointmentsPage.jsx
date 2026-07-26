import { useEffect, useState } from 'react'
import api, { unwrap, apiMessage } from '../../lib/api'
import { formatDateTime } from '../../lib/format'
import { useToast } from '../../components/Toast'
import { Button, Card, Spinner, EmptyState, PageHeader, StatusBadge } from '../../components/ui'
import { CalendarDays, Stethoscope, XCircle } from 'lucide-react'

export default function MyAppointmentsPage() {
  const toast = useToast()
  const [items, setItems] = useState([])
  const [doctors, setDoctors] = useState({})
  const [loading, setLoading] = useState(true)
  const [cancelling, setCancelling] = useState(null)

  async function load() {
    setLoading(true)
    try {
      const [appts, docs] = await Promise.all([
        api.get('/appointments/patients/me'),
        api.get('/doctors'),
      ])
      setItems((unwrap(appts) || []).slice().sort((a, b) => new Date(b.appointmentTime) - new Date(a.appointmentTime)))
      const map = {}
      ;(unwrap(docs) || []).forEach((d) => (map[d.id] = d))
      setDoctors(map)
    } catch (e) {
      toast.error(apiMessage(e))
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { load() }, []) // eslint-disable-line

  async function cancel(id) {
    if (!window.confirm('Bạn chắc chắn muốn hủy lịch hẹn này?')) return
    setCancelling(id)
    try {
      await api.patch(`/appointments/${id}/cancel`)
      toast.success('Đã hủy lịch hẹn')
      load()
    } catch (e) {
      toast.error(apiMessage(e))
    } finally {
      setCancelling(null)
    }
  }

  if (loading) return <Spinner />

  return (
    <>
      <PageHeader title="Lịch hẹn của tôi" subtitle="Theo dõi và quản lý các lịch khám đã đặt" />
      {items.length === 0 ? (
        <EmptyState icon={CalendarDays} title="Chưa có lịch hẹn nào" subtitle="Hãy tìm bác sĩ và đặt lịch khám đầu tiên" />
      ) : (
        <div className="space-y-3">
          {items.map((a) => {
            const d = doctors[a.doctorId]
            const canCancel = a.status !== 'COMPLETED' && a.status !== 'CANCELLED'
            return (
              <Card key={a.id} className="flex flex-wrap items-center gap-4 p-4">
                <div className="flex h-11 w-11 items-center justify-center rounded-full bg-[--color-primary-soft] text-[--color-primary]">
                  <Stethoscope className="h-5 w-5" />
                </div>
                <div className="min-w-0 flex-1">
                  <p className="font-semibold text-[--color-text]">{d?.fullName || 'Bác sĩ'}</p>
                  <p className="text-sm text-[--color-muted]">
                    {d?.specialization} · {formatDateTime(a.appointmentTime)}
                  </p>
                  {a.reason && <p className="mt-0.5 truncate text-sm text-slate-400">Lý do: {a.reason}</p>}
                </div>
                <StatusBadge status={a.status} />
                {canCancel && (
                  <Button
                    variant="ghost"
                    className="text-[--color-danger]"
                    loading={cancelling === a.id}
                    onClick={() => cancel(a.id)}
                  >
                    <XCircle className="h-4 w-4" /> Hủy
                  </Button>
                )}
              </Card>
            )
          })}
        </div>
      )}
    </>
  )
}
