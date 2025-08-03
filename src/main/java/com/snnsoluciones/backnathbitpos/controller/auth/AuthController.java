package com.snnsoluciones.backnathbitpos.controller.auth;

import com.snnsoluciones.backnathbitpos.dto.request.LoginRequest;
import com.snnsoluciones.backnathbitpos.dto.response.LoginResponse;
import com.snnsoluciones.backnathbitpos.service.auth.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Autenticación", description = "Endpoints para autenticación y autorización")
public class AuthController {

  private final AuthService authService;

  @PostMapping("/login")
  @Operation(summary = "Login de usuario", description = "Autentica un usuario y devuelve los tokens de acceso")
  public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
    log.info("Login request para: {}", loginRequest.getEmail());
    LoginResponse response = authService.login(loginRequest);
    return ResponseEntity.ok(response);
  }

  @PostMapping("/refresh")
  @Operation(summary = "Refrescar token", description = "Genera un nuevo access token usando el refresh token")
  public ResponseEntity<LoginResponse> refreshToken(@RequestParam String refreshToken) {
    LoginResponse response = authService.refreshToken(refreshToken);
    return ResponseEntity.ok(response);
  }

  @PostMapping("/logout")
  @Operation(summary = "Logout", description = "Cierra la sesión del usuario")
  public ResponseEntity<String> logout(@RequestHeader("Authorization") String token) {
    authService.logout(token);
    return ResponseEntity.ok("Logout exitoso");
  }
}