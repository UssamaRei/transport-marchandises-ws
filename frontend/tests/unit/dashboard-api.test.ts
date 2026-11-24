import { beforeEach, expect, it, vi } from "vitest";
vi.mock("../../src/api", () => ({ request: vi.fn() }));
import { request } from "../../src/api";
import { dashboardApi } from "../../src/dashboard-api";
const call = vi.mocked(request);
const signal = new AbortController().signal;
beforeEach(() => {
  call.mockReset();
});
it("distingue le total des camions de ceux disponibles", async () => {
  call.mockResolvedValueOnce({ totalElements: 12 }).mockResolvedValueOnce({ totalElements: 7 });
  await expect(dashboardApi.camions(signal)).resolves.toEqual({ total: 12, disponibles: 7 });
  expect(call.mock.calls.every((args) => args[3] === signal)).toBe(true);
});
it("utilise les statistiques globales des chargements, pas la taille de la liste récente", async () => {
  const recent = [{ id: "load-1" }];
  call
    .mockResolvedValueOnce({ total: 125, brouillons: 80, valides: 45 })
    .mockResolvedValueOnce(recent);
  await expect(dashboardApi.chargements(signal)).resolves.toEqual({
    total: 125,
    brouillons: 80,
    valides: 45,
    recent,
  });
});
it("sépare les bons en attente et les documents récents", async () => {
  call
    .mockResolvedValueOnce({ totalElements: 21, content: [{ id: "bon-1" }] })
    .mockResolvedValueOnce({ totalElements: 9 });
  await expect(dashboardApi.bons(signal)).resolves.toEqual({
    total: 21,
    enAttente: 9,
    recent: [{ id: "bon-1" }],
  });
});
it("distingue transports actifs, livrés, annulés et à confirmer", async () => {
  call
    .mockResolvedValueOnce({ totalElements: 4, content: [{ id: "trip-1" }] })
    .mockResolvedValueOnce({ totalElements: 20 })
    .mockResolvedValueOnce({ totalElements: 2 })
    .mockResolvedValueOnce({ totalElements: 1 });
  await expect(dashboardApi.transports(signal)).resolves.toEqual({
    actifs: 4,
    livres: 20,
    annules: 2,
    aConfirmer: 1,
    recent: [{ id: "trip-1" }],
  });
});
it("ne remplace pas un service indisponible par des compteurs à zéro", async () => {
  const error = new Error("indisponible");
  call.mockRejectedValue(error);
  await expect(dashboardApi.camions(signal)).rejects.toBe(error);
});
