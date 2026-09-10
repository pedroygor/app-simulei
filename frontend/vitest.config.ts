import { defineConfig } from "vitest/config";
import { resolve } from "node:path";

export default defineConfig({
  test: {
    // Ambiente node: os testes cobrem a camada de API (lógica pura sobre
    // fetch), não componentes. Testar componente exigiria jsdom + testing
    // library, o que entra quando houver tela com lógica que valha testar.
    environment: "node",
    include: ["src/**/*.test.ts"],
  },
  resolve: {
    // Mesmo alias do tsconfig, pro import "@/..." funcionar nos testes
    alias: { "@": resolve(__dirname, "./src") },
  },
});
