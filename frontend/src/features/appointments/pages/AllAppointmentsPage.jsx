import { useEffect, useState } from 'react'
import api, { unwrap, apiMessage, fetchByIdsMap } from '@/shared/lib/api'
import { formatDateTime } from '@/shared/lib/format'
import { useToast } from '@/shared/components/Toast'
import { useI18n } from '@/shared/i18n/I18nProvider'
import { Button, Card, Select, Spinner, EmptyState, PageHeader, StatusBadge } from '@/shared/ui'
import { ClipboardList, ChevronLeft, ChevronRight } from 'lucide-react'

const STATUS_VALUES = ['PENDING', 'CONFIRMED', 'COMPLETED', 'CANCELLED']

const PAGE_SIZE = 20

export default function AllAppointmentsPage() {
  const toast = useToast()
  const { t } = useI18n()
  const [items, setItems] = useState([])
  const [doctors, setDoctors] = useState({})
  const [loading, setLoading] = useState(true)
  const [filter, setFilter] = useState('')
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [totalElements, setTotalElements] = useState(0)

  // Doi bo loc -> ve trang dau
  useEffect(() => { setPage(0) }, [filter])

  // Phan trang phia SERVER: khong tai toan bang, loc trang thai o backend
  useEffect(() => {
    let cancelled = false
    async function load() {
      setLoading(true)
      try {
        const res = await api.get('/appointments', {
          params: { page, size: PAGE_SIZE, status: filter || undefined },
        })
        const d = unwrap(res) || {}
        const list = d.content || []
        if (cancelled) return
        setItems(list)
        setTotalPages(d.totalPages || 0)
        setTotalElements(d.totalElements || 0)
        // Ten benh nhan da co san (appointment-service lam giau); chi batch ten bac si
        setDoctors(await fetchByIdsMap('doctors', list.map((a) => a.doctorId)))
      } catch (e) {
        if (!cancelled) toast.error(apiMessage(e))
      } finally {
        if (!cancelled) setLoading(false)
      }
    }
    load()
    return () => { cancelled = true }
  }, [page, filter]) // eslint-disable-line

  return (
    <>
      <PageHeader
        title={t('page.allApptTitle')}
        subtitle={t('allAppt.subtitle', { count: totalElements })}
        action={
          <Select value={filter} onChange={(e) => setFilter(e.target.value)} className="w-auto" aria-label={t('allAppt.filterAria')}>
            <option value="">{t('allAppt.allStatus')}</option>
            {STATUS_VALUES.map((v) => <option key={v} value={v}>{t(`status.${v}`)}</option>)}
          </Select>
        }
      />

      {loading ? (
        <Spinner />
      ) : items.length === 0 ? (
        <EmptyState icon={ClipboardList} title={t('allAppt.empty')} subtitle={t('allAppt.emptySub')} />
      ) : (
        <>
          <Card className="overflow-hidden">
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead className="border-b border-border bg-slate-50 text-left text-xs text-muted">
                  <tr>
                    <th className="px-4 py-3 font-medium">{t('table.time')}</th>
                    <th className="px-4 py-3 font-medium">{t('table.patient')}</th>
                    <th className="px-4 py-3 font-medium">{t('table.doctor')}</th>
                    <th className="px-4 py-3 font-medium">{t('table.reason')}</th>
                    <th className="px-4 py-3 font-medium">{t('table.status')}</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-border">
                  {items.map((a) => (
                    <tr key={a.id}>
                      <td className="whitespace-nowrap px-4 py-3 tabular-nums text-text">{formatDateTime(a.appointmentTime)}</td>
                      <td className="px-4 py-3 text-text">{a.patientName || '—'}</td>
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

          {totalPages > 1 && (
            <div className="mt-4 flex items-center justify-center gap-3">
              <Button variant="secondary" disabled={page === 0} onClick={() => setPage((p) => p - 1)}>
                <ChevronLeft className="h-4 w-4" /> {t('common.prev')}
              </Button>
              <span className="text-sm text-muted">{t('allAppt.pageInfo', { page: page + 1, total: totalPages, count: totalElements })}</span>
              <Button variant="secondary" disabled={page >= totalPages - 1} onClick={() => setPage((p) => p + 1)}>
                {t('common.next')} <ChevronRight className="h-4 w-4" />
              </Button>
            </div>
          )}
        </>
      )}
    </>
  )
}
