package com.snnsoluciones.backnathbitpos.dto.auth;

import com.snnsoluciones.backnathbitpos.dto.usuarios.UsuarioResponse;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class LoginResponse {
    private String token;
    private String refreshToken;  // AÑADIDO: Faltaba este campo
    private UsuarioResponse usuario;
    private boolean requiereSeleccion;
    private String rutaDestino;

    // Campos opcionales según rol
    private List<EmpresaResumen> empresas;  // Para SUPER_ADMIN
    private EmpresaResumen empresa;         // Para ADMIN
    private List<SucursalResumen> sucursales; // Para ADMIN
    private Contexto contexto;              // Para OPERATIVOS
}