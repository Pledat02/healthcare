export function Card({ className = '', children }) {
  return (
    <div
      className={`motion-card rounded-xl border border-border bg-surface shadow-sm ${className}`}
    >
      {children}
    </div>
  )
}
