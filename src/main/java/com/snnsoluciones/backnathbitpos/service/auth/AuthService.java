package com.snnsoluciones.backnathbitpos.service.auth;

import com.snnsoluciones.backnathbitpos.dto.request.LoginRequest;
import com.snnsoluciones.backnathbitpos.dto.response.LoginResponse;

public interface AuthService {

  LoginResponse login(LoginRequest loginRequest);

  LoginResponse refreshToken(String refreshToken);

  void logout(String token);
}