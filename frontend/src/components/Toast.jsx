import { createContext, useContext, useState, useCallback } from 'react'
import { CheckCircle2, XCircle, Info, X } from 'lucide-react'

const ToastContext = createContext(null)

const ICONS = {
  success: { icon: CheckCircle2, cls: 'text-[--color-success]' },
  error: { icon: XCircle, cls: 'text-[--color-danger]' },
  info: { icon: Info, cls: 'text-[--color-info]' },
}

export function ToastProvider({ children }) {
  const [toasts, setToasts] = useState([])

  const dismiss = useCallback((id) => {
    setToasts((t) => t.filter((x) => x.id !== id))
  }, [])

  const push = useCallback(
    (type, message) => {
      const id = Date.now() + Math.random()
      setToasts((t) => [...t, { id, type, message }])
      setTimeout(() => dismiss(id), 4000) // auto-dismiss 4s
    },
    [dismiss],
  )

  const toast = {
    success: (m) => push('success', m),
    error: (m) => push('error', m),
    info: (m) => push('info', m),
  }

  return (
    <ToastContext.Provider value={toast}>
      {children}
      {/* aria-live de screen reader doc; khong cuop focus */}
      <div
        className="fixed bottom-4 right-4 z-[1000] flex flex-col gap-2"
        aria-live="polite"
      >
        {toasts.map((t) => {
          const { icon: Icon, cls } = ICONS[t.type] || ICONS.info
          return (
            <div
              key={t.id}
              className="flex w-80 max-w-[90vw] items-start gap-3 rounded-lg border border-[--color-border] bg-white p-3.5 shadow-lg"
            >
              <Icon className={`mt-0.5 h-5 w-5 shrink-0 ${cls}`} />
              <p className="flex-1 text-sm text-[--color-text]">{t.message}</p>
              <button
                onClick={() => dismiss(t.id)}
                className="text-slate-400 hover:text-slate-600"
                aria-label="Đóng"
              >
                <X className="h-4 w-4" />
              </button>
            </div>
          )
        })}
      </div>
    </ToastContext.Provider>
  )
}

export function useToast() {
  const ctx = useContext(ToastContext)
  if (!ctx) throw new Error('useToast must be used within ToastProvider')
  return ctx
}
