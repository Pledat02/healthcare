import axios from 'axios'
import keycloak from '@/auth/keycloak'

// DEV: goi '/api' -> Vite proxy -> api-gateway (8090).
// PROD (Vercel): dat VITE_API_URL = URL public cua gateway, vd https://api.example.com/api
const api = axios.create({ baseURL: import.meta.env.VITE_API_URL || '/api' })

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

// Chuan hoa endpoint tra mang truc tiep va endpoint tra PageResponse ve cung mot kieu.
export function unwrapList(res) {
  const data = unwrap(res)
  if (Array.isArray(data)) return data
  return Array.isArray(data?.content) ? data.content : []
}

export function apiMessage(err) {
  return err?.response?.data?.message || err?.message || 'Đã có lỗi xảy ra'
}

// Lay nhieu ban ghi theo id trong 1 request (endpoint /batch) -> map {id: obj}.
// Thay cho viec goi GET /{resource}/{id} lap tung cai (fix N+1).
export async function fetchByIdsMap(resource, ids) {
  const unique = [...new Set(ids)].filter(Boolean)
  if (unique.length === 0) return {}
  // Chia chunk <= 100 id/request: tranh URL qua dai + khop cap 100 cua backend
  const CHUNK = 100
  const chunks = []
  for (let i = 0; i < unique.length; i += CHUNK) chunks.push(unique.slice(i, i + CHUNK))
  const results = await Promise.all(
    chunks.map((c) => api.get(`/${resource}/batch`, { params: { ids: c.join(',') } })),
  )
  const map = {}
  results.forEach((res) => (unwrap(res) || []).forEach((o) => (map[o.id] = o)))
  return map
}

export default api
