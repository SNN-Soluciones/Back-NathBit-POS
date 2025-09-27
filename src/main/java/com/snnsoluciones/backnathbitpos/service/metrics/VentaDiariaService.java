package com.snnsoluciones.backnathbitpos.service.metrics;

import com.snnsoluciones.backnathbitpos.dto.metrics.VentaDiariaDTO;
import com.snnsoluciones.backnathbitpos.entity.VentaDiaria;
import com.snnsoluciones.backnathbitpos.repository.VentaDiariaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true) // Solo lectura!
public class VentaDiariaService {

    private final VentaDiariaRepository ventaDiariaRepository;

    /**
     * Obtener ventas del día para empresa (consolidado)
     */
    public VentaDiariaDTO obtenerVentasHoyEmpresa(Long empresaId) {
        LocalDate hoy = LocalDate.now();
        
        return ventaDiariaRepository.findVentasHoyEmpresa(empresaId, hoy)
                .map(this::toDTO)
                .orElse(crearDTOVacio(hoy));
    }

    /**
     * Obtener ventas del día para sucursal
     */
    public VentaDiariaDTO obtenerVentasHoySucursal(Long sucursalId) {
        LocalDate hoy = LocalDate.now();
        
        return ventaDiariaRepository.findVentasHoySucursal(sucursalId, hoy)
                .map(this::toDTO)
                .orElse(crearDTOVacio(hoy));
    }

    /**
     * Convertir entidad a DTO
     */
    private VentaDiariaDTO toDTO(VentaDiaria venta) {
        return VentaDiariaDTO.builder()
                .fecha(venta.getFecha())
                .ventasMh(venta.getVentasMh())
                .ventasInternas(venta.getVentasInternas())
                .ventasTotales(venta.getVentasTotales())
                .impuestoTotal(venta.getImpuestoTotal())
                .descuentosTotal(venta.getDescuentosTotal())
                .cantidadMh(venta.getCantidadMh())
                .cantidadInternas(venta.getCantidadInternas())
                .cantidadTotal(venta.getCantidadTotal())
                .build();
    }

    /**
     * Crear DTO vacío para cuando no hay ventas
     */
    private VentaDiariaDTO crearDTOVacio(LocalDate fecha) {
        return VentaDiariaDTO.builder()
                .fecha(fecha)
                .ventasMh(BigDecimal.ZERO)
                .ventasInternas(BigDecimal.ZERO)
                .ventasTotales(BigDecimal.ZERO)
                .impuestoTotal(BigDecimal.ZERO)
                .descuentosTotal(BigDecimal.ZERO)
                .cantidadMh(0)
                .cantidadInternas(0)
                .cantidadTotal(0)
                .build();
    }
}