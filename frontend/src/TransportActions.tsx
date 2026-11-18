import { useState, type FormEvent } from "react";
import { useNavigate } from "react-router-dom";

import { keycloak, useAuth } from "./auth";
import { type Transport } from "./api";
import { Button, Card, Field } from "./components/ui";

const base = (import.meta.env.VITE_API_BASE_URL || "").replace(/\/$/, "");

const pendingKey = "transports.planification.en-attente.v1";

type Plan = {
  bonId: string;
  camionId: string;
  departPrevu: string;
  operationKey: string;
};

async function post<T>(path: string, data?: unknown): Promise<T> {
  if (!keycloak.authenticated) {
    throw new Error("Session expirée : reconnectez-vous.");
  }

  try {
    await keycloak.updateToken(30);
  } catch {
    throw new Error("Session expirée : reconnectez-vous.");
  }

  let response: Response;

  try {
    response = await fetch(base + path, {
      method: "POST",
      headers: {
        Accept: "application/json",
        Authorization: `Bearer ${keycloak.token}`,
        ...(data === undefined
          ? {}
          : { "Content-Type": "application/json" }),
      },
      ...(data === undefined
        ? {}
        : { body: JSON.stringify(data) }),
      signal: AbortSignal.timeout(20000),
    });
  } catch {
    throw new Error(
      "Résultat inconnu (connexion interrompue ou délai dépassé). Vérifiez l'état avant toute nouvelle tentative."
    );
  }

  if (!response.ok) {
    const body = await response.json().catch(() => ({}));

    throw new Error(
      typeof body.message === "string" && body.message
        ? body.message
        : `Erreur HTTP ${response.status}. Vérifiez l'état avant de réessayer.`
    );
  }

  return response.json() as Promise<T>;
}

function toIso(value: string): string {
  const date = new Date(value);

  if (!value || Number.isNaN(date.getTime())) {
    throw new Error("Saisissez une date et une heure valides.");
  }

  return date.toISOString();
}

function readPending(): Plan | null {
  try {
    const text = sessionStorage.getItem(pendingKey);

    if (!text) return null;

    const value = JSON.parse(text) as Plan;

    return value.bonId &&
      value.camionId &&
      value.departPrevu &&
      value.operationKey
      ? value
      : null;
  } catch {
    return null;
  }
}

export function Planification() {
  const { canEdit } = useAuth();
  const navigate = useNavigate();

  const [bonId, setBonId] = useState("");
  const [camionId, setCamionId] = useState("");
  const [depart, setDepart] = useState("");
  const [pending, setPending] = useState<Plan | null>(readPending);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");

  if (!canEdit) return null;

  async function submit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();

    if (busy) return;

    setError("");

    try {
      if (
        !window.confirm(
          "Cette opération peut créer un transport et réserver un camion. Continuer ?"
        )
      ) {
        return;
      }

      let plan = pending;

      if (!plan) {
        if (!bonId.trim() || !camionId.trim()) {
          throw new Error("Indiquez le bon et le camion.");
        }

        plan = {
          bonId: bonId.trim(),
          camionId: camionId.trim(),
          departPrevu: toIso(depart),
          operationKey: crypto.randomUUID(),
        };

        // Conserver la demande exacte avant l'appel.
        try {
          sessionStorage.setItem(pendingKey, JSON.stringify(plan));
        } catch {
          throw new Error(
            "Impossible de conserver la clé d'opération dans ce navigateur. Planification non envoyée."
          );
        }

        setPending(plan);
      }

      setBusy(true);

      const result = await post<Transport>(
        "/api/v1/transports",
        plan
      );

      try {
        sessionStorage.removeItem(pendingKey);
      } catch {
        // Ne pas renvoyer une création réussie.
      }

      setPending(null);
      navigate(`/transports/${encodeURIComponent(result.id)}`);
    } catch (e) {
      setError(
        (e instanceof Error ? e.message : "Erreur inattendue.") +
          " Si la demande a été envoyée, vérifiez la liste et réessayez uniquement avec la même demande."
      );
    } finally {
      setBusy(false);
    }
  }

  function abandon() {
    if (
      !window.confirm(
        "Attention : un transport ou une réservation peut déjà exister. N'abandonnez cette demande qu'après vérification avec l'équipe. Confirmer ?"
      )
    ) {
      return;
    }

    try {
      sessionStorage.removeItem(pendingKey);
    } catch {
      setError("Impossible d'effacer la demande sauvegardée.");
      return;
    }

    setPending(null);
    setError("");
  }

  return (
    <Card
      title="Planifier un transport"
      subtitle="Utilisez un bon émis et un camion disponible convenus avec l'équipe."
    >
      <form className="form-body" onSubmit={submit}>
        {pending ? (
          <div className="notice warning">
            <div>
              <strong>Demande à vérifier ou à reprendre</strong>

              <p>
                Bon : {pending.bonId} — Camion : {pending.camionId}
              </p>

              <p>Départ : {pending.departPrevu}</p>
              <p>Clé d'opération : {pending.operationKey}</p>
              <p>
                Une reprise conserve exactement la même demande
                et la même clé.
              </p>
            </div>
          </div>
        ) : (
          <>
            <Field label="ID du bon émis">
              <input
                required
                maxLength={64}
                value={bonId}
                onChange={(e) => setBonId(e.target.value)}
              />
            </Field>

            <Field label="ID du camion">
              <input
                required
                maxLength={64}
                value={camionId}
                onChange={(e) => setCamionId(e.target.value)}
              />
            </Field>

            <Field label="Départ prévu">
              <input
                required
                type="datetime-local"
                value={depart}
                onChange={(e) => setDepart(e.target.value)}
              />
            </Field>
          </>
        )}

        {error && (
          <p className="live-error" role="alert">
            {error}
          </p>
        )}

        <Button type="submit" disabled={busy}>
          {busy
            ? "Envoi…"
            : pending
              ? "Reprendre la même demande"
              : "Planifier"}
        </Button>{" "}

        {pending && (
          <Button
            secondary
            disabled={busy}
            onClick={abandon}
          >
            Abandonner après vérification
          </Button>
        )}
      </form>
    </Card>
  );
}

export function ActionsTransport({
  transport,
  onChanged,
}: {
  transport: Transport;
  onChanged: () => void;
}) {
  const { canEdit } = useAuth();

  const [date, setDate] = useState("");
  const [type, setType] = useState("POINTAGE");
  const [description, setDescription] = useState("");
  const [receptionnaire, setReceptionnaire] = useState("");
  const [dateReception, setDateReception] = useState("");
  const [commentaire, setCommentaire] = useState("");
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState("");
  const [uncertain, setUncertain] = useState(false);

  if (!canEdit) return null;

  const id = encodeURIComponent(transport.id);
  const status = transport.statut;

  async function execute(
    action: string,
    data: unknown,
    label: string
  ) {
    if (
      busy ||
      !window.confirm(
        `${label} ? Cette action modifie des données réelles.`
      )
    ) {
      return;
    }

    setMessage("");
    setBusy(true);

    try {
      await post(`/api/v1/transports/${id}/${action}`, data);

      setMessage("Action enregistrée. Les données ont été actualisées.");
      onChanged();
    } catch (e) {
      setMessage(
        (e instanceof Error ? e.message : "Erreur inattendue.") +
          " Vérifiez l'état et les événements avant toute nouvelle tentative. Pour une livraison déjà commencée, réutilisez exactement les mêmes informations."
      );

      setUncertain(true);
    } finally {
      setBusy(false);
    }
  }

  function dated(action: string, field: string, label: string) {
    try {
      void execute(
        action,
        { [field]: toIso(date) },
        label
      );
    } catch (e) {
      setMessage(e instanceof Error ? e.message : "Date invalide.");
    }
  }

  return (
    <Card
      title="Actions du transport"
      subtitle="Les actions modifient les données réelles."
    >
      <div className="form-body">
        {message && (
          <p className="notice" role="status">
            {message}
          </p>
        )}

        {uncertain && (
          <Button secondary onClick={onChanged}>
            Actualiser et vérifier avant de continuer
          </Button>
        )}

        {(status === "PLANIFIE" || status === "EN_COURS") && (
          <Field
            label={
              status === "PLANIFIE"
                ? "Date et heure du départ réel"
                : "Date et heure de l'arrivée réelle"
            }
          >
            <input
              type="datetime-local"
              value={date}
              onChange={(e) => setDate(e.target.value)}
            />
          </Field>
        )}

        {status === "PLANIFIE" && (
          <Button
            disabled={busy || uncertain}
            onClick={() =>
              dated("depart", "departReel", "Déclarer le départ")
            }
          >
            Déclarer le départ
          </Button>
        )}

        {status === "EN_COURS" && (
          <>
            <Button
              disabled={busy || uncertain}
              onClick={() =>
                dated("arrivee", "arriveeReelle", "Déclarer l'arrivée")
              }
            >
              Déclarer l'arrivée
            </Button>

            <h3>Ajouter un événement</h3>

            <Field label="Type">
              <select
                value={type}
                onChange={(e) => setType(e.target.value)}
              >
                <option value="POINTAGE">Pointage</option>
                <option value="COMMENTAIRE">Commentaire</option>
              </select>
            </Field>

            <Field label="Description">
              <textarea
                maxLength={1000}
                value={description}
                onChange={(e) => setDescription(e.target.value)}
              />
            </Field>

            <p>
              La date et l'heure ci-dessus sont également utilisées
              pour l'événement.
            </p>

            <Button
              secondary
              disabled={busy || uncertain}
              onClick={() => {
                try {
                  if (!description.trim()) {
                    throw new Error(
                      "La description de l’événement est obligatoire."
                    );
                  }

                  void execute(
                    "evenements",
                    {
                      type,
                      description: description.trim(),
                      dateEvenement: toIso(date),
                      latitude: null,
                      longitude: null,
                    },
                    "Ajouter cet événement"
                  );
                } catch (e) {
                  setMessage(
                    e instanceof Error ? e.message : "Date invalide."
                  );
                }
              }}
            >
              Ajouter l'événement
            </Button>
          </>
        )}

        {status === "CLOTURE_EN_ATTENTE" && (
          <>
            <h3>Confirmer la livraison</h3>

            <p>
              En cas de reprise, utilisez exactement les mêmes
              données de réception.
            </p>

            <Field label="Réceptionnaire">
              <input
                maxLength={160}
                value={receptionnaire}
                onChange={(e) => setReceptionnaire(e.target.value)}
              />
            </Field>

            <Field label="Date de réception">
              <input
                type="date"
                value={dateReception}
                onChange={(e) => setDateReception(e.target.value)}
              />
            </Field>

            <Field label="Commentaire">
              <textarea
                maxLength={1000}
                value={commentaire}
                onChange={(e) => setCommentaire(e.target.value)}
              />
            </Field>

            <Button
              disabled={busy || uncertain}
              onClick={() => {
                if (!receptionnaire.trim() || !dateReception) {
                  setMessage(
                    "Réceptionnaire et date de réception obligatoires."
                  );
                  return;
                }

                void execute(
                  "livraison",
                  {
                    receptionnaire: receptionnaire.trim(),
                    dateReception,
                    commentaire: commentaire || null,
                  },
                  "Confirmer la livraison"
                );
              }}
            >
              Confirmer la livraison
            </Button>
          </>
        )}

        {(status === "PLANIFIE" ||
          status === "ANNULATION_EN_ATTENTE") && (
          <div>
            <Button
              danger
              disabled={busy || uncertain}
              onClick={() =>
                void execute(
                  "annulation",
                  undefined,
                  status === "PLANIFIE"
                    ? "Annuler le transport"
                    : "Reprendre l'annulation"
                )
              }
            >
              {status === "PLANIFIE"
                ? "Annuler le transport"
                : "Reprendre l'annulation"}
            </Button>
          </div>
        )}

        {status === "PREPARATION" && (
          <p>
            Réservation en attente. Reprenez la planification avec
            la même clé et les mêmes données depuis la liste.
          </p>
        )}

        {(status === "LIVRE" || status === "ANNULE") && (
          <p>Ce transport est terminé.</p>
        )}

        {busy && <p>Opération en cours…</p>}
      </div>
    </Card>
  );
}