import { NavLink, useLocation } from 'react-router-dom'
import { useAuth } from '@/auth/AuthContext'
import { useTheme } from '@/shared/theme/ThemeProvider'
import { useI18n } from '@/shared/i18n/I18nProvider'
import {
  Stethoscope,
  CalendarDays,
  Users,
  FileText,
  UserCircle,
  LogOut,
  ClipboardList,
  CalendarCheck,
  ChartNoAxesColumnIncreasing,
  UserRoundCog,
  Sun,
  Moon,
  Languages,
  History,
} from 'lucide-react'

// Menu theo tung vai tro (label lay qua i18n key)
const NAV = {
  PATIENT: [
    { to: '/doctors', key: 'nav.doctors', icon: Stethoscope },
    { to: '/appointments', key: 'nav.myAppointments', icon: CalendarDays },
    { to: '/records', key: 'nav.records', icon: FileText },
    { to: '/profile', key: 'nav.profile', icon: UserCircle },
  ],
  DOCTOR: [
    { to: '/schedule', key: 'nav.schedule', icon: CalendarCheck },
    { to: '/schedule/history', key: 'nav.history', icon: History },
    { to: '/profile', key: 'nav.profile', icon: UserCircle },
  ],
  ADMIN: [
    { to: '/admin/analytics', key: 'nav.analytics', icon: ChartNoAxesColumnIncreasing },
    { to: '/admin/doctors', key: 'nav.manageDoctors', icon: Users },
    { to: '/admin/patients', key: 'nav.managePatients', icon: UserRoundCog },
    { to: '/admin/appointments', key: 'nav.allAppointments', icon: ClipboardList },
  ],
}

function NavItems({ items, onClick }) {
  const { t } = useI18n()
  return items.map(({ to, key, icon: Icon }) => (
    <NavLink
      key={to}
      to={to}
      end
      onClick={onClick}
      className={({ isActive }) =>
        `flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium transition-colors ${
          isActive
            ? 'bg-primary-soft text-primary'
            : 'text-muted hover:bg-slate-100 hover:text-text'
        }`
      }
    >
      <Icon className="h-5 w-5" />
      <span>{t(key)}</span>
    </NavLink>
  ))
}

// Nut doi giao dien sang/toi
function ThemeToggle() {
  const { theme, toggleTheme } = useTheme()
  const { t } = useI18n()
  const dark = theme === 'dark'
  return (
    <button
      onClick={toggleTheme}
      className="flex h-9 w-9 items-center justify-center rounded-lg text-muted transition-colors hover:bg-slate-100 hover:text-text"
      title={dark ? t('common.lightMode') : t('common.darkMode')}
      aria-label={dark ? t('common.lightMode') : t('common.darkMode')}
    >
      {dark ? <Sun className="h-5 w-5" /> : <Moon className="h-5 w-5" />}
    </button>
  )
}

// Nut doi ngon ngu VI/EN
function LangToggle() {
  const { lang, toggleLang } = useI18n()
  return (
    <button
      onClick={toggleLang}
      className="flex h-9 items-center gap-1.5 rounded-lg px-2.5 text-sm font-semibold text-muted transition-colors hover:bg-slate-100 hover:text-text"
      title="Vietnamese / English"
      aria-label="Đổi ngôn ngữ"
    >
      <Languages className="h-5 w-5" />
      {lang === 'vi' ? 'VI' : 'EN'}
    </button>
  )
}

export default function AppShell({ children }) {
  const { name, role, logout } = useAuth()
  const { t } = useI18n()
  const location = useLocation()
  const items = NAV[role] || []

  return (
    <div className="min-h-dvh bg-bg">
      {/* Sidebar - desktop */}
      <aside className="app-sidebar fixed inset-y-0 left-0 hidden w-64 flex-col border-r border-border bg-white lg:flex">
        <div className="flex items-center justify-between px-6 py-5">
          <div className="flex items-center gap-2">
            <div className="rounded-lg bg-primary p-1.5 text-white">
              <Stethoscope className="h-5 w-5" />
            </div>
            <span className="text-lg font-bold text-text">MediBook</span>
          </div>
          <div className="flex items-center gap-0.5">
            <ThemeToggle />
            <LangToggle />
          </div>
        </div>
        <nav className="flex flex-1 flex-col gap-1 px-3">
          <NavItems items={items} />
        </nav>
        <div className="border-t border-border p-3">
          <div className="mb-2 px-3">
            <p className="truncate text-sm font-medium text-text">{name}</p>
            <p className="text-xs text-muted">{t(`role.${role}`)}</p>
          </div>
          <button
            onClick={() => logout()}
            className="flex w-full items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium text-muted hover:bg-slate-100 hover:text-danger"
          >
            <LogOut className="h-5 w-5" />
            {t('common.logout')}
          </button>
        </div>
      </aside>

      {/* Topbar - mobile */}
      <header className="sticky top-0 z-40 flex items-center justify-between border-b border-border bg-white px-4 py-3 lg:hidden">
        <div className="flex items-center gap-2">
          <div className="rounded-lg bg-primary p-1.5 text-white">
            <Stethoscope className="h-5 w-5" />
          </div>
          <span className="font-bold text-text">MediBook</span>
        </div>
        <div className="flex items-center gap-0.5">
          <ThemeToggle />
          <LangToggle />
          <button
            onClick={() => logout()}
            className="flex h-9 w-9 items-center justify-center rounded-lg text-muted hover:text-danger"
            aria-label={t('common.logout')}
          >
            <LogOut className="h-5 w-5" />
          </button>
        </div>
      </header>

      {/* Main content */}
      <main className="px-4 py-6 pb-24 lg:ml-64 lg:px-8 lg:pb-8">
        <div key={location.pathname} className="page-transition mx-auto max-w-6xl">{children}</div>
      </main>

      {/* Bottom nav - mobile (<=5 items) */}
      <nav className="fixed inset-x-0 bottom-0 z-40 flex border-t border-border bg-white lg:hidden">
        {items.slice(0, 5).map(({ to, key, icon: Icon }) => (
          <NavLink
            key={to}
            to={to}
            end
            className={({ isActive }) =>
              `flex flex-1 flex-col items-center gap-0.5 py-2 text-[11px] font-medium ${
                isActive ? 'text-primary' : 'text-muted'
              }`
            }
          >
            <Icon className="h-5 w-5" />
            {t(key)}
          </NavLink>
        ))}
      </nav>
    </div>
  )
}
