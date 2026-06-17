# classroomiq — Backend

API del backend de **classroomiq**, la plataforma de asistencia a evaluación docente universitaria. El backend implementa multi-tenancy con jerarquía de roles, el pipeline de procesamiento de entregas (extracción → embeddings → pgvector), el motor de evaluación LLM por criterio, la detección de similitud, los reportes por grupo y el portal de administración con métricas de uso/costo.

> **Principio inamovible:** el docente es el juez; la herramienta elimina el trabajo cognitivo repetitivo. El sistema **nunca asigna la nota final** — genera borradores fundamentados que el  docente revisa, ajusta y aprueba.

El **contrato de la API** (endpoints, request/response, códigos de error) vive en [`../openapi.yaml`](../openapi.yaml) — una especificación **OpenAPI 3.1** que es la fuente de verdad para el frontend (apta para generar tipos/cliente TS). Este README cubre arquitectura, ejecución y configuración del backend.

---

## Stack

- **Java 21** · **Spring Boot 3.5** · **Maven**
- **Spring MVC** para la API REST · **WebFlux** conviviendo en el classpath solo para el SSE de procesamiento en tiempo real (la app corre como servlet: `spring.main.web-application-type: servlet`)
- **PostgreSQL + pgvector** (imagen `pgvector/pgvector:pg16`) · embeddings en columnas `vector(1024)` con índice **HNSW** (`vector_cosine_ops`), mapeadas con **hibernate-vector**
- **Flyway** para migraciones de esquema (`ddl-auto: validate`) · **MapStruct** para mappers · **Lombok** en entidades
- **Spring Security** + **JWT propio** (HMAC HS256, OAuth2 resource server)
- **Apache PDFBox** (PDF) · **Apache POI** (DOCX) · parser propio de notebooks/ZIP/código
- **Anthropic Java SDK** para el LLM (proveedor intercambiable) · **Ollama** para embeddings locales
- **Testcontainers** para tests de integración contra un Postgres real

---

## Arquitectura

### Organización: paquete por dominio

Cada feature es un paquete autocontenido bajo `com.classroomiq.backend` con la misma estructura interna (`domain/`, `repository/`, `dto/`, `web/`, y subpaquetes específicos):

| Paquete | Responsabilidad |
|---|---|
| `institucion` | Tenant raíz (institución). |
| `usuario` + `auth` | Cuentas, roles, login JWT. |
| `materia` + `rubrica` | CRUD de materias y rúbricas (agregado `Rubrica→Criterio→NivelDesempeno`). |
| `entrega` | Lotes y entregas; subpaquetes `storage/` (FS local), `extraccion/` (PDF/DOCX/código/notebook/ZIP), `procesamiento/` (chunking, embeddings, background, SSE). |
| `evaluacion` | Motor LLM por criterio; subpaquetes `motor/`, `retrieval/` (búsqueda coseno por criterio); API de revisión/aprobación. |
| `similitud` | Detección de similitud semántica (pgvector) y textual (n-gramas); subpaquete `calculo/`. |
| `reportes` | Resumen por grupo (estadísticas + narrativa LLM); subpaquete `calculo/`. |
| `coordinador` | Acceso de solo lectura del coordinador a reportes agregados de materias asignadas. |
| `metricas` | Libro mayor de uso del LLM y métricas de costo del portal admin; subpaquete `costo/`. |
| `provider` | Proveedores intercambiables: `embeddings/` (Ollama) y `llm/` (Anthropic). |
| `common` | Transversales: `security/`, `tenant/`, `domain/` (superclases), `error/`, `web/`. |
| `seed` | Siembra idempotente de datos demo (off en prod/tests). |

### Multi-tenancy y aislamiento de datos

- **Por tenant:** multi-tenancy por discriminador de **Hibernate 6** (`@TenantId` en   `AbstractTenantEntity`): estampa `tenant_id` en cada INSERT y lo añade a cada SELECT/UPDATE/DELETE.   La fuente es un `TenantContext` **ThreadLocal fail-closed** (UUID cero ⇒ no devuelve filas en lugar de cruzar datos). Lo puebla `TenantFilter` desde el JWT en cada request; en procesos sin request (workers async, seed, tests) se fija explícitamente con `TenantContext.set(...)`.
- **Por docente:** además del tenant, los repositorios filtran por `docente_id` (los datos de un docente son invisibles para otro dentro del mismo tenant).
- **SQL nativo** (p. ej. el retrieval coseno) **no** aplica `@TenantId`, por eso esas consultas filtran `tenant_id` **explícitamente**.

### Seguridad

- Stateless (`SessionCreationPolicy.STATELESS`), CSRF off.
- **JWT propio** HMAC **HS256** (secreto `app.jwt.secret`), validado como OAuth2 resource server. El token porta `sub` (usuarioId), `tenant_id`, `rol` y `email`.
- El claim `rol` se mapea a la autoridad `ROLE_<ROL>`; los controllers usan `@PreAuthorize("hasRole('ADMIN'|'DOCENTE'|'COORDINADOR')")`.
- Rutas abiertas: `POST /api/auth/login` y `/actuator/health`. El resto exige token.
- Sin self-signup: **el admin crea las cuentas**.

### Proveedores intercambiables (cloud / local)

`EmbeddingProvider` y `LlmProvider` abstraen el modelo concreto detrás de una interfaz seleccionable por configuración (`app.embeddings.provider`, `app.llm.provider`), para alternar entre *cloud* (Anthropic) y *local self-hosted* sin tocar el resto del código.

- **Embeddings:** `bge-m3` vía Ollama, dimensión **1024**, vectores normalizados L2.
- **LLM:** dos tiers — **potente** (`claude-sonnet-4-6`, análisis de evaluación) y **económico** (`claude-haiku-4-5`, narrativa y tareas simples).

### Procesamiento en background y SSE

El indexado y la evaluación corren en un executor dedicado (`@Async`), con el `tenant_id` propagado explícitamente al hilo (job auto-describible) más un `TaskDecorator` de seguridad. El estado por entrega se emite en tiempo real por **SSE** (`GET /api/lotes/{id}/eventos`) usando Reactor `Sinks` alimentados por eventos de dominio — sin ThreadLocal en el hilo reactivo.

### Esquema de datos (Flyway)

| Migración | Contenido |
|---|---|
| `V1` | Extensiones (`vector`). |
| `V2` | Dominio base: institución, usuario, materia, rúbrica/criterio/nivel. |
| `V3` | Entregas: `lote`, `entrega`, `archivo_entrega`, `fragmento_entrega` (`vector(1024)` + HNSW). |
| `V4` | Evaluación: `evaluacion`, `evaluacion_criterio`, `cita_fragmento`. |
| `V5` | Similitud: `reporte_similitud`, `par_similitud`, `fragmento_par_similar`. |
| `V6` | `resumen_grupo` (narrativa LLM persistida). |
| `V7` | `coordinador_materia` (asignación). |
| `V8` | `registro_uso_llm` (libro mayor de uso/costo). |

---

## Cómo ejecutar

### Requisitos

- JDK 21
- Docker + Docker Compose (para Postgres/pgvector)
- Para el pipeline real de embeddings: **Ollama** con el modelo `bge-m3` (`ollama pull bge-m3`), local o vía el servicio opcional de compose
- Para la evaluación LLM real: una `ANTHROPIC_API_KEY`

### 1. Variables de entorno

Copiar `../.env.example` a `../.env` (el backend lo importa vía `spring.config.import`) y ajustar. Ver [Configuración](#configuración) para el detalle de las variables.

### 2. Levantar la base de datos

Desde la raíz del repo:

```bash
docker compose up -d db
# opcional: embeddings locales en contenedor
docker compose --profile ollama up -d ollama
```

Postgres queda en `localhost:5436` por defecto (configurable con `POSTGRES_PORT`).

### 3. Arrancar el backend

```bash
cd backend
./mvnw spring-boot:run
```

Flyway aplica las migraciones al arrancar (`ddl-auto: validate`). En **dev** el `DataSeeder` siembra datos demo de forma idempotente: institución demo + `admin@demo.local` / `admin12345`, `docente@demo.local` / `docente12345`, 3 materias y las 4 rúbricas de ejemplo (off en prod y tests).

La API queda en `http://localhost:8080`. Smoke test:

```bash
curl -s localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"docente@demo.local","password":"docente12345"}'
```

### Perfil de producción

`SPRING_PROFILES_ACTIVE=prod` activa `application-prod.yml`, que **exige sin defaults** las variables sensibles (`DB_URL`/`DB_USERNAME`/`DB_PASSWORD`, `JWT_SECRET`, `ANTHROPIC_API_KEY`), desactiva el seed y reduce verbosidad.

---

## API (resumen)

Todas las rutas cuelgan de `/api`. Detalle completo (cuerpos, validaciones, errores) en
[`../openapi.yaml`](../openapi.yaml).

| Área | Rol | Rutas |
|---|---|---|
| Auth | público | `POST /api/auth/login` |
| Admin · usuarios | ADMIN | `POST/GET /api/admin/usuarios`, `POST /api/admin/usuarios/{id}/{activar\|desactivar}` |
| Admin · coordinadores | ADMIN | `GET/POST/DELETE /api/admin/coordinadores/{coordId}/materias[/{materiaId}]` |
| Admin · métricas | ADMIN | `GET /api/admin/metricas/uso`, `GET /api/admin/metricas/uso/{docenteId}` |
| Materias | DOCENTE | `POST/GET /api/materias`, `GET/PUT /api/materias/{id}`, `POST /api/materias/{id}/archivar` |
| Rúbricas | DOCENTE | `POST/GET /api/materias/{id}/rubricas`, `GET/PUT/DELETE /api/rubricas/{id}` |
| Lotes/entregas | DOCENTE | `POST/GET/DELETE /api/lotes…`, subida multipart `…/{id}/entregas`, `POST …/{id}/procesar`, SSE `GET …/{id}/eventos` |
| Evaluación | DOCENTE | `POST /api/lotes/{id}/evaluar` |
| Revisión | DOCENTE | `GET /api/entregas/{id}/evaluacion`, `PATCH /api/evaluaciones/{id}/criterios/{cid}`, `PATCH …/comentario`, `POST …/aprobar` |
| Similitud | DOCENTE | `POST/GET /api/lotes/{id}/similitud` |
| Resumen por grupo | DOCENTE | `GET /api/lotes/{id}/resumen`, `POST /api/lotes/{id}/resumen/narrativa` |
| Coordinador | COORDINADOR | `GET /api/coordinador/{materias, materias/{id}/lotes, lotes/{id}/resumen}` |

> Nota: varias rutas bajo `/api/lotes` se reparten entre controllers distintos (lotes/entregas, evaluación, similitud, resumen) a propósito, para no acoplar módulos.

---

## Configuración

El backend lee `../.env` (raíz del repo). Variables principales (defaults entre paréntesis):

| Variable | Para qué |
|---|---|
| `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` | Datasource Postgres. |
| `POSTGRES_*` / `POSTGRES_PORT` (5436) | Servicio `db` de compose. |
| `JWT_SECRET` | Secreto HMAC del JWT (≥ 32 bytes). |
| `SEED_ENABLED` (true en dev) | Siembra demo al arrancar. |
| `EMBEDDINGS_PROVIDER` (ollama) · `EMBEDDINGS_DIMENSION` (1024) | Proveedor/dimensión de embeddings. |
| `OLLAMA_BASE_URL` · `OLLAMA_EMBED_MODEL` (bge-m3) · `OLLAMA_TIMEOUT` · `OLLAMA_EMBED_BATCH` | Ollama. |
| `CHUNK_MAX_CHARS` (2000) · `CHUNK_OVERLAP_CHARS` (300) | Chunking antes de embeddings. |
| `EVAL_RETRIEVAL_TOPK` (8) | Fragmentos recuperados por criterio (RAG). |
| `LLM_PROVIDER` (anthropic) · `ANTHROPIC_API_KEY` | LLM. |
| `LLM_MODELO_POTENTE` (claude-sonnet-4-6) · `LLM_MODELO_ECONOMICO` (claude-haiku-4-5) · `LLM_MAX_TOKENS` · `LLM_EFFORT` (high) | Tiers y parámetros del LLM. |
| `SIMILITUD_UMBRAL_DEFAULT` (0.75) · `SIMILITUD_TOP_FRAGMENTOS` (5) · `SIMILITUD_NGRAMA_TEXTUAL` (5) | Similitud. |
| `COSTO_MONEDA` (USD) · `COSTO_UMBRAL_MENSUAL` (50.00) · `COSTO_{SONNET,HAIKU}_{INPUT,OUTPUT}` | Tarifas y umbral del costo estimado. |
| `STORAGE_BASE_PATH` (./var/entregas) | Raíz de archivos de entregas (datos sensibles, no salen del server). |
| `UPLOAD_MAX_FILE_SIZE` (50MB) · `UPLOAD_MAX_REQUEST_SIZE` (200MB) | Límites de subida multipart. |

Los `verify-on-startup` de embeddings y LLM están **off** por defecto (no atan el arranque a la
disponibilidad/costo de los proveedores).

---

## Testing

```bash
cd backend
./mvnw test          # suite completa (Testcontainers levanta Postgres+pgvector)
./mvnw clean test    # usar si aparece NoClassDefFoundError DataSeeder$1 (staleness de target)
```

- **Suite: 90 tests** (88 run / 2 *skipped*). Los 2 *skipped* son un harness de validación opt-in contra el ground truth con **LLM Anthropic y embeddings Ollama reales**, gateado por la variable `VALIDACION_GROUNDTRUTH` (la suite normal lo salta).
- Los tests de integración usan un **Postgres real** vía Testcontainers; los LLM/embeddings se *stubbean* (`app.llm.provider=fake`, `app.embeddings.provider=stub`) para no depender de servicios externos ni incurrir en costo.
- Ejecutar un test puntual: `./mvnw test -Dtest=MetricasApiTest`.

---

## Estado

Backend completo: auth/multi-tenancy, CRUD de materias/rúbricas, procesamiento de entregas con SSE, motor de evaluación LLM, revisión/aprobación, similitud, reportes por grupo con narrativa, acceso del coordinador y portal admin con métricas de uso/costo.
