import { render, screen } from '@testing-library/react'

import { resumenGrupoFixture } from '@/test/msw/handlers'
import { ResumenGrupoView } from './resumen-grupo-view'

describe('ResumenGrupoView (componente compartido docente/coordinador)', () => {
  it('muestra agregados y narrativa', () => {
    render(<ResumenGrupoView resumen={resumenGrupoFixture} />)
    expect(screen.getByText('Narrativa')).toBeInTheDocument()
    expect(screen.getByText(resumenGrupoFixture.narrativa!)).toBeInTheDocument()
    expect(screen.getByText('Mapa de dominio por criterio')).toBeInTheDocument()
    // Estadísticas formateadas a 2 decimales.
    expect(screen.getByText('15.50')).toBeInTheDocument()
  })

  it('en modo lectura (coordinador) no renderiza acción de narrativa', () => {
    render(
      <ResumenGrupoView
        resumen={{ ...resumenGrupoFixture, narrativa: null }}
        mensajeSinNarrativa="El docente aún no ha generado una narrativa para este grupo."
      />,
    )
    expect(screen.queryByRole('button')).not.toBeInTheDocument()
    expect(
      screen.getByText('El docente aún no ha generado una narrativa para este grupo.'),
    ).toBeInTheDocument()
  })

  it('renderiza el slot de acción cuando se provee (docente)', () => {
    render(
      <ResumenGrupoView
        resumen={resumenGrupoFixture}
        narrativaAccion={<button type="button">Regenerar</button>}
      />,
    )
    expect(screen.getByRole('button', { name: 'Regenerar' })).toBeInTheDocument()
  })
})
