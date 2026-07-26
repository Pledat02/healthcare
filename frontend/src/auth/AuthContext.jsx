import { createContext, useContext } from 'react'
import keycloak, { getRoles, hasRole, primaryRole } from '../keycloak'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const value = {
    keycloak,
    authenticated: keycloak.authenticated,
    username: keycloak.tokenParsed?.preferred_username,
    name: keycloak.tokenParsed?.name || keycloak.tokenParsed?.preferred_username,
    keycloakId: keycloak.tokenParsed?.sub,
    roles: getRoles(),
    role: primaryRole(),
    hasRole,
    login: () => keycloak.login(),
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
