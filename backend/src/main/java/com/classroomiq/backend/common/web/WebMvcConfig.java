package com.classroomiq.backend.common.web;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configura el manejo asíncrono de Spring MVC. Sin esto, MVC sirve las respuestas asíncronas con un
 * {@code SimpleAsyncTaskExecutor} (un hilo nuevo por tarea, no apto para producción) — en este
 * proyecto, el stream SSE de progreso ({@code Flux<ServerSentEvent>} del Hito 6). Aquí se le asigna
 * un pool acotado y dedicado, separado del executor de procesamiento de entregas.
 *
 * <p>El timeout async se deshabilita ({@code -1}): los streams SSE son de larga duración y su
 * liveness se mantiene con el keep-alive del endpoint; el cierre lo decide el cliente al
 * desconectarse. Sin esto, el timeout por defecto del contenedor (~30 s) cerraría el stream.
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
        configurer.setTaskExecutor(mvcAsyncTaskExecutor());
        configurer.setDefaultTimeout(-1);
    }

    @Bean
    AsyncTaskExecutor mvcAsyncTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(16);
        executor.setQueueCapacity(64);
        executor.setThreadNamePrefix("mvc-async-");
        executor.initialize();
        return executor;
    }
}
