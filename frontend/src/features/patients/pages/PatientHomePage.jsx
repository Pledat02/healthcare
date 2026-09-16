import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '@/auth/AuthContext'
import { usePatientProfile } from '@/auth/PatientProfile'
import { useI18n } from '@/shared/i18n/I18nProvider'
import api, { fetchByIdsMap, unwrap } from '@/shared/lib/api'
import heroDoctor from '@/assets/patient-home-doctor.jpg'
import {
  FeatureCards, MarketingCta, MarketingPage, SectionHeading, StarRating,
  Testimonial, TrustList,
} from '@/features/marketing/components/MarketingSections'
import {
  ArrowRight, ArrowUpRight, CalendarCheck2, CalendarClock,
  ChevronRight, CircleUserRound, Clock3, FileHeart, FileClock,
  HeartPulse, ShieldCheck, Sparkles, Stethoscope,
  UserRoundSearch, Video,
} from 'lucide-react'

const copy = {
  vi: {
    overview: 'Tổng quan hôm nay', overviewSub: 'Mọi thông tin quan trọng cho hành trình chăm sóc của bạn.',
    nextAppointment: 'Lịch hẹn tiếp theo', viewSchedule: 'Xem toàn bộ lịch',
    noAppointment: 'Bạn chưa có lịch hẹn sắp tới', noAppointmentSub: 'Chọn bác sĩ phù hợp và đặt lịch chỉ trong vài phút.',
    bookFirst: 'Đặt lịch khám', profilePending: 'Hoàn thiện hồ sơ để bắt đầu',
    profilePendingSub: 'Chúng tôi cần một vài thông tin cơ bản trước khi bạn có thể đặt lịch.', completeProfile: 'Hoàn thiện hồ sơ',
    quickTitle: 'Truy cập nhanh', quickDoctors: 'Tìm bác sĩ', quickAppointments: 'Lịch của tôi', quickRecords: 'Hồ sơ khám',
    status: { PENDING: 'Chờ xác nhận', CONFIRMED: 'Đã xác nhận' }, doctorFallback: 'Bác sĩ MediBook',
    eyebrow: 'Chăm sóc sức khỏe theo cách nhẹ nhàng hơn', hello: 'Xin chào',
    titleA: 'Sức khỏe của bạn,', titleB: 'được chăm sóc đúng lúc.',
    intro: 'Tìm bác sĩ phù hợp, chọn giờ khám thuận tiện và quản lý hành trình sức khỏe của bạn — tất cả trong một nơi.',
    findDoctor: 'Tìm bác sĩ ngay', mySchedule: 'Xem lịch của tôi',
    trust: ['Bác sĩ được xác minh', 'Đặt lịch trong 2 phút', 'Dữ liệu luôn bảo mật'],
    live: 'Đang có bác sĩ trực tuyến', slots: 'Còn lịch trống hôm nay',
    reminder: 'Lịch hẹn đã được xác nhận', reminderTime: 'Ngày mai · 09:30',
    specialtyEyebrow: 'Chăm sóc toàn diện', specialtyTitle: 'Chuyên khoa bạn cần, ngay khi bạn cần',
    specialtySub: 'Tiếp cận đội ngũ bác sĩ giàu kinh nghiệm ở nhiều lĩnh vực.',
    specialties: ['Nội tổng quát', 'Tim mạch', 'Nhi khoa', 'Da liễu', 'Tai Mũi Họng', 'Thần kinh', 'Mắt', 'Dinh dưỡng'],
    valueEyebrow: 'Một hành trình liền mạch', valueTitle: 'Ít chờ đợi. Nhiều an tâm hơn.',
    features: [
      { title: 'Đặt lịch thông minh', desc: 'Chọn bác sĩ và khung giờ phù hợp với bạn, không cần gọi điện hay xếp hàng.' },
      { title: 'Hồ sơ luôn bên bạn', desc: 'Kết quả khám, chẩn đoán và đơn thuốc được lưu trữ gọn gàng, dễ dàng tra cứu.' },
      { title: 'Chăm sóc đáng tin cậy', desc: 'Thông tin bác sĩ minh bạch, đánh giá thực tế và dữ liệu cá nhân được bảo vệ.' },
    ],
    stepsEyebrow: 'Bắt đầu thật đơn giản', stepsTitle: 'Ba bước đến lịch hẹn của bạn',
    steps: [
      { title: 'Tìm bác sĩ', desc: 'Lọc theo chuyên khoa, tên và đánh giá.' },
      { title: 'Chọn thời gian', desc: 'Xem lịch trống và chọn khung giờ thuận tiện.' },
      { title: 'An tâm đi khám', desc: 'Nhận xác nhận, nhắc lịch và theo dõi hồ sơ.' },
    ],
    quote: '“MediBook giúp việc đi khám trở nên dễ dàng như đặt một cuộc hẹn cà phê.”',
    quoteBy: 'Minh Anh · Bệnh nhân MediBook', ctaEyebrow: 'Sức khỏe không nên phải chờ',
    ctaTitle: 'Sẵn sàng chăm sóc bản thân tốt hơn?', ctaSub: 'Bắt đầu với bác sĩ phù hợp chỉ trong vài phút.',
    cta: 'Đặt lịch khám hôm nay', reviews: '2.000+ đánh giá',
    heroAlt: 'Bác sĩ MediBook thân thiện sẵn sàng tư vấn', loadingAppointment: 'Đang tải lịch hẹn',
  },
  en: {
    overview: 'Today at a glance', overviewSub: 'Everything important for your care journey, in one place.',
    nextAppointment: 'Next appointment', viewSchedule: 'View all appointments',
    noAppointment: 'No upcoming appointments', noAppointmentSub: 'Find the right doctor and book in just a few minutes.',
    bookFirst: 'Book an appointment', profilePending: 'Complete your profile to begin',
    profilePendingSub: 'We need a few basic details before you can book an appointment.', completeProfile: 'Complete profile',
    quickTitle: 'Quick access', quickDoctors: 'Find a doctor', quickAppointments: 'My appointments', quickRecords: 'Medical records',
    status: { PENDING: 'Pending', CONFIRMED: 'Confirmed' }, doctorFallback: 'MediBook doctor',
    eyebrow: 'Healthcare, made gentler', hello: 'Hello', titleA: 'Your health, cared for',
    titleB: 'at the right moment.', intro: 'Find the right doctor, choose a convenient time and manage your care journey — all in one place.',
    findDoctor: 'Find a doctor', mySchedule: 'My appointments',
    trust: ['Verified doctors', 'Book in 2 minutes', 'Your data is protected'], live: 'Doctors online now',
    slots: 'Appointments today', reminder: 'Appointment confirmed', reminderTime: 'Tomorrow · 09:30',
    specialtyEyebrow: 'Complete care', specialtyTitle: 'The care you need, right when you need it',
    specialtySub: 'Connect with experienced doctors across a wide range of specialties.',
    specialties: ['General medicine', 'Cardiology', 'Pediatrics', 'Dermatology', 'ENT', 'Neurology', 'Ophthalmology', 'Nutrition'],
    valueEyebrow: 'One seamless journey', valueTitle: 'Less waiting. More peace of mind.',
    features: [
      { title: 'Smarter booking', desc: 'Choose the right doctor and time without phone calls or waiting in line.' },
      { title: 'Records that follow you', desc: 'Visits, diagnoses and prescriptions stay organized and easy to access.' },
      { title: 'Care you can trust', desc: 'Clear doctor profiles, real ratings and personal data that stays protected.' },
    ],
    stepsEyebrow: 'Getting started is easy', stepsTitle: 'Three steps to your appointment',
    steps: [
      { title: 'Find a doctor', desc: 'Filter by specialty, name and rating.' },
      { title: 'Pick a time', desc: 'See open slots and choose what works for you.' },
      { title: 'Visit with confidence', desc: 'Get confirmations, reminders and care records.' },
    ],
    quote: '“MediBook makes seeing a doctor feel as easy as scheduling a coffee.”',
    quoteBy: 'Minh Anh · MediBook patient', ctaEyebrow: 'Your health should not have to wait',
    ctaTitle: 'Ready to take better care of yourself?', ctaSub: 'Start with the right doctor in just a few minutes.',
    cta: 'Book an appointment today', reviews: '2,000+ reviews',
    heroAlt: 'A friendly MediBook doctor ready to help', loadingAppointment: 'Loading appointment',
  },
}

const featureIcons = [CalendarCheck2, FileHeart, ShieldCheck]
const stepIcons = [UserRoundSearch, Clock3, HeartPulse]

function formatAppointmentDate(iso, lang) {
  return new Intl.DateTimeFormat(lang === 'vi' ? 'vi-VN' : 'en-US', {
    timeZone: 'Asia/Ho_Chi_Minh',
    weekday: 'long', day: '2-digit', month: 'long',
    hour: '2-digit', minute: '2-digit',
  }).format(new Date(iso))
}

function PatientOverview({ c, lang }) {
  const { hasProfile, loading: profileLoading } = usePatientProfile()
  const [summary, setSummary] = useState({ loading: false, appointment: null, doctor: null })

  useEffect(() => {
    if (profileLoading) return
    if (!hasProfile) {
      setSummary({ loading: false, appointment: null, doctor: null })
      return
    }

    let active = true
    setSummary((current) => ({ ...current, loading: true }))
    async function loadUpcoming() {
      try {
        const response = await api.get('/appointments/patients/me')
        const appointment = (unwrap(response) || [])
          .filter((item) => !['COMPLETED', 'CANCELLED'].includes(item.status) && new Date(item.appointmentTime) >= new Date())
          .sort((a, b) => new Date(a.appointmentTime) - new Date(b.appointmentTime))[0] || null
        const doctors = appointment ? await fetchByIdsMap('doctors', [appointment.doctorId]) : {}
        if (active) setSummary({ loading: false, appointment, doctor: appointment ? doctors[appointment.doctorId] : null })
      } catch {
        if (active) setSummary({ loading: false, appointment: null, doctor: null })
      }
    }
    loadUpcoming()
    return () => { active = false }
  }, [hasProfile, profileLoading])

  const actions = [
    { to: '/doctors', label: c.quickDoctors, icon: Stethoscope },
    { to: '/appointments', label: c.quickAppointments, icon: CalendarClock },
    { to: '/records', label: c.quickRecords, icon: FileClock },
  ]
  const loading = profileLoading || summary.loading

  return (
    <section className="relative bg-white px-5 py-7 sm:px-9 lg:px-12 lg:py-9">
      <div className="mx-auto max-w-6xl">
        <div className="mb-5 flex flex-col justify-between gap-2 sm:flex-row sm:items-end">
          <div><p className="text-xl font-black tracking-tight text-slate-950 sm:text-2xl">{c.overview}</p><p className="mt-1 text-sm text-slate-500">{c.overviewSub}</p></div>
          <Link to="/appointments" className="hidden items-center gap-1 text-sm font-bold text-teal-700 hover:text-teal-900 sm:flex">{c.viewSchedule}<ArrowUpRight className="h-4 w-4" /></Link>
        </div>

        <div className="grid gap-4 lg:grid-cols-[1.35fr_.65fr]">
          <div className="relative min-h-52 overflow-hidden rounded-[1.75rem] bg-slate-950 p-6 text-white shadow-xl shadow-slate-950/10 sm:p-7" aria-live="polite" aria-busy={loading}>
            <div className="absolute -right-12 -top-16 h-52 w-52 rounded-full bg-teal-400/15 blur-2xl" />
            <div className="absolute -bottom-20 right-24 h-40 w-40 rounded-full bg-sky-400/10 blur-2xl" />
            {loading ? (
              <div className="relative animate-pulse" role="status" aria-label={c.loadingAppointment}>
                <div className="h-4 w-32 rounded bg-white/15" /><div className="mt-7 h-7 w-64 max-w-full rounded bg-white/15" /><div className="mt-3 h-4 w-44 rounded bg-white/10" /><div className="mt-7 h-10 w-32 rounded-xl bg-white/10" />
              </div>
            ) : !hasProfile ? (
              <div className="relative flex min-h-40 flex-col justify-between">
                <div><span className="flex h-11 w-11 items-center justify-center rounded-2xl bg-amber-300 text-slate-950"><CircleUserRound className="h-6 w-6" /></span><h2 className="mt-5 text-xl font-black sm:text-2xl">{c.profilePending}</h2><p className="mt-2 max-w-xl text-sm leading-6 text-slate-300">{c.profilePendingSub}</p></div>
                <Link to="/profile" className="mt-5 inline-flex w-fit items-center gap-2 rounded-xl bg-white px-4 py-2.5 text-sm font-extrabold text-slate-900 transition hover:-translate-y-0.5">{c.completeProfile}<ArrowRight className="h-4 w-4" /></Link>
              </div>
            ) : summary.appointment ? (
              <div className="relative flex min-h-40 flex-col justify-between">
                <div className="flex items-center justify-between gap-4"><p className="text-xs font-extrabold uppercase tracking-[0.14em] text-teal-300">{c.nextAppointment}</p><span className="rounded-full bg-white/10 px-3 py-1 text-[11px] font-bold text-teal-200">{c.status[summary.appointment.status] || summary.appointment.status}</span></div>
                <div className="mt-5 flex items-center gap-4"><span className="flex h-13 w-13 shrink-0 items-center justify-center rounded-2xl bg-teal-400 text-slate-950"><Stethoscope className="h-6 w-6" /></span><div className="min-w-0"><h2 className="truncate text-xl font-black sm:text-2xl">{summary.doctor?.fullName || c.doctorFallback}</h2><p className="mt-1 truncate text-sm font-semibold text-teal-200">{summary.doctor?.specialization}</p></div></div>
                <div className="mt-6 flex flex-col justify-between gap-3 border-t border-white/10 pt-4 sm:flex-row sm:items-center"><p className="flex items-center gap-2 text-sm font-semibold text-slate-200"><CalendarCheck2 className="h-4 w-4 text-teal-300" />{formatAppointmentDate(summary.appointment.appointmentTime, lang)}</p><Link to="/appointments" className="inline-flex items-center gap-1 text-sm font-bold text-white hover:text-teal-200">{c.viewSchedule}<ChevronRight className="h-4 w-4" /></Link></div>
              </div>
            ) : (
              <div className="relative flex min-h-40 flex-col justify-between">
                <div><span className="flex h-11 w-11 items-center justify-center rounded-2xl bg-white/10 text-teal-300"><CalendarCheck2 className="h-6 w-6" /></span><h2 className="mt-5 text-xl font-black sm:text-2xl">{c.noAppointment}</h2><p className="mt-2 text-sm text-slate-300">{c.noAppointmentSub}</p></div>
                <Link to="/doctors" className="mt-5 inline-flex w-fit items-center gap-2 rounded-xl bg-teal-400 px-4 py-2.5 text-sm font-extrabold text-slate-950 transition hover:-translate-y-0.5">{c.bookFirst}<ArrowRight className="h-4 w-4" /></Link>
              </div>
            )}
          </div>

          <div className="rounded-[1.75rem] border border-slate-100 bg-slate-50 p-5 sm:p-6">
            <p className="text-xs font-extrabold uppercase tracking-[0.14em] text-slate-500">{c.quickTitle}</p>
            <div className="mt-4 grid grid-cols-1 gap-2 sm:grid-cols-3 lg:grid-cols-1">
              {actions.map(({ to, label, icon: Icon }) => <Link key={to} to={to} className="group flex min-w-0 items-center gap-3 rounded-2xl border border-transparent bg-white p-3 text-left shadow-sm transition hover:-translate-y-0.5 hover:border-teal-100 hover:shadow-md sm:flex-col sm:gap-2 sm:text-center lg:flex-row lg:p-3.5 lg:text-left"><span className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-teal-50 text-teal-600"><Icon className="h-5 w-5" aria-hidden="true" /></span><span className="min-w-0 flex-1 text-sm font-extrabold text-slate-700 sm:text-xs lg:text-sm">{label}</span><ChevronRight className="ml-auto h-4 w-4 text-slate-300 transition group-hover:translate-x-0.5 group-hover:text-teal-500 sm:hidden lg:block" aria-hidden="true" /></Link>)}
            </div>
          </div>
        </div>
      </div>
    </section>
  )
}

export default function PatientHomePage() {
  const { name } = useAuth()
  const { lang } = useI18n()
  const c = copy[lang] || copy.vi
  const firstName = name?.trim().split(/\s+/).pop()

  return (
    <MarketingPage>
      <PatientOverview c={c} lang={lang} />
      <section className="patient-hero relative overflow-hidden px-5 pb-12 pt-8 sm:px-9 sm:pb-16 sm:pt-12 lg:px-12 lg:py-16">
        <div className="patient-grid pointer-events-none absolute inset-0" />
        <div className="patient-blob patient-blob-one" /><div className="patient-blob patient-blob-two" />
        <div className="relative z-10 grid items-center gap-12 lg:grid-cols-[1.02fr_.98fr]">
          <div className="max-w-2xl">
            <div className="patient-reveal patient-delay-1 inline-flex items-center gap-2 rounded-full border border-teal-200/80 bg-white/80 px-3.5 py-2 text-xs font-bold uppercase tracking-[0.12em] text-teal-700 shadow-sm backdrop-blur-md">
              <Sparkles className="h-4 w-4" />{c.eyebrow}
            </div>
            <p className="patient-reveal patient-delay-2 mt-7 text-sm font-semibold text-teal-700">{c.hello}{firstName ? `, ${firstName}` : ''} <span aria-hidden="true">👋</span></p>
            <h1 className="patient-reveal patient-delay-2 mt-2 text-[2.7rem] font-black leading-[1.04] tracking-[-0.045em] text-slate-950 sm:text-6xl lg:text-[4.1rem]">
              {c.titleA}<br /><span className="patient-gradient-text">{c.titleB}</span>
            </h1>
            <p className="patient-reveal patient-delay-3 mt-6 max-w-xl text-base leading-7 text-slate-600 sm:text-lg sm:leading-8">{c.intro}</p>
            <div className="patient-reveal patient-delay-4 mt-8 flex flex-col gap-3 sm:flex-row">
              <Link to="/doctors" className="patient-primary-cta group inline-flex min-h-14 items-center justify-center gap-2 rounded-2xl px-6 text-sm font-bold text-white shadow-lg shadow-teal-600/20">
                <Stethoscope className="h-5 w-5" />{c.findDoctor}<ArrowRight className="h-4 w-4 transition-transform duration-300 group-hover:translate-x-1" />
              </Link>
              <Link to="/appointments" className="inline-flex min-h-14 items-center justify-center gap-2 rounded-2xl border border-white/90 bg-white/80 px-6 text-sm font-bold text-slate-700 shadow-sm backdrop-blur-md transition hover:-translate-y-0.5 hover:border-teal-200 hover:text-teal-700 hover:shadow-lg">
                <CalendarCheck2 className="h-5 w-5 text-teal-600" />{c.mySchedule}
              </Link>
            </div>
            <div className="patient-reveal patient-delay-5 mt-9"><TrustList items={c.trust} /></div>
          </div>

          <div className="patient-visual patient-reveal patient-delay-3 relative mx-auto w-full max-w-[31rem] lg:ml-auto">
            <div className="patient-image-halo absolute -inset-5 rounded-[3rem]" />
            <div className="patient-image-frame relative aspect-[4/5] overflow-hidden rounded-[2.5rem] border-[7px] border-white/80 bg-white shadow-2xl shadow-teal-900/15">
              <img src={heroDoctor} alt={c.heroAlt} width="1024" height="1536" fetchPriority="high" decoding="async" className="h-full w-full object-cover object-center" />
              <div className="absolute inset-x-0 bottom-0 h-1/4 bg-gradient-to-t from-teal-950/20 to-transparent" />
            </div>
            <div className="patient-float-card patient-float-card-left absolute -left-5 top-[17%] flex items-center gap-3 rounded-2xl border border-white/90 bg-white/90 p-3 shadow-xl backdrop-blur-xl sm:-left-14">
              <span className="patient-pulse flex h-10 w-10 items-center justify-center rounded-xl bg-emerald-100 text-emerald-600"><Video className="h-5 w-5" /></span>
              <div><p className="text-xs font-bold text-slate-800">{c.live}</p><p className="mt-0.5 text-[11px] text-slate-500">12 {c.slots.toLowerCase()}</p></div>
            </div>
            <div className="patient-float-card patient-float-card-right absolute -right-3 bottom-[14%] flex items-center gap-3 rounded-2xl border border-white/90 bg-white/90 p-3 shadow-xl backdrop-blur-xl sm:-right-10">
              <span className="flex h-10 w-10 items-center justify-center rounded-xl bg-teal-100 text-teal-600"><CalendarCheck2 className="h-5 w-5" /></span>
              <div><p className="text-xs font-bold text-slate-800">{c.reminder}</p><p className="mt-0.5 text-[11px] text-slate-500">{c.reminderTime}</p></div>
            </div>
            <div className="absolute -bottom-4 left-5 flex items-center gap-3 rounded-2xl border border-white/90 bg-white/90 px-4 py-3 shadow-xl backdrop-blur-xl sm:left-10">
              <div className="flex -space-x-2">{['AN', 'TH', 'MD'].map((avatar, i) => <span key={avatar} className={`flex h-8 w-8 items-center justify-center rounded-full border-2 border-white ${['bg-sky-200', 'bg-amber-200', 'bg-rose-200'][i]} text-[10px] font-bold text-slate-700`}>{avatar}</span>)}</div>
              <div><StarRating /><p className="mt-0.5 text-[10px] font-semibold text-slate-500">4.9 · {c.reviews}</p></div>
            </div>
          </div>
        </div>
      </section>

      <section className="bg-white px-5 py-16 sm:px-9 lg:px-12 lg:py-20">
        <SectionHeading eyebrow={c.specialtyEyebrow} title={c.specialtyTitle} description={c.specialtySub} className="mx-auto max-w-5xl" />
        <div className="patient-marquee-mask mt-10 overflow-hidden"><div className="patient-marquee flex w-max gap-3">{[...c.specialties, ...c.specialties].map((item, i) => <div key={`${item}-${i}`} className="flex min-w-max items-center gap-2 rounded-full border border-teal-100 bg-teal-50/70 px-5 py-3 text-sm font-bold text-teal-800"><HeartPulse className="h-4 w-4 text-teal-500" />{item}</div>)}</div></div>
      </section>

      <section className="bg-slate-50 px-5 py-16 sm:px-9 lg:px-12 lg:py-24">
        <div className="mx-auto max-w-5xl">
          <SectionHeading eyebrow={c.valueEyebrow} title={c.valueTitle} align="left" className="max-w-xl" />
          <div className="mt-10"><FeatureCards items={c.features} icons={featureIcons} links={['/doctors', '/records', '/doctors']} columns="lg:grid-cols-3" /></div>
        </div>
      </section>

      <section className="bg-white px-5 py-16 sm:px-9 lg:px-12 lg:py-24">
        <div className="mx-auto max-w-5xl">
          <SectionHeading eyebrow={c.stepsEyebrow} title={c.stepsTitle} />
          <div className="relative mt-12 grid gap-8 md:grid-cols-3"><div className="absolute left-[16%] right-[16%] top-8 hidden border-t-2 border-dashed border-teal-200 md:block" />
            {c.steps.map((step, index) => { const Icon = stepIcons[index]; return <div key={step.title} className="relative text-center"><div className="relative mx-auto flex h-16 w-16 items-center justify-center rounded-2xl border-4 border-white bg-teal-600 text-white shadow-lg shadow-teal-600/20"><Icon className="h-7 w-7" /><span className="absolute -right-2 -top-2 flex h-6 w-6 items-center justify-center rounded-full bg-amber-400 text-[11px] font-black text-slate-900">{index + 1}</span></div><h3 className="mt-5 font-extrabold text-slate-900">{step.title}</h3><p className="mx-auto mt-2 max-w-[16rem] text-sm leading-6 text-slate-500">{step.desc}</p></div>})}
          </div>
          <div className="mt-16"><Testimonial quote={c.quote} by={c.quoteBy} /></div>
        </div>
      </section>

      <MarketingCta eyebrow={c.ctaEyebrow} title={c.ctaTitle} description={c.ctaSub} label={c.cta} />
    </MarketingPage>
  )
}
