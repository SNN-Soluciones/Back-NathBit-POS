package com.snnsoluciones.backnathbitpos.dto.sucursal;

import com.snnsoluciones.backnathbitpos.dto.terminal.TerminalRequest;
import com.snnsoluciones.backnathbitpos.enums.ModoFacturacion;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Data;

@Data
public class SucursalRequest {

    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

    private String telefono;
    
    @Email(message = "Email inválido")
    private String email;
    
    @NotNull(message = "La empresa es obligatoria")
    private Long empresaId;

    private Integer provinciaId;

    private Integer cantonId;

    private Integer distritoId;

    private Integer barrioId;

    private String otrasSenas;

    private ModoFacturacion modoFacturacion;
    
    private Boolean activa = true;

    private String numeroSucursal;

    private List<TerminalRequest> terminales;
}