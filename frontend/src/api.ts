import { keycloak } from "./auth";

export type Statut = "BROUILLON" | "VALIDE";

export type Infos = {
  origine: string | null;
  destination: string | null;
  destinataireNom: string | null;
  destinataireTelephone: string | null;
};

export type Ligne = {
  id: string;
  designation: string;
  quantite: number;
  poidsUnitaireKg: number;
};

export type LigneInput = Omit<Ligne, "id">;

export type Resume = {
  id: string;
  origine: string | null;
  destination: string | null;
  poidsTotalKg: number;
  statut: Statut;
};

export type Chargement = Resume &
  Infos & {
    camionSouhaiteId: string | null;
    createdAt: string;
    version: number;
    lignes: Ligne[];
  };

export type Camion = {
  id: string;
  immatriculation: string;
  capaciteKg: number;
  etat: string;
  archive: boolean;
  disponible: boolean;
  createdAt: string;
};

export type CamionsPage = {
  items: Camion[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
};

export type Transport = {
  id: string;
  bonId: string;
  camionId: string;
  destination: string | null;
  poidsKg: number | null;
  departPrevu: string | null;
  statut: string;
  reservationId: string | null;
  departReel: string | null;
  arriveeReelle: string | null;
  preuveConfirmee: boolean;
  liberationConfirmee: boolean;
  syncEtat: string;
};

export type Bon = {
  id: string;
  numero: string;
  chargementId: string;
  camionSouhaiteId: string;
  origine: string;
  destination: string;
  destinataireNom: string;
  destinataireTelephone: string | null;
  poidsTotalKg: number;
  statut: "EMIS" | "LIVRE";
  transportId: string | null;
  recepteur: string | null;
  dateReception: string | null;
  commentaire: string | null;
  createdAt: string | null;
  lignes: Ligne[];
};

export type BonResume = Pick<
  Bon,
  | "id"
  | "numero"
  | "chargementId"
  | "camionSouhaiteId"
  | "destination"
  | "poidsTotalKg"
  | "statut"
  | "createdAt"
>;
export type BonsPage = {
  content: BonResume[];
  number: number;
  totalPages: number;
  totalElements: number;
};

export type TransportsPage = {
  content: Transport[];
  number: number;
  totalPages: number;
  totalElements: number;
};

export type EvenementTransport = {
  id: string;
  type: string;
  description: string | null;
  dateEvenement: string | null;
};

export type EvenementsPage = {
  content: EvenementTransport[];
  totalElements: number;
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

export async function request<T>(
  path: string,
  method = "GET",
  data?: unknown,
  signal?: AbortSignal,
): Promise<T> {
  if (!keycloak.authenticated) {
    throw new ApiError("Votre session a expiré. Reconnectez-vous.", 401);
  }

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
          : error.message ||
            (response.status === 409
              ? "Les données ont changé ou cette opération existe déjà. Actualisez avant de continuer."
              : `L’opération a échoué (${response.status}).`);

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

  flotte: (page: number, disponible: string, signal?: AbortSignal) =>
    request<CamionsPage>(
      `/api/v1/camions?page=${page}&size=20${disponible ? `&disponible=${disponible}` : ""}`,
      "GET",
      undefined,
      signal,
    ),
  camion: (id: string, signal?: AbortSignal) =>
    request<Camion>(`/api/v1/camions/${encodeURIComponent(id)}`, "GET", undefined, signal),
  creerCamion: (data: { immatriculation: string; capaciteKg: number }) =>
    request<Camion>("/api/v1/camions", "POST", data),
  modifierCamion: (id: string, data: { immatriculation: string; capaciteKg: number }) =>
    request<Camion>(`/api/v1/camions/${encodeURIComponent(id)}`, "PATCH", data),

  bons: (page: number, signal?: AbortSignal, statut = "") =>
    request<BonsPage>(
      `/api/v1/bons?page=${page}&size=20${statut ? `&statut=${statut}` : ""}`,
      "GET",
      undefined,
      signal,
    ),

  transports: (page: number, signal?: AbortSignal, camionId = "") =>
    request<TransportsPage>(
      `/api/v1/transports?page=${page}&size=20${camionId ? `&camionId=${encodeURIComponent(camionId)}` : ""}`,
      "GET",
      undefined,
      signal,
    ),

  transport: (id: string, signal?: AbortSignal) =>
    request<Transport>(`/api/v1/transports/${encodeURIComponent(id)}`, "GET", undefined, signal),

  evenementsTransport: (id: string, signal?: AbortSignal) =>
    request<EvenementsPage>(
      `/api/v1/transports/${encodeURIComponent(id)}/evenements?page=0&size=20`,
      "GET",
      undefined,
      signal,
    ),

  creerBon: (chargementId: string) => request<Bon>("/api/v1/bons", "POST", { chargementId }),

  bon: (id: string, signal?: AbortSignal) =>
    request<Bon>(`/api/v1/bons/${encodeURIComponent(id)}`, "GET", undefined, signal),
};

export const formatKg = (value: number) =>
  new Intl.NumberFormat("fr-FR", { maximumFractionDigits: 3 }).format(value) + " kg";

export const shortId = (id: string) => id.slice(0, 8).toUpperCase();

export const statusLabel = (status: Statut) => (status === "VALIDE" ? "Validé" : "Brouillon");

export const errorOf = (error: unknown) =>
  error instanceof ApiError ? error : new ApiError("Une erreur est survenue. Veuillez réessayer.");
