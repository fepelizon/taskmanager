package com.taskmanager.backend.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HealthController.class) // Sobe só a parte web do Spring, com o HealthController
class HealthControllerTest {

    @Autowired
    private MockMvc mockMvc; // Faz as requisições HTTP para o controller, sem precisar subir o servidor

    @Test
    void indexShouldReturn200WithRunningMessage() throws Exception {
        // Ação: envia GET /
        mockMvc.perform(get("/"))
                // Verificação
                .andExpect(status().isOk()) // HTTP 200
                .andExpect(content().string("Task Manager backend is running")); // Mensagem de que o backend está no ar
    }
}
