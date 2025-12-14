package com.snnsoluciones.backnathbitpos.service.pdf;

import com.snnsoluciones.backnathbitpos.entity.*;
import com.snnsoluciones.backnathbitpos.repository.FacturaInternaRepository;
import com.snnsoluciones.backnathbitpos.repository.OrdenRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Servicio para mapear datos de facturas internas a comandas de cocina
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ComandaCocinaMapperService {

    private final FacturaInternaRepository facturaInternaRepository;
    private final OrdenRepository ordenRepository;
    private static final DateTimeFormatter FECHA_FORMATO = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /**
     * Mapea una factura interna a parámetros para comanda de cocina
     * @param numeroInterno Número interno de la factura (ej: INT-2024-00001)
     * @return Mapa con todos los parámetros necesarios para el reporte
     */
    @Transactional(readOnly = true)
    public Map<String, Object> mapearComandaCocina(String numeroInterno) {
        log.info("Mapeando comanda cocina para: {}", numeroInterno);

        Map<String, Object> parametros = new HashMap<>();

        // 🔥 Intentar primero como ORDEN, luego como FACTURA
        Optional<Orden> ordenOpt = ordenRepository.findByNumero(numeroInterno);

        if (ordenOpt.isPresent()) {
            // ✅ Es una ORDEN
            Orden orden = ordenOpt.get();
            log.info("✅ Mapeando desde ORDEN: {}", orden.getNumero());

            parametros.put("numero_orden", orden.getNumero());
            parametros.put("mesa", orden.getMesa() != null ? orden.getMesa().getCodigo() : "VENTANILLA");
            parametros.put("numero_viper", null);
            parametros.put("fecha_hora", orden.getFechaCreacion().format(FECHA_FORMATO));
            parametros.put("mesero", orden.getMesero().getNombre() + " " + orden.getMesero().getApellidos());
            parametros.put("sucursal", orden.getSucursal().getNombre());
            parametros.put("observaciones_generales", orden.getObservaciones());

            // Mapear items de la orden
            List<DetalleComandaDTO> detalles = mapearDetallesOrden(orden.getItems());
            JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(detalles);
            parametros.put("datasource_detalles", dataSource);

        } else {
            // Es una FACTURA INTERNA
            FacturaInterna facturaInterna = facturaInternaRepository.findByNumero(numeroInterno)
                .orElseThrow(() -> new RuntimeException("Orden o Factura interna no encontrada: " + numeroInterno));

            log.info("✅ Mapeando desde FACTURA INTERNA: {}", facturaInterna.getNumero());

            parametros.put("numero_orden", facturaInterna.getNumero());
            parametros.put("mesa", obtenerNumeroMesa(facturaInterna));
                parametros.put("numero_viper", facturaInterna.getNumeroViper());
            parametros.put("fecha_hora", facturaInterna.getFecha().format(FECHA_FORMATO));
            parametros.put("mesero", obtenerNombreMesero(facturaInterna));
            parametros.put("sucursal", facturaInterna.getSucursal().getNombre());
            parametros.put("observaciones_generales", facturaInterna.getNotas());

            // Mapear detalles de factura
            List<DetalleComandaDTO> detalles = mapearDetalles(facturaInterna.getDetalles());
            JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(detalles);
            parametros.put("datasource_detalles", dataSource);
        }

        return parametros;
    }

    // 🆕 NUEVO MÉTODO para mapear items de orden
    private List<DetalleComandaDTO> mapearDetallesOrden(List<OrdenItem> items) {
        List<DetalleComandaDTO> dtos = new ArrayList<>();

        if (items != null) {
            for (OrdenItem item : items) {
                DetalleComandaDTO dto = new DetalleComandaDTO();
                dto.setCantidad(item.getCantidad());

                StringBuilder descripcion = new StringBuilder(item.getProducto().getNombre());

                // Agregar notas (que incluyen las opciones de compuestos)
                if (item.getNotas() != null && !item.getNotas().trim().isEmpty()) {
                    String[] lineasNotas = item.getNotas().split("\n");
                    for (String lineaNota : lineasNotas) {
                        if (!lineaNota.trim().isEmpty()) {
                            descripcion.append("\n  ").append(lineaNota.trim());
                        }
                    }
                }

                dto.setDescripcion(descripcion.toString());
                dtos.add(dto);
            }
        }

        return dtos;
    }

    /**
     * Mapea los detalles de la factura interna a DTOs para la comanda
     */
    private List<DetalleComandaDTO> mapearDetalles(List<FacturaInternaDetalle> detalles) {
        List<DetalleComandaDTO> dtos = new ArrayList<>();

        if (detalles != null) {
            for (FacturaInternaDetalle detalle : detalles) {
                DetalleComandaDTO dto = new DetalleComandaDTO();
                dto.setCantidad(detalle.getCantidad());
                
                // Construir descripción con producto y notas
                StringBuilder descripcion = new StringBuilder(detalle.getNombreProducto());
                
                // Si hay notas/observaciones, agregarlas en líneas separadas
                if (detalle.getNotas() != null && !detalle.getNotas().trim().isEmpty()) {
                    // Las notas vienen separadas por saltos de línea
                    String[] lineasNotas = detalle.getNotas().split("\n");
                    for (String lineaNota : lineasNotas) {
                        if (!lineaNota.trim().isEmpty()) {
                            descripcion.append("\n  ").append(lineaNota.trim());
                        }
                    }
                }
                
                dto.setDescripcion(descripcion.toString());
                dtos.add(dto);
            }
        }

        return dtos;
    }

    /**
     * Obtiene el nombre del mesero/cajero
     */
    private String obtenerNombreMesero(FacturaInterna facturaInterna) {
        if (facturaInterna.getCajero() != null) {
            Usuario usuario = facturaInterna.getCajero();
            return usuario.getNombre() + " " + usuario.getApellidos();
        }
        return "Sistema";
    }

    /**
     * Obtiene el número de mesa (por ahora retorna null, luego lo expandiremos)
     */
    private String obtenerNumeroMesa(FacturaInterna facturaInterna) {
        // TODO: Cuando tengamos el campo mesa en la factura
        return null;
    }

    /**
     * DTO para los detalles de la comanda
     */
    public static class DetalleComandaDTO {
        private BigDecimal cantidad;
        private String descripcion;

        // Getters y setters
        public BigDecimal getCantidad() {
            return cantidad;
        }

        public void setCantidad(BigDecimal cantidad) {
            this.cantidad = cantidad;
        }

        public String getDescripcion() {
            return descripcion;
        }

        public void setDescripcion(String descripcion) {
            this.descripcion = descripcion;
        }
    }
}