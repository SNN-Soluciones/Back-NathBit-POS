package com.snnsoluciones.backnathbitpos.dto.info;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AppVersionInfo {
    private int versionCode;
    private String versionName;
    private String downloadUrl;
    private boolean forceUpdate;
    private String changelog;
    private Integer minSupportedVersion;
}