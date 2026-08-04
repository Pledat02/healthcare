// Cac control form dung chung base style
const inputBase =
  'w-full min-h-[46px] rounded-xl border border-border bg-surface px-3.5 py-2.5 text-sm text-text shadow-sm shadow-slate-950/[.02] outline-none placeholder:text-slate-400 focus:border-primary focus:ring-4 focus:ring-primary/10'

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
