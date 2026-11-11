package com.taskmanager.backend.infrastructure.event;

import com.taskmanager.backend.domain.Task;
import com.taskmanager.backend.domain.TaskStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Testes da classe que envia os eventos das tarefas para o Kafka (KafkaTaskEventPublisher).
// O Kafka é simulado (mock): o teste confere a mensagem que SERIA enviada.
@ExtendWith(MockitoExtension.class)
class KafkaTaskEventPublisherTest {

    private static final Instant CREATED_AT = Instant.parse("2026-01-01T10:00:00Z");

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    private KafkaTaskEventPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new KafkaTaskEventPublisher(kafkaTemplate, "task-events"); // "task-events" é o nome do tópico do Kafka
    }

    @Test
    void publishTaskCreatedShouldSendCreatedEventWithTaskIdAsKey() { // a criação de uma tarefa deve disparar um evento de criação, tendo o ID da tarefa como chave
        // Ação: publica o evento de criação da tarefa 1
        publisher.publishTaskCreated(new Task(1L, "Title", "Description", TaskStatus.PENDING, CREATED_AT));

        // Verificação: foi enviada uma mensagem para o tópico "task-events", com a chave "1" (o id da tarefa). O captor "guarda" a mensagem enviada para podermos olhar o conteúdo dela
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(eq("task-events"), eq("1"), captor.capture());
        String message = captor.getValue();

        // A mensagem é um JSON com o tipo do evento e os dados da tarefa
        assertThat(message).contains("\"eventType\":\"TASK_CREATED\"");
        assertThat(message).contains("\"taskId\":1");
        assertThat(message).contains("\"title\":\"Title\"");
        assertThat(message).contains("\"description\":\"Description\"");
        assertThat(message).contains("\"status\":\"PENDING\"");
    }

    @Test
    void publishTaskUpdatedShouldSendUpdatedEvent() { // Publicar tarefa atualizada, deve enviar evento de atualização
        // Ação: publica o evento de atualização da tarefa 2
        publisher.publishTaskUpdated(new Task(2L, "Title", "Description", TaskStatus.DONE, CREATED_AT));

        // Verificação: a mensagem foi enviada com a chave "2" e tem o tipo de evento de atualização
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(eq("task-events"), eq("2"), captor.capture());
        String message = captor.getValue();
        assertThat(message).contains("\"eventType\":\"TASK_UPDATED\"");
        assertThat(message).contains("\"status\":\"DONE\"");
    }

    @Test
    void publishShouldEscapeQuotesAndBackslashes() { // A função publish deve escapar aspas e barras invertidas
        // Ação: título com aspas -> Revisar contrato "ABC"  /  descrição com barra -> C:\temp (no código Java, as aspas e a barra dentro do texto são escritas como \" e \\)
        publisher.publishTaskCreated(new Task(1L, "Revisar contrato \"ABC\"", "C:\\temp", TaskStatus.PENDING, CREATED_AT));

        // Verificação
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(eq("task-events"), eq("1"), captor.capture());
        String message = captor.getValue();
        assertThat(message).contains("\"title\":\"Revisar contrato \\\"ABC\\\"\""); // No JSON fica: "title":"Revisar contrato \"ABC\""
        assertThat(message).contains("\"description\":\"C:\\\\temp\""); // No JSON fica: "description":"C:\\temp"
    }

    @Test
    void publishShouldSendEmptyTextWhenDescriptionIsNull() { // A função publish deve enviar um texto vazio quando a descrição for nula
        // Ação: publica uma tarefa sem descrição (null)
        publisher.publishTaskCreated(new Task(1L, "Title", null, TaskStatus.PENDING, CREATED_AT));

        // Verificação: a descrição vai como texto vazio
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(eq("task-events"), eq("1"), captor.capture());
        assertThat(captor.getValue()).contains("\"description\":\"\"");
    }

    @Test
    void publishShouldNotThrowWhenKafkaSendFails() { // A função publish não deve lançar exceção caso o envio para o Kafka falhe
        // Preparação: qualquer envio para o Kafka dá erro
        when(kafkaTemplate.send(anyString(), anyString(), anyString())).thenThrow(new IllegalStateException("broker unavailable"));

        // Ação: se esta linha der erro, o teste falha sozinho
        publisher.publishTaskCreated(new Task(1L, "Title", "Description", TaskStatus.PENDING, CREATED_AT));

        // Verificação: o envio foi tentado uma vez, e o erro não passou para frente
        verify(kafkaTemplate).send(eq("task-events"), eq("1"), anyString());
    }
}
