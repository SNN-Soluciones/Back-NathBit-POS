package com.snnsoluciones.backnathbitpos.service.auth;

import com.snnsoluciones.backnathbitpos.dto.auth.*;

public interface AuthService {
    
    LoginResponse login(LoginRequest request);
    
    TokenResponse establecerContexto(ContextoRequest request);
    
    TokenResponse refresh(String refreshToken);
}