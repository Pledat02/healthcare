export function EmptyState({ icon: Icon, title, subtitle, action }) {
  return (
    <div className="flex flex-col items-center justify-center gap-3 py-16 text-center">
      {Icon && (
        <div className="rounded-full bg-primary-soft p-3 text-primary">
          <Icon className="h-6 w-6" />
        </div>
      )}
      <div>
        <p className="font-semibold text-text">{title}</p>
        {subtitle && (
          <p className="mt-1 text-sm text-muted">{subtitle}</p>
        )}
      </div>
      {action}
    </div>
  )
}
