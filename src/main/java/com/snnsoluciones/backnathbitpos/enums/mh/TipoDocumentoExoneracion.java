package com.snnsoluciones.backnathbitpos.enums.mh;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TipoDocumentoExoneracion {
    COMPRAS_AUTORIZADAS_DGT("01", "Compras autorizadas por la Dirección General de Tributación"),
    VENTAS_EXENTAS_DIPLOMATICOS("02", "Ventas exentas a diplomáticos"),
    AUTORIZADO_POR_LEY_ESPECIAL("03", "Autorizado por Ley especial"),
    EXENCIONES_DGH_AUT_LOCAL_GENERICA("04", "Exenciones Dirección General de Hacienda Autorización Local Genérica"),
    EXENCIONES_DGH_TRANSITORIO_V("05", "Exenciones Dirección General de Hacienda Transitorio V (servicios de ingeniería, arquitectura, topografía obra civil)"),
    SERVICIOS_TURISTICOS_ICT("06", "Servicios turísticos inscritos ante el Instituto Costarricense de Turismo (ICT)"),
    TRANSITORIO_XVII_RECICLAJE("07", "Transitorio XVII (Recolección, Clasificación, almacenamiento de Reciclaje y reutilizable)"),
    EXONERACION_ZONA_FRANCA("08", "Exoneración a Zona Franca"),
    EXONERACION_SERV_COMP_EXPORTACION_ART11_RLIVA("09", "Exoneración de servicios complementarios para la exportación artículo 11 RLIVA"),
    ORGANO_CORPORACIONES_MUNICIPALES("10", "Órgano de las corporaciones municipales"),
    EXENCIONES_DGH_AUT_IMPUESTO_LOCAL_CONCRETA("11", "Exenciones Dirección General de Hacienda Autorización de Impuesto Local Concreta"),
    OTROS("99", "Otros");

    private final String codigo;
    private final String descripcion;

    public static TipoDocumentoExoneracion fromCodigo(String codigo) {
        for (TipoDocumentoExoneracion t : values()) {
            if (t.codigo.equals(codigo)) return t;
        }
        throw new IllegalArgumentException("Código de tipo documento exoneración no válido: " + codigo);
    }
}