# classroomiq — Frontend

Interfaz web de **classroomiq**, la plataforma de asistencia a evaluación docente universitaria. El frontend encarna el principio inamovible del producto: **el docente es el juez; la herramienta prepara la evaluación**. Los valores que sugiere el LLM se muestran siempre como **borradores**; la nota final la decide el profesor.

> **v1** cubre el camino completo del **docente**. **v2** abre la plataforma a los roles
> institucionales: **admin** (gestión de cuentas, asignación de coordinadores, dashboard de
> uso/costo del LLM) y **coordinador** (acceso de solo lectura a los reportes agregados por grupo
> de las materias asignadas — nunca trabajos, evaluaciones individuales ni similitud).

## Stack

- **React 19 + TypeScript** (strict) sobre **Vite 8**.
- **Shadcn/ui** (Radix + **Tailwind v4**) con identidad visual propia (tokens en `@theme`, modo claro/oscuro).
- **TanStack Router** (rutas type-safe, file-based) + **TanStack Query** (estado de servidor) + **TanStack Table** (listados).
- **Cliente de API generado** desde `../openapi.yaml`: `openapi-typescript` (tipos) + `openapi-fetch` (cliente tipado). Los tipos nunca se desincronizan del backend.
- Formularios: **react-hook-form + zod**. SSE: **@microsoft/fetch-event-source**. Charts: **Recharts**. PDF: **@react-pdf/renderer** (en cliente). Toasts: **sonner**.

## Requisitos

- Node ≥ 20 y **pnpm**.
- El **backend** de classroomiq corriendo (ver `../backend/README.md`) y su CORS apuntando al origen del frontend (default `http://localhost:5173`).

## Puesta en marcha

```bash
pnpm install
cp .env.example .env        # define VITE_API_URL (default http://localhost:8080)
pnpm gen:api                # genera src/api/schema.d.ts desde ../openapi.yaml
pnpm dev                    # http://localhost:5173
```

Credenciales del seed de demo del backend, una por rol:

| Rol         | Email                    | Contraseña     |
| ----------- | ------------------------ | -------------- |
| Docente     | `docente@demo.local`     | `docente12345` |
| Admin       | `admin@demo.local`       | `admin12345`   |
| Coordinador | `coordinador@demo.local` | `coord12345`   |

Tras el login, el **dispatcher por rol** enruta a cada usuario a su portal (docente → `/materias`,
admin → `/admin/cuentas`, coordinador → `/coordinador`); las rutas quedan protegidas por **guards de
rol** y el shell muestra solo la navegación de ese rol.

## Scripts

| Script                        | Qué hace                                             |
| ----------------------------- | ---------------------------------------------------- |
| `pnpm dev`                    | Servidor de desarrollo (HMR).                        |
| `pnpm build`                  | Genera rutas, typecheck y build de producción.       |
| `pnpm preview`                | Sirve el build.                                      |
| `pnpm gen:api`                | Regenera los tipos TS desde `../openapi.yaml`.       |
| `pnpm gen:routes`             | Regenera el árbol de rutas de TanStack Router.       |
| `pnpm typecheck`              | `tsc` sin emitir.                                    |
| `pnpm lint` / `pnpm format`   | ESLint (+ jsx-a11y) / Prettier.                      |
| `pnpm test` / `pnpm test:run` | Vitest (watch / una pasada).                         |
| `pnpm e2e`                    | Playwright (requiere el stack vivo — ver más abajo). |

**Al cambiar el contrato del backend:** regenera con `pnpm gen:api`; TypeScript marca en
rojo todo lo que dejó de cuadrar.

## Arquitectura

```
src/
  api/           # cliente generado (schema.d.ts), client.ts (Bearer + 401), errores, queryKeys
  features/      # por dominio: auth, materias, rubricas, lotes, revision, similitud, reportes, admin, coordinador
  components/ui/ # primitivos de Shadcn
  components/    # compartidos (shell, estados, marca, tema)
  hooks/         # transversales (useEventosLote — SSE)
  routes/        # árbol de TanStack Router (file-based)
  test/          # setup de Vitest + handlers/server de MSW
e2e/             # specs de Playwright
```

Los componentes nunca llaman a `fetch`: consumen **hooks de TanStack Query** por feature.
Los tipos de dominio siempre se derivan del schema generado.

## Decisiones y notas

- **SSE con Bearer:** la `EventSource` nativa no envía `Authorization`; se usa `@microsoft/fetch-event-source`. `useEventosLote` abre el stream del lote y actualiza el cache de entregas por evento (`setQueryData`).
- **Subida de entregas:** el `tipo` (DOCUMENTO/CODIGO/MIXTA) se **deriva** de las extensiones en cliente (espejo del backend) para evitar el 422. El progreso es indeterminado ("Subiendo…") porque `fetch` no expone bytes.
- **Pantalla de revisión (la pieza central):** dos paneles. El panel izquierdo muestra el **documento completo** de la entrega (vía `GET /api/entregas/{id}/contenido`, reconstruido en el backend por re-extracción, sin costo de LLM) con los **fragmentos citados resaltados en contexto** (`<mark>`); los chips de cita del panel derecho hacen scroll + destello hacia el resaltado. Si el contenido no está disponible, cae a un panel de fragmentos citados (fallback). El emparejado cita↔texto es una función pura (`resaltado.ts`) que normaliza espacios/mayúsculas y mapea la posición de vuelta al texto original. El puntaje se acota al rango del nivel, el total se proyecta en vivo, y **aprobar congela** la evaluación.
- **Export PDF:** se genera en cliente (no hay endpoint backend). `@react-pdf/renderer` se carga por **dynamic import** al exportar, fuera del bundle principal. El **export Excel/CSV** por lote reusa la misma recolección de datos (`features/lotes/export/`); SheetJS (`xlsx`) también se carga diferido.
- **Multi-rol (v2):** un único shell autenticado con navegación derivada del rol del JWT. El **portal admin** (`/admin/*`) cubre cuentas, asignación de coordinadores y el **dashboard de uso/costo** (`/api/admin/metricas/uso`, selector de mes, alerta de umbral, desglose por docente/modelo/operación con Recharts). La **vista coordinador** (`/coordinador/*`) es de **solo lectura** y reusa el componente compartido de resumen por grupo (`ResumenGrupoView`) sin el botón de generar narrativa; por diseño no ofrece ninguna ruta a trabajos, evaluaciones individuales ni similitud.

## Tests

- **Vitest + Testing Library** para lógica pura y hooks (clasificación tipo↔archivos, niveles/total de revisión, `useEventosLote` con SSE mockeado, mapeo de `ProblemDetail`).
- **MSW** mockea el contrato en los tests de hooks/red (handlers en `src/test/msw/`).
- **Playwright** (`e2e/`) cubre el camino crítico del **docente** (`critico.spec.ts`), el portal **admin** (`admin.spec.ts`: cuentas, métricas de uso/costo, coordinadores) y la vista **coordinador** (`coordinador.spec.ts`: materias asignadas en solo lectura + guard de rol). Además, sobre las vistas multi-rol nuevas: un **pase de accesibilidad con axe-core** (`a11y.spec.ts`, vía `@axe-core/playwright`, falla ante violaciones WCAG A/AA de impacto `critical`/`serious`) y un **pase responsive en tablet** (`responsive.spec.ts`, 768×1024, sin desbordamiento horizontal). Requiere el **stack vivo** (backend + Postgres + Ollama + frontend) y navegadores instalados (`pnpm exec playwright install`); se corre en la fase de pruebas integrales, no en el CI unitario. Variables: `E2E_BASE_URL`, `E2E_EMAIL`/`E2E_PASSWORD` (docente), `E2E_ADMIN_EMAIL`/`E2E_ADMIN_PASSWORD`, `E2E_COORD_EMAIL`/`E2E_COORD_PASSWORD`.

## Convenciones

TypeScript `strict` sin `any`; estado de servidor solo en TanStack Query; estados de carga/vacío/error en cada vista; a11y (teclado, foco, ARIA, contraste) y responsive como requisito; copy en español; el principio "asistente, no reemplazante" visible donde el docente decide.
