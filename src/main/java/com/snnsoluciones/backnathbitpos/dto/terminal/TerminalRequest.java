package com.snnsoluciones.backnathbitpos.dto.terminal;

import com.snnsoluciones.backnathbitpos.enums.TipoImpresion;
import jakarta.persistence.Column;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TerminalRequest {
    
    @NotBlank(message = "El número de terminal es requerido")
    @Pattern(regexp = "^[0-9]{5}$", message = "El número de terminal debe tener 5 dígitos")
    private String numeroTerminal;
    
    @NotBlank(message = "El nombre es requerido")
    @Size(max = 100, message = "El nombre no puede exceder 100 caracteres")
    private String nombre;
    
    @Size(max = 255, message = "La descripción no puede exceder 255 caracteres")
    private String descripcion;
    
    @NotNull(message = "El estado activa es requerido")
    private Boolean activa = true;
    
    private Boolean imprimirAutomatico = false;

    private TipoImpresion tipoImpresion = TipoImpresion.TICKET;
    
    // Para crear terminal en una sucursal específica
    private Long sucursalId;
    private Long consecutivoOrdenPedido = 0L;
    private Long consecutivoFacturaElectronica = 0L;
    private Long consecutivoTiqueteElectronico = 0L;
    private Long consecutivoNotaCredito = 0L;
    private Long consecutivoNotaDebito = 0L;
    private Long consecutivoFacturaCompra = 0L;
    private Long consecutivoFacturaExportacion = 0L;
    private Long consecutivoReciboPago = 0L;
    private Long consecutivoTiqueteInterno = 0L;
    private Long consecutivoFacturaInterna = 0L;
    private Long consecutivoProforma = 0L;
}