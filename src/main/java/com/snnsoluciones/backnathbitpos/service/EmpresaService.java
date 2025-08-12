package com.snnsoluciones.backnathbitpos.service;

import com.snnsoluciones.backnathbitpos.dto.empresa.CertificadoResponse;
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

    /**
     * Sube el certificado P12 de una empresa
     * @param empresaId ID de la empresa
     * @param certificado archivo del certificado
     * @param pin PIN del certificado
     * @return información del certificado procesado
     */
    CertificadoResponse subirCertificado(Long empresaId, MultipartFile certificado, String pin);

    /**
     * Genera una URL temporal para descargar el certificado
     * @param empresaId ID de la empresa
     * @return URL pre-firmada con tiempo de expiración
     */
    UrlCertificadoResponse generarUrlCertificado(Long empresaId);

    /**
     * Elimina el certificado de una empresa
     * @param empresaId ID de la empresa
     */
    void eliminarCertificado(Long empresaId);

    /**
     * Sube el logo de una empresa
     * @param empresaId ID de la empresa
     * @param logo archivo del logo
     * @return URL pública del logo
     */
    String subirLogo(Long empresaId, MultipartFile logo);

    /**
     * Elimina el logo de una empresa
     * @param empresaId ID de la empresa
     */
    void eliminarLogo(Long empresaId);
}