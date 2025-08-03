package com.snnsoluciones.backnathbitpos.service.admin.impl;

import com.snnsoluciones.backnathbitpos.dto.request.UsuarioCreateRequest;
import com.snnsoluciones.backnathbitpos.dto.response.UsuarioResponse;
import com.snnsoluciones.backnathbitpos.mapper.UsuarioMapper;
import com.snnsoluciones.backnathbitpos.repository.RolRepository;
import com.snnsoluciones.backnathbitpos.repository.UsuarioRepository;
import com.snnsoluciones.backnathbitpos.service.admin.UsuarioService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UsuarioServiceImpl implements UsuarioService {
    
    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;
    private final UsuarioMapper usuarioMapper;
    
    @Value("${app.security.max-intentos-login:3}")
    private int maxIntentosLogin;
    
    @Override
    public UsuarioResponse crear(UsuarioCreateRequest request) {
        // 1. Verificar que el email no exista
        // 2. Validar contraseña segura
        // 3. Encriptar contraseña
        // 4. Asignar roles
        // 5. Guardar usuario
        // 6. Retornar response
    }
    
    @Override
    public void manejarLoginFallido(String email) {
        // 1. Buscar usuario
        // 2. Incrementar intentos fallidos
        // 3. Si supera el máximo, bloquear cuenta
        // 4. Registrar en auditoría
    }
    
    // ... implementar otros métodos
}