import { createContext, useContext, useState, useCallback, useMemo } from 'react'
import { dictionaries } from './dictionaries'

const I18nContext = createContext(null)
const STORAGE_KEY = 'medibook-lang'

function initialLang() {
  if (typeof window === 'undefined') return 'vi'
  const saved = localStorage.getItem(STORAGE_KEY)
  if (saved === 'vi' || saved === 'en') return saved
  // Mac dinh tieng Viet; neu trinh duyet uu tien tieng Anh thi dung en
  return navigator.language?.startsWith('en') ? 'en' : 'vi'
}

// Lay gia tri theo key dot-path, vd 'nav.doctors'
function lookup(dict, key) {
  return key.split('.').reduce((o, k) => (o == null ? undefined : o[k]), dict)
}

export function I18nProvider({ children }) {
  const [lang, setLangState] = useState(initialLang)

  const setLang = useCallback((l) => {
    const next = l === 'en' ? 'en' : 'vi'
    setLangState(next)
    localStorage.setItem(STORAGE_KEY, next)
  }, [])

  const toggleLang = useCallback(() => {
    setLangState((l) => {
      const next = l === 'vi' ? 'en' : 'vi'
      localStorage.setItem(STORAGE_KEY, next)
      return next
    })
  }, [])

  // t(key): tra chuoi theo ngon ngu hien tai; thieu key -> fallback tieng Viet -> chinh key
  const t = useCallback(
    (key) => lookup(dictionaries[lang], key) ?? lookup(dictionaries.vi, key) ?? key,
    [lang],
  )

  const value = useMemo(() => ({ lang, setLang, toggleLang, t }), [lang, setLang, toggleLang, t])
  return <I18nContext.Provider value={value}>{children}</I18nContext.Provider>
}

export function useI18n() {
  const ctx = useContext(I18nContext)
  if (!ctx) throw new Error('useI18n phải dùng trong <I18nProvider>')
  return ctx
}
