import { useState } from 'react'
import api, { apiMessage } from '@/shared/lib/api'
import { usePaginatedList } from '@/shared/hooks/usePaginatedList'
import { useToast } from '@/shared/components/Toast'
import { useI18n } from '@/shared/i18n/I18nProvider'
import { Spinner, EmptyState, PageHeader } from '@/shared/ui'
import Paginator from '@/shared/components/Paginator'
import AppointmentRow from '@/features/appointments/components/AppointmentRow'
import RecordModal from '@/features/appointments/components/RecordModal'
import { History } from 'lucide-react'

// Trang rieng: lich su hen cua bac si (cac buoi da qua), phan trang moi nhat truoc
export default function AppointmentHistoryPage() {
  const toast = useToast()
  const { t } = useI18n()
  const [acting, setActing] = useState(null)
  const [recording, setRecording] = useState(null)
  const { items, page, setPage, totalPages, loading, reload } = usePaginatedList(
    '/appointments/doctors/me/history', { pageSize: 10 },
  )

  async function act(id, action, okMsg) {
    setActing(id + action)
    try {
      await api.patch(`/appointments/${id}/${action}`)
      toast.success(okMsg)
      reload()
    } catch (e) { toast.error(apiMessage(e)) } finally { setActing(null) }
  }

  const rowProps = { acting, act, onRecord: setRecording, t }

  return (
    <>
      <PageHeader title={t('schedule.historyTitle')} subtitle={t('schedule.historySubtitle')} />

      {loading ? (
        <Spinner />
      ) : items.length === 0 ? (
        <EmptyState icon={History} title={t('schedule.historyEmpty')} subtitle={t('schedule.historyEmptySub')} />
      ) : (
        <>
          <div className="space-y-3">
            {items.map((a) => <AppointmentRow key={a.id} a={a} withDate {...rowProps} />)}
          </div>
          <Paginator page={page} totalPages={totalPages} onPage={setPage} info={`${page + 1}/${totalPages}`} />
        </>
      )}

      {recording && (
        <RecordModal appointment={recording} onClose={() => setRecording(null)} onSaved={reload} toast={toast} />
      )}
    </>
  )
}
