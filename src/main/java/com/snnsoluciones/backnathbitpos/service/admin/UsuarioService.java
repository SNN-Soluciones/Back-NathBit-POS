package com.snnsoluciones.backnathbitpos.service.admin;

import com.snnsoluciones.backnathbitpos.dto.request.UsuarioCreateRequest;
import com.snnsoluciones.backnathbitpos.dto.request.UsuarioUpdateRequest;
import com.snnsoluciones.backnathbitpos.dto.request.CambioPasswordRequest;
import com.snnsoluciones.backnathbitpos.dto.response.UsuarioResponse;
import com.snnsoluciones.backnathbitpos.entity.security.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface UsuarioService {
    UsuarioResponse crear(UsuarioCreateRequest request);
    UsuarioResponse actualizar(UUID id, UsuarioUpdateRequest request);
    void cambiarPassword(UUID id, CambioPasswordRequest request);
    void bloquearUsuario(UUID id);
    void desbloquearUsuario(UUID id);
    void resetearIntentosFallidos(String email);
    void manejarLoginExitoso(String email);
    void manejarLoginFallido(String email);
    UsuarioResponse obtenerPorId(UUID id);
    Page<UsuarioResponse> listar(Pageable pageable);
    boolean existeEmail(String email);
    void eliminar(UUID id);
}