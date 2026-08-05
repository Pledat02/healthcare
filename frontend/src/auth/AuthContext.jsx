import { createContext, useContext } from 'react'
import keycloak, { authConfig, getRoles, hasRole, primaryRole } from '@/auth/keycloak'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const value = {
    keycloak,
    authenticated: keycloak.authenticated,
    username: keycloak.tokenParsed?.preferred_username,
    name: keycloak.tokenParsed?.name || keycloak.tokenParsed?.preferred_username,
    email: keycloak.tokenParsed?.email,
    keycloakId: keycloak.tokenParsed?.sub,
    roles: getRoles(),
    role: primaryRole(),
    hasRole,
    login: () => keycloak.login({ redirectUri: window.location.origin }),
    loginWithGoogle: () =>
      keycloak.login({
        idpHint: authConfig.googleIdpAlias,
        redirectUri: window.location.origin,
      }),
    logout: () => keycloak.logout({ redirectUri: window.location.origin }),
    register: () => keycloak.register(),
    // Doi mat khau: dung required-action UPDATE_PASSWORD cua Keycloak (chinh chu,
    // khong tu viet endpoint). Sau khi doi xong Keycloak quay lai dung trang hien tai.
    changePassword: () =>
      keycloak.login({ action: 'UPDATE_PASSWORD', redirectUri: window.location.href }),
    // Quen mat khau: dua toi trang dang nhap Keycloak, noi co link "Quen mat khau?"
    // (chi hien khi realm bat resetPasswordAllowed). Can SMTP de gui email reset.
    forgotPassword: () => keycloak.login({ redirectUri: window.location.origin }),
  }
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used within AuthProvider')
  return ctx
}
