package com.snnsoluciones.backnathbitpos.service.auth.impl;

import com.snnsoluciones.backnathbitpos.service.auth.RateLimiterService;
import lombok.Getter;
import org.springframework.stereotype.Service;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
public class RateLimiterServiceImpl implements RateLimiterService {

  private final ConcurrentHashMap<String, AttemptRecord> attempts = new ConcurrentHashMap<>();
  private static final int MAX_ATTEMPTS = 3;
  private static final long BLOCK_DURATION_MINUTES = 15;

  @Override
  public boolean isBlocked(String key) {
    AttemptRecord record = attempts.get(key);
    if (record == null) return false;

    if (record.isBlocked() && !record.isBlockExpired()) {
      return true;
    }

    if (record.isBlockExpired()) {
      attempts.remove(key);
      return false;
    }

    return false;
  }

  @Override
  public void loginFailed(String key, Object request) {
    attempts.compute(key, (k, record) -> {
      if (record == null) {
        record = new AttemptRecord();
      }
      record.incrementAttempts();
      return record;
    });
  }

  @Override
  public void loginSucceeded(String key) {
    attempts.remove(key);
  }

  @Override
  public int getRemainingAttempts(String key) {
    AttemptRecord record = attempts.get(key);
    if (record == null) return MAX_ATTEMPTS;
    return Math.max(0, MAX_ATTEMPTS - record.getAttempts());
  }

  @Override
  public long getBlockedMinutesRemaining(String key) {
    AttemptRecord record = attempts.get(key);
    if (record == null || !record.isBlocked()) return 0;

    long elapsedMinutes = TimeUnit.MILLISECONDS.toMinutes(
        System.currentTimeMillis() - record.getBlockedTime()
    );
    return Math.max(0, BLOCK_DURATION_MINUTES - elapsedMinutes);
  }

  @Getter
  private static class AttemptRecord {
    private int attempts = 0;
    private long blockedTime = 0;

    public void incrementAttempts() {
      attempts++;
      if (attempts >= MAX_ATTEMPTS) {
        blockedTime = System.currentTimeMillis();
      }
    }

    public boolean isBlocked() {
      return attempts >= MAX_ATTEMPTS;
    }

    public boolean isBlockExpired() {
      if (!isBlocked()) return false;
      long elapsedMinutes = TimeUnit.MILLISECONDS.toMinutes(
          System.currentTimeMillis() - blockedTime
      );
      return elapsedMinutes >= BLOCK_DURATION_MINUTES;
    }

  }
}
