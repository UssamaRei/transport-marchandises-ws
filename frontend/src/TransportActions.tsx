import { useEffect, useState, type FormEvent } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";

import { useAuth } from "./auth";
import {
  api,
  request,
  errorOf,
  formatKg,
  shortId,
  type Transport,
  type Bon,
  type BonResume,
  type Camion,
} from "./api";
import { Button, Card, Field } from "./components/ui";

const pendingKey = "transports.planification.en-attente.v1";

type Delivery = { receptionnaire: string; dateReception: string; commentaire: string | null };
const deliveryKey = (id: string) => `transports.livraison.${id}`;
function readDelivery(id: string): Delivery | null {
  try {
    const value = JSON.parse(sessionStorage.getItem(deliveryKey(id)) || "null") as Delivery | null;
    return value &&
      typeof value.receptionnaire === "string" &&
      typeof value.dateReception === "string" &&
      (value.commentaire === null || typeof value.commentaire === "string")
      ? value
      : null;
  } catch {
    return null;
  }
}

type Plan = {
  bonId: string;
  camionId: string;
  departPrevu: string;
  operationKey: string;
};

async function post<T>(path: string, data?: unknown): Promise<T> {
  return request<T>(path, "POST", data);
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

    return value.bonId && value.camionId && value.departPrevu && value.operationKey ? value : null;
  } catch {
    return null;
  }
}

export function Planification() {
  const { canEdit } = useAuth();
  const navigate = useNavigate();
  const [params] = useSearchParams();
  const [bonId, setBonId] = useState(params.get("bonId") || "");
  const [camionId, setCamionId] = useState("");
  const [depart, setDepart] = useState("");
  const [pending, setPending] = useState<Plan | null>(readPending);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");

  const [bons, setBons] = useState<BonResume[]>([]);
  const [trucks, setTrucks] = useState<Camion[]>([]);
  const [bonPage, setBonPage] = useState(0);
  const [truckPage, setTruckPage] = useState(0);
  const [moreBons, setMoreBons] = useState(false);
  const [moreTrucks, setMoreTrucks] = useState(false);
  const [selectedBon, setSelectedBon] = useState<Bon | null>(null);
  const [catalogVersion, setCatalogVersion] = useState(0);
  const [catalogLoading, setCatalogLoading] = useState(true);
  const [catalogError, setCatalogError] = useState("");
  useEffect(() => {
    if (!canEdit || pending) return;
    const controller = new AbortController();
    setCatalogLoading(true);
    setCatalogError("");
    Promise.all([
      api.bons(bonPage, controller.signal, "EMIS"),
      api.camions(truckPage, controller.signal),
    ])
      .then(([documents, vehicles]) => {
        if (controller.signal.aborted) return;
        setBons(documents.content);
        setTrucks(vehicles.items);
        setMoreBons(bonPage + 1 < documents.totalPages);
        setMoreTrucks(truckPage + 1 < vehicles.totalPages);
      })
      .catch((cause) => {
        if (!controller.signal.aborted) setCatalogError(errorOf(cause).message);
      })
      .finally(() => {
        if (!controller.signal.aborted) setCatalogLoading(false);
      });
    return () => controller.abort();
  }, [canEdit, pending, bonPage, truckPage, catalogVersion]);
  useEffect(() => {
    if (!bonId || pending) {
      setSelectedBon(null);
      return;
    }
    const controller = new AbortController();
    setSelectedBon(null);
    api
      .bon(bonId, controller.signal)
      .then((bon) => {
        if (!controller.signal.aborted) setSelectedBon(bon);
      })
      .catch((cause) => {
        if (!controller.signal.aborted) setCatalogError(errorOf(cause).message);
      });
    return () => controller.abort();
  }, [bonId, pending]);
  useEffect(() => {
    if (
      !camionId &&
      selectedBon &&
      trucks.some((truck) => truck.id === selectedBon.camionSouhaiteId)
    )
      setCamionId(selectedBon.camionSouhaiteId);
  }, [camionId, selectedBon, trucks]);
  const selectedTruck = trucks.find((truck) => truck.id === camionId);

  if (!canEdit) return null;

  async function submit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();

    if (busy) return;

    setError("");

    try {
      if (
        !window.confirm(
          "Cette opération peut créer un transport et réserver un camion. Continuer ?",
        )
      ) {
        return;
      }

      let plan = pending;

      if (!plan) {
        if (!selectedBon || !selectedTruck || selectedTruck.capaciteKg < selectedBon.poidsTotalKg) {
          throw new Error("Choisissez un bon et un camion disponible de capacité suffisante.");
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
            "Impossible de conserver la clé d'opération dans ce navigateur. Planification non envoyée.",
          );
        }

        setPending(plan);
      }

      setBusy(true);

      const result = await post<Transport>("/api/v1/transports", plan);

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
          " Si la demande a été envoyée, vérifiez la liste et réessayez uniquement avec la même demande.",
      );
    } finally {
      setBusy(false);
    }
  }

  function abandon() {
    if (
      !window.confirm(
        "Attention : un transport ou une réservation peut déjà exister. N'abandonnez cette demande qu'après vérification avec l'équipe. Confirmer ?",
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
      id="planifier-transport"
      title="Planifier un transport"
      subtitle="Choisissez le bon de livraison, un camion disponible et la date de départ."
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
              <p>Une reprise conserve exactement la même demande pour éviter les doublons.</p>
            </div>
          </div>
        ) : (
          <>
            {catalogError && (
              <p className="live-error" role="alert">
                {catalogError}{" "}
                <Button secondary onClick={() => setCatalogVersion((v) => v + 1)}>
                  Réessayer les choix
                </Button>
              </p>
            )}
            <Field label="Bon de livraison">
              <select
                required
                value={bonId}
                disabled={busy || catalogLoading}
                onChange={(event) => {
                  setBonId(event.target.value);
                  setCamionId("");
                }}
              >
                <option value="">Choisir un bon émis…</option>
                {bonId && !bons.some((bon) => bon.id === bonId) && (
                  <option value={bonId}>{selectedBon?.numero || shortId(bonId)}</option>
                )}
                {bons.map((bon) => (
                  <option key={bon.id} value={bon.id}>
                    {bon.numero.slice(0, 15)} · {bon.destination} · {formatKg(bon.poidsTotalKg)}
                  </option>
                ))}
              </select>
            </Field>
            <div className="workflow-actions">
              <Button
                secondary
                disabled={!bonPage || catalogLoading || busy}
                onClick={() => setBonPage((p) => p - 1)}
              >
                Bons précédents
              </Button>
              <span>Page {bonPage + 1}</span>
              <Button
                secondary
                disabled={!moreBons || catalogLoading || busy}
                onClick={() => setBonPage((p) => p + 1)}
              >
                Bons suivants
              </Button>
            </div>
            <Field label="Camion disponible">
              <select
                required
                value={camionId}
                disabled={busy || catalogLoading}
                onChange={(event) => setCamionId(event.target.value)}
              >
                <option value="">Choisir un camion…</option>
                {trucks.map((truck) => (
                  <option
                    key={truck.id}
                    value={truck.id}
                    disabled={!!selectedBon && truck.capaciteKg < selectedBon.poidsTotalKg}
                  >
                    {truck.immatriculation} · {formatKg(truck.capaciteKg)}
                    {selectedBon && truck.capaciteKg < selectedBon.poidsTotalKg
                      ? " · capacité insuffisante"
                      : ""}
                  </option>
                ))}
              </select>
            </Field>
            <div className="workflow-actions">
              <Button
                secondary
                disabled={!truckPage || catalogLoading || busy}
                onClick={() => {
                  setTruckPage((p) => p - 1);
                  setCamionId("");
                }}
              >
                Camions précédents
              </Button>
              <span>Page {truckPage + 1}</span>
              <Button
                secondary
                disabled={!moreTrucks || catalogLoading || busy}
                onClick={() => {
                  setTruckPage((p) => p + 1);
                  setCamionId("");
                }}
              >
                Camions suivants
              </Button>
              <Button
                secondary
                disabled={catalogLoading || busy}
                onClick={() => setCatalogVersion((v) => v + 1)}
              >
                Actualiser les choix
              </Button>
            </div>
            {selectedBon && (
              <p className="notice">
                Destination : {selectedBon.destination} · Poids :{" "}
                {formatKg(selectedBon.poidsTotalKg)}
              </p>
            )}
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
        <Button
          type="submit"
          disabled={
            busy ||
            (!pending && (catalogLoading || !!catalogError || !selectedBon || !selectedTruck))
          }
        >
          {busy ? "Envoi…" : pending ? "Reprendre la même demande" : "Planifier"}
        </Button>{" "}
        {pending && (
          <Button secondary disabled={busy} onClick={abandon}>
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
  refreshing = false,
}: {
  transport: Transport;
  onChanged: () => void;
  refreshing?: boolean;
}) {
  const { canEdit } = useAuth();

  const [date, setDate] = useState("");
  const [type, setType] = useState("POINTAGE");
  const [description, setDescription] = useState("");
  const [preparedProof, setPreparedProof] = useState<Delivery | null>(() =>
    readDelivery(transport.id),
  );
  const [receptionnaire, setReceptionnaire] = useState(preparedProof?.receptionnaire || "");
  const [dateReception, setDateReception] = useState(preparedProof?.dateReception || "");
  const [commentaire, setCommentaire] = useState(preparedProof?.commentaire || "");
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState("");
  const [uncertain, setUncertain] = useState(false);

  if (!canEdit) return null;

  const id = encodeURIComponent(transport.id);
  const status = transport.statut;

  async function execute(action: string, data: unknown, label: string) {
    if (busy || !window.confirm(`${label} ?`)) {
      return;
    }

    setMessage("");
    setBusy(true);

    try {
      if (action === "livraison") {
        sessionStorage.setItem(deliveryKey(transport.id), JSON.stringify(data));
        setPreparedProof(data as Delivery);
      }
      await post(`/api/v1/transports/${id}/${action}`, data);

      if (action === "livraison") {
        try {
          sessionStorage.removeItem(deliveryKey(transport.id));
        } catch {
          /* La livraison a réussi. */
        }
        setPreparedProof(null);
      }

      setMessage("Action enregistrée. Les données ont été actualisées.");
      onChanged();
    } catch (e) {
      setMessage(
        (e instanceof Error ? e.message : "Erreur inattendue.") +
          " Vérifiez l'état et les événements avant toute nouvelle tentative. Pour une livraison déjà commencée, réutilisez exactement les mêmes informations.",
      );

      setUncertain(true);
    } finally {
      setBusy(false);
    }
  }

  function dated(action: string, field: string, label: string) {
    try {
      void execute(action, { [field]: toIso(date) }, label);
    } catch (e) {
      setMessage(e instanceof Error ? e.message : "Date invalide.");
    }
  }

  return (
    <Card title="Actions du transport" subtitle="Enregistrez les étapes du voyage.">
      <fieldset className="form-body transport-action-fields" disabled={busy || refreshing}>
        {message && (
          <p className="notice" role="status">
            {message}
          </p>
        )}

        {uncertain && (
          <Button
            secondary
            onClick={() => {
              setUncertain(false);
              onChanged();
            }}
          >
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
            <input type="datetime-local" value={date} onChange={(e) => setDate(e.target.value)} />
          </Field>
        )}

        {status === "PLANIFIE" && (
          <Button
            disabled={busy || uncertain}
            onClick={() => dated("depart", "departReel", "Déclarer le départ")}
          >
            Déclarer le départ
          </Button>
        )}

        {status === "EN_COURS" && (
          <>
            <Button
              disabled={busy || uncertain}
              onClick={() => dated("arrivee", "arriveeReelle", "Déclarer l'arrivée")}
            >
              Déclarer l'arrivée
            </Button>

            <h3>Ajouter un événement</h3>

            <Field label="Type">
              <select value={type} onChange={(e) => setType(e.target.value)}>
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

            <p>La date et l'heure ci-dessus sont également utilisées pour l'événement.</p>

            <Button
              secondary
              disabled={busy || uncertain}
              onClick={() => {
                try {
                  if (!description.trim()) {
                    throw new Error("La description de l’événement est obligatoire.");
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
                    "Ajouter cet événement",
                  );
                } catch (e) {
                  setMessage(e instanceof Error ? e.message : "Date invalide.");
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

            {preparedProof && (
              <p className="notice">
                Les informations de réception ont été conservées pour reprendre sans doublon.
              </p>
            )}

            <p>En cas de reprise, utilisez exactement les mêmes données de réception.</p>

            <Field label="Réceptionnaire">
              <input
                disabled={busy || !!preparedProof}
                maxLength={160}
                value={receptionnaire}
                onChange={(e) => setReceptionnaire(e.target.value)}
              />
            </Field>

            <Field label="Date de réception">
              <input
                disabled={busy || !!preparedProof}
                type="date"
                value={dateReception}
                onChange={(e) => setDateReception(e.target.value)}
              />
            </Field>

            <Field label="Commentaire">
              <textarea
                disabled={busy || !!preparedProof}
                maxLength={1000}
                value={commentaire}
                onChange={(e) => setCommentaire(e.target.value)}
              />
            </Field>

            <Button
              disabled={busy || uncertain}
              onClick={() => {
                if (!receptionnaire.trim() || !dateReception) {
                  setMessage("Réceptionnaire et date de réception obligatoires.");
                  return;
                }

                void execute(
                  "livraison",
                  {
                    receptionnaire: receptionnaire.trim(),
                    dateReception,
                    commentaire: commentaire || null,
                  },
                  "Confirmer la livraison",
                );
              }}
            >
              Confirmer la livraison
            </Button>
          </>
        )}

        {(status === "PLANIFIE" || status === "ANNULATION_EN_ATTENTE") && (
          <div>
            <Button
              danger
              disabled={busy || uncertain}
              onClick={() =>
                void execute(
                  "annulation",
                  undefined,
                  status === "PLANIFIE" ? "Annuler le transport" : "Reprendre l'annulation",
                )
              }
            >
              {status === "PLANIFIE" ? "Annuler le transport" : "Reprendre l'annulation"}
            </Button>
          </div>
        )}

        {status === "PREPARATION" && (
          <p>
            Réservation en attente. Reprenez la planification avec la même clé et les mêmes données
            depuis la liste.
          </p>
        )}

        {(status === "LIVRE" || status === "ANNULE") && <p>Ce transport est terminé.</p>}

        {busy && <p>Opération en cours…</p>}
      </fieldset>
    </Card>
  );
}
