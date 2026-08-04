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
        <div className="space-y-5">
          {records.map((r) => (
            <Card key={r.id} className="overflow-hidden">
              <div className="flex flex-col gap-3 border-b border-border bg-slate-50 px-5 py-4 sm:flex-row sm:items-center sm:justify-between sm:px-6">
                <div className="flex items-center gap-3"><span className="flex h-10 w-10 items-center justify-center rounded-xl bg-primary-soft text-primary"><FileText className="h-5 w-5" aria-hidden="true" /></span><span className="text-sm font-bold text-text">{formatDateTime(r.createdAt)}</span></div>
                <Button
                  variant="ghost"
                  loading={downloading === r.id}
                  onClick={() => downloadPdf(r.id)}
                  title={t('records.downloadTip')}
                >
                  <Download className="h-4 w-4" aria-hidden="true" /> {t('records.downloadPdf')}
                </Button>
              </div>
              <div className="p-5 sm:p-6">
              <div className="rounded-2xl border border-teal-100 bg-teal-50/70 p-4">
                <p className="text-xs font-extrabold uppercase tracking-[0.1em] text-primary">{t('records.diagnosis')}</p>
                <p className="mt-2 text-lg font-extrabold text-text">{r.diagnosis || '—'}</p>
              </div>
              {r.notes && (
                <div className="mt-5">
                  <p className="text-xs font-extrabold uppercase tracking-[0.1em] text-muted">{t('records.notes')}</p>
                  <p className="mt-2 text-sm leading-6 text-text">{r.notes}</p>
                </div>
              )}
              {r.prescriptionItems?.length > 0 && (
                <div className="mt-4">
                  <p className="mb-3 flex items-center gap-2 text-xs font-extrabold uppercase tracking-[0.1em] text-muted">
                    <Pill className="h-4 w-4 text-primary" aria-hidden="true" /> {t('records.prescription')}
                  </p>
                  <div className="overflow-x-auto rounded-xl border border-border">
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
              </div>
            </Card>
          ))}
        </div>
      )}
    </>
  )
}
