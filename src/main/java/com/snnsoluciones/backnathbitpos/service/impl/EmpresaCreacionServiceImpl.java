package com.snnsoluciones.backnathbitpos.service.impl;

import com.snnsoluciones.backnathbitpos.dto.confighacienda.ActividadEconomicaRequest;
import com.snnsoluciones.backnathbitpos.dto.confighacienda.ConfigHaciendaRequest;
import com.snnsoluciones.backnathbitpos.dto.empresa.CrearEmpresaCompletaRequest;
import com.snnsoluciones.backnathbitpos.dto.empresa.CrearEmpresaCompletaResponse;
import com.snnsoluciones.backnathbitpos.entity.*;
import com.snnsoluciones.backnathbitpos.repository.ActividadEconomicaRepository;
import com.snnsoluciones.backnathbitpos.repository.EmpresaActividadRepository;
import com.snnsoluciones.backnathbitpos.repository.EmpresaRepository;
import com.snnsoluciones.backnathbitpos.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmpresaCreacionServiceImpl implements EmpresaCreacionService {
    
    private final EmpresaRepository empresaRepository;
    private final EmpresaService empresaService;
    private final ConfigHaciendaService configHaciendaService;
    private final StorageService storageService;
    private final CertificadoService certificadoService;
    private final ActividadEconomicaRepository actividadEconomicaRepository;
    private final EmpresaActividadRepository empresaActividadRepository;
    private final UbicacionService ubicacionService;

    @Override
    @Transactional
    public CrearEmpresaCompletaResponse crearEmpresaCompleta(
            CrearEmpresaCompletaRequest request,
            MultipartFile logo,
            MultipartFile certificado) {
        
        log.info("Iniciando creación completa de empresa: {}", request.getNombreComercial());
        
        try {
            // 1. Validaciones previas
            validarDatosEmpresa(request);
            
            // 2. Preparar nombre base para archivos (nombre_comercial limpio)
            String nombreBase = prepararNombreBase(
                request.getNombreComercial() != null ? request.getNombreComercial() : request.getNombre()
            );
            
            // 3. Procesar archivos ANTES de crear la empresa
            String logoUrl = null;
            String certificadoUrl = null;
            
            // 3.1 Subir logo si existe
            if (logo != null && !logo.isEmpty()) {
                log.info("Subiendo logo para empresa: {}", nombreBase);
                logoUrl = subirLogo(logo, nombreBase);
            }
            
            // 3.2 Validar y subir certificado si requiere Hacienda
            if (request.getRequiereHacienda() && certificado != null) {
                log.info("Validando y subiendo certificado P12");
                
                // Validar PIN y certificado
                String pin = request.getConfigHacienda().getPinCertificado();
                if (pin == null || pin.isEmpty()) {
                    throw new RuntimeException("El PIN del certificado es requerido");
                }
                
                // Validar certificado con PIN
                boolean certificadoValido = certificadoService.validarCertificado(certificado, pin);
                if (!certificadoValido) {
                    throw new RuntimeException("El certificado no es válido o el PIN es incorrecto");
                }
                
                // Subir certificado
                certificadoUrl = subirCertificado(certificado, nombreBase);
            }
            
            // 4. Crear empresa con URLs ya listas
            Empresa empresa = crearEmpresa(request, logoUrl);
            
            // 5. Si requiere Hacienda, crear configuración y actividades
            if (request.getRequiereHacienda() && request.getConfigHacienda() != null) {
                crearConfiguracionHacienda(empresa, request.getConfigHacienda(), certificadoUrl);
                crearActividadesEconomicas(empresa, request.getConfigHacienda().getActividades());
            }
            
            // 6. Construir respuesta
            return CrearEmpresaCompletaResponse.builder()
                .empresaId(empresa.getId())
                .nombre(empresa.getNombre())
                .nombreComercial(empresa.getNombreComercial())
                .identificacion(empresa.getIdentificacion())
                .logoUrl(empresa.getLogoUrl())
                .requiereHacienda(empresa.getRequiereHacienda())
                .mensaje("Empresa creada exitosamente")
                .build();
                
        } catch (Exception e) {
            log.error("Error creando empresa completa: ", e);
            // La transacción hará rollback automáticamente
            // TODO: Implementar limpieza de archivos subidos si falla
            throw new RuntimeException("Error al crear la empresa: " + e.getMessage(), e);
        }
    }
    
    private void validarDatosEmpresa(CrearEmpresaCompletaRequest request) {
        // Validar que no exista otra empresa con la misma identificación
        if (empresaRepository.existsByIdentificacion(request.getIdentificacion())) {
            throw new RuntimeException("Ya existe una empresa con esa identificación");
        }
        
        // Si requiere Hacienda, validar datos obligatorios
        if (request.getRequiereHacienda()) {
            if (request.getConfigHacienda() == null) {
                throw new RuntimeException("La configuración de Hacienda es requerida");
            }
            
            if (request.getConfigHacienda().getActividades() == null || 
                request.getConfigHacienda().getActividades().isEmpty()) {
                throw new RuntimeException("Debe seleccionar al menos una actividad económica");
            }
        }
    }
    
    private String prepararNombreBase(String nombreComercial) {
        // Normalizar el nombre: quitar acentos, espacios por _, minúsculas
        String normalizado = Normalizer.normalize(nombreComercial, Normalizer.Form.NFD)
            .replaceAll("[^\\p{ASCII}]", "") // Quitar caracteres no ASCII
            .replaceAll("[^a-zA-Z0-9\\s]", "") // Quitar caracteres especiales
            .trim()
            .replaceAll("\\s+", "_") // Espacios por _
            .toLowerCase();
            
        return normalizado;
    }
    
    private String subirLogo(MultipartFile logo, String nombreBase) {
        String nombreArchivo = nombreBase + "_logo";
        String carpeta = "empresas/logos";
        
        return storageService.subirArchivo(logo, carpeta, nombreArchivo, true);
    }
    
    private String subirCertificado(MultipartFile certificado, String nombreBase) {
        String nombreArchivo = nombreBase + "_certificado";
        String carpeta = "empresas/certificados";
        
        return storageService.subirArchivo(certificado, carpeta, nombreArchivo, true);
    }
    
    private Empresa crearEmpresa(CrearEmpresaCompletaRequest request, String logoUrl) {
        Empresa empresa = new Empresa();
        
        // Datos básicos
        empresa.setNombre(request.getNombre());
        empresa.setNombreComercial(request.getNombreComercial());
        empresa.setNombreRazonSocial(request.getNombre()); // Por defecto igual al nombre
        empresa.setTipoIdentificacion(request.getTipoIdentificacion());
        empresa.setIdentificacion(request.getIdentificacion());
        
        // Contacto
        empresa.setEmail(request.getEmail());
        empresa.setEmailNotificacion(request.getEmailNotificacion());
        empresa.setTelefono(request.getTelefono());
        empresa.setFax(request.getFax());
        
        // Ubicación
        empresa.setProvincia(ubicacionService.buscarProvinciaPorId(request.getProvincia()).orElse(null));
        empresa.setCanton(ubicacionService.buscarCantonPorId(request.getCanton()).orElse(null));
        empresa.setDistrito(ubicacionService.buscarDistritoPorId(request.getDistrito()).orElse(null));
        empresa.setBarrio(ubicacionService.buscarBarrioPorId(request.getBarrio()).orElse(null));
        empresa.setOtrasSenas(request.getOtrasSenas());
        
        // Configuración
        empresa.setActiva(request.getActiva());
        empresa.setRequiereHacienda(request.getRequiereHacienda());
        empresa.setRegimenTributario(request.getRegimenTributario());
        
        // Logo
        empresa.setLogoUrl(logoUrl);
        
        return empresaRepository.save(empresa);
    }
    
    private void crearConfiguracionHacienda(Empresa empresa, 
            CrearEmpresaCompletaRequest.ConfigHaciendaData configData, String certificadoUrl) {
        
        ConfigHaciendaRequest configRequest = new ConfigHaciendaRequest();
        configRequest.setEmpresaId(empresa.getId());
        configRequest.setAmbiente(configData.getAmbiente());
        configRequest.setUsuarioHacienda(configData.getUsuarioHacienda());
        configRequest.setClaveHacienda(configData.getClaveHacienda());
        configRequest.setUrlCertificadoKey(certificadoUrl);
        configRequest.setPinCertificado(configData.getPinCertificado());
        configRequest.setNotaFactura(configData.getNotaFactura());
        configRequest.setNotaValidezProforma(configData.getNotaValidezProforma());
        configRequest.setDetalleFactura1(configData.getDetalleFactura1());
        configRequest.setDetalleFactura2(configData.getDetalleFactura2());
        
        configHaciendaService.crearOActualizar(configRequest);
    }
    
    private void crearActividadesEconomicas(Empresa empresa, 
            List<ActividadEconomicaRequest> actividadesRequest) {
        
        List<EmpresaActividad> actividades = new ArrayList<>();
        
        for (int i = 0; i < actividadesRequest.size(); i++) {
            ActividadEconomicaRequest actRequest = actividadesRequest.get(i);
            
            // Buscar la actividad económica
            ActividadEconomica actividad = actividadEconomicaRepository
                .findByCodigo(actRequest.getCodigo())
                .orElseThrow(() -> new RuntimeException(
                    "Actividad económica no encontrada: " + actRequest.getCodigo()));
            
            // Crear relación empresa-actividad
            EmpresaActividad empresaActividad = new EmpresaActividad();
            empresaActividad.setEmpresa(empresa);
            empresaActividad.setActividad(actividad);
            empresaActividad.setEsPrincipal(i == 0); // La primera es principal
            empresaActividad.setOrden(i);
            
            actividades.add(empresaActividad);
        }
        
        empresaActividadRepository.saveAll(actividades);
    }
}