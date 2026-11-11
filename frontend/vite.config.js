import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  // Configuração do Vitest. Fica no mesmo arquivo do Vite, então os testes entendem o código do mesmo jeito que o build
  test: {
    environment: 'jsdom', // Navegador simulado: sem isso não existiria "document" e os componentes não poderiam ser desenhados
    setupFiles: ['./src/test/setup.ts'], // Arquivo que roda antes de cada arquivo de teste
    coverage: {
      provider: 'v8', // Mede a cobertura com o pacote @vitest/coverage-v8
      reporter: ['text', 'html', 'lcov'], // Tabela no terminal, página HTML e lcov (formato que o SonarQube lê)
      // Mede TODO o src/, inclusive arquivos que nenhum teste abre (senão a porcentagem pareceria maior do que é)
      include: ['src/**/*.{ts,tsx}'],
      // Tira da conta o que não é lógica a testar: o main.tsx (só liga o React na página), os tipos do Vite,
      // a preparação dos testes e os próprios testes. Segue o plano: "todo o src/, exceto main.tsx"
      exclude: ['src/main.tsx', 'src/vite-env.d.ts', 'src/test/**', 'src/**/*.test.{ts,tsx}'],
    },
  },
});
