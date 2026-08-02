import { useEffect, useState, useCallback } from 'react'
import api, { unwrap, apiMessage } from '@/shared/lib/api'
import { useToast } from '@/shared/components/Toast'
import { useI18n } from '@/shared/i18n/I18nProvider'
import { Button, Spinner, EmptyState, PageHeader } from '@/shared/ui'
import AppointmentRow from '@/features/appointments/components/AppointmentRow'
import RecordModal from '@/features/appointments/components/RecordModal'
import { History, ChevronLeft, ChevronRight } from 'lucide-react'

// Trang rieng: lich su hen cua bac si (cac buoi da qua), phan trang moi nhat truoc
export default function AppointmentHistoryPage() {
  const toast = useToast()
  const { t } = useI18n()
  const [data, setData] = useState({ content: [], totalPages: 0 })
  const [page, setPage] = useState(0)
  const [loading, setLoading] = useState(true)
  const [acting, setActing] = useState(null)
  const [recording, setRecording] = useState(null)

  const load = useCallback(async () => {
    setLoading(true)
    try {
      const res = await api.get('/appointments/doctors/me/history', { params: { page, size: 20 } })
      setData(unwrap(res) || { content: [], totalPages: 0 })
    } catch (e) { toast.error(apiMessage(e)) } finally { setLoading(false) }
  }, [page]) // eslint-disable-line

  useEffect(() => { load() }, [load])

  async function act(id, action, okMsg) {
    setActing(id + action)
    try {
      await api.patch(`/appointments/${id}/${action}`)
      toast.success(okMsg)
      load()
    } catch (e) { toast.error(apiMessage(e)) } finally { setActing(null) }
  }

  const rowProps = { acting, act, onRecord: setRecording, t }

  return (
    <>
      <PageHeader title={t('schedule.historyTitle')} subtitle={t('schedule.historySubtitle')} />

      {loading ? (
        <Spinner />
      ) : data.content.length === 0 ? (
        <EmptyState icon={History} title={t('schedule.historyEmpty')} subtitle={t('schedule.historyEmptySub')} />
      ) : (
        <>
          <div className="space-y-2">
            {data.content.map((a) => <AppointmentRow key={a.id} a={a} withDate {...rowProps} />)}
          </div>
          {data.totalPages > 1 && (
            <div className="mt-4 flex items-center justify-center gap-3">
              <Button variant="secondary" disabled={page === 0} onClick={() => setPage((p) => p - 1)}>
                <ChevronLeft className="h-4 w-4" /> {t('common.prev')}
              </Button>
              <span className="text-sm text-muted">{page + 1}/{data.totalPages}</span>
              <Button variant="secondary" disabled={page >= data.totalPages - 1} onClick={() => setPage((p) => p + 1)}>
                {t('common.next')} <ChevronRight className="h-4 w-4" />
              </Button>
            </div>
          )}
        </>
      )}

      {recording && (
        <RecordModal appointment={recording} onClose={() => setRecording(null)} onSaved={load} toast={toast} />
      )}
    </>
  )
}
