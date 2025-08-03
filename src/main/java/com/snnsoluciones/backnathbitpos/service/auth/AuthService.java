package com.snnsoluciones.backnathbitpos.service.auth;

import com.snnsoluciones.backnathbitpos.dto.request.LoginRequest;
import com.snnsoluciones.backnathbitpos.dto.request.RefreshTokenRequest;
import com.snnsoluciones.backnathbitpos.dto.request.TenantSelectionRequest;
import com.snnsoluciones.backnathbitpos.dto.response.LoginResponse;
import com.snnsoluciones.backnathbitpos.dto.response.RefreshTokenResponse;
import com.snnsoluciones.backnathbitpos.dto.response.TenantSelectionResponse;

public interface AuthService {

  LoginResponse login(LoginRequest loginRequest, String ipAddress);

  TenantSelectionResponse selectTenant(TenantSelectionRequest request);

  RefreshTokenResponse refreshToken(RefreshTokenRequest refreshToken);

  void logout(String token);
}