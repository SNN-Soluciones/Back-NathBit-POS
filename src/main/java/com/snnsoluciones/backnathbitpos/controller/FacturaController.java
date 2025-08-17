package com.snnsoluciones.backnathbitpos.controller;

import com.snnsoluciones.backnathbitpos.dto.common.ApiResponse;
import com.snnsoluciones.backnathbitpos.dto.factura.*;
import com.snnsoluciones.backnathbitpos.entity.*;
import com.snnsoluciones.backnathbitpos.enums.EstadoSesion;
import com.snnsoluciones.backnathbitpos.enums.mh.CondicionVenta;
import com.snnsoluciones.backnathbitpos.enums.mh.MedioPago;
import com.snnsoluciones.backnathbitpos.enums.mh.TipoDocumento;
import com.snnsoluciones.backnathbitpos.repository.*;
import com.snnsoluciones.backnathbitpos.security.jwt.JwtTokenProvider;
import com.snnsoluciones.backnathbitpos.service.FacturaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/facturas")
@RequiredArgsConstructor
@Tag(name = "Facturas", description = "Gestión de facturas y documentos")
public class FacturaController {
    
    private final FacturaService facturaService;
    private final ClienteRepository clienteRepository;
    private final ProductoRepository productoRepository;
    private final TerminalRepository terminalRepository;
    private final SesionCajaRepository sesionCajaRepository;
    private final UsuarioRepository usuarioRepository;
    private final JwtTokenProvider jwtService;

    @PostMapping
    @PreAuthorize("hasAnyRole('CAJERO', 'JEFE_CAJAS', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<FacturaResponse>> crear(
        @Valid @RequestBody CrearFacturaRequest request,
        HttpServletRequest httpRequest) {

        try {
            log.info("Creando factura para cliente: {}", request.getClienteId());

            // Obtener usuario del JWT
            String authHeader = httpRequest.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                throw new RuntimeException("Token no válido");
            }

            String token = authHeader.substring(7);
            String username = jwtService.getEmailFromToken(token); // Asumo que tienes JwtService

            Usuario cajero = usuarioRepository.findByEmail(username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            // Obtener terminal del request
            Terminal terminal = terminalRepository.findById(request.getTerminalId())
                .orElseThrow(() -> new RuntimeException("Terminal no encontrada"));

            // Obtener sesión de caja del request
            SesionCaja sesionCaja = sesionCajaRepository.findById(request.getSesionCajaId())
                .orElseThrow(() -> new RuntimeException("Sesión de caja no encontrada"));

            // Validar que la sesión esté abierta y sea de la terminal correcta
            if (!sesionCaja.getEstado().equals(EstadoSesion.ABIERTA)) {
                throw new RuntimeException("La sesión de caja está cerrada");
            }

            if (!sesionCaja.getTerminal().getId().equals(request.getTerminalId())) {
                throw new RuntimeException("La sesión no corresponde a la terminal seleccionada");
            }

            // Obtener cliente
            Cliente cliente = clienteRepository.findById(request.getClienteId())
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));

            // Construir entidad Factura
            Factura factura = new Factura();
            factura.setCliente(cliente);
            factura.setTipoDocumento(TipoDocumento.valueOf(request.getTipoDocumento()));
            factura.setCondicionVenta(CondicionVenta.fromCodigo(request.getCondicionVenta()));
            factura.setPlazoCredito(request.getPlazoCredito());
            factura.setTerminal(terminal);
            factura.setSucursal(terminal.getSucursal());
            factura.setSesionCaja(sesionCaja);
            factura.setCajero(cajero);
            factura.setDescuentos(request.getDescuento() != null ? request.getDescuento() : BigDecimal.ZERO);
            factura.setFechaEmision(LocalDateTime.now());

            // Construir detalles
            List<FacturaDetalle> detalles = new ArrayList<>();
            int numeroLinea = 1;

            for (DetalleFacturaRequest detalleReq : request.getDetalles()) {
                Producto producto = productoRepository.findById(detalleReq.getProductoId())
                    .orElseThrow(() -> new RuntimeException("Producto no encontrado: " + detalleReq.getProductoId()));

                FacturaDetalle detalle = new FacturaDetalle();
                detalle.setNumeroLinea(numeroLinea++);
                detalle.setProducto(producto);
                detalle.setCantidad(detalleReq.getCantidad());
                detalle.setPrecioUnitario(detalleReq.getPrecioUnitario());
                detalle.setMontoDescuento(detalleReq.getDescuento() != null ? detalleReq.getDescuento() : BigDecimal.ZERO);
                detalle.setUnidadMedida(producto.getUnidadMedida().name());
                detalle.setCodigoCabys(producto.getEmpresaCabys().getCodigoCabys().getCodigo());
                detalle.setDetalle(detalleReq.getDescripcionPersonalizada() != null
                    ? detalleReq.getDescripcionPersonalizada()
                    : producto.getDescripcion());

                detalles.add(detalle);
            }

            // Construir medios de pago
            List<FacturaMedioPago> mediosPago = new ArrayList<>();
            for (MedioPagoRequest medioPagoReq : request.getMediosPago()) {
                FacturaMedioPago medioPago = new FacturaMedioPago();
                medioPago.setMedioPago(MedioPago.fromCodigo(medioPagoReq.getMedioPago()));
                medioPago.setMonto(medioPagoReq.getMonto());
                medioPago.setReferencia(medioPagoReq.getReferencia());
                medioPago.setBanco(medioPagoReq.getBanco());

                mediosPago.add(medioPago);
            }

            // Crear factura
            Factura facturaCreada = facturaService.crear(factura, detalles, mediosPago);

            // Construir respuesta
            FacturaResponse response = construirResponse(facturaCreada);
            response.setMensaje("Factura creada exitosamente");

            return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Factura creada exitosamente", response));

        } catch (Exception e) {
            log.error("Error creando factura: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("Error al crear factura: " + e.getMessage()));
        }
    }
    
    @Operation(summary = "Buscar factura por clave")
    @GetMapping("/clave/{clave}")
    @PreAuthorize("hasAnyRole('CAJERO', 'JEFE_CAJAS', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<FacturaResponse>> buscarPorClave(@PathVariable String clave) {
        return facturaService.buscarPorClave(clave)
            .map(factura -> ResponseEntity.ok(ApiResponse.ok(construirResponse(factura))))
            .orElse(ResponseEntity.notFound().build());
    }
    
    @Operation(summary = "Buscar factura por consecutivo")
    @GetMapping("/consecutivo/{consecutivo}")
    @PreAuthorize("hasAnyRole('CAJERO', 'JEFE_CAJAS', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<FacturaResponse>> buscarPorConsecutivo(@PathVariable String consecutivo) {
        return facturaService.buscarPorConsecutivo(consecutivo)
            .map(factura -> ResponseEntity.ok(ApiResponse.ok(construirResponse(factura))))
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/sesion-actual")
    @PreAuthorize("hasAnyRole('CAJERO', 'JEFE_CAJAS')")
    public ResponseEntity<ApiResponse<List<FacturaListaResponse>>> listarSesionActual(
        @RequestParam Long sesionCajaId) { // Ahora viene como parámetro

        List<Factura> facturas = facturaService.listarPorSesionCaja(sesionCajaId);
        List<FacturaListaResponse> response = facturas.stream()
            .map(this::construirListaResponse)
            .toList();

        return ResponseEntity.ok(ApiResponse.ok(response));
    }
    
    @Operation(summary = "Listar facturas con error")
    @GetMapping("/errores/{sucursalId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'JEFE_CAJAS')")
    public ResponseEntity<ApiResponse<List<FacturaListaResponse>>> listarConError(@PathVariable Long sucursalId) {
        List<Factura> facturas = facturaService.listarFacturasConError(sucursalId);
        List<FacturaListaResponse> response = facturas.stream()
            .map(this::construirListaResponse)
            .toList();
        
        return ResponseEntity.ok(ApiResponse.ok(
            "Se encontraron " + response.size() + " facturas con error", 
            response
        ));
    }
    
    @Operation(summary = "Anular factura")
    @PostMapping("/{id}/anular")
    @PreAuthorize("hasAnyRole('JEFE_CAJAS', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<FacturaResponse>> anular(
            @PathVariable Long id,
            @Valid @RequestBody AnularFacturaRequest request) {
        
        try {
            Factura facturaAnulada = facturaService.anular(id, request.getMotivo());
            FacturaResponse response = construirResponse(facturaAnulada);
            response.setMensaje("Factura anulada exitosamente");
            
            return ResponseEntity.ok(ApiResponse.ok("Factura anulada", response));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Error al anular: " + e.getMessage()));
        }
    }
    
    @Operation(summary = "Reenviar factura a Hacienda")
    @PostMapping("/{id}/reenviar")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<String>> reenviar(@PathVariable Long id) {
        try {
            facturaService.reenviar(id);
            return ResponseEntity.ok(ApiResponse.ok(
                "Factura marcada para reenvío", 
                "La factura será procesada nuevamente en breve"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Error al reenviar: " + e.getMessage()));
        }
    }
    
    // Métodos helper privados
    
    private Long obtenerUsuarioId(HttpServletRequest request) {
        // TODO: Extraer del JWT token
        return 1L; // Hardcodeado por ahora
    }
    
    private FacturaResponse construirResponse(Factura factura) {
        return FacturaResponse.builder()
            .id(factura.getId())
            .clave(factura.getClave())
            .consecutivo(factura.getConsecutivo())
            .tipoDocumento(factura.getTipoDocumento().getCodigo())
            .tipoDocumentoNombre(factura.getTipoDocumento().getDescripcion())
            .fechaEmision(factura.getFechaEmision())
            .estado(factura.getEstado().name())
            .clienteId(factura.getCliente().getId())
            .clienteNombre(factura.getCliente().getRazonSocial())
            .clienteIdentificacion(factura.getCliente().getNumeroIdentificacion())
            .subtotal(factura.getSubtotal())
            .descuentos(factura.getDescuentos())
            .impuestos(factura.getImpuestos())
            .total(factura.getTotal())
            .sucursalNombre(factura.getSucursal().getNombre())
            .terminalNombre(factura.getTerminal().getNombre())
            .cajeroNombre(factura.getCajero().getNombre() + " " + factura.getCajero().getApellidos())
            .puedeAnularse(factura.getEstado().puedeAnularse())
            .puedeReenviarse(factura.getEstado().puedeReprocesarse())
            .esElectronica(factura.esElectronica())
            .build();
    }
    
    private FacturaListaResponse construirListaResponse(Factura factura) {
        String estadoColor = switch (factura.getEstado()) {
            case ACEPTADA -> "green";
            case RECHAZADA, ERROR -> "red";
            case PROCESANDO, ENVIADA -> "yellow";
            case ANULADA -> "gray";
            default -> "blue";
        };
        
        return FacturaListaResponse.builder()
            .id(factura.getId())
            .consecutivo(factura.getConsecutivo())
            .tipoDocumento(factura.getTipoDocumento().getCodigo())
            .fechaEmision(factura.getFechaEmision())
            .clienteNombre(factura.getCliente().getRazonSocial())
            .total(factura.getTotal())
            .estado(factura.getEstado().getDescripcion())
            .estadoColor(estadoColor)
            .puedeImprimir(true)
            .puedeAnular(factura.getEstado().puedeAnularse())
            .puedeReenviar(factura.getEstado().puedeReprocesarse())
            .build();
    }
}