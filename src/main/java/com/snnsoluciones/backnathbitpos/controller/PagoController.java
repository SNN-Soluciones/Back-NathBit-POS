package com.snnsoluciones.backnathbitpos.controller;

import com.snnsoluciones.backnathbitpos.dto.common.ApiResponse;
import com.snnsoluciones.backnathbitpos.dto.pago.PagoResponseDTO;
import com.snnsoluciones.backnathbitpos.dto.pago.RegistrarPagoDTO;
import com.snnsoluciones.backnathbitpos.entity.Pago;
import com.snnsoluciones.backnathbitpos.enums.EstadoPago;
import com.snnsoluciones.backnathbitpos.repository.PagoRepository;
import com.snnsoluciones.backnathbitpos.dto.cobros.ResumenCobrosDTO;
import com.snnsoluciones.backnathbitpos.service.PagoService;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/pagos")
@RequiredArgsConstructor
@CrossOrigin
public class PagoController {
    
    private final PagoService pagoService;
    private final PagoRepository pagoRepository;
    
    @PostMapping("/registrar")
    @PreAuthorize("hasAnyRole('CAJERO', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse> registrarPago(@Valid @RequestBody RegistrarPagoDTO dto) {
        try {
            Pago pago = pagoService.registrarPago(dto);
            
            // Mapear a DTO de respuesta
            PagoResponseDTO response = new PagoResponseDTO();
            response.setId(pago.getId());
            response.setNumeroRecibo(pago.getNumeroRecibo());
            response.setMonto(pago.getMonto());
            response.setMedioPago(pago.getMedioPago().getDescripcion());
            response.setReferencia(pago.getReferencia());
            response.setFechaPago(pago.getFechaPago());
            response.setClienteNombre(pago.getCliente().getRazonSocial());
            response.setFacturaConsecutivo(pago.getCuentaPorCobrar().getFactura().getConsecutivo());
            response.setCajero(pago.getCajero().getNombre());
            
            // Calcular saldos
            response.setSaldoAnterior(
                pago.getCuentaPorCobrar().getSaldo().add(pago.getMonto())
            );
            response.setSaldoActual(pago.getCuentaPorCobrar().getSaldo());
            
            return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success("Pago registrado exitosamente", response)
            );
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(e.getMessage())
            );
        }
    }

    @GetMapping("/resumen-dia")
    @PreAuthorize("hasAnyRole('CAJERO', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse> resumenDelDia() {
        LocalDateTime inicio = LocalDate.now().atStartOfDay();
        LocalDateTime fin = LocalDate.now().atTime(23, 59, 59);

        // Obtener pagos del día (implementar query en repository)
        List<Pago> pagosDelDia = pagoRepository.findByFechaPagoBetweenAndEstadoOrderByFechaPagoDesc(
            inicio, fin, EstadoPago.APLICADO
        );

        ResumenCobrosDTO resumen = new ResumenCobrosDTO();
        resumen.setTotalCobrado(
            pagosDelDia.stream()
                .map(Pago::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
        );
        resumen.setCantidadPagos(pagosDelDia.size());

        // Agrupar por medio de pago
        Map<String, BigDecimal> porMedio = new HashMap<>();
        for (Pago pago : pagosDelDia) {
            String medio = pago.getMedioPago().getDescripcion();
            porMedio.merge(medio, pago.getMonto(), BigDecimal::add);
        }
        resumen.setPorMedioPago(porMedio);

        return ResponseEntity.ok(ApiResponse.success("Resumen de cobros", resumen));
    }
}