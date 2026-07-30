// Trang thai lich hen: mau + chu (khong chi dua vao mau)
const STATUS = {
  PENDING: { label: 'Chờ xác nhận', cls: 'bg-amber-50 text-amber-700 ring-amber-200' },
  CONFIRMED: { label: 'Đã xác nhận', cls: 'bg-blue-50 text-blue-700 ring-blue-200' },
  COMPLETED: { label: 'Đã khám', cls: 'bg-green-50 text-green-700 ring-green-200' },
  CANCELLED: { label: 'Đã hủy', cls: 'bg-slate-100 text-slate-600 ring-slate-200' },
}

export function StatusBadge({ status }) {
  const s = STATUS[status] || { label: status, cls: 'bg-slate-100 text-slate-600 ring-slate-200' }
  return (
    <span
      className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium ring-1 ring-inset ${s.cls}`}
    >
      {s.label}
    </span>
  )
}
