import { Component } from 'react'

/**
 * Chan loi render toan app. Khong co boundary -> 1 component throw la React unmount
 * ca cay -> trang trang. Boundary hien fallback than thien + nut tai lai.
 * (Class component vi React chua co hook cho getDerivedStateFromError.)
 */
export default class ErrorBoundary extends Component {
  state = { hasError: false }

  static getDerivedStateFromError() {
    return { hasError: true }
  }

  componentDidCatch(error, info) {
    // Log de dieu tra; thuc te co the gui ve service giam sat
    console.error('App render error:', error, info)
  }

  render() {
    if (!this.state.hasError) return this.props.children
    return (
      <div
        style={{
          minHeight: '100dvh', display: 'flex', alignItems: 'center', justifyContent: 'center',
          padding: '2rem', fontFamily: 'Inter, system-ui, sans-serif',
          background: 'var(--color-bg, #f8fafc)', color: 'var(--color-text, #0f172a)',
        }}
      >
        <div style={{ maxWidth: 420, textAlign: 'center' }}>
          <div style={{ fontSize: 44, marginBottom: 12 }}>⚠️</div>
          <h1 style={{ fontSize: 20, fontWeight: 700, margin: '0 0 8px' }}>
            Đã xảy ra lỗi · Something went wrong
          </h1>
          <p style={{ fontSize: 14, color: 'var(--color-muted, #64748b)', margin: '0 0 20px' }}>
            Giao diện gặp sự cố. Vui lòng tải lại trang.<br />
            The page hit an error. Please reload.
          </p>
          <button
            onClick={() => window.location.reload()}
            style={{
              minHeight: 44, padding: '0 20px', borderRadius: 10, border: 'none', cursor: 'pointer',
              background: 'var(--color-primary, #0d9488)', color: '#fff', fontSize: 14, fontWeight: 600,
            }}
          >
            Tải lại trang · Reload
          </button>
        </div>
      </div>
    )
  }
}
