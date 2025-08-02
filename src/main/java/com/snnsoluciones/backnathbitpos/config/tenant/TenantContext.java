package com.snnsoluciones.backnathbitpos.config.tenant;

/**
 * Contexto para manejar el tenant actual en cada request.
 * Utiliza ThreadLocal para mantener el tenant aislado por hilo.
 */
public class TenantContext {

  private static final ThreadLocal<String> currentTenant = new InheritableThreadLocal<>();

  private TenantContext() {
    // Utility class
  }

  /**
   * Establece el tenant actual para el hilo de ejecución
   * @param tenant ID del tenant
   */
  public static void setCurrentTenant(String tenant) {
    currentTenant.set(tenant);
  }

  /**
   * Obtiene el tenant actual del hilo de ejecución
   * @return ID del tenant actual
   */
  public static String getCurrentTenant() {
    return currentTenant.get();
  }

  /**
   * Limpia el tenant del contexto actual
   */
  public static void clear() {
    currentTenant.remove();
  }

  /**
   * Verifica si hay un tenant establecido
   * @return true si hay un tenant, false si no
   */
  public static boolean hasTenant() {
    return currentTenant.get() != null;
  }
}