package com.snnsoluciones.backnathbitpos.service.promociones;

import com.snnsoluciones.backnathbitpos.dto.promociones.*;
import com.snnsoluciones.backnathbitpos.entity.*;
import com.snnsoluciones.backnathbitpos.repository.*;
import com.snnsoluciones.backnathbitpos.enums.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PromocionMotorService {

    private final OrdenRepository            ordenRepository;
    private final OrdenItemRepository        ordenItemRepository;
    private final PromocionRepository        promocionRepository;
    private final PromocionItemRepository    promocionItemRepository;
    private final OrdenPromocionEstadoRepository estadoRepository;

    // =========================================================================
    // EVALUAR — solo lectura, no persiste nada
    // Devuelve qué promos califican y qué descuento tendría cada ítem.
    // El frontend lo llama cada vez que cambia la orden.
    // =========================================================================

    @Transactional(readOnly = true)
    public List<PromocionAplicableDTO> evaluar(Long ordenId) {
        Orden orden = cargarOrden(ordenId);

        // Día y hora actuales para filtrar promos vigentes
        LocalDate hoy   = LocalDate.now();
        LocalTime ahora = LocalTime.now();
        String dia      = diaComercial();

        List<Promocion> candidatas = promocionRepository.findActivasConAlcance();

        List<PromocionAplicableDTO> resultado = new ArrayList<>();

        for (Promocion promo : candidatas) {

            // ── Filtro de vigencia por fecha ──────────────────────────
            if (!estaEnFechaVigente(promo, hoy)) continue;

            // ── Filtro de día activo ──────────────────────────────────
            if (!estaActivaElDia(promo, dia)) continue;

            // ── Filtro de horario ─────────────────────────────────────
            if (!estaEnHorario(promo, ahora)) continue;

            // ── Evaluar por tipo ──────────────────────────────────────
            Optional<PromocionAplicableDTO> aplicable = switch (promo.getTipo()) {
                case NXM             -> evaluarNXM(promo, orden);
                case GRUPO_CONDICIONAL -> evaluarGrupoCondicional(promo, orden);
                case PORCENTAJE      -> evaluarDescuentoSimple(promo, orden);
                case MONTO_FIJO      -> evaluarDescuentoSimple(promo, orden);
                case ALL_YOU_CAN_EAT,
                     BARRA_LIBRE     -> evaluarAYCE(promo, orden);
                case HAPPY_HOUR      -> evaluarHappyHour(promo, orden, ahora);
                case ESPECIAL        -> Optional.empty(); // lógica en frontend
            };

            aplicable.ifPresent(resultado::add);
        }

        return resultado;
    }

    // =========================================================================
    // APLICAR — persiste los descuentos en los OrdenItems
    // El mesero elige cuáles promos aplicar del resultado de evaluar().
    // =========================================================================

    @Transactional
    public void aplicar(Long ordenId, AplicarPromocionesRequest request) {
        Orden orden = cargarOrden(ordenId);

        if (!orden.puedeModificarse()) {
            throw new IllegalStateException("La orden no puede modificarse en estado: " + orden.getEstado());
        }

        // Re-evaluar para garantizar que aún califican
        List<PromocionAplicableDTO> aplicables = evaluar(ordenId);
        Map<Long, PromocionAplicableDTO> porId = aplicables.stream()
                .collect(Collectors.toMap(PromocionAplicableDTO::getPromocionId, p -> p));

        // Verificar stacking — si hay más de una promo y alguna no permite stack
        if (request.getPromocionIds().size() > 1) {
            for (Long promoId : request.getPromocionIds()) {
                Promocion promo = promocionRepository.findById(promoId)
                        .orElseThrow(() -> new IllegalArgumentException("Promoción no encontrada: " + promoId));
                if (!Boolean.TRUE.equals(promo.getPermitirStack())) {
                    throw new IllegalStateException(
                        "La promoción '" + promo.getNombre() + "' no permite combinarse con otras.");
                }
            }
        }

        for (Long promoId : request.getPromocionIds()) {
            PromocionAplicableDTO aplicable = porId.get(promoId);
            if (aplicable == null) {
                throw new IllegalStateException(
                    "La promoción ID " + promoId + " ya no califica para esta orden.");
            }

            Promocion promo = promocionRepository.findById(promoId).orElseThrow();

            switch (promo.getTipo()) {
                case NXM, GRUPO_CONDICIONAL, PORCENTAJE, MONTO_FIJO, HAPPY_HOUR ->
                    aplicarDescuentosEnItems(aplicable, orden);

                case ALL_YOU_CAN_EAT, BARRA_LIBRE ->
                    activarAYCE(promo, orden);
            }
        }

        orden.recalcularTotales();
        ordenRepository.save(orden);
        log.info("Promociones {} aplicadas a orden {}", request.getPromocionIds(), ordenId);
    }

    // =========================================================================
    // NUEVA RONDA — para AYCE y BARRA_LIBRE
    // El mesero solicita explícitamente una ronda adicional.
    // =========================================================================

    @Transactional
    public OrdenItem nuevaRonda(Long ordenId, NuevaRondaRequest request) {
        Orden orden = cargarOrden(ordenId);

        if (!orden.puedeModificarse()) {
            throw new IllegalStateException("La orden no puede modificarse en estado: " + orden.getEstado());
        }

        OrdenPromocionEstado estado = estadoRepository
                .findByOrdenIdAndPromocionIdAndProductoId(
                        ordenId, request.getPromocionId(), request.getProductoId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "No hay promo AYCE activa para ese producto en esta orden."));

        if (!estado.puedeServirRonda()) {
            throw new IllegalStateException(
                "El producto '" + estado.getNombreProducto() +
                "' ya alcanzó el máximo de rondas (" + estado.getMaxRondas() + ").");
        }

        // Buscar el producto en la orden para tomar precio y datos
        Producto producto = orden.getItems().stream()
                .filter(i -> i.getProducto().getId().equals(request.getProductoId()))
                .map(OrdenItem::getProducto)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Producto no encontrado en la orden."));

        // Crear nuevo ítem a precio $0
        OrdenItem ronda = OrdenItem.builder()
                .orden(orden)
                .producto(producto)
                .cantidad(new BigDecimal(estado.getCantidadPorRonda()))
                .precioUnitario(BigDecimal.ZERO)
                .porcentajeDescuento(BigDecimal.ZERO)
                .montoDescuento(BigDecimal.ZERO)
                .notas("Ronda " + (estado.getRondasConsumidas() + 1) +
                       " - Promo ID " + request.getPromocionId())
                .build();

        ronda.calcularTotales();
        orden.agregarItem(ronda);

        // Registrar la ronda consumida
        estado.consumirRonda();
        estadoRepository.save(estado);

        orden.recalcularTotales();
        ordenRepository.save(orden);

        log.info("Nueva ronda de '{}' servida en orden {}. Rondas consumidas: {}",
                estado.getNombreProducto(), ordenId, estado.getRondasConsumidas());

        return ronda;
    }

    // =========================================================================
    // EVALUADORES PRIVADOS POR TIPO
    // =========================================================================

    private Optional<PromocionAplicableDTO> evaluarNXM(Promocion promo, Orden orden) {
        List<OrdenItem> triggers = itemsEnAlcance(orden, promo, RolPromocionAlcance.TRIGGER);

        // Necesitamos al menos llevaN ítems en el trigger
        int totalUnidades = triggers.stream()
                .mapToInt(i -> i.getCantidad().intValue())
                .sum();

        if (totalUnidades < promo.getLlevaN()) return Optional.empty();

        // Cuántas veces se activa la promo (ej: 6 ítems con 3x2 = 2 activaciones)
        int activaciones = totalUnidades / promo.getLlevaN();
        int itemsGratis  = activaciones * (promo.getLlevaN() - promo.getPagaM());

        List<OrdenItem> beneficio = resolverBeneficioNXM(promo, orden, triggers, itemsGratis);
        if (beneficio.isEmpty()) return Optional.empty();

        List<ItemDescuentoDTO> afectados = beneficio.stream()
                .map(item -> ItemDescuentoDTO.builder()
                        .ordenItemId(item.getId())
                        .productoId(item.getProducto().getId())
                        .nombreProducto(item.getProducto().getNombre())
                        .precioOriginal(item.getPrecioUnitario())
                        .descuento(item.getPrecioUnitario())
                        .precioFinal(BigDecimal.ZERO)
                        .motivo("NXM " + promo.getLlevaN() + "x" + promo.getPagaM())
                        .build())
                .collect(Collectors.toList());

        BigDecimal totalDescuento = afectados.stream()
                .map(ItemDescuentoDTO::getDescuento)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return Optional.of(PromocionAplicableDTO.builder()
                .promocionId(promo.getId())
                .nombre(promo.getNombre())
                .tipo(promo.getTipo())
                .itemsAfectados(afectados)
                .totalDescuento(totalDescuento)
                .build());
    }

    private Optional<PromocionAplicableDTO> evaluarGrupoCondicional(Promocion promo, Orden orden) {
        List<OrdenItem> triggers   = itemsEnAlcance(orden, promo, RolPromocionAlcance.TRIGGER);
        List<OrdenItem> beneficios = itemsEnAlcance(orden, promo, RolPromocionAlcance.BENEFICIO);

        int totalTrigger = triggers.stream().mapToInt(i -> i.getCantidad().intValue()).sum();
        if (totalTrigger < promo.getCantidadTrigger()) return Optional.empty();
        if (beneficios.isEmpty()) return Optional.empty();

        // Cuántas activaciones caben
        int activaciones = totalTrigger / promo.getCantidadTrigger();
        int maxBeneficios = activaciones * promo.getCantidadBeneficio();

        // Tomar los primeros N ítems del grupo beneficio (los más baratos si GRATIS)
        List<OrdenItem> itemsBeneficiados = beneficios.stream()
                .sorted(Comparator.comparing(OrdenItem::getPrecioUnitario))
                .limit(maxBeneficios)
                .collect(Collectors.toList());

        List<ItemDescuentoDTO> afectados = itemsBeneficiados.stream()
                .map(item -> {
                    BigDecimal descuento = calcularDescuento(
                            item.getPrecioUnitario(),
                            promo.getCriterioBeneficio(),
                            promo.getValorBeneficio());
                    return ItemDescuentoDTO.builder()
                            .ordenItemId(item.getId())
                            .productoId(item.getProducto().getId())
                            .nombreProducto(item.getProducto().getNombre())
                            .precioOriginal(item.getPrecioUnitario())
                            .descuento(descuento)
                            .precioFinal(item.getPrecioUnitario().subtract(descuento).max(BigDecimal.ZERO))
                            .motivo("GRUPO_CONDICIONAL - " + promo.getNombre())
                            .build();
                })
                .collect(Collectors.toList());

        BigDecimal totalDescuento = afectados.stream()
                .map(ItemDescuentoDTO::getDescuento)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return Optional.of(PromocionAplicableDTO.builder()
                .promocionId(promo.getId())
                .nombre(promo.getNombre())
                .tipo(promo.getTipo())
                .itemsAfectados(afectados)
                .totalDescuento(totalDescuento)
                .build());
    }

    private Optional<PromocionAplicableDTO> evaluarDescuentoSimple(Promocion promo, Orden orden) {
        List<OrdenItem> triggers = itemsEnAlcance(orden, promo, RolPromocionAlcance.TRIGGER);
        if (triggers.isEmpty()) return Optional.empty();

        List<ItemDescuentoDTO> afectados = triggers.stream()
                .map(item -> {
                    BigDecimal descuento = promo.getTipo() == TipoPromocion.PORCENTAJE
                            ? item.getPrecioUnitario()
                                .multiply(promo.getPorcentajeDescuento())
                                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP)
                                .multiply(item.getCantidad())
                            : promo.getMontoDescuento().multiply(item.getCantidad());

                    BigDecimal precioFinal = item.getPrecioUnitario()
                            .subtract(promo.getTipo() == TipoPromocion.PORCENTAJE
                                ? item.getPrecioUnitario()
                                    .multiply(promo.getPorcentajeDescuento())
                                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP)
                                : promo.getMontoDescuento())
                            .max(BigDecimal.ZERO);

                    return ItemDescuentoDTO.builder()
                            .ordenItemId(item.getId())
                            .productoId(item.getProducto().getId())
                            .nombreProducto(item.getProducto().getNombre())
                            .precioOriginal(item.getPrecioUnitario())
                            .descuento(descuento)
                            .precioFinal(precioFinal)
                            .motivo(promo.getTipo() + " - " + promo.getNombre())
                            .build();
                })
                .collect(Collectors.toList());

        BigDecimal totalDescuento = afectados.stream()
                .map(ItemDescuentoDTO::getDescuento)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return Optional.of(PromocionAplicableDTO.builder()
                .promocionId(promo.getId())
                .nombre(promo.getNombre())
                .tipo(promo.getTipo())
                .itemsAfectados(afectados)
                .totalDescuento(totalDescuento)
                .build());
    }

    private Optional<PromocionAplicableDTO> evaluarAYCE(Promocion promo, Orden orden) {
        // El trigger es el producto que representa la promo (ej: "Promo Alitas AYCE")
        List<OrdenItem> triggers = itemsEnAlcance(orden, promo, RolPromocionAlcance.TRIGGER);
        if (triggers.isEmpty()) return Optional.empty();

        // Verificar que no esté ya activada en esta orden
        boolean yaActiva = estadoRepository
                .findByOrdenIdAndPromocionId(orden.getId(), promo.getId())
                .size() > 0;
        if (yaActiva) return Optional.empty();

        // Construir preview de ítems iniciales
        List<PromocionItem> reglasItems = promocionItemRepository.findByPromocionId(promo.getId());

        List<ItemRondaDTO> itemsIniciales = reglasItems.stream()
                .map(r -> ItemRondaDTO.builder()
                        .productoId(r.getProductoId())
                        .nombreProducto(r.getNombreProducto())
                        .cantidad(r.getCantidadPorRonda())
                        .maxRondas(r.getMaxRondas())
                        .build())
                .collect(Collectors.toList());

        return Optional.of(PromocionAplicableDTO.builder()
                .promocionId(promo.getId())
                .nombre(promo.getNombre())
                .tipo(promo.getTipo())
                .itemsAfectados(Collections.emptyList())
                .totalDescuento(BigDecimal.ZERO)
                .itemsInicialesAYCE(itemsIniciales)
                .build());
    }

    private Optional<PromocionAplicableDTO> evaluarHappyHour(
            Promocion promo, Orden orden, LocalTime ahora) {
        // Happy hour es como un descuento simple pero ya filtrado por horario
        return evaluarDescuentoSimple(promo, orden);
    }

    // =========================================================================
    // APLICADORES PRIVADOS
    // =========================================================================

    private void aplicarDescuentosEnItems(PromocionAplicableDTO aplicable, Orden orden) {
        Map<Long, OrdenItem> itemsPorId = orden.getItems().stream()
                .collect(Collectors.toMap(OrdenItem::getId, i -> i));

        for (ItemDescuentoDTO desc : aplicable.getItemsAfectados()) {
            OrdenItem item = itemsPorId.get(desc.getOrdenItemId());
            if (item == null) continue;

            // Se aplica como monto fijo por unidad para no romper calcularTotales()
            item.setMontoDescuento(desc.getDescuento().divide(
                    item.getCantidad(), 2, RoundingMode.HALF_UP));
            item.setPorcentajeDescuento(BigDecimal.ZERO);
            item.calcularTotales();
        }
    }

    private void activarAYCE(Promocion promo, Orden orden) {
        List<PromocionItem> reglas = promocionItemRepository.findByPromocionId(promo.getId());

        for (PromocionItem regla : reglas) {
            // Buscar el producto en la orden para obtener la entidad
            Producto producto = orden.getItems().stream()
                    .filter(i -> i.getProducto().getId().equals(regla.getProductoId()))
                    .map(OrdenItem::getProducto)
                    .findFirst()
                    .orElse(null);

            if (producto == null) {
                log.warn("Producto ID {} de promo AYCE {} no encontrado en orden {}",
                        regla.getProductoId(), promo.getId(), orden.getId());
                continue;
            }

            // Verificar que no exista ya el estado (idempotencia)
            if (estadoRepository.existsByOrdenIdAndPromocionIdAndProductoId(
                    orden.getId(), promo.getId(), regla.getProductoId())) {
                continue;
            }

            // Crear ítem inicial a precio $0
            OrdenItem itemInicial = OrdenItem.builder()
                    .orden(orden)
                    .producto(producto)
                    .cantidad(new BigDecimal(regla.getCantidadPorRonda()))
                    .precioUnitario(BigDecimal.ZERO)
                    .porcentajeDescuento(BigDecimal.ZERO)
                    .montoDescuento(BigDecimal.ZERO)
                    .notas("Ronda 1 - " + promo.getNombre())
                    .build();

            itemInicial.calcularTotales();
            orden.agregarItem(itemInicial);

            // Crear estado de ronda
            OrdenPromocionEstado estado = OrdenPromocionEstado.builder()
                    .orden(orden)
                    .promocionId(promo.getId())
                    .productoId(regla.getProductoId())
                    .nombreProducto(regla.getNombreProducto())
                    .rondasConsumidas(1)
                    .maxRondas(regla.getMaxRondas())
                    .cantidadPorRonda(regla.getCantidadPorRonda())
                    .fechaUltimaRonda(LocalDateTime.now())
                    .build();

            estadoRepository.save(estado);

            log.info("AYCE activado: {} x{} en orden {}. Max rondas: {}",
                    regla.getNombreProducto(), regla.getCantidadPorRonda(),
                    orden.getId(), regla.getMaxRondas());
        }
    }

    // =========================================================================
    // HELPERS PRIVADOS
    // =========================================================================

    /**
     * Retorna los ítems de la orden que pertenecen al alcance de la promo
     * según el rol dado (TRIGGER o BENEFICIO).
     *
     * Un ítem califica si su producto está en:
     *   - PromocionProducto con ese rol, O
     *   - PromocionCategoria con ese rol (alguna categoría del producto), O
     *   - PromocionFamilia con ese rol (la familia del producto)
     *
     * Si la promo no tiene alcance definido para ese rol, retorna lista vacía.
     */
    private List<OrdenItem> itemsEnAlcance(Orden orden, Promocion promo, RolPromocionAlcance rol) {
        // IDs de productos explícitos con ese rol
        Set<Long> productosRol = promo.getProductos().stream()
                .filter(p -> p.getRol() == rol)
                .map(PromocionProducto::getProductoId)
                .collect(Collectors.toSet());

        // IDs de categorías con ese rol
        Set<Long> categoriasRol = promo.getCategorias().stream()
                .filter(c -> c.getRol() == rol)
                .map(PromocionCategoria::getCategoriaId)
                .collect(Collectors.toSet());

        // IDs de familias con ese rol
        Set<Long> familiasRol = promo.getFamilias().stream()
                .filter(f -> f.getRol() == rol)
                .map(PromocionFamilia::getFamiliaId)
                .collect(Collectors.toSet());

        // Si no hay ningún alcance definido para este rol, no aplica
        if (productosRol.isEmpty() && categoriasRol.isEmpty() && familiasRol.isEmpty()) {
            return Collections.emptyList();
        }

        return orden.getItems().stream()
                .filter(item -> {
                    Producto p = item.getProducto();

                    // Por producto explícito
                    if (productosRol.contains(p.getId())) return true;

                    // Por categoría (producto puede tener varias)
                    boolean enCategoria = p.getCategorias().stream()
                            .anyMatch(cat -> categoriasRol.contains(cat.getId()));
                    if (enCategoria) return true;

                    // Por familia
                    return p.getFamilia() != null &&
                           familiasRol.contains(p.getFamilia().getId());
                })
                .collect(Collectors.toList());
    }

    /**
     * Resuelve cuáles ítems reciben el beneficio en NXM.
     */
    private List<OrdenItem> resolverBeneficioNXM(
            Promocion promo, Orden orden,
            List<OrdenItem> triggers, int itemsGratis) {

        return switch (promo.getCriterioItemGratis()) {
            case MAS_BARATO ->
                // Del mismo grupo trigger, tomar los más baratos
                triggers.stream()
                        .sorted(Comparator.comparing(OrdenItem::getPrecioUnitario))
                        .limit(itemsGratis)
                        .collect(Collectors.toList());

            case PRODUCTO_ESPECIFICO ->
                // Ítems del grupo BENEFICIO en la orden
                itemsEnAlcance(orden, promo, RolPromocionAlcance.BENEFICIO)
                        .stream()
                        .limit(itemsGratis)
                        .collect(Collectors.toList());
        };
    }

    /**
     * Calcula el monto de descuento sobre un precio según el criterio.
     */
    private BigDecimal calcularDescuento(
            BigDecimal precio, CriterioDescuento criterio, BigDecimal valor) {
        return switch (criterio) {
            case GRATIS     -> precio;
            case PORCENTAJE -> precio.multiply(valor)
                                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
            case MONTO_FIJO -> valor.min(precio); // no puede ser mayor al precio
        };
    }

    /**
     * Verifica si la promo está dentro del rango de fechas.
     * Si no tiene fechas definidas, siempre está vigente.
     */
    private boolean estaEnFechaVigente(Promocion promo, LocalDate hoy) {
        if (promo.getFechaInicio() == null) return true;
        return !hoy.isBefore(promo.getFechaInicio()) &&
               !hoy.isAfter(promo.getFechaFin());
    }

    /**
     * Verifica si la promo está activa el día comercial actual.
     */
    private boolean estaActivaElDia(Promocion promo, String dia) {
        return switch (dia) {
            case "LUNES"     -> Boolean.TRUE.equals(promo.getLunes());
            case "MARTES"    -> Boolean.TRUE.equals(promo.getMartes());
            case "MIERCOLES" -> Boolean.TRUE.equals(promo.getMiercoles());
            case "JUEVES"    -> Boolean.TRUE.equals(promo.getJueves());
            case "VIERNES"   -> Boolean.TRUE.equals(promo.getViernes());
            case "SABADO"    -> Boolean.TRUE.equals(promo.getSabado());
            case "DOMINGO"   -> Boolean.TRUE.equals(promo.getDomingo());
            default          -> false;
        };
    }

    /**
     * Verifica si la hora actual está dentro del rango horario de la promo.
     * Si la promo no tiene rango horario, siempre aplica.
     */
    private boolean estaEnHorario(Promocion promo, LocalTime ahora) {
        if (promo.getHoraInicio() == null) return true;
        return !ahora.isBefore(promo.getHoraInicio()) &&
               !ahora.isAfter(promo.getHoraFin());
    }

    /**
     * Día comercial actual en mayúsculas.
     * El día comercial arranca a las 04:00am — si son menos de las 4am
     * se considera el día anterior (lógica del frontend replicada aquí).
     */
    private String diaComercial() {
        LocalDateTime ahora = LocalDateTime.now();
        LocalDateTime inicio = ahora.toLocalDate().atTime(4, 0);
        java.time.DayOfWeek dia = ahora.isBefore(inicio)
                ? ahora.minusDays(1).getDayOfWeek()
                : ahora.getDayOfWeek();

        return switch (dia) {
            case MONDAY    -> "LUNES";
            case TUESDAY   -> "MARTES";
            case WEDNESDAY -> "MIERCOLES";
            case THURSDAY  -> "JUEVES";
            case FRIDAY    -> "VIERNES";
            case SATURDAY  -> "SABADO";
            case SUNDAY    -> "DOMINGO";
        };
    }

    private Orden cargarOrden(Long ordenId) {
        return ordenRepository.findById(ordenId)
                .orElseThrow(() -> new IllegalArgumentException("Orden no encontrada: " + ordenId));
    }
}