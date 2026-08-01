import { createContext, useCallback, useContext, useRef, useState } from 'react'
import { AlertTriangle } from 'lucide-react'
import Modal from './Modal'
import { Button } from '@/shared/ui'
import { useI18n } from '@/shared/i18n/I18nProvider'

// Hop thoai xac nhan trong app, thay cho window.confirm() xau xi cua trinh duyet.
// Dung: const confirm = useConfirm(); if (!(await confirm({ ... }))) return
const ConfirmContext = createContext(null)

export function ConfirmProvider({ children }) {
  const { t } = useI18n()
  const [state, setState] = useState(null)
  const resolver = useRef(null)

  // Luu opts nguyen ban; text mac dinh giai quyet luc render bang t() de doi ngon ngu duoc
  const confirm = useCallback((opts = {}) => {
    return new Promise((resolve) => {
      resolver.current = resolve
      setState({ ...opts, danger: opts.danger ?? false })
    })
  }, [])

  // Dong hop thoai va tra ket qua ve cho ben goi (Promise)
  const settle = useCallback((result) => {
    setState(null)
    resolver.current?.(result)
    resolver.current = null
  }, [])

  return (
    <ConfirmContext.Provider value={confirm}>
      {children}
      <Modal open={!!state} onClose={() => settle(false)} title={state?.title || t('confirm.title')}>
        {state && (
          <div>
            <div className="flex items-start gap-3">
              <div
                className={`flex h-10 w-10 shrink-0 items-center justify-center rounded-full ${
                  state.danger ? 'bg-danger/10 text-danger' : 'bg-primary-soft text-primary'
                }`}
              >
                <AlertTriangle className="h-5 w-5" />
              </div>
              <p className="pt-1.5 text-sm text-text">{state.message || t('confirm.message')}</p>
            </div>
            <div className="mt-6 flex justify-end gap-2">
              <Button variant="secondary" type="button" onClick={() => settle(false)}>
                {state.cancelText || t('confirm.cancel')}
              </Button>
              <Button
                variant={state.danger ? 'danger' : 'primary'}
                type="button"
                onClick={() => settle(true)}
              >
                {state.confirmText || t('confirm.ok')}
              </Button>
            </div>
          </div>
        )}
      </Modal>
    </ConfirmContext.Provider>
  )
}

export function useConfirm() {
  const ctx = useContext(ConfirmContext)
  if (!ctx) throw new Error('useConfirm must be used within ConfirmProvider')
  return ctx
}
