export function EmptyState({ icon: Icon, title, subtitle, action }) {
  return (
    <div className="app-empty-state flex flex-col items-center justify-center gap-4 rounded-[1.75rem] border border-dashed border-border bg-surface px-6 py-16 text-center shadow-sm">
      {Icon && (
        <div className="rounded-2xl bg-primary-soft p-4 text-primary shadow-sm">
          <Icon className="h-7 w-7" aria-hidden="true" />
        </div>
      )}
      <div>
        <p className="text-lg font-extrabold text-text">{title}</p>
        {subtitle && (
          <p className="mx-auto mt-1 max-w-md text-sm leading-6 text-muted">{subtitle}</p>
        )}
      </div>
      {action}
    </div>
  )
}
