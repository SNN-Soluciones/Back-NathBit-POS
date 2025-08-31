package com.snnsoluciones.backnathbitpos.dto.cliente;

import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClienteEmailDTO {
    private Long id;
    private String email;
    private Boolean esPrincipal;
    private LocalDateTime ultimoUso;
    private Integer vecesUsado;
    private LocalDateTime createdAt;
    
    // Para mostrar en UI
    public String getDisplayText() {
        if (vecesUsado > 0) {
            return String.format("%s (usado %d veces)", email, vecesUsado);
        }
        return email;
    }
}