import { request, type Resume, type CamionsPage, type BonsPage, type TransportsPage } from "./api";

export type ChargementStats = { total: number; brouillons: number; valides: number };

export const dashboardApi = {
  async camions(signal: AbortSignal) {
    const [all, available] = await Promise.all([
      request<CamionsPage>("/api/v1/camions?page=0&size=1", "GET", undefined, signal),
      request<CamionsPage>(
        "/api/v1/camions?page=0&size=1&disponible=true",
        "GET",
        undefined,
        signal,
      ),
    ]);
    return { total: all.totalElements, disponibles: available.totalElements };
  },
  async chargements(signal: AbortSignal) {
    const [stats, recent] = await Promise.all([
      request<ChargementStats>("/api/v1/chargements/statistiques", "GET", undefined, signal),
      request<Resume[]>("/api/v1/chargements?limit=4&offset=0", "GET", undefined, signal),
    ]);
    return { ...stats, recent };
  },
  async bons(signal: AbortSignal) {
    const [all, pending] = await Promise.all([
      request<BonsPage>("/api/v1/bons?page=0&size=4", "GET", undefined, signal),
      request<BonsPage>("/api/v1/bons?page=0&size=1&statut=EMIS", "GET", undefined, signal),
    ]);
    return { total: all.totalElements, enAttente: pending.totalElements, recent: all.content };
  },
  async transports(signal: AbortSignal) {
    const [active, delivered, cancelled, closing] = await Promise.all([
      request<TransportsPage>(
        "/api/v1/transports?page=0&size=5&actifs=true",
        "GET",
        undefined,
        signal,
      ),
      request<TransportsPage>(
        "/api/v1/transports?page=0&size=1&statut=LIVRE",
        "GET",
        undefined,
        signal,
      ),
      request<TransportsPage>(
        "/api/v1/transports?page=0&size=1&statut=ANNULE",
        "GET",
        undefined,
        signal,
      ),
      request<TransportsPage>(
        "/api/v1/transports?page=0&size=1&statut=CLOTURE_EN_ATTENTE",
        "GET",
        undefined,
        signal,
      ),
    ]);
    return {
      actifs: active.totalElements,
      livres: delivered.totalElements,
      annules: cancelled.totalElements,
      aConfirmer: closing.totalElements,
      recent: active.content,
    };
  },
};
