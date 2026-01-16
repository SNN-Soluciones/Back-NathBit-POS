package com.snnsoluciones.backnathbitpos.enums;

/**
 * Tipos de productos en el sistema
 * Define la naturaleza y comportamiento del producto
 */
public enum TipoProducto {
    MIXTO("MIXTO", "Se vende y/o es ingrediente"),
    MATERIA_PRIMA("MATERIA_PRIMA", "Solo ingrediente, no se vende"),
    VENTA("VENTA", "Solo para venta directa"),
    COMBO("COMBO", "Conjunto con precio especial"),
    COMPUESTO("COMPUESTO", "Personalizable con opciones"),
    CATEGORIA_MENU("CATEGORIA_MENU", "Categoría que agrupa productos");

    private final String codigo;
    private final String descripcion;

    TipoProducto(String codigo, String descripcion) {
        this.codigo = codigo;
        this.descripcion = descripcion;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getDescripcion() {
        return descripcion;
    }
}