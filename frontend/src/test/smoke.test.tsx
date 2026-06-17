import { render, screen } from '@testing-library/react'

import { Brand } from '@/components/brand'
import { ApiError, toApiError } from '@/api/errors'

describe('andamiaje H0', () => {
  it('renderiza la marca', () => {
    render(<Brand />)
    expect(screen.getByText('classroom')).toBeInTheDocument()
    expect(screen.getByText('iq')).toBeInTheDocument()
  })

  it('mapea un ProblemDetail a ApiError', () => {
    const error = toApiError({
      type: 'about:blank',
      title: 'Rúbrica incoherente',
      status: 422,
      detail: 'La suma de criterios no coincide con el total',
    })
    expect(error).toBeInstanceOf(ApiError)
    expect(error.status).toBe(422)
    expect(error.message).toBe('La suma de criterios no coincide con el total')
  })

  it('produce un ApiError defensivo ante un error desconocido', () => {
    const error = toApiError(new Error('boom'))
    expect(error).toBeInstanceOf(ApiError)
    expect(error.status).toBe(0)
  })
})
