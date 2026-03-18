package com.snnsoluciones.backnathbitpos.dto.promociones;

import com.snnsoluciones.backnathbitpos.enums.CriterioDescuento;
import com.snnsoluciones.backnathbitpos.enums.CriterioItemGratis;
import com.snnsoluciones.backnathbitpos.enums.TipoPromocion;
import com.snnsoluciones.backnathbitpos.entity.Promocion;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PromocionDTO {

    private Long id;

    // ── Identificación ────────────────────────────────────────────────

    private String nombre;
    private String descripcion;
    private TipoPromocion tipo;
    private Boolean activo;

    // ── Vigencia por fecha ────────────────────────────────────────────

    private LocalDate fechaInicio;
    private LocalDate fechaFin;

    // ── Stacking ──────────────────────────────────────────────────────

    private Boolean permitirStack;

    // ── Días activos ──────────────────────────────────────────────────

    private Boolean lunes;
    private Boolean martes;
    private Boolean miercoles;
    private Boolean jueves;
    private Boolean viernes;
    private Boolean sabado;
    private Boolean domingo;

    // ── Rango horario ─────────────────────────────────────────────────

    private LocalTime horaInicio;
    private LocalTime horaFin;

    // ── NXM ───────────────────────────────────────────────────────────

    private Integer llevaN;
    private Integer pagaM;
    private CriterioItemGratis criterioItemGratis;

    // ── PORCENTAJE ────────────────────────────────────────────────────

    private BigDecimal porcentajeDescuento;

    // ── MONTO_FIJO ────────────────────────────────────────────────────

    private BigDecimal montoDescuento;

    // ── BARRA_LIBRE / ALL_YOU_CAN_EAT ────────────────────────────────

    private BigDecimal precioPromo;

    // ── GRUPO_CONDICIONAL ─────────────────────────────────────────────

    private Integer cantidadTrigger;
    private Integer cantidadBeneficio;
    private CriterioDescuento criterioBeneficio;
    private BigDecimal valorBeneficio;

    // ── Colecciones ───────────────────────────────────────────────────

    private List<PromocionItemDTO> items;

    // ── Auditoría ─────────────────────────────────────────────────────

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ── Alcances ──────────────────────────────────────────────────────────
    private List<PromocionAlcanceDTO> productos;
    private List<PromocionAlcanceDTO> familias;
    private List<PromocionAlcanceDTO> categorias;

    // ── Mapping ───────────────────────────────────────────────────────

    public static PromocionDTO fromEntity(Promocion entity) {
        if (entity == null) return null;

        List<PromocionItemDTO> itemsDTO = entity.getItems() != null
            ? entity.getItems().stream()
            .map(PromocionItemDTO::fromEntity)
            .collect(Collectors.toList())
            : Collections.emptyList();

        List<PromocionAlcanceDTO> productosDTO = entity.getProductos() != null
            ? entity.getProductos().stream()
            .map(p -> PromocionAlcanceDTO.builder()
                .id(p.getProductoId())
                .nombre(p.getNombreProducto())
                .rol(p.getRol())
                .build())
            .collect(Collectors.toList())
            : Collections.emptyList();

        List<PromocionAlcanceDTO> familiasDTO = entity.getFamilias() != null
            ? entity.getFamilias().stream()
            .map(f -> PromocionAlcanceDTO.builder()
                .id(f.getFamiliaId())
                .nombre(f.getNombreFamilia())
                .rol(f.getRol())
                .build())
            .collect(Collectors.toList())
            : Collections.emptyList();

        List<PromocionAlcanceDTO> categoriasDTO = entity.getCategorias() != null
            ? entity.getCategorias().stream()
            .map(c -> PromocionAlcanceDTO.builder()
                .id(c.getCategoriaId())
                .nombre(c.getNombreCategoria())
                .rol(c.getRol())
                .build())
            .collect(Collectors.toList())
            : Collections.emptyList();

        return PromocionDTO.builder()
            .id(entity.getId())
            .nombre(entity.getNombre())
            .descripcion(entity.getDescripcion())
            .tipo(entity.getTipo())
            .activo(entity.getActivo())
            .fechaInicio(entity.getFechaInicio())
            .fechaFin(entity.getFechaFin())
            .permitirStack(entity.getPermitirStack())
            .lunes(entity.getLunes())
            .martes(entity.getMartes())
            .miercoles(entity.getMiercoles())
            .jueves(entity.getJueves())
            .viernes(entity.getViernes())
            .sabado(entity.getSabado())
            .domingo(entity.getDomingo())
            .horaInicio(entity.getHoraInicio())
            .productos(productosDTO)
            .familias(familiasDTO)
            .categorias(categoriasDTO)
            .horaFin(entity.getHoraFin())
            .llevaN(entity.getLlevaN())
            .pagaM(entity.getPagaM())
            .criterioItemGratis(entity.getCriterioItemGratis())
            .porcentajeDescuento(entity.getPorcentajeDescuento())
            .montoDescuento(entity.getMontoDescuento())
            .precioPromo(entity.getPrecioPromo())
            .cantidadTrigger(entity.getCantidadTrigger())
            .cantidadBeneficio(entity.getCantidadBeneficio())
            .criterioBeneficio(entity.getCriterioBeneficio())
            .valorBeneficio(entity.getValorBeneficio())
            .items(itemsDTO)
            .createdAt(entity.getCreatedAt())
            .updatedAt(entity.getUpdatedAt())
            .build();
    }
}