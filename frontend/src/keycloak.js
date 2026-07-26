import Keycloak from 'keycloak-js'

// Ket noi toi Keycloak realm healthcare (client public: healthcare-app)
const keycloak = new Keycloak({
  url: 'http://localhost:8080',
  realm: 'healthcare',
  clientId: 'healthcare-app',
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
