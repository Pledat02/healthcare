import { NavLink, useLocation } from 'react-router-dom'
import { useAuth } from '@/auth/AuthContext'
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
} from 'lucide-react'

// Menu theo tung vai tro
const NAV = {
  PATIENT: [
    { to: '/doctors', label: 'Bác sĩ', icon: Stethoscope },
    { to: '/appointments', label: 'Lịch của tôi', icon: CalendarDays },
    { to: '/records', label: 'Hồ sơ khám', icon: FileText },
    { to: '/profile', label: 'Hồ sơ', icon: UserCircle },
  ],
  DOCTOR: [
    { to: '/schedule', label: 'Lịch khám', icon: CalendarCheck },
    { to: '/profile', label: 'Hồ sơ', icon: UserCircle },
  ],
  ADMIN: [
    { to: '/admin/analytics', label: 'Thống kê', icon: ChartNoAxesColumnIncreasing },
    { to: '/admin/doctors', label: 'Bác sĩ', icon: Users },
    { to: '/admin/patients', label: 'Bệnh nhân', icon: UserRoundCog },
    { to: '/admin/appointments', label: 'Lịch hẹn', icon: ClipboardList },
  ],
}

const ROLE_LABEL = { PATIENT: 'Bệnh nhân', DOCTOR: 'Bác sĩ', ADMIN: 'Quản trị' }

function NavItems({ items, onClick }) {
  return items.map(({ to, label, icon: Icon }) => (
    <NavLink
      key={to}
      to={to}
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
      <span>{label}</span>
    </NavLink>
  ))
}

export default function AppShell({ children }) {
  const { name, role, logout } = useAuth()
  const location = useLocation()
  const items = NAV[role] || []

  return (
    <div className="min-h-dvh bg-bg">
      {/* Sidebar - desktop */}
      <aside className="app-sidebar fixed inset-y-0 left-0 hidden w-64 flex-col border-r border-border bg-white lg:flex">
        <div className="flex items-center gap-2 px-6 py-5">
          <div className="rounded-lg bg-primary p-1.5 text-white">
            <Stethoscope className="h-5 w-5" />
          </div>
          <span className="text-lg font-bold text-text">MediBook</span>
        </div>
        <nav className="flex flex-1 flex-col gap-1 px-3">
          <NavItems items={items} />
        </nav>
        <div className="border-t border-border p-3">
          <div className="mb-2 px-3">
            <p className="truncate text-sm font-medium text-text">{name}</p>
            <p className="text-xs text-muted">{ROLE_LABEL[role]}</p>
          </div>
          <button
            onClick={() => logout()}
            className="flex w-full items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium text-muted hover:bg-slate-100 hover:text-danger"
          >
            <LogOut className="h-5 w-5" />
            Đăng xuất
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
        <button
          onClick={() => logout()}
          className="text-muted hover:text-danger"
          aria-label="Đăng xuất"
        >
          <LogOut className="h-5 w-5" />
        </button>
      </header>

      {/* Main content */}
      <main className="px-4 py-6 pb-24 lg:ml-64 lg:px-8 lg:pb-8">
        <div key={location.pathname} className="page-transition mx-auto max-w-6xl">{children}</div>
      </main>

      {/* Bottom nav - mobile (<=5 items) */}
      <nav className="fixed inset-x-0 bottom-0 z-40 flex border-t border-border bg-white lg:hidden">
        {items.slice(0, 5).map(({ to, label, icon: Icon }) => (
          <NavLink
            key={to}
            to={to}
            className={({ isActive }) =>
              `flex flex-1 flex-col items-center gap-0.5 py-2 text-[11px] font-medium ${
                isActive ? 'text-primary' : 'text-muted'
              }`
            }
          >
            <Icon className="h-5 w-5" />
            {label}
          </NavLink>
        ))}
      </nav>
    </div>
  )
}
