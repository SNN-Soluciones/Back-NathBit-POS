package com.snnsoluciones.backnathbitpos.sign;

import com.snnsoluciones.backnathbitpos.enums.mh.TipoDocumento;

public interface SignerService {
  /**
   * Firma el XML (bytes UTF-8) en XAdES-EPES Enveloped usando el certificado .p12
   * asociado a la empresa (resuelto via repos y S3).
   *
   * @param xmlUnsigned bytes del XML sin firmar (UTF-8, exactamente como se generó)
   * @param empresaId   id de la empresa para resolver credenciales
   * @param tipo        FE/TE/NC/ND/FEC/FEE/REP (para ubicar el root y role)
   * @return bytes del XML firmado (NO re-pretty-print)
   */
  byte[] signXmlForEmpresa(byte[] xmlUnsigned, Long empresaId, TipoDocumento tipo);
}