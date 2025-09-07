package com.snnsoluciones.backnathbitpos.dto.factura;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * DTO para información de referencia según v4.4 de Hacienda
 * Requerido para Notas de Crédito, Notas de Débito y otros casos especiales
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InformacionReferenciaDto {
    
    /**
     * Tipo de documento de referencia (2 dígitos)
     * 01 = Factura Electrónica
     * 02 = Nota de Débito
     * 03 = Nota de Crédito
     * 04 = Tiquete Electrónico
     * 99 = Otros
     */
    @NotBlank(message = "Tipo de documento de referencia es requerido")
    @Pattern(regexp = "^(01|02|03|04|05|06|07|08|09|10|11|12|13|14|15|16|17|18|99)$", 
             message = "Tipo de documento de referencia inválido")
    @Size(min = 2, max = 2, message = "Tipo de documento debe tener exactamente 2 caracteres")
    private String tipoDoc;
    
    /**
     * Descripción cuando tipoDoc = '99' (Otros)
     */
    @Size(min = 5, max = 100, message = "Tipo documento OTRO debe tener entre 5 y 100 caracteres")
    private String tipoDocRefOTRO;
    
    /**
     * Clave numérica o consecutivo del documento de referencia (máx 50)
     * Para facturas electrónicas es la clave de 50 dígitos
     * Para otros documentos puede ser el consecutivo
     */
    @Size(max = 50, message = "Número de referencia no puede exceder 50 caracteres")
    private String numero;
    
    /**
     * Fecha de emisión del documento de referencia
     * Formato: ISO DateTime (YYYY-MM-DDTHH:mm:ss-06:00)
     */
    @NotBlank(message = "Fecha de emisión de referencia es requerida")
    private String fechaEmision;
    
    /**
     * Código de referencia según nota 9 (2 dígitos)
     * 01 = Anula Documento de Referencia
     * 02 = Corrige monto
     * 04 = Referencia a otro documento
     * 05 = Sustituye comprobante provisional por contingencia
     * 06 = Devolución de mercancía
     * 07 = Sustituye comprobante electrónico
     * 08 = Factura Endosada
     * 09 = Nota de crédito financiera
     * 10 = Nota de débito financiera
     * 11 = Proveedor No Domiciliado
     * 12 = Crédito por exoneración posterior a la facturación
     * 99 = Otros
     */
    @NotBlank(message = "Código de referencia es requerido")
    @Pattern(regexp = "^(01|02|04|05|06|07|08|09|10|11|12|99)$", 
             message = "Código de referencia inválido")
    @Size(min = 2, max = 2, message = "Código de referencia debe tener exactamente 2 caracteres")
    private String codigo;
    
    /**
     * Descripción cuando codigo = '99' (Otros)
     */
    @Size(min = 5, max = 100, message = "Código referencia OTRO debe tener entre 5 y 100 caracteres")
    private String codigoReferenciaOTRO;
    
    /**
     * Razón o descripción de la referencia
     * Explica el motivo de la referencia
     */
    @NotBlank(message = "Razón de referencia es requerida")
    @Size(min = 3, max = 180, message = "Razón debe tener entre 3 y 180 caracteres")
    private String razon;
}