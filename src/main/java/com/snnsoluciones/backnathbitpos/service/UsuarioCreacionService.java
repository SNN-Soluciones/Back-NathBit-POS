package com.snnsoluciones.backnathbitpos.service;

import com.snnsoluciones.backnathbitpos.dto.usuarios.CrearUsuarioCompletoRequest;
import com.snnsoluciones.backnathbitpos.dto.usuarios.CrearUsuarioCompletoResponse;
import com.snnsoluciones.backnathbitpos.entity.*;
import com.snnsoluciones.backnathbitpos.enums.RolNombre;
import com.snnsoluciones.backnathbitpos.repository.*;
import com.snnsoluciones.backnathbitpos.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UsuarioCreacionService {

    private final UsuarioRepository usuarioRepository;
    private final UsuarioRegistroRepository usuarioRegistroRepository;
    private final UsuarioEmpresaRepository usuarioEmpresaRepository;
    private final UsuarioSucursalRepository usuarioSucursalRepository;
    private final EmpresaRepository empresaRepository;
    private final SucursalRepository sucursalRepository;
    private final UsuarioPermisosService permisosService;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public CrearUsuarioCompletoResponse crearUsuarioCompleto(CrearUsuarioCompletoRequest request) {
        log.info("=== INICIANDO CREACIÓN DE USUARIO ===");
        log.info("Email: {}, Rol: {}", request.getEmail(), request.getRol());

        try {
            // 1. Obtener usuario actual (quien está creando)
            Usuario usuarioCreador = obtenerUsuarioActual();
            log.info("Usuario creador: {} ({})", usuarioCreador.getEmail(), usuarioCreador.getRol());

            // 2. Validar permisos de creación
            log.info("2. Validando permisos de creación...");
            permisosService.validarPermisoCreacion(usuarioCreador, request.getRol());
            log.info("   ✅ Permisos validados");

            // 3. Preparar listas de IDs
            List<Long> empresasIds = prepararEmpresasIds(request);
            List<Long> sucursalesIds = prepararSucursalesIds(request);

            // 4. Validar asignaciones
            log.info("4. Validando asignaciones...");
            permisosService.validarAsignaciones(usuarioCreador, request.getRol(), empresasIds, sucursalesIds);
            log.info("   ✅ Asignaciones válidas");

            // 5. Validar que no exista el email
            if (usuarioRepository.existsByEmail(request.getEmail())) {
                throw new RuntimeException("Ya existe un usuario con el email: " + request.getEmail());
            }

            // 6. Crear el usuario
            log.info("6. Creando usuario...");
            String passwordTemporal = generarPasswordTemporal(request.getPassword());
            Usuario nuevoUsuario = crearUsuario(request, passwordTemporal);
            log.info("   ✅ Usuario creado con ID: {}", nuevoUsuario.getId());

            // 7. Crear registro de auditoría
            log.info("7. Creando registro de auditoría...");
            crearRegistroAuditoria(nuevoUsuario, usuarioCreador);
            log.info("   ✅ Auditoría registrada");

            // 8. Crear asignaciones según el rol
            List<String> empresasAsignadas = new ArrayList<>();
            List<String> sucursalesAsignadas = new ArrayList<>();

            if (!empresasIds.isEmpty()) {
                log.info("8. Creando asignaciones de empresa...");
                empresasAsignadas = crearAsignacionesEmpresa(nuevoUsuario, empresasIds);
                log.info("   ✅ {} empresas asignadas", empresasAsignadas.size());
            }

            if (!sucursalesIds.isEmpty()) {
                log.info("9. Creando asignaciones de sucursal...");
                sucursalesAsignadas = crearAsignacionesSucursal(nuevoUsuario, sucursalesIds);
                log.info("   ✅ {} sucursales asignadas", sucursalesAsignadas.size());
            }

            // 9. Construir respuesta
            CrearUsuarioCompletoResponse response = construirRespuesta(
                nuevoUsuario, 
                passwordTemporal, 
                empresasAsignadas, 
                sucursalesAsignadas,
                usuarioCreador
            );

            log.info("=== USUARIO CREADO EXITOSAMENTE ===");
            return response;

        } catch (RuntimeException e) {
            log.error("❌ Error de negocio: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("❌ Error inesperado creando usuario", e);
            throw new RuntimeException("Error al crear el usuario: " + e.getMessage(), e);
        }
    }

    private Usuario obtenerUsuarioActual() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return usuarioRepository.findById(Long.parseLong(email))
            .orElseThrow(() -> new RuntimeException("Usuario actual no encontrado"));
    }

    private List<Long> prepararEmpresasIds(CrearUsuarioCompletoRequest request) {
        if (request.getRol() == RolNombre.SUPER_ADMIN && request.getEmpresasIds() != null) {
            return request.getEmpresasIds();
        } else if (request.getEmpresaId() != null) {
            return List.of(request.getEmpresaId());
        }
        return new ArrayList<>();
    }

    private List<Long> prepararSucursalesIds(CrearUsuarioCompletoRequest request) {
        if (request.getRol() == RolNombre.ADMIN && request.getSucursalesIds() != null) {
            return request.getSucursalesIds();
        } else if (request.getSucursalId() != null) {
            return List.of(request.getSucursalId());
        }
        return new ArrayList<>();
    }

    private String generarPasswordTemporal(String passwordRequest) {
        if (passwordRequest != null && !passwordRequest.isEmpty()) {
            return passwordRequest;
        }
        // Generar password temporal
        return "TempPass2024!";
    }

    private Usuario crearUsuario(CrearUsuarioCompletoRequest request, String password) {
        Usuario usuario = new Usuario();
        usuario.setEmail(request.getEmail());
        usuario.setNombre(request.getNombre());
        usuario.setApellidos(request.getApellidos());
        usuario.setTelefono(request.getTelefono());
        usuario.setRol(request.getRol());
        usuario.setPassword(passwordEncoder.encode(password));
        usuario.setActivo(true);
        usuario.setRequiereCambioPassword(true); // Para v2.0 forzar cambio

        return usuarioRepository.save(usuario);
    }

    private void crearRegistroAuditoria(Usuario usuario, Usuario creador) {
        UsuarioRegistro registro = UsuarioRegistro.builder()
            .usuario(usuario)
            .creadoPor(creador)
            .actualizadoPor(creador)
            .build();
        
        usuarioRegistroRepository.save(registro);
    }

    private List<String> crearAsignacionesEmpresa(Usuario usuario, List<Long> empresasIds) {
        List<String> nombresEmpresas = new ArrayList<>();
        
        for (Long empresaId : empresasIds) {
            Empresa empresa = empresaRepository.findById(empresaId)
                .orElseThrow(() -> new RuntimeException("Empresa no encontrada: " + empresaId));
            
            UsuarioEmpresa usuarioEmpresa = new UsuarioEmpresa();
            usuarioEmpresa.setUsuario(usuario);
            usuarioEmpresa.setEmpresa(empresa);
            usuarioEmpresa.setActivo(true);

            usuarioEmpresaRepository.save(usuarioEmpresa);
            nombresEmpresas.add(empresa.getNombreComercial());
            
            log.info("   Usuario asignado a empresa: {}", empresa.getNombreComercial());
        }
        
        return nombresEmpresas;
    }

    private List<String> crearAsignacionesSucursal(Usuario usuario, List<Long> sucursalesIds) {
        List<String> nombresSucursales = new ArrayList<>();
        
        for (Long sucursalId : sucursalesIds) {
            Sucursal sucursal = sucursalRepository.findById(sucursalId)
                .orElseThrow(() -> new RuntimeException("Sucursal no encontrada: " + sucursalId));
            
            UsuarioSucursal usuarioSucursal = UsuarioSucursal.builder()
                .usuario(usuario)
                .sucursal(sucursal)
                .activo(true)
                .build();
            
            usuarioSucursalRepository.save(usuarioSucursal);
            nombresSucursales.add(sucursal.getNombre());
            
            log.info("   Usuario asignado a sucursal: {}", sucursal.getNombre());
        }
        
        return nombresSucursales;
    }

    private CrearUsuarioCompletoResponse construirRespuesta(
            Usuario usuario, 
            String passwordTemporal,
            List<String> empresasAsignadas,
            List<String> sucursalesAsignadas,
            Usuario creador) {
        
        return CrearUsuarioCompletoResponse.builder()
            .usuarioId(usuario.getId())
            .email(usuario.getEmail())
            .nombre(usuario.getNombre())
            .apellidos(usuario.getApellidos())
            .rol(usuario.getRol())
            .passwordTemporal(passwordTemporal)
            .empresasAsignadas(empresasAsignadas)
            .sucursalesAsignadas(sucursalesAsignadas)
            .mensaje("Usuario creado exitosamente")
            .createdAt(usuario.getCreatedAt())
            .creadoPor(creador.getNombre() + " " + creador.getApellidos())
            .build();
    }
}