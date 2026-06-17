import { cn } from '@/lib/utils'

/**
 * Marca de classroomiq. El glifo es una "marca al margen": una barra vertical
 * (el margen del papel) con un trazo de decisión en ocre — la firma visual del
 * principio "el docente decide". El sufijo "iq" hereda el color de decisión.
 */
export function Brand({ className }: { className?: string }) {
  return (
    <span className={cn('inline-flex items-center gap-2', className)}>
      <MarginMark className="text-primary" />
      <span className="font-sans text-lg font-semibold tracking-tight">
        classroom<span className="text-decision">iq</span>
      </span>
    </span>
  )
}

export function MarginMark({ className }: { className?: string }) {
  return (
    <svg
      viewBox="0 0 24 24"
      width="22"
      height="22"
      fill="none"
      aria-hidden="true"
      className={className}
    >
      {/* margen */}
      <line
        x1="6"
        y1="3"
        x2="6"
        y2="21"
        stroke="currentColor"
        strokeWidth="2"
        strokeLinecap="round"
      />
      {/* trazo de decisión */}
      <path
        d="M10 12.5l3 3 5-7"
        stroke="var(--decision)"
        strokeWidth="2.4"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </svg>
  )
}
