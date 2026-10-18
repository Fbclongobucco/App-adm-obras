package com.longobuccodev.app_adm_obras.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * So para testes: informa se o atendimento HTTP e as tarefas agendadas rodam em virtual thread.
 * Importar com {@code @Import(ThreadProbe.class)}.
 */
@TestConfiguration
public class ThreadProbe {

    // Por contexto (nao estatico): contextos com modos de thread diferentes convivem na suite
    private final CompletableFuture<Boolean> scheduledVirtual = new CompletableFuture<>();

    @Scheduled(fixedDelay = 50)
    void probeScheduler() {
        scheduledVirtual.complete(Thread.currentThread().isVirtual());
    }

    /** Concluido com {@code isVirtual()} da primeira execucao agendada deste contexto. */
    public CompletableFuture<Boolean> scheduledVirtual() {
        return scheduledVirtual;
    }

    @RestController
    static class Endpoint {

        @GetMapping("/api/test/thread")
        Map<String, Object> thread() {
            return Map.of("virtual", Thread.currentThread().isVirtual());
        }
    }
}
