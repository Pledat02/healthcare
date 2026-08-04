import { Loader2 } from 'lucide-react'

const btnVariants = {
  primary:
    'app-button-primary bg-primary text-white hover:bg-primary-hover disabled:opacity-50',
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
      className={`motion-button inline-flex min-h-[44px] cursor-pointer items-center justify-center gap-2 rounded-xl px-4 py-2.5 text-sm font-bold disabled:cursor-not-allowed ${btnVariants[variant]} ${className}`}
      {...props}
    >
      {loading && <Loader2 className="h-4 w-4 animate-spin" aria-hidden="true" />}
      {children}
    </button>
  )
}
