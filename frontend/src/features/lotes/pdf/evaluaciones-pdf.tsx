import { Document, Page, StyleSheet, Text, View } from '@react-pdf/renderer'

export interface FilaCriterioPdf {
  nombre: string
  nivel: string | null
  puntaje: number | null
  max: number
  justificacion: string | null
}

export interface EvaluacionPdf {
  identificador: string
  total: number | null
  comentario: string | null
  criterios: FilaCriterioPdf[]
}

export interface DatosPdf {
  loteNombre: string
  totalMax: number | null
  generadoAt: string
  evaluaciones: EvaluacionPdf[]
}

const COLORES = {
  texto: '#1c1c1c',
  tenue: '#6b6b6b',
  borde: '#d9d9d9',
  acento: '#0e5a63',
}

const estilos = StyleSheet.create({
  page: {
    paddingVertical: 40,
    paddingHorizontal: 48,
    fontSize: 10,
    color: COLORES.texto,
    fontFamily: 'Helvetica',
  },
  encabezadoLote: {
    marginBottom: 16,
    borderBottomWidth: 1,
    borderBottomColor: COLORES.borde,
    paddingBottom: 8,
  },
  tituloLote: { fontSize: 16, fontFamily: 'Helvetica-Bold' },
  meta: { fontSize: 9, color: COLORES.tenue, marginTop: 2 },
  aviso: { fontSize: 8, color: COLORES.tenue, marginTop: 6, fontStyle: 'italic' },
  evaluacion: { marginBottom: 18 },
  cabeceraEval: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'flex-end',
    marginBottom: 8,
  },
  identificador: { fontSize: 13, fontFamily: 'Helvetica-Bold' },
  total: { fontSize: 13, fontFamily: 'Helvetica-Bold', color: COLORES.acento },
  criterio: {
    marginBottom: 8,
    paddingBottom: 6,
    borderBottomWidth: 0.5,
    borderBottomColor: COLORES.borde,
  },
  filaCriterio: { flexDirection: 'row', justifyContent: 'space-between', marginBottom: 2 },
  nombreCriterio: { fontFamily: 'Helvetica-Bold', flex: 1, paddingRight: 8 },
  puntajeCriterio: { color: COLORES.tenue },
  nivel: { fontSize: 9, color: COLORES.acento, marginBottom: 2 },
  justificacion: { fontSize: 9, color: COLORES.texto, lineHeight: 1.4 },
  comentario: { marginTop: 6, padding: 8, backgroundColor: '#f5f5f3', borderRadius: 4 },
  comentarioTitulo: { fontSize: 9, fontFamily: 'Helvetica-Bold', marginBottom: 2 },
  pie: {
    position: 'absolute',
    bottom: 24,
    left: 48,
    right: 48,
    fontSize: 8,
    color: COLORES.tenue,
    textAlign: 'center',
    borderTopWidth: 0.5,
    borderTopColor: COLORES.borde,
    paddingTop: 6,
  },
})

const fmt = (v: number | null) => (v == null ? '—' : Number.isInteger(v) ? String(v) : v.toFixed(2))

/** Documento PDF con las evaluaciones aprobadas de un lote (generado en cliente). */
export function EvaluacionesPdf({ datos }: { datos: DatosPdf }) {
  return (
    <Document title={`Evaluaciones — ${datos.loteNombre}`}>
      <Page size="A4" style={estilos.page}>
        <View style={estilos.encabezadoLote}>
          <Text style={estilos.tituloLote}>{datos.loteNombre}</Text>
          <Text style={estilos.meta}>
            {datos.evaluaciones.length}{' '}
            {datos.evaluaciones.length === 1 ? 'evaluación aprobada' : 'evaluaciones aprobadas'} ·
            Generado el {datos.generadoAt}
          </Text>
          <Text style={estilos.aviso}>
            Borradores generados como asistencia al docente. La nota final es responsabilidad
            exclusiva del profesor.
          </Text>
        </View>

        {datos.evaluaciones.map((evaluacion, i) => (
          <View key={i} style={estilos.evaluacion} wrap={false} break={i > 0}>
            <View style={estilos.cabeceraEval}>
              <Text style={estilos.identificador}>{evaluacion.identificador}</Text>
              <Text style={estilos.total}>
                {fmt(evaluacion.total)}
                {datos.totalMax != null ? ` / ${fmt(datos.totalMax)}` : ''}
              </Text>
            </View>

            {evaluacion.criterios.map((criterio, j) => (
              <View key={j} style={estilos.criterio} wrap={false}>
                <View style={estilos.filaCriterio}>
                  <Text style={estilos.nombreCriterio}>{criterio.nombre}</Text>
                  <Text style={estilos.puntajeCriterio}>
                    {fmt(criterio.puntaje)} / {fmt(criterio.max)}
                  </Text>
                </View>
                {criterio.nivel ? <Text style={estilos.nivel}>{criterio.nivel}</Text> : null}
                {criterio.justificacion ? (
                  <Text style={estilos.justificacion}>{criterio.justificacion}</Text>
                ) : null}
              </View>
            ))}

            {evaluacion.comentario ? (
              <View style={estilos.comentario}>
                <Text style={estilos.comentarioTitulo}>Comentario general</Text>
                <Text style={estilos.justificacion}>{evaluacion.comentario}</Text>
              </View>
            ) : null}
          </View>
        ))}

        <Text
          style={estilos.pie}
          render={({ pageNumber, totalPages }) => `${pageNumber} / ${totalPages}`}
          fixed
        />
      </Page>
    </Document>
  )
}
