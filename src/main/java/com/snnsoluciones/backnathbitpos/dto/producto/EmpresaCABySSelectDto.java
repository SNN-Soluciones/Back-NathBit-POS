package com.snnsoluciones.backnathbitpos.dto.producto;

import com.snnsoluciones.backnathbitpos.entity.CodigoCAByS;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// DTO simplificado para selects
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmpresaCABySSelectDto {
    private Long id;
    private String codigo;
    private CodigoCABySDto codigoCabys;
    private Boolean activo;
}