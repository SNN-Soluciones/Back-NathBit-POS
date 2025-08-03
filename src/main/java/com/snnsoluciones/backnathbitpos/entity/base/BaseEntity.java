package com.snnsoluciones.backnathbitpos.entity.base;

import com.snnsoluciones.backnathbitpos.config.tenant.TenantContext;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

@MappedSuperclass
@Getter
@Setter
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity implements Serializable {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(updatable = false, nullable = false)
  private UUID id;

  @Column(name = "tenant_id", nullable = false)
  private String tenantId;

  @CreatedDate
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @LastModifiedDate
  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  @Column(name = "activo", nullable = false)
  private Boolean activo = true;

  @Version
  @Column(name = "version")
  private Long version;

  @PrePersist
  protected void prePersist() {
    if (this.tenantId == null) {
      this.tenantId = TenantContext.getCurrentTenant();
    }
    if (this.activo == null) {
      this.activo = true;
    }
  }

  @PreUpdate
  protected void preUpdate() {
    this.tenantId = TenantContext.getCurrentTenant();
  }
}