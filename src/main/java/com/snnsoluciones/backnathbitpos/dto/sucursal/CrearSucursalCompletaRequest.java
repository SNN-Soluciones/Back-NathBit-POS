package com.snnsoluciones.backnathbitpos.dto.sucursal;

import com.snnsoluciones.backnathbitpos.dto.terminal.TerminalRequest;
import com.snnsoluciones.backnathbitpos.enums.ModoFacturacion;
import com.snnsoluciones.backnathbitpos.enums.ModoImpresion;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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

    @Builder.Default
    private Boolean activa = true;
    
    private String numeroSucursal;

    @Builder.Default
    @NotNull(message = "El modo de impresión es obligatorio")
    private ModoImpresion modoImpresion = ModoImpresion.LOCAL;

    @Size(max = 100, message = "La IP del orquestador no puede exceder 100 caracteres")
    private String ipOrquestador;

    // Terminales
    private List<TerminalRequest> terminales;
}