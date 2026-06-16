# Test de validación de Ground Truth

- Comando: `VALIDACION_GROUNDTRUTH=true ./mvnw test -Dtest=GroundTruthValidacionTest`
- Resultados:

```bash
Mockito is currently self-attaching to enable the inline-mock-maker. This will no longer work in future releases of the JDK. Please add Mockito as an agent to your build as described in Mockito's documentation: https://javadoc.io/doc/org.mockito/mockito-core/latest/org.mockito/org/mockito/Mockito.html#0.3
OpenJDK 64-Bit Server VM warning: Sharing is only supported for boot loader classes because bootstrap classpath has been appended
WARNING: A Java agent has been loaded dynamically (/home/felipep/.m2/repository/net/bytebuddy/byte-buddy-agent/1.17.8/byte-buddy-agent-1.17.8.jar)
WARNING: If a serviceability tool is in use, please run with -XX:+EnableDynamicAgentLoading to hide this warning
WARNING: If a serviceability tool is not in use, please run with -Djdk.instrument.traceUsage for more information
WARNING: Dynamic loading of agents will be disallowed by default in a future release

===== VALIDACIÓN vs GROUND TRUTH (±1 nivel) =====

## informe-proyecto-software / entrega-01-excelente.md
  [OK ] Definición del problema y requisitos     esp=Excelente     obt=Excelente     nivelΔ=0 ✓  pts esp=4 obt=4.00 ✓
  [OK ] Diseño de la arquitectura                esp=Excelente     obt=Excelente     nivelΔ=0 ✓  pts esp=5 obt=5.00 ✓
  [OK ] Justificación de decisiones técnicas     esp=Excelente     obt=Excelente     nivelΔ=0 ✓  pts esp=5 obt=5.00 ✓
  [OK ] Fundamentación teórica y referencias     esp=Excelente     obt=Excelente     nivelΔ=0 ✓  pts esp=3 obt=3.00 ✓
  [OK ] Claridad, estructura y ortografía        esp=Excelente     obt=Excelente     nivelΔ=0 ✓  pts esp=3 obt=3.00 ✓

## informe-proyecto-software / entrega-02-bueno.md
  [OK ] Definición del problema y requisitos     esp=Bueno         obt=Bueno         nivelΔ=0 ✓  pts esp=3 obt=3.00 ✓
  [OK ] Diseño de la arquitectura                esp=Bueno         obt=Bueno         nivelΔ=0 ✓  pts esp=4 obt=4.00 ✓
  [OK ] Justificación de decisiones técnicas     esp=Bueno         obt=Bueno         nivelΔ=0 ✓  pts esp=4 obt=4.00 ✓
  [OK ] Fundamentación teórica y referencias     esp=Bueno         obt=Bueno         nivelΔ=0 ✓  pts esp=2 obt=2.00 ✓
  [OK ] Claridad, estructura y ortografía        esp=Excelente     obt=Excelente     nivelΔ=0 ✓  pts esp=3 obt=3.00 ✓  ⚠ La evaluación se basa únicamente en los fragmentos proporcionados, no en el doc…

## informe-proyecto-software / entrega-03-basico.md
  [OK ] Definición del problema y requisitos     esp=Básico        obt=Básico        nivelΔ=0 ✓  pts esp=2 obt=2.00 ✓  ⚠ Los fragmentos disponibles cubren la sección de problema y requisitos en su tot…
  [OK ] Diseño de la arquitectura                esp=Básico        obt=Básico        nivelΔ=0 ✓  pts esp=2 obt=2.00 ✓
  [OK ] Justificación de decisiones técnicas     esp=Básico        obt=Básico        nivelΔ=0 ✓  pts esp=2 obt=2.00 ✓
  [OK ] Fundamentación teórica y referencias     esp=Insuficiente  obt=Insuficiente  nivelΔ=0 ✓  pts esp=0 obt=0.00 ✓  ⚠ El trabajo no presenta ninguna sección de referencias ni citas en el texto. La …
  [OK ] Claridad, estructura y ortografía        esp=Bueno         obt=Básico        nivelΔ=1 ✓  pts esp=2 obt=1.00 ✗

## informe-proyecto-software / entrega-04-insuficiente.md
  [OK ] Definición del problema y requisitos     esp=Insuficiente  obt=Insuficiente  nivelΔ=0 ✓  pts esp=1 obt=1.00 ✓  ⚠ Los fragmentos provistos son muy escuetos y de carácter narrativo-informal. No …
  [OK ] Diseño de la arquitectura                esp=Insuficiente  obt=Insuficiente  nivelΔ=0 ✓  pts esp=0 obt=1.00 ✓  ⚠ Los fragmentos disponibles son muy escasos y no contienen sección de arquitectu…
  [OK ] Justificación de decisiones técnicas     esp=Insuficiente  obt=Insuficiente  nivelΔ=0 ✓  pts esp=0 obt=0.00 ✓  ⚠ Los fragmentos proporcionados son insuficientes para evaluar decisiones técnica…
  [OK ] Fundamentación teórica y referencias     esp=Insuficiente  obt=Insuficiente  nivelΔ=0 ✓  pts esp=0 obt=0.00 ✓  ⚠ El fragmento proporcionado es muy breve y podría corresponder solo a una secció…
  [OK ] Claridad, estructura y ortografía        esp=Insuficiente  obt=Básico        nivelΔ=1 ✓  pts esp=0 obt=1.00 ✗  ⚠ Los fragmentos disponibles son muy breves. El docente deberá verificar si el tr…

## tarea-pila-python / entrega-01-excelente/
  [OK ] Correctitud de la implementación         esp=Excelente     obt=Excelente     nivelΔ=0 ✓  pts esp=8 obt=8.00 ✓
  [OK ] Pruebas automatizadas                    esp=Excelente     obt=Excelente     nivelΔ=0 ✓  pts esp=5 obt=5.00 ✓
  [OK ] Calidad de código                        esp=Excelente     obt=Excelente     nivelΔ=0 ✓  pts esp=4 obt=4.00 ✓
  [OK ] Documentación de uso (README)            esp=Excelente     obt=Excelente     nivelΔ=0 ✓  pts esp=3 obt=3.00 ✓

## tarea-pila-python / entrega-02-bueno/
  [OK ] Correctitud de la implementación         esp=Bueno         obt=Bueno         nivelΔ=0 ✓  pts esp=6 obt=5.00 ✓
  [OK ] Pruebas automatizadas                    esp=Bueno         obt=Bueno         nivelΔ=0 ✓  pts esp=4 obt=4.00 ✓
  [OK ] Calidad de código                        esp=Bueno         obt=Bueno         nivelΔ=0 ✓  pts esp=3 obt=3.00 ✓
  [OK ] Documentación de uso (README)            esp=Bueno         obt=Bueno         nivelΔ=0 ✓  pts esp=2 obt=2.00 ✓

## tarea-pila-python / entrega-03-basico/
  [OK ] Correctitud de la implementación         esp=Básico        obt=Básico        nivelΔ=0 ✓  pts esp=4 obt=3.00 ✓  ⚠ El método tope (peek) no aparece en ninguno de los fragmentos proporcionados. S…
  [OK ] Pruebas automatizadas                    esp=Básico        obt=Básico        nivelΔ=0 ✓  pts esp=2 obt=2.00 ✓  ⚠ El fragmento no muestra uso de unittest ni pytest; es un script plano con un ún…
  [OK ] Calidad de código                        esp=Básico        obt=Básico        nivelΔ=0 ✓  pts esp=2 obt=2.00 ✓
  [OK ] Documentación de uso (README)            esp=Insuficiente  obt=Insuficiente  nivelΔ=0 ✓  pts esp=0 obt=0.00 ✓  ⚠ Los fragmentos disponibles no incluyen ningún README. Es posible que el reposit…

## tarea-pila-python / entrega-04-insuficiente/
  [OK ] Correctitud de la implementación         esp=Insuficiente  obt=Insuficiente  nivelΔ=0 ✓  pts esp=2 obt=2.00 ✓  ⚠ Los fragmentos proporcionados solo muestran '__init__', 'apilar' y 'desapilar'.…
  [OK ] Pruebas automatizadas                    esp=Insuficiente  obt=Insuficiente  nivelΔ=0 ✓  pts esp=0 obt=0.00 ✓  ⚠ El fragmento entregado solo contiene la implementación de la clase. No se propo…
  [OK ] Calidad de código                        esp=Insuficiente  obt=Básico        nivelΔ=1 ✓  pts esp=1 obt=2.00 ✗  ⚠ Solo se dispone de un fragmento corto. No es posible evaluar con certeza la sep…
  [OK ] Documentación de uso (README)            esp=Insuficiente  obt=Insuficiente  nivelΔ=0 ✓  pts esp=0 obt=0.00 ✓  ⚠ Solo se proporcionó un fragmento de código fuente. No es posible confirmar la a…

RESUMEN: 36 criterios | nivel ±1: 36 (100%) | nivel exacto: 33 (92%) | puntaje en rango esperado: 33 (92%)
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 534.2 s -- in com.classroomiq.backend.GroundTruthValidacionTest
[INFO] 
[INFO] Results:
[INFO] 
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  08:55 min
[INFO] Finished at: 2026-06-15T20:53:32-05:00
[INFO] ------------------------------------------------------------------------
```

- Costo mediante API de Anthropic: 0.55 USD
