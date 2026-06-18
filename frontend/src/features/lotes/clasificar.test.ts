import { clasificar } from './clasificar'

function archivo(nombre: string): File {
  return new File(['x'], nombre, { type: 'application/octet-stream' })
}

describe('clasificar tipo↔archivos', () => {
  it('sin archivos pide al menos uno', () => {
    expect(clasificar([])).toEqual({ tipo: null, error: expect.stringContaining('al menos un') })
  })

  it('pdf/docx → DOCUMENTO', () => {
    expect(clasificar([archivo('informe.pdf')]).tipo).toBe('DOCUMENTO')
    expect(clasificar([archivo('a.docx'), archivo('b.doc')]).tipo).toBe('DOCUMENTO')
  })

  it('zip → CODIGO', () => {
    expect(clasificar([archivo('proyecto.zip')]).tipo).toBe('CODIGO')
  })

  it('documento + zip → MIXTA', () => {
    expect(clasificar([archivo('informe.pdf'), archivo('codigo.zip')]).tipo).toBe('MIXTA')
  })

  it('extensión no admitida es inválida', () => {
    const r = clasificar([archivo('foto.png')])
    expect(r.tipo).toBeNull()
    expect(r.error).toContain('.png')
  })

  it('ignora mayúsculas en la extensión', () => {
    expect(clasificar([archivo('INFORME.PDF')]).tipo).toBe('DOCUMENTO')
  })
})
