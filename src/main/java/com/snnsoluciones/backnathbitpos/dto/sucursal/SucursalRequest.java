package com.snnsoluciones.backnathbitpos.dto.sucursal;

import com.snnsoluciones.backnathbitpos.dto.terminal.TerminalRequest;
import com.snnsoluciones.backnathbitpos.enums.ModoFacturacion;
import com.snnsoluciones.backnathbitpos.enums.ModoImpresion;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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

    @NotNull(message = "El modo de impresión es obligatorio")
    private ModoImpresion modoImpresion = ModoImpresion.LOCAL;

    @Size(max = 100, message = "La IP del orquestador no puede exceder 100 caracteres")
    private String ipOrquestador;

    private Boolean impresionAutomatica = false;
    private Boolean autoImprimirFactura = true;
    private Boolean autoImprimirComanda = false;
    private Integer tiempoAutoClose = 2;

    private List<TerminalRequest> terminales;
}