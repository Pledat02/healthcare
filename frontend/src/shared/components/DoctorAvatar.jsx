import { useEffect, useState } from 'react'
import { Stethoscope } from 'lucide-react'

export default function DoctorAvatar({ doctor, className = 'h-14 w-14', rounded = 'rounded-2xl', priority = false }) {
  const [failed, setFailed] = useState(false)
  const src = doctor?.avatarUrl
  useEffect(() => setFailed(false), [src])

  const classes = `${className} ${rounded} shrink-0 overflow-hidden bg-gradient-to-br from-teal-500 to-cyan-600 shadow-lg shadow-teal-700/15`
  if (src && !failed) {
    return (
      <img
        src={src}
        alt={`Ảnh đại diện của ${doctor?.fullName || 'bác sĩ'}`}
        className={`${classes} object-cover`}
        width="512"
        height="512"
        loading={priority ? 'eager' : 'lazy'}
        decoding="async"
        onError={() => setFailed(true)}
      />
    )
  }

  return (
    <div className={`${classes} flex items-center justify-center text-white`} role="img" aria-label={`Chưa có ảnh của ${doctor?.fullName || 'bác sĩ'}`}>
      <Stethoscope className="h-[42%] w-[42%]" aria-hidden="true" />
    </div>
  )
}
