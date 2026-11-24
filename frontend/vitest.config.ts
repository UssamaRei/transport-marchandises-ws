import { defineConfig } from "vitest/config";
import react from "@vitejs/plugin-react";

export default defineConfig({
  plugins: [react()],
  test: {
    environment: "jsdom",
    include: ["tests/unit/**/*.test.{ts,tsx}"],
    setupFiles: ["./tests/unit/setup.ts"],
    restoreMocks: true,
    coverage: {
      provider: "istanbul",
      include: ["src/**/*.{ts,tsx}"],
      exclude: ["src/main.tsx", "src/vite-env.d.ts", "src/maquettes/**"],
      reporter: ["text", "html", "json", "json-summary", "lcov"],
      reportsDirectory: "coverage",
    },
    reporters: ["default", "json", "junit"],
    outputFile: {
      json: "../.tools/frontend-unit/resultats.json",
      junit: "../.tools/frontend-unit/junit.xml",
    },
  },
});
