package com.snnsoluciones.backnathbitpos.dto.reporte;

import com.snnsoluciones.backnathbitpos.dto.reporte.ReporteIvaComprasLineaDTO;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;

/**
 * RowMapper para el reporte de IVA por tarifa en compras.
 *
 * A diferencia de las ventas, fecha_emision en facturas_recepcion es un
 * TIMESTAMP real — no VARCHAR — así que se lee directamente con getTimestamp().
 */
@Component
public class ReporteIvaComprasRowMapper implements RowMapper<ReporteIvaComprasLineaDTO> {

    @Override
    public ReporteIvaComprasLineaDTO mapRow(ResultSet rs, int rowNum) throws SQLException {

        // fecha_emision es TIMESTAMP real en facturas_recepcion
        LocalDateTime fechaEmision = null;
        Timestamp ts = rs.getTimestamp("fecha_emision");
        if (ts != null) {
            fechaEmision = ts.toLocalDateTime();
        }

        return ReporteIvaComprasLineaDTO.builder()
            // Identificación
            .tipoDocumento(rs.getString("tipo_documento"))
            .clave(rs.getString("clave"))
            .consecutivo(rs.getString("consecutivo"))
            // Proveedor
            .proveedorNombre(rs.getString("proveedor_nombre"))
            .proveedorIdentificacion(rs.getString("proveedor_identificacion"))
            // Fecha
            .fechaEmision(fechaEmision)
            // IVA por tarifa (calculado con SUM CASE en la query)
            .iva0(decimal(rs, "iva_0"))
            .iva1(decimal(rs, "iva_1"))
            .iva2(decimal(rs, "iva_2"))
            .iva4(decimal(rs, "iva_4"))
            .iva8(decimal(rs, "iva_8"))
            .iva13(decimal(rs, "iva_13"))
            .otrosImpuestos(decimal(rs, "otros_impuestos"))
            // Totales directos de facturas_recepcion
            .totalGravado(decimal(rs, "total_gravado"))
            .totalExento(decimal(rs, "total_exento"))
            .totalExonerado(decimal(rs, "total_exonerado"))
            .totalVentaNeta(decimal(rs, "total_venta_neta"))
            .totalImpuesto(decimal(rs, "total_impuesto"))
            .totalDescuentos(decimal(rs, "total_descuentos"))
            .totalOtrosCargos(decimal(rs, "total_otros_cargos"))
            .totalComprobante(decimal(rs, "total_comprobante"))
            .build();
    }

    private BigDecimal decimal(ResultSet rs, String col) throws SQLException {
        BigDecimal v = rs.getBigDecimal(col);
        return v != null ? v : BigDecimal.ZERO;
    }
}