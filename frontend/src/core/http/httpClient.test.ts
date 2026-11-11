import { beforeEach, describe, expect, it, vi } from 'vitest';
import { httpClient, setHttpErrorHandler } from './httpClient';

describe('httpClient', () => {
  const showError = vi.fn(); // Função de mentira no lugar do aviso de erro que aparece na tela

  beforeEach(() => {
    showError.mockClear();
    setHttpErrorHandler(showError); // Diz ao httpClient para avisar os erros nesta função
  });

  // Quando o backend responde sem erro, deve devolver os dados e não mostrar aviso de erro
  it('should return the response body when the request succeeds', async () => {
    // Preparação: o backend responde 200 com uma lista
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(new Response('[{"id":1}]', { status: 200 }));

    // Ação
    const result = await httpClient.get('/api/tasks');

    // Verificação
    expect(result).toEqual([{ id: 1 }]); // Devolveu a lista
    expect(showError).not.toHaveBeenCalled(); // Não mostrou erro
  });

  // Quando o backend responde que não encontrou (404) sem mandar mensagem, deve mostrar uma mensagem em português
  it('should convert a not found response into an app error and notify the handler', async () => {
    // Preparação: o backend responde 404 (não encontrado), sem mensagem
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(new Response('{"code":"TASK_NOT_FOUND"}', { status: 404 }));

    // Ação e verificação: a chamada dá erro com uma mensagem em português
    await expect(httpClient.get('/api/tasks/9')).rejects.toMatchObject({
      statusCode: 404,
      code: 'TASK_NOT_FOUND',
      message: 'Recurso nao encontrado.',
    });
    expect(showError).toHaveBeenCalled(); // O erro foi avisado na tela
  });

  // Quando o backend dá erro interno (500) sem mandar nada, deve mostrar uma mensagem amigável
  it('should convert a server error without body into a friendly message', async () => {
    // Preparação: o backend responde 500 (erro no servidor), sem nada no corpo
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(new Response(null, { status: 500 }));

    // Ação e verificação
    await expect(httpClient.get('/api/tasks')).rejects.toMatchObject({
      statusCode: 500,
      message: 'Falha interna no servidor.',
    });
    expect(showError).toHaveBeenCalled();
  });

  // Quando o backend recusa um campo (400), a mensagem de erro desse campo deve chegar até o formulário
  it('should keep the field errors of a validation response', async () => {
    // Preparação: o backend responde 400 com um erro no campo "title"
    const body = '{"detail":"Existem campos invalidos no formulario.","code":"VALIDATION_ERROR","fieldErrors":{"title":"não deve estar em branco"}}';
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(new Response(body, { status: 400 }));

    // Ação e verificação: o erro guarda a mensagem do campo "title"
    await expect(httpClient.post('/api/tasks', { title: '' })).rejects.toMatchObject({
      statusCode: 400,
      message: 'Existem campos invalidos no formulario.',
      fieldErrors: { title: 'não deve estar em branco' },
    });
  });

  // Quando o backend está desligado, deve mostrar a mensagem de falha de conexão
  it('should report a connection failure when the backend is unreachable', async () => {
    // Preparação: o backend está desligado (a chamada falha sem resposta)
    vi.spyOn(globalThis, 'fetch').mockRejectedValue(new TypeError('Failed to fetch'));

    // Ação e verificação: aparece a mensagem de falha de conexão
    await expect(httpClient.get('/api/tasks')).rejects.toMatchObject({
      code: 'NETWORK_ERROR',
      message: 'Falha de conexao. Verifique se o backend esta disponivel.',
    });
    expect(showError).toHaveBeenCalled();
  });
});