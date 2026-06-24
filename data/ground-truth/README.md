# Datos de ground truth

Entregas ficticias con su evaluación esperada, para validar el motor de evaluación (Fase 4). Cada conjunto corresponde a una rúbrica de `data/rubricas-ejemplo/` e incluye una entrega **por cada nivel de desempeño**, de modo que el borrador generado por el LLM pueda contrastarse contra un resultado conocido.

Todos los datos son **ficticios**: ningún trabajo, nombre o nota corresponde a una persona real. Las entregas reales se mantienen fuera del repositorio (`private/`).

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

### `tarea-pila-python/`

Rúbrica: `tarea-pila-python/rubrica.json` (entrega tipo **código**, 20 pts, `modoTotal: suma`). Tarea ficticia: implementar una estructura de datos Pila (LIFO) en Python con pruebas y README. Valida el pipeline de **código** del motor (ZIP → extracción → detección de lenguaje → chunking por archivo). Cada entrega es un **directorio** de archivos fuente que se sube como una entrega tipo `CODIGO` (comprimida en `.zip`).

| Entrega | Perfil esperado | Total esperado |
|---|---|---|
| `entrega-01-excelente/`   | Completa y limpia, con casos de borde y README | 20 |
| `entrega-02-bueno/`       | Correcta, manejo de vacío parcial, README incompleto | 15 |
| `entrega-03-basico/`      | Caso feliz, falta `tope`, pruebas mínimas, sin README | 8 |
| `entrega-04-insuficiente/`| Rompe LIFO, sin pruebas ni README | 3 |

Las etiquetas por criterio están en `tarea-pila-python/etiquetas-esperadas.json`.

### `lista-enlazada-python/`

Rúbrica: `lista-enlazada-python/rubrica.json` (entrega tipo **código**, 20 pts, `modoTotal: suma`), sembrada en el sistema como `Estructuras de Datos` (ver `backend/src/main/resources/seed/rubricas/lista-enlazada-python.json` y su espejo en `data/rubricas-ejemplo/`). Tarea ficticia: implementar una lista enlazada simple en Python (inserción en ambos extremos, eliminación por valor y búsqueda) con pruebas y README.

Pensado para una **demo de lote grande**: 15 entregas ficticias repartidas en los cuatro niveles, para mostrar que la herramienta acelera la revisión de muchos trabajos sin quitarle el criterio al docente. Los `.zip` listos para subir están en `lista-enlazada-python/zips/` (uno por estudiante ficticio); cada uno contiene los archivos fuente en la raíz, como espera el pipeline de código.

| Rango | Entregas | Total esperado |
|---|---|---|
| Excelente   | `01-ana-torres`, `02-bruno-mendez`, `12-luis-bravo`                                  | 18–20 |
| Bueno       | `03-carla-rios`, `04-diego-fuentes`, `05-elena-paredes`, `11-karina-lucero`, `15-paula-herrera` | 14–16 |
| Básico      | `06-fabian-soto`, `07-gabriela-nunez`, `08-hugo-ramirez`, `13-maria-salas`          | 8–11 |
| Insuficiente| `09-ines-cardenas`, `10-javier-ortega`, `14-nicolas-vega`                           | 3–6 |

Las entregas **11 (karina-lucero)** y **15 (paula-herrera)** son gemelas semánticas (misma lógica, identificadores y comentarios distintos): sirven para demostrar la **detección de similitud semántica** como alerta de revisión manual.

Las etiquetas por criterio están en `lista-enlazada-python/etiquetas-esperadas.json`. Los directorios `entrega-*/` y los `zips/` se regeneran de forma reproducible con `lista-enlazada-python/_generar.py` a partir de `_entregas.bundle`.

## Cómo se usan (Fase 4)

1. Sembrar la rúbrica correspondiente y subir cada entrega como una entrega del lote.
2. Ejecutar el motor de evaluación para generar el borrador.
3. Comparar el nivel sugerido por criterio contra `etiquetas-esperadas.json` (tolerancia: ±1 nivel; el puntaje debe caer dentro del rango del nivel esperado).
4. Las discrepancias sistemáticas señalan dónde ajustar el prompt de evaluación — este es el método de iteración del prompt engineering descrito en el CLAUDE.md.

## Pendiente (cuando el motor sea ejecutable)

El set `tarea-pila-python/` cubre el camino de **código puro** (ZIP de proyecto) de forma compacta y autocontenida. Las rúbricas pesadas de notebook/mixta (`fotoflux-*`, `knn-*`, `analisis-exploratorio-*`) replicarían el mismo enfoque pero requieren artefactos mucho más grandes (proyecto multi-componente, GPU, notebooks); se dejan para cuando el pipeline de evaluación pueda ejecutarse contra ellas y aporten señal de validación adicional.
