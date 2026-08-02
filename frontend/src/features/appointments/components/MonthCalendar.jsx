import { Card } from '@/shared/ui'
import { ChevronLeft, ChevronRight } from 'lucide-react'

// Mau cham theo trang thai lich hen
const DOT = {
  PENDING: 'bg-amber-400',
  CONFIRMED: 'bg-blue-500',
  COMPLETED: 'bg-green-500',
  CANCELLED: 'bg-slate-400',
}
const WD_VI = ['T2', 'T3', 'T4', 'T5', 'T6', 'T7', 'CN']
const WD_EN = ['Mo', 'Tu', 'We', 'Th', 'Fr', 'Sa', 'Su']

const p2 = (n) => String(n).padStart(2, '0')
const ymdOf = (d) => `${d.getFullYear()}-${p2(d.getMonth() + 1)}-${p2(d.getDate())}`

/**
 * Lich thang kieu "cuon lich" cho bac si: hien het cac ngay trong thang,
 * moi ngay danh dau cham mau theo trang thai lich hen ngay do.
 */
export default function MonthCalendar({ year, month, apptsByDay, todayYmd, onPrev, onNext, onThis, t, lang }) {
  const monthLabel = new Intl.DateTimeFormat(lang === 'vi' ? 'vi-VN' : 'en-US', { month: 'long', year: 'numeric' })
    .format(new Date(year, month, 1))
  const WD = lang === 'vi' ? WD_VI : WD_EN

  // 42 o (6 tuan), bat dau tu Thu Hai on/before ngay 1
  const start = new Date(year, month, 1)
  start.setDate(1 - ((start.getDay() + 6) % 7))
  const cells = Array.from({ length: 42 }, (_, i) => { const d = new Date(start); d.setDate(start.getDate() + i); return d })

  return (
    <Card className="mb-6 p-4">
      <div className="mb-3 flex items-center justify-between">
        <p className="font-semibold capitalize text-text">{monthLabel}</p>
        <div className="flex items-center gap-1">
          <button onClick={onThis} className="mr-1 text-xs text-primary hover:underline">{t('schedule.thisMonth')}</button>
          <button onClick={onPrev} aria-label="prev month" className="flex h-8 w-8 items-center justify-center rounded-lg text-muted hover:bg-slate-100 hover:text-text"><ChevronLeft className="h-4 w-4" /></button>
          <button onClick={onNext} aria-label="next month" className="flex h-8 w-8 items-center justify-center rounded-lg text-muted hover:bg-slate-100 hover:text-text"><ChevronRight className="h-4 w-4" /></button>
        </div>
      </div>

      <div className="grid grid-cols-7 gap-1 text-center text-xs font-medium text-muted">
        {WD.map((w) => <div key={w} className="py-1">{w}</div>)}
      </div>
      <div className="grid grid-cols-7 gap-1">
        {cells.map((d, i) => {
          const ymd = ymdOf(d)
          const inMonth = d.getMonth() === month
          const isToday = ymd === todayYmd
          const appts = apptsByDay[ymd] || []
          return (
            <div
              key={i}
              className={[
                'min-h-[46px] rounded-lg border p-1 text-xs',
                inMonth ? 'border-border bg-surface' : 'border-transparent bg-transparent text-slate-300',
                isToday ? 'ring-2 ring-primary ring-inset' : '',
              ].join(' ')}
            >
              <div className={`text-right font-medium ${isToday ? 'text-primary' : inMonth ? 'text-text' : 'text-slate-300'}`}>{d.getDate()}</div>
              {appts.length > 0 && (
                <div className="mt-0.5 flex flex-wrap gap-0.5">
                  {appts.slice(0, 4).map((a) => (
                    <span key={a.id} className={`h-1.5 w-1.5 rounded-full ${DOT[a.status] || 'bg-slate-400'}`} title={`${a.patientName || ''} · ${a.status}`} />
                  ))}
                  {appts.length > 4 && <span className="text-[9px] leading-none text-muted">+{appts.length - 4}</span>}
                </div>
              )}
            </div>
          )
        })}
      </div>

      {/* Chu thich mau */}
      <div className="mt-3 flex flex-wrap gap-3 text-xs text-muted">
        {['PENDING', 'CONFIRMED', 'COMPLETED', 'CANCELLED'].map((s) => (
          <span key={s} className="inline-flex items-center gap-1.5">
            <span className={`h-2 w-2 rounded-full ${DOT[s]}`} />{t(`status.${s}`)}
          </span>
        ))}
      </div>
    </Card>
  )
}
