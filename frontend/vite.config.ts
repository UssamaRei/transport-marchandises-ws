import { defineConfig, loadEnv } from "vite";
import react from "@vitejs/plugin-react";
import tailwindcss from "@tailwindcss/vite";

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), "");
  return {
    plugins: [react(), tailwindcss()],
    server: {
      host: "localhost",
      port: 5173,
      strictPort: true,
      proxy: {
        "/api/v1/chargements": {
          target: env.CHARGEMENTS_PROXY_TARGET || "http://localhost:8082",
          changeOrigin: true,
        },
        "/api/v1/camions": {
          target: env.CAMIONS_PROXY_TARGET || "http://localhost:8081",
          changeOrigin: true,
        },
        "/api/v1/transports": {
        target: env.TRANSPORTS_PROXY_TARGET || "http://localhost:8085",
        changeOrigin: true,
},
      },
    },
  };
});
