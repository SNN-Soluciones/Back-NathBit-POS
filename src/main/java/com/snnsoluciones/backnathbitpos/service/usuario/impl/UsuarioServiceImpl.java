package com.snnsoluciones.backnathbitpos.service.usuario.impl;

import com.snnsoluciones.backnathbitpos.dto.usuario.AccesoDTO;
import com.snnsoluciones.backnathbitpos.dto.usuario.CrearUsuarioRequest;
import com.snnsoluciones.backnathbitpos.dto.usuario.UsuarioDTO;
import com.snnsoluciones.backnathbitpos.entity.Usuario;
import com.snnsoluciones.backnathbitpos.entity.UsuarioEmpresaRol;
import com.snnsoluciones.backnathbitpos.exception.BadRequestException;
import com.snnsoluciones.backnathbitpos.exception.ConflictException;
import com.snnsoluciones.backnathbitpos.exception.ResourceNotFoundException;
import com.snnsoluciones.backnathbitpos.mapper.UsuarioMapper;
import com.snnsoluciones.backnathbitpos.repository.UsuarioEmpresaRolRepository;
import com.snnsoluciones.backnathbitpos.repository.UsuarioRepository;
import com.snnsoluciones.backnathbitpos.service.usuario.UsuarioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UsuarioServiceImpl implements UsuarioService {
    
    private final UsuarioRepository usuarioRepository;
    private final UsuarioEmpresaRolRepository usuarioEmpresaRolRepository;
    private final UsuarioMapper usuarioMapper;
    private final PasswordEncoder passwordEncoder;
    
    @Override
    public Usuario findByEmail(String email) {
        return usuarioRepository.findByEmail(email)
            .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con email: " + email));
    }
    
    @Override
    public UsuarioDTO findById(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id: " + id));
        return usuarioMapper.toDTO(usuario);
    }
    
    @Override
    public List<AccesoDTO> obtenerAccesos(Long usuarioId) {
        List<UsuarioEmpresaRol> roles = usuarioEmpresaRolRepository
            .findByUsuarioIdWithRelaciones(usuarioId);
        
        return roles.stream()
            .filter(UsuarioEmpresaRol::getActivo)
            .filter(UsuarioEmpresaRol::esValido)
            .map(this::mapearAcceso)
            .collect(Collectors.toList());
    }
    
    @Override
    public boolean validarAcceso(Long usuarioId, Long empresaId, Long sucursalId) {
        if (sucursalId != null) {
            return usuarioEmpresaRolRepository
                .existsByUsuarioIdAndEmpresaIdAndSucursalIdAndActivoTrue(
                    usuarioId, empresaId, sucursalId);
        } else {
            return usuarioEmpresaRolRepository
                .existsByUsuarioIdAndEmpresaIdAndSucursalIsNullAndActivoTrue(
                    usuarioId, empresaId);
        }
    }
    
    @Override
    @Transactional
    public UsuarioDTO crearUsuario(CrearUsuarioRequest request) {
        // Validar que no exista email
        if (existePorEmail(request.getEmail())) {
            throw new ConflictException("Ya existe un usuario con el email: " + request.getEmail());
        }
        
        // Validar identificación si se proporciona
        if (request.getIdentificacion() != null && 
            existePorIdentificacion(request.getIdentificacion())) {
            throw new ConflictException("Ya existe un usuario con la identificación: " + 
                request.getIdentificacion());
        }
        
        // Crear usuario
        Usuario usuario = new Usuario();
        usuario.setEmail(request.getEmail());
        usuario.setPassword(passwordEncoder.encode(request.getPassword()));
        usuario.setNombre(request.getNombre());
        usuario.setApellidos(request.getApellidos());
        usuario.setTelefono(request.getTelefono());
        usuario.setIdentificacion(request.getIdentificacion());
        usuario.setActivo(true);
        
        usuario = usuarioRepository.save(usuario);
        
        log.info("Usuario creado: {}", usuario.getEmail());
        return usuarioMapper.toDTO(usuario);
    }
    
    @Override
    @Transactional
    public UsuarioDTO actualizarUsuario(Long id, UsuarioDTO usuarioDTO) {
        Usuario usuario = usuarioRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id: " + id));
        
        // Validar email si cambió
        if (!usuario.getEmail().equals(usuarioDTO.getEmail()) && 
            existePorEmail(usuarioDTO.getEmail())) {
            throw new ConflictException("Ya existe un usuario con el email: " + usuarioDTO.getEmail());
        }
        
        // Validar identificación si cambió
        if (usuarioDTO.getIdentificacion() != null &&
            !usuarioDTO.getIdentificacion().equals(usuario.getIdentificacion()) &&
            existePorIdentificacion(usuarioDTO.getIdentificacion())) {
            throw new ConflictException("Ya existe un usuario con la identificación: " + 
                usuarioDTO.getIdentificacion());
        }
        
        // Actualizar campos
        usuario.setEmail(usuarioDTO.getEmail());
        usuario.setNombre(usuarioDTO.getNombre());
        usuario.setApellidos(usuarioDTO.getApellidos());
        usuario.setTelefono(usuarioDTO.getTelefono());
        usuario.setIdentificacion(usuarioDTO.getIdentificacion());
        
        usuario = usuarioRepository.save(usuario);
        
        log.info("Usuario actualizado: {}", usuario.getEmail());
        return usuarioMapper.toDTO(usuario);
    }
    
    @Override
    public Page<UsuarioDTO> listarUsuarios(Long empresaId, Long sucursalId, 
                                          String search, Pageable pageable) {
        Page<Usuario> usuarios;
        
        if (sucursalId != null) {
            usuarios = usuarioRepository.findBySucursalId(sucursalId, pageable);
        } else if (empresaId != null) {
            usuarios = usuarioRepository.findByEmpresaId(empresaId, pageable);
        } else {
            // Solo SUPER_ADMIN puede ver todos
            usuarios = usuarioRepository.findAll(pageable);
        }
        
        return usuarios.map(usuarioMapper::toDTO);
    }
    
    @Override
    @Transactional
    public UsuarioDTO cambiarEstadoUsuario(Long id, boolean activo) {
        Usuario usuario = usuarioRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id: " + id));
        
        usuario.setActivo(activo);
        usuario = usuarioRepository.save(usuario);
        
        log.info("Estado de usuario {} cambiado a: {}", usuario.getEmail(), activo);
        return usuarioMapper.toDTO(usuario);
    }
    
    @Override
    @Transactional
    public void actualizarUltimoAcceso(Long usuarioId) {
        usuarioRepository.actualizarUltimoAcceso(usuarioId, LocalDateTime.now());
    }
    
    @Override
    @Transactional
    public void cambiarPassword(Long usuarioId, String passwordActual, String passwordNueva) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
            .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id: " + usuarioId));
        
        // Verificar password actual
        if (!passwordEncoder.matches(passwordActual, usuario.getPassword())) {
            throw new BadRequestException("La contraseña actual es incorrecta");
        }
        
        // Actualizar password
        usuario.setPassword(passwordEncoder.encode(passwordNueva));
        usuarioRepository.save(usuario);
        
        log.info("Contraseña cambiada para usuario: {}", usuario.getEmail());
    }
    
    @Override
    public boolean existePorEmail(String email) {
        return usuarioRepository.existsByEmail(email);
    }
    
    @Override
    public boolean existePorIdentificacion(String identificacion) {
        return usuarioRepository.existsByIdentificacion(identificacion);
    }
    
    // Métodos auxiliares privados
    
    private AccesoDTO mapearAcceso(UsuarioEmpresaRol uer) {
        AccesoDTO acceso = new AccesoDTO();
        
        // Mapear empresa
        AccesoDTO.EmpresaInfo empresaInfo = new AccesoDTO.EmpresaInfo();
        empresaInfo.setId(uer.getEmpresa().getId());
        empresaInfo.setNombre(uer.getEmpresa().getNombre());
        empresaInfo.setCodigo(uer.getEmpresa().getCodigo());
        acceso.setEmpresa(empresaInfo);
        
        // Mapear sucursal si existe
        if (uer.getSucursal() != null) {
            AccesoDTO.SucursalInfo sucursalInfo = new AccesoDTO.SucursalInfo();
            sucursalInfo.setId(uer.getSucursal().getId());
            sucursalInfo.setNombre(uer.getSucursal().getNombre());
            sucursalInfo.setCodigo(uer.getSucursal().getCodigo());
            acceso.setSucursal(sucursalInfo);
        }
        
        // Mapear rol y permisos
        acceso.setRol(uer.getRol());
        acceso.setPermisos(uer.getPermisos());
        acceso.setEsPrincipal(uer.getEsPrincipal());
        
        return acceso;
    }
}