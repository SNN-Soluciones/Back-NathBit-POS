package com.snnsoluciones.backnathbitpos.dto.producto;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Value;

/**
 * DTO for {@link com.snnsoluciones.backnathbitpos.entity.CodigoCAByS}
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CodigoCABySDto implements Serializable {

  String codigo;
  String descripcion;
  String impuestoSugerido;
}