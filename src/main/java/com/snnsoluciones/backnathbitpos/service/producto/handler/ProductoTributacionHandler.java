package com.snnsoluciones.backnathbitpos.service.producto.handler;

import com.snnsoluciones.backnathbitpos.dto.producto.CrearImpuestoDto;
import com.snnsoluciones.backnathbitpos.dto.producto.ProductoCreateDto;
import com.snnsoluciones.backnathbitpos.entity.*;
import com.snnsoluciones.backnathbitpos.enums.ModoFacturacion;
import com.snnsoluciones.backnathbitpos.enums.ZonaPreparacion;
import com.snnsoluciones.backnathbitpos.enums.mh.CodigoTarifaIVA;
import com.snnsoluciones.backnathbitpos.enums.mh.RegimenTributario;
import com.snnsoluciones.backnathbitpos.enums.mh.TipoImpuesto;
import com.snnsoluciones.backnathbitpos.enums.mh.UnidadMedida;
import com.snnsoluciones.backnathbitpos.exception.BusinessException;
import com.snnsoluciones.backnathbitpos.repository.CodigoCABySRepository;
import com.snnsoluciones.backnathbitpos.repository.EmpresaCABySRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Handler para gestionar la configuración tributaria de productos.
 * 
 * REGLAS DE NEGOCIO:
 * 
 * ✅ CONFIGURACIÓN SIMPLIFICADA (CABYS genérico + IVA exento):
 * 1. Sucursal SOLO_INTERNO (siempre)
 * 2. Sucursal MIXTO + Empresa SIMPLIFICADO
 * 3. Producto global + Empresa SIMPLIFICADO + NO requiere Hacienda
 * 
 * ❌ CONFIGURACIÓN TRADICIONAL (requiere CABYS + impuestos del usuario):
 * 1. Empresa TRADICIONAL (cualquier modo)
 * 2. Sucursal ELECTRONICO con cualquier régimen
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProductoTributacionHandler {

    private final EmpresaCABySRepository empresaCAbySRepository;
    private final CodigoCABySRepository codigoCAbySRepository;
    private final ProductoImpuestoHandler impuestoHandler;

    private static final String CODIGO_CABYS_GENERICO = "6332000000000";

    /**
     * Configura la tributación del producto según el régimen y modo de facturación.
     * 
     * @param producto Producto a configurar
     * @param dto DTO con datos del producto
     */
    @Transactional
    public void configurarTributacion(Producto producto, ProductoCreateDto dto) {
        Empresa empresa = producto.getEmpresa();
        Sucursal sucursal = producto.getSucursal();

        boolean aplicaSimplificada = determinarSiAplicaSimplificada(empresa, sucursal);

        if (aplicaSimplificada) {
            log.info("✅ Configurando producto con tributación SIMPLIFICADA");
            configurarSimplificada(producto, dto);
        } else {
            log.info("✅ Configurando producto con tributación TRADICIONAL");
            configurarTradicional(producto, dto);
        }
    }

    // ==================== MÉTODOS PRIVADOS ====================

    /**
     * Determina si aplica configuración simplificada según régimen y modo.
     */
    private boolean determinarSiAplicaSimplificada(Empresa empresa, Sucursal sucursal) {
        
        if (sucursal != null) {
            // ✅ CASO 1: Sucursal SOLO_INTERNO → Siempre simplificada
            if (sucursal.getModoFacturacion() == ModoFacturacion.SOLO_INTERNO) {
                log.debug("→ Simplificada por modo SOLO_INTERNO");
                return true;
            }
            
            // ✅ CASO 2: Sucursal MIXTO + Empresa SIMPLIFICADO → Simplificada
            if (sucursal.getModoFacturacion() == ModoFacturacion.MIXTO &&
                empresa.getRegimenTributario() == RegimenTributario.REGIMEN_SIMPLIFICADO) {
                log.debug("→ Simplificada por MIXTO + SIMPLIFICADO");
                return true;
            }
            
            // ❌ Cualquier otro caso con sucursal → Tradicional
            log.debug("→ Tradicional (sucursal {} + empresa {})", 
                sucursal.getModoFacturacion(), 
                empresa.getRegimenTributario());
            return false;
            
        } else {
            // ✅ CASO 3: Producto GLOBAL + Empresa SIMPLIFICADO + NO requiere Hacienda
            if (empresa.getRegimenTributario() == RegimenTributario.REGIMEN_SIMPLIFICADO &&
                Boolean.FALSE.equals(empresa.getRequiereHacienda())) {
                log.debug("→ Simplificada por producto global + SIMPLIFICADO + sin Hacienda");
                return true;
            }
            
            // ❌ Producto global con TRADICIONAL → Tradicional
            log.debug("→ Tradicional para producto global");
            return false;
        }
    }

    /**
     * Configura producto con CABYS genérico + IVA exento.
     */
    private void configurarSimplificada(Producto producto, ProductoCreateDto dto) {
        Empresa empresa = producto.getEmpresa();
        Sucursal sucursal = producto.getSucursal();

        // 1️⃣ BUSCAR O CREAR CABYS GENÉRICO
        EmpresaCAByS empresaCabys = buscarOCrearCabysGenerico(empresa, sucursal);
        producto.setEmpresaCabys(empresaCabys);

        // 2️⃣ ZONA DE PREPARACIÓN
        if (dto.getZonaPreparacion() != null) {
            try {
                producto.setZonaPreparacion(ZonaPreparacion.valueOf(dto.getZonaPreparacion()));
            } catch (IllegalArgumentException e) {
                throw new BusinessException("Zona de preparación inválida: " + dto.getZonaPreparacion());
            }
        } else {
            producto.setZonaPreparacion(ZonaPreparacion.NINGUNA);
        }

        // 3️⃣ UNIDAD DE MEDIDA
        producto.setUnidadMedida(UnidadMedida.SERVICIOS_PROFESIONALES);

        // 4️⃣ CREAR IMPUESTO IVA EXENTO
        CrearImpuestoDto impuestoDto = CrearImpuestoDto.builder()
            .tipo(TipoImpuesto.IVA)
            .codigoTarifa(CodigoTarifaIVA.TARIFA_EXENTA.getCodigo())
            .tarifa(BigDecimal.ZERO)
            .build();

        impuestoHandler.crearImpuestos(producto, List.of(impuestoDto));

        log.info("✅ Tributación simplificada configurada: CABYS genérico + IVA EXENTO");
    }

    /**
     * Configura producto tradicional (valida que vengan CABYS + impuestos).
     */
    private void configurarTradicional(Producto producto, ProductoCreateDto dto) {
        
        // ❌ VALIDAR QUE VENGAN DATOS REQUERIDOS
        if (dto.getEmpresaCabysId() == null) {
            throw new BusinessException("Régimen tradicional requiere código CABYS");
        }

        if (dto.getImpuestos() == null || dto.getImpuestos().isEmpty()) {
            throw new BusinessException("Régimen tradicional requiere configuración de impuestos");
        }

        if (dto.getUnidadMedida() == null) {
            throw new BusinessException("Régimen tradicional requiere unidad de medida");
        }

        // ✅ ASIGNAR CABYS AUTORIZADO
        EmpresaCAByS empresaCabys = empresaCAbySRepository.findById(dto.getEmpresaCabysId())
            .orElseThrow(() -> new BusinessException("CABYS no autorizado para esta empresa"));
        producto.setEmpresaCabys(empresaCabys);

        // ✅ UNIDAD DE MEDIDA
        producto.setUnidadMedida(dto.getUnidadMedida());

        // ✅ ZONA DE PREPARACIÓN (si viene)
        if (dto.getZonaPreparacion() != null) {
            try {
                producto.setZonaPreparacion(ZonaPreparacion.valueOf(dto.getZonaPreparacion()));
            } catch (IllegalArgumentException e) {
                throw new BusinessException("Zona de preparación inválida: " + dto.getZonaPreparacion());
            }
        }

        // ✅ CREAR IMPUESTOS (el handler los validará)
        impuestoHandler.crearImpuestos(producto, dto.getImpuestos());

        log.info("✅ Tributación tradicional configurada: CABYS específico + impuestos del usuario");
    }

    /**
     * Busca o crea el CABYS genérico para la empresa/sucursal.
     */
    private EmpresaCAByS buscarOCrearCabysGenerico(Empresa empresa, Sucursal sucursal) {
        EmpresaCAByS empresaCabys;

        if (sucursal != null) {
            // Buscar por sucursal
            empresaCabys = empresaCAbySRepository
                .findBySucursalIdAndCodigoCabysCodigoAndActivoTrue(sucursal.getId(), CODIGO_CABYS_GENERICO)
                .orElse(null);
        } else {
            // Buscar por empresa
            empresaCabys = empresaCAbySRepository
                .findByEmpresaIdAndCodigoCabysCodigoAndActivoTrue(empresa.getId(), CODIGO_CABYS_GENERICO)
                .orElse(null);
        }

        // Si no existe, crearlo
        if (empresaCabys == null) {
            CodigoCAByS codigoGenerico = codigoCAbySRepository.findByCodigo(CODIGO_CABYS_GENERICO)
                .orElseThrow(() -> new BusinessException(
                    "CABYS genérico '" + CODIGO_CABYS_GENERICO + "' no existe en el sistema"));

            empresaCabys = EmpresaCAByS.builder()
                .empresa(empresa)
                .sucursal(sucursal)  // Puede ser null para productos globales
                .codigoCabys(codigoGenerico)
                .activo(true)
                .createdAt(LocalDateTime.now())
                .build();

            empresaCabys = empresaCAbySRepository.save(empresaCabys);
            
            log.info("✅ CABYS genérico creado para {}", 
                sucursal != null ? "sucursal " + sucursal.getNombre() : "empresa " + empresa.getNombreComercial());
        }

        return empresaCabys;
    }
}