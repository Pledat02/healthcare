import { useState } from 'react'
import api, { apiMessage } from '@/shared/lib/api'
import { usePaginatedList } from '@/shared/hooks/usePaginatedList'
import { formatDateTime } from '@/shared/lib/format'
import { useToast } from '@/shared/components/Toast'
import { useI18n } from '@/shared/i18n/I18nProvider'
import Modal from '@/shared/components/Modal'
import Paginator from '@/shared/components/Paginator'
import { Button, Card, Select, Field, Textarea, Spinner, EmptyState, PageHeader, StatusBadge } from '@/shared/ui'
import { ClipboardList, Eye, XCircle } from 'lucide-react'

const STATUS_VALUES = ['PENDING', 'CONFIRMED', 'COMPLETED', 'CANCELLED']

const PAGE_SIZE = 10

export default function AllAppointmentsPage() {
  const toast = useToast()
  const { t } = useI18n()
  const [filter, setFilter] = useState('')
  const [detail, setDetail] = useState(null)       // lich dang xem chi tiet
  const [cancelling, setCancelling] = useState(null) // lich dang huy
  // Ten benh nhan + bac si da duoc appointment-service lam giau san trong response
  const { items, page, setPage, totalPages, totalElements, loading, reload } = usePaginatedList(
    '/appointments', { params: { status: filter || undefined }, pageSize: PAGE_SIZE, deps: [filter] },
  )

  const canCancel = (a) => a.status !== 'COMPLETED' && a.status !== 'CANCELLED'

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
                    <th className="px-4 py-3 text-right font-medium">{t('allAppt.actions')}</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-border">
                  {items.map((a) => (
                    <tr key={a.id}>
                      <td className="whitespace-nowrap px-4 py-3 tabular-nums text-text">{formatDateTime(a.appointmentTime)}</td>
                      <td className="px-4 py-3 text-text">{a.patientName || '—'}</td>
                      <td className="px-4 py-3 text-muted">
                        {a.doctorName || '—'}
                        <span className="block text-xs text-slate-400">{a.specialization}</span>
                      </td>
                      <td className="max-w-[16rem] truncate px-4 py-3 text-muted">{a.reason || '—'}</td>
                      <td className="px-4 py-3"><StatusBadge status={a.status} /></td>
                      <td className="whitespace-nowrap px-4 py-3 text-right">
                        <div className="inline-flex gap-1">
                          <Button variant="ghost" onClick={() => setDetail(a)} title={t('allAppt.detail')}>
                            <Eye className="h-4 w-4" aria-hidden="true" /> {t('allAppt.detail')}
                          </Button>
                          {canCancel(a) && (
                            <Button variant="ghost" className="text-danger" onClick={() => setCancelling(a)} title={t('allAppt.cancel')}>
                              <XCircle className="h-4 w-4" aria-hidden="true" /> {t('allAppt.cancel')}
                            </Button>
                          )}
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </Card>

          <Paginator page={page} totalPages={totalPages} onPage={setPage}
            info={t('allAppt.pageInfo', { page: page + 1, total: totalPages, count: totalElements })} />
        </>
      )}

      {detail && <DetailModal appointment={detail} onClose={() => setDetail(null)} />}
      {cancelling && (
        <CancelModal
          appointment={cancelling}
          onClose={() => setCancelling(null)}
          onDone={() => { setCancelling(null); reload() }}
          toast={toast}
        />
      )}
    </>
  )
}

function Row({ label, value }) {
  return (
    <div className="flex justify-between gap-4 border-b border-border py-2 last:border-0">
      <span className="shrink-0 text-sm text-muted">{label}</span>
      <span className="text-right text-sm font-medium text-text">{value || '—'}</span>
    </div>
  )
}

function DetailModal({ appointment: a, onClose }) {
  const { t } = useI18n()
  return (
    <Modal open onClose={onClose} title={t('allAppt.detailTitle')}>
      <div className="space-y-0.5">
        <Row label={t('table.time')} value={formatDateTime(a.appointmentTime)} />
        <Row label={t('table.patient')} value={a.patientName} />
        <Row label={t('allAppt.patientPhone')} value={a.patientPhone} />
        <Row label={t('table.doctor')} value={a.doctorName} />
        <Row label={t('allAppt.specialization')} value={a.specialization} />
        <Row label={t('table.status')} value={<StatusBadge status={a.status} />} />
        <Row label={t('table.reason')} value={a.reason} />
        {a.status === 'CANCELLED' && <Row label={t('allAppt.cancelReason')} value={a.cancelReason} />}
      </div>
      <div className="flex justify-end pt-4">
        <Button variant="secondary" onClick={onClose}>{t('common.close')}</Button>
      </div>
    </Modal>
  )
}

function CancelModal({ appointment: a, onClose, onDone, toast }) {
  const { t } = useI18n()
  const [reason, setReason] = useState('')
  const [saving, setSaving] = useState(false)

  async function submit(e) {
    e.preventDefault()
    setSaving(true)
    try {
      await api.patch(`/appointments/${a.id}/cancel`, { reason: reason.trim() || undefined })
      toast.success(t('allAppt.cancelled'))
      onDone()
    } catch (err) {
      toast.error(apiMessage(err))
    } finally {
      setSaving(false)
    }
  }

  return (
    <Modal open onClose={onClose} title={t('allAppt.cancelTitle')}>
      <form onSubmit={submit} className="space-y-4">
        <p className="rounded-lg bg-amber-50 p-3 text-sm text-amber-700 ring-1 ring-amber-200">
          {t('allAppt.cancelWarn', { patient: a.patientName || '—', time: formatDateTime(a.appointmentTime) })}
        </p>
        <Field label={t('allAppt.cancelReason')}>
          <Textarea
            value={reason}
            onChange={(e) => setReason(e.target.value)}
            maxLength={500}
            placeholder={t('allAppt.cancelReasonPh')}
          />
        </Field>
        <div className="flex justify-end gap-2 pt-2">
          <Button variant="secondary" type="button" onClick={onClose}>{t('common.cancel')}</Button>
          <Button type="submit" className="bg-danger hover:bg-danger" loading={saving}>{t('allAppt.cancelSubmit')}</Button>
        </div>
      </form>
    </Modal>
  )
}
