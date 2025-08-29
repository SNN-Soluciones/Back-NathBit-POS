package com.snnsoluciones.backnathbitpos.sign;

import com.snnsoluciones.backnathbitpos.enums.mh.TipoDocumento;

public class XadesClaimedRoleHelper {
    /**
     * Devuelve el rol reclamado según el tipo de documento.
     * - Para MensajeReceptor -> "Receptor"
     * - Para los demás -> null (sin rol)
     */
    public String resolveClaimedRole(TipoDocumento tipo) {
        if (tipo == null) return null;
        switch (tipo) {
            case MENSAJE_RECEPTOR: // Ajusta si tu enum lo llama distinto
                return "Receptor";
            default:
                return null;
        }
    }
}