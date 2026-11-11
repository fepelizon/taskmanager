// Preparação que roda antes de cada arquivo de teste (configurada em vite.config.js, setupFiles)
import '@testing-library/jest-dom/vitest';
import { cleanup } from '@testing-library/react';
import { afterEach, vi } from 'vitest';

afterEach(() => { // Roda depois de cada teste
  cleanup(); // Limpa a tela simulada, para um teste não enxergar o que o anterior desenhou
  vi.restoreAllMocks(); // Desfaz as simulações feitas com vi.spyOn (como a do window.confirm), para cada teste começar do zero
});
