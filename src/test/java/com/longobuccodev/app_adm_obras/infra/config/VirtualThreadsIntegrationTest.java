package com.longobuccodev.app_adm_obras.infra.config;

import com.longobuccodev.app_adm_obras.support.AdminApi;
import com.longobuccodev.app_adm_obras.support.ThreadProbe;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/** Configuracao padrao: requisicoes e tarefas agendadas em virtual threads (spec virtual-thread-execution). */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@ActiveProfiles("test")
@Import(ThreadProbe.class)
class VirtualThreadsIntegrationTest {

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private ThreadProbe probe;

    @Test
    @DisplayName("Requisicao deve ser atendida em virtual thread por padrao")
    void requestRunsOnVirtualThread() {
        assertThat(new AdminApi(rest).get("/api/test/thread").getBody()).containsEntry("virtual", true);
    }

    @Test
    @DisplayName("Tarefa agendada deve rodar em virtual thread por padrao")
    void scheduledTaskRunsOnVirtualThread() throws Exception {
        assertThat(probe.scheduledVirtual().get(10, TimeUnit.SECONDS)).isTrue();
    }
}
