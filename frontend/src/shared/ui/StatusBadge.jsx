import { useI18n } from '@/shared/i18n/I18nProvider'

// Trang thai lich hen: mau + chu (khong chi dua vao mau). Nhan dich qua i18n (status.*)
const CLS = {
  PENDING: 'bg-amber-50 text-amber-700 ring-amber-200',
  CONFIRMED: 'bg-blue-50 text-blue-700 ring-blue-200',
  COMPLETED: 'bg-green-50 text-green-700 ring-green-200',
  CANCELLED: 'bg-slate-100 text-slate-600 ring-slate-200',
}

export function StatusBadge({ status }) {
  const { t } = useI18n()
  const cls = CLS[status] || 'bg-slate-100 text-slate-600 ring-slate-200'
  const label = CLS[status] ? t(`status.${status}`) : status
  return (
    <span
      data-status={status}
      className={`status-badge inline-flex items-center rounded-full px-3 py-1 text-xs font-bold ring-1 ring-inset ${cls}`}
    >
      {label}
    </span>
  )
}
