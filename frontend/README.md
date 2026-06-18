# classroomiq — Frontend

Interfaz web de **classroomiq**, la plataforma de asistencia a evaluación docente universitaria. El frontend encarna el principio inamovible del producto: **el docente es el juez; la herramienta prepara la evaluación**. Los valores que sugiere el LLM se muestran siempre como **borradores**; la nota final la decide el profesor.

> v1 cubre el camino completo del **docente**. Admin y coordinador quedan para v2.

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

Credenciales del seed de demo del backend: `docente@demo.local` / `docente12345`.

## Scripts

| Script | Qué hace |
|---|---|
| `pnpm dev` | Servidor de desarrollo (HMR). |
| `pnpm build` | Genera rutas, typecheck y build de producción. |
| `pnpm preview` | Sirve el build. |
| `pnpm gen:api` | Regenera los tipos TS desde `../openapi.yaml`. |
| `pnpm gen:routes` | Regenera el árbol de rutas de TanStack Router. |
| `pnpm typecheck` | `tsc` sin emitir. |
| `pnpm lint` / `pnpm format` | ESLint (+ jsx-a11y) / Prettier. |
| `pnpm test` / `pnpm test:run` | Vitest (watch / una pasada). |
| `pnpm e2e` | Playwright (requiere el stack vivo — ver más abajo). |

**Al cambiar el contrato del backend:** regenera con `pnpm gen:api`; TypeScript marca en
rojo todo lo que dejó de cuadrar.

## Arquitectura

```
src/
  api/           # cliente generado (schema.d.ts), client.ts (Bearer + 401), errores, queryKeys
  features/      # por dominio: auth, materias, rubricas, lotes, revision, similitud, reportes
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
- **Pantalla de revisión (la pieza central):** dos paneles. Como el backend no expone el texto completo de la entrega, el panel izquierdo muestra **evidencia** (archivos + fragmentos citados por el LLM); las citas del panel derecho hacen scroll a su fragmento. El puntaje se acota al rango del nivel, el total se proyecta en vivo, y **aprobar congela** la evaluación.
- **Export PDF:** se genera en cliente (no hay endpoint backend). `@react-pdf/renderer` se carga por **dynamic import** al exportar, fuera del bundle principal.

## Tests

- **Vitest + Testing Library** para lógica pura y hooks (clasificación tipo↔archivos, niveles/total de revisión, `useEventosLote` con SSE mockeado, mapeo de `ProblemDetail`).
- **MSW** mockea el contrato en los tests de hooks/red (handlers en `src/test/msw/`).
- **Playwright** (`e2e/`) cubre el camino crítico del docente. Requiere el **stack vivo** (backend + Postgres + Ollama + frontend) y navegadores instalados (`pnpm exec playwright install`); se corre en la fase de pruebas integrales, no en el CI unitario. Variables: `E2E_BASE_URL`, `E2E_EMAIL`, `E2E_PASSWORD`.

## Convenciones

TypeScript `strict` sin `any`; estado de servidor solo en TanStack Query; estados de carga/vacío/error en cada vista; a11y (teclado, foco, ARIA, contraste) y responsive como requisito; copy en español; el principio "asistente, no reemplazante" visible donde el docente decide.
