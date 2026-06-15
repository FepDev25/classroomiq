package com.classroomiq.backend.entrega.procesamiento;

import java.util.concurrent.Executor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Executor dedicado al procesamiento de entregas en background. Lleva el {@link
 * TenantTaskDecorator} para propagar el tenant al hilo de trabajo como red de seguridad.
 */
@Configuration
@EnableAsync
class AsyncConfig {

    @Bean(name = "procesamientoExecutor")
    Executor procesamientoExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("proc-entrega-");
        executor.setTaskDecorator(new TenantTaskDecorator());
        executor.initialize();
        return executor;
    }
}
