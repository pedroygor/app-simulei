import * as React from "react"

import { cn } from "@/lib/utils"

// Desvio deliberado do shadcn original: a borda usa --contorno, não --input.
// O --input vale o mesmo que o hairline decorativo (--border) e dava 1.43 no
// escuro e 1.34 no claro. A borda é o que identifica o campo, então precisa de
// 3:1 (WCAG 1.4.11) — a mesma correção feita em 27/08 no docs/identidade-visual.md
function Input({ className, type, ...props }: React.ComponentProps<"input">) {
  return (
    <input
      type={type}
      data-slot="input"
      className={cn(
        "h-8 w-full min-w-0 rounded-lg border border-contorno bg-transparent px-2.5 py-1 text-base transition-colors outline-none file:inline-flex file:h-6 file:border-0 file:bg-transparent file:text-sm file:font-medium file:text-foreground placeholder:text-muted-foreground focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50 disabled:pointer-events-none disabled:cursor-not-allowed disabled:bg-input/50 disabled:opacity-50 aria-invalid:border-destructive aria-invalid:ring-3 aria-invalid:ring-destructive/20 md:text-sm dark:bg-input/30 dark:disabled:bg-input/80 dark:aria-invalid:border-destructive/50 dark:aria-invalid:ring-destructive/40",
        className
      )}
      {...props}
    />
  )
}

export { Input }
