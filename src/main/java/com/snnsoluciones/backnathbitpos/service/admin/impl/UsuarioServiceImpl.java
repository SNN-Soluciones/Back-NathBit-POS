package com.snnsoluciones.backnathbitpos.service.admin.impl;

import com.snnsoluciones.backnathbitpos.config.tenant.TenantContext;
import com.snnsoluciones.backnathbitpos.dto.request.CambioPasswordRequest;
import com.snnsoluciones.backnathbitpos.dto.request.UsuarioCreateRequest;
import com.snnsoluciones.backnathbitpos.dto.request.UsuarioUpdateRequest;
import com.snnsoluciones.backnathbitpos.dto.response.UsuarioResponse;
import com.snnsoluciones.backnathbitpos.entity.operacion.Caja;
import com.snnsoluciones.backnathbitpos.entity.security.Rol;
import com.snnsoluciones.backnathbitpos.entity.security.Usuario;
import com.snnsoluciones.backnathbitpos.entity.tenant.Sucursal;
import com.snnsoluciones.backnathbitpos.exception.BusinessException;
import com.snnsoluciones.backnathbitpos.exception.ResourceNotFoundException;
import com.snnsoluciones.backnathbitpos.mapper.UsuarioMapper;
import com.snnsoluciones.backnathbitpos.repository.CajaRepository;
import com.snnsoluciones.backnathbitpos.repository.RolRepository;
import com.snnsoluciones.backnathbitpos.repository.SucursalRepository;
import com.snnsoluciones.backnathbitpos.repository.UsuarioRepository;
import com.snnsoluciones.backnathbitpos.service.admin.UsuarioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UsuarioServiceImpl implements UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final SucursalRepository sucursalRepository;
    private final CajaRepository cajaRepository;
    private final PasswordEncoder passwordEncoder;
    private final UsuarioMapper usuarioMapper;

    @Value("${app.security.max-intentos-login:3}")
    private int maxIntentosLogin;

    @Override
    public UsuarioResponse crear(UsuarioCreateRequest request) {
        log.info("Creando nuevo usuario con email: {}", request.getEmail());

        // 1. Verificar que el email no exista para el tenant actual
        String tenantId = TenantContext.getCurrentTenant();
        if (usuarioRepository.existsByEmailAndTenantId(request.getEmail(), tenantId)) {
            throw new BusinessException("Ya existe un usuario con el email: " + request.getEmail());
        }

        // 2. Validar y encriptar contraseña
        validarPasswordSegura(request.getPassword());
        String passwordEncriptado = passwordEncoder.encode(request.getPassword());

        // 3. Crear entidad usuario
        Usuario usuario = usuarioMapper.toEntity(request);
        usuario.setPassword(passwordEncriptado);
        usuario.setTenantId(tenantId);

        // 4. Asignar rol
        if (request.getRolId() != null) {
            Rol rol = rolRepository.findById(request.getRolId())
                .orElseThrow(() -> new ResourceNotFoundException("Rol no encontrado"));
            usuario.setRol(rol);
        }

        // 5. Asignar sucursal predeterminada
        if (request.getSucursalPredeterminadaId() != null) {
            Sucursal sucursal = sucursalRepository.findById(request.getSucursalPredeterminadaId())
                .orElseThrow(() -> new ResourceNotFoundException("Sucursal no encontrada"));
            usuario.setSucursalPredeterminada(sucursal);
        }

        // 6. Guardar usuario
        usuario = usuarioRepository.save(usuario);

        // 7. Asignar sucursales y cajas
        asignarSucursalesYCajas(usuario, request.getSucursalesIds(), request.getCajasIds());

        log.info("Usuario creado exitosamente con ID: {}", usuario.getId());

        return usuarioMapper.toResponse(usuario);
    }

    @Override
    public UsuarioResponse actualizar(UUID id, UsuarioUpdateRequest request) {
        log.info("Actualizando usuario con ID: {}", id);

        Usuario usuario = usuarioRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        // Verificar si el email cambió y no está duplicado
        if (!usuario.getEmail().equals(request.getEmail())) {
            String tenantId = TenantContext.getCurrentTenant();
            if (usuarioRepository.existsByEmailAndTenantId(request.getEmail(), tenantId)) {
                throw new BusinessException("Ya existe un usuario con el email: " + request.getEmail());
            }
        }

        // Actualizar datos básicos
        usuarioMapper.updateEntity(usuario, request);

        // Actualizar rol si cambió
        if (request.getRolId() != null && !request.getRolId().equals(usuario.getRol().getId())) {
            Rol rol = rolRepository.findById(request.getRolId())
                .orElseThrow(() -> new ResourceNotFoundException("Rol no encontrado"));
            usuario.setRol(rol);
        }

        // Actualizar sucursal predeterminada si cambió
        if (request.getSucursalPredeterminadaId() != null) {
            Sucursal sucursal = sucursalRepository.findById(request.getSucursalPredeterminadaId())
                .orElseThrow(() -> new ResourceNotFoundException("Sucursal no encontrada"));
            usuario.setSucursalPredeterminada(sucursal);
        }

        // Actualizar sucursales y cajas
        asignarSucursalesYCajas(usuario, request.getSucursalesIds(), request.getCajasIds());

        usuario = usuarioRepository.save(usuario);

        log.info("Usuario actualizado exitosamente");

        return usuarioMapper.toResponse(usuario);
    }

    @Override
    public void cambiarPassword(UUID id, CambioPasswordRequest request) {
        log.info("Cambiando contraseña para usuario ID: {}", id);

        Usuario usuario = usuarioRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        // Verificar contraseña actual
        if (!passwordEncoder.matches(request.getPasswordActual(), usuario.getPassword())) {
            throw new BusinessException("La contraseña actual es incorrecta");
        }

        // Verificar que la nueva contraseña sea diferente
        if (passwordEncoder.matches(request.getPasswordNueva(), usuario.getPassword())) {
            throw new BusinessException("La nueva contraseña debe ser diferente a la actual");
        }

        // Verificar que las contraseñas coincidan
        if (!request.getPasswordNueva().equals(request.getPasswordConfirmacion())) {
            throw new BusinessException("Las contraseñas no coinciden");
        }

        // Validar y actualizar contraseña
        validarPasswordSegura(request.getPasswordNueva());
        usuario.setPassword(passwordEncoder.encode(request.getPasswordNueva()));

        usuarioRepository.save(usuario);

        log.info("Contraseña cambiada exitosamente");
    }

    @Override
    public void bloquearUsuario(UUID id) {
        log.info("Bloqueando usuario ID: {}", id);

        Usuario usuario = usuarioRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        usuario.setCuentaBloqueada(true);
        usuarioRepository.save(usuario);

        log.info("Usuario bloqueado exitosamente");
    }

    @Override
    public void desbloquearUsuario(UUID id) {
        log.info("Desbloqueando usuario ID: {}", id);

        Usuario usuario = usuarioRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        usuario.setCuentaBloqueada(false);
        usuario.setIntentosFallidos(0);
        usuarioRepository.save(usuario);

        log.info("Usuario desbloqueado exitosamente");
    }

    @Override
    public void resetearIntentosFallidos(String email) {
        String tenantId = TenantContext.getCurrentTenant();
        usuarioRepository.findByEmailAndTenantId(email, tenantId).ifPresent(usuario -> {
            usuario.setIntentosFallidos(0);
            usuarioRepository.save(usuario);
        });
    }

    @Override
    public void manejarLoginExitoso(String email) {
        String tenantId = TenantContext.getCurrentTenant();
        usuarioRepository.findByEmailAndTenantId(email, tenantId).ifPresent(usuario -> {
            usuario.setIntentosFallidos(0);
            usuario.setUltimoAcceso(LocalDateTime.now());
            usuarioRepository.save(usuario);
        });
    }

    @Override
    public void manejarLoginFallido(String email) {
        String tenantId = TenantContext.getCurrentTenant();
        usuarioRepository.findByEmailAndTenantId(email, tenantId).ifPresent(usuario -> {
            int intentosFallidos = usuario.getIntentosFallidos() + 1;
            usuario.setIntentosFallidos(intentosFallidos);

            if (intentosFallidos >= maxIntentosLogin) {
                usuario.setCuentaBloqueada(true);
                log.warn("Usuario {} bloqueado por exceder máximo de intentos", email);
            }

            usuarioRepository.save(usuario);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public UsuarioResponse obtenerPorId(UUID id) {
        Usuario usuario = usuarioRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        return usuarioMapper.toResponse(usuario);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UsuarioResponse> listar(Pageable pageable) {
        String tenantId = TenantContext.getCurrentTenant();
        Page<Usuario> usuarios = usuarioRepository.findByTenantId(tenantId, pageable);

        return usuarios.map(usuarioMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existeEmail(String email) {
        String tenantId = TenantContext.getCurrentTenant();
        return usuarioRepository.existsByEmailAndTenantId(email, tenantId);
    }

    @Override
    public void eliminar(UUID id) {
        log.info("Eliminando usuario ID: {}", id);

        Usuario usuario = usuarioRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        // Soft delete
        usuario.setActivo(false);
        usuarioRepository.save(usuario);

        log.info("Usuario eliminado (soft delete) exitosamente");
    }

    /**
     * Método auxiliar para validar la seguridad de la contraseña
     */
    private void validarPasswordSegura(String password) {
        if (password == null || password.length() < 8) {
            throw new BusinessException("La contraseña debe tener al menos 8 caracteres");
        }

        // Verificar que contenga al menos una mayúscula, una minúscula, un número y un carácter especial
        String pattern = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!])(?=\\S+$).{8,}$";
        if (!password.matches(pattern)) {
            throw new BusinessException(
                "La contraseña debe contener al menos una mayúscula, una minúscula, " +
                    "un número y un carácter especial (@#$%^&+=!)"
            );
        }
    }

    /**
     * Método auxiliar para asignar sucursales y cajas al usuario
     */
    private void asignarSucursalesYCajas(Usuario usuario, Set<UUID> sucursalesIds, Set<UUID> cajasIds) {
        // Limpiar y asignar sucursales
        if (sucursalesIds != null) {
            usuario.getSucursales().clear();
            Set<Sucursal> sucursales = new HashSet<>(sucursalRepository.findAllById(sucursalesIds));
            usuario.setSucursales(sucursales);
        }

        // Limpiar y asignar cajas
        if (cajasIds != null) {
            usuario.getCajas().clear();
            Set<Caja> cajas = new HashSet<>(cajaRepository.findAllById(cajasIds));
            usuario.setCajas(cajas);
        }
    }
}