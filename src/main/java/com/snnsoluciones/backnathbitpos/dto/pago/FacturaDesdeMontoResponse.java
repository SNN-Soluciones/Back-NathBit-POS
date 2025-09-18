package com.snnsoluciones.backnathbitpos.dto.pago;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class FacturaDesdeMontoResponse {
    private List<DocumentoGenerado> documentos;
    private Resumen resumen;

    @Data
    public static class DocumentoGenerado {
        private String clave;
        private BigDecimal total;
        private String estado;
    }

    @Data
    public static class Resumen {
        private BigDecimal totalFacturado;
        private BigDecimal diferencia;
    }
}