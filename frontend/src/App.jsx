import { Routes, Route, Navigate } from 'react-router-dom'
import { useAuth } from './auth/AuthContext'
import { ToastProvider } from './components/Toast'
import AppShell from './components/AppShell'
import LoginPage from './pages/LoginPage'

// Patient
import DoctorsPage from './pages/patient/DoctorsPage'
import MyAppointmentsPage from './pages/patient/MyAppointmentsPage'
import MyRecordsPage from './pages/patient/MyRecordsPage'
import ProfilePage from './pages/patient/ProfilePage'
// Doctor
import SchedulePage from './pages/doctor/SchedulePage'
// Admin
import ManageDoctorsPage from './pages/admin/ManageDoctorsPage'
import AllAppointmentsPage from './pages/admin/AllAppointmentsPage'

// Trang mac dinh theo vai tro
function HomeRedirect() {
  const { role } = useAuth()
  if (role === 'ADMIN') return <Navigate to="/admin/doctors" replace />
  if (role === 'DOCTOR') return <Navigate to="/schedule" replace />
  if (role === 'PATIENT') return <Navigate to="/doctors" replace />
  return <Navigate to="/profile" replace />
}

// Chan theo vai tro
function RequireRole({ role, children }) {
  const auth = useAuth()
  if (!auth.hasRole(role)) return <Navigate to="/" replace />
  return children
}

export default function App() {
  const { authenticated } = useAuth()

  if (!authenticated) return <LoginPage />

  return (
    <ToastProvider>
      <AppShell>
        <Routes>
          <Route path="/" element={<HomeRedirect />} />

          {/* Patient */}
          <Route path="/doctors" element={<RequireRole role="PATIENT"><DoctorsPage /></RequireRole>} />
          <Route path="/appointments" element={<RequireRole role="PATIENT"><MyAppointmentsPage /></RequireRole>} />
          <Route path="/records" element={<RequireRole role="PATIENT"><MyRecordsPage /></RequireRole>} />
          <Route path="/profile" element={<ProfilePage />} />

          {/* Doctor */}
          <Route path="/schedule" element={<RequireRole role="DOCTOR"><SchedulePage /></RequireRole>} />

          {/* Admin */}
          <Route path="/admin/doctors" element={<RequireRole role="ADMIN"><ManageDoctorsPage /></RequireRole>} />
          <Route path="/admin/appointments" element={<RequireRole role="ADMIN"><AllAppointmentsPage /></RequireRole>} />

          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </AppShell>
    </ToastProvider>
  )
}
