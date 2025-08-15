package com.snnsoluciones.backnathbitpos.service;

import com.snnsoluciones.backnathbitpos.dto.usuarios.UsuarioAsignacionRequest;
import com.snnsoluciones.backnathbitpos.entity.*;
import com.snnsoluciones.backnathbitpos.enums.RolNombre;
import com.snnsoluciones.backnathbitpos.repository.*;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UsuarioAsignacionService {

    private final UsuarioRepository usuarioRepository;
    private final EmpresaRepository empresaRepository;
    private final UsuarioEmpresaRepository usuarioEmpresaRepository;
    private final UsuarioRegistroRepository usuarioRegistroRepository;
    private final UsuarioPermisosService permisosService;

    @Transactional
    public void gestionarAsignacionesEmpresa(Long usuarioId, UsuarioAsignacionRequest request, Usuario solicitante) {
        log.info("=== GESTIONANDO ASIGNACIONES DE EMPRESA ===");
        log.info("Usuario a modificar: {}, Modo: {}", usuarioId, request.getModo());

        // 1. Obtener el usuario a modificar
        Usuario usuario = usuarioRepository.findById(usuarioId)
            .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // 2. Validar permisos del solicitante
        validarPermisosAsignacion(solicitante, usuario, request.getEmpresasIds());

        // 3. Ejecutar según el modo
        switch (request.getModo()) {
            case AGREGAR:
                agregarEmpresas(usuario, request.getEmpresasIds());
                break;
            case REEMPLAZAR:
                reemplazarEmpresas(usuario, request.getEmpresasIds());
                break;
            case QUITAR:
                quitarEmpresas(usuario, request.getEmpresasIds());
                break;
        }

        // 4. Actualizar auditoría
        actualizarAuditoria(usuario, solicitante);

        log.info("=== ASIGNACIONES ACTUALIZADAS EXITOSAMENTE ===");
    }

    private void validarPermisosAsignacion(Usuario solicitante, Usuario usuarioObjetivo, List<Long> empresasIds) {
        // ROOT y SOPORTE pueden asignar cualquier cosa
        if (solicitante.getRol() == RolNombre.ROOT || solicitante.getRol() == RolNombre.SOPORTE) {
            return;
        }

        // SUPER_ADMIN solo puede asignar SUS empresas
        if (solicitante.getRol() == RolNombre.SUPER_ADMIN) {
            List<Long> empresasPermitidas = permisosService.obtenerEmpresasAsignables(solicitante)
                .stream()
                .map(Empresa::getId)
                .toList();

            for (Long empresaId : empresasIds) {
                if (!empresasPermitidas.contains(empresaId)) {
                    throw new RuntimeException(
                        "No tiene permisos para asignar la empresa ID: " + empresaId
                    );
                }
            }

            // SUPER_ADMIN no puede modificar usuarios de nivel superior
            if (usuarioObjetivo.getRol() == RolNombre.ROOT || 
                usuarioObjetivo.getRol() == RolNombre.SOPORTE) {
                throw new RuntimeException("No puede modificar usuarios de nivel superior");
            }
        } else {
            // ADMIN y otros no pueden gestionar empresas
            throw new RuntimeException("No tiene permisos para gestionar asignaciones de empresa");
        }
    }

    private void agregarEmpresas(Usuario usuario, List<Long> empresasIds) {
        log.info("Agregando {} empresas al usuario", empresasIds.size());

        for (Long empresaId : empresasIds) {
            // Verificar si ya existe la asignación
            boolean yaAsignado = usuarioEmpresaRepository
                .existsByUsuarioIdAndEmpresaId(usuario.getId(), empresaId);

            if (!yaAsignado) {
                Empresa empresa = empresaRepository.findById(empresaId)
                    .orElseThrow(() -> new RuntimeException("Empresa no encontrada: " + empresaId));

                UsuarioEmpresa nuevaAsignacion = new UsuarioEmpresa();
                nuevaAsignacion.setUsuario(usuario);
                nuevaAsignacion.setEmpresa(empresa);
                nuevaAsignacion.setActivo(true);

                usuarioEmpresaRepository.save(nuevaAsignacion);
                log.info("   ✅ Asignado a empresa: {}", empresa.getNombreComercial());
            } else {
                log.info("   ⚠️  Ya estaba asignado a empresa ID: {}", empresaId);
            }
        }
    }

    private void reemplazarEmpresas(Usuario usuario, List<Long> empresasIds) {
        log.info("Reemplazando todas las empresas del usuario");

        // 1. Desactivar todas las asignaciones actuales
        List<UsuarioEmpresa> asignacionesActuales = usuarioEmpresaRepository.findByUsuarioId(usuario.getId());
        for (UsuarioEmpresa asignacion : asignacionesActuales) {
            asignacion.setActivo(false);
            usuarioEmpresaRepository.save(asignacion);
        }

        // 2. Agregar las nuevas
        agregarEmpresas(usuario, empresasIds);
    }

    private void quitarEmpresas(Usuario usuario, List<Long> empresasIds) {
        log.info("Quitando {} empresas del usuario", empresasIds.size());

        for (Long empresaId : empresasIds) {
            Optional<Object> asignacionOpt = usuarioEmpresaRepository
                .findByUsuarioIdAndEmpresaId(usuario.getId(), empresaId);

            if (asignacionOpt.isPresent()) {
                UsuarioEmpresa asignacion = (UsuarioEmpresa) asignacionOpt.get();
                asignacion.setActivo(false);
                usuarioEmpresaRepository.save(asignacion);
                log.info("   ✅ Removido de empresa ID: {}", empresaId);
            }
        }
    }

    private void actualizarAuditoria(Usuario usuario, Usuario solicitante) {
        usuarioRegistroRepository.findByUsuarioId(usuario.getId())
            .ifPresent(registro -> {
                registro.setActualizadoPor(solicitante);
                usuarioRegistroRepository.save(registro);
            });
    }
}