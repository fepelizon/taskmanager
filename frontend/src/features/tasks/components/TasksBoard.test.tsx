import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { TasksBoard } from './TasksBoard';
import type { Task } from '../model/task';

const PENDING_TASK: Task = { id: 1, title: 'Tarefa pendente', description: 'D1', status: 'PENDING', createdAt: '2026-01-01T10:00:00Z' };
const IN_PROGRESS_TASK: Task = { id: 2, title: 'Tarefa em andamento', description: 'D2', status: 'IN_PROGRESS', createdAt: '2026-01-01T10:00:00Z' };
const DONE_TASK: Task = { id: 3, title: 'Tarefa concluida', description: 'D3', status: 'DONE', createdAt: '2026-01-01T10:00:00Z' };

describe('TasksBoard', () => {
  // Ao mostrar o quadro, cada tarefa deve aparecer na raia do seu status
  it('should place each task in the lane of its status', () => {
    // Ação: mostra o quadro com as três tarefas
    render(<TasksBoard tasks={[PENDING_TASK, IN_PROGRESS_TASK, DONE_TASK]} onEdit={vi.fn()} onDelete={vi.fn()} onMove={vi.fn()} />);

    // Verificação: cada raia (a <section> que tem o título da raia) contém a tarefa certa
    expect(screen.getByText('Pendente').closest('section')).toHaveTextContent('Tarefa pendente');
    expect(screen.getByText('Em andamento').closest('section')).toHaveTextContent('Tarefa em andamento');
    expect(screen.getByText('Concluida').closest('section')).toHaveTextContent('Tarefa concluida');
  });

  // Ao clicar em "Mover para frente" ou "Mover para tras", a tarefa deve ir para a raia vizinha certa
  it('should move the task forward and back to the neighbour lane', async () => {
    // Preparação
    const user = userEvent.setup(); // Simula uma pessoa usando a tela
    const onMove = vi.fn(); // Função de mentira chamada quando uma tarefa é movida
    render(<TasksBoard tasks={[PENDING_TASK, IN_PROGRESS_TASK, DONE_TASK]} onEdit={vi.fn()} onDelete={vi.fn()} onMove={onMove} />);
    const forwardButtons = screen.getAllByText('Mover para frente'); // [0] pendente, [1] em andamento, [2] concluída
    const backButtons = screen.getAllByText('Mover para tras');

    // Ação: move cada tarefa para a raia vizinha
    await user.click(forwardButtons[0]); // Tarefa pendente para frente
    await user.click(forwardButtons[1]); // Tarefa em andamento para frente
    await user.click(backButtons[1]); // Tarefa em andamento para trás
    await user.click(backButtons[2]); // Tarefa concluída para trás

    // Verificação: cada clique pediu a mudança para o status certo
    expect(onMove).toHaveBeenCalledWith(PENDING_TASK, 'IN_PROGRESS'); // Pendente -> Em andamento
    expect(onMove).toHaveBeenCalledWith(IN_PROGRESS_TASK, 'DONE'); // Em andamento -> Concluída
    expect(onMove).toHaveBeenCalledWith(IN_PROGRESS_TASK, 'PENDING'); // Em andamento -> Pendente
    expect(onMove).toHaveBeenCalledWith(DONE_TASK, 'IN_PROGRESS'); // Concluída -> Em andamento
    expect(onMove).toHaveBeenCalledTimes(4); // Foram exatamente 4 movimentos
  });

  // Na primeira raia não dá para voltar e na última não dá para avançar: esses botões devem ficar desabilitados
  it('should disable move back in the first lane and move forward in the last lane', () => {
    // Ação
    render(<TasksBoard tasks={[PENDING_TASK, IN_PROGRESS_TASK, DONE_TASK]} onEdit={vi.fn()} onDelete={vi.fn()} onMove={vi.fn()} />);
    const forwardButtons = screen.getAllByText('Mover para frente');
    const backButtons = screen.getAllByText('Mover para tras');

    // Verificação
    expect(backButtons[0]).toBeDisabled(); // Pendente: não dá para voltar
    expect(forwardButtons[0]).toBeEnabled(); // Pendente: dá para avançar
    expect(backButtons[1]).toBeEnabled(); // Em andamento: dá para voltar
    expect(forwardButtons[1]).toBeEnabled(); // Em andamento: dá para avançar
    expect(backButtons[2]).toBeEnabled(); // Concluída: dá para voltar
    expect(forwardButtons[2]).toBeDisabled(); // Concluída: não dá para avançar
  });

  // Clicar num botão desabilitado não deve mover a tarefa
  it('should not call move when a disabled button is clicked', async () => {
    // Preparação
    const user = userEvent.setup();
    const onMove = vi.fn();
    render(<TasksBoard tasks={[PENDING_TASK, IN_PROGRESS_TASK, DONE_TASK]} onEdit={vi.fn()} onDelete={vi.fn()} onMove={onMove} />);

    // Ação: tenta clicar nos dois botões desabilitados
    await user.click(screen.getAllByText('Mover para tras')[0]); // "Mover para tras" da tarefa pendente
    await user.click(screen.getAllByText('Mover para frente')[2]); // "Mover para frente" da tarefa concluída

    // Verificação: nada foi movido
    expect(onMove).not.toHaveBeenCalled();
  });

  // Quando não há nenhuma tarefa, o quadro deve mostrar uma mensagem avisando que a lista está vazia
  it('should show an empty message when there are no tasks', () => {
    // Ação: mostra o quadro sem nenhuma tarefa
    render(<TasksBoard tasks={[]} onEdit={vi.fn()} onDelete={vi.fn()} onMove={vi.fn()} />);

    // Verificação
    expect(screen.getByText('Nenhuma task cadastrada ate o momento.')).toBeInTheDocument();
  });
});
