import { Loader2 } from 'lucide-react'
import { useI18n } from '@/shared/i18n/I18nProvider'

export function Spinner({ label }) {
  const { t } = useI18n()
  return (
    <div className="flex min-h-48 items-center justify-center gap-3 rounded-[1.75rem] border border-border bg-surface py-12 text-muted" role="status">
      <span className="flex h-10 w-10 items-center justify-center rounded-xl bg-primary-soft text-primary"><Loader2 className="h-5 w-5 animate-spin" aria-hidden="true" /></span>
      <span className="text-sm">{label || t('common.loading')}</span>
    </div>
  )
}
