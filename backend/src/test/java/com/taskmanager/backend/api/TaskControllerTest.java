package com.taskmanager.backend.api;

import com.taskmanager.backend.application.port.in.CreateTaskUseCase;
import com.taskmanager.backend.application.port.in.DeleteTaskUseCase;
import com.taskmanager.backend.application.port.in.GetTaskUseCase;
import com.taskmanager.backend.application.port.in.ListTasksUseCase;
import com.taskmanager.backend.application.port.in.UpdateTaskUseCase;
import com.taskmanager.backend.domain.Task;
import com.taskmanager.backend.domain.TaskNotFoundException;
import com.taskmanager.backend.domain.TaskStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TaskController.class)
@Import(GlobalExceptionHandler.class)
class TaskControllerTest {

    private static final Instant CREATED_AT = Instant.parse("2026-01-01T10:00:00Z"); // Data fixa usada em todos os testes

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateTaskUseCase createTaskUseCase;

    @MockitoBean
    private ListTasksUseCase listTasksUseCase;

    @MockitoBean
    private GetTaskUseCase getTaskUseCase;

    @MockitoBean
    private UpdateTaskUseCase updateTaskUseCase;

    @MockitoBean
    private DeleteTaskUseCase deleteTaskUseCase;

    @Test
    void createShouldReturn201AndTaskPayload() throws Exception { // Criar tarefa com dados válidos
        // Preparação: quando o controller pedir para criar "Title" / "Description", devolve a tarefa com id 1
        Task createdTask = new Task(1L, "Title", "Description", TaskStatus.PENDING, CREATED_AT);
        when(createTaskUseCase.create(new CreateTaskUseCase.CreateTaskCommand("Title", "Description"))).thenReturn(createdTask);

        // Ação: envia POST /api/tasks com o JSON da tarefa
        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "title": "Title",
                                    "description": "Description"
                                }
                                """))
                // Verificação
                .andExpect(status().isCreated()) // HTTP 201
                .andExpect(jsonPath("$.id").value(1)) // O JSON da resposta tem o id 1
                .andExpect(jsonPath("$.title").value("Title"))
                .andExpect(jsonPath("$.status").value("PENDING")); // Tarefa nova começa como Pendente
    }

    @Test
    void createShouldReturn400WhenTitleIsEmpty() throws Exception { // Título vazio não pode ser aceito
        // Ação: envia uma tarefa com o título vazio
        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "title": "",
                                    "description": "Description"
                                }
                                """))
                // Verificação
                .andExpect(status().isBadRequest()) // HTTP 400
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR")) // Código de erro de validação
                .andExpect(jsonPath("$.fieldErrors.title").exists()); // A resposta aponta o erro no campo "title"

        verifyNoInteractions(createTaskUseCase); // A criação nem foi chamada, ou seja, nada foi salvo
    }

    @Test
    void createShouldReturn400WhenTitleHasOnlyWhitespace() throws Exception { // Título só com espaços também não pode ser aceito
        // Ação: envia uma tarefa com o título só com espaços
        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "title": "   ",
                                    "description": "Description"
                                }
                                """))
                // Verificação
                .andExpect(status().isBadRequest()) // HTTP 400
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors.title").exists());

        verifyNoInteractions(createTaskUseCase); // Nada foi salvo
    }

    @Test
    void createShouldReturn201WhenTitleHas200Characters() throws Exception { // O título pode ter até 200 caracteres, então 200 tem que ser aceito
        // Preparação
        String title = "A".repeat(200); // Texto com 200 letras "A"
        Task createdTask = new Task(1L, title, "Description", TaskStatus.PENDING, CREATED_AT);
        when(createTaskUseCase.create(new CreateTaskUseCase.CreateTaskCommand(title, "Description"))).thenReturn(createdTask);
        String json = """
                {
                    "title": "%s",
                    "description": "Description"
                }
                """.formatted(title); // O %s é trocado pelo título de 200 letras

        // Ação
        mockMvc.perform(post("/api/tasks").contentType(MediaType.APPLICATION_JSON).content(json))
                // Verificação
                .andExpect(status().isCreated()) // HTTP 201
                .andExpect(jsonPath("$.title").value(title)); // O título voltou inteiro
    }

    @Test
    void createShouldReturn400WhenTitleExceeds200Characters() throws Exception { // 201 caracteres já passa do limite e tem que ser recusado
        // Preparação
        String title = "A".repeat(201);
        String json = """
                {
                    "title": "%s",
                    "description": "Description"
                }
                """.formatted(title);

        // Ação
        mockMvc.perform(post("/api/tasks").contentType(MediaType.APPLICATION_JSON).content(json))
                // Verificação
                .andExpect(status().isBadRequest()) // HTTP 400
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors.title").exists());

        verifyNoInteractions(createTaskUseCase); // Nada foi salvo
    }

    @Test
    void listShouldReturnTasks() throws Exception { // Listar tarefas
        // Preparação: a listagem devolve duas tarefas
        Task task1 = new Task(1L, "T1", "D1", TaskStatus.PENDING, CREATED_AT);
        Task task2 = new Task(2L, "T2", "D2", TaskStatus.DONE, CREATED_AT);
        when(listTasksUseCase.list()).thenReturn(List.of(task1, task2));

        // Ação: envia GET /api/tasks
        mockMvc.perform(get("/api/tasks"))
                // Verificação
                .andExpect(status().isOk()) // HTTP 200
                .andExpect(jsonPath("$[0].id").value(1)) // $[0] é o primeiro item da lista
                .andExpect(jsonPath("$[1].id").value(2)) // $[1] é o segundo item da lista
                .andExpect(jsonPath("$[1].status").value("DONE"));
    }

    @Test
    void listShouldReturn500WithoutInternalDetailsWhenUnexpectedErrorOccurs() throws Exception { // Um erro inesperado deve virar HTTP 500
        // Preparação: a listagem dá um erro com uma mensagem interna (como se o banco tivesse caído)
        when(listTasksUseCase.list()).thenThrow(new IllegalStateException("database connection lost"));

        // Ação
        mockMvc.perform(get("/api/tasks"))
                // Verificação
                .andExpect(status().isInternalServerError()) // HTTP 500
                .andExpect(jsonPath("$.code").value("UNEXPECTED_ERROR"))
                // A mensagem é a amigável. A mensagem interna "database connection lost" não aparece
                .andExpect(jsonPath("$.detail").value("Ocorreu um erro inesperado. Tente novamente."));
    }

    @Test
    void getByIdShouldReturn200WhenTaskExists() throws Exception {  // consultar uma tarefa que existe
        // Preparação: consultar o id 7 devolve esta tarefa
        Task task = new Task(7L, "Title", "Description", TaskStatus.IN_PROGRESS, CREATED_AT);
        when(getTaskUseCase.getById(7L)).thenReturn(task);

        // Ação: envia GET /api/tasks/7
        mockMvc.perform(get("/api/tasks/7"))
                // Verificação: HTTP 200 e todos os campos da tarefa no JSON
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.title").value("Title"))
                .andExpect(jsonPath("$.description").value("Description"))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.createdAt").value("2026-01-01T10:00:00Z"));
    }

    @Test
    void getByIdShouldReturn404WithProblemDetailContract() throws Exception { // consultar uma tarefa que não existe
        // Preparação: consultar o id 99 dá o erro "tarefa não encontrada"
        when(getTaskUseCase.getById(99L)).thenThrow(new TaskNotFoundException(99L));

        // Ação
        mockMvc.perform(get("/api/tasks/99"))
                // Verificação
                .andExpect(status().isNotFound()) // HTTP 404
                .andExpect(jsonPath("$.title").value("Task not found"))
                .andExpect(jsonPath("$.code").value("TASK_NOT_FOUND"))
                .andExpect(jsonPath("$.userSafe").value(true)); // A mensagem pode ser mostrada ao usuário
    }

    @Test
    void updateShouldReturn200WithUpdatedTask() throws Exception {  // atualizar uma tarefa
        // Preparação: atualizar o id 1 com estes dados devolve a tarefa atualizada
        UpdateTaskUseCase.UpdateTaskCommand command =
                new UpdateTaskUseCase.UpdateTaskCommand("New title", "New description", TaskStatus.IN_PROGRESS);
        Task updatedTask = new Task(1L, "New title", "New description", TaskStatus.IN_PROGRESS, CREATED_AT);
        when(updateTaskUseCase.update(1L, command)).thenReturn(updatedTask);

        // Ação: envia PUT /api/tasks/1 com os dados novos
        mockMvc.perform(put("/api/tasks/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "title": "New title",
                                    "description": "New description",
                                    "status": "IN_PROGRESS"
                                }
                                """))
                // Verificação
                .andExpect(status().isOk()) // HTTP 200
                .andExpect(jsonPath("$.title").value("New title"))
                .andExpect(jsonPath("$.description").value("New description"))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

        verify(updateTaskUseCase).update(1L, command); // O controller pediu a atualização do id 1 com exatamente esses dados
    }

    @Test
    void updateShouldReturn404WhenTaskDoesNotExist() throws Exception { // Atualizar uma tarefa que não existe
        // Preparação: atualizar o id 999 dá o erro "tarefa não encontrada"
        UpdateTaskUseCase.UpdateTaskCommand command =
                new UpdateTaskUseCase.UpdateTaskCommand("Title", "Description", TaskStatus.DONE);
        when(updateTaskUseCase.update(999L, command)).thenThrow(new TaskNotFoundException(999L));

        // Ação
        mockMvc.perform(put("/api/tasks/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "title": "Title",
                                    "description": "Description",
                                    "status": "DONE"
                                }
                                """))
                // Verificação
                .andExpect(status().isNotFound()) // HTTP 404
                .andExpect(jsonPath("$.code").value("TASK_NOT_FOUND"));
    }

    @Test
    void updateShouldReturn400WithFieldErrorsWhenValidationFails() throws Exception { // validação na atualização (título vazio e status vazio)
        // Ação: envia título vazio e status nulo
        mockMvc.perform(put("/api/tasks/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "title": "",
                                    "description": "Description",
                                    "status": null
                                }
                                """))
                // Verificação: HTTP 400 com um erro para cada campo inválido
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation error"))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.userSafe").value(true))
                .andExpect(jsonPath("$.fieldErrors.title").exists())
                .andExpect(jsonPath("$.fieldErrors.status").exists());
    }

    @Test
    void deleteShouldReturn204() throws Exception { // excluir tarefa
        // Ação: envia DELETE /api/tasks/1
        mockMvc.perform(delete("/api/tasks/1"))
                // Verificação
                .andExpect(status().isNoContent()); // HTTP 204

        verify(deleteTaskUseCase).delete(1L);
    }

    @Test
    void deleteShouldReturn404WhenTaskDoesNotExist() throws Exception { // excluir uma tarefa que não existe
        // Preparação: excluir o id 500 dá o erro "tarefa não encontrada"
        doThrow(new TaskNotFoundException(500L)).when(deleteTaskUseCase).delete(500L);

        // Ação
        mockMvc.perform(delete("/api/tasks/500"))
                // Verificação
                .andExpect(status().isNotFound()) // HTTP 404
                .andExpect(jsonPath("$.code").value("TASK_NOT_FOUND"));
    }
}