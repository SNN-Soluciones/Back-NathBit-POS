package com.snnsoluciones.backnathbitpos.dto.response;

import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ContextoActual {
    private String username;
    private UUID empresaId;
    private UUID sucursalId;
    private String tenantId;
    private List<String> authorities;
}