// JobErrorResponse.java
package com.snnsoluciones.backnathbitpos.dto.pago;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobErrorResponse {
    private String error;
    private String mensaje;
    private LocalDateTime timestamp;
    private String path;
}