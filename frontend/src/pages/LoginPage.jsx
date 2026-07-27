import { useAuth } from '../auth/AuthContext'
import { Button } from '../components/ui'
import { Stethoscope, CalendarCheck, ShieldCheck, Clock, ArrowRight } from 'lucide-react'

const FEATURES = [
  { icon: CalendarCheck, title: 'Đặt lịch online', desc: 'Chọn bác sĩ và khung giờ chỉ trong 2 phút' },
  { icon: Clock, title: 'Nhắc lịch tự động', desc: 'Email xác nhận và nhắc trước 24 giờ' },
  { icon: ShieldCheck, title: 'Hồ sơ an toàn', desc: 'Dữ liệu khám được bảo mật, chỉ bạn xem được' },
]

export default function LoginPage() {
  const { login, loginWithGoogle, register } = useAuth()

  return (
    <div className="login-page min-h-dvh overflow-hidden lg:grid lg:grid-cols-2">
      {/* Left: brand / features */}
      <div className="login-brand relative hidden flex-col justify-between overflow-hidden bg-primary p-12 text-white lg:flex">
        <div className="login-orb login-orb-one" />
        <div className="login-orb login-orb-two" />
        <div className="relative z-10 flex items-center gap-2 motion-fade-down">
          <div className="rounded-lg bg-white/20 p-2">
            <Stethoscope className="h-6 w-6" />
          </div>
          <span className="text-xl font-bold">MediBook</span>
        </div>
        <div className="relative z-10">
          <p className="mb-3 text-sm font-semibold uppercase tracking-[0.2em] text-white/70 motion-fade-up">
            Chăm sóc sức khỏe thông minh
          </p>
          <h1 className="text-4xl font-bold leading-tight motion-fade-up motion-delay-1">
            Đặt lịch khám bệnh,
            <br />đơn giản và nhanh chóng
          </h1>
          <p className="mt-4 max-w-md text-white/80 motion-fade-up motion-delay-2">
            Hệ thống đặt lịch khám trực tuyến — kết nối bệnh nhân với bác sĩ, quản lý
            lịch hẹn và hồ sơ khám tập trung một chỗ.
          </p>
          <div className="mt-9 space-y-4 stagger-list">
            {FEATURES.map(({ icon: Icon, title, desc }, index) => (
              <div key={title} className="feature-row flex items-start gap-3" style={{ '--item-index': index }}>
                <div className="mt-0.5 rounded-lg bg-white/15 p-2">
                  <Icon className="h-5 w-5" />
                </div>
                <div>
                  <p className="font-semibold">{title}</p>
                  <p className="text-sm text-white/70">{desc}</p>
                </div>
              </div>
            ))}
          </div>
        </div>
        <p className="relative z-10 text-sm text-white/60 motion-fade-up motion-delay-3">© 2026 MediBook. Healthcare Booking System.</p>
      </div>

      {/* Right: actions */}
      <div className="relative flex min-h-dvh flex-col items-center justify-center bg-white p-6 lg:min-h-0">
        <div className="w-full max-w-sm motion-auth-card">
          <div className="mb-8 flex items-center gap-2 lg:hidden">
            <div className="rounded-lg bg-primary p-2 text-white">
              <Stethoscope className="h-6 w-6" />
            </div>
            <span className="text-xl font-bold text-text">MediBook</span>
          </div>

          <span className="inline-flex rounded-full bg-primary-soft px-3 py-1 text-xs font-semibold text-primary">
            Cổng chăm sóc MediBook
          </span>
          <h2 className="mt-4 text-3xl font-bold tracking-tight text-text">Chào mừng trở lại</h2>
          <p className="mt-1 text-sm text-muted">
            Đăng nhập để đặt lịch và quản lý hồ sơ khám của bạn.
          </p>

          <div className="mt-8 space-y-3">
            <Button className="group w-full" onClick={() => loginWithGoogle()}>
              <GoogleIcon />
              Tiếp tục với Google
              <ArrowRight className="ml-auto h-4 w-4 transition-transform duration-300 group-hover:translate-x-1" />
            </Button>
            <div className="flex items-center gap-3 py-1 text-xs text-muted">
              <span className="h-px flex-1 bg-border" />
              hoặc
              <span className="h-px flex-1 bg-border" />
            </div>
            <Button variant="secondary" className="w-full" onClick={() => login()}>
              Đăng nhập bằng tài khoản
            </Button>
            <Button variant="ghost" className="w-full" onClick={() => register()}>
              Đăng ký tài khoản bệnh nhân
            </Button>
          </div>

          <p className="mt-6 text-center text-xs text-muted">
            Bằng việc tiếp tục, bạn đồng ý với điều khoản sử dụng của MediBook.
          </p>
        </div>
      </div>
    </div>
  )
}

function GoogleIcon() {
  return (
    <svg viewBox="0 0 24 24" className="h-5 w-5 rounded-full bg-white p-0.5" aria-hidden="true">
      <path fill="#4285F4" d="M21.6 12.2c0-.7-.1-1.4-.2-2H12v3.8h5.4a4.6 4.6 0 0 1-2 3v2.5h3.2c1.9-1.7 3-4.3 3-7.3Z" />
      <path fill="#34A853" d="M12 22c2.7 0 5-.9 6.6-2.4L15.4 17c-.9.6-2 1-3.4 1a5.8 5.8 0 0 1-5.5-4H3.2v2.6A10 10 0 0 0 12 22Z" />
      <path fill="#FBBC05" d="M6.5 14a6 6 0 0 1 0-3.9V7.5H3.2a10 10 0 0 0 0 9.1L6.5 14Z" />
      <path fill="#EA4335" d="M12 6c1.5 0 2.8.5 3.8 1.5l2.8-2.8A9.4 9.4 0 0 0 12 2a10 10 0 0 0-8.8 5.5l3.3 2.6A5.8 5.8 0 0 1 12 6Z" />
    </svg>
  )
}
