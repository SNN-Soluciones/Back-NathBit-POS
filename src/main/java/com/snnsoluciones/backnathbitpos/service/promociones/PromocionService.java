package com.snnsoluciones.backnathbitpos.service.promociones;

import com.snnsoluciones.backnathbitpos.dto.promociones.CreatePromocionAlcanceRequest;
import com.snnsoluciones.backnathbitpos.dto.promociones.CreatePromocionItemRequest;
import com.snnsoluciones.backnathbitpos.dto.promociones.CreatePromocionRequest;
import com.snnsoluciones.backnathbitpos.dto.promociones.PromocionDTO;
import com.snnsoluciones.backnathbitpos.entity.Promocion;
import com.snnsoluciones.backnathbitpos.entity.PromocionCategoria;
import com.snnsoluciones.backnathbitpos.entity.PromocionFamilia;
import com.snnsoluciones.backnathbitpos.entity.PromocionItem;
import com.snnsoluciones.backnathbitpos.entity.PromocionProducto;
import com.snnsoluciones.backnathbitpos.repository.PromocionCategoriaRepository;
import com.snnsoluciones.backnathbitpos.repository.PromocionFamiliaRepository;
import com.snnsoluciones.backnathbitpos.repository.PromocionItemRepository;
import com.snnsoluciones.backnathbitpos.repository.PromocionProductoRepository;
import com.snnsoluciones.backnathbitpos.repository.PromocionRepository;
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

    private final PromocionRepository promocionRepository;
    private final PromocionItemRepository promocionItemRepository;
    private final PromocionFamiliaRepository promocionFamiliaRepository;
    private final PromocionCategoriaRepository promocionCategoriaRepository;
    private final PromocionProductoRepository promocionProductoRepository;

    // =========================================================================
    // LECTURA
    // =========================================================================

    @Transactional(readOnly = true)
    public List<PromocionDTO> listarTodas() {
        return promocionRepository.findActivasWithItems()
                .stream()
                .map(PromocionDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PromocionDTO obtenerPorId(Long id) {
        Promocion promo = promocionRepository.findByIdWithItems(id)
                .orElseThrow(() -> new IllegalArgumentException("Promoción no encontrada: " + id));
        return PromocionDTO.fromEntity(promo);
    }

    /**
     * Promos activas para un día comercial específico.
     * El frontend envía el día en mayúsculas: LUNES, MARTES... DOMINGO.
     */
    @Transactional(readOnly = true)
    public List<PromocionDTO> listarActivasPorDia(String dia) {
        return promocionRepository.findActivasByDia(dia.toUpperCase())
                .stream()
                .map(PromocionDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Promos activas para un día y hora específicos.
     * Cubre el caso donde horaInicio/horaFin son NULL (aplica todo el día).
     */
    @Transactional(readOnly = true)
    public List<PromocionDTO> listarActivasPorDiaYHora(String dia, LocalTime hora) {
        return promocionRepository.findActivasByDiaYHora(dia.toUpperCase(), hora)
                .stream()
                .map(PromocionDTO::fromEntity)
                .collect(Collectors.toList());
    }

    // =========================================================================
    // CREACIÓN
    // =========================================================================

    @Transactional
    public PromocionDTO crear(CreatePromocionRequest request) {
        log.info("Creando promoción: {}", request.getNombre());

        validarRequest(request);

        Promocion promo = Promocion.builder()
                .nombre(request.getNombre())
                .descripcion(request.getDescripcion())
                .tipo(request.getTipo())
                .activo(request.getActivo() != null ? request.getActivo() : true)
                .lunes(bool(request.getLunes()))
                .martes(bool(request.getMartes()))
                .miercoles(bool(request.getMiercoles()))
                .jueves(bool(request.getJueves()))
                .viernes(bool(request.getViernes()))
                .sabado(bool(request.getSabado()))
                .domingo(bool(request.getDomingo()))
                .horaInicio(request.getHoraInicio())
                .horaFin(request.getHoraFin())
                .llevaN(request.getLlevaN())
                .pagaM(request.getPagaM())
                .porcentajeDescuento(request.getPorcentajeDescuento())
                .montoDescuento(request.getMontoDescuento())
                .precioPromo(request.getPrecioPromo())
                .build();

        Promocion saved = promocionRepository.save(promo);
        log.info("Promoción creada con ID: {}", saved.getId());

        // Items (ALL_YOU_CAN_EAT / BARRA_LIBRE / ESPECIAL)
        if (request.getItems() != null && !request.getItems().isEmpty()) {
            guardarItems(saved, request.getItems());
        }

        // Alcance
        if (request.getFamilias() != null && !request.getFamilias().isEmpty()) {
            guardarFamilias(saved, request.getFamilias());
        }
        if (request.getCategorias() != null && !request.getCategorias().isEmpty()) {
            guardarCategorias(saved, request.getCategorias());
        }
        if (request.getProductos() != null && !request.getProductos().isEmpty()) {
            guardarProductos(saved, request.getProductos());
        }

        return PromocionDTO.fromEntity(promocionRepository.findByIdWithItems(saved.getId()).orElseThrow());
    }

    // =========================================================================
    // ACTUALIZACIÓN (reemplazo completo de alcance e items)
    // =========================================================================

    @Transactional
    public PromocionDTO actualizar(Long id, CreatePromocionRequest request) {
        log.info("Actualizando promoción ID: {}", id);

        Promocion promo = promocionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Promoción no encontrada: " + id));

        validarRequest(request);

        promo.setNombre(request.getNombre());
        promo.setDescripcion(request.getDescripcion());
        promo.setTipo(request.getTipo());
        promo.setActivo(bool(request.getActivo()));
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
        promo.setPorcentajeDescuento(request.getPorcentajeDescuento());
        promo.setMontoDescuento(request.getMontoDescuento());
        promo.setPrecioPromo(request.getPrecioPromo());

        promocionRepository.save(promo);

        // Reemplazar items
        promocionItemRepository.deleteByPromocionId(id);
        if (request.getItems() != null && !request.getItems().isEmpty()) {
            guardarItems(promo, request.getItems());
        }

        // Reemplazar alcance
        promocionFamiliaRepository.deleteByPromocionId(id);
        if (request.getFamilias() != null && !request.getFamilias().isEmpty()) {
            guardarFamilias(promo, request.getFamilias());
        }

        promocionCategoriaRepository.deleteByPromocionId(id);
        if (request.getCategorias() != null && !request.getCategorias().isEmpty()) {
            guardarCategorias(promo, request.getCategorias());
        }

        promocionProductoRepository.deleteByPromocionId(id);
        if (request.getProductos() != null && !request.getProductos().isEmpty()) {
            guardarProductos(promo, request.getProductos());
        }

        log.info("Promoción ID {} actualizada", id);
        return PromocionDTO.fromEntity(promocionRepository.findByIdWithItems(id).orElseThrow());
    }

    // =========================================================================
    // ACTIVAR / DESACTIVAR
    // =========================================================================

    @Transactional
    public PromocionDTO cambiarEstado(Long id, Boolean activo) {
        Promocion promo = promocionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Promoción no encontrada: " + id));
        promo.setActivo(activo);
        promocionRepository.save(promo);
        log.info("Promoción ID {} → activo={}", id, activo);
        return PromocionDTO.fromEntity(promo);
    }

    /**
     * Devuelve todas las promociones activas que aplican a un producto
     * en este momento (día comercial + hora actual).
     *
     * El frontend recibe la lista y decide cuál aplicar al carrito.
     *
     * @param productoId  ID del producto agregado al carrito
     * @param categoriaId ID de la categoría del producto (puede ser null)
     * @param familiaId   ID de la familia del producto (puede ser null)
     * @param dia         Día comercial actual en mayúsculas: LUNES...DOMINGO
     * @param hora        Hora actual (null = no filtrar por hora)
     */
    @Transactional(readOnly = true)
    public List<PromocionDTO> buscarParaProducto(
        Long productoId,
        Long categoriaId,
        Long familiaId,
        String dia,
        LocalTime hora) {

        // Usamos -1L como centinela cuando categoriaId o familiaId son null
        // para que el EXISTS no matchee nada (ningún ID real será -1)
        Long catId = categoriaId != null ? categoriaId : -1L;
        Long famId = familiaId   != null ? familiaId   : -1L;

        return promocionRepository.findPromocionesParaProducto(
                productoId,
                catId,
                famId,
                dia.toUpperCase(),
                hora)
            .stream()
            .map(PromocionDTO::fromEntity)
            .collect(Collectors.toList());
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
                        .build())
                .collect(Collectors.toList());
        promocionProductoRepository.saveAll(productos);
    }

    // =========================================================================
    // PRIVADOS — validación
    // =========================================================================

    private void validarRequest(CreatePromocionRequest r) {
        // Al menos un día activo
        boolean tieneDia = bool(r.getLunes()) || bool(r.getMartes()) || bool(r.getMiercoles())
                || bool(r.getJueves()) || bool(r.getViernes()) || bool(r.getSabado()) || bool(r.getDomingo());
        if (!tieneDia) {
            throw new IllegalArgumentException("La promoción debe tener al menos un día activo.");
        }

        // Horario: si uno viene el otro es obligatorio
        if ((r.getHoraInicio() == null) != (r.getHoraFin() == null)) {
            throw new IllegalArgumentException("hora_inicio y hora_fin deben venir juntos o ambos vacíos.");
        }
        if (r.getHoraInicio() != null && !r.getHoraFin().isAfter(r.getHoraInicio())) {
            throw new IllegalArgumentException("hora_fin debe ser posterior a hora_inicio.");
        }

        // Validación cruzada por tipo
        switch (r.getTipo()) {
            case NXM -> {
                if (r.getLlevaN() == null || r.getPagaM() == null)
                    throw new IllegalArgumentException("NXM requiere lleva_n y paga_m.");
                if (r.getLlevaN() <= r.getPagaM())
                    throw new IllegalArgumentException("lleva_n debe ser mayor que paga_m.");
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
            }
            case HAPPY_HOUR -> {
                if (r.getHoraInicio() == null)
                    throw new IllegalArgumentException("HAPPY_HOUR requiere hora_inicio y hora_fin.");
            }
            case ESPECIAL -> { /* sin restricciones adicionales */ }
        }
    }

    private boolean bool(Boolean value) {
        return value != null && value;
    }
}