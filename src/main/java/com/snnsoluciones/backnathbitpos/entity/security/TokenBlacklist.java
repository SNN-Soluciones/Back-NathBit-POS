package com.snnsoluciones.backnathbitpos.entity.security;

import com.snnsoluciones.backnathbitpos.entity.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Entity
@Table(name = "token_blacklist",
    indexes = {
        @Index(name = "idx_token_blacklist_token", columnList = "token"),
        @Index(name = "idx_token_blacklist_expiration", columnList = "expiration_date")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class TokenBlacklist extends BaseEntity {

    @Column(nullable = false, unique = true, length = 500)
    private String token;

    @Column(nullable = false, length = 255)
    private String username;

    @Column(name = "expiration_date", nullable = false)
    private LocalDateTime expirationDate;

    @Column(name = "blacklisted_at", nullable = false)
    private LocalDateTime blacklistedAt;

    @Column(length = 50)
    private String reason; // LOGOUT, REVOKED, SECURITY_BREACH, etc.

    @Column(length = 500)
    private String description;

    // Método para verificar si el token debe ser eliminado de la blacklist
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expirationDate);
    }
}