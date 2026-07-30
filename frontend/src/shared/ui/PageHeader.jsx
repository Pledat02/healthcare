export function PageHeader({ title, subtitle, action }) {
  return (
    <div className="page-header mb-6 flex flex-wrap items-end justify-between gap-3">
      <div>
        <h1 className="text-2xl font-bold text-text">{title}</h1>
        {subtitle && (
          <p className="mt-1 text-sm text-muted">{subtitle}</p>
        )}
      </div>
      {action}
    </div>
  )
}
