package com.snnsoluciones.backnathbitpos.dto.empresa;

import com.snnsoluciones.backnathbitpos.enums.mh.RegimenTributario;
import com.snnsoluciones.backnathbitpos.enums.mh.TipoIdentificacion;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class EmpresaResponse {
    private Long id;
    private String nombreComercial;
    private String nombreRazonSocial;
    private TipoIdentificacion tipoIdentificacion;
    private String identificacion;
    private String telefono;
    private String email;
    private Boolean activa;
    private Boolean requiereHacienda;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean productosPorSucursal;
    private Boolean clientesPorSucursal;
    private Boolean categoriasPorSucursal;
    private Boolean proveedoresPorSucursal;
    private Boolean inventarioPorSucursal;
    private RegimenTributario regimenTributario;
}