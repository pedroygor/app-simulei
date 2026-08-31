import type { Metadata } from "next";
import { Bricolage_Grotesque, Geist, Geist_Mono } from "next/font/google";
import { ThemeProvider } from "@/components/theme-provider";
import { AuthProvider } from "@/providers/auth-provider";
import { QueryProvider } from "@/providers/query-provider";
import { Toaster } from "@/components/ui/sonner";
import "./globals.css";

// Corpo: Geist. Vem com o Next e tem acentuação portuguesa bem desenhada.
const geistSans = Geist({
  variable: "--font-sans",
  subsets: ["latin"],
});

// Dados: tempo, nota, percentual, ano da questão.
const geistMono = Geist_Mono({
  variable: "--font-geist-mono",
  subsets: ["latin"],
});

// Display: uso restrito (título de tela, nota final, número grande). É variável,
// o que dá controle fino de peso sem baixar um arquivo por variação.
const bricolage = Bricolage_Grotesque({
  variable: "--font-bricolage",
  subsets: ["latin"],
  display: "swap",
});

export const metadata: Metadata = {
  title: "Simulei",
  description: "Simulados do ENEM para cursinhos, professores e alunos",
};

export default function RootLayout({ children }: LayoutProps<"/">) {
  return (
    // suppressHydrationWarning é exigido pelo next-themes: ele altera a classe
    // do <html> antes do React hidratar, e sem isto o React acusa divergência
    <html
      lang="pt-BR"
      suppressHydrationWarning
      className={`${geistSans.variable} ${geistMono.variable} ${bricolage.variable} h-full antialiased`}
    >
      <body className="min-h-full flex flex-col">
        <ThemeProvider
          attribute="class"
          // Escuro é o padrão: o caso de uso real é o aluno estudando de
          // madrugada no celular
          defaultTheme="dark"
          // ...mas quem configurou o sistema em claro recebe claro
          enableSystem
          disableTransitionOnChange
        >
          {/* QueryProvider fica por fora: o AuthProvider usa useQuery */}
          <QueryProvider>
            <AuthProvider>{children}</AuthProvider>
          </QueryProvider>
          {/* O rótulo padrão do sonner é em inglês; leitor de tela num app
              em português precisa anunciar em português */}
          <Toaster
            containerAriaLabel="Notificações"
            toastOptions={{ closeButton: true }}
          />
        </ThemeProvider>
      </body>
    </html>
  );
}
