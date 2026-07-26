import axios from 'axios'
import keycloak from '../keycloak'

// Goi API qua proxy '/api' -> api-gateway (8090). Gateway verify JWT roi route.
const api = axios.create({ baseURL: '/api' })

// Gan Bearer token vao moi request; refresh neu sap het han
api.interceptors.request.use(async (config) => {
  if (keycloak.authenticated) {
    try {
      await keycloak.updateToken(10) // refresh neu con < 10s
    } catch {
      keycloak.login()
    }
    config.headers.Authorization = `Bearer ${keycloak.token}`
  }
  return config
})

// Bóc lớp vỏ ApiResponse: { code, message, data } -> tra thang data
api.interceptors.response.use(
  (res) => res,
  (err) => Promise.reject(err),
)

// Helper lay .data.data (data ben trong ApiResponse)
export function unwrap(res) {
  return res?.data?.data
}

export function apiMessage(err) {
  return err?.response?.data?.message || err?.message || 'Đã có lỗi xảy ra'
}

export default api
