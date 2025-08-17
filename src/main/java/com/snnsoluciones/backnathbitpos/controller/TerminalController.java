package com.snnsoluciones.backnathbitpos.controller;

import com.snnsoluciones.backnathbitpos.dto.common.ApiResponse;
import com.snnsoluciones.backnathbitpos.dto.documento.GenerarConsecutivoRequest;
import com.snnsoluciones.backnathbitpos.dto.terminal.TerminalRequest;
import com.snnsoluciones.backnathbitpos.dto.terminal.TerminalResponse;
import com.snnsoluciones.backnathbitpos.entity.Terminal;
import com.snnsoluciones.backnathbitpos.service.SucursalService;
import com.snnsoluciones.backnathbitpos.service.TerminalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/terminales")
@RequiredArgsConstructor
@Tag(name = "Terminales", description = "Gestión de terminales/cajas")
public class TerminalController {

    private final TerminalService terminalService;
    private final SucursalService sucursalService;

    @Operation(summary = "Listar terminales por sucursal")
    @GetMapping("/sucursal/{sucursalId}")
    @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE', 'SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<TerminalResponse>>> listarPorSucursal(
        @PathVariable Long sucursalId) {

        List<Terminal> terminales = terminalService.listarPorSucursal(sucursalId);
        List<TerminalResponse> response = terminales.stream()
            .map(this::convertirAResponse)
            .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @Operation(summary = "Obtener terminal por ID")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE', 'SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<TerminalResponse>> obtenerPorId(@PathVariable Long id) {
        Terminal terminal = terminalService.buscarPorId(id)
            .orElseThrow(() -> new RuntimeException("Terminal no encontrada"));

        return ResponseEntity.ok(ApiResponse.ok(convertirAResponse(terminal)));
    }

    @Operation(summary = "Crear nueva terminal")
    @PostMapping
    @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<TerminalResponse>> crear(
        @Valid @RequestBody TerminalRequest request) {

        Terminal terminal = Terminal.builder()
            .numeroTerminal(request.getNumeroTerminal())
            .nombre(request.getNombre())
            .descripcion(request.getDescripcion())
            .activa(request.getActiva())
            .imprimirAutomatico(request.getImprimirAutomatico())
            .build();

        // Crear usando el servicio de sucursal que valida el límite
        Terminal creada = sucursalService.crearTerminal(request.getSucursalId(), terminal);

        return new ResponseEntity<>(
            ApiResponse.ok("Terminal creada exitosamente", convertirAResponse(creada)),
            HttpStatus.CREATED
        );
    }

    @Operation(summary = "Actualizar terminal")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<TerminalResponse>> actualizar(
        @PathVariable Long id,
        @Valid @RequestBody TerminalRequest request) {

        Terminal terminal = Terminal.builder()
            .numeroTerminal(request.getNumeroTerminal())
            .nombre(request.getNombre())
            .descripcion(request.getDescripcion())
            .activa(request.getActiva())
            .imprimirAutomatico(request.getImprimirAutomatico())
            .build();

        Terminal actualizada = terminalService.actualizar(id, terminal);

        return ResponseEntity.ok(
            ApiResponse.ok("Terminal actualizada exitosamente", convertirAResponse(actualizada))
        );
    }

    @Operation(summary = "Eliminar terminal")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE')")
    public ResponseEntity<ApiResponse<Void>> eliminar(@PathVariable Long id) {
        terminalService.eliminar(id);
        return ResponseEntity.ok(ApiResponse.ok("Terminal eliminada exitosamente", null));
    }

    @Operation(summary = "Generar siguiente consecutivo")
    @PostMapping("/{id}/siguiente-consecutivo")
    @PreAuthorize("hasAnyRole('CAJERO', 'JEFE_CAJAS', 'ADMIN', 'SUPER_ADMIN', 'ROOT', 'SOPORTE')")
    public ResponseEntity<ApiResponse<String>> generarConsecutivo(
        @PathVariable Long id,
        @Valid @RequestBody GenerarConsecutivoRequest request) {

        String consecutivo = terminalService.generarNumeroConsecutivo(id, request.getTipoDocumento());

        return ResponseEntity.ok(
            ApiResponse.ok("Consecutivo generado: " + consecutivo, consecutivo)
        );
    }

    // Método helper para convertir
    private TerminalResponse convertirAResponse(Terminal terminal) {
        // Construir mapa de consecutivos actuales
        Map<String, Long> consecutivos = new HashMap<>();
        consecutivos.put("01", terminal.getConsecutivoFacturaElectronica());
        consecutivos.put("04", terminal.getConsecutivoTiqueteElectronico());
        consecutivos.put("03", terminal.getConsecutivoNotaCredito());
        consecutivos.put("02", terminal.getConsecutivoNotaDebito());

        return TerminalResponse.builder()
            .id(terminal.getId())
            .numeroTerminal(terminal.getNumeroTerminal())
            .nombre(terminal.getNombre())
            .descripcion(terminal.getDescripcion())
            .activa(terminal.getActiva())
            .imprimirAutomatico(terminal.getImprimirAutomatico())
            .sucursalId(terminal.getSucursal().getId())
            .sucursalNombre(terminal.getSucursal().getNombre())
            .sucursalNumero(terminal.getSucursal().getNumeroSucursal())
            .consecutivosActuales(consecutivos)
            .tieneSesionActiva(false) // Por ahora false, sesiones es fase 2
            .usuarioSesion(null) // Por ahora null, sesiones es fase 2
            .createdAt(terminal.getCreatedAt())
            .updatedAt(terminal.getUpdatedAt())
            .build();
    }
}