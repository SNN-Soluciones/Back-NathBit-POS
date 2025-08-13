package com.snnsoluciones.backnathbitpos.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.security.Key;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.*;

@Slf4j
@RestController
@RequestMapping("/api/test/certificado")
@RequiredArgsConstructor
@Tag(name = "Test Certificados", description = "Endpoints de prueba para certificados P12")
public class CertificadoTestController {
    
    @Operation(summary = "Probar apertura de certificado P12")
    @PostMapping(value = "/validar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> probarCertificado(
            @RequestPart("certificado") MultipartFile certificado,
            @RequestParam("pin") String pin) {
        
        Map<String, Object> resultado = new HashMap<>();
        
        try {
            log.info("=== INICIANDO PRUEBA DE CERTIFICADO ===");
            log.info("Archivo: {}", certificado.getOriginalFilename());
            log.info("Tamaño: {} bytes", certificado.getSize());
            log.info("Content Type: {}", certificado.getContentType());
            log.info("PIN length: {}", pin.length());
            
            resultado.put("archivo", certificado.getOriginalFilename());
            resultado.put("tamaño", certificado.getSize());
            resultado.put("tipo", certificado.getContentType());
            
            // Intentar diferentes tipos de KeyStore
            String[] keystoreTypes = {"PKCS12", "JKS", "JCEKS"};
            Map<String, Object> intentos = new HashMap<>();
            
            for (String type : keystoreTypes) {
                Map<String, Object> intento = new HashMap<>();
                try {
                    log.info("Intentando con tipo: {}", type);
                    KeyStore keyStore = KeyStore.getInstance(type);
                    
                    // Cargar el keystore
                    byte[] certificadoBytes = certificado.getBytes();
                    keyStore.load(new ByteArrayInputStream(certificadoBytes), pin.toCharArray());
                    
                    intento.put("cargado", true);
                    intento.put("tipo", type);
                    
                    // Obtener información del certificado
                    List<Map<String, Object>> certificados = new ArrayList<>();
                    Enumeration<String> aliases = keyStore.aliases();
                    
                    if (!aliases.hasMoreElements()) {
                        intento.put("error", "No se encontraron certificados en el keystore");
                    }
                    
                    while (aliases.hasMoreElements()) {
                        String alias = aliases.nextElement();
                        Map<String, Object> certInfo = new HashMap<>();
                        certInfo.put("alias", alias);
                        
                        try {
                            // Verificar si es una entrada de clave
                            if (keyStore.isKeyEntry(alias)) {
                                certInfo.put("tipoEntrada", "Clave privada");
                                
                                // Obtener la clave
                                Key key = keyStore.getKey(alias, pin.toCharArray());
                                certInfo.put("algoritmoLlave", key != null ? key.getAlgorithm() : "null");
                                certInfo.put("formatoLlave", key != null ? key.getFormat() : "null");
                            } else if (keyStore.isCertificateEntry(alias)) {
                                certInfo.put("tipoEntrada", "Certificado");
                            }
                            
                            // Obtener el certificado
                            Certificate cert = keyStore.getCertificate(alias);
                            if (cert instanceof X509Certificate) {
                                X509Certificate x509 = (X509Certificate) cert;
                                certInfo.put("subject", x509.getSubjectDN().toString());
                                certInfo.put("issuer", x509.getIssuerDN().toString());
                                certInfo.put("serialNumber", x509.getSerialNumber().toString());
                                certInfo.put("notBefore", x509.getNotBefore().toString());
                                certInfo.put("notAfter", x509.getNotAfter().toString());
                                certInfo.put("sigAlgName", x509.getSigAlgName());
                                certInfo.put("version", x509.getVersion());
                                
                                // Verificar validez
                                try {
                                    x509.checkValidity();
                                    certInfo.put("valido", true);
                                    certInfo.put("estadoValidez", "Certificado válido");
                                } catch (Exception e) {
                                    certInfo.put("valido", false);
                                    certInfo.put("estadoValidez", e.getMessage());
                                }
                            }
                            
                            certificados.add(certInfo);
                            
                        } catch (Exception e) {
                            certInfo.put("errorLeyendoEntrada", e.getMessage());
                            certificados.add(certInfo);
                        }
                    }
                    
                    intento.put("certificados", certificados);
                    intento.put("numeroCertificados", certificados.size());
                    intentos.put(type, intento);
                    
                    // Si llegamos aquí, funcionó
                    resultado.put("exitoso", true);
                    resultado.put("tipoKeyStore", type);
                    resultado.put("detalles", intento);
                    break;
                    
                } catch (Exception e) {
                    log.error("Error con tipo {}: {}", type, e.getMessage());
                    intento.put("cargado", false);
                    intento.put("error", e.getMessage());
                    intento.put("tipoError", e.getClass().getSimpleName());
                    intentos.put(type, intento);
                }
            }
            
            resultado.put("intentos", intentos);
            
            // Si ninguno funcionó
            if (!resultado.containsKey("exitoso")) {
                resultado.put("exitoso", false);
                resultado.put("mensaje", "No se pudo abrir el certificado con ningún tipo de KeyStore");
            }
            
        } catch (Exception e) {
            log.error("Error general procesando certificado", e);
            resultado.put("exitoso", false);
            resultado.put("error", e.getMessage());
            resultado.put("tipoError", e.getClass().getName());
            resultado.put("stackTrace", Arrays.toString(e.getStackTrace()));
        }
        
        return ResponseEntity.ok(resultado);
    }
    
    @Operation(summary = "Probar certificado con diferentes encodings del PIN")
    @PostMapping(value = "/validar-encoding", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> probarCertificadoConEncodings(
            @RequestPart("certificado") MultipartFile certificado,
            @RequestParam("pin") String pin) {
        
        Map<String, Object> resultado = new HashMap<>();
        List<Map<String, Object>> intentos = new ArrayList<>();
        
        try {
            // Probar diferentes formas de procesar el PIN
            String[] pinsAPorbar = {
                pin,                          // Original
                pin.trim(),                   // Sin espacios
                new String(pin.getBytes("UTF-8"), "UTF-8"),  // UTF-8 explícito
                new String(pin.getBytes("ISO-8859-1"), "ISO-8859-1"), // Latin-1
            };
            
            byte[] certificadoBytes = certificado.getBytes();
            
            for (int i = 0; i < pinsAPorbar.length; i++) {
                Map<String, Object> intento = new HashMap<>();
                String pinActual = pinsAPorbar[i];
                intento.put("metodo", obtenerDescripcionMetodo(i));
                intento.put("pinLength", pinActual.length());
                
                try {
                    KeyStore keyStore = KeyStore.getInstance("PKCS12");
                    keyStore.load(new ByteArrayInputStream(certificadoBytes), pinActual.toCharArray());
                    
                    intento.put("exitoso", true);
                    intento.put("mensaje", "PIN correcto con este método");
                    
                    // Si funcionó, obtener info básica
                    Enumeration<String> aliases = keyStore.aliases();
                    if (aliases.hasMoreElements()) {
                        String alias = aliases.nextElement();
                        Certificate cert = keyStore.getCertificate(alias);
                        if (cert instanceof X509Certificate) {
                            X509Certificate x509 = (X509Certificate) cert;
                            intento.put("certificadoPara", x509.getSubjectDN().toString());
                        }
                    }
                    
                    resultado.put("exitoso", true);
                    resultado.put("metodoExitoso", obtenerDescripcionMetodo(i));
                    
                } catch (Exception e) {
                    intento.put("exitoso", false);
                    intento.put("error", e.getMessage());
                }
                
                intentos.add(intento);
            }
            
            resultado.put("intentos", intentos);
            
        } catch (Exception e) {
            resultado.put("error", e.getMessage());
            resultado.put("exitoso", false);
        }
        
        return ResponseEntity.ok(resultado);
    }
    
    private String obtenerDescripcionMetodo(int index) {
        switch (index) {
            case 0: return "PIN original sin modificar";
            case 1: return "PIN con trim()";
            case 2: return "PIN con encoding UTF-8";
            case 3: return "PIN con encoding ISO-8859-1";
            default: return "Método desconocido";
        }
    }
    
    @Operation(summary = "Información básica del archivo")
    @PostMapping(value = "/info-archivo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> infoArchivo(
            @RequestPart("certificado") MultipartFile certificado) {
        
        Map<String, Object> info = new HashMap<>();
        
        try {
            info.put("nombreOriginal", certificado.getOriginalFilename());
            info.put("tamaño", certificado.getSize());
            info.put("tipoContenido", certificado.getContentType());
            info.put("vacio", certificado.isEmpty());
            
            // Leer primeros bytes para verificar formato
            byte[] primerosBytes = new byte[Math.min(20, (int)certificado.getSize())];
            certificado.getInputStream().read(primerosBytes);
            
            // Verificar si es un archivo PKCS#12 (usualmente empieza con ciertos bytes)
            StringBuilder hex = new StringBuilder();
            for (byte b : primerosBytes) {
                hex.append(String.format("%02X ", b));
            }
            info.put("primerosBytes", hex.toString());
            
            // Los archivos PKCS#12 típicamente empiezan con 30 82
            boolean pareceP12 = primerosBytes.length >= 2 && 
                               primerosBytes[0] == 0x30 && 
                               (primerosBytes[1] & 0xFF) == 0x82;
            info.put("pareceSer_PKCS12", pareceP12);
            
        } catch (Exception e) {
            info.put("error", e.getMessage());
        }
        
        return ResponseEntity.ok(info);
    }
}