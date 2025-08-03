package com.snnsoluciones.backnathbitpos.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditEventResponse {
    
    private String eventType;
    private LocalDateTime eventDate;
    private String ipAddress;
    private String userAgent;
    private String details;
    private Boolean success;
    private String errorMessage;
}