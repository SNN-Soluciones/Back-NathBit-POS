package com.snnsoluciones.backnathbitpos.service;

import com.snnsoluciones.backnathbitpos.dto.empresa.CertificadoResponse;
import com.snnsoluciones.backnathbitpos.dto.empresa.EmpresaResponse;
import com.snnsoluciones.backnathbitpos.dto.empresa.UrlCertificadoResponse;
import com.snnsoluciones.backnathbitpos.entity.Empresa;
import com.snnsoluciones.backnathbitpos.entity.EmpresaActividad;
import com.snnsoluciones.backnathbitpos.entity.EmpresaConfigHacienda;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

public interface EmpresaService {

    Empresa actualizar(Long id, Empresa empresa);
    Empresa buscarPorId(Long id);
    List<Empresa> listarTodas();
    void eliminar(Long id);
    boolean existeIdentificacion(String identificacion);
    Page<Empresa> listarPorUsuario(Long usuarioId, Pageable pageable );

    Page<Empresa> listarPorUsuarioPaginado(Long usuarioId, Pageable pageable);

    /**
     * Verifica si un usuario tiene acceso a una empresa
     * @param usuarioId ID del usuario
     * @param empresaId ID de la empresa
     * @return true si tiene acceso
     */
    boolean usuarioTieneAcceso(Long usuarioId, Long empresaId);

    Page<EmpresaResponse> listar(Pageable pageable);
}