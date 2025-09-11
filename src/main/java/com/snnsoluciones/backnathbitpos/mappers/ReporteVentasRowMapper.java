package com.snnsoluciones.backnathbitpos.mappers;

import com.snnsoluciones.backnathbitpos.dto.reporte.ReporteVentasLineaDTO;
import com.snnsoluciones.backnathbitpos.enums.mh.EstadoBitacora;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

@Component
public class ReporteVentasRowMapper implements RowMapper<ReporteVentasLineaDTO> {

    @Override
    public ReporteVentasLineaDTO mapRow(ResultSet rs, int rowNum) throws SQLException {
        // fecha_emision es VARCHAR ISO con offset: 2025-09-02T11:07:52.98118523-06:00
        String fechaEmisionStr = rs.getString("fechaEmision");
        LocalDateTime fechaEmision = null;
        if (fechaEmisionStr != null && !fechaEmisionStr.isBlank()) {
            fechaEmision = OffsetDateTime.parse(fechaEmisionStr).toLocalDateTime();
        }

        return ReporteVentasLineaDTO.builder()
            .clave(rs.getString("clave"))
            .consecutivo(rs.getString("consecutivo"))
            .tipoDocumento(rs.getString("tipoDocumento"))
            .fechaEmision(fechaEmision)
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
            .estado(EstadoBitacora.ACEPTADA.toString())
            .build();
    }

    private BigDecimal getBigDecimalSafe(ResultSet rs, String column) throws SQLException {
        BigDecimal v = rs.getBigDecimal(column);
        return v != null ? v : BigDecimal.ZERO;
    }
}