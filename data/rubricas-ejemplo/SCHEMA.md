# Esquema de rúbrica (`data/rubricas-ejemplo/*.json`)

Este es el formato canónico de rúbrica de classroomiq. Está diseñado a partir de rúbricas universitarias reales (ver `private/rubricas/`, no versionado) y soporta las variaciones que realmente aparecen en ellas: número de niveles variable por criterio, tres formas de puntaje por nivel, y total por suma o promedio.

> Decisión de diseño: el modelo primario es **puntos absolutos por criterio**.
> El "peso %" del CLAUDE.md es una vista derivada (`peso = puntajeMaximo / total`), no el dato de origen. Razón: ninguna de las rúbricas reales revisadas usa pesos que sumen 100; usan puntos que suman al total de la tarea (20, 25…) o el promedio de criterios.

## Estructura

```jsonc
{
  "nombre": "string",                 // título de la rúbrica
  "materia": "string",                // materia/asignatura de origen
  "descripcion": "string",            // descripción general de la tarea evaluada
  "puntajeTotal": 25,                 // puntaje máximo de la tarea
  "modoTotal": "suma",                // "suma" | "promedio"
                                      //   suma     → total = Σ puntaje de cada criterio
                                      //   promedio → total = promedio de los criterios (se reescala a puntajeTotal)
  "criterios": [
    {
      "nombre": "string",             // identificador corto del criterio
      "descripcion": "string",        // QUÉ debe demostrar el estudiante. Es el contexto
                                      //   principal que recibe el LLM. Cuanto más concreto y
                                      //   verificable, mejor el borrador (ver RUBRIC-GUIDE.md).
      "puntajeMaximo": 6,             // puntos que aporta este criterio al total
      "evaluablePorContenido": true,  // true  → el LLM puede evaluarlo desde el texto/código entregado
                                      // false → requiere juicio presencial/visual del docente
                                      //         (demo en vivo, interfaz, exposición). El sistema NO
                                      //         inventa puntaje: marca "requiere juicio del docente".
      "niveles": [
        {
          "nombre": "Excelente",
          "tipoPuntaje": "bandaPct",  // "rango" | "fijo" | "bandaPct"
          "pctMin": 90,               // tipoPuntaje=bandaPct → % del puntajeMaximo (90 = 90%)
          "pctMax": 100,
          "descripcion": "Qué debe contener el trabajo para merecer este nivel."
        }
        // tipoPuntaje="rango":  usar  "min": 17, "max": 20   (puntos absolutos)
        // tipoPuntaje="fijo":   usar  "valor": 10            (puntos absolutos, nivel discreto)
      ]
    }
  ]
}
```

## Las tres formas de puntaje por nivel (todas vistas en rúbricas reales)

| `tipoPuntaje` | Campos       | Ejemplo real            | Puntos resultantes              |
|---------------|--------------|-------------------------|---------------------------------|
| `rango`       | `min`,`max`  | Análisis Exploratorio   | nivel "Bueno" = 6–8 pts         |
| `fijo`        | `valor`      | KNN vs RN               | nivel "Excelente" = 10 pts      |
| `bandaPct`    | `pctMin`,`pctMax` | FotoFlux           | nivel 90–100% de `puntajeMaximo`|

El motor de evaluación normaliza las tres a un rango de puntos `[min, max]` sobre el `puntajeMaximo` del criterio, y el LLM sugiere un puntaje dentro de ese rango.
