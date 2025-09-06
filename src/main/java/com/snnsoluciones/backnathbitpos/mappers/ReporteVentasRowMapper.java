package com.snnsoluciones.backnathbitpos.mappers;

import com.snnsoluciones.backnathbitpos.dto.reporte.ReporteVentasLineaDTO;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;

/**
 * RowMapper para convertir resultados SQL a ReporteVentasLineaDTO
 * Necesario porque @Query con queries nativas no mapea automáticamente a DTOs
 */
@Component
public class ReporteVentasRowMapper implements RowMapper<ReporteVentasLineaDTO> {
    
    @Override
    public ReporteVentasLineaDTO mapRow(ResultSet rs, int rowNum) throws SQLException {
        return ReporteVentasLineaDTO.builder()
            .clave(rs.getString("clave"))
            .consecutivo(rs.getString("consecutivo"))
            .tipoDocumento(rs.getString("tipoDocumento"))
            .fechaEmision(rs.getObject("fechaEmision", LocalDateTime.class))
            .actividadEconomicaCodigo(rs.getString("actividadEconomicaCodigo"))
            .actividadEconomicaDescripcion(rs.getString("actividadEconomicaDescripcion"))
            .clienteNombre(rs.getString("clienteNombre"))
            .clienteIdentificacion(rs.getString("clienteIdentificacion"))
            .clienteTipoIdentificacion(rs.getString("clienteTipoIdentificacion"))
            .totalMercanciasGravadas(getBigDecimalSafe(rs, "totalMercanciasGravadas"))
            .totalMercanciasExentas(getBigDecimalSafe(rs, "totalMercanciasExentas"))
            .totalMercanciasExoneradas(getBigDecimalSafe(rs, "totalMercanciasExoneradas"))
            .totalServiciosGravados(getBigDecimalSafe(rs, "totalServiciosGravados"))
            .totalServiciosExentos(getBigDecimalSafe(rs, "totalServiciosExentos"))
            .totalServiciosExonerados(getBigDecimalSafe(rs, "totalServiciosExonerados"))
            .totalVentaNeta(getBigDecimalSafe(rs, "totalVentaNeta"))
            .totalImpuesto(getBigDecimalSafe(rs, "totalImpuesto"))
            .totalDescuentos(getBigDecimalSafe(rs, "totalDescuentos"))
            .montoTotalExonerado(getBigDecimalSafe(rs, "montoTotalExonerado"))
            .totalOtrosCargos(getBigDecimalSafe(rs, "totalOtrosCargos"))
            .totalComprobante(getBigDecimalSafe(rs, "totalComprobante"))
            .moneda(rs.getString("moneda"))
            .tipoCambio(getBigDecimalSafe(rs, "tipoCambio"))
            .estado(rs.getString("estado"))
            .build();
    }
    
    /**
     * Helper para obtener BigDecimal de forma segura
     * Si el valor es NULL en la BD, retorna BigDecimal.ZERO
     */
    private BigDecimal getBigDecimalSafe(ResultSet rs, String columnName) throws SQLException {
        BigDecimal value = rs.getBigDecimal(columnName);
        return value != null ? value : BigDecimal.ZERO;
    }
}