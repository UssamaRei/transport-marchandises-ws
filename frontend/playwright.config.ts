import { defineConfig } from "@playwright/test";
import path from "node:path";

export default defineConfig({
  testDir: "./tests",
  workers: 1,
  retries: 0,
  timeout: 180000,
  expect: { timeout: 15000 },
  reporter: [["list"], ["json", { outputFile: "../.tools/frontend-systeme/resultats.json" }]],
  outputDir: "../.tools/frontend-systeme/artifacts",
  use: {
    baseURL: process.env.E2E_BASE_URL || "http://localhost:5173",
    viewport: { width: 1440, height: 1000 },
    screenshot: "only-on-failure",
    launchOptions: process.env.E2E_CHROME_PATH
      ? { executablePath: path.resolve(process.env.E2E_CHROME_PATH) }
      : { channel: "chrome" },
  },
});
