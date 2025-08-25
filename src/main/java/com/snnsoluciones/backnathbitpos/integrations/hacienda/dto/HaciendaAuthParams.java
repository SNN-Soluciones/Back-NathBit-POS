package com.snnsoluciones.backnathbitpos.integrations.hacienda.dto;

import lombok.Builder;
import lombok.Value;

/** Parametría por EMPRESA para autenticación OIDC (IdP DGT). */
@Value
@Builder
public class HaciendaAuthParams {
    Long empresaId;                // para cachear el token por empresa
    boolean sandbox;               // true: SANDBOX, false: PRODUCCION
    String clientId;               // api-stag | api-prod
    String username;               // usuario ATV (cpf/cpj/cf...)
    String password;               // contraseña de API CE
}