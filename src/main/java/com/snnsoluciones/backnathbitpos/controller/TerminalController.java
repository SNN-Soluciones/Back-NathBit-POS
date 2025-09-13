package com.snnsoluciones.backnathbitpos.controller;

import com.snnsoluciones.backnathbitpos.dto.common.ApiResponse;
import com.snnsoluciones.backnathbitpos.dto.documento.GenerarConsecutivoRequest;
import com.snnsoluciones.backnathbitpos.dto.terminal.TerminalRequest;
import com.snnsoluciones.backnathbitpos.dto.terminal.TerminalResponse;
import com.snnsoluciones.backnathbitpos.entity.Terminal;
import com.snnsoluciones.backnathbitpos.service.SesionCajaService;
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
    private final SesionCajaService sesionCajaService;

    @Operation(summary = "Listar terminales por sucursal")
    @GetMapping("/sucursal/{sucursalId}")
    @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE', 'SUPER_ADMIN', 'ADMIN', 'JEFE_CAJAS', 'CAJERO', 'MESERO')")
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
    @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE', 'SUPER_ADMIN', 'ADMIN', 'JEFE_CAJAS', 'CAJERO', 'MESERO')")
    public ResponseEntity<ApiResponse<TerminalResponse>> obtenerPorId(@PathVariable Long id) {
        Terminal terminal = terminalService.buscarPorId(id)
            .orElseThrow(() -> new RuntimeException("Terminal no encontrada"));

        return ResponseEntity.ok(ApiResponse.ok(convertirAResponse(terminal)));
    }

    @Operation(summary = "Crear nueva terminal")
    @PostMapping
    @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE', 'SUPER_ADMIN', 'ADMIN', 'JEFE_CAJAS', 'CAJERO', 'MESERO')")
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
    @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE', 'SUPER_ADMIN', 'ADMIN', 'JEFE_CAJAS', 'CAJERO', 'MESERO')")
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
    @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE', 'SUPER_ADMIN', 'ADMIN', 'JEFE_CAJAS', 'CAJERO', 'MESERO')")
    public ResponseEntity<ApiResponse<Void>> eliminar(@PathVariable Long id) {
        terminalService.eliminar(id);
        return ResponseEntity.ok(ApiResponse.ok("Terminal eliminada exitosamente", null));
    }

    @Operation(summary = "Generar siguiente consecutivo")
    @PostMapping("/{id}/siguiente-consecutivo")
    @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE', 'SUPER_ADMIN', 'ADMIN', 'JEFE_CAJAS', 'CAJERO', 'MESERO')")
    public ResponseEntity<ApiResponse<String>> generarConsecutivo(
        @PathVariable Long id,
        @Valid @RequestBody GenerarConsecutivoRequest request) {

        String consecutivo = terminalService.generarNumeroConsecutivo(id, request.getTipoDocumento());

        return ResponseEntity.ok(
            ApiResponse.ok("Consecutivo generado: " + consecutivo, consecutivo)
        );
    }

    @Operation(summary = "Cambiar estado de terminal")
    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE', 'SUPER_ADMIN', 'ADMIN', 'JEFE_CAJAS', 'CAJERO', 'MESERO')")
    public ResponseEntity<ApiResponse<TerminalResponse>> cambiarEstado(
        @PathVariable Long id,
        @RequestBody Map<String, Boolean> request) {

        Boolean activa = request.get("activa");
        if (activa == null) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Debe especificar el estado"));
        }

        // Verificar que no tenga sesión activa si se quiere desactivar
        if (!activa && terminalService.estaOcupada(id)) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("No se puede desactivar una terminal con sesión activa"));
        }

        Terminal terminal = terminalService.buscarPorId(id)
            .orElseThrow(() -> new RuntimeException("Terminal no encontrada"));

        terminal.setActiva(activa);
        terminal = terminalService.actualizar(id, terminal);

        return ResponseEntity.ok(ApiResponse.ok(convertirAResponse(terminal)));
    }

    @Operation(summary = "Verificar disponibilidad de terminal")
    @GetMapping("/{id}/disponible")
    @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE', 'SUPER_ADMIN', 'ADMIN', 'JEFE_CAJAS', 'CAJERO', 'MESERO')")
    public ResponseEntity<ApiResponse<Boolean>> verificarDisponibilidad(@PathVariable Long id) {
        boolean disponible = !terminalService.estaOcupada(id);
        return ResponseEntity.ok(ApiResponse.ok(disponible));
    }

    @Operation(summary = "Listar terminales libres por sucursal")
    @GetMapping("/sucursal/{sucursalId}/libres")
    @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE', 'SUPER_ADMIN', 'ADMIN', 'JEFE_CAJAS', 'CAJERO', 'MESERO')")
    public ResponseEntity<ApiResponse<List<TerminalResponse>>> listarTerminalesLibres(
        @PathVariable Long sucursalId) {

        List<Terminal> terminales = terminalService.listarPorSucursal(sucursalId)
            .stream()
            .filter(t -> t.getActiva() && !terminalService.estaOcupada(t.getId()))
            .toList();

        List<TerminalResponse> response = terminales.stream()
            .map(this::convertirAResponse)
            .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    private TerminalResponse convertirAResponse(Terminal terminal) {
        TerminalResponse.TerminalResponseBuilder builder = TerminalResponse.builder()
            .id(terminal.getId())
            .numeroTerminal(terminal.getNumeroTerminal())
            .nombre(terminal.getNombre())
            .descripcion(terminal.getDescripcion())
            .activa(terminal.getActiva())
            .sucursalId(terminal.getSucursal().getId())
            .sucursalNombre(terminal.getSucursal().getNombre())
            .tipoImpresion(terminal.getTipoImpresion())
            .imprimirAutomatico(terminal.getImprimirAutomatico())
            // Consecutivos
            .consecutivoFacturaElectronica(terminal.getConsecutivoFacturaElectronica())
            .consecutivoTiqueteElectronico(terminal.getConsecutivoTiqueteElectronico())
            .consecutivoNotaCredito(terminal.getConsecutivoNotaCredito())
            .consecutivoNotaDebito(terminal.getConsecutivoNotaDebito())
            .consecutivoFacturaCompra(terminal.getConsecutivoFacturaCompra())
            .consecutivoFacturaExportacion(terminal.getConsecutivoFacturaExportacion())
            .consecutivoReciboPago(terminal.getConsecutivoReciboPago())
            .consecutivoTiqueteInterno(terminal.getConsecutivoTiqueteInterno())
            .consecutivoFacturaInterna(terminal.getConsecutivoFacturaInterna())
            .consecutivoProforma(terminal.getConsecutivoProforma())
            .consecutivoOrdenPedido(terminal.getConsecutivoOrdenPedido());

        // Verificar si tiene sesión activa
        boolean tieneSesionActiva = terminalService.estaOcupada(terminal.getId());
        builder.tieneSesionActiva(tieneSesionActiva);

        // Si tiene sesión, obtener info adicional
        if (tieneSesionActiva) {
            // Este método debería agregarse al servicio para obtener la sesión activa
            sesionCajaService.buscarSesionActivaPorTerminal(terminal.getId())
                .ifPresent(sesion -> {
                    builder.usuarioSesion(sesion.getUsuario().getNombre())
                        .fechaAperturaSesion(sesion.getFechaHoraApertura());
                });
        }

        return builder.build();
    }


}