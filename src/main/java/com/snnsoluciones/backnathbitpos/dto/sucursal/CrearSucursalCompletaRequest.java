package com.snnsoluciones.backnathbitpos.dto.sucursal;

import com.snnsoluciones.backnathbitpos.dto.terminal.TerminalRequest;
import com.snnsoluciones.backnathbitpos.enums.ModoFacturacion;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrearSucursalCompletaRequest {

    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

    private String telefono;
    
    @Email(message = "Email inválido")
    private String email;
    
    @NotNull(message = "La empresa es obligatoria")
    private Long empresaId;

    // Ubicación
    private Integer provinciaId;
    private Integer cantonId;
    private Integer distritoId;
    private Integer barrioId;
    private String otrasSenas;

    // Facturación
    @NotNull(message = "El modo de facturación es obligatorio")
    private ModoFacturacion modoFacturacion;
    
    private Boolean activa = true;
    
    private String numeroSucursal;

    // Terminales
    private List<TerminalRequest> terminales;
}