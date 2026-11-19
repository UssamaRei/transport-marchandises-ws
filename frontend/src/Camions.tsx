import { useEffect, useState, type FormEvent } from "react";
import { Link, useParams, useSearchParams } from "react-router-dom";
import {
  api,
  errorOf,
  formatKg,
  shortId,
  type Camion,
  type CamionsPage,
  type ApiError,
} from "./api";
import { useAuth } from "./auth";
import { Badge, Button, Card, Field, Icon } from "./components/ui";
import { ErrorBanner, Loading, Modal, useToast } from "./components/live";

export function CamionsList() {
  const { canEdit } = useAuth();
  const [params, setParams] = useSearchParams();
  const [page, setPage] = useState(0);
  const disponible = ["true", "false"].includes(params.get("disponible") || "")
    ? params.get("disponible")!
    : "";
  const [data, setData] = useState<CamionsPage | null>(null);
  const [error, setError] = useState<ApiError | null>(null);
  const [version, setVersion] = useState(0);
  const [creating, setCreating] = useState(false);
  const toast = useToast();
  useEffect(() => {
    const controller = new AbortController();
    setData(null);
    setError(null);
    api
      .flotte(page, disponible, controller.signal)
      .then((result) => {
        if (!controller.signal.aborted) setData(result);
      })
      .catch((cause) => {
        if (!controller.signal.aborted) setError(errorOf(cause));
      });
    return () => controller.abort();
  }, [page, disponible, version]);
  return (
    <>
      <div className="welcome inner-welcome">
        <div>
          <span className="eyebrow">FLOTTE</span>
          <h1>
            Vos camions<span className="title-dot">.</span>
          </h1>
          <p>Les véhicules, leur capacité et leur disponibilité en temps réel.</p>
        </div>
        {canEdit && (
          <Button icon="plus" onClick={() => setCreating(true)}>
            Ajouter un camion
          </Button>
        )}
      </div>
      <ErrorBanner error={error} retry={() => setVersion((v) => v + 1)} />
      <Card
        title="La flotte"
        action={
          <Button secondary icon="clock" onClick={() => setVersion((v) => v + 1)}>
            Actualiser
          </Button>
        }
      >
        <div className="form-body fleet-filter">
          <Field label="Disponibilité">
            <select
              value={disponible}
              onChange={(event) => {
                setParams(event.target.value ? { disponible: event.target.value } : {});
                setPage(0);
              }}
            >
              <option value="">Tous les camions</option>
              <option value="true">Disponibles</option>
              <option value="false">Indisponibles</option>
            </select>
          </Field>
        </div>
        {!data && !error ? (
          <Loading />
        ) : (
          data && (
            <>
              {data.items.length ? (
                <div className="table-scroll">
                  <table>
                    <thead>
                      <tr>
                        <th>Immatriculation</th>
                        <th>Capacité</th>
                        <th>État</th>
                        <th>Disponibilité</th>
                        <th>Détail</th>
                      </tr>
                    </thead>
                    <tbody>
                      {data.items.map((truck) => (
                        <tr key={truck.id}>
                          <td>
                            <Link className="inline-link" to={`/camions/${truck.id}`}>
                              {truck.immatriculation}
                            </Link>
                          </td>
                          <td>{formatKg(truck.capaciteKg)}</td>
                          <td>
                            {truck.archive
                              ? "Archivé"
                              : truck.etat === "MAINTENANCE"
                                ? "Maintenance"
                                : "En service"}
                          </td>
                          <td>
                            <Badge>{truck.disponible ? "Disponible" : "Indisponible"}</Badge>
                          </td>
                          <td>
                            <Link
                              to={`/camions/${truck.id}`}
                              aria-label={`Consulter ${truck.immatriculation}`}
                            >
                              <Icon name="chevron" size={16} />
                            </Link>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              ) : (
                <div className="live-empty">
                  <Icon name="truck" size={34} />
                  <h3>Aucun camion à afficher</h3>
                  <p>
                    {disponible
                      ? "Essayez une autre disponibilité."
                      : "Ajoutez votre premier véhicule pour préparer un transport."}
                  </p>
                </div>
              )}
              <div className="table-footer">
                <span>{data.totalElements} camion(s)</span>
                <div className="workflow-actions">
                  <Button secondary disabled={page === 0} onClick={() => setPage((p) => p - 1)}>
                    Précédent
                  </Button>
                  <span>
                    Page {page + 1} / {Math.max(1, data.totalPages)}
                  </span>
                  <Button
                    secondary
                    disabled={page + 1 >= data.totalPages}
                    onClick={() => setPage((p) => p + 1)}
                  >
                    Suivant
                  </Button>
                </div>
              </div>
            </>
          )
        )}
      </Card>
      {creating && (
        <CamionForm
          close={() => setCreating(false)}
          saved={() => {
            setCreating(false);
            setPage(0);
            setVersion((v) => v + 1);
            toast("Camion ajouté à la flotte");
          }}
        />
      )}
    </>
  );
}

export function CamionDetail() {
  const { id = "" } = useParams();
  const { canEdit } = useAuth();
  const [data, setData] = useState<Camion | null>(null);
  const [error, setError] = useState<ApiError | null>(null);
  const [version, setVersion] = useState(0);
  const [editing, setEditing] = useState(false);
  const toast = useToast();
  useEffect(() => {
    const controller = new AbortController();
    setData(null);
    setError(null);
    api
      .camion(id, controller.signal)
      .then((result) => {
        if (!controller.signal.aborted) setData(result);
      })
      .catch((cause) => {
        if (!controller.signal.aborted) setError(errorOf(cause));
      });
    return () => controller.abort();
  }, [id, version]);
  return (
    <>
      <div className="welcome inner-welcome">
        <div>
          <span className="eyebrow">FLOTTE · {shortId(id)}</span>
          <h1>
            {data?.immatriculation || "Camion"}
            <span className="title-dot">.</span>
          </h1>
          <p>La disponibilité tient compte des réservations actives.</p>
        </div>
        <Link className="btn secondary" to="/camions">
          Retour à la flotte
        </Link>
      </div>
      <ErrorBanner error={error} retry={() => setVersion((v) => v + 1)} />
      {!data && !error ? (
        <Loading />
      ) : (
        data && (
          <>
            <Card
              title="Fiche du véhicule"
              action={
                canEdit && (
                  <Button icon="edit" secondary onClick={() => setEditing(true)}>
                    Modifier le camion
                  </Button>
                )
              }
            >
              <div className="workflow-facts">
                <div>
                  <span>IMMATRICULATION</span>
                  <strong>{data.immatriculation}</strong>
                </div>
                <div>
                  <span>CAPACITÉ</span>
                  <strong>{formatKg(data.capaciteKg)}</strong>
                </div>
                <div>
                  <span>DISPONIBILITÉ</span>
                  <Badge>{data.disponible ? "Disponible" : "Indisponible"}</Badge>
                </div>
                <div>
                  <span>ÉTAT</span>
                  <strong>
                    {data.archive
                      ? "Archivé"
                      : data.etat === "MAINTENANCE"
                        ? "Maintenance"
                        : "En service"}
                  </strong>
                </div>
              </div>
              <div className="form-body workflow-actions">
                <Link
                  className="btn secondary"
                  to={`/transports?camionId=${encodeURIComponent(id)}`}
                >
                  Voir ses transports
                </Link>
                <Link className="btn secondary" to="/chargements">
                  Préparer un chargement
                </Link>
                <Button secondary onClick={() => setVersion((v) => v + 1)}>
                  Actualiser
                </Button>
              </div>
            </Card>
            {editing && (
              <CamionForm
                camion={data}
                close={() => setEditing(false)}
                saved={(truck) => {
                  setData(truck);
                  setEditing(false);
                  toast("Camion mis à jour");
                }}
              />
            )}
          </>
        )
      )}
    </>
  );
}

function CamionForm({
  camion,
  close,
  saved,
}: {
  camion?: Camion;
  close: () => void;
  saved: (truck: Camion) => void;
}) {
  const [immatriculation, setImmatriculation] = useState(camion?.immatriculation || "");
  const [capacite, setCapacite] = useState(camion?.capaciteKg.toString() || "");
  const [error, setError] = useState<ApiError | null>(null);
  const [busy, setBusy] = useState(false);
  async function submit(event: FormEvent) {
    event.preventDefault();
    if (busy) return;
    setBusy(true);
    setError(null);
    try {
      const body = { immatriculation: immatriculation.trim(), capaciteKg: Number(capacite) };
      saved(camion ? await api.modifierCamion(camion.id, body) : await api.creerCamion(body));
    } catch (cause) {
      setError(errorOf(cause));
    } finally {
      setBusy(false);
    }
  }
  return (
    <Modal title={camion ? "Modifier le camion" : "Ajouter un camion"} close={close} busy={busy}>
      <ErrorBanner error={error} />
      <form onSubmit={(event) => void submit(event)}>
        <Field label="Immatriculation">
          <input
            autoFocus
            required
            maxLength={40}
            disabled={busy}
            value={immatriculation}
            onChange={(event) => setImmatriculation(event.target.value)}
            placeholder="Ex. 12345 A 1"
          />
        </Field>
        <Field
          label="Capacité (kg)"
          hint="Jusqu’à trois décimales. La capacité ne peut pas devenir inférieure au poids déjà réservé."
        >
          <input
            required
            type="number"
            min="0.001"
            max="99999999999.999"
            step="0.001"
            disabled={busy}
            value={capacite}
            onChange={(event) => setCapacite(event.target.value)}
          />
        </Field>
        <div className="live-dialog-actions">
          <Button secondary disabled={busy} onClick={close}>
            Annuler
          </Button>
          <Button type="submit" disabled={busy || !immatriculation.trim()}>
            {busy ? "Enregistrement…" : camion ? "Enregistrer le camion" : "Créer le camion"}
          </Button>
        </div>
      </form>
    </Modal>
  );
}
