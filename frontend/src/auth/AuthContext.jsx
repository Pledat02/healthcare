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
  }
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used within AuthProvider')
  return ctx
}
