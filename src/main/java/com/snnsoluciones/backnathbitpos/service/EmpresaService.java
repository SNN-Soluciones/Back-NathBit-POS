package com.snnsoluciones.backnathbitpos.service;

import com.snnsoluciones.backnathbitpos.entity.Empresa;
import com.snnsoluciones.backnathbitpos.entity.EmpresaActividad;
import com.snnsoluciones.backnathbitpos.entity.EmpresaConfigHacienda;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface EmpresaService {

    // Métodos existentes
    Empresa crear(Empresa empresa);
    Empresa actualizar(Long id, Empresa empresa);
    Optional<Empresa> buscarPorId(Long id);
    Optional<Empresa> buscarPorCodigo(String codigo);
    List<Empresa> listarTodas();
    void eliminar(Long id);
    boolean existeCodigo(String codigo);
    boolean existeIdentificacion(String identificacion);
    Page<Empresa> listarPorUsuario(Long usuarioId, Pageable pageable );

    // Configuración Hacienda
    EmpresaConfigHacienda crearConfiguracionHacienda(Long empresaId, EmpresaConfigHacienda config);
    EmpresaConfigHacienda actualizarConfiguracionHacienda(Long empresaId, EmpresaConfigHacienda config);
    Optional<EmpresaConfigHacienda> buscarConfiguracionHacienda(Long empresaId);

    // Actividades económicas
    EmpresaActividad agregarActividad(Long empresaId, String codigoActividad, Boolean esPrincipal);
    List<EmpresaActividad> listarActividades(Long empresaId);

    // Validaciones
    boolean tieneFacturacionElectronicaConfigurada(Long empresaId);
    Page<Empresa> listarPorUsuarioPaginado(Long usuarioId, Pageable pageable);
}