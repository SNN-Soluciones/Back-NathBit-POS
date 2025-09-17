package com.snnsoluciones.backnathbitpos.controller;

import com.snnsoluciones.backnathbitpos.dto.common.ApiResponse;
import com.snnsoluciones.backnathbitpos.dto.cxc.CuentaPorCobrarDTO;
import com.snnsoluciones.backnathbitpos.entity.CuentaPorCobrar;
import com.snnsoluciones.backnathbitpos.enums.EstadoCuenta;
import com.snnsoluciones.backnathbitpos.repository.ClienteRepository;
import com.snnsoluciones.backnathbitpos.repository.CuentaPorCobrarRepository;
import com.snnsoluciones.backnathbitpos.service.CuentaPorCobrarService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cuentas-por-cobrar")
@RequiredArgsConstructor
@CrossOrigin
public class CuentaPorCobrarController {
    
    private final CuentaPorCobrarRepository repository;
    private final CuentaPorCobrarService cuentaPorCobrarService;
    private final ClienteRepository clienteRepository;
    private final ModelMapper modelMapper;
    
    @GetMapping("/cliente/{clienteId}")
    @PreAuthorize("hasAnyRole('CAJERO', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse> obtenerPorCliente(
            @PathVariable Long clienteId,
            @RequestParam(required = false) EstadoCuenta estado,
            @PageableDefault(size = 20) Pageable pageable) {
        
        Page<CuentaPorCobrar> cuentas;
        
        if (estado != null) {
            cuentas = repository.findByClienteIdAndEstado(clienteId, estado, pageable);
        } else {
            cuentas = repository.findAll(pageable); // Temporal, mejorar query
        }
        
        Page<CuentaPorCobrarDTO> dtos = cuentas.map(cuenta -> {
            CuentaPorCobrarDTO dto = modelMapper.map(cuenta, CuentaPorCobrarDTO.class);
            dto.setClienteNombre(cuenta.getCliente().getRazonSocial());
            dto.setNumeroFactura(cuenta.getFactura().getConsecutivo());
            return dto;
        });
        
        return ResponseEntity.ok(ApiResponse.success(
            "Cuentas obtenidas", 
            dtos
        ));
    }

    @GetMapping("/resumen/{empresaId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'ROOT', 'SOPORTE')")
    public ResponseEntity<ApiResponse> obtenerResumen(
        @PathVariable long empresaId
    ) {
        Map<String, Object> resumen = new HashMap<>();

        // Total por cobrar
        BigDecimal totalPorCobrar = repository
            .findAll()
            .stream()
            .filter(c -> c.getEstado() != EstadoCuenta.PAGADA)
            .map(CuentaPorCobrar::getSaldo)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Cuentas vencidas
        long cuentasVencidas = repository
            .findVencidas(LocalDate.now())
            .size();

        resumen.put("totalPorCobrar", totalPorCobrar);
        resumen.put("cuentasVencidas", cuentasVencidas);
        resumen.put("clientesBloqueados", clienteRepository.countByEmpresaIdAndBloqueadoPorMora(empresaId,true));

        return ResponseEntity.ok(ApiResponse.success("Resumen cuentas por cobrar", resumen));
    }

    @GetMapping("/listar/{empresaId}")
    @PreAuthorize("hasAnyRole('ROOT', 'SUPER_ADMIN', 'ADMIN', 'CAJERO')")
    public ResponseEntity<ApiResponse<Page<CuentaPorCobrarDTO>>> listarCuentasPorEmpresa(
        @PathVariable Long empresaId,
        @RequestParam(required = false) String busqueda,
        @RequestParam(required = false) String estado,
        @RequestParam(required = false, defaultValue = "false") boolean soloVencidas,
        @RequestParam(required = false) Long clienteId,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
        Pageable pageable) {

        try {
            // Validar que el usuario tenga acceso a la empresa
            Page<CuentaPorCobrarDTO> cuentas = cuentaPorCobrarService.listarPorEmpresa(
                empresaId,
                busqueda,
                estado,
                soloVencidas,
                clienteId,
                fechaDesde,
                fechaHasta,
                pageable
            );

            return ResponseEntity.ok(ApiResponse.success(
                "Cuentas por cobrar obtenidas exitosamente",
                cuentas
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Error al obtener cuentas por cobrar: " + e.getMessage()));
        }
    }
}