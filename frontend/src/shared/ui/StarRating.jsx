import { Star } from 'lucide-react'

// value: so sao (0..5). Neu truyen onChange -> tuong tac (chon sao). count: so luot (tuy chon).
export function StarRating({ value = 0, count, onChange, size = 'h-4 w-4', showNumber = false }) {
  const interactive = typeof onChange === 'function'
  return (
    <span className="inline-flex items-center gap-0.5">
      {[1, 2, 3, 4, 5].map((n) => (
        <button
          key={n}
          type="button"
          disabled={!interactive}
          onClick={interactive ? () => onChange(n) : undefined}
          className={interactive ? 'cursor-pointer transition hover:scale-110' : 'cursor-default'}
          aria-label={`${n} sao`}
        >
          <Star className={`${size} ${n <= Math.round(value) ? 'fill-amber-400 text-amber-400' : 'text-slate-300'}`} />
        </button>
      ))}
      {showNumber && value > 0 && <span className="ml-1 text-sm font-medium text-text">{value.toFixed(1)}</span>}
      {count != null && <span className="ml-1 text-xs text-muted">({count})</span>}
    </span>
  )
}
