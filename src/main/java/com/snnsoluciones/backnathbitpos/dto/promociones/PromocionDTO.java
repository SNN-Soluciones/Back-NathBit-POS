package com.snnsoluciones.backnathbitpos.dto.promociones;

import com.snnsoluciones.backnathbitpos.entity.Promocion;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * DTO de respuesta completo para Promocion.
 * Incluye los items anidados cuando aplica
 * (BARRA_LIBRE, ALL_YOU_CAN_EAT, ESPECIAL).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PromocionDTO {

    private Long id;

    // ── Identificación ────────────────────────────────────────────────

    private String nombre;
    private String descripcion;
    private com.snnsoluciones.nathbitbusinesscore.model.enums.TipoPromocion tipo;
    private Boolean activo;

    // ── Días activos ──────────────────────────────────────────────────

    private Boolean lunes;
    private Boolean martes;
    private Boolean miercoles;
    private Boolean jueves;
    private Boolean viernes;
    private Boolean sabado;
    private Boolean domingo;

    // ── Rango horario (NULL = todo el día comercial) ──────────────────

    private LocalTime horaInicio;
    private LocalTime horaFin;

    // ── Campos por tipo ───────────────────────────────────────────────

    /** NXM: cantidad que lleva el cliente */
    private Integer llevaN;

    /** NXM: cantidad que paga el cliente */
    private Integer pagaM;

    /** PORCENTAJE: % de descuento */
    private BigDecimal porcentajeDescuento;

    /** MONTO_FIJO: monto a descontar */
    private BigDecimal montoDescuento;

    /** BARRA_LIBRE / ALL_YOU_CAN_EAT: precio fijo del combo */
    private BigDecimal precioPromo;

    // ── Items incluidos ───────────────────────────────────────────────

    private List<PromocionItemDTO> items;

    // ── Auditoría ─────────────────────────────────────────────────────

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ── Mapping ───────────────────────────────────────────────────────

    public static PromocionDTO fromEntity(Promocion entity) {
        if (entity == null) return null;

        List<PromocionItemDTO> itemsDTO = entity.getItems() != null
                ? entity.getItems().stream()
                        .map(PromocionItemDTO::fromEntity)
                        .collect(Collectors.toList())
                : Collections.emptyList();

        return PromocionDTO.builder()
                .id(entity.getId())
                .nombre(entity.getNombre())
                .descripcion(entity.getDescripcion())
                .tipo(entity.getTipo())
                .activo(entity.getActivo())
                .lunes(entity.getLunes())
                .martes(entity.getMartes())
                .miercoles(entity.getMiercoles())
                .jueves(entity.getJueves())
                .viernes(entity.getViernes())
                .sabado(entity.getSabado())
                .domingo(entity.getDomingo())
                .horaInicio(entity.getHoraInicio())
                .horaFin(entity.getHoraFin())
                .llevaN(entity.getLlevaN())
                .pagaM(entity.getPagaM())
                .porcentajeDescuento(entity.getPorcentajeDescuento())
                .montoDescuento(entity.getMontoDescuento())
                .precioPromo(entity.getPrecioPromo())
                .items(itemsDTO)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}