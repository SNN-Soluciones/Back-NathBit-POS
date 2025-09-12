package com.snnsoluciones.backnathbitpos.service;

import com.snnsoluciones.backnathbitpos.entity.Empresa;
import com.snnsoluciones.backnathbitpos.entity.Sucursal;
import com.snnsoluciones.backnathbitpos.entity.Usuario;
import com.snnsoluciones.backnathbitpos.enums.RolNombre;
import com.snnsoluciones.backnathbitpos.repository.EmpresaRepository;
import com.snnsoluciones.backnathbitpos.repository.SucursalRepository;
import com.snnsoluciones.backnathbitpos.repository.UsuarioEmpresaRepository;
import com.snnsoluciones.backnathbitpos.repository.UsuarioSucursalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UsuarioPermisosService {

    private final EmpresaRepository empresaRepository;
    private final SucursalRepository sucursalRepository;
    private final UsuarioEmpresaRepository usuarioEmpresaRepository;
    private final UsuarioSucursalRepository usuarioSucursalRepository;

    /**
     * Obtiene los roles que un usuario puede crear basado en su propio rol
     */
    public List<RolNombre> obtenerRolesCreables(RolNombre rolCreador) {
        List<RolNombre> rolesPermitidos = new ArrayList<>();

        switch (rolCreador) {
            case ROOT:
                // ROOT puede crear todos los roles
                rolesPermitidos.addAll(Arrays.asList(RolNombre.values()));
                break;

            case SOPORTE:
                // SOPORTE puede crear todos excepto ROOT
                rolesPermitidos.addAll(Arrays.asList(
                    RolNombre.SOPORTE,
                    RolNombre.SUPER_ADMIN,
                    RolNombre.ADMIN,
                    RolNombre.JEFE_CAJAS,
                    RolNombre.CAJERO,
                    RolNombre.MESERO,
                    RolNombre.COCINA
                ));
                break;

            case SUPER_ADMIN:
                // SUPER_ADMIN puede crear desde su nivel hacia abajo
                rolesPermitidos.addAll(Arrays.asList(
                    RolNombre.SUPER_ADMIN,
                    RolNombre.ADMIN,
                    RolNombre.JEFE_CAJAS,
                    RolNombre.CAJERO,
                    RolNombre.MESERO,
                    RolNombre.COCINA
                ));
                break;

            case ADMIN:
                // ADMIN solo puede crear roles operativos (NO otros ADMIN)
                rolesPermitidos.addAll(Arrays.asList(
                    RolNombre.JEFE_CAJAS,
                    RolNombre.CAJERO,
                    RolNombre.MESERO,
                    RolNombre.COCINA
                ));
                break;

            default:
                // Roles operativos no pueden crear usuarios
                log.warn("Rol {} intentó crear usuarios", rolCreador);
                break;
        }

        return rolesPermitidos;
    }

    /**
     * Obtiene las empresas a las que un usuario puede asignar nuevos usuarios
     */
    public List<Empresa> obtenerEmpresasAsignables(Usuario usuarioCreador) {
        RolNombre rol = usuarioCreador.getRol();

        // ROOT y SOPORTE pueden asignar a cualquier empresa
        if (rol == RolNombre.ROOT || rol == RolNombre.SOPORTE) {
            return empresaRepository.findAllByActivaTrue();
        }

        // SUPER_ADMIN y ADMIN solo pueden asignar a sus empresas
        if (rol == RolNombre.SUPER_ADMIN || rol == RolNombre.ADMIN) {
            return usuarioEmpresaRepository.findEmpresasByUsuarioId(usuarioCreador.getId());
        }

        // Otros roles no pueden asignar
        return new ArrayList<>();
    }

    /**
     * Obtiene las sucursales a las que un usuario puede asignar nuevos usuarios
     */
    public List<Sucursal> obtenerSucursalesAsignables(Usuario usuarioCreador, Long empresaId) {
        RolNombre rol = usuarioCreador.getRol();

        // ROOT y SOPORTE pueden asignar a cualquier sucursal de la empresa
        if (rol == RolNombre.ROOT || rol == RolNombre.SOPORTE) {
            return sucursalRepository.findAllByEmpresaIdAndActivaTrue(empresaId);
        }

        // SUPER_ADMIN puede asignar a cualquier sucursal de sus empresas
        if (rol == RolNombre.SUPER_ADMIN) {
            // Verificar que la empresa sea suya
            boolean esEmpresaPropia = usuarioEmpresaRepository
                .existsByUsuarioIdAndEmpresaId(usuarioCreador.getId(), empresaId);

            if (esEmpresaPropia) {
                return sucursalRepository.findAllByEmpresaIdAndActivaTrue(empresaId);
            }
        }

        // ADMIN solo puede asignar a sus sucursales asignadas
        if (rol == RolNombre.ADMIN) {
            return usuarioSucursalRepository.findSucursalesByUsuarioIdAndEmpresaId(
                usuarioCreador.getId(), empresaId
            );
        }

        return new ArrayList<>();
    }

    /**
     * Valida si un usuario puede crear otro usuario con el rol especificado
     */
    public void validarPermisoCreacion(Usuario creador, RolNombre rolACrear) {
        List<RolNombre> rolesPermitidos = obtenerRolesCreables(creador.getRol());

        if (!rolesPermitidos.contains(rolACrear)) {
            throw new RuntimeException(
                String.format("Usuario con rol %s no puede crear usuarios con rol %s",
                    creador.getRol(), rolACrear)
            );
        }
    }

    /**
     * Valida las asignaciones según el rol a crear y quien lo crea
     */
    public void validarAsignaciones(Usuario creador, RolNombre rolACrear,
        List<Long> empresasIds, List<Long> sucursalesIds) {

        // ROOT y SOPORTE no necesitan asignaciones
        if (rolACrear == RolNombre.ROOT || rolACrear == RolNombre.SOPORTE) {
            if (empresasIds != null && !empresasIds.isEmpty()) {
                log.warn("Se intentó asignar empresas a un rol {}, se ignorarán", rolACrear);
            }
            return;
        }

        // Todos los demás roles necesitan al menos una empresa
        if (empresasIds == null || empresasIds.isEmpty()) {
            throw new RuntimeException(
                String.format("El rol %s requiere al menos una empresa asignada", rolACrear)
            );
        }

        // Validaciones específicas por rol
        switch (rolACrear) {
            case SUPER_ADMIN:
                // SUPER_ADMIN puede tener múltiples empresas
                if (creador.getRol() == RolNombre.SUPER_ADMIN || creador.getRol() == RolNombre.ADMIN) {
                    validarEmpresasPropias(creador, empresasIds);
                }
                break;

            case ADMIN:
                // ADMIN debe tener exactamente una empresa
                if (empresasIds.size() != 1) {
                    throw new RuntimeException("ADMIN debe tener exactamente una empresa asignada");
                }
                // Y puede tener múltiples sucursales
                if (sucursalesIds != null && !sucursalesIds.isEmpty()) {
                    validarSucursalesPropias(creador, empresasIds.get(0), sucursalesIds);
                }
                break;

            case JEFE_CAJAS:
            case CAJERO:
            case MESERO:
            case COCINA:
                // Roles operativos: una empresa, una sucursal
                if (empresasIds.size() != 1) {
                    throw new RuntimeException("Roles operativos deben tener exactamente una empresa");
                }
                if (sucursalesIds == null || sucursalesIds.size() != 1) {
                    throw new RuntimeException("Roles operativos deben tener exactamente una sucursal");
                }
                validarSucursalesPropias(creador, empresasIds.get(0), sucursalesIds);
                break;
        }
    }

    /**
     * Valida que las empresas a asignar sean propias del creador
     */
    private void validarEmpresasPropias(Usuario creador, List<Long> empresasIds) {
        if (creador.getRol() == RolNombre.ROOT || creador.getRol() == RolNombre.SOPORTE) {
            return; // Tienen acceso a todas
        }

        List<Long> empresasPropias = usuarioEmpresaRepository
            .findEmpresasByUsuarioId(creador.getId())
            .stream()
            .map(Empresa::getId)
            .toList();

        for (Long empresaId : empresasIds) {
            if (!empresasPropias.contains(empresaId)) {
                throw new RuntimeException(
                    String.format("No tienes permisos para asignar usuarios a la empresa con ID %d", empresaId)
                );
            }
        }
    }

    /**
     * Valida que las sucursales a asignar sean propias del creador
     */
    private void validarSucursalesPropias(Usuario creador, Long empresaId, List<Long> sucursalesIds) {
        List<Sucursal> sucursalesPermitidas = obtenerSucursalesAsignables(creador, empresaId);
        List<Long> idsPermitidos = sucursalesPermitidas.stream()
            .map(Sucursal::getId)
            .toList();

        for (Long sucursalId : sucursalesIds) {
            if (!idsPermitidos.contains(sucursalId)) {
                throw new RuntimeException(
                    String.format("No tienes permisos para asignar usuarios a la sucursal con ID %d", sucursalId)
                );
            }
        }
    }
}