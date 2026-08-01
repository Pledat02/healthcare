import { Loader2 } from 'lucide-react'
import { useI18n } from '@/shared/i18n/I18nProvider'

export function Spinner({ label }) {
  const { t } = useI18n()
  return (
    <div className="flex items-center justify-center gap-2 py-12 text-muted">
      <Loader2 className="h-5 w-5 animate-spin" />
      <span className="text-sm">{label || t('common.loading')}</span>
    </div>
  )
}
