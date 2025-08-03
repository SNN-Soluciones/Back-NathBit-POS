package com.snnsoluciones.backnathbitpos.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

  @JsonProperty("access_token")
  private String accessToken;

  @JsonProperty("refresh_token")
  private String refreshToken;

  @JsonProperty("token_type")
  @Builder.Default
  private String tokenType = "Bearer";

  @JsonProperty("expires_in")
  private Long expiresIn;

  private String email;

  private String nombre;

  private String apellidos;

  private String roles;

  @JsonProperty("tenant_id")
  private String tenantId;

  private List<TenantInfo> tenants;
  private boolean requiresTenantSelection;
}