import { useEffect, useState } from 'react'
import api, { unwrap, apiMessage } from '@/shared/lib/api'
import { formatDateTime } from '@/shared/lib/format'
import { useToast } from '@/shared/components/Toast'
import { useI18n } from '@/shared/i18n/I18nProvider'
import { Button, Card, Spinner, EmptyState, PageHeader } from '@/shared/ui'
import { FileText, Pill, Download } from 'lucide-react'

export default function MyRecordsPage() {
  const toast = useToast()
  const { t } = useI18n()
  const [records, setRecords] = useState([])
  const [loading, setLoading] = useState(true)
  const [downloading, setDownloading] = useState(null)

  async function downloadPdf(id) {
    setDownloading(id)
    try {
      const res = await api.get(`/medical-records/${id}/pdf`, { responseType: 'blob' })
      const url = URL.createObjectURL(new Blob([res.data], { type: 'application/pdf' }))
      const a = document.createElement('a')
      a.href = url
      a.download = `ho-so-kham-${id}.pdf`
      document.body.appendChild(a)
      a.click()
      a.remove()
      URL.revokeObjectURL(url)
    } catch (e) {
      toast.error(apiMessage(e))
    } finally {
      setDownloading(null)
    }
  }

  useEffect(() => {
    api
      .get('/medical-records/me')
      .then((res) => setRecords((unwrap(res) || []).slice().sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt))))
      .catch((e) => toast.error(apiMessage(e)))
      .finally(() => setLoading(false))
  }, []) // eslint-disable-line

  if (loading) return <Spinner />

  return (
    <>
      <PageHeader title={t('page.recordsTitle')} subtitle={t('page.recordsSubtitle')} />
      {records.length === 0 ? (
        <EmptyState icon={FileText} title={t('records.empty')} subtitle={t('records.emptySub')} />
      ) : (
        <div className="space-y-4">
          {records.map((r) => (
            <Card key={r.id} className="p-5">
              <div className="mb-3 flex items-center justify-between">
                <span className="text-sm text-muted">{formatDateTime(r.createdAt)}</span>
                <Button
                  variant="ghost"
                  loading={downloading === r.id}
                  onClick={() => downloadPdf(r.id)}
                  title={t('records.downloadTip')}
                >
                  <Download className="h-4 w-4" /> {t('records.downloadPdf')}
                </Button>
              </div>
              <div>
                <p className="text-xs font-medium uppercase tracking-wide text-muted">{t('records.diagnosis')}</p>
                <p className="mt-0.5 font-medium text-text">{r.diagnosis || '—'}</p>
              </div>
              {r.notes && (
                <div className="mt-3">
                  <p className="text-xs font-medium uppercase tracking-wide text-muted">{t('records.notes')}</p>
                  <p className="mt-0.5 text-sm text-text">{r.notes}</p>
                </div>
              )}
              {r.prescriptionItems?.length > 0 && (
                <div className="mt-4">
                  <p className="mb-2 flex items-center gap-1.5 text-xs font-medium uppercase tracking-wide text-muted">
                    <Pill className="h-3.5 w-3.5" /> {t('records.prescription')}
                  </p>
                  <div className="overflow-hidden rounded-lg border border-border">
                    <table className="w-full text-sm">
                      <thead className="bg-slate-50 text-left text-xs text-muted">
                        <tr>
                          <th className="px-3 py-2 font-medium">{t('records.medicine')}</th>
                          <th className="px-3 py-2 font-medium">{t('records.dosage')}</th>
                          <th className="px-3 py-2 font-medium tabular-nums">{t('records.qty')}</th>
                          <th className="px-3 py-2 font-medium">{t('records.usage')}</th>
                        </tr>
                      </thead>
                      <tbody className="divide-y divide-border">
                        {r.prescriptionItems.map((p, i) => (
                          <tr key={i}>
                            <td className="px-3 py-2 font-medium text-text">{p.medicineName}</td>
                            <td className="px-3 py-2 text-muted">{p.dosage}</td>
                            <td className="px-3 py-2 tabular-nums text-muted">{p.quantity}</td>
                            <td className="px-3 py-2 text-muted">{p.instruction}</td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                </div>
              )}
            </Card>
          ))}
        </div>
      )}
    </>
  )
}
