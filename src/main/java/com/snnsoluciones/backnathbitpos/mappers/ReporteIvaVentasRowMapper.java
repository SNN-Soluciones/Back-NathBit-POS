package com.snnsoluciones.backnathbitpos.mappers;

import com.snnsoluciones.backnathbitpos.dto.reporte.ReporteIvaVentasLineaDTO;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

/**
 * RowMapper para el reporte de IVA por tarifa en ventas.
 *
 * <p>Convierte cada fila del {@link ResultSet} al DTO {@link ReporteIvaVentasLineaDTO}.
 * El campo {@code fecha_emision} se almacena como VARCHAR ISO 8601 con offset
 * (ej. {@code 2026-02-15T10:30:00.123456-06:00}), por lo que se parsea con
 * {@link OffsetDateTime} antes de convertir a {@link LocalDateTime}.</p>
 */
@Component
public class ReporteIvaVentasRowMapper implements RowMapper<ReporteIvaVentasLineaDTO> {

    @Override
    public ReporteIvaVentasLineaDTO mapRow(ResultSet rs, int rowNum) throws SQLException {

        // fecha_emision viene como VARCHAR ISO con offset de zona horaria
        String fechaStr = rs.getString("fecha_emision");
        LocalDateTime fechaEmision = null;
        if (fechaStr != null && !fechaStr.isBlank()) {
            try {
                fechaEmision = OffsetDateTime.parse(fechaStr).toLocalDateTime();
            } catch (Exception ex) {
                // Fallback: intentar parsear sin offset
                fechaEmision = LocalDateTime.parse(fechaStr.substring(0, 19));
            }
        }

        return ReporteIvaVentasLineaDTO.builder()
            // Identificación
            .tipoDocumento(rs.getString("tipo_documento"))
            .clave(rs.getString("clave"))
            .consecutivo(rs.getString("consecutivo"))
            // Cliente
            .clienteNombre(rs.getString("cliente_nombre"))
            .clienteIdentificacion(rs.getString("cliente_identificacion"))
            // Fecha
            .fechaEmision(fechaEmision)
            // IVA por tarifa
            .iva0(getDecimal(rs, "iva_0"))
            .iva1(getDecimal(rs, "iva_1"))
            .iva2(getDecimal(rs, "iva_2"))
            .iva4(getDecimal(rs, "iva_4"))
            .iva8(getDecimal(rs, "iva_8"))
            .iva13(getDecimal(rs, "iva_13"))
            // Totales
            .totalNeto(getDecimal(rs, "total_neto"))
            .totalImpuestos(getDecimal(rs, "total_impuestos"))
            .descuentos(getDecimal(rs, "descuentos"))
            .total(getDecimal(rs, "total"))
            .build();
    }

    /**
     * Retorna {@link BigDecimal#ZERO} si la columna es SQL NULL para evitar NPE en sumas.
     */
    private BigDecimal getDecimal(ResultSet rs, String column) throws SQLException {
        BigDecimal value = rs.getBigDecimal(column);
        return value != null ? value : BigDecimal.ZERO;
    }
}