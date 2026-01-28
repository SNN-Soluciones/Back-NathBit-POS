package com.snnsoluciones.backnathbitpos.service;

import com.snnsoluciones.backnathbitpos.dto.orden.*;
import com.snnsoluciones.backnathbitpos.entity.Orden;
import com.snnsoluciones.backnathbitpos.entity.OrdenItem;
import com.snnsoluciones.backnathbitpos.entity.OrdenPersona;
import com.snnsoluciones.backnathbitpos.entity.Producto;
import com.snnsoluciones.backnathbitpos.enums.EstadoPagoItem;
import com.snnsoluciones.backnathbitpos.exception.BadRequestException;
import com.snnsoluciones.backnathbitpos.exception.BusinessException;
import com.snnsoluciones.backnathbitpos.exception.ResourceNotFoundException;
import com.snnsoluciones.backnathbitpos.repository.OrdenPersonaRepository;
import com.snnsoluciones.backnathbitpos.repository.OrdenRepository;
import com.snnsoluciones.backnathbitpos.repository.OrdenItemRepository;
import com.snnsoluciones.backnathbitpos.repository.ProductoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrdenPersonaService {

    private final OrdenRepository ordenRepository;
    private final OrdenPersonaRepository ordenPersonaRepository;
    private final OrdenItemRepository ordenItemRepository;
    private final ProductoRepository productoRepository;

    private static final String[] COLORES_PREDETERMINADOS = {
        "#EF4444", "#F59E0B", "#10B981", "#3B82F6", "#8B5CF6",
        "#EC4899", "#14B8A6", "#F97316", "#6366F1", "#84CC16"
    };

    // ==================== CREAR PERSONA ====================

    @Transactional
    public OrdenPersonaDTO crearPersona(Long ordenId, CrearPersonaRequest request) {
        log.info("📝 Creando persona '{}' en orden {}", request.nombre(), ordenId);

        Orden orden = ordenRepository.findById(ordenId)
            .orElseThrow(() -> new ResourceNotFoundException("Orden no encontrada"));

        // Validar que no exista ya una persona con ese nombre
        if (ordenPersonaRepository.existsByOrdenIdAndNombre(ordenId, request.nombre())) {
            throw new BadRequestException("Ya existe una persona con el nombre '" 
                + request.nombre() + "' en esta orden");
        }

        // Generar color automático si no viene
        String color = request.color() != null && !request.color().isBlank()
            ? request.color()
            : generarColorAleatorio();

        // Determinar orden de visualización
        int ordenVis = request.ordenVisualizacion() != null
            ? request.ordenVisualizacion()
            : (int) ordenPersonaRepository.countByOrdenId(ordenId);

        OrdenPersona persona = OrdenPersona.builder()
            .orden(orden)
            .nombre(request.nombre())
            .color(color)
            .ordenVisualizacion(ordenVis)
            .activo(true)
            .build();

        persona = ordenPersonaRepository.save(persona);

        log.info("✅ Persona creada: {} (color: {})", persona.getNombre(), persona.getColor());

        return mapToDTO(persona);
    }

    // ==================== LISTAR PERSONAS DE ORDEN ====================

    @Transactional(readOnly = true)
    public List<OrdenPersonaDTO> listarPersonas(Long ordenId) {
        log.info("🔍 Listando personas de orden {}", ordenId);

        // Verificar que existe la orden
        if (!ordenRepository.existsById(ordenId)) {
            throw new ResourceNotFoundException("Orden no encontrada");
        }

        List<OrdenPersona> personas = ordenPersonaRepository
            .findByOrdenIdAndActivoTrueOrderByOrdenVisualizacionAsc(ordenId);

        return personas.stream()
            .map(this::mapToDTO)
            .collect(Collectors.toList());
    }

    // ==================== AGREGAR ITEMS CON PERSONA ====================

    @Transactional
    public OrdenResponse agregarItemsConPersona(Long ordenId,
        AgregarItemsConPersonaRequest request) {
        log.info("📝 Agregando {} items para persona '{}' en orden {}",
            request.items().size(), request.nombrePersona(), ordenId);

        Orden orden = ordenRepository.findById(ordenId)
            .orElseThrow(() -> new ResourceNotFoundException("Orden no encontrada"));

        if (!orden.puedeModificarse()) {
            throw new BusinessException("La orden no puede modificarse en estado: "
                + orden.getEstado());
        }

        // ⭐ Variables finales para usar en lambda
        final Orden ordenFinal = orden;
        final int ordenVisualizacionCalculado = (int) ordenPersonaRepository.countByOrdenId(ordenId);

        // Buscar o crear persona
        OrdenPersona persona = ordenPersonaRepository
            .findByOrdenIdAndNombre(ordenId, request.nombrePersona())
            .orElseGet(() -> {
                log.info("🆕 Persona '{}' no existe, creando...", request.nombrePersona());

                String color = request.colorPersona() != null && !request.colorPersona().isBlank()
                    ? request.colorPersona()
                    : generarColorAleatorio();

                OrdenPersona nuevaPersona = OrdenPersona.builder()
                    .orden(ordenFinal) // ⭐ Usar ordenFinal
                    .nombre(request.nombrePersona())
                    .color(color)
                    .ordenVisualizacion(ordenVisualizacionCalculado)
                    .activo(true)
                    .build();

                return ordenPersonaRepository.save(nuevaPersona);
            });

        // Agregar items
        for (var itemReq : request.items()) {
            Producto producto = productoRepository.findById(itemReq.productoId())
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Producto no encontrado: " + itemReq.productoId()));

            BigDecimal precioUnitario = producto.getPrecioVenta() != null
                ? producto.getPrecioVenta()
                : BigDecimal.ZERO;

            OrdenItem item = OrdenItem.builder()
                .orden(orden) // ⭐ Aquí seguimos usando 'orden' normal
                .producto(producto)
                .cantidad(itemReq.cantidad())
                .precioUnitario(precioUnitario)
                .tarifaImpuesto(obtenerTarifaImpuesto(producto))
                .notas(itemReq.notas())
                .ordenPersona(persona)
                .build();

            item.calcularTotales();
            orden.agregarItem(item);
        }

        orden.recalcularTotales();
        orden = ordenRepository.save(orden); // ⭐ Ahora esto funciona

        log.info("✅ Items agregados para persona: {}", persona.getNombre());

        return mapOrdenToResponse(orden);
    }

    // ==================== ASIGNAR ITEM A PERSONA ====================

    @Transactional
    public void asignarItemAPersona(Long ordenId, Long itemId, 
                                     AsignarItemAPersonaRequest request) {
        log.info("🔗 Asignando item {} a persona {} en orden {}", 
            itemId, request.personaId(), ordenId);

        OrdenItem item = ordenItemRepository.findById(itemId)
            .orElseThrow(() -> new ResourceNotFoundException("Item no encontrado"));

        if (!item.getOrden().getId().equals(ordenId)) {
            throw new BadRequestException("El item no pertenece a esta orden");
        }

        OrdenPersona persona = ordenPersonaRepository.findById(request.personaId())
            .orElseThrow(() -> new ResourceNotFoundException("Persona no encontrada"));

        if (!persona.getOrden().getId().equals(ordenId)) {
            throw new BadRequestException("La persona no pertenece a esta orden");
        }

        item.setOrdenPersona(persona);
        ordenItemRepository.save(item);

        log.info("✅ Item asignado a persona: {}", persona.getNombre());
    }

    // ==================== DESASIGNAR ITEM DE PERSONA ====================

    @Transactional
    public void desasignarItemDePersona(Long ordenId, Long itemId) {
        log.info("🔓 Desasignando item {} de su persona en orden {}", itemId, ordenId);

        OrdenItem item = ordenItemRepository.findById(itemId)
            .orElseThrow(() -> new ResourceNotFoundException("Item no encontrado"));

        if (!item.getOrden().getId().equals(ordenId)) {
            throw new BadRequestException("El item no pertenece a esta orden");
        }

        item.setOrdenPersona(null);
        ordenItemRepository.save(item);

        log.info("✅ Item desasignado (ahora es compartido)");
    }

    // ==================== HELPER METHODS ====================

    private String generarColorAleatorio() {
        Random random = new Random();
        return COLORES_PREDETERMINADOS[random.nextInt(COLORES_PREDETERMINADOS.length)];
    }

    private BigDecimal obtenerTarifaImpuesto(Producto producto) {
        if (producto.getImpuestos() != null && !producto.getImpuestos().isEmpty()) {
            return producto.getImpuestos().stream()
                .filter(imp -> "01".equals(imp.getTipoImpuesto().name()))
                .map(imp -> imp.getPorcentaje() != null 
                    ? imp.getPorcentaje() 
                    : BigDecimal.ZERO)
                .findFirst()
                .orElse(BigDecimal.ZERO);
        }
        return BigDecimal.ZERO;
    }

    private OrdenPersonaDTO mapToDTO(OrdenPersona persona) {
        List<Long> itemIds = persona.getItems().stream()
            .map(OrdenItem::getId)
            .collect(Collectors.toList());

        String estadoPago = determinarEstadoPago(persona);

        return new OrdenPersonaDTO(
            persona.getId(),
            persona.getNombre(),
            persona.getColor(),
            persona.getOrdenVisualizacion(),
            persona.getCantidadItems(),
            persona.getTotal(),
            estadoPago,
            persona.getCreatedAt(),
            itemIds
        );
    }

    private String determinarEstadoPago(OrdenPersona persona) {
        if (persona.getItems().isEmpty()) {
            return "PENDIENTE";
        }

        boolean todosPagados = persona.todoPagado();
        boolean algunoPagado = persona.tienePagosParciales();

        if (todosPagados) {
            return "PAGADO";
        } else if (algunoPagado) {
            return "PARCIAL";
        } else {
            return "PENDIENTE";
        }
    }

    // TODO: Este método debería estar en OrdenService, aquí es temporal
    private OrdenResponse mapOrdenToResponse(Orden orden) {
        // Por ahora retornamos null, lo implementaremos después
        // cuando modifiquemos OrdenService
        return null;
    }
}