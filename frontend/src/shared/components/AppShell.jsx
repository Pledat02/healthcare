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
  House,
  Building2,
  ChevronDown,
  MoreHorizontal,
} from 'lucide-react'

// Menu theo tung vai tro (label lay qua i18n key)
const NAV = {
  PATIENT: [
    { to: '/home', key: 'nav.home', icon: House, group: 'primary' },
    { to: '/doctors', key: 'nav.doctors', icon: Stethoscope, group: 'primary' },
    { to: '/appointments', key: 'nav.myAppointments', icon: CalendarDays, group: 'primary' },
    { to: '/records', key: 'nav.records', icon: FileText, group: 'primary' },
    { to: '/hospital', key: 'nav.hospital', icon: Building2, group: 'secondary' },
    { to: '/profile', key: 'nav.profile', icon: UserCircle, group: 'secondary' },
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
      <Icon className="h-5 w-5" aria-hidden="true" />
      <span>{t(key)}</span>
    </NavLink>
  ))
}

// Patient uses a horizontal navigation bar so the marketing-style home page
// can use the full screen width instead of being pushed by a left sidebar.
function PatientNavItems({ items }) {
  const { t } = useI18n()
  return items.map(({ to, key, icon: Icon }) => (
    <NavLink
      key={to}
      to={to}
      end
      className={({ isActive }) =>
        `flex min-w-0 items-center justify-center gap-2 whitespace-nowrap rounded-xl px-2 py-2.5 text-sm font-semibold transition-all ${
          isActive
            ? 'bg-primary-soft text-primary shadow-sm'
            : 'text-muted hover:bg-slate-100 hover:text-text'
        }`
      }
    >
      <Icon className="hidden h-4 w-4 xl:block" aria-hidden="true" />
      <span className="truncate">{t(key)}</span>
    </NavLink>
  ))
}

function SecondaryMenu({ items, onLogout }) {
  const { t } = useI18n()
  if (!items.length) return null
  return (
    <details className="group relative shrink-0">
      <summary aria-label={t('nav.more')} className="flex min-h-11 cursor-pointer list-none items-center gap-1.5 rounded-xl px-3 text-sm font-semibold text-muted transition-colors hover:bg-slate-100 hover:text-text [&::-webkit-details-marker]:hidden">
        <MoreHorizontal className="h-5 w-5" aria-hidden="true" />
        <span className="hidden lg:inline">{t('nav.more')}</span>
        <ChevronDown className="h-3.5 w-3.5 transition-transform group-open:rotate-180" aria-hidden="true" />
      </summary>
      <div className="absolute right-0 top-[calc(100%+.5rem)] z-60 w-56 rounded-2xl border border-border bg-white p-2 shadow-2xl shadow-slate-950/15">
        <p className="px-3 pb-2 pt-1 text-[11px] font-bold uppercase tracking-[0.12em] text-muted">{t('nav.secondary')}</p>
        {items.map(({ to, key, icon: Icon }) => (
          <NavLink
            key={to}
            to={to}
            end
            onClick={(event) => event.currentTarget.closest('details')?.removeAttribute('open')}
            className={({ isActive }) => `flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-semibold transition-colors ${isActive ? 'bg-primary-soft text-primary' : 'text-muted hover:bg-slate-100 hover:text-text'}`}
          >
            <Icon className="h-5 w-5" aria-hidden="true" /><span>{t(key)}</span>
          </NavLink>
        ))}
        {onLogout && <div className="mt-2 border-t border-border pt-2"><button onClick={() => onLogout()} className="flex w-full items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-semibold text-muted transition-colors hover:bg-slate-100 hover:text-danger"><LogOut className="h-5 w-5" aria-hidden="true" />{t('common.logout')}</button></div>}
      </div>
    </details>
  )
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
      {dark ? <Sun className="h-5 w-5" aria-hidden="true" /> : <Moon className="h-5 w-5" aria-hidden="true" />}
    </button>
  )
}

// Nut doi ngon ngu VI/EN
function LangToggle() {
  const { lang, toggleLang, t } = useI18n()
  return (
    <button
      onClick={toggleLang}
      className="flex h-9 items-center gap-1.5 rounded-lg px-2.5 text-sm font-semibold text-muted transition-colors hover:bg-slate-100 hover:text-text"
      title="Vietnamese / English"
      aria-label={t('common.language')}
    >
      <Languages className="h-5 w-5" aria-hidden="true" />
      {lang === 'vi' ? 'VI' : 'EN'}
    </button>
  )
}

export default function AppShell({ children }) {
  const { name, role, logout } = useAuth()
  const { t } = useI18n()
  const location = useLocation()
  const items = NAV[role] || []
  const patientLayout = role === 'PATIENT'
  const patientPrimary = patientLayout ? items.filter((item) => item.group === 'primary') : []
  const patientSecondary = patientLayout ? items.filter((item) => item.group === 'secondary') : []

  return (
    <div className="min-h-dvh bg-bg">
      <a href="#main-content" className="fixed left-3 top-3 z-[100] -translate-y-24 rounded-xl bg-primary px-4 py-2.5 text-sm font-bold text-white shadow-xl transition-transform focus:translate-y-0">
        {t('common.skipToContent')}
      </a>

      {/* Patient navigation - desktop */}
      {patientLayout && (
        <header className="sticky top-0 z-50 hidden border-b border-border bg-white/95 backdrop-blur-xl xl:block">
          <div className="mx-auto flex h-20 max-w-7xl items-center gap-3 px-8">
            <NavLink to="/home" className="flex w-36 shrink-0 items-center gap-2.5" aria-label="MediBook Home">
              <div className="rounded-xl bg-primary p-2 text-white shadow-sm shadow-primary/20">
                <Stethoscope className="h-5 w-5" aria-hidden="true" />
              </div>
              <span className="text-lg font-extrabold tracking-tight text-text">MediBook</span>
            </NavLink>

            <nav className="grid min-w-0 flex-1 grid-cols-4 gap-0.5" aria-label={t('nav.primary')}>
              <PatientNavItems items={patientPrimary} />
            </nav>
            <nav className="grid w-56 shrink-0 grid-cols-2 gap-0.5 border-l border-border pl-3" aria-label={t('nav.secondary')}>
              <PatientNavItems items={patientSecondary} />
            </nav>

            <div className="ml-auto flex shrink-0 items-center gap-0.5 border-l border-border pl-3">
              <ThemeToggle />
              <LangToggle />
              <button
                onClick={() => logout()}
                className="flex h-9 w-9 items-center justify-center rounded-lg text-muted transition-colors hover:bg-slate-100 hover:text-danger"
                title={t('common.logout')}
                aria-label={t('common.logout')}
              >
                <LogOut className="h-5 w-5" aria-hidden="true" />
              </button>
            </div>
          </div>
        </header>
      )}

      {/* Patient navigation - tablet */}
      {patientLayout && (
        <header className="sticky top-0 z-50 hidden border-b border-border bg-white/95 backdrop-blur-xl md:block xl:hidden">
          <div className="mx-auto flex h-18 items-center gap-2 px-5 lg:px-7">
            <NavLink to="/home" className="flex shrink-0 items-center gap-2" aria-label="MediBook Home">
              <div className="rounded-xl bg-primary p-2 text-white"><Stethoscope className="h-5 w-5" aria-hidden="true" /></div>
              <span className="hidden font-extrabold text-text lg:block">MediBook</span>
            </NavLink>
            <nav className="grid min-w-0 flex-1 grid-cols-4 gap-0.5" aria-label={t('nav.primary')}><PatientNavItems items={patientPrimary} /></nav>
            <div className="flex shrink-0 items-center gap-0.5 border-l border-border pl-2"><SecondaryMenu items={patientSecondary} onLogout={logout} /><ThemeToggle /><LangToggle /></div>
          </div>
        </header>
      )}

      {/* Doctor/Admin sidebar - desktop */}
      {!patientLayout && <aside className="app-sidebar fixed inset-y-0 left-0 hidden w-64 flex-col border-r border-border bg-white lg:flex">
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
      </aside>}

      {/* Patient topbar - mobile */}
      {patientLayout && <header className="sticky top-0 z-50 flex items-center justify-between border-b border-border bg-white/95 px-4 py-3 backdrop-blur-xl md:hidden">
        <div className="flex items-center gap-2">
          <div className="rounded-lg bg-primary p-1.5 text-white">
            <Stethoscope className="h-5 w-5" aria-hidden="true" />
          </div>
          <span className="font-bold text-text">MediBook</span>
        </div>
        <div className="flex items-center gap-0.5">
          <SecondaryMenu items={patientSecondary} onLogout={logout} />
          <ThemeToggle />
          <LangToggle />
        </div>
      </header>}

      {/* Doctor/Admin topbar - mobile/tablet */}
      {!patientLayout && <header className="sticky top-0 z-40 flex items-center justify-between border-b border-border bg-white px-4 py-3 lg:hidden">
        <div className="flex items-center gap-2"><div className="rounded-lg bg-primary p-1.5 text-white"><Stethoscope className="h-5 w-5" aria-hidden="true" /></div><span className="font-bold text-text">MediBook</span></div>
        <div className="flex items-center gap-0.5"><ThemeToggle /><LangToggle />
          <button
            onClick={() => logout()}
            className="flex h-9 w-9 items-center justify-center rounded-lg text-muted hover:text-danger"
            aria-label={t('common.logout')}
          >
            <LogOut className="h-5 w-5" aria-hidden="true" />
          </button>
        </div>
      </header>}

      {/* Main content */}
      <main id="main-content" tabIndex="-1" className={`px-4 py-6 lg:px-8 lg:pb-8 ${patientLayout ? 'pb-24 md:pb-8' : 'pb-24 lg:ml-64 lg:pb-8'}`}>
        <div key={location.pathname} className={`page-transition mx-auto ${patientLayout ? 'max-w-7xl' : 'max-w-6xl'}`}>{children}</div>
      </main>

      {/* Patient bottom nav - primary actions only */}
      {patientLayout && <nav className="fixed inset-x-0 bottom-0 z-40 flex border-t border-border bg-white/95 pb-[env(safe-area-inset-bottom)] backdrop-blur-xl md:hidden" aria-label={t('nav.primary')}>
        {patientPrimary.map(({ to, key, icon: Icon }) => (
          <NavLink
            key={to}
            to={to}
            end
            className={({ isActive }) =>
              `flex min-w-0 flex-1 flex-col items-center gap-0.5 px-0.5 py-2 text-[10px] font-medium ${
                isActive ? 'text-primary' : 'text-muted'
              }`
            }
          >
            <Icon className="h-5 w-5" aria-hidden="true" />
            <span className="max-w-full truncate">{t(key)}</span>
          </NavLink>
        ))}
      </nav>}

      {/* Doctor/Admin bottom nav */}
      {!patientLayout && <nav className="fixed inset-x-0 bottom-0 z-40 flex border-t border-border bg-white lg:hidden">
        {items.slice(0, 5).map(({ to, key, icon: Icon }) => <NavLink key={to} to={to} end className={({ isActive }) => `flex min-w-0 flex-1 flex-col items-center gap-0.5 px-0.5 py-2 text-[10px] font-medium ${isActive ? 'text-primary' : 'text-muted'}`}><Icon className="h-5 w-5" aria-hidden="true" /><span className="max-w-full truncate">{t(key)}</span></NavLink>)}
      </nav>}
    </div>
  )
}
