import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { TaskFormModal } from './TaskFormModal';
import type { Task } from '../model/task';

const TASK: Task = {
  id: 7,
  title: 'Revisar contrato',
  description: 'Conferir prazos e valores',
  status: 'IN_PROGRESS',
  createdAt: '2026-01-01T10:00:00Z',
};

describe('TaskFormModal', () => {
  // Ao abrir para criar uma tarefa, o formulário deve vir com os campos vazios e o status travado em Pendente
  it('should open in create mode with empty fields and status locked as pending', () => {
    // Ação: abre o formulário no modo de criação
    render(
      <TaskFormModal
        isOpen={true}
        mode="create"
        initialTask={null}
        isSubmitting={false}
        fieldErrors={{}}
        onClose={vi.fn()} // vi.fn() é uma função de mentira, que só registra se foi chamada
        onCreate={vi.fn()}
        onUpdate={vi.fn()}
      />
    );

    // Verificação
    expect(screen.getByText('Nova Task')).toBeInTheDocument(); // O título do formulário é "Nova Task"
    expect(screen.getByLabelText('Titulo')).toHaveValue(''); // O campo Titulo está vazio
    expect(screen.getByLabelText('Descricao')).toHaveValue(''); // O campo Descricao está vazio
    expect(screen.getByLabelText('Status')).toHaveValue('PENDING'); // O status é Pendente
    expect(screen.getByLabelText('Status')).toBeDisabled(); // E não dá para mudar o status
  });

  // Ao criar uma tarefa, o formulário deve enviar só o título e a descrição, sem o status
  it('should submit title and description without status in create mode', async () => {
    // Preparação
    const user = userEvent.setup(); // Simula uma pessoa usando a tela
    const onCreate = vi.fn();
    const onUpdate = vi.fn();
    render(
      <TaskFormModal
        isOpen={true}
        mode="create"
        initialTask={null}
        isSubmitting={false}
        fieldErrors={{}}
        onClose={vi.fn()}
        onCreate={onCreate}
        onUpdate={onUpdate}
      />
    );

    // Ação: preenche o formulário e clica em "Criar Task"
    await user.type(screen.getByLabelText('Titulo'), 'Revisar contrato');
    await user.type(screen.getByLabelText('Descricao'), 'Conferir prazos e valores');
    await user.click(screen.getByText('Criar Task'));

    // Verificação: o formulário enviou o título e a descrição (sem o status)
    expect(onCreate).toHaveBeenCalledWith({ title: 'Revisar contrato', description: 'Conferir prazos e valores' });
    expect(onUpdate).not.toHaveBeenCalled(); // Não chamou a atualização
  });

  // Ao abrir para editar uma tarefa, o formulário deve vir preenchido com os dados dela e com o status liberado
  it('should open in edit mode with the fields filled with the task data', () => {
    // Ação: abre o formulário no modo de edição, com a tarefa de exemplo
    render(
      <TaskFormModal
        isOpen={true}
        mode="edit"
        initialTask={TASK}
        isSubmitting={false}
        fieldErrors={{}}
        onClose={vi.fn()}
        onCreate={vi.fn()}
        onUpdate={vi.fn()}
      />
    );

    // Verificação: os campos vêm preenchidos com os dados da tarefa
    expect(screen.getByText('Editar Task')).toBeInTheDocument();
    expect(screen.getByLabelText('Titulo')).toHaveValue('Revisar contrato');
    expect(screen.getByLabelText('Descricao')).toHaveValue('Conferir prazos e valores');
    expect(screen.getByLabelText('Status')).toHaveValue('IN_PROGRESS');
    expect(screen.getByLabelText('Status')).toBeEnabled(); // Na edição dá para mudar o status
  });

  // Ao salvar uma edição, o formulário deve enviar o id da tarefa junto com os dados alterados
  it('should submit the changed task with its id in edit mode', async () => {
    // Preparação
    const user = userEvent.setup();
    const onCreate = vi.fn();
    const onUpdate = vi.fn();
    render(
      <TaskFormModal
        isOpen={true}
        mode="edit"
        initialTask={TASK}
        isSubmitting={false}
        fieldErrors={{}}
        onClose={vi.fn()}
        onCreate={onCreate}
        onUpdate={onUpdate}
      />
    );

    // Ação: troca o título, muda o status para Concluída e salva
    await user.clear(screen.getByLabelText('Titulo')); // Apaga o título
    await user.type(screen.getByLabelText('Titulo'), 'Revisar contrato do fornecedor');
    await user.selectOptions(screen.getByLabelText('Status'), 'DONE');
    await user.click(screen.getByText('Salvar Alteracoes'));

    // Verificação: enviou o id 7 da tarefa e os dados novos
    expect(onUpdate).toHaveBeenCalledWith(7, {
      title: 'Revisar contrato do fornecedor',
      description: 'Conferir prazos e valores',
      status: 'DONE',
    });
    expect(onCreate).not.toHaveBeenCalled(); // Não criou uma tarefa nova
  });

  // Quando o backend recusa um campo, o formulário deve mostrar a mensagem de erro desse campo
  it('should show the validation error below the field', () => {
    // Ação: abre o formulário com um erro no campo "title", do jeito que o backend devolve
    render(
      <TaskFormModal
        isOpen={true}
        mode="create"
        initialTask={null}
        isSubmitting={false}
        fieldErrors={{ title: 'não deve estar em branco' }}
        onClose={vi.fn()}
        onCreate={vi.fn()}
        onUpdate={vi.fn()}
      />
    );

    // Verificação: a mensagem de erro aparece na tela
    expect(screen.getByText('não deve estar em branco')).toBeInTheDocument();
  });
});
