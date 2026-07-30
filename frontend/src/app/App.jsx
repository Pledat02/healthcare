import { useEffect } from 'react'
import { Routes, Route, Navigate } from 'react-router-dom'
import { useAuth } from '@/auth/AuthContext'
import { PatientProfileProvider, usePatientProfile } from '@/auth/PatientProfile'
import { ToastProvider, useToast } from '@/shared/components/Toast'
import { ConfirmProvider } from '@/shared/components/Confirm'
import { Spinner } from '@/shared/ui'
import AppShell from '@/shared/components/AppShell'
import LoginPage from '@/auth/LoginPage'

// Patient
import DoctorsPage from '@/features/doctors/pages/DoctorsPage'
import MyAppointmentsPage from '@/features/appointments/pages/MyAppointmentsPage'
import MyRecordsPage from '@/features/records/pages/MyRecordsPage'
import ProfilePage from '@/features/patients/pages/ProfilePage'
// Doctor
import SchedulePage from '@/features/appointments/pages/SchedulePage'
// Admin
import ManageDoctorsPage from '@/features/doctors/pages/ManageDoctorsPage'
import AllAppointmentsPage from '@/features/appointments/pages/AllAppointmentsPage'
import ManagePatientsPage from '@/features/patients/pages/ManagePatientsPage'
import AnalyticsPage from '@/features/dashboard/pages/AnalyticsPage'

// Trang mac dinh theo vai tro
function HomeRedirect() {
  const { role } = useAuth()
  if (role === 'ADMIN') return <Navigate to="/admin/analytics" replace />
  if (role === 'DOCTOR') return <Navigate to="/schedule" replace />
  if (role === 'PATIENT') return <Navigate to="/doctors" replace />
  return <Navigate to="/profile" replace />
}

// Chan theo vai tro CHINH (ADMIN > DOCTOR > PATIENT).
// Dung vai tro chinh thay vi hasRole: tai khoan co the mang kem role phu
// (vd PATIENT la default role), khong nen vi the ma vao duoc trang khac.
function RequireRole({ role, children }) {
  const { role: primary } = useAuth()
  if (primary !== role) return <Navigate to="/" replace />
  return children
}

// US-02b: chan cac chuc nang can ho so benh nhan (dat lich, lich cua toi,
// ho so kham). Chua co ho so -> nhac va day ve trang Ho so de hoan thien.
function RequirePatientProfile({ children }) {
  const { loading, hasProfile } = usePatientProfile()
  const toast = useToast()
  useEffect(() => {
    if (!loading && !hasProfile) {
      toast.info('Vui lòng hoàn thiện hồ sơ trước khi đặt lịch')
    }
  }, [loading, hasProfile]) // eslint-disable-line
  if (loading) return <Spinner />
  if (!hasProfile) return <Navigate to="/profile" replace />
  return children
}

export default function App() {
  const { authenticated } = useAuth()

  if (!authenticated) return <LoginPage />

  return (
    <ToastProvider>
      <ConfirmProvider>
       <PatientProfileProvider>
        <AppShell>
          <Routes>
            <Route path="/" element={<HomeRedirect />} />

            {/* Patient — can ho so benh nhan truoc khi dung (US-02b) */}
            <Route path="/doctors" element={<RequireRole role="PATIENT"><RequirePatientProfile><DoctorsPage /></RequirePatientProfile></RequireRole>} />
            <Route path="/appointments" element={<RequireRole role="PATIENT"><RequirePatientProfile><MyAppointmentsPage /></RequirePatientProfile></RequireRole>} />
            <Route path="/records" element={<RequireRole role="PATIENT"><RequirePatientProfile><MyRecordsPage /></RequirePatientProfile></RequireRole>} />
            {/* Trang Ho so KHONG bi chan — day la noi hoan thien ho so */}
            <Route path="/profile" element={<ProfilePage />} />

            {/* Doctor */}
            <Route path="/schedule" element={<RequireRole role="DOCTOR"><SchedulePage /></RequireRole>} />

            {/* Admin */}
            <Route path="/admin/analytics" element={<RequireRole role="ADMIN"><AnalyticsPage /></RequireRole>} />
            <Route path="/admin/doctors" element={<RequireRole role="ADMIN"><ManageDoctorsPage /></RequireRole>} />
            <Route path="/admin/patients" element={<RequireRole role="ADMIN"><ManagePatientsPage /></RequireRole>} />
            <Route path="/admin/appointments" element={<RequireRole role="ADMIN"><AllAppointmentsPage /></RequireRole>} />

            <Route path="*" element={<Navigate to="/" replace />} />
          </Routes>
        </AppShell>
       </PatientProfileProvider>
      </ConfirmProvider>
    </ToastProvider>
  )
}
