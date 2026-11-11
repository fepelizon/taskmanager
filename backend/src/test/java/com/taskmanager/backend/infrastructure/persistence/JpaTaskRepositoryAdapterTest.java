package com.taskmanager.backend.infrastructure.persistence;

import com.taskmanager.backend.domain.Task;
import com.taskmanager.backend.domain.TaskStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Testes da classe que liga o sistema ao banco de dados (JpaTaskRepositoryAdapter).
// Ela converte a tarefa para o formato do banco, chama o banco e converte a resposta de volta.
// O banco é simulado (mock), então nenhum banco de verdade é usado.
@ExtendWith(MockitoExtension.class)
class JpaTaskRepositoryAdapterTest {

    private static final Instant CREATED_AT = Instant.parse("2026-01-01T10:00:00Z");

    @Mock
    private JpaTaskRepository repository;

    private JpaTaskRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new JpaTaskRepositoryAdapter(repository, new TaskPersistenceMapper());
    }

    @Test
    void saveShouldPersistEntityAndReturnTaskWithGeneratedId() { // Ao salvar uma tarefa nova, o método save deve devolver a tarefa com o id que o banco criou
        // Preparação: o banco salva e devolve a tarefa já com o id 1
        when(repository.save(any(TaskEntity.class))).thenReturn(createEntity(1L, "Title", TaskStatus.PENDING));

        // Ação: salva uma tarefa nova, ainda sem id (null)
        Task result = adapter.save(new Task(null, "Title", "Description", TaskStatus.PENDING, CREATED_AT));

        // Verificação: devolveu a tarefa com o id criado pelo banco
        assertThat(result).isEqualTo(new Task(1L, "Title", "Description", TaskStatus.PENDING, CREATED_AT));
    }

    @Test
    void findByIdShouldReturnTaskWhenEntityExists() { // Ao buscar uma tarefa que existe no banco, o método findById deve devolver essa tarefa
        // Preparação: o banco encontra a tarefa 5
        when(repository.findById(5L)).thenReturn(Optional.of(createEntity(5L, "Title", TaskStatus.DONE)));

        // Ação
        Optional<Task> result = adapter.findById(5L);

        // Verificação: encontrou a tarefa, com os dados certos
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(new Task(5L, "Title", "Description", TaskStatus.DONE, CREATED_AT));
    }

    @Test
    void findByIdShouldReturnEmptyWhenEntityDoesNotExist() { // Ao buscar uma tarefa que não existe no banco, o método findById deve devolver um resultado vazio
        // Preparação: o banco não encontra a tarefa 999
        when(repository.findById(999L)).thenReturn(Optional.empty());

        // Ação
        Optional<Task> result = adapter.findById(999L);

        // Verificação: o resultado está vazio
        assertThat(result).isEmpty();
    }

    @Test
    void findAllShouldReturnAllEntitiesAsTasks() { // O método findAll deve devolver todas as tarefas que estão no banco, na mesma ordem
        // Preparação: o banco devolve duas tarefas
        TaskEntity entity1 = createEntity(1L, "T1", TaskStatus.PENDING);
        TaskEntity entity2 = createEntity(2L, "T2", TaskStatus.IN_PROGRESS);
        when(repository.findAll()).thenReturn(List.of(entity1, entity2));

        // Ação
        List<Task> result = adapter.findAll();

        // Verificação: devolveu as duas tarefas convertidas, na mesma ordem
        assertThat(result).containsExactly(
                new Task(1L, "T1", "Description", TaskStatus.PENDING, CREATED_AT),
                new Task(2L, "T2", "Description", TaskStatus.IN_PROGRESS, CREATED_AT)
        );
    }

    @Test
    void deleteByIdShouldDeleteEntityFromRepository() { // O método deleteById deve pedir ao banco para excluir a tarefa com o id informado
        // Ação
        adapter.deleteById(3L);

        // Verificação: mandou o banco excluir a tarefa 3
        verify(repository).deleteById(3L);
    }

    // Cria uma tarefa no formato do banco, para não repetir estas linhas em todos os testes
    private TaskEntity createEntity(Long id, String title, TaskStatus status) {
        TaskEntity entity = new TaskEntity();
        entity.setId(id);
        entity.setTitle(title);
        entity.setDescription("Description");
        entity.setStatus(status);
        entity.setCreatedAt(CREATED_AT);
        return entity;
    }
}
