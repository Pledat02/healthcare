import Keycloak from 'keycloak-js'

export const authConfig = {
  url: import.meta.env.VITE_KEYCLOAK_URL || 'http://localhost:8080',
  realm: import.meta.env.VITE_KEYCLOAK_REALM || 'healthcare',
  clientId: import.meta.env.VITE_KEYCLOAK_CLIENT_ID || 'healthcare-app',
  googleIdpAlias: import.meta.env.VITE_GOOGLE_IDP_ALIAS || 'google',
}

// Kết nối tới Keycloak; các giá trị có thể được thay bằng biến môi trường Vite.
const keycloak = new Keycloak({
  url: authConfig.url,
  realm: authConfig.realm,
  clientId: authConfig.clientId,
})

export default keycloak

// Lay danh sach role tu token (realm_access.roles)
export function getRoles() {
  return keycloak?.tokenParsed?.realm_access?.roles ?? []
}

export function hasRole(role) {
  return getRoles().includes(role)
}

// Vai tro chinh de dieu huong (uu tien ADMIN > DOCTOR > PATIENT)
export function primaryRole() {
  if (hasRole('ADMIN')) return 'ADMIN'
  if (hasRole('DOCTOR')) return 'DOCTOR'
  if (hasRole('PATIENT')) return 'PATIENT'
  return null
}
