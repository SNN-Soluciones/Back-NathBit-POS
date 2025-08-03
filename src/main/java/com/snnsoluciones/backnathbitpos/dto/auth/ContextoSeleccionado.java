package com.snnsoluciones.backnathbitpos.dto.auth;

import java.util.UUID;
import lombok.*;

@Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public class ContextoSeleccionado {
        private UUID empresaId;
        private String empresaNombre;
        private UUID sucursalId;
        private String sucursalNombre;
        private String schemaName; // tenant_id
        private String rol;
        private boolean esPropietario;
    }