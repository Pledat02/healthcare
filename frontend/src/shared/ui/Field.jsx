// Label + control + hint/error
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
