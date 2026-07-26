import { useAuth } from '../auth/AuthContext'
import { Button } from '../components/ui'
import { Stethoscope, CalendarCheck, ShieldCheck, Clock } from 'lucide-react'

const FEATURES = [
  { icon: CalendarCheck, title: 'Đặt lịch online', desc: 'Chọn bác sĩ và khung giờ chỉ trong 2 phút' },
  { icon: Clock, title: 'Nhắc lịch tự động', desc: 'Email xác nhận và nhắc trước 24 giờ' },
  { icon: ShieldCheck, title: 'Hồ sơ an toàn', desc: 'Dữ liệu khám được bảo mật, chỉ bạn xem được' },
]

export default function LoginPage() {
  const { login, register } = useAuth()

  return (
    <div className="min-h-dvh lg:grid lg:grid-cols-2">
      {/* Left: brand / features */}
      <div className="hidden flex-col justify-between bg-[--color-primary] p-12 text-white lg:flex">
        <div className="flex items-center gap-2">
          <div className="rounded-lg bg-white/20 p-2">
            <Stethoscope className="h-6 w-6" />
          </div>
          <span className="text-xl font-bold">MediBook</span>
        </div>
        <div>
          <h1 className="text-3xl font-bold leading-tight">
            Đặt lịch khám bệnh,
            <br />đơn giản và nhanh chóng
          </h1>
          <p className="mt-3 max-w-md text-white/80">
            Hệ thống đặt lịch khám trực tuyến — kết nối bệnh nhân với bác sĩ, quản lý
            lịch hẹn và hồ sơ khám tập trung một chỗ.
          </p>
          <div className="mt-8 space-y-4">
            {FEATURES.map(({ icon: Icon, title, desc }) => (
              <div key={title} className="flex items-start gap-3">
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
        <p className="text-sm text-white/60">© 2026 MediBook. Healthcare Booking System.</p>
      </div>

      {/* Right: actions */}
      <div className="flex min-h-dvh flex-col items-center justify-center p-6 lg:min-h-0">
        <div className="w-full max-w-sm">
          <div className="mb-8 flex items-center gap-2 lg:hidden">
            <div className="rounded-lg bg-[--color-primary] p-2 text-white">
              <Stethoscope className="h-6 w-6" />
            </div>
            <span className="text-xl font-bold text-[--color-text]">MediBook</span>
          </div>

          <h2 className="text-2xl font-bold text-[--color-text]">Chào mừng trở lại</h2>
          <p className="mt-1 text-sm text-[--color-muted]">
            Đăng nhập để đặt lịch và quản lý hồ sơ khám của bạn.
          </p>

          <div className="mt-8 space-y-3">
            <Button className="w-full" onClick={() => login()}>
              Đăng nhập
            </Button>
            <Button variant="secondary" className="w-full" onClick={() => register()}>
              Đăng ký tài khoản bệnh nhân
            </Button>
          </div>

          <p className="mt-6 text-center text-xs text-[--color-muted]">
            Bằng việc tiếp tục, bạn đồng ý với điều khoản sử dụng của MediBook.
          </p>
        </div>
      </div>
    </div>
  )
}
