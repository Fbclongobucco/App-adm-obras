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

/** APP_VIRTUAL_THREADS=false: o atendimento volta para threads de plataforma. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "APP_VIRTUAL_THREADS=false")
@AutoConfigureTestRestTemplate
@ActiveProfiles("test")
@Import(ThreadProbe.class)
class PlatformThreadsIntegrationTest {

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private ThreadProbe probe;

    @Test
    @DisplayName("Com virtual threads desligadas, a requisicao deve ser atendida em thread de plataforma")
    void requestRunsOnPlatformThread() {
        assertThat(new AdminApi(rest).get("/api/test/thread").getBody()).containsEntry("virtual", false);
    }

    @Test
    @DisplayName("Com virtual threads desligadas, a tarefa agendada deve rodar em thread de plataforma")
    void scheduledTaskRunsOnPlatformThread() throws Exception {
        assertThat(probe.scheduledVirtual().get(10, TimeUnit.SECONDS)).isFalse();
    }
}
