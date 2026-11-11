package com.taskmanager.backend.application;

import com.taskmanager.backend.application.port.in.CreateTaskUseCase;
import com.taskmanager.backend.application.port.in.UpdateTaskUseCase;
import com.taskmanager.backend.application.port.out.TaskEventPublisherPort;
import com.taskmanager.backend.application.port.out.TaskRepositoryPort;
import com.taskmanager.backend.domain.Task;
import com.taskmanager.backend.domain.TaskNotFoundException;
import com.taskmanager.backend.domain.TaskStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

// Testes das regras de negócio das tarefas
@ExtendWith(MockitoExtension.class)
class TaskApplicationServiceTest {

    private static final Instant CREATED_AT = Instant.parse("2026-01-01T10:00:00Z"); // Data fixa usada em todos os testes

    @Mock
    private TaskRepositoryPort taskRepository;

    @Mock
    private TaskEventPublisherPort taskEventPublisher;

    @InjectMocks
    private TaskApplicationService service;

    @Test
    void createShouldPersistTaskAndPublishCreatedEvent() { // criar uma tarefa salva no banco e publica
        // Preparação: o banco devolve a tarefa salva com o id 1
        Task savedTask = new Task(1L, "Title", "Description", TaskStatus.PENDING, CREATED_AT);
        when(taskRepository.save(any(Task.class))).thenReturn(savedTask);

        // Ação
        Task result = service.create(new CreateTaskUseCase.CreateTaskCommand("Title", "Description"));

        // Verificação
        assertThat(result).isEqualTo(savedTask); // Devolveu a tarefa salva
        verify(taskEventPublisher).publishTaskCreated(savedTask); // Publicou o evento de criação
    }

    @Test
    void createShouldSaveNewTaskAsPendingWithoutId() { // a tarefa nova vai para o banco sem id e com status Pendente
        // Preparação
        Task savedTask = new Task(1L, "Title", "Description", TaskStatus.PENDING, CREATED_AT);
        when(taskRepository.save(any(Task.class))).thenReturn(savedTask);

        // Ação
        service.create(new CreateTaskUseCase.CreateTaskCommand("Title", "Description"));

        // Verificação: pega a tarefa que o serviço mandou para o banco e confere os campos.
        // Não dá para comparar a tarefa inteira porque a data de criação é a hora atual
        ArgumentCaptor<Task> captor = ArgumentCaptor.forClass(Task.class); // O captor "guarda" o que foi enviado ao save
        verify(taskRepository).save(captor.capture());
        Task sentToDatabase = captor.getValue();
        assertThat(sentToDatabase.id()).isNull(); // Sem id: quem cria o id é o banco
        assertThat(sentToDatabase.title()).isEqualTo("Title");
        assertThat(sentToDatabase.description()).isEqualTo("Description");
        assertThat(sentToDatabase.status()).isEqualTo(TaskStatus.PENDING); // Toda tarefa nova começa como Pendente
        assertThat(sentToDatabase.createdAt()).isNotNull(); // A data de criação foi preenchida
    }

    @Test
    void getByIdShouldReturnTaskWhenItExists() { // consultar uma tarefa que existe
        // Preparação: o banco encontra a tarefa 5
        Task task = new Task(5L, "Title", "Description", TaskStatus.DONE, CREATED_AT);
        when(taskRepository.findById(5L)).thenReturn(Optional.of(task));

        // Ação
        Task result = service.getById(5L);

        // Verificação
        assertThat(result).isEqualTo(task);
    }

    @Test
    void getByIdShouldThrowWhenTaskDoesNotExist() { // consultar uma tarefa que não existe
        // Preparação: o banco não encontra a tarefa 999 (Optional.empty = "vazio")
        when(taskRepository.findById(999L)).thenReturn(Optional.empty());

        // Ação e verificação: consultar o id 999 tem que dar o erro "tarefa não encontrada"
        assertThatThrownBy(() -> service.getById(999L)).isInstanceOf(TaskNotFoundException.class);
    }

    @Test
    void updateShouldPersistUpdatedTaskAndPublishUpdatedEvent() { // atualizar uma tarefa salva no banco e publica
        // Preparação
        Task oldTask = new Task(10L, "Old title", "Old description", TaskStatus.PENDING, CREATED_AT); // Como está no banco
        Task newTask = new Task(10L, "New title", "New description", TaskStatus.DONE, CREATED_AT); // Como deve ficar
        when(taskRepository.findById(10L)).thenReturn(Optional.of(oldTask));
        when(taskRepository.save(newTask)).thenReturn(newTask);

        // Ação
        Task result = service.update(10L, new UpdateTaskUseCase.UpdateTaskCommand("New title", "New description", TaskStatus.DONE));

        // Verificação
        assertThat(result).isEqualTo(newTask); // Devolveu a tarefa atualizada
        verify(taskEventPublisher).publishTaskUpdated(newTask); // Publicou o evento de atualização
    }

    @Test
    void updateShouldKeepIdAndCreatedAtOfExistingTask() { // ao atualizar, o id e a data de criação continuam os mesmos
        // Preparação
        Task oldTask = new Task(10L, "Old title", "Old description", TaskStatus.PENDING, CREATED_AT);
        Task newTask = new Task(10L, "New title", "New description", TaskStatus.IN_PROGRESS, CREATED_AT); // Mesmo id e mesma data
        when(taskRepository.findById(10L)).thenReturn(Optional.of(oldTask));
        when(taskRepository.save(newTask)).thenReturn(newTask);

        // Ação
        service.update(10L, new UpdateTaskUseCase.UpdateTaskCommand("New title", "New description", TaskStatus.IN_PROGRESS));

        // Verificação: a tarefa salva no banco mudou o título, a descrição e o status, mas manteve o id 10 e a data
        verify(taskRepository).save(newTask);
    }

    @Test
    void updateShouldThrowWhenTaskDoesNotExist() { // atualizar uma tarefa que não existe
        // Preparação: o banco não encontra a tarefa 999
        when(taskRepository.findById(999L)).thenReturn(Optional.empty());
        UpdateTaskUseCase.UpdateTaskCommand command =
                new UpdateTaskUseCase.UpdateTaskCommand("Title", "Description", TaskStatus.DONE);

        // Ação e verificação: tem que dar o erro "tarefa não encontrada"
        assertThatThrownBy(() -> service.update(999L, command)).isInstanceOf(TaskNotFoundException.class);

        verify(taskRepository, never()).save(any()); // Nada foi salvo no banco
        verifyNoInteractions(taskEventPublisher); // Nenhum evento foi publicado no Kafka
    }

    @Test
    void deleteShouldRemoveTaskWhenItExists() { // excluir uma tarefa que existe
        // Preparação: o banco encontra a tarefa 3
        Task task = new Task(3L, "Title", "Description", TaskStatus.PENDING, CREATED_AT);
        when(taskRepository.findById(3L)).thenReturn(Optional.of(task));

        // Ação
        service.delete(3L);

        // Verificação
        verify(taskRepository).deleteById(3L); // Mandou o banco excluir a tarefa 3
    }

    @Test
    void deleteShouldThrowWhenTaskDoesNotExist() { // excluir uma tarefa que não existe
        // Preparação: o banco não encontra a tarefa 999
        when(taskRepository.findById(999L)).thenReturn(Optional.empty());

        // Ação e verificação: tem que dar o erro "tarefa não encontrada"
        assertThatThrownBy(() -> service.delete(999L)).isInstanceOf(TaskNotFoundException.class);

        verify(taskRepository, never()).deleteById(any()); // Nada foi excluído
    }

    @Test
    void listShouldReturnAllTasks() { // listar as tarefas
        // Preparação: o banco devolve duas tarefas
        Task task1 = new Task(1L, "T1", "D1", TaskStatus.PENDING, CREATED_AT);
        Task task2 = new Task(2L, "T2", "D2", TaskStatus.IN_PROGRESS, CREATED_AT);
        when(taskRepository.findAll()).thenReturn(List.of(task1, task2));

        // Ação
        List<Task> result = service.list();

        // Verificação: devolveu as duas tarefas, na mesma ordem
        assertThat(result).containsExactly(task1, task2);
    }
}