import { useEffect, useMemo, useState } from 'react'
import api, { unwrap, fetchByIdsMap, apiMessage } from '@/shared/lib/api'
import { useToast } from '@/shared/components/Toast'
import { useI18n } from '@/shared/i18n/I18nProvider'
import { Button, Card, EmptyState, Field, Input, PageHeader, Spinner } from '@/shared/ui'
import {
  CalendarDays, ChartNoAxesColumnIncreasing, CircleCheckBig, RefreshCcw,
  Repeat2, Stethoscope, TrendingUp, UserRoundCheck, UsersRound, PieChart,
} from 'lucide-react'

function percent(value, total) {
  return total ? Math.round((value / total) * 100) : 0
}

function formatNumber(value) {
  return new Intl.NumberFormat('vi-VN').format(value || 0)
}

export default function AnalyticsPage() {
  const toast = useToast()
  const { t } = useI18n()
  const [statistics, setStatistics] = useState(null)
  const [doctors, setDoctors] = useState({})
  const [patients, setPatients] = useState({})
  const [loading, setLoading] = useState(true)
  const [from, setFrom] = useState('')
  const [to, setTo] = useState('')

  useEffect(() => {
    let active = true
    let timer
    async function load() {
      setLoading(true)
      try {
        const response = await api.get('/appointments/statistics', {
          params: { from: from || undefined, to: to || undefined, limit: 10 },
        })
        const data = unwrap(response)
        const [doctorMap, patientMap] = await Promise.all([
          fetchByIdsMap('doctors', (data?.topDoctors || []).map((item) => item.doctorId)),
          fetchByIdsMap('patients', (data?.frequentPatients || []).map((item) => item.patientId)),
        ])
        if (!active) return
        setStatistics(data)
        setDoctors(doctorMap)
        setPatients(patientMap)
      } catch (e) {
        if (active) toast.error(apiMessage(e))
      } finally {
        if (active) setLoading(false)
      }
    }
    timer = setTimeout(load, 200)
    return () => {
      active = false
      clearTimeout(timer)
    }
  }, [from, to]) // eslint-disable-line

  const analysis = useMemo(() => {
    const summary = statistics?.summary || {}
    const doctorRanking = (statistics?.topDoctors || []).map((item) => ({
      id: item.doctorId,
      name: doctors[item.doctorId]?.fullName || t('analytics.doctorNoProfile'),
      specialization: doctors[item.doctorId]?.specialization || t('analytics.specUnknown'),
      appointments: item.appointmentCount,
      completed: item.completedCount,
    }))
    const patientRanking = (statistics?.frequentPatients || []).map((item) => ({
      id: item.patientId,
      name: patients[item.patientId]?.fullName || t('analytics.patientNoProfile'),
      phone: patients[item.patientId]?.phone || '',
      visits: item.completedVisits,
      doctorCount: item.distinctDoctorCount,
      lastVisit: item.lastVisit,
    }))
    const monthlyTrend = (statistics?.monthlyTrend || []).map((item) => {
      const [year, month] = item.month.split('-')
      return {
        key: item.month,
        label: `T${Number(month)}/${year.slice(-2)}`,
        valid: item.validAppointments,
        completed: item.completedAppointments,
        cancelled: item.cancelledAppointments,
      }
    })

    return {
      totalAppointments: summary.totalAppointments || 0,
      validAppointments: summary.validAppointments || 0,
      completedAppointments: summary.completedAppointments || 0,
      doctorRanking,
      patientRanking,
      statusCounts: {
        PENDING: statistics?.statusDistribution?.PENDING || 0,
        CONFIRMED: statistics?.statusDistribution?.CONFIRMED || 0,
        COMPLETED: statistics?.statusDistribution?.COMPLETED || 0,
        CANCELLED: statistics?.statusDistribution?.CANCELLED || 0,
      },
      monthlyTrend,
      activeDoctors: summary.activeDoctors || 0,
      returningPatients: summary.returningPatients || 0,
      repeatRate: summary.repeatRate || 0,
      averageVisits: summary.averageVisits || 0,
      cancellationRate: summary.cancellationRate || 0,
      busiestDay: summary.busiestDay ? t(`weekday.${summary.busiestDay}`) : t('analytics.noData'),
    }
  }, [statistics, doctors, patients, t])

  if (loading) return <Spinner label={t('analytics.loading')} />

  const maxDoctor = analysis.doctorRanking[0]?.appointments || 1
  const maxPatient = analysis.patientRanking[0]?.visits || 1

  return (
    <>
      <PageHeader
        title={t('page.analyticsTitle')}
        subtitle={t('page.analyticsSubtitle')}
        action={(from || to) && <Button variant="secondary" onClick={() => { setFrom(''); setTo('') }}><RefreshCcw className="h-4 w-4" /> {t('analytics.clearRange')}</Button>}
      />

      <Card className="mb-6 p-4">
        <div className="flex flex-col gap-4 md:flex-row md:items-end md:justify-between">
          <div>
            <p className="font-semibold text-text">{t('analytics.rangeTitle')}</p>
            <p className="mt-1 text-sm text-muted">{t('analytics.rangeHint')}</p>
          </div>
          <div className="grid gap-3 sm:grid-cols-2">
            <Field label={t('analytics.from')}><Input type="date" value={from} max={to || undefined} onChange={(e) => setFrom(e.target.value)} /></Field>
            <Field label={t('analytics.to')}><Input type="date" value={to} min={from || undefined} onChange={(e) => setTo(e.target.value)} /></Field>
          </div>
        </div>
      </Card>

      <div className="mb-6 grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <MetricCard icon={CalendarDays} label={t('analytics.validAppts')} value={analysis.validAppointments} note={t('analytics.validApptsNote')} color="primary" />
        <MetricCard icon={CircleCheckBig} label={t('analytics.completedAppts')} value={analysis.completedAppointments} note={t('analytics.completedNote', { pct: percent(analysis.completedAppointments, analysis.validAppointments) })} color="success" />
        <MetricCard icon={Stethoscope} label={t('analytics.activeDoctors')} value={analysis.activeDoctors} note={t('analytics.activeDoctorsNote')} color="accent" />
        <MetricCard icon={Repeat2} label={t('analytics.returningPatients')} value={analysis.returningPatients} note={t('analytics.returningNote', { pct: analysis.repeatRate })} color="warning" />
      </div>

      <div className="mb-6 grid gap-4 lg:grid-cols-4">
        <InsightCard icon={TrendingUp} label={t('analytics.avgVisits')} value={t('analytics.avgVisitsVal', { n: analysis.averageVisits })} />
        <InsightCard icon={Repeat2} label={t('analytics.repeatRate')} value={`${analysis.repeatRate}%`} />
        <InsightCard icon={CalendarDays} label={t('analytics.cancelRate')} value={`${analysis.cancellationRate}%`} />
        <InsightCard icon={UserRoundCheck} label={t('analytics.busiestDay')} value={analysis.busiestDay} />
      </div>

      {analysis.totalAppointments === 0 ? (
        <EmptyState icon={ChartNoAxesColumnIncreasing} title={t('analytics.emptyTitle')} subtitle={t('analytics.emptySub')} />
      ) : (
        <div className="space-y-6">
          <div className="grid gap-6 xl:grid-cols-3">
            <div className="xl:col-span-2">
              <MonthlyBarChart data={analysis.monthlyTrend} />
            </div>
            <StatusDonutChart counts={analysis.statusCounts} />
          </div>
          <div className="grid gap-6 xl:grid-cols-2">
            <RankingCard
              title={t('analytics.topDoctorsTitle')}
              subtitle={t('analytics.topDoctorsSub')}
              icon={Stethoscope}
              emptyText={t('analytics.topDoctorsEmpty')}
              items={analysis.doctorRanking}
              max={maxDoctor}
              renderMain={(item) => item.name}
              renderSub={(item) => t('analytics.doctorSub', { spec: item.specialization, n: item.completed })}
              renderValue={(item) => t('analytics.doctorValue', { n: formatNumber(item.appointments) })}
              getValue={(item) => item.appointments}
            />
            <RankingCard
              title={t('analytics.topPatientsTitle')}
              subtitle={t('analytics.topPatientsSub')}
              icon={UsersRound}
              emptyText={t('analytics.topPatientsEmpty')}
              items={analysis.patientRanking}
              max={maxPatient}
              renderMain={(item) => item.name}
              renderSub={(item) => t('analytics.patientSub', { phone: item.phone || t('analytics.patientNoPhone'), n: item.doctorCount })}
              renderValue={(item) => t('analytics.patientValue', { n: formatNumber(item.visits) })}
              getValue={(item) => item.visits}
            />
          </div>
        </div>
      )}
    </>
  )
}

const METRIC_COLORS = {
  primary: 'bg-primary-soft text-primary',
  success: 'bg-green-50 text-success',
  accent: 'bg-blue-50 text-accent',
  warning: 'bg-amber-50 text-warning',
}

function MetricCard({ icon: Icon, label, value, note, color }) {
  return (
    <Card className="p-5">
      <div className="flex items-start justify-between gap-3">
        <div>
          <p className="text-sm text-muted">{label}</p>
          <p className="mt-2 text-3xl font-bold tabular-nums text-text">{formatNumber(value)}</p>
          <p className="mt-1 text-xs text-slate-400">{note}</p>
        </div>
        <div className={`rounded-xl p-2.5 ${METRIC_COLORS[color]}`}><Icon className="h-5 w-5" /></div>
      </div>
    </Card>
  )
}

function InsightCard({ icon: Icon, label, value }) {
  return (
    <div className="flex items-center gap-3 rounded-xl border border-border bg-white p-4">
      <div className="rounded-lg bg-slate-100 p-2 text-muted"><Icon className="h-4 w-4" /></div>
      <div className="min-w-0">
        <p className="truncate text-xs text-muted">{label}</p>
        <p className="truncate font-semibold text-text">{value}</p>
      </div>
    </div>
  )
}

function MonthlyBarChart({ data }) {
  const { t } = useI18n()
  const max = Math.max(...data.map((item) => item.valid), ...data.map((item) => item.cancelled), 1)
  return (
    <Card className="h-full overflow-hidden">
      <div className="flex flex-wrap items-start justify-between gap-3 border-b border-border p-5">
        <div className="flex items-start gap-3">
          <div className="rounded-lg bg-primary-soft p-2 text-primary"><ChartNoAxesColumnIncreasing className="h-5 w-5" /></div>
          <div>
            <h2 className="font-semibold text-text">{t('analytics.monthlyTitle')}</h2>
            <p className="mt-0.5 text-xs text-muted">{t('analytics.monthlySub')}</p>
          </div>
        </div>
        <div className="flex flex-wrap gap-3 text-xs text-muted">
          <ChartLegend color="bg-primary" label={t('analytics.legendValid')} />
          <ChartLegend color="bg-success" label={t('analytics.legendCompleted')} />
          <ChartLegend color="bg-slate-300" label={t('analytics.legendCancelled')} />
        </div>
      </div>
      <div className="overflow-x-auto px-4 pb-5 pt-6">
        <div className="flex min-w-[32rem] items-end gap-3" role="img" aria-label={t('analytics.chartAria')}>
          {data.map((item) => (
            <div key={item.key} className="flex min-w-14 flex-1 flex-col items-center">
              <div className="mb-2 flex h-48 w-full items-end justify-center gap-1 border-b border-slate-200 px-1">
                <ChartBar value={item.valid} max={max} color="bg-primary" label={t('analytics.barValid', { label: item.label, n: item.valid })} />
                <ChartBar value={item.completed} max={max} color="bg-success" label={t('analytics.barCompleted', { label: item.label, n: item.completed })} />
                <ChartBar value={item.cancelled} max={max} color="bg-slate-300" label={t('analytics.barCancelled', { label: item.label, n: item.cancelled })} />
              </div>
              <span className="text-xs font-medium text-muted">{item.label}</span>
            </div>
          ))}
        </div>
      </div>
    </Card>
  )
}

function ChartBar({ value, max, color, label }) {
  return (
    <div
      className={`w-full max-w-5 rounded-t-md ${color} transition-[height] duration-300`}
      style={{ height: value ? `${Math.max(5, percent(value, max))}%` : '2px' }}
      title={label}
      aria-label={label}
    />
  )
}

function ChartLegend({ color, label }) {
  return <span className="inline-flex items-center gap-1.5"><span className={`h-2.5 w-2.5 rounded-sm ${color}`} />{label}</span>
}

const STATUS_CHART = [
  { key: 'COMPLETED', color: '#16a34a', dot: 'bg-success' },
  { key: 'CONFIRMED', color: '#2563eb', dot: 'bg-accent' },
  { key: 'PENDING', color: '#d97706', dot: 'bg-warning' },
  { key: 'CANCELLED', color: '#cbd5e1', dot: 'bg-slate-300' },
]

function StatusDonutChart({ counts }) {
  const { t } = useI18n()
  const total = Object.values(counts).reduce((sum, value) => sum + value, 0)
  let cursor = 0
  const segments = STATUS_CHART.map((status) => {
    const start = cursor
    cursor += total ? (counts[status.key] / total) * 100 : 0
    return `${status.color} ${start}% ${cursor}%`
  })
  const background = total ? `conic-gradient(${segments.join(', ')})` : '#e2e8f0'

  return (
    <Card className="overflow-hidden">
      <div className="border-b border-border p-5">
        <div className="flex items-start gap-3">
          <div className="rounded-lg bg-blue-50 p-2 text-accent"><PieChart className="h-5 w-5" /></div>
          <div>
            <h2 className="font-semibold text-text">{t('analytics.statusTitle')}</h2>
            <p className="mt-0.5 text-xs text-muted">{t('analytics.statusSub')}</p>
          </div>
        </div>
      </div>
      <div className="flex flex-col items-center gap-6 p-6 sm:flex-row xl:flex-col">
        <div className="relative h-40 w-40 shrink-0 rounded-full" style={{ background }} role="img" aria-label={t('analytics.donutAria', { n: total })}>
          <div className="absolute inset-7 flex flex-col items-center justify-center rounded-full bg-white shadow-inner">
            <span className="text-2xl font-bold tabular-nums text-text">{formatNumber(total)}</span>
            <span className="text-xs text-muted">{t('analytics.apptsUnit')}</span>
          </div>
        </div>
        <div className="w-full space-y-3">
          {STATUS_CHART.map((status) => (
            <div key={status.key} className="flex items-center justify-between gap-3 text-sm">
              <span className="inline-flex items-center gap-2 text-muted"><span className={`h-2.5 w-2.5 rounded-full ${status.dot}`} />{t(`status.${status.key}`)}</span>
              <span className="font-semibold tabular-nums text-text">{counts[status.key]} <span className="font-normal text-slate-400">({percent(counts[status.key], total)}%)</span></span>
            </div>
          ))}
        </div>
      </div>
    </Card>
  )
}

function RankingCard({ title, subtitle, icon: Icon, emptyText, items, max, renderMain, renderSub, renderValue, getValue }) {
  return (
    <Card className="overflow-hidden">
      <div className="border-b border-border p-5">
        <div className="flex items-start gap-3">
          <div className="rounded-lg bg-primary-soft p-2 text-primary"><Icon className="h-5 w-5" /></div>
          <div>
            <h2 className="font-semibold text-text">{title}</h2>
            <p className="mt-0.5 text-xs text-muted">{subtitle}</p>
          </div>
        </div>
      </div>
      {items.length === 0 ? (
        <p className="p-8 text-center text-sm text-muted">{emptyText}</p>
      ) : (
        <div className="divide-y divide-border">
          {items.slice(0, 10).map((item, index) => (
            <div key={item.id} className="p-4">
              <div className="flex items-center gap-3">
                <span className={`flex h-7 w-7 shrink-0 items-center justify-center rounded-full text-xs font-bold ${index < 3 ? 'bg-primary text-white' : 'bg-slate-100 text-muted'}`}>
                  {index + 1}
                </span>
                <div className="min-w-0 flex-1">
                  <div className="flex items-baseline justify-between gap-3">
                    <p className="truncate font-medium text-text">{renderMain(item)}</p>
                    <p className="shrink-0 text-sm font-semibold tabular-nums text-primary">{renderValue(item)}</p>
                  </div>
                  <p className="mt-0.5 truncate text-xs text-muted">{renderSub(item)}</p>
                  <div className="mt-2 h-1.5 overflow-hidden rounded-full bg-slate-100">
                    <div className="h-full rounded-full bg-primary" style={{ width: `${Math.max(4, percent(getValue(item), max))}%` }} />
                  </div>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
    </Card>
  )
}
