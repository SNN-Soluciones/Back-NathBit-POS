package com.snnsoluciones.backnathbitpos.dto.sistema;

import com.snnsoluciones.backnathbitpos.enums.EstadoPago;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmpresaSistemaDTO {
    private Long id;
    private String codigo;
    private String nombre;
    private String nombreComercial;
    private String cedulaJuridica;
    private EstadoPago estadoPago;
    private LocalDateTime fechaUltimoPago;
    private Double montoAdeudado;
    private Integer diasMora;
    private Boolean activa;
    private Integer totalSucursales;
    private Integer totalUsuarios;
    private LocalDateTime createdAt;
}
