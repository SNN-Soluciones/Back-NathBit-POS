package com.snnsoluciones.backnathbitpos.service.auth;

public interface RateLimiterService {
  boolean isBlocked(String key);
  void loginFailed(String key, Object request);
  void loginSucceeded(String key);
  int getRemainingAttempts(String key);
  long getBlockedMinutesRemaining(String key);
}
