import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import './index.css'
import keycloak from './keycloak'
import { AuthProvider } from './auth/AuthContext'
import App from './App.jsx'

// Khoi tao Keycloak truoc khi render (check-sso: khong ep dang nhap ngay)
keycloak
  .init({
    onLoad: 'check-sso',
    pkceMethod: 'S256',
    checkLoginIframe: false,
  })
  .then(() => {
    createRoot(document.getElementById('root')).render(
      <StrictMode>
        <BrowserRouter>
          <AuthProvider>
            <App />
          </AuthProvider>
        </BrowserRouter>
      </StrictMode>,
    )
  })
  .catch((err) => {
    console.error('Keycloak init failed', err)
    document.getElementById('root').innerHTML =
      '<div style="padding:2rem;font-family:sans-serif">Không kết nối được Keycloak (http://localhost:8080). Hãy chắc chắn Keycloak đang chạy.</div>'
  })
