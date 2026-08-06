import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import '@/index.css'
import keycloak from '@/auth/keycloak'
import { AuthProvider } from '@/auth/AuthContext'
import { ThemeProvider } from '@/shared/theme/ThemeProvider'
import { I18nProvider } from '@/shared/i18n/I18nProvider'
import ErrorBoundary from './ErrorBoundary.jsx'
import App from './App.jsx'

function render() {
  createRoot(document.getElementById('root')).render(
    <StrictMode>
      <ThemeProvider>
        <I18nProvider>
          <ErrorBoundary>
            <BrowserRouter>
              <AuthProvider>
                <App />
              </AuthProvider>
            </BrowserRouter>
          </ErrorBoundary>
        </I18nProvider>
      </ThemeProvider>
    </StrictMode>,
  )
}

// Khoi tao Keycloak truoc khi render (check-sso: khong ep dang nhap ngay).
// Neu Keycloak khong ket noi duoc (vd deploy UI-only, chua co Keycloak public),
// van render app: authenticated=false -> hien trang Login (nut login se can Keycloak).
keycloak
  .init({
    onLoad: 'check-sso',
    pkceMethod: 'S256',
    checkLoginIframe: false,
  })
  .then(render)
  .catch((err) => {
    console.error('Keycloak init failed — rendering app in logged-out state', err)
    render()
  })
