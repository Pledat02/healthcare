import { createContext, useCallback, useContext, useEffect, useState } from 'react'
import api, { unwrap } from '@/shared/lib/api'
import { useAuth } from './AuthContext'

// US-02b: dang nhap chi tao user Keycloak, chua co ho so benh nhan.
// Context nay nap /patients/me MOT lan, chia se cho ca app de:
//  - chan dat lich / lich cua toi / ho so kham khi chua co ho so (RequirePatientProfile)
//  - trang Ho so dung chung state, tao xong thi "mo cong" ngay khong phai load lai
const PatientProfileContext = createContext(null)

export function PatientProfileProvider({ children }) {
  const { role } = useAuth()
  const [profile, setProfile] = useState(null)
  // Chi benh nhan moi can ho so; vai tro khac coi nhu da xong (khong chan)
  const [loading, setLoading] = useState(role === 'PATIENT')

  const refresh = useCallback(async () => {
    if (role !== 'PATIENT') {
      setLoading(false)
      return
    }
    setLoading(true)
    try {
      const res = await api.get('/patients/me')
      setProfile(unwrap(res))
    } catch (e) {
      // 404 = chua co ho so (dang nhap lan dau) -> de profile = null, cong dong
      // Loi khac (mang/500...) cung coi nhu chua co, trang Ho so se hien de tao lai
      if (e?.response?.status !== 404) {
        console.error('Khong nap duoc /patients/me', e)
      }
      setProfile(null)
    } finally {
      setLoading(false)
    }
  }, [role])

  useEffect(() => {
    refresh()
  }, [refresh])

  const value = {
    profile,
    loading,
    hasProfile: !!profile,
    setProfile,
    refresh,
  }
  return <PatientProfileContext.Provider value={value}>{children}</PatientProfileContext.Provider>
}

export function usePatientProfile() {
  const ctx = useContext(PatientProfileContext)
  if (!ctx) throw new Error('usePatientProfile must be used within PatientProfileProvider')
  return ctx
}
