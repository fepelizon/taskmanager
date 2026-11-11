import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { TasksPage } from './TasksPage';
import { taskService } from '../services/taskService';
import type { Task } from '../model/task';

vi.mock('../services/taskService', () => ({
  taskService: {
    list: vi.fn(),
    create: vi.fn(),
    update: vi.fn(),
    remove: vi.fn(),
  },
}));

const TASK: Task = { id: 1, title: 'Revisar contrato', description: 'Conferir prazos e valores', status: 'PENDING', createdAt: '2026-01-01T10:00:00Z' };

describe('TasksPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(taskService.list).mockResolvedValue([TASK]);
    vi.mocked(taskService.remove).mockResolvedValue(undefined);
  });

  // Ao clicar em Excluir e confirmar, a tarefa deve ser excluída e a lista deve ser recarregada
  it('should delete the task and reload the list when deletion is confirmed', async () => {
    // Preparação
    const user = userEvent.setup(); // Simula uma pessoa usando a tela
    vi.spyOn(window, 'confirm').mockReturnValue(true); // Quando aparecer a pergunta "Deseja excluir...?", responde OK
    render(<TasksPage />);
    await screen.findByText('Revisar contrato'); // Espera a tarefa aparecer na tela
    vi.mocked(taskService.list).mockResolvedValue([]); // Depois da exclusão, a lista vem vazia

    // Ação: clica em Excluir
    await user.click(screen.getByText('Excluir'));

    // Verificação
    expect(window.confirm).toHaveBeenCalledWith('Deseja excluir a task "Revisar contrato"?'); // Perguntou antes de excluir
    expect(taskService.remove).toHaveBeenCalledWith(1); // Pediu ao backend para excluir a tarefa 1
    expect(await screen.findByText('Nenhuma task cadastrada ate o momento.')).toBeInTheDocument(); // A lista foi recarregada e agora está vazia
  });

  // Ao clicar em Excluir e cancelar, nada deve ser excluído e a tarefa deve continuar na tela
  it('should keep the task when deletion is cancelled', async () => {
    // Preparação
    const user = userEvent.setup();
    vi.spyOn(window, 'confirm').mockReturnValue(false); // Quando aparecer a pergunta, responde Cancelar
    render(<TasksPage />);
    await screen.findByText('Revisar contrato'); // Espera a tarefa aparecer na tela

    // Ação: clica em Excluir
    await user.click(screen.getByText('Excluir'));

    // Verificação
    expect(taskService.remove).not.toHaveBeenCalled(); // Não pediu para excluir
    expect(screen.getByText('Revisar contrato')).toBeInTheDocument(); // A tarefa continua na tela
  });
});
