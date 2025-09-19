// JobRegistry.java
package com.snnsoluciones.backnathbitpos.jobs;

import lombok.*;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class JobRegistry {
    @Getter
    public enum Status { PENDING, RUNNING, SUCCESS, FAILED, CANCELED }

    @Data @Builder
    public static class JobInfo {
        private UUID jobId;
        private Status status;
        private String message;
        private int emitidos;          // facturas emitidas
        private Integer objetivoTickets; // opcional, si lo calculás
        private Instant startedAt;
        private Instant finishedAt;
        private String lastError;
    }

    private static final Map<UUID, JobInfo> JOBS = new ConcurrentHashMap<>();
    private static final Map<UUID, Boolean> CANCEL_FLAGS = new ConcurrentHashMap<>();

    public static UUID newJob() {
        UUID id = UUID.randomUUID();
        JOBS.put(id, JobInfo.builder()
                .jobId(id).status(Status.PENDING).message("En cola")
                .emitidos(0).startedAt(null).finishedAt(null).build());
        CANCEL_FLAGS.put(id, false);
        return id;
    }

    public static JobInfo get(UUID id) { return JOBS.get(id); }
    public static void update(JobInfo info) { JOBS.put(info.getJobId(), info); }
    public static void cancel(UUID id) { CANCEL_FLAGS.put(id, true); }
    public static boolean isCanceled(UUID id) { return CANCEL_FLAGS.getOrDefault(id, false); }
}