package com.classroomiq.backend.metricas.web;

import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.classroomiq.backend.metricas.MetricasUsoService;
import com.classroomiq.backend.metricas.dto.DocenteUsoDetalleResponse;
import com.classroomiq.backend.metricas.dto.MetricasUsoResponse;

/**
 * Métricas de uso y costo del LLM para el admin institucional (Fase 6, sección 7). Solo lectura,
 * rol ADMIN, acotado al tenant del admin. El parámetro {@code mes} (formato {@code YYYY-MM}) es
 * opcional: si se omite, se toma el mes actual.
 */
@RestController
@RequestMapping("/api/admin/metricas")
@PreAuthorize("hasRole('ADMIN')")
public class AdminMetricasController {

    private final MetricasUsoService metricas;

    public AdminMetricasController(MetricasUsoService metricas) {
        this.metricas = metricas;
    }

    /** Resumen institucional del mes: uso/costo por docente + total + bandera de umbral superado. */
    @GetMapping("/uso")
    public MetricasUsoResponse uso(@RequestParam(required = false) String mes) {
        return metricas.resumen(mes);
    }

    /** Detalle del mes para un docente: desglose por modelo y por operación. */
    @GetMapping("/uso/{docenteId}")
    public DocenteUsoDetalleResponse usoDocente(@PathVariable UUID docenteId,
            @RequestParam(required = false) String mes) {
        return metricas.detalleDocente(docenteId, mes);
    }
}
