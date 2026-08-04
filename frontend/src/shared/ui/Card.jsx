export function Card({ className = '', children }) {
  return (
    <div
      className={`app-card motion-card rounded-2xl border border-border bg-surface shadow-sm ${className}`}
    >
      {children}
    </div>
  )
}
