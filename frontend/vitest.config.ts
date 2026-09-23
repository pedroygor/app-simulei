import { defineConfig } from "vitest/config";
import { resolve } from "node:path";

export default defineConfig({
  test: {
    // Ambiente node mesmo com teste de componente: o TextoDaQuestao é
    // verificado com renderToStaticMarkup, que devolve a STRING de HTML e não
    // precisa de DOM. Para a pergunta que importa ali — "o <SCRIPT> da fonte
    // virou tag?" — inspecionar a string é mais direto que montar jsdom.
    environment: "node",
    include: ["src/**/*.test.{ts,tsx}"],
  },
  resolve: {
    // Mesmo alias do tsconfig, pro import "@/..." funcionar nos testes
    alias: { "@": resolve(__dirname, "./src") },
  },
});
