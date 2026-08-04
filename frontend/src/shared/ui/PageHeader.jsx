import { Sparkles } from 'lucide-react'

export function PageHeader({ title, subtitle, action }) {
  return (
    <header className="app-page-hero page-header relative mb-6 overflow-hidden rounded-[1.75rem] border border-border px-5 py-6 shadow-sm sm:px-7 sm:py-7">
      <div className="app-page-orb pointer-events-none absolute -right-16 -top-20 h-52 w-52 rounded-full" aria-hidden="true" />
      <div className="relative flex flex-col justify-between gap-5 sm:flex-row sm:items-end">
        <div className="min-w-0 max-w-3xl">
          <p className="mb-3 flex items-center gap-2 text-[11px] font-extrabold uppercase tracking-[0.14em] text-primary"><Sparkles className="h-4 w-4" aria-hidden="true" />MediBook Care</p>
          <h1 className="text-2xl font-black tracking-tight text-text sm:text-3xl">{title}</h1>
        {subtitle && (
            <p className="mt-2 max-w-2xl text-sm leading-6 text-muted">{subtitle}</p>
        )}
        </div>
        {action && <div className="shrink-0">{action}</div>}
      </div>
    </header>
  )
}
