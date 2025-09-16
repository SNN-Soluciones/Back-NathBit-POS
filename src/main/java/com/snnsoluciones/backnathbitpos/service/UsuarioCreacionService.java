package com.snnsoluciones.backnathbitpos.service;

import com.snnsoluciones.backnathbitpos.dto.usuarios.CrearUsuarioCompletoRequest;
import com.snnsoluciones.backnathbitpos.dto.usuarios.CrearUsuarioCompletoResponse;
import com.snnsoluciones.backnathbitpos.entity.*;
import com.snnsoluciones.backnathbitpos.enums.RolNombre;
import com.snnsoluciones.backnathbitpos.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UsuarioCreacionService {

    private final UsuarioRepository usuarioRepository;
    private final EmpresaRepository empresaRepository;
    private final SucursalRepository sucursalRepository;
    private final UsuarioEmpresaRepository usuarioEmpresaRepository;
    private final UsuarioSucursalRepository usuarioSucursalRepository;
    private final UsuarioPermisosService usuarioPermisosService;
    private final PasswordEncoder passwordEncoder;
    private final UsuarioEmailService usuarioEmailService;

    /**
     * Crea un usuario completo con todas sus asignaciones en una transacción
     */
    @Transactional
    public CrearUsuarioCompletoResponse crearUsuarioCompleto(CrearUsuarioCompletoRequest request) {
        log.info("Iniciando creación de usuario completo: {}", request.getEmail());

        // Obtener usuario creador
        String emailCreador = SecurityContextHolder.getContext().getAuthentication().getName();
        Usuario creador = usuarioRepository.findByEmail(emailCreador)
            .orElseThrow(() -> new RuntimeException("Usuario creador no encontrado"));

        // Validar permisos de creación
        usuarioPermisosService.validarPermisoCreacion(creador, request.getRol());

        // Validar email único
        if (usuarioRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("El email ya está registrado");
        }

        // Preparar listas de IDs según el rol
        List<Long> empresasIds = prepararEmpresasIds(request);
        List<Long> sucursalesIds = prepararSucursalesIds(request);

        // Validar asignaciones
        usuarioPermisosService.validarAsignaciones(creador, request.getRol(), empresasIds, sucursalesIds);

        // Crear usuario
        Usuario nuevoUsuario = new Usuario();
        nuevoUsuario.setEmail(request.getEmail());
        nuevoUsuario.setNombre(request.getNombre());
        nuevoUsuario.setApellidos(request.getApellidos());
        nuevoUsuario.setRol(request.getRol());
        nuevoUsuario.setTelefono(request.getTelefono());
        nuevoUsuario.setActivo(true);
        nuevoUsuario.setCreatedAt(LocalDateTime.now());

        // Generar contraseña
        String passwordTemporal = request.getPassword();
        if (passwordTemporal == null || passwordTemporal.isEmpty()) {
            passwordTemporal = generarPasswordTemporal();
        }
        nuevoUsuario.setPassword(passwordEncoder.encode(passwordTemporal));

        // Guardar usuario
        Usuario usuarioGuardado = usuarioRepository.save(nuevoUsuario);
        log.info("Usuario creado con ID: {}", usuarioGuardado.getId());

      usuarioEmailService.enviarCredencialesTemporal(
          usuarioGuardado.getEmail(),
          usuarioGuardado.getNombre(),
          passwordTemporal
      );

      // Asignar empresas y sucursales según el rol
        List<String> empresasAsignadas = new ArrayList<>();
        List<String> sucursalesAsignadas = new ArrayList<>();

        if (request.getRol() != RolNombre.ROOT && request.getRol() != RolNombre.SOPORTE) {
            // Asignar empresas
            for (Long empresaId : empresasIds) {
                Empresa empresa = empresaRepository.findById(empresaId)
                    .orElseThrow(() -> new RuntimeException("Empresa no encontrada: " + empresaId));

                UsuarioEmpresa ue = new UsuarioEmpresa();
                ue.setUsuario(usuarioGuardado);
                ue.setEmpresa(empresa);
                ue.setFechaAsignacion(LocalDateTime.now());
                ue.setActivo(true);
                usuarioEmpresaRepository.save(ue);

                empresasAsignadas.add(empresa.getNombreComercial());
            }

            // Asignar sucursales (solo para ADMIN y roles operativos)
            if (sucursalesIds != null && !sucursalesIds.isEmpty()) {
                for (Long sucursalId : sucursalesIds) {
                    Sucursal sucursal = sucursalRepository.findById(sucursalId)
                        .orElseThrow(() -> new RuntimeException("Sucursal no encontrada: " + sucursalId));

                    UsuarioSucursal us = new UsuarioSucursal();
                    us.setUsuario(usuarioGuardado);
                    us.setSucursal(sucursal);
                    us.setActivo(true);
                    usuarioSucursalRepository.save(us);

                    sucursalesAsignadas.add(sucursal.getNombre());
                }
            }
        }

        // Construir respuesta
        CrearUsuarioCompletoResponse response = new CrearUsuarioCompletoResponse();
        response.setUsuarioId(usuarioGuardado.getId());
        response.setEmail(usuarioGuardado.getEmail());
        response.setNombre(usuarioGuardado.getNombre());
        response.setApellidos(usuarioGuardado.getApellidos());
        response.setRol(usuarioGuardado.getRol());
        response.setPasswordTemporal(request.getPassword() == null ? passwordTemporal : null);
        response.setEmpresasAsignadas(empresasAsignadas);
        response.setSucursalesAsignadas(sucursalesAsignadas);
        response.setCreatedAt(usuarioGuardado.getCreatedAt());
        response.setCreadoPor(creador.getEmail());

        String mensaje = construirMensajeRespuesta(usuarioGuardado.getRol(), empresasAsignadas, sucursalesAsignadas);
        response.setMensaje(mensaje);

        log.info("Usuario creado exitosamente: {} con rol {}", usuarioGuardado.getEmail(), usuarioGuardado.getRol());

        return response;
    }

    /**
     * Prepara la lista de IDs de empresas según el request
     */
    private List<Long> prepararEmpresasIds(CrearUsuarioCompletoRequest request) {
        List<Long> empresasIds = new ArrayList<>();

        // Para SUPER_ADMIN usar empresasIds (múltiples)
        if (request.getRol() == RolNombre.SUPER_ADMIN && request.getEmpresasIds() != null) {
            empresasIds.addAll(request.getEmpresasIds());
        }
        // Para otros roles usar empresaId (singular)
        else if (request.getEmpresaId() != null) {
            empresasIds.add(request.getEmpresaId());
        }

        return empresasIds;
    }

    /**
     * Prepara la lista de IDs de sucursales según el request
     */
    private List<Long> prepararSucursalesIds(CrearUsuarioCompletoRequest request) {
        List<Long> sucursalesIds = new ArrayList<>();

        // Para ADMIN usar sucursalesIds (múltiples)
        if (request.getRol() == RolNombre.ADMIN && request.getSucursalesIds() != null) {
            sucursalesIds.addAll(request.getSucursalesIds());
        }
        // Para roles operativos usar sucursalId (singular)
        else if (request.getSucursalId() != null) {
            sucursalesIds.add(request.getSucursalId());
        }

        return sucursalesIds;
    }

    /**
     * Genera una contraseña temporal
     */
    private String generarPasswordTemporal() {
        return "Temp" + UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * Construye el mensaje de respuesta según el rol
     */
    private String construirMensajeRespuesta(RolNombre rol, List<String> empresas, List<String> sucursales) {
        StringBuilder mensaje = new StringBuilder();
        mensaje.append("Usuario creado exitosamente con rol ").append(rol);

        if (rol == RolNombre.ROOT || rol == RolNombre.SOPORTE) {
            mensaje.append(". Tiene acceso total al sistema.");
        } else {
            if (!empresas.isEmpty()) {
                mensaje.append(". Asignado a empresa(s): ").append(String.join(", ", empresas));
            }
            if (!sucursales.isEmpty()) {
                mensaje.append(". Sucursal(es): ").append(String.join(", ", sucursales));
            }
        }

        return mensaje.toString();
    }
}