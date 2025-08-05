package com.snnsoluciones.backnathbitpos.enums;

import lombok.Getter;

/**
 * Tipos de empresa según el giro de negocio
 */
@Getter
public enum TipoEmpresa {

        RESTAURANTE("Restaurante", "Servicio completo con mesas"),
        CAFETERIA("Cafetería", "Café y comida ligera"),
        BAR("Bar", "Bebidas y comida de bar"),
        COMIDA_RAPIDA("Comida Rápida", "Servicio rápido"),
        FOOD_TRUCK("Food Truck", "Cocina móvil"),
        PANADERIA("Panadería", "Pan y repostería"),
        SODA("Soda", "Comida típica costarricense"),
        VENTANA("Ventana", "Solo para llevar"),
        OTRO("Otro", "Otro tipo de negocio");

        private final String nombre;
        private final String descripcion;

        TipoEmpresa(String nombre, String descripcion) {
                this.nombre = nombre;
                this.descripcion = descripcion;
        }

        /**
         * Indica si el tipo de empresa maneja mesas
         */
        public boolean manejaMesas() {
                return this == RESTAURANTE || this == CAFETERIA ||
                    this == BAR || this == SODA;
        }

        /**
         * Indica si es principalmente para llevar
         */
        public boolean esParaLlevar() {
                return this == FOOD_TRUCK || this == VENTANA ||
                    this == COMIDA_RAPIDA;
        }
}