// AsyncConfig.java
package com.snnsoluciones.backnathbitpos.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean("invoiceGenExecutor")
    public TaskExecutor invoiceGenExecutor() {
        ThreadPoolTaskExecutor exec = new ThreadPoolTaskExecutor();
        exec.setCorePoolSize(1);          // 1 hilo → serializa emisión y respeta las pausas
        exec.setMaxPoolSize(2);
        exec.setQueueCapacity(100);       // cola de jobs pendientes
        exec.setThreadNamePrefix("invoice-gen-");
        exec.initialize();
        return exec;
    }
}