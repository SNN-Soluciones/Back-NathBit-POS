package com.snnsoluciones.backnathbitpos.dto.cliente;

import com.snnsoluciones.backnathbitpos.enums.mh.TipoDocumentoExoneracion;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClienteExoneracionDTO {
    private Long id;
    private Long clienteId;
    private String clienteNombre;
    private TipoDocumentoExoneracion tipoDocumento;
    private String numeroDocumento;
    private String nombreInstitucion;
    private LocalDate fechaEmision;
    private LocalDate fechaVencimiento;
    private BigDecimal porcentajeExoneracion;
    private String categoriaCompra;
    private BigDecimal montoMaximo;
    private Boolean activo;
    private Boolean vigente; // Calculado
    private Integer diasParaVencer; // Calculado
    private String observaciones;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}