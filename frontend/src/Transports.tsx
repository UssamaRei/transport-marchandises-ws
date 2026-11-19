import { useEffect, useState } from "react";
import { Link, useParams, useSearchParams } from "react-router-dom";

import { api, errorOf, type EvenementsPage, type Transport, type TransportsPage } from "./api";

import { Button, Card } from "./components/ui";
import { Loading } from "./components/live";
import { Planification, ActionsTransport } from "./TransportActions";

function formatDate(value: string | null) {
  if (!value) return "—";

  const date = new Date(value);

  return Number.isNaN(date.getTime()) ? value : date.toLocaleString("fr-FR");
}

export function TransportsList() {
  const [params] = useSearchParams();
  const camionId = params.get("camionId") || "";
  const [page, setPage] = useState(0);
  const [data, setData] = useState<TransportsPage | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [version, setVersion] = useState(0);

  useEffect(() => {
    const controller = new AbortController();

    setLoading(true);
    setError("");
    setData(null);

    api
      .transports(page, controller.signal, camionId)
      .then(setData)
      .catch((err: unknown) => {
        if (!controller.signal.aborted) {
          setError(errorOf(err).message);
        }
      })
      .finally(() => {
        if (!controller.signal.aborted) {
          setLoading(false);
        }
      });

    return () => controller.abort();
  }, [page, version, camionId]);

  return (
    <>
      <div className="welcome inner-welcome">
        <div>
          <span className="eyebrow">SUIVI DES TRANSPORTS</span>
          <h1>
            Transports<span className="title-dot">.</span>
          </h1>
          <p>Consultez et planifiez vos transports.</p>
          {camionId && (
            <p>
              Transports du camion sélectionné ·{" "}
              <Link to="/transports">Voir tous les transports</Link>
            </p>
          )}
        </div>
      </div>

      <Card title="Tous les transports">
        {loading && <Loading />}

        {error && (
          <div className="live-error" role="alert">
            <p>{error}</p>

            <Button secondary onClick={() => setVersion((v) => v + 1)}>
              Réessayer
            </Button>
          </div>
        )}

        {!loading && !error && data && (
          <>
            {data.content.length === 0 ? (
              <div className="form-body">
                <p>Aucun transport à afficher.</p>
              </div>
            ) : (
              <div className="table-scroll">
                <table>
                  <thead>
                    <tr>
                      <th>Transport</th>
                      <th>Destination</th>
                      <th>Camion ID</th>
                      <th>Départ prévu</th>
                      <th>Statut</th>
                    </tr>
                  </thead>

                  <tbody>
                    {data.content.map((transport) => (
                      <tr key={transport.id}>
                        <td>
                          <Link to={`/transports/${encodeURIComponent(transport.id)}`}>
                            {transport.id}
                          </Link>
                        </td>

                        <td>{transport.destination || "—"}</td>
                        <td>{transport.camionId || "—"}</td>
                        <td>{formatDate(transport.departPrevu)}</td>
                        <td>{transport.statut}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}

            <div className="table-footer">
              <span>{data.totalElements} transport(s) au total</span>

              <Button secondary disabled={page === 0} onClick={() => setPage((p) => p - 1)}>
                Précédent
              </Button>

              <span>
                Page {page + 1} / {Math.max(data.totalPages, 1)}
              </span>

              <Button
                secondary
                disabled={page + 1 >= data.totalPages}
                onClick={() => setPage((p) => p + 1)}
              >
                Suivant
              </Button>
            </div>
          </>
        )}
      </Card>

      <Planification />
    </>
  );
}

export function TransportDetail() {
  const { id } = useParams();

  const [transport, setTransport] = useState<Transport | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [version, setVersion] = useState(0);

  useEffect(() => {
    const controller = new AbortController();

    setError("");

    if (!id) {
      setError("Identifiant du transport manquant.");
      setLoading(false);
      return () => controller.abort();
    }

    setLoading(true);

    api
      .transport(id, controller.signal)
      .then(setTransport)
      .catch((err: unknown) => {
        if (!controller.signal.aborted) {
          setError(errorOf(err).message);
        }
      })
      .finally(() => {
        if (!controller.signal.aborted) {
          setLoading(false);
        }
      });

    return () => controller.abort();
  }, [id, version]);

  return (
    <>
      <div className="welcome inner-welcome">
        <div>
          <span className="eyebrow">SUIVI DES TRANSPORTS</span>
          <h1>
            Détail du transport<span className="title-dot">.</span>
          </h1>
          <Link to="/transports">← Retour à la liste</Link>
        </div>
      </div>

      <Card title="Informations">
        {loading && <Loading />}

        {error && (
          <p className="live-error" role="alert">
            {error}{" "}
            <Button secondary onClick={() => setVersion((v) => v + 1)}>
              Réessayer
            </Button>
          </p>
        )}

        {transport && !loading && !error && (
          <div className="form-body">
            <p>
              <strong>ID :</strong> {transport.id}
            </p>
            <p>
              <strong>Bon de livraison :</strong>{" "}
              <Link className="inline-link" to={`/bons/${transport.bonId}`}>
                Consulter le bon
              </Link>
            </p>
            <p>
              <strong>Camion :</strong>{" "}
              <Link className="inline-link" to={`/camions/${transport.camionId}`}>
                Consulter le camion
              </Link>
            </p>
            <p>
              <strong>Destination :</strong> {transport.destination || "—"}
            </p>
            <p>
              <strong>Poids :</strong> {transport.poidsKg == null ? "—" : `${transport.poidsKg} kg`}
            </p>
            <p>
              <strong>Départ prévu :</strong> {formatDate(transport.departPrevu)}
            </p>
            <p>
              <strong>Statut :</strong> {transport.statut}
            </p>
          </div>
        )}
      </Card>

      {transport && !error && (
        <>
          <ActionsTransport
            key={transport.id}
            transport={transport}
            refreshing={loading}
            onChanged={() => setVersion((v) => v + 1)}
          />

          <TransportEvenements id={transport.id} version={version} />
        </>
      )}
    </>
  );
}

function TransportEvenements({ id, version }: { id: string; version: number }) {
  const [data, setData] = useState<EvenementsPage | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    const controller = new AbortController();

    setData(null);
    setLoading(true);
    setError("");

    api
      .evenementsTransport(id, controller.signal)
      .then(setData)
      .catch((err: unknown) => {
        if (!controller.signal.aborted) {
          setError(errorOf(err).message);
        }
      })
      .finally(() => {
        if (!controller.signal.aborted) {
          setLoading(false);
        }
      });

    return () => controller.abort();
  }, [id, version]);

  return (
    <Card title="Événements de suivi">
      {loading && <Loading />}

      {error && (
        <p className="live-error" role="alert">
          {error}
        </p>
      )}

      {!loading && !error && data && (
        <div className="form-body">
          {data.content.length === 0 ? (
            <p>Aucun événement à afficher.</p>
          ) : (
            <ul>
              {data.content.map((evenement) => (
                <li key={evenement.id}>
                  <strong>{evenement.type}</strong>
                  {" — "}
                  {formatDate(evenement.dateEvenement)}
                  {evenement.description && ` — ${evenement.description}`}
                </li>
              ))}
            </ul>
          )}

          <p>{data.totalElements} événement(s) au total — 20 premiers affichés.</p>
        </div>
      )}
    </Card>
  );
}
