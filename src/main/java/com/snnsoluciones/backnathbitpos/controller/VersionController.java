package com.snnsoluciones.backnathbitpos.controller;

import com.snnsoluciones.backnathbitpos.dto.info.AppVersionInfo;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/app-version")
public class VersionController {
    
    @GetMapping("/latest")
    public ResponseEntity<AppVersionInfo> getLatestVersion() {
        AppVersionInfo info = AppVersionInfo.builder()
            .versionCode(42)  // Incrementa cada vez que subas APK
            .versionName("1.4.2")
            .downloadUrl("https://tu-servidor.com/downloads/app-latest.apk")
            .forceUpdate(false)  // true = actualización obligatoria
            .changelog("- Corrección de impresoras\n- Mejoras en bitácora")
            .minSupportedVersion(38)  // Versiones anteriores deben actualizar sí o sí
            .build();
            
        return ResponseEntity.ok(info);
    }
}
