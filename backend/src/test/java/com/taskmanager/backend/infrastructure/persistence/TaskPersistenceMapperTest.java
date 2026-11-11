package com.taskmanager.backend.infrastructure.persistence;

import com.taskmanager.backend.domain.Task;
import com.taskmanager.backend.domain.TaskStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

// Testes do conversor entre a tarefa do sistema (Task) e a tarefa do banco de dados (TaskEntity).
// Se o conversor esquecer algum campo, essa informação se perde ao salvar ou ao ler do banco.
// Não precisa de mocks: o conversor não depende de nada.
class TaskPersistenceMapperTest {

    private static final Instant CREATED_AT = Instant.parse("2026-01-01T10:00:00Z");

    private final TaskPersistenceMapper mapper = new TaskPersistenceMapper();

    @Test
    void toEntityShouldCopyAllFieldsOfTask() { // Ao converter a tarefa para o formato do banco, o método toEntity deve copiar todos os campos
        // Preparação: uma tarefa do sistema com todos os campos preenchidos
        Task task = new Task(1L, "Title", "Description", TaskStatus.IN_PROGRESS, CREATED_AT);

        // Ação: converte para o formato do banco
        TaskEntity entity = mapper.toEntity(task);

        // Verificação: id, título, descrição, status e data de criação foram copiados
        assertThat(entity.getId()).isEqualTo(1L);
        assertThat(entity.getTitle()).isEqualTo("Title");
        assertThat(entity.getDescription()).isEqualTo("Description");
        assertThat(entity.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
        assertThat(entity.getCreatedAt()).isEqualTo(CREATED_AT);
    }

    @Test
    void toDomainShouldCopyAllFieldsOfEntity() { // Ao converter a tarefa que veio do banco para o formato do sistema, o método toDomain deve copiar todos os campos
        // Preparação: uma tarefa no formato do banco, com todos os campos preenchidos
        TaskEntity entity = new TaskEntity();
        entity.setId(2L);
        entity.setTitle("Title");
        entity.setDescription("Description");
        entity.setStatus(TaskStatus.DONE);
        entity.setCreatedAt(CREATED_AT);

        // Ação: converte para o formato do sistema
        Task task = mapper.toDomain(entity);

        // Verificação: a tarefa do sistema tem o mesmo id, título, descrição, status e data de criação
        assertThat(task).isEqualTo(new Task(2L, "Title", "Description", TaskStatus.DONE, CREATED_AT));
    }
}
