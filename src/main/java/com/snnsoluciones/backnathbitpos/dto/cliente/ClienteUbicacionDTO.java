package com.snnsoluciones.backnathbitpos.dto.cliente;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClienteUbicacionDTO {
  // Campos simples alineados con tu UI actual
  private Integer provincia;
  private Integer canton;
  private Integer distrito;
  private Integer barrio;
  private String otrasSenas;
}