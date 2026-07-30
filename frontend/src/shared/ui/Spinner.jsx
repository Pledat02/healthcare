import { Loader2 } from 'lucide-react'

export function Spinner({ label = 'Đang tải…' }) {
  return (
    <div className="flex items-center justify-center gap-2 py-12 text-muted">
      <Loader2 className="h-5 w-5 animate-spin" />
      <span className="text-sm">{label}</span>
    </div>
  )
}
