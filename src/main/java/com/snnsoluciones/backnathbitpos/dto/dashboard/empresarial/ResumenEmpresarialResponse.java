package com.snnsoluciones.backnathbitpos.dto.dashboard.empresarial;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
 
@Data @Builder
public class ResumenEmpresarialResponse {
    private BigDecimal ventasActual;
    private BigDecimal ventasAnterior;
    private BigDecimal porcentajeCambio;
    private String     tendencia;           // "up" | "down" | "stable"
    private BigDecimal ticketPromedio;
    private long       totalFacturas;
    private long       totalEmpresas;
    private long       empresasActivas;
    private long       totalSucursales;
    private long       totalUsuarios;
}