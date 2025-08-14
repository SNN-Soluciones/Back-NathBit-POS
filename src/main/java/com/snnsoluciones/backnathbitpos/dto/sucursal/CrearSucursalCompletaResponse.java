package com.snnsoluciones.backnathbitpos.dto.sucursal;

import com.snnsoluciones.backnathbitpos.dto.terminal.TerminalResponse;
import com.snnsoluciones.backnathbitpos.enums.ModoFacturacion;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrearSucursalCompletaResponse {

    private Long sucursalId;
    private String nombre;
    private String numeroSucursal;
    private Long empresaId;
    private String empresaNombre;
    private ModoFacturacion modoFacturacion;
    private Boolean activa;
    
    // Resumen de terminales creadas
    private Integer terminalesCreadas;
    private List<TerminalResponse> terminales;
    
    // Metadata
    private String mensaje;
    private LocalDateTime createdAt;
}