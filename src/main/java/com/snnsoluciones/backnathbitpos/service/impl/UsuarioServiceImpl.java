package com.snnsoluciones.backnathbitpos.service.impl;

import com.snnsoluciones.backnathbitpos.entity.Usuario;
import com.snnsoluciones.backnathbitpos.repository.UsuarioRepository;
import com.snnsoluciones.backnathbitpos.service.UsuarioService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class UsuarioServiceImpl implements UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public Usuario crear(Usuario usuario) {
        // Encriptar password
        usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));
        return usuarioRepository.save(usuario);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Usuario> buscarPorId(Long id) {
        return usuarioRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Usuario> buscarPorEmail(String email) {
        return usuarioRepository.findByEmail(email);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Usuario> listarTodos() {
        return usuarioRepository.findAll();
    }

    @Override
    public void eliminar(Long id) {
        usuarioRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existeEmail(String email) {
        return usuarioRepository.existsByEmail(email);
    }

    @Override
    public Optional<Usuario> buscarPorUsername(String username) {
        return usuarioRepository.findByUsernameIgnoreCase(username);
    }

    @Override
    public List<Usuario> listarPorEmpresa(Long empresaId) {
        return usuarioRepository.findByEmpresaId(empresaId);
    }

    /**
     * Verifica si la contraseña proporcionada es correcta
     */
    public boolean verificarPassword(Long usuarioId, String password) {
        Usuario usuario = buscarPorId(usuarioId)
            .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        return passwordEncoder.matches(password, usuario.getPassword());
    }

    /**
     * Actualiza un usuario
     */
    @Transactional
    public Usuario actualizar(Long id, Usuario datosActualizados) {
        Usuario usuario = buscarPorId(id)
            .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // Actualizar solo campos permitidos
        if (datosActualizados.getNombre() != null) {
            usuario.setNombre(datosActualizados.getNombre());
        }
        if (datosActualizados.getApellidos() != null) {
            usuario.setApellidos(datosActualizados.getApellidos());
        }
        if (datosActualizados.getTelefono() != null) {
            usuario.setTelefono(datosActualizados.getTelefono());
        }
        if (datosActualizados.getActivo() != null) {
            usuario.setActivo(datosActualizados.getActivo());
        }

        // Si hay nueva contraseña, encriptarla
        if (datosActualizados.getPassword() != null && !datosActualizados.getPassword().isEmpty()) {
            usuario.setPassword(passwordEncoder.encode(datosActualizados.getPassword()));
        }

        usuario.setCreatedAt(LocalDateTime.now());

        return usuarioRepository.save(usuario);
    }

    /**
     * Cambia la contraseña de un usuario
     * @param usuarioId ID del usuario
     * @param nuevaPassword Nueva contraseña sin encriptar
     */
    @Transactional
    public void cambiarPassword(Long usuarioId, String nuevaPassword) {
        log.info("Cambiando contraseña para usuario ID: {}", usuarioId);

        Usuario usuario = buscarPorId(usuarioId)
            .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // Validar que la nueva contraseña no sea vacía
        if (nuevaPassword == null || nuevaPassword.trim().isEmpty()) {
            throw new RuntimeException("La contraseña no puede estar vacía");
        }

        // Validar longitud mínima
        if (nuevaPassword.length() < 6) {
            throw new RuntimeException("La contraseña debe tener al menos 6 caracteres");
        }

        // Encriptar y guardar
        usuario.setPassword(passwordEncoder.encode(nuevaPassword));
        usuario.setRequiereCambioPassword(false); // ← AGREGAR ESTO
        usuario.setUpdatedAt(LocalDateTime.now()); // ← CAMBIAR ESTO (era setCreatedAt)

        usuarioRepository.save(usuario);

        log.info("Contraseña actualizada exitosamente para usuario: {}", usuario.getEmail());
    }
}
