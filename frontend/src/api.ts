import { keycloak } from "./auth";

export type Statut = "BROUILLON" | "VALIDE";
export type Infos = {
  origine: string | null;
  destination: string | null;
  destinataireNom: string | null;
  destinataireTelephone: string | null;
};
export type Ligne = { id: string; designation: string; quantite: number; poidsUnitaireKg: number };
export type LigneInput = Omit<Ligne, "id">;
export type Resume = {
  id: string;
  origine: string | null;
  destination: string | null;
  poidsTotalKg: number;
  statut: Statut;
};
export type Chargement = Resume &
  Infos & { camionSouhaiteId: string | null; createdAt: string; version: number; lignes: Ligne[] };
export type Camion = {
  id: string;
  immatriculation: string;
  capaciteKg: number;
  etat: string;
  archive: boolean;
  disponible: boolean;
};
export type CamionsPage = {
  items: Camion[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
};
export class ApiError extends Error {
  constructor(
    message: string,
    public status = 0,
    public fields: Record<string, string> = {},
  ) {
    super(message);
  }
}
const base = (import.meta.env.VITE_API_BASE_URL || "").replace(/\/$/, "");
async function request<T>(
  path: string,
  method = "GET",
  data?: unknown,
  signal?: AbortSignal,
): Promise<T> {
  if (!keycloak.authenticated) throw new ApiError("Votre session a expiré. Reconnectez-vous.", 401);
  try {
    await keycloak.updateToken(30);
  } catch {
    keycloak.clearToken();
    throw new ApiError("Votre session a expiré. Reconnectez-vous.", 401);
  }
  const perform = () =>
    fetch(base + path, {
      method,
      headers: {
        Accept: "application/json",
        Authorization: `Bearer ${keycloak.token}`,
        ...(data === undefined ? {} : { "Content-Type": "application/json" }),
      },
      ...(data === undefined ? {} : { body: JSON.stringify(data) }),
      signal: signal
        ? AbortSignal.any([signal, AbortSignal.timeout(20000)])
        : AbortSignal.timeout(20000),
    });
  let response: Response;
  try {
    response = await perform();
    if (response.status === 401) {
      try {
        await keycloak.updateToken(-1);
      } catch {
        keycloak.clearToken();
        throw new ApiError("Votre session a expiré. Reconnectez-vous.", 401);
      }
      response = await perform();
    }
  } catch (error) {
    if (error instanceof ApiError || signal?.aborted) throw error;
    throw new ApiError(
      method === "GET"
        ? "Le service est injoignable ou met trop de temps à répondre."
        : "Impossible de confirmer l’enregistrement. Consultez les données à jour avant de réessayer.",
    );
  }
  if (!response.ok) {
    const error = await response.json().catch(() => ({}));
    const message =
      response.status === 403
        ? "Votre rôle ne permet pas cette action."
        : response.status === 401
          ? "La session n’est plus valide. Reconnectez-vous."
          : error.message || `L’opération a échoué (${response.status}).`;
    throw new ApiError(message, response.status, error.erreurs || {});
  }
  return response.status === 204 ? (undefined as T) : response.json();
}
const path = "/api/v1/chargements";
const idPath = (id: string) => `${path}/${encodeURIComponent(id)}`;
export const api = {
  list: (offset: number, signal?: AbortSignal) =>
    request<Resume[]>(`${path}?limit=20&offset=${offset}`, "GET", undefined, signal),
  detail: (id: string, signal?: AbortSignal) =>
    request<Chargement>(idPath(id), "GET", undefined, signal),
  create: (data: Infos) => request<Chargement>(path, "POST", data),
  update: (id: string, data: Infos) => request<Chargement>(idPath(id), "PUT", data),
  delete: (id: string) => request<void>(idPath(id), "DELETE"),
  addLigne: (id: string, data: LigneInput) =>
    request<Chargement>(`${idPath(id)}/lignes`, "POST", data),
  updateLigne: (id: string, ligneId: string, data: LigneInput) =>
    request<Chargement>(`${idPath(id)}/lignes/${encodeURIComponent(ligneId)}`, "PUT", data),
  deleteLigne: (id: string, ligneId: string) =>
    request<Chargement>(`${idPath(id)}/lignes/${encodeURIComponent(ligneId)}`, "DELETE"),
  validate: (id: string, camionId: string) =>
    request<Chargement>(`${idPath(id)}/validation`, "POST", { camionId }),
  camions: (page: number, signal?: AbortSignal) =>
    request<CamionsPage>(
      `/api/v1/camions?page=${page}&size=20&disponible=true`,
      "GET",
      undefined,
      signal,
    ),
};
export const formatKg = (value: number) =>
  new Intl.NumberFormat("fr-FR", { maximumFractionDigits: 3 }).format(value) + " kg";
export const shortId = (id: string) => id.slice(0, 8).toUpperCase();
export const statusLabel = (status: Statut) => (status === "VALIDE" ? "Validé" : "Brouillon");
export const errorOf = (error: unknown) =>
  error instanceof ApiError ? error : new ApiError("Une erreur est survenue. Veuillez réessayer.");
