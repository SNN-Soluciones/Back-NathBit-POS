package com.snnsoluciones.backnathbitpos.dto.promociones;

import com.snnsoluciones.nathbitbusinesscore.model.enums.TipoPromocion;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Request para crear o actualizar una promoción.
 *
 * Campos requeridos según tipo:
 *   NXM             → llevaN, pagaM
 *   BARRA_LIBRE     → precioPromo + items
 *   ALL_YOU_CAN_EAT → precioPromo + items (con reglas de ronda)
 *   PORCENTAJE      → porcentajeDescuento
 *   MONTO_FIJO      → montoDescuento
 *   HAPPY_HOUR      → cualquier combinación + horaInicio + horaFin
 *   ESPECIAL        → descripcion + items opcionales
 *
 * La validación cruzada por tipo se hace en la capa de servicio.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreatePromocionRequest {

    // Identificación
    @NotBlank(message = "El nombre de la promoción es obligatorio")
    @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
    private String nombre;

    @Size(max = 300, message = "La descripción no puede superar los 300 caracteres")
    private String descripcion;

    @NotNull(message = "El tipo de promoción es obligatorio")
    private TipoPromocion tipo;

    @Builder.Default
    private Boolean activo = true;

    // Días activos (al menos uno requerido, validado en servicio)
    @Builder.Default private Boolean lunes     = false;
    @Builder.Default private Boolean martes    = false;
    @Builder.Default private Boolean miercoles = false;
    @Builder.Default private Boolean jueves    = false;
    @Builder.Default private Boolean viernes   = false;
    @Builder.Default private Boolean sabado    = false;
    @Builder.Default private Boolean domingo   = false;

    // Rango horario (ambos NULL = todo el día comercial)
    private LocalTime horaInicio;
    private LocalTime horaFin;

    // Campos para NXM
    @Min(value = 2, message = "lleva_n debe ser al menos 2")
    private Integer llevaN;

    @Min(value = 1, message = "paga_m debe ser al menos 1")
    private Integer pagaM;

    // Campos para PORCENTAJE
    @DecimalMin(value = "0.01", message = "El porcentaje debe ser mayor a 0")
    @DecimalMax(value = "100.00", message = "El porcentaje no puede superar 100")
    private BigDecimal porcentajeDescuento;

    // Campos para MONTO_FIJO
    @DecimalMin(value = "0.01", message = "El monto de descuento debe ser mayor a 0")
    private BigDecimal montoDescuento;

    // Campos para BARRA_LIBRE y ALL_YOU_CAN_EAT
    @DecimalMin(value = "0.00", message = "El precio promo no puede ser negativo")
    private BigDecimal precioPromo;

    // Items (solo para BARRA_LIBRE, ALL_YOU_CAN_EAT, ESPECIAL)
    @Valid
    @Builder.Default
    private List<CreatePromocionItemRequest> items = new ArrayList<>();

    // Alcance por familia (vacío = aplica a todas)
    @Valid
    @Builder.Default
    private List<CreatePromocionAlcanceRequest> familias = new ArrayList<>();

    // Alcance por categoría (vacío = aplica a todas)
    @Valid
    @Builder.Default
    private List<CreatePromocionAlcanceRequest> categorias = new ArrayList<>();

    // Alcance por producto puntual (vacío = aplica a todos)
    @Valid
    @Builder.Default
    private List<CreatePromocionAlcanceRequest> productos = new ArrayList<>();
}

// NOTA: Reemplazar el archivo original con este bloque adicional al final,
// antes del cierre de clase. Agregar también los imports:
//   import com.snnsoluciones.nathbitbusinesscore.model.dto.promociones.CreatePromocionAlcanceRequest;