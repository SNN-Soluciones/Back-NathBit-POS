package com.snnsoluciones.backnathbitpos.dto.promociones;

import com.snnsoluciones.backnathbitpos.entity.PromocionItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO de respuesta para PromocionItem.
 * Representa un producto incluido dentro de una promo.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PromocionItemDTO {

    private Long id;
    private Long promocionId;

    private Long productoId;
    private String nombreProducto;

    private Integer cantidadPorRonda;

    /**
     * NULL = ilimitado.
     * N    = máximo N rondas.
     */
    private Integer maxRondas;

    private LocalDateTime createdAt;

    public static PromocionItemDTO fromEntity(PromocionItem entity) {
        if (entity == null) return null;

        return PromocionItemDTO.builder()
                .id(entity.getId())
                .promocionId(entity.getPromocion().getId())
                .productoId(entity.getProductoId())
                .nombreProducto(entity.getNombreProducto())
                .cantidadPorRonda(entity.getCantidadPorRonda())
                .maxRondas(entity.getMaxRondas())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}