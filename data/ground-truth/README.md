# Datos de ground truth

Entregas ficticias con su evaluación esperada, para validar el motor de evaluación
(Fase 4). Cada conjunto corresponde a una rúbrica de `data/rubricas-ejemplo/` e incluye
una entrega **por cada nivel de desempeño**, de modo que el borrador generado por el LLM
pueda contrastarse contra un resultado conocido.

Todos los datos son **ficticios**: ningún trabajo, nombre o nota corresponde a una
persona real. Las entregas reales se mantienen fuera del repositorio (`private/`).

## Conjuntos disponibles

### `informe-proyecto-software/`

Rúbrica: `informe-proyecto-software.json` (documento puro, 20 pts, `modoTotal: suma`).
Proyecto ficticio: sistema de reservas de salas de estudio de una biblioteca.

| Entrega | Perfil esperado | Total esperado |
|---|---|---|
| `entrega-01-excelente.md`   | Excelente y consistente               | 20 |
| `entrega-02-bueno.md`       | Sólido, sin profundidad               | 16 |
| `entrega-03-basico.md`      | Vago, tecnologías sin justificar      | 8  |
| `entrega-04-insuficiente.md`| Apenas esbozado, desorganizado        | 1  |

Las etiquetas por criterio están en `etiquetas-esperadas.json`.

## Cómo se usan (Fase 4)

1. Sembrar la rúbrica correspondiente y subir cada entrega como una entrega del lote.
2. Ejecutar el motor de evaluación para generar el borrador.
3. Comparar el nivel sugerido por criterio contra `etiquetas-esperadas.json`
   (tolerancia: ±1 nivel; el puntaje debe caer dentro del rango del nivel esperado).
4. Las discrepancias sistemáticas señalan dónde ajustar el prompt de evaluación —
   este es el método de iteración del prompt engineering descrito en el CLAUDE.md.

## Pendiente (cuando el motor sea ejecutable)

Replicar el mismo enfoque para las rúbricas basadas en código/notebook
(`fotoflux-*`, `knn-*`, `analisis-exploratorio-*`). Esas entregas de ground truth
requieren artefactos de código y son más pesadas de construir; se dejan para cuando el
pipeline de evaluación pueda ejecutarse contra ellas.
