// Cac control form dung chung base style
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
