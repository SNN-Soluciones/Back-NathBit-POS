package com.snnsoluciones.backnathbitpos.controller.kiosko;
 
import com.snnsoluciones.backnathbitpos.dto.common.ApiResponse;
import com.snnsoluciones.backnathbitpos.dto.kiosko.KioscoConfigDTO;
import com.snnsoluciones.backnathbitpos.dto.kiosko.KioskoInitResponse;
import com.snnsoluciones.backnathbitpos.dto.kiosko.KioskoOrdenDTOs;
import com.snnsoluciones.backnathbitpos.dto.kiosko.KioskoOrdenDTOs.OrdenKioskoResponse;
import com.snnsoluciones.backnathbitpos.dto.kiosko.KioskoOrdenDTOs.OrdenPendientePagoResponse;
import com.snnsoluciones.backnathbitpos.entity.global.Dispositivo;
import com.snnsoluciones.backnathbitpos.repository.KioscoConfigRepository;
import com.snnsoluciones.backnathbitpos.security.ContextoUsuario;
import com.snnsoluciones.backnathbitpos.service.kiosko.KioskoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
 
@RestController
@RequestMapping("/api/kiosko")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Kiosko", description = "Endpoints para dispositivos kiosko de autoservicio")
public class KioskoController {
 
    private final KioskoService kioskoService;
    private static final String HEADER_DEVICE_TOKEN = "X-Device-Token";
    private final KioscoConfigRepository kioscoConfigRepository;
 
    @Operation(
        summary = "Inicializar kiosko",
        description = """
            Endpoint de arranque del kiosko. Valida el dispositivo, abre o recupera
            la sesión de caja autónoma, y retorna el catálogo de productos con la
            configuración del kiosko. Llamar cada vez que el kiosko arranca o se reactiva.
            """
    )
    @GetMapping("/init")
    public ResponseEntity<ApiResponse<KioskoInitResponse>> init(
            @RequestHeader(HEADER_DEVICE_TOKEN) String deviceToken) {
 
        log.info("GET /api/kiosko/init");
 
        try {
            KioskoInitResponse response = kioskoService.init(deviceToken);
 
            // Si el kiosko está pausado, retornar 503 con el motivo
            if (response.getConfig().isPausado()) {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(ApiResponse.error("Kiosko pausado temporalmente. Consulte en caja."));
            }
 
            return ResponseEntity.ok(ApiResponse.ok("Kiosko listo", response));
 
        } catch (Exception e) {
            log.error("Error en kiosko init: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(e.getMessage()));
        }
    }

    @Operation(summary = "Obtener configuración visual del kiosco",
        description = "Retorna design tokens, template y assets para el arranque visual. Público.")
    @GetMapping("/config")
    public ResponseEntity<ApiResponse<KioscoConfigDTO>> getConfig(
        @RequestParam Long sucursalId) {

        log.info("GET /api/kiosco/config?sucursalId={}", sucursalId);
        try {
            KioscoConfigDTO config = kioscoConfigRepository
                .findBySucursalId(sucursalId)
                .map(KioscoConfigDTO::from)
                .orElse(KioscoConfigDTO.builder()
                    .sucursalId(sucursalId)
                    .templateId("CLEAN_LIGHT")
                    .colorPrimary("#1F4E79")
                    .colorBackground("#FFFFFF")
                    .textoBienvenida("¡Bienvenido!")
                    .tiempoInactividad(60)
                    .mostrarPrecios(true)
                    .requierePagoEnCaja(true)
                    .build());

            return ResponseEntity.ok(ApiResponse.ok("Configuración kiosco", config));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @Operation(summary = "Crear orden desde kiosko")
    @PostMapping("/orden")
    public ResponseEntity<ApiResponse<OrdenKioskoResponse>> crearOrden(
        @RequestHeader(HEADER_DEVICE_TOKEN) String deviceToken,
        @Valid @RequestBody KioskoOrdenDTOs.CrearOrdenKioskoRequest request) {

        log.info("POST /api/kiosko/orden");
        try {
            OrdenKioskoResponse response = kioskoService.crearOrden(deviceToken, request);
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Orden creada exitosamente", response));
        } catch (Exception e) {
            log.error("Error creando orden kiosko: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @Operation(summary = "Obtener estado de una orden (para pantalla de cliente)")
    @GetMapping("/orden/{ordenId}/estado")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getEstadoOrden(
        @RequestHeader(HEADER_DEVICE_TOKEN) String deviceToken,
        @PathVariable Long ordenId) {

        log.info("GET /api/kiosko/orden/{}/estado", ordenId);
        try {
            // Validar dispositivo y obtener schema
            Dispositivo dispositivo = kioskoService.validarDispositivo(deviceToken);
            String schemaName = dispositivo.getTenant().getSchemaName();

            Map<String, Object> estado = kioskoService.getEstadoOrden(schemaName, ordenId);
            return ResponseEntity.ok(ApiResponse.ok("Estado obtenido", estado));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // ── Endpoint para el cajero ────────────────────────────────────────────

    @Operation(summary = "Listar órdenes pendientes de pago del kiosko (para cajero)")
    @GetMapping("/ordenes-pendientes")
    @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE', 'SUPER_ADMIN', 'ADMIN', 'CAJERO')")
    public ResponseEntity<ApiResponse<List<OrdenPendientePagoResponse>>> getOrdenesPendientes(
        @RequestParam Long sucursalId,
        Authentication authentication) {

        log.info("GET /api/kiosko/ordenes-pendientes — sucursal={}", sucursalId);
        try {
            // Obtener schemaName desde el contexto del JWT
            ContextoUsuario contexto = (ContextoUsuario) authentication.getPrincipal();
            String schemaName = kioskoService.getSchemaNamePorEmpresa(contexto.getEmpresaId());

            List<OrdenPendientePagoResponse> ordenes =
                kioskoService.getOrdenesPendientesPago(schemaName, sucursalId);
            return ResponseEntity.ok(ApiResponse.ok("Órdenes pendientes", ordenes));
        } catch (Exception e) {
            log.error("Error obteniendo órdenes pendientes: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}