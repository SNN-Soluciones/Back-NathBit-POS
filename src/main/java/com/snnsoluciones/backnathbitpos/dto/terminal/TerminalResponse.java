// src/main/java/com/snnsoluciones/backnathbitpos/dto/terminal/TerminalResponse.java
package com.snnsoluciones.backnathbitpos.dto.terminal;

import com.snnsoluciones.backnathbitpos.enums.TipoImpresion;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TerminalResponse {
    
    private Long id;
    private String numeroTerminal;
    private String nombre;
    private String descripcion;
    private Boolean activa;
    private Boolean imprimirAutomatico;
    
    // Información de la sucursal
    private Long sucursalId;
    private String sucursalNombre;
    private TipoImpresion tipoImpresion;

    // Estado de sesión
    private Boolean tieneSesionActiva;
    private String usuarioSesion;
    private LocalDateTime fechaAperturaSesion;
    
    // Consecutivos
    private Long consecutivoFacturaElectronica;
    private Long consecutivoTiqueteElectronico;
    private Long consecutivoNotaCredito;
    private Long consecutivoNotaDebito;
    private Long consecutivoFacturaCompra;
    private Long consecutivoFacturaExportacion;
    private Long consecutivoReciboPago;
    private Long consecutivoTiqueteInterno;
    private Long consecutivoFacturaInterna;
    private Long consecutivoProforma;
    private Long consecutivoOrdenPedido;
}