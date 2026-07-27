import { Loader2 } from 'lucide-react'

/* ---------- Button ---------- */
const btnVariants = {
  primary:
    'bg-primary text-white hover:bg-primary-hover disabled:opacity-50',
  secondary:
    'bg-white text-text border border-border hover:bg-slate-50 disabled:opacity-50',
  danger: 'bg-danger text-white hover:bg-red-700 disabled:opacity-50',
  ghost: 'text-text hover:bg-slate-100 disabled:opacity-50',
}
export function Button({
  variant = 'primary',
  loading = false,
  disabled,
  className = '',
  children,
  ...props
}) {
  return (
    <button
      disabled={disabled || loading}
      className={`motion-button inline-flex min-h-[44px] cursor-pointer items-center justify-center gap-2 rounded-lg px-4 py-2.5 text-sm font-semibold disabled:cursor-not-allowed ${btnVariants[variant]} ${className}`}
      {...props}
    >
      {loading && <Loader2 className="h-4 w-4 animate-spin" />}
      {children}
    </button>
  )
}

/* ---------- Card ---------- */
export function Card({ className = '', children }) {
  return (
    <div
      className={`motion-card rounded-xl border border-border bg-surface shadow-sm ${className}`}
    >
      {children}
    </div>
  )
}

/* ---------- Field (label + input) ---------- */
export function Field({ label, error, required, hint, children }) {
  return (
    <label className="block">
      {label && (
        <span className="mb-1.5 block text-sm font-medium text-text">
          {label}
          {required && <span className="text-danger"> *</span>}
        </span>
      )}
      {children}
      {hint && !error && (
        <span className="mt-1 block text-xs text-muted">{hint}</span>
      )}
      {error && (
        <span className="mt-1 block text-xs text-danger" role="alert">
          {error}
        </span>
      )}
    </label>
  )
}

const inputBase =
  'w-full rounded-lg border border-border bg-white px-3.5 py-2.5 text-sm text-text placeholder:text-slate-400 focus:border-primary focus:ring-2 focus:ring-primary/20 outline-none min-h-[44px]'

export function Input({ className = '', ...props }) {
  return <input className={`${inputBase} ${className}`} {...props} />
}
export function Select({ className = '', children, ...props }) {
  return (
    <select className={`${inputBase} ${className}`} {...props}>
      {children}
    </select>
  )
}
export function Textarea({ className = '', ...props }) {
  return (
    <textarea
      className={`${inputBase} min-h-[96px] resize-y ${className}`}
      {...props}
    />
  )
}

/* ---------- Spinner / loading ---------- */
export function Spinner({ label = 'Đang tải…' }) {
  return (
    <div className="flex items-center justify-center gap-2 py-12 text-muted">
      <Loader2 className="h-5 w-5 animate-spin" />
      <span className="text-sm">{label}</span>
    </div>
  )
}

/* ---------- Empty state ---------- */
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

/* ---------- Page header ---------- */
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

/* ---------- Appointment status badge (mau + chu, khong chi mau) ---------- */
const STATUS = {
  PENDING: { label: 'Chờ xác nhận', cls: 'bg-amber-50 text-amber-700 ring-amber-200' },
  CONFIRMED: { label: 'Đã xác nhận', cls: 'bg-blue-50 text-blue-700 ring-blue-200' },
  COMPLETED: { label: 'Đã khám', cls: 'bg-green-50 text-green-700 ring-green-200' },
  CANCELLED: { label: 'Đã hủy', cls: 'bg-slate-100 text-slate-600 ring-slate-200' },
}
export function StatusBadge({ status }) {
  const s = STATUS[status] || { label: status, cls: 'bg-slate-100 text-slate-600 ring-slate-200' }
  return (
    <span
      className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium ring-1 ring-inset ${s.cls}`}
    >
      {s.label}
    </span>
  )
}
