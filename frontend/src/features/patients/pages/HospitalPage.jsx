import { Link } from 'react-router-dom'
import { useI18n } from '@/shared/i18n/I18nProvider'
import hospitalVisual from '@/assets/hospital-lobby.webp'
import facilityVisual from '@/assets/hospital-diagnostics.webp'
import {
  FeatureCards, MarketingCta, MarketingPage, SectionHeading, Testimonial,
} from '@/features/marketing/components/MarketingSections'
import {
  ArrowRight, Award, Building2, Check, Clock3, HeartHandshake,
  HeartPulse, Leaf, Microscope, ShieldCheck, Sparkles, Stethoscope,
} from 'lucide-react'

const content = {
  vi: {
    badge: 'Về Bệnh viện MediBook',
    titleA: 'Y khoa hiện đại.',
    titleB: 'Chăm sóc từ trái tim.',
    intro: 'MediBook xây dựng một môi trường chăm sóc sức khỏe nơi chuyên môn, công nghệ và sự thấu cảm cùng đồng hành trong mỗi hành trình điều trị.',
    findDoctor: 'Tìm bác sĩ phù hợp',
    explore: 'Khám phá bệnh viện',
    imageNote: 'Tận tâm trong từng cuộc gặp',
    imageSub: 'Đội ngũ y tế luôn lắng nghe và đồng hành cùng bạn',
    stats: [
      { value: '10+', label: 'Năm đồng hành' },
      { value: '50+', label: 'Bác sĩ chuyên môn cao' },
      { value: '15', label: 'Chuyên khoa' },
      { value: '24/7', label: 'Hỗ trợ người bệnh' },
    ],
    missionTag: 'Sứ mệnh của chúng tôi',
    missionTitle: 'Không chỉ điều trị bệnh — chúng tôi chăm sóc con người.',
    missionDesc: 'Mỗi quyết định tại MediBook đều bắt đầu từ nhu cầu thực sự của người bệnh. Chúng tôi kết nối đội ngũ chuyên gia với quy trình thuận tiện và công nghệ an toàn để việc chăm sóc sức khỏe trở nên gần gũi hơn.',
    promises: ['Tôn trọng và lắng nghe người bệnh', 'Minh bạch trong tư vấn và điều trị', 'Liên tục nâng cao chất lượng chuyên môn'],
    valuesTag: 'Điều làm nên MediBook',
    valuesTitle: 'Một tiêu chuẩn chăm sóc khác biệt',
    values: [
      { title: 'Chuyên môn đáng tin cậy', desc: 'Đội ngũ bác sĩ giàu kinh nghiệm, phối hợp đa chuyên khoa và liên tục cập nhật y học hiện đại.' },
      { title: 'Công nghệ vì con người', desc: 'Đặt lịch nhanh, hồ sơ số liền mạch và dữ liệu sức khỏe được bảo vệ ở mọi điểm chạm.' },
      { title: 'Trải nghiệm đầy thấu cảm', desc: 'Không gian thân thiện, quy trình rõ ràng và sự đồng hành tận tâm từ lần khám đầu tiên.' },
    ],
    spaceTag: 'Không gian chữa lành',
    spaceTitle: 'Được thiết kế để bạn cảm thấy an tâm',
    spaces: [
      { title: 'Chẩn đoán hiện đại', desc: 'Trang thiết bị hỗ trợ chẩn đoán chính xác và kịp thời.' },
      { title: 'Không gian xanh', desc: 'Ánh sáng tự nhiên và mảng xanh giúp mỗi lần thăm khám nhẹ nhàng hơn.' },
      { title: 'Quy trình an toàn', desc: 'Tiêu chuẩn kiểm soát chất lượng được áp dụng xuyên suốt.' },
    ],
    quote: '“Chúng tôi tin rằng trải nghiệm chăm sóc tốt bắt đầu từ cảm giác được lắng nghe.”',
    quoteBy: 'Đội ngũ MediBook Care',
    ctaTag: 'MediBook luôn sẵn sàng',
    ctaTitle: 'Hãy để chúng tôi đồng hành cùng sức khỏe của bạn.',
    ctaDesc: 'Chọn bác sĩ và thời gian phù hợp chỉ trong vài phút.',
    cta: 'Đặt lịch khám ngay',
    heroAlt: 'Không gian tiếp đón tại Bệnh viện MediBook',
    facilityAlt: 'Bác sĩ MediBook tư vấn trong khu chẩn đoán hiện đại',
    support: 'Hỗ trợ người bệnh',
  },
  en: {
    badge: 'About MediBook Hospital',
    titleA: 'Modern medicine.',
    titleB: 'Care from the heart.',
    intro: 'MediBook creates a healthcare environment where expertise, technology and empathy come together throughout every care journey.',
    findDoctor: 'Find the right doctor',
    explore: 'Explore our hospital',
    imageNote: 'Dedicated to every encounter',
    imageSub: 'Our care team listens and stays with you at every step',
    stats: [
      { value: '10+', label: 'Years of care' },
      { value: '50+', label: 'Expert doctors' },
      { value: '15', label: 'Specialties' },
      { value: '24/7', label: 'Patient support' },
    ],
    missionTag: 'Our mission',
    missionTitle: 'We do more than treat conditions — we care for people.',
    missionDesc: 'Every decision at MediBook starts with what patients truly need. We connect medical experts, convenient processes and secure technology to make healthcare feel more human.',
    promises: ['Respect and listen to every patient', 'Be transparent in advice and treatment', 'Continuously improve clinical quality'],
    valuesTag: 'What defines MediBook',
    valuesTitle: 'A different standard of care',
    values: [
      { title: 'Expertise you can trust', desc: 'Experienced doctors, multidisciplinary collaboration and continuously updated clinical knowledge.' },
      { title: 'Human-centered technology', desc: 'Fast booking, connected digital records and health data protected at every touchpoint.' },
      { title: 'An empathetic experience', desc: 'Welcoming spaces, clear processes and thoughtful support from the very first visit.' },
    ],
    spaceTag: 'A healing environment',
    spaceTitle: 'Designed to help you feel at ease',
    spaces: [
      { title: 'Modern diagnostics', desc: 'Technology that supports accurate and timely diagnosis.' },
      { title: 'Calming green spaces', desc: 'Natural light and greenery make every visit feel gentler.' },
      { title: 'Safe processes', desc: 'Quality and safety standards applied throughout your journey.' },
    ],
    quote: '“We believe great care begins with the feeling of being truly heard.”',
    quoteBy: 'MediBook Care team',
    ctaTag: 'MediBook is ready',
    ctaTitle: 'Let us be part of your healthier journey.',
    ctaDesc: 'Choose the right doctor and time in just a few minutes.',
    cta: 'Book an appointment',
    heroAlt: 'The welcoming lobby at MediBook Hospital',
    facilityAlt: 'A MediBook doctor consulting in a modern diagnostic suite',
    support: 'Patient support',
  },
}

const valueIcons = [Award, HeartPulse, HeartHandshake]
const spaceIcons = [Microscope, Leaf, ShieldCheck]

export default function HospitalPage() {
  const { lang } = useI18n()
  const c = content[lang] || content.vi

  return (
    <MarketingPage>
      <section className="marketing-hero relative overflow-hidden bg-gradient-to-br from-teal-50 via-white to-sky-50 px-5 py-10 sm:px-10 lg:px-14 lg:py-16">
        <div className="patient-grid pointer-events-none absolute inset-0" />
        <div className="absolute -right-24 -top-28 h-80 w-80 rounded-full bg-teal-200/30 blur-3xl" />
        <div className="relative z-10 grid items-center gap-12 lg:grid-cols-[1.05fr_.95fr]">
          <div>
            <span className="patient-reveal patient-delay-1 inline-flex items-center gap-2 rounded-full border border-teal-200 bg-white/80 px-4 py-2 text-xs font-extrabold uppercase tracking-[0.14em] text-teal-700 shadow-sm backdrop-blur">
              <Building2 className="h-4 w-4" />{c.badge}
            </span>
            <h1 className="patient-reveal patient-delay-2 mt-7 text-4xl font-black leading-[1.05] tracking-[-0.045em] text-slate-950 sm:text-6xl lg:text-7xl">
              {c.titleA}<br /><span className="patient-gradient-text">{c.titleB}</span>
            </h1>
            <p className="patient-reveal patient-delay-3 mt-6 max-w-2xl text-base leading-8 text-slate-600 sm:text-lg">{c.intro}</p>
            <div className="patient-reveal patient-delay-4 mt-8 flex flex-col gap-3 sm:flex-row">
              <Link to="/doctors" className="patient-primary-cta group inline-flex min-h-14 items-center justify-center gap-2 rounded-2xl px-6 text-sm font-extrabold text-white shadow-lg shadow-teal-600/20">
                <Stethoscope className="h-5 w-5" />{c.findDoctor}<ArrowRight className="h-4 w-4 transition-transform group-hover:translate-x-1" />
              </Link>
              <a href="#mission" className="inline-flex min-h-14 items-center justify-center gap-2 rounded-2xl border border-slate-200 bg-white px-6 text-sm font-extrabold text-slate-700 shadow-sm transition hover:-translate-y-0.5 hover:border-teal-200 hover:text-teal-700">
                <Sparkles className="h-5 w-5 text-teal-600" />{c.explore}
              </a>
            </div>
          </div>

          <div className="patient-reveal patient-delay-3 relative mx-auto w-full max-w-[30rem]">
            <div className="absolute -inset-4 rotate-3 rounded-[2.5rem] bg-gradient-to-br from-teal-200/70 to-sky-200/60" />
            <div className="relative aspect-[4/5] overflow-hidden rounded-[2.2rem] border-[6px] border-white shadow-2xl shadow-teal-900/15">
              <img src={hospitalVisual} alt={c.heroAlt} width="1024" height="1280" fetchPriority="high" decoding="async" className="h-full w-full object-cover" />
              <div className="absolute inset-x-0 bottom-0 bg-gradient-to-t from-slate-950/85 via-slate-950/45 to-transparent px-6 pb-6 pt-24 text-white">
                <p className="font-extrabold">{c.imageNote}</p><p className="mt-1 text-xs leading-5 text-white/75">{c.imageSub}</p>
              </div>
            </div>
            <div className="patient-float-card-left absolute -left-4 top-10 flex items-center gap-3 rounded-2xl border border-white bg-white/90 p-3 shadow-xl backdrop-blur sm:-left-10">
              <span className="patient-pulse flex h-10 w-10 items-center justify-center rounded-xl bg-emerald-100 text-emerald-600"><Clock3 className="h-5 w-5" /></span>
              <div><p className="text-sm font-black text-slate-900">24/7</p><p className="text-[11px] text-slate-500">{c.support}</p></div>
            </div>
          </div>
        </div>
      </section>

      <section className="border-y border-teal-100 bg-white px-5 py-8 sm:px-10 lg:px-14">
        <div className="mx-auto grid max-w-6xl grid-cols-2 gap-y-8 md:grid-cols-4">
          {c.stats.map((stat, index) => <div key={stat.label} className={`text-center ${index > 0 ? 'md:border-l md:border-teal-100' : ''}`}><p className="text-3xl font-black tracking-tight text-teal-700 sm:text-4xl">{stat.value}</p><p className="mt-1 text-xs font-semibold text-slate-500 sm:text-sm">{stat.label}</p></div>)}
        </div>
      </section>

      <section id="mission" className="bg-white px-5 py-16 sm:px-10 lg:px-14 lg:py-24">
        <div className="mx-auto grid max-w-6xl gap-10 lg:grid-cols-[.85fr_1.15fr] lg:gap-20">
          <SectionHeading eyebrow={c.missionTag} title={c.missionTitle} align="left" />
          <div className="lg:pt-8"><p className="text-base leading-8 text-slate-600">{c.missionDesc}</p><div className="mt-7 space-y-3">{c.promises.map((item) => <div key={item} className="flex items-center gap-3 text-sm font-bold text-slate-700"><span className="flex h-7 w-7 shrink-0 items-center justify-center rounded-full bg-teal-100 text-teal-700"><Check className="h-4 w-4" /></span>{item}</div>)}</div></div>
        </div>
      </section>

      <section className="bg-slate-50 px-5 py-16 sm:px-10 lg:px-14 lg:py-24">
        <div className="mx-auto max-w-6xl">
          <SectionHeading eyebrow={c.valuesTag} title={c.valuesTitle} />
          <div className="mt-12"><FeatureCards items={c.values} icons={valueIcons} /></div>
        </div>
      </section>

      <section className="bg-white px-5 py-16 sm:px-10 lg:px-14 lg:py-24">
        <div className="mx-auto max-w-6xl">
          <SectionHeading eyebrow={c.spaceTag} title={c.spaceTitle} align="left" className="max-w-2xl" />
          <div className="mt-10 grid items-stretch gap-6 lg:grid-cols-[1.15fr_.85fr]">
            <figure className="relative min-h-80 overflow-hidden rounded-[1.75rem] bg-slate-100 shadow-lg">
              <img src={facilityVisual} alt={c.facilityAlt} width="1280" height="853" loading="lazy" decoding="async" className="absolute inset-0 h-full w-full object-cover" />
              <div className="absolute inset-0 bg-gradient-to-t from-slate-950/35 to-transparent" aria-hidden="true" />
            </figure>
            <div className="grid gap-3">{c.spaces.map((space, index) => { const Icon = spaceIcons[index]; return <div key={space.title} className="flex gap-4 rounded-2xl border border-slate-100 bg-slate-50 p-5"><span className="flex h-11 w-11 shrink-0 items-center justify-center rounded-xl bg-white text-teal-600 shadow-sm"><Icon className="h-5 w-5" aria-hidden="true" /></span><div><h3 className="font-extrabold text-slate-900">{space.title}</h3><p className="mt-2 text-sm leading-6 text-slate-500">{space.desc}</p></div></div> })}</div>
          </div>
          <div className="mt-12"><Testimonial quote={c.quote} by={c.quoteBy} stars={false} icon="badge" /></div>
        </div>
      </section>

      <MarketingCta eyebrow={c.ctaTag} title={c.ctaTitle} description={c.ctaDesc} label={c.cta} />
    </MarketingPage>
  )
}
