import { Link } from 'react-router-dom'
import { ArrowRight, BadgeCheck, Check, MessageCircleHeart, Star } from 'lucide-react'

export function MarketingPage({ children, className = '' }) {
  return <div className={`patient-landing -mx-1 -mt-2 overflow-hidden rounded-[2rem] sm:-mx-2 lg:-mx-3 ${className}`}>{children}</div>
}

export function SectionHeading({ eyebrow, title, description, align = 'center', className = '', id }) {
  const centered = align === 'center'
  return (
    <div className={`${centered ? 'text-center' : 'text-left'} ${className}`}>
      <p className="patient-section-kicker">{eyebrow}</p>
      <h2 id={id} className={`patient-section-title ${centered ? 'mx-auto' : ''}`}>{title}</h2>
      {description && <p className={`mt-4 leading-7 text-slate-500 ${centered ? 'mx-auto max-w-2xl' : 'max-w-2xl'}`}>{description}</p>}
    </div>
  )
}

export function FeatureCards({ items, icons, links = [], columns = 'md:grid-cols-3' }) {
  return (
    <div className={`grid gap-5 ${columns}`}>
      {items.map((item, index) => {
        const Icon = icons[index]
        const to = links[index]
        const content = (
          <>
            <div className="absolute -right-8 -top-8 h-28 w-28 rounded-full bg-teal-100/60 blur-2xl transition-transform duration-500 group-hover:scale-150" aria-hidden="true" />
            <span className="relative flex h-13 w-13 items-center justify-center rounded-2xl bg-teal-50 text-teal-600 transition-transform duration-300 group-hover:rotate-3 group-hover:scale-110"><Icon className="h-6 w-6" aria-hidden="true" /></span>
            <h3 className="relative mt-7 text-xl font-extrabold text-slate-900">{item.title}</h3>
            <p className="relative mt-3 text-sm leading-7 text-slate-500">{item.desc}</p>
            {to && <span className="relative mt-6 inline-flex items-center gap-1 text-sm font-bold text-teal-700"><ArrowRight className="h-4 w-4" aria-hidden="true" /></span>}
          </>
        )
        const classes = 'patient-feature-card group relative overflow-hidden rounded-[1.75rem] border border-white bg-white p-7 shadow-sm'
        return to
          ? <Link key={item.title} to={to} className={classes} aria-label={item.title}>{content}</Link>
          : <article key={item.title} className={classes}>{content}</article>
      })}
    </div>
  )
}

export function TrustList({ items }) {
  return (
    <div className="flex flex-wrap gap-x-5 gap-y-3">
      {items.map((item) => <div key={item} className="flex items-center gap-2 text-xs font-semibold text-slate-600 sm:text-sm"><span className="flex h-5 w-5 items-center justify-center rounded-full bg-teal-100 text-teal-700"><Check className="h-3.5 w-3.5" aria-hidden="true" /></span>{item}</div>)}
    </div>
  )
}

export function StarRating({ label = '5 stars' }) {
  return <div className="flex gap-0.5 text-amber-400" aria-label={label}>{[0, 1, 2, 3, 4].map((n) => <Star key={n} className="h-4 w-4 fill-current" aria-hidden="true" />)}</div>
}

export function Testimonial({ quote, by, stars = true, icon = 'message' }) {
  const Icon = icon === 'badge' ? BadgeCheck : MessageCircleHeart
  return (
    <div className="patient-quote relative flex flex-col items-start gap-6 overflow-hidden rounded-[2rem] bg-slate-950 p-8 text-white sm:flex-row sm:items-center sm:p-10">
      <span className="flex h-14 w-14 shrink-0 items-center justify-center rounded-2xl bg-white/10 text-teal-300"><Icon className="h-7 w-7" aria-hidden="true" /></span>
      <div>{stars && <StarRating />}<blockquote className={`${stars ? 'mt-3' : ''} text-lg font-bold leading-8 sm:text-xl`}>{quote}</blockquote><p className="mt-3 text-sm text-slate-400">{by}</p></div>
    </div>
  )
}

export function MarketingCta({ eyebrow, title, description, label, to = '/doctors' }) {
  return (
    <section className="patient-cta relative overflow-hidden px-5 py-16 text-center text-white sm:px-10 lg:px-14 lg:py-20" aria-labelledby="marketing-cta-title">
      <div className="patient-cta-orb patient-cta-orb-one" aria-hidden="true" /><div className="patient-cta-orb patient-cta-orb-two" aria-hidden="true" />
      <div className="relative z-10 mx-auto max-w-2xl"><p className="text-xs font-extrabold uppercase tracking-[0.16em] text-teal-100">{eyebrow}</p><h2 id="marketing-cta-title" className="mt-4 text-3xl font-black tracking-tight sm:text-4xl">{title}</h2><p className="mt-4 text-teal-50/80">{description}</p><Link to={to} className="group mt-8 inline-flex min-h-14 items-center justify-center gap-2 rounded-2xl bg-white px-7 text-sm font-extrabold text-teal-700 shadow-xl transition hover:-translate-y-1 hover:shadow-2xl">{label}<ArrowRight className="h-4 w-4 transition-transform group-hover:translate-x-1" aria-hidden="true" /></Link></div>
    </section>
  )
}
