import { Button } from '@/shared/ui'
import { ChevronLeft, ChevronRight } from 'lucide-react'
import { useI18n } from '@/shared/i18n/I18nProvider'

// Thanh phan trang Prev/Next dung chung cho cac trang list. An khi <= 1 trang.
export default function Paginator({ page, totalPages, onPage, info }) {
  const { t } = useI18n()
  if (totalPages <= 1) return null
  return (
    <div className="mt-4 flex items-center justify-center gap-3">
      <Button variant="secondary" disabled={page === 0} onClick={() => onPage(page - 1)}>
        <ChevronLeft className="h-4 w-4" /> {t('common.prev')}
      </Button>
      <span className="text-sm text-muted">{info}</span>
      <Button variant="secondary" disabled={page >= totalPages - 1} onClick={() => onPage(page + 1)}>
        {t('common.next')} <ChevronRight className="h-4 w-4" />
      </Button>
    </div>
  )
}
