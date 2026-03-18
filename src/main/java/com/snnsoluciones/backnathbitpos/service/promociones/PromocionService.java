package com.snnsoluciones.backnathbitpos.service.promociones;

import com.snnsoluciones.backnathbitpos.dto.promociones.CreatePromocionAlcanceRequest;
import com.snnsoluciones.backnathbitpos.dto.promociones.CreatePromocionItemRequest;
import com.snnsoluciones.backnathbitpos.dto.promociones.CreatePromocionRequest;
import com.snnsoluciones.backnathbitpos.dto.promociones.PromocionDTO;
import com.snnsoluciones.backnathbitpos.entity.Empresa;
import com.snnsoluciones.backnathbitpos.entity.Promocion;
import com.snnsoluciones.backnathbitpos.entity.PromocionCategoria;
import com.snnsoluciones.backnathbitpos.entity.PromocionFamilia;
import com.snnsoluciones.backnathbitpos.entity.PromocionItem;
import com.snnsoluciones.backnathbitpos.entity.PromocionProducto;
import com.snnsoluciones.backnathbitpos.entity.Sucursal;
import com.snnsoluciones.backnathbitpos.enums.CriterioDescuento;
import com.snnsoluciones.backnathbitpos.enums.RolPromocionAlcance;
import com.snnsoluciones.backnathbitpos.repository.EmpresaRepository;
import com.snnsoluciones.backnathbitpos.repository.PromocionCategoriaRepository;
import com.snnsoluciones.backnathbitpos.repository.PromocionFamiliaRepository;
import com.snnsoluciones.backnathbitpos.repository.PromocionItemRepository;
import com.snnsoluciones.backnathbitpos.repository.PromocionProductoRepository;
import com.snnsoluciones.backnathbitpos.repository.PromocionRepository;
import com.snnsoluciones.backnathbitpos.repository.SucursalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PromocionService {

    private final PromocionRepository          promocionRepository;
    private final PromocionItemRepository      promocionItemRepository;
    private final PromocionFamiliaRepository   promocionFamiliaRepository;
    private final PromocionCategoriaRepository promocionCategoriaRepository;
    private final PromocionProductoRepository  promocionProductoRepository;
    private final EmpresaRepository            empresaRepository;
    private final SucursalRepository           sucursalRepository;

    // =========================================================================
    // LECTURA
    // =========================================================================

    @Transactional(readOnly = true)
    public List<PromocionDTO> listarTodas(Long empresaId, Long sucursalId) {
        return promocionRepository.findActivasWithItems(empresaId, sucursalId)
            .stream()
            .map(PromocionDTO::fromEntity)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PromocionDTO obtenerPorId(Long id, Long empresaId) {
        Promocion promo = promocionRepository.findByIdWithItems(id, empresaId)
            .orElseThrow(() -> new IllegalArgumentException("Promoción no encontrada: " + id));
        return PromocionDTO.fromEntity(promo);
    }

    @Transactional(readOnly = true)
    public List<PromocionDTO> listarActivasPorDia(Long empresaId, Long sucursalId, String dia) {
        return promocionRepository.findActivasByDia(empresaId, sucursalId, dia.toUpperCase())
            .stream()
            .map(PromocionDTO::fromEntity)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PromocionDTO> listarActivasPorDiaYHora(
            Long empresaId, Long sucursalId, String dia, LocalTime hora) {
        return promocionRepository.findActivasByDiaYHora(empresaId, sucursalId, dia.toUpperCase(), hora)
            .stream()
            .map(PromocionDTO::fromEntity)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PromocionDTO> buscarParaProducto(
            Long empresaId, Long sucursalId,
            Long productoId, Long categoriaId, Long familiaId,
            String dia, LocalTime hora) {

        // -1L como centinela para IDs opcionales nulos — la query los ignora
        Long catId = categoriaId != null ? categoriaId : -1L;
        Long famId = familiaId   != null ? familiaId   : -1L;

        return promocionRepository.findPromocionesParaProducto(
                empresaId, sucursalId, productoId, catId, famId, dia.toUpperCase(), hora)
            .stream()
            .map(PromocionDTO::fromEntity)
            .collect(Collectors.toList());
    }

    // =========================================================================
    // CREACIÓN
    // =========================================================================

    @Transactional
    public PromocionDTO crear(Long empresaId, Long sucursalId, CreatePromocionRequest request) {
        log.info("Creando promoción '{}' para empresa={} sucursal={}", request.getNombre(), empresaId, sucursalId);

        validarRequest(request);

        Empresa empresa = empresaRepository.findById(empresaId)
            .orElseThrow(() -> new IllegalArgumentException("Empresa no encontrada: " + empresaId));

        // sucursalId null = promo global de empresa
        Sucursal sucursal = null;
        if (sucursalId != null) {
            sucursal = sucursalRepository.findById(sucursalId)
                .orElseThrow(() -> new IllegalArgumentException("Sucursal no encontrada: " + sucursalId));
        }

        Promocion promo = Promocion.builder()
            // Tenant
            .empresa(empresa)
            .sucursal(sucursal)
            // Identificación
            .nombre(request.getNombre())
            .descripcion(request.getDescripcion())
            .tipo(request.getTipo())
            .activo(bool(request.getActivo()))
            // Vigencia
            .fechaInicio(request.getFechaInicio())
            .fechaFin(request.getFechaFin())
            // Stacking
            .permitirStack(bool(request.getPermitirStack()))
            // Días
            .lunes(bool(request.getLunes()))
            .martes(bool(request.getMartes()))
            .miercoles(bool(request.getMiercoles()))
            .jueves(bool(request.getJueves()))
            .viernes(bool(request.getViernes()))
            .sabado(bool(request.getSabado()))
            .domingo(bool(request.getDomingo()))
            // Horario
            .horaInicio(request.getHoraInicio())
            .horaFin(request.getHoraFin())
            // NXM
            .llevaN(request.getLlevaN())
            .pagaM(request.getPagaM())
            .criterioItemGratis(request.getCriterioItemGratis())
            // Descuentos simples
            .porcentajeDescuento(request.getPorcentajeDescuento())
            .montoDescuento(request.getMontoDescuento())
            // AYCE / Barra libre
            .precioPromo(request.getPrecioPromo())
            // Grupo condicional
            .cantidadTrigger(request.getCantidadTrigger())
            .cantidadBeneficio(request.getCantidadBeneficio())
            .criterioBeneficio(request.getCriterioBeneficio())
            .valorBeneficio(request.getValorBeneficio())
            .build();

        Promocion saved = promocionRepository.save(promo);
        log.info("Promoción creada con ID: {}", saved.getId());

        if (request.getItems()      != null && !request.getItems().isEmpty())
            guardarItems(saved, request.getItems());
        if (request.getFamilias()   != null && !request.getFamilias().isEmpty())
            guardarFamilias(saved, request.getFamilias());
        if (request.getCategorias() != null && !request.getCategorias().isEmpty())
            guardarCategorias(saved, request.getCategorias());
        if (request.getProductos()  != null && !request.getProductos().isEmpty())
            guardarProductos(saved, request.getProductos());

        return PromocionDTO.fromEntity(
            promocionRepository.findByIdWithItems(saved.getId(), empresaId).orElseThrow());
    }

    // =========================================================================
    // ACTUALIZACIÓN
    // =========================================================================

    @Transactional
    public PromocionDTO actualizar(Long id, Long empresaId, CreatePromocionRequest request) {
        log.info("Actualizando promoción ID: {} empresa: {}", id, empresaId);

        // Verificamos que la promo pertenezca a esta empresa
        Promocion promo = promocionRepository.findByIdWithItems(id, empresaId)
            .orElseThrow(() -> new IllegalArgumentException("Promoción no encontrada: " + id));

        validarRequest(request);

        promo.setNombre(request.getNombre());
        promo.setDescripcion(request.getDescripcion());
        promo.setTipo(request.getTipo());
        promo.setActivo(bool(request.getActivo()));
        promo.setFechaInicio(request.getFechaInicio());
        promo.setFechaFin(request.getFechaFin());
        promo.setPermitirStack(bool(request.getPermitirStack()));
        promo.setLunes(bool(request.getLunes()));
        promo.setMartes(bool(request.getMartes()));
        promo.setMiercoles(bool(request.getMiercoles()));
        promo.setJueves(bool(request.getJueves()));
        promo.setViernes(bool(request.getViernes()));
        promo.setSabado(bool(request.getSabado()));
        promo.setDomingo(bool(request.getDomingo()));
        promo.setHoraInicio(request.getHoraInicio());
        promo.setHoraFin(request.getHoraFin());
        promo.setLlevaN(request.getLlevaN());
        promo.setPagaM(request.getPagaM());
        promo.setCriterioItemGratis(request.getCriterioItemGratis());
        promo.setPorcentajeDescuento(request.getPorcentajeDescuento());
        promo.setMontoDescuento(request.getMontoDescuento());
        promo.setPrecioPromo(request.getPrecioPromo());
        promo.setCantidadTrigger(request.getCantidadTrigger());
        promo.setCantidadBeneficio(request.getCantidadBeneficio());
        promo.setCriterioBeneficio(request.getCriterioBeneficio());
        promo.setValorBeneficio(request.getValorBeneficio());

        promocionRepository.save(promo);

        // Reemplazar colecciones completas
        promocionItemRepository.deleteByPromocionId(id);
        if (request.getItems()      != null && !request.getItems().isEmpty())
            guardarItems(promo, request.getItems());

        promocionFamiliaRepository.deleteByPromocionId(id);
        if (request.getFamilias()   != null && !request.getFamilias().isEmpty())
            guardarFamilias(promo, request.getFamilias());

        promocionCategoriaRepository.deleteByPromocionId(id);
        if (request.getCategorias() != null && !request.getCategorias().isEmpty())
            guardarCategorias(promo, request.getCategorias());

        promocionProductoRepository.deleteByPromocionId(id);
        if (request.getProductos()  != null && !request.getProductos().isEmpty())
            guardarProductos(promo, request.getProductos());

        log.info("Promoción ID {} actualizada", id);
        return PromocionDTO.fromEntity(
            promocionRepository.findByIdWithItems(id, empresaId).orElseThrow());
    }

    // =========================================================================
    // ACTIVAR / DESACTIVAR
    // =========================================================================

    @Transactional
    public PromocionDTO cambiarEstado(Long id, Long empresaId, Boolean activo) {
        Promocion promo = promocionRepository.findByIdWithItems(id, empresaId)
            .orElseThrow(() -> new IllegalArgumentException("Promoción no encontrada: " + id));
        promo.setActivo(activo);
        promocionRepository.save(promo);
        log.info("Promoción ID {} → activo={}", id, activo);
        return PromocionDTO.fromEntity(promo);
    }

    // =========================================================================
    // PRIVADOS — guardar colecciones
    // =========================================================================

    private void guardarItems(Promocion promo, List<CreatePromocionItemRequest> requests) {
        List<PromocionItem> items = requests.stream()
            .map(r -> PromocionItem.builder()
                .promocion(promo)
                .productoId(r.getProductoId())
                .nombreProducto(r.getNombreProducto())
                .cantidadPorRonda(r.getCantidadPorRonda())
                .maxRondas(r.getMaxRondas())
                .build())
            .collect(Collectors.toList());
        promocionItemRepository.saveAll(items);
    }

    private void guardarFamilias(Promocion promo, List<CreatePromocionAlcanceRequest> requests) {
        List<PromocionFamilia> familias = requests.stream()
            .map(r -> PromocionFamilia.builder()
                .promocion(promo)
                .familiaId(r.getId())
                .nombreFamilia(r.getNombre())
                .rol(r.getRol() != null ? r.getRol() : RolPromocionAlcance.TRIGGER)
                .build())
            .collect(Collectors.toList());
        promocionFamiliaRepository.saveAll(familias);
    }

    private void guardarCategorias(Promocion promo, List<CreatePromocionAlcanceRequest> requests) {
        List<PromocionCategoria> categorias = requests.stream()
            .map(r -> PromocionCategoria.builder()
                .promocion(promo)
                .categoriaId(r.getId())
                .nombreCategoria(r.getNombre())
                .rol(r.getRol() != null ? r.getRol() : RolPromocionAlcance.TRIGGER)
                .build())
            .collect(Collectors.toList());
        promocionCategoriaRepository.saveAll(categorias);
    }

    private void guardarProductos(Promocion promo, List<CreatePromocionAlcanceRequest> requests) {
        List<PromocionProducto> productos = requests.stream()
            .map(r -> PromocionProducto.builder()
                .promocion(promo)
                .productoId(r.getId())
                .nombreProducto(r.getNombre())
                .rol(r.getRol() != null ? r.getRol() : RolPromocionAlcance.TRIGGER)
                .build())
            .collect(Collectors.toList());
        promocionProductoRepository.saveAll(productos);
    }

    // =========================================================================
    // PRIVADOS — validación
    // =========================================================================

    private void validarRequest(CreatePromocionRequest r) {
        boolean tieneDia = bool(r.getLunes()) || bool(r.getMartes()) || bool(r.getMiercoles())
            || bool(r.getJueves()) || bool(r.getViernes()) || bool(r.getSabado()) || bool(r.getDomingo());
        if (!tieneDia)
            throw new IllegalArgumentException("La promoción debe tener al menos un día activo.");

        if ((r.getFechaInicio() == null) != (r.getFechaFin() == null))
            throw new IllegalArgumentException("fecha_inicio y fecha_fin deben venir juntos o ambos vacíos.");
        if (r.getFechaInicio() != null && r.getFechaFin().isBefore(r.getFechaInicio()))
            throw new IllegalArgumentException("fecha_fin debe ser igual o posterior a fecha_inicio.");

        if ((r.getHoraInicio() == null) != (r.getHoraFin() == null))
            throw new IllegalArgumentException("hora_inicio y hora_fin deben venir juntos o ambos vacíos.");
        if (r.getHoraInicio() != null && !r.getHoraFin().isAfter(r.getHoraInicio()))
            throw new IllegalArgumentException("hora_fin debe ser posterior a hora_inicio.");

        switch (r.getTipo()) {
            case NXM -> {
                if (r.getLlevaN() == null || r.getPagaM() == null)
                    throw new IllegalArgumentException("NXM requiere lleva_n y paga_m.");
                if (r.getLlevaN() <= r.getPagaM())
                    throw new IllegalArgumentException("lleva_n debe ser mayor que paga_m.");
                if (r.getCriterioItemGratis() == null)
                    throw new IllegalArgumentException("NXM requiere criterio_item_gratis.");
            }
            case PORCENTAJE -> {
                if (r.getPorcentajeDescuento() == null)
                    throw new IllegalArgumentException("PORCENTAJE requiere porcentaje_descuento.");
            }
            case MONTO_FIJO -> {
                if (r.getMontoDescuento() == null)
                    throw new IllegalArgumentException("MONTO_FIJO requiere monto_descuento.");
            }
            case BARRA_LIBRE, ALL_YOU_CAN_EAT -> {
                if (r.getPrecioPromo() == null)
                    throw new IllegalArgumentException(r.getTipo() + " requiere precio_promo.");
                if (r.getItems() == null || r.getItems().isEmpty())
                    throw new IllegalArgumentException(r.getTipo() + " requiere al menos un ítem.");
                boolean tieneTrigger = r.getProductos() != null && r.getProductos().stream()
                    .anyMatch(p -> p.getRol() == RolPromocionAlcance.TRIGGER);
                if (!tieneTrigger)
                    throw new IllegalArgumentException(r.getTipo() + " requiere al menos un producto con rol TRIGGER.");
            }
            case GRUPO_CONDICIONAL -> {
                if (r.getCantidadTrigger() == null)
                    throw new IllegalArgumentException("GRUPO_CONDICIONAL requiere cantidad_trigger.");
                if (r.getCantidadBeneficio() == null)
                    throw new IllegalArgumentException("GRUPO_CONDICIONAL requiere cantidad_beneficio.");
                if (r.getCriterioBeneficio() == null)
                    throw new IllegalArgumentException("GRUPO_CONDICIONAL requiere criterio_beneficio.");
                if (r.getCriterioBeneficio() != CriterioDescuento.GRATIS && r.getValorBeneficio() == null)
                    throw new IllegalArgumentException("criterio_beneficio " + r.getCriterioBeneficio() + " requiere valor_beneficio.");
                if (!tieneRol(r, RolPromocionAlcance.TRIGGER))
                    throw new IllegalArgumentException("GRUPO_CONDICIONAL requiere al menos un alcance con rol TRIGGER.");
                if (!tieneRol(r, RolPromocionAlcance.BENEFICIO))
                    throw new IllegalArgumentException("GRUPO_CONDICIONAL requiere al menos un alcance con rol BENEFICIO.");
            }
            case HAPPY_HOUR -> {
                if (r.getHoraInicio() == null)
                    throw new IllegalArgumentException("HAPPY_HOUR requiere hora_inicio y hora_fin.");
            }
            case ESPECIAL -> { /* sin restricciones adicionales */ }
        }
    }

    private boolean tieneRol(CreatePromocionRequest r, RolPromocionAlcance rol) {
        return (r.getFamilias()   != null && r.getFamilias().stream().anyMatch(x -> x.getRol() == rol))
            || (r.getCategorias() != null && r.getCategorias().stream().anyMatch(x -> x.getRol() == rol))
            || (r.getProductos()  != null && r.getProductos().stream().anyMatch(x -> x.getRol() == rol));
    }

    private boolean bool(Boolean value) {
        return value != null && value;
    }
}