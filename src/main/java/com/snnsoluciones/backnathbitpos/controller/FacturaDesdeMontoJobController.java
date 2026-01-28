package com.snnsoluciones.backnathbitpos.controller;

import com.snnsoluciones.backnathbitpos.dto.dashboard.UsuarioSimpleDTO;
import com.snnsoluciones.backnathbitpos.dto.pago.FacturaDesdeMontoRequest;
import com.snnsoluciones.backnathbitpos.dto.pago.JobCreateResponse;
import com.snnsoluciones.backnathbitpos.dto.pago.JobErrorResponse;
import com.snnsoluciones.backnathbitpos.entity.SesionCaja;
import com.snnsoluciones.backnathbitpos.entity.Usuario;
import com.snnsoluciones.backnathbitpos.jobs.FacturaDesdeMontoJobRunner;
import com.snnsoluciones.backnathbitpos.jobs.JobRegistry;
import com.snnsoluciones.backnathbitpos.repository.SesionCajaRepository;
import com.snnsoluciones.backnathbitpos.repository.UsuarioRepository;
import com.snnsoluciones.backnathbitpos.service.impl.SecurityContextService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Controller para gestión de jobs de generación de facturas desde monto
 * Permite crear, consultar estado y cancelar jobs de generación masiva
 */
@Slf4j
@RestController
@RequestMapping("/tickets/jobs")
@RequiredArgsConstructor
public class FacturaDesdeMontoJobController {

    private final FacturaDesdeMontoJobRunner runner;
    private final SecurityContextService securityContextService;
    private final UsuarioRepository usuarioRepository;
    private final SesionCajaRepository sesionCajaRepository;

    /**
     * Crea un job para generar facturas desde un monto total
     * El cajero especificado debe tener sesión de caja abierta
     *
     * @param request Request con monto, medios de pago y cajeroId
     * @return Response con jobId y estado inicial
     */
    @PostMapping
    public ResponseEntity<?> crearJob(@Valid @RequestBody FacturaDesdeMontoRequest request,
        @RequestParam(required = false) Long sucursalId) {
        try {
            log.info("📝 Iniciando creación de job para monto: {} con cajero ID: {}",
                request.getMontoTotal(), request.getCajeroId());

            // 1. Validar que el cajero existe
            Usuario cajero = usuarioRepository.findById(request.getCajeroId())
                .orElseThrow(() -> new IllegalArgumentException(
                    "Cajero no encontrado con ID: " + request.getCajeroId()));

            log.info("✅ Cajero encontrado: {} {}", cajero.getNombre(), cajero.getApellidos());

            // 2. Obtener sucursal del contexto (del admin autenticado)
            if (sucursalId == null) {
                throw new IllegalStateException("No se pudo determinar la sucursal actual");
            }

            // 3. Buscar sesión ABIERTA del cajero especificado
            SesionCaja sesionCaja = sesionCajaRepository
                .findSesionActivaByUsuarioAndSucursal(request.getCajeroId(), sucursalId)
                .orElseThrow(() -> new IllegalStateException(
                    String.format("El cajero %s %s no tiene sesión de caja abierta en esta sucursal",
                        cajero.getNombre(), cajero.getApellidos())));

            log.info("✅ Sesión de caja encontrada ID: {} - Terminal: {}",
                sesionCaja.getId(), sesionCaja.getTerminal().getNombre());

            // 4. Crear el job
            UUID jobId = JobRegistry.newJob();

            // 5. Ejecutar asíncronamente
            runner.run(
                jobId,
                request,
                sesionCaja.getId(),
                sesionCaja.getTerminal().getId(),
                request.getCajeroId(),
                sucursalId
            );

            log.info("🚀 Job creado exitosamente: {}", jobId);

            // 6. Respuesta estructurada
            JobCreateResponse response = JobCreateResponse.builder()
                .jobId(jobId.toString())
                .status("EN_QUEUE")
                .cajeroNombre(cajero.getNombre() + " " + cajero.getApellidos())
                .sesionCajaId(sesionCaja.getId())
                .terminalId(sesionCaja.getTerminal().getId())
                .mensaje("Proceso encolado exitosamente")
                .build();

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.error("❌ Error de validación: {}", e.getMessage());
            return ResponseEntity.badRequest().body(
                JobErrorResponse.builder()
                    .error("VALIDATION_ERROR")
                    .mensaje(e.getMessage())
                    .timestamp(LocalDateTime.now())
                    .path("/tickets/jobs")
                    .build()
            );
        } catch (IllegalStateException e) {
            log.error("❌ Error de estado: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(
                JobErrorResponse.builder()
                    .error("STATE_ERROR")
                    .mensaje(e.getMessage())
                    .timestamp(LocalDateTime.now())
                    .path("/tickets/jobs")
                    .build()
            );
        } catch (Exception e) {
            log.error("❌ Error inesperado creando job", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                JobErrorResponse.builder()
                    .error("INTERNAL_ERROR")
                    .mensaje("Error inesperado: " + e.getMessage())
                    .timestamp(LocalDateTime.now())
                    .path("/tickets/jobs")
                    .build()
            );
        }
    }

    /**
     * Consulta el estado de un job específico
     *
     * @param jobId ID del job a consultar
     * @return Estado actual del job con progreso
     */
    @GetMapping("/{jobId}")
    public ResponseEntity<?> estado(@PathVariable String jobId) {
        try {
            var info = JobRegistry.get(UUID.fromString(jobId));
            return ResponseEntity.ok(info != null ? info : Map.of("error", "job no encontrado"));
        } catch (IllegalArgumentException e) {
            log.error("❌ ID de job inválido: {}", jobId);
            return ResponseEntity.badRequest().body(
                JobErrorResponse.builder()
                    .error("INVALID_JOB_ID")
                    .mensaje("ID de job inválido: " + jobId)
                    .timestamp(LocalDateTime.now())
                    .path("/tickets/jobs/" + jobId)
                    .build()
            );
        }
    }

    /**
     * Solicita la cancelación de un job en ejecución
     *
     * @param jobId ID del job a cancelar
     * @return Confirmación de solicitud de cancelación
     */
    @PostMapping("/{jobId}/cancel")
    public ResponseEntity<?> cancelar(@PathVariable String jobId) {
        try {
            UUID id = UUID.fromString(jobId);
            JobRegistry.cancel(id);

            log.info("⚠️ Cancelación solicitada para job: {}", jobId);

            return ResponseEntity.ok(Map.of(
                "jobId", jobId,
                "status", "CANCEL_REQUESTED",
                "mensaje", "Cancelación solicitada exitosamente"
            ));
        } catch (IllegalArgumentException e) {
            log.error("❌ ID de job inválido: {}", jobId);
            return ResponseEntity.badRequest().body(
                JobErrorResponse.builder()
                    .error("INVALID_JOB_ID")
                    .mensaje("ID de job inválido: " + jobId)
                    .timestamp(LocalDateTime.now())
                    .path("/tickets/jobs/" + jobId + "/cancel")
                    .build()
            );
        }
    }

    @GetMapping("/cajeros-disponibles")
    public ResponseEntity<List<UsuarioSimpleDTO>> getCajerosConSesionAbierta(
        @RequestParam(required = false) Long sucursalId) {  // 👈 Parámetro opcional
        try {
            // Si no viene por parámetro, intentar obtenerlo del contexto
            if (sucursalId == null) {
                sucursalId = securityContextService.getCurrentSucursalId();
            }

            if (sucursalId == null) {
                log.error("❌ No se pudo determinar la sucursal");
                return ResponseEntity.badRequest().body(
                    List.of() // O puedes lanzar una excepción
                );
            }

            log.info("🔍 Buscando cajeros con sesión abierta en sucursal ID: {}", sucursalId);

            List<Usuario> cajeros = usuarioRepository.findCajerosConSesionAbierta(sucursalId);
            log.info("✅ Cajeros encontrados: {}", cajeros.size());

            cajeros.forEach(c -> log.info("   - {} {} (ID: {})",
                c.getNombre(), c.getApellidos(), c.getId()));

            List<UsuarioSimpleDTO> dtos = cajeros.stream()
                .map(u -> UsuarioSimpleDTO.builder()
                    .id(u.getId())
                    .nombre(u.getNombre() + " " + u.getApellidos())
                    .rol(u.getRol().name())
                    .build())
                .toList();

            return ResponseEntity.ok(dtos);
        } catch (Exception e) {
            log.error("❌ Error obteniendo cajeros con sesión abierta", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}