package com.taskmanager.backend.application.port.out;

import com.taskmanager.backend.domain.Task;

public interface TaskEventPublisherPort {
    void publishTaskCreated(Task task);

    void publishTaskUpdated(Task task);
}
