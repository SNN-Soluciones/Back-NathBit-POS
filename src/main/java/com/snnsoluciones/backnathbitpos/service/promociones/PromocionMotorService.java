package com.snnsoluciones.backnathbitpos.service.promociones;

import com.snnsoluciones.backnathbitpos.dto.promociones.*;
import com.snnsoluciones.backnathbitpos.entity.*;
import com.snnsoluciones.backnathbitpos.enums.*;
import com.snnsoluciones.backnathbitpos.repository.*;
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

    private final OrdenRepository                 ordenRepository;
    private final OrdenItemRepository             ordenItemRepository;
    private final PromocionRepository             promocionRepository;
    private final PromocionItemRepository         promocionItemRepository;
    private final OrdenPromocionEstadoRepository  estadoRepository;

    // =========================================================================
    // EVALUAR — solo lectura, no persiste nada.
    // Devuelve qué promos califican y qué descuento/beneficio tendría cada ítem.
    // El frontend lo llama cada vez que el mesero agrega o quita un ítem.
    // =========================================================================

    @Transactional(readOnly = true)
    public List<PromocionAplicableDTO> evaluar(Long ordenId, Long empresaId, Long sucursalId) {
        Orden orden = cargarOrden(ordenId);

        LocalDate hoy   = LocalDate.now();
        LocalTime ahora = LocalTime.now();
        String dia      = diaComercial();

        // Solo cargamos promos de la empresa/sucursal correcta
        List<Promocion> candidatas = promocionRepository
            .findActivasConProductos(empresaId, sucursalId);

// Forzar carga de categorías y familias en memoria (evita N+1 en el loop)
        promocionRepository.findActivasConCategorias(empresaId, sucursalId);
        promocionRepository.findActivasConFamilias(empresaId, sucursalId);

        List<PromocionAplicableDTO> resultado = new ArrayList<>();

        for (Promocion promo : candidatas) {
            if (!estaEnFechaVigente(promo, hoy)) continue;
            if (!estaActivaElDia(promo, dia))    continue;
            if (!estaEnHorario(promo, ahora))    continue;

            Optional<PromocionAplicableDTO> aplicable = switch (promo.getTipo()) {
                case NXM              -> evaluarNXM(promo, orden);
                case GRUPO_CONDICIONAL -> evaluarGrupoCondicional(promo, orden);
                case PORCENTAJE       -> evaluarDescuentoSimple(promo, orden);
                case MONTO_FIJO       -> evaluarDescuentoSimple(promo, orden);
                case ALL_YOU_CAN_EAT,
                     BARRA_LIBRE      -> evaluarAYCE(promo, orden);
                case HAPPY_HOUR       -> evaluarHappyHour(promo, orden, ahora);
                case ESPECIAL         -> Optional.empty();
            };

            aplicable.ifPresent(resultado::add);
        }

        return resultado;
    }

    // =========================================================================
    // APLICAR — persiste los descuentos en los OrdenItems.
    // El mesero elige cuáles promos aplicar del resultado de evaluar().
    // =========================================================================

    @Transactional
    public void aplicar(Long ordenId, Long empresaId, Long sucursalId, AplicarPromocionesRequest request) {
        Orden orden = cargarOrden(ordenId);

        if (!orden.puedeModificarse()) {
            throw new IllegalStateException(
                "La orden no puede modificarse en estado: " + orden.getEstado());
        }

        // Re-evaluar para garantizar que aún califican
        List<PromocionAplicableDTO> aplicables = evaluar(ordenId, empresaId, sucursalId);
        Map<Long, PromocionAplicableDTO> porId = aplicables.stream()
            .collect(Collectors.toMap(PromocionAplicableDTO::getPromocionId, p -> p));

        // Verificar stacking
        if (request.getPromocionIds().size() > 1) {
            for (Long promoId : request.getPromocionIds()) {
                Promocion promo = promocionRepository.findById(promoId)
                    .orElseThrow(() -> new IllegalArgumentException(
                        "Promoción no encontrada: " + promoId));
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
    // NUEVA RONDA — para AYCE y BARRA_LIBRE.
    // El mesero solicita explícitamente una ronda adicional.
    // =========================================================================

    @Transactional
    public OrdenItem nuevaRonda(Long ordenId, NuevaRondaRequest request) {
        Orden orden = cargarOrden(ordenId);

        if (!orden.puedeModificarse()) {
            throw new IllegalStateException(
                "La orden no puede modificarse en estado: " + orden.getEstado());
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

        Producto producto = orden.getItems().stream()
            .filter(i -> i.getProducto().getId().equals(request.getProductoId()))
            .map(OrdenItem::getProducto)
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException(
                "Producto no encontrado en la orden."));

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

        int totalUnidades = triggers.stream()
            .mapToInt(i -> i.getCantidad().intValue())
            .sum();

        if (totalUnidades < promo.getLlevaN()) return Optional.empty();

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

    /**
     * GRUPO_CONDICIONAL — ej: "Niños comen gratis domingos"
     *
     * Flujo:
     *   1. Cuenta ítems TRIGGER en la orden (los adultos).
     *   2. Si hay suficientes → califica.
     *   3. Devuelve DOS cosas al frontend:
     *      a) itemsAfectados  → ítems BENEFICIO ya en la orden (serán descontados al aplicar).
     *      b) productosBeneficioDisponibles → catálogo completo de productos BENEFICIO
     *                          habilitados por la promo, para que el mesero elija cuál agregar.
     *   4. cantidadBeneficioDisponible → cuántos puede agregar todavía.
     */
    private Optional<PromocionAplicableDTO> evaluarGrupoCondicional(Promocion promo, Orden orden) {
        List<OrdenItem> triggers   = itemsEnAlcance(orden, promo, RolPromocionAlcance.TRIGGER);
        List<OrdenItem> enOrden    = itemsEnAlcance(orden, promo, RolPromocionAlcance.BENEFICIO);

        int totalTrigger = triggers.stream().mapToInt(i -> i.getCantidad().intValue()).sum();
        if (totalTrigger < promo.getCantidadTrigger()) return Optional.empty();

        // Cuántas activaciones hay (ej: 4 adultos / 2 por activación = 2 activaciones)
        int activaciones   = totalTrigger / promo.getCantidadTrigger();
        int maxBeneficios  = activaciones * promo.getCantidadBeneficio();

        // ── a) Ítems ya en la orden que serán descontados ────────────
        // Toma los más baratos primero (criterio más favorable para el cliente)
        List<OrdenItem> itemsBeneficiados = enOrden.stream()
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

        // ── b) Catálogo BENEFICIO disponible para que el mesero elija ─
        // Viene de PromocionProducto con rol=BENEFICIO (nombre/id desnormalizados)
        List<ProductoBeneficioDTO> disponibles = promo.getProductos().stream()
            .filter(p -> p.getRol() == RolPromocionAlcance.BENEFICIO)
            .map(p -> ProductoBeneficioDTO.builder()
                .productoId(p.getProductoId())
                .nombre(p.getNombreProducto())
                .criterio(promo.getCriterioBeneficio())
                .valorBeneficio(promo.getValorBeneficio())
                .build())
            .collect(Collectors.toList());

        // Cuántos beneficios puede agregar aún el mesero
        int yaEnOrden    = itemsBeneficiados.size();
        int disponibleN  = Math.max(0, maxBeneficios - yaEnOrden);

        return Optional.of(PromocionAplicableDTO.builder()
            .promocionId(promo.getId())
            .nombre(promo.getNombre())
            .tipo(promo.getTipo())
            .itemsAfectados(afectados)
            .totalDescuento(totalDescuento)
            .productosBeneficioDisponibles(disponibles.isEmpty() ? null : disponibles)
            .cantidadBeneficioDisponible(disponibleN)
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
        List<OrdenItem> triggers = itemsEnAlcance(orden, promo, RolPromocionAlcance.TRIGGER);
        if (triggers.isEmpty()) return Optional.empty();

        // Si ya está activo en esta orden, no lo ofrecemos de nuevo
        boolean yaActiva = !estadoRepository
            .findByOrdenIdAndPromocionId(orden.getId(), promo.getId())
            .isEmpty();
        if (yaActiva) return Optional.empty();

        List<PromocionItem> reglasItems = promocionItemRepository
            .findByPromocionId(promo.getId());

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
        // Happy hour ya fue filtrado por horario en evaluar(); delegamos al descuento simple
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

            item.setMontoDescuento(desc.getDescuento().divide(
                item.getCantidad(), 2, RoundingMode.HALF_UP));
            item.setPorcentajeDescuento(BigDecimal.ZERO);
            item.calcularTotales();
        }
    }

    private void activarAYCE(Promocion promo, Orden orden) {
        List<PromocionItem> reglas = promocionItemRepository.findByPromocionId(promo.getId());

        for (PromocionItem regla : reglas) {
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

            // Idempotencia — no crear duplicados
            if (estadoRepository.existsByOrdenIdAndPromocionIdAndProductoId(
                orden.getId(), promo.getId(), regla.getProductoId())) {
                continue;
            }

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
     * Ítems de la orden que pertenecen al alcance de la promo con el rol dado.
     * Un ítem califica si su producto está en PromocionProducto, PromocionCategoria
     * o PromocionFamilia con ese rol.
     */
    private List<OrdenItem> itemsEnAlcance(Orden orden, Promocion promo, RolPromocionAlcance rol) {
        Set<Long> productosRol = promo.getProductos().stream()
            .filter(p -> p.getRol() == rol)
            .map(PromocionProducto::getProductoId)
            .collect(Collectors.toSet());

        Set<Long> categoriasRol = promo.getCategorias().stream()
            .filter(c -> c.getRol() == rol)
            .map(PromocionCategoria::getCategoriaId)
            .collect(Collectors.toSet());

        Set<Long> familiasRol = promo.getFamilias().stream()
            .filter(f -> f.getRol() == rol)
            .map(PromocionFamilia::getFamiliaId)
            .collect(Collectors.toSet());

        if (productosRol.isEmpty() && categoriasRol.isEmpty() && familiasRol.isEmpty()) {
            return Collections.emptyList();
        }

        return orden.getItems().stream()
            .filter(item -> {
                Producto p = item.getProducto();
                if (productosRol.contains(p.getId())) return true;
                boolean enCategoria = p.getCategorias().stream()
                    .anyMatch(cat -> categoriasRol.contains(cat.getId()));
                if (enCategoria) return true;
                return p.getFamilia() != null &&
                    familiasRol.contains(p.getFamilia().getId());
            })
            .collect(Collectors.toList());
    }

    private List<OrdenItem> resolverBeneficioNXM(
        Promocion promo, Orden orden,
        List<OrdenItem> triggers, int itemsGratis) {

        return switch (promo.getCriterioItemGratis()) {
            case MAS_BARATO ->
                triggers.stream()
                    .sorted(Comparator.comparing(OrdenItem::getPrecioUnitario))
                    .limit(itemsGratis)
                    .collect(Collectors.toList());
            case PRODUCTO_ESPECIFICO ->
                itemsEnAlcance(orden, promo, RolPromocionAlcance.BENEFICIO)
                    .stream()
                    .limit(itemsGratis)
                    .collect(Collectors.toList());
        };
    }

    private BigDecimal calcularDescuento(
        BigDecimal precio, CriterioDescuento criterio, BigDecimal valor) {
        return switch (criterio) {
            case GRATIS     -> precio;
            case PORCENTAJE -> precio.multiply(valor)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
            case MONTO_FIJO -> valor.min(precio);
        };
    }

    private boolean estaEnFechaVigente(Promocion promo, LocalDate hoy) {
        if (promo.getFechaInicio() == null) return true;
        return !hoy.isBefore(promo.getFechaInicio()) &&
            !hoy.isAfter(promo.getFechaFin());
    }

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

    private boolean estaEnHorario(Promocion promo, LocalTime ahora) {
        if (promo.getHoraInicio() == null) return true;
        return !ahora.isBefore(promo.getHoraInicio()) &&
            !ahora.isAfter(promo.getHoraFin());
    }

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