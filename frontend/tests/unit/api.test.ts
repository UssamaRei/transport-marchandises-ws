import { beforeEach, describe, expect, it, vi } from "vitest";
const { kc } = vi.hoisted(() => ({
  kc: { authenticated: true, token: "test-token", updateToken: vi.fn(), clearToken: vi.fn() },
}));
vi.mock("../../src/auth", () => ({ keycloak: kc }));
import { api, ApiError, errorOf, formatKg, request, shortId, statusLabel } from "../../src/api";

const response = (status: number, body: unknown = {}) =>
  new Response(status === 204 ? null : JSON.stringify(body), { status });
let fetcher: ReturnType<typeof vi.fn>;
beforeEach(() => {
  kc.authenticated = true;
  kc.token = "test-token";
  kc.updateToken.mockReset().mockResolvedValue(false);
  kc.clearToken.mockReset();
  fetcher = vi.fn();
  vi.stubGlobal("fetch", fetcher);
});

describe("Client API : session et accès", () => {
  it("refuse la requête sans session avant tout appel réseau", async () => {
    kc.authenticated = false;
    await expect(request("/api/v1/camions")).rejects.toMatchObject({ status: 401 });
    expect(fetcher).not.toHaveBeenCalled();
  });
  it("renouvelle le jeton et envoie une lecture authentifiée", async () => {
    kc.updateToken.mockImplementation(async () => {
      kc.token = "fresh-token";
      return true;
    });
    fetcher.mockResolvedValue(response(200, { id: "camion-1" }));
    await expect(request("/api/v1/camions/camion-1")).resolves.toEqual({ id: "camion-1" });
    expect(kc.updateToken).toHaveBeenCalledWith(30);
    expect(fetcher.mock.calls[0][1].headers.Authorization).toBe("Bearer fresh-token");
    expect(fetcher.mock.calls[0][1].headers["Content-Type"]).toBeUndefined();
  });
  it("efface la session si le renouvellement initial échoue", async () => {
    kc.updateToken.mockRejectedValue(new Error("expired"));
    await expect(request("/api/v1/camions")).rejects.toMatchObject({ status: 401 });
    expect(kc.clearToken).toHaveBeenCalledOnce();
    expect(fetcher).not.toHaveBeenCalled();
  });
  it("réessaie une seule fois après un HTTP 401", async () => {
    fetcher.mockResolvedValueOnce(response(401)).mockResolvedValueOnce(response(200, { id: "ok" }));
    await expect(request("/api/v1/camions")).resolves.toEqual({ id: "ok" });
    expect(kc.updateToken.mock.calls).toEqual([[30], [-1]]);
    expect(fetcher).toHaveBeenCalledTimes(2);
  });
  it("ne boucle pas si le deuxième appel reçoit aussi 401", async () => {
    fetcher.mockResolvedValue(response(401));
    await expect(request("/api/v1/camions")).rejects.toMatchObject({ status: 401 });
    expect(fetcher).toHaveBeenCalledTimes(2);
  });
  it("annule la session si le renouvellement forcé échoue", async () => {
    fetcher.mockResolvedValue(response(401));
    kc.updateToken.mockResolvedValueOnce(false).mockRejectedValueOnce(new Error());
    await expect(request("/api/v1/camions")).rejects.toMatchObject({ status: 401 });
    expect(kc.clearToken).toHaveBeenCalledOnce();
    expect(fetcher).toHaveBeenCalledOnce();
  });
});

describe("Client API : erreurs et intégrité des échanges", () => {
  it("sérialise les données d’une écriture en JSON", async () => {
    fetcher.mockResolvedValue(response(201, { id: "new" }));
    await request("/api/v1/camions", "POST", { capaciteKg: 1200 });
    expect(fetcher.mock.calls[0][1]).toMatchObject({
      method: "POST",
      body: '{"capaciteKg":1200}',
      headers: { "Content-Type": "application/json" },
    });
  });
  it("accepte une suppression HTTP 204 sans essayer de lire du JSON", async () => {
    fetcher.mockResolvedValue(response(204));
    await expect(api.delete("x")).resolves.toBeUndefined();
  });
  it("explique un refus de rôle HTTP 403", async () => {
    fetcher.mockResolvedValue(response(403));
    await expect(request("/api/v1/camions")).rejects.toMatchObject({
      status: 403,
      message: "Votre rôle ne permet pas cette action.",
    });
  });
  it("conserve les erreurs de champs retournées par le service", async () => {
    fetcher.mockResolvedValue(
      response(400, { message: "Poids invalide", erreurs: { poids: "Doit être positif" } }),
    );
    await expect(request("/api/v1/chargements", "POST", {})).rejects.toMatchObject({
      status: 400,
      fields: { poids: "Doit être positif" },
    });
  });
  it("explique un conflit même sans message serveur", async () => {
    fetcher.mockResolvedValue(response(409));
    await expect(request("/api/v1/bons", "POST", {})).rejects.toThrow(
      "Actualisez avant de continuer",
    );
  });
  it("gère une page d’erreur non JSON", async () => {
    fetcher.mockResolvedValue(new Response("unavailable", { status: 503 }));
    await expect(request("/api/v1/bons")).rejects.toMatchObject({
      status: 503,
      message: "L’opération a échoué (503).",
    });
  });
  it("signale clairement une panne de lecture", async () => {
    fetcher.mockRejectedValue(new TypeError("offline"));
    await expect(request("/api/v1/bons")).rejects.toThrow("Le service est injoignable");
  });
  it("ne prétend pas qu’une écriture a échoué sans effet après une coupure", async () => {
    fetcher.mockRejectedValue(new TypeError("offline"));
    await expect(request("/api/v1/bons", "POST", {})).rejects.toThrow(
      "Consultez les données à jour avant de réessayer",
    );
  });
  it("préserve l’annulation d’une navigation", async () => {
    const controller = new AbortController();
    controller.abort();
    const error = new DOMException("Aborted", "AbortError");
    fetcher.mockRejectedValue(error);
    await expect(request("/api/v1/bons", "GET", undefined, controller.signal)).rejects.toBe(error);
  });
  it("encode les identifiants pour empêcher leur interprétation comme route", async () => {
    fetcher.mockResolvedValue(response(200));
    await api.detail("id/a?x=1");
    expect(fetcher.mock.calls[0][0]).toBe("/api/v1/chargements/id%2Fa%3Fx%3D1");
  });
  it("conserve les filtres d’un suivi ciblé sans casser l’URL", async () => {
    fetcher.mockResolvedValue(response(200));
    await api.transports(2, undefined, "truck/a", "EN_COURS", true);
    expect(fetcher.mock.calls[0][0]).toBe(
      "/api/v1/transports?page=2&size=20&camionId=truck%2Fa&statut=EN_COURS&actifs=true",
    );
  });
});

describe("Présentation des données", () => {
  it("affiche le poids avec la précision française", () => {
    expect(formatKg(27.125)).toBe("27,125 kg");
    expect(formatKg(0)).toBe("0 kg");
  });
  it("distingue brouillon et validé", () => {
    expect(statusLabel("BROUILLON")).toBe("Brouillon");
    expect(statusLabel("VALIDE")).toBe("Validé");
  });
  it("produit un identifiant court lisible", () => {
    expect(shortId("abcdef123456")).toBe("ABCDEF12");
  });
  it("préserve une erreur métier et masque une erreur interne", () => {
    const e = new ApiError("Conflit", 409);
    expect(errorOf(e)).toBe(e);
    expect(errorOf(new Error("secret serveur"))).toMatchObject({
      message: "Une erreur est survenue. Veuillez réessayer.",
      status: 0,
    });
  });
});
