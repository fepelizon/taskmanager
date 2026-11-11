// Testes da regra "a tarefa só pode ir para o status vizinho".
import { describe, expect, it } from 'vitest';
import { nextTaskStatus, previousTaskStatus } from './taskStatus';

describe('taskStatus', () => {
  // Para Pendente e Em andamento, deve devolver o próximo status
  it('should return the next status in the flow', () => {
    expect(nextTaskStatus('PENDING')).toBe('IN_PROGRESS'); // Depois de Pendente vem Em andamento
    expect(nextTaskStatus('IN_PROGRESS')).toBe('DONE'); // Depois de Em andamento vem Concluída
  });

  // Para Concluída, que é o último status, não deve haver próximo
  it('should return null when there is no next status', () => {
    expect(nextTaskStatus('DONE')).toBeNull(); // Concluída é o último: não tem próximo (null = "nenhum")
  });

  // Para Concluída e Em andamento, deve devolver o status anterior
  it('should return the previous status in the flow', () => {
    expect(previousTaskStatus('DONE')).toBe('IN_PROGRESS'); // Antes de Concluída vem Em andamento
    expect(previousTaskStatus('IN_PROGRESS')).toBe('PENDING'); // Antes de Em andamento vem Pendente
  });

  // Para Pendente, que é o primeiro status, não deve haver anterior
  it('should return null when there is no previous status', () => {
    expect(previousTaskStatus('PENDING')).toBeNull(); // Pendente é o primeiro: não tem anterior
  });
});
