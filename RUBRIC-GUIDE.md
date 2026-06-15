# Guía para definir rúbricas evaluables — classroomiq

Esta guía es para docentes. Explica cómo escribir rúbricas que classroomiq pueda usar para generar **borradores de evaluación fundamentados**. No cambia tu forma de calificar: la nota final sigue siendo tuya. Lo que hace una buena rúbrica aquí es darle al asistente el contexto suficiente para que su borrador sea útil y verificable — y para que reconozca con honestidad cuándo no tiene evidencia para opinar.

> **Principio.** classroomiq genera borradores como asistencia al docente. La nota final es responsabilidad exclusiva del profesor. El sistema no reemplaza el criterio académico humano.

---

## 1. Cómo funciona la evaluación (en una frase)

Para cada criterio, el sistema busca en la entrega del estudiante los fragmentos más relacionados con la **descripción** del criterio, y le pide al modelo que sugiera un **nivel** y un **puntaje dentro del rango de ese nivel**, citando fragmentos del trabajo. Por eso la **descripción del criterio** y las **descripciones de los niveles** son lo que más impacta la calidad del borrador.

---

## 2. Anatomía de una rúbrica

Una rúbrica tiene un nombre, una descripción general, un **puntaje total**, un **modo de total** (`suma` o `promedio`) y una lista de **criterios**. Cada criterio tiene:

- **Nombre** — identificador corto ("Análisis de complejidad").
- **Descripción** — qué debe demostrar el estudiante. *Este es el texto que el modelo recibe como contexto principal.*
- **Puntaje máximo** — los puntos que aporta el criterio al total (p. ej. 6 sobre 25).
- **¿Evaluable por contenido?** — si el criterio se puede juzgar desde lo entregado (texto/código) o requiere tu juicio presencial (ver §5).
- **Niveles de desempeño** — al menos tres, cada uno con nombre, puntaje y descripción.

### Puntaje: trabajamos con puntos absolutos

A diferencia de las rúbricas que reparten porcentajes que suman 100, aquí cada criterio vale **puntos** que suman al total de la tarea (20, 25, 10…), tal como muchas rúbricas universitarias que ya usas. El "peso" de un criterio es simplemente `puntaje del criterio / total`.

- `modoTotal: suma` → el total es la suma de los criterios (ej. 6+5+3+4+4+3 = 25).
- `modoTotal: promedio` → el total es el promedio de los criterios (común cuando todos los criterios se califican sobre la misma escala, p. ej. 0–10).

### Los niveles aceptan tres formas de puntaje

Puedes mezclar el estilo que ya usas; las tres aparecen en rúbricas reales:

| Forma        | Cómo se ve              | Cuándo usarla                                   |
|--------------|-------------------------|-------------------------------------------------|
| **Rango**    | Excelente: 17–20 pts    | Das un rango de puntos por nivel.               |
| **Fijo**     | Excelente: 10 pts       | Cada nivel vale un puntaje exacto.              |
| **Banda %**  | Excelente: 90–100%      | Defines el nivel como % del puntaje del criterio.|

El número de niveles **puede variar por criterio** (un criterio con 5 niveles y otro con 2 en la misma rúbrica es válido).

---

## 3. Qué hace evaluable a un criterio

El modelo evalúa bien lo que es **observable y verificable en el trabajo entregado**. La diferencia entre un criterio bueno y uno malo es si puede contestarse mirando la entrega.

### Criterio bien definido

```
Nombre: Análisis de complejidad
Descripción: El estudiante calcula y justifica la complejidad temporal y espacial
  del algoritmo implementado, en el peor caso y el caso promedio, con notación Big-O
  correcta. Para el nivel más alto, compara con al menos una alternativa.
Puntaje máximo: 20
Niveles:
  Excelente (17–20): Calcula ambas complejidades, las justifica con desarrollo
    matemático y compara con alternativas.
  Bueno (13–16): Calcula las complejidades con justificación básica, sin comparación.
  Básico (8–12): Menciona las complejidades sin justificación o con errores menores.
  Insuficiente (0–7): No incluye análisis de complejidad o los valores son incorrectos.
```

Por qué funciona: la descripción nombra **artefactos concretos** (cálculo, justificación, notación Big-O, comparación) que el sistema puede buscar y citar en el trabajo. Los niveles se distinguen por **qué contiene** el trabajo, no por adjetivos.

### Criterio mal definido (y cómo arreglarlo)

```
✗ Nombre: Calidad
  Descripción: El trabajo demuestra calidad y dominio del tema.
  Niveles: Excelente / Bueno / Regular.
```

Problemas: "calidad" no es observable, no hay artefactos que buscar, y los niveles no
dicen **qué los diferencia**. El modelo no puede citar evidencia → el borrador será vago
o, peor, inventado.

```
✓ Nombre: Fundamentación teórica
  Descripción: El trabajo sustenta sus afirmaciones con al menos 3 referencias
    académicas pertinentes citadas en formato APA, y explica cómo cada fuente
    respalda el argumento (no solo la lista de referencias).
  Niveles:
    Excelente: 3+ fuentes pertinentes, integradas al argumento, citadas en APA.
    Bueno: 2–3 fuentes citadas, integración parcial al argumento.
    Insuficiente: menos de 2 fuentes o citadas sin relación con el texto.
```

### Reglas rápidas para descripciones

- **Nombra artefactos concretos** que deben aparecer (una tabla comparativa, un cálculo, un diagrama, una función, un test). El modelo cita mejor lo que es identificable.
- **Distingue los niveles por contenido, no por adjetivos.** "Con comparación" vs "sin comparación" es evaluable; "excelente" vs "bueno" no lo es por sí solo.
- **Lenguaje descriptivo, no valorativo.** El sistema describe lo que el trabajo *incluye*; tú decides si eso es excelente.
- **Evita criterios que mezclan varias cosas.** "Diseño, implementación y documentación" en un solo criterio produce un borrador difuso. Sepáralos.

---

## 4. Criterios sobre código

Para entregas de código, el sistema ve los archivos, lenguajes y estructura. Funcionan bien criterios que se pueden constatar en el código:

- "Implementa la función X con manejo de errores para entradas inválidas."
- "Usa kernels CUDA ejecutados en GPU (no solo librerías de alto nivel)."
- "Incluye pruebas automatizadas que cubren los casos límite descritos en la consigna."

Funcionan mal en código (déjalos para tu juicio): "código elegante", "buen estilo" sin una convención declarada. Si quieres evaluar estilo, **declara la convención** (p. ej. "sigue PEP 8: nombres, longitud de línea, docstrings").

---

## 5. Criterios que el sistema NO debe puntuar

Algunos criterios no se pueden juzgar desde lo entregado: una **demostración en vivo**, la **estética de una interfaz**, una **exposición oral**, o un entregable que el sistema no procesa (un link a un tablero, un APK, un video). Marca estos criterios como **no evaluables por contenido**.

Para esos criterios el sistema **no inventa un puntaje**: muestra el criterio con sus niveles para que **tú lo califiques**, y lo deja explícito en el borrador. Esto es deliberado — la credibilidad del asistente depende de que reconozca lo que no puede ver.

> Ejemplo: en la rúbrica de FotoFlux (`data/rubricas-ejemplo/`), el criterio "Presentación y demostración en red local" está marcado como no evaluable por contenido. Los demás (CUDA, backend, Supabase, Docker) sí lo son.

El sistema también marca **"evidencia insuficiente"** cuando un criterio *es* evaluable pero el trabajo entregado no tiene contenido suficiente para juzgarlo con confianza — en vez de rellenar con una suposición.

---

## 6. Lista de verificación antes de usar una rúbrica

- [ ] Cada criterio nombra **artefactos concretos** que el sistema puede buscar.
- [ ] Los niveles se distinguen por **qué contiene** el trabajo, no por adjetivos.
- [ ] Los puntajes de los criterios suman (o promedian) al **total declarado**.
- [ ] Los criterios que dependen de una **demo, exposición o estética** están marcados como **no evaluables por contenido**.
- [ ] Ningún criterio mezcla varias dimensiones independientes en uno solo.

---

## 7. Ejemplo completo

`data/rubricas-ejemplo/fotoflux-computacion-paralela.json` es una rúbrica universitaria real anonimizada (Computación Paralela, 25 puntos, entrega mixta código + informe) modelada con este formato. Úsala como referencia de estructura y nivel de detalle. El esquema de los campos está documentado en `data/rubricas-ejemplo/SCHEMA.md`.
