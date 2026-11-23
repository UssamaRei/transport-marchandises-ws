import { useEffect, useState, type CSSProperties, type ReactNode } from "react";
import { Link } from "react-router-dom";
import { errorOf, formatKg, shortId, type ApiError } from "./api";
import { useAuth } from "./auth";
import { dashboardApi } from "./dashboard-api";
import { Badge, Button, Card, Icon } from "./components/ui";
import { Loading } from "./components/live";
import "./dashboard.css";

type Section<T> = { data: T | null; loading: boolean; error: ApiError | null };
function useSection<T>(load: (signal: AbortSignal) => Promise<T>, version: number) {
  const [state, setState] = useState<Section<T>>({ data: null, loading: true, error: null });
  useEffect(() => {
    const controller = new AbortController();
    setState({ data: null, loading: true, error: null });
    load(controller.signal).then(
      (data) => {
        if (!controller.signal.aborted) setState({ data, loading: false, error: null });
      },
      (error: unknown) => {
        if (!controller.signal.aborted)
          setState({ data: null, loading: false, error: errorOf(error) });
      },
    );
    return () => controller.abort();
  }, [load, version]);
  return state;
}

function SectionBody<T>({
  state,
  retry,
  children,
}: {
  state: Section<T>;
  retry: () => void;
  children: (data: T) => ReactNode;
}) {
  if (state.loading) return <Loading text="Chargement des données…" />;
  if (state.error)
    return (
      <div className="dash-error" role="alert">
        <Icon name="alert" />
        <p>{state.error.message}</p>
        <Button secondary onClick={retry}>
          Réessayer
        </Button>
      </div>
    );
  return state.data ? children(state.data) : null;
}

const transportLabels: Record<string, string> = {
  PREPARATION: "En préparation",
  PLANIFIE: "Planifié",
  EN_COURS: "En cours",
  CLOTURE_EN_ATTENTE: "Clôture en attente",
  LIVRE: "Livré",
  ANNULATION_EN_ATTENTE: "Annulation en attente",
  ANNULE: "Annulé",
};
const count = (value: number) => value.toLocaleString("fr-FR");
const date = (value: string | null) =>
  value
    ? new Date(value).toLocaleDateString("fr-FR", {
        day: "numeric",
        month: "short",
        hour: "2-digit",
        minute: "2-digit",
      })
    : "Non prévu";

function Metric({
  title,
  value,
  detail,
  icon,
  to,
  loading,
  error,
}: {
  title: string;
  value?: number;
  detail: string;
  icon: "truck" | "box" | "route" | "file";
  to: string;
  loading: boolean;
  error: ApiError | null;
}) {
  return (
    <Link
      className={`dash-metric dash-metric-${icon}`}
      to={to}
      aria-label={`${title} : ${loading ? "chargement" : error ? "indisponible" : count(value ?? 0)}`}
    >
      <div className="dash-metric-top">
        <span>{title}</span>
        <span className="dash-metric-icon">
          <Icon name={icon} />
        </span>
      </div>
      <strong className="dash-number" data-testid={`metric-${icon}`}>
        {loading ? "…" : error ? "—" : count(value ?? 0)}
      </strong>
      <div className="dash-metric-foot">
        <span>
          {loading ? "Récupération des données" : error ? "Service indisponible" : detail}
        </span>
        <Icon name="arrow" size={16} />
      </div>
    </Link>
  );
}

export default function Dashboard() {
  const auth = useAuth();
  const [version, setVersion] = useState(0);
  const camions = useSection(dashboardApi.camions, version);
  const chargements = useSection(dashboardApi.chargements, version);
  const bons = useSection(dashboardApi.bons, version);
  const transports = useSection(dashboardApi.transports, version);
  const refresh = () => setVersion((v) => v + 1);
  const busy = [camions, chargements, bons, transports].some((s) => s.loading);
  return (
    <div className="live-dashboard">
      <div className="welcome inner-welcome dash-welcome">
        <div>
          <span className="eyebrow">VUE D’ENSEMBLE</span>
          <h1>
            Tableau de bord<span className="title-dot">.</span>
          </h1>
          <p>Bonjour {auth.name}, voici où en sont vos opérations.</p>
          <span className="dash-date">
            <Icon name="calendar" size={15} />
            {new Date().toLocaleDateString("fr-FR", {
              weekday: "long",
              day: "numeric",
              month: "long",
              year: "numeric",
            })}
          </span>
        </div>
        <div className="dash-actions">
          <Button secondary disabled={busy} onClick={refresh}>
            {busy ? "Actualisation…" : "Actualiser"}
          </Button>
          {auth.canEdit && (
            <Link className="btn" to="/chargements/nouveau">
              <Icon name="plus" size={17} />
              Nouveau chargement
            </Link>
          )}
        </div>
      </div>

      <div className="dash-metrics">
        <Metric
          title="Camions disponibles"
          icon="truck"
          to="/camions?disponible=true"
          value={camions.data?.disponibles}
          detail={`sur ${count(camions.data?.total ?? 0)} camions`}
          {...camions}
        />
        <Metric
          title="Chargements en brouillon"
          icon="box"
          to="/chargements"
          value={chargements.data?.brouillons}
          detail={`sur ${count(chargements.data?.total ?? 0)} chargements`}
          {...chargements}
        />
        <Metric
          title="Transports actifs"
          icon="route"
          to="/transports?actifs=true"
          value={transports.data?.actifs}
          detail="En préparation, en route ou à clôturer"
          {...transports}
        />
        <Metric
          title="Bons de livraison"
          icon="file"
          to="/bons"
          value={bons.data?.total}
          detail={`${count(bons.data?.enAttente ?? 0)} en attente de réception`}
          {...bons}
        />
      </div>

      {(camions.error || chargements.error || bons.error || transports.error) && (
        <div className="dash-warning" role="alert">
          <Icon name="alert" />
          <span>
            Certaines données sont indisponibles. Les autres modules restent consultables.
          </span>
          <Button secondary onClick={refresh} disabled={busy}>
            Réessayer
          </Button>
        </div>
      )}

      <div className="dash-attention">
        <div>
          <span className="dash-attention-icon">
            <Icon name="clock" />
          </span>
          <div>
            <strong>À suivre</strong>
            <small>Vos prochaines étapes</small>
          </div>
        </div>
        <Link to="/chargements">
          <strong>{chargements.data ? count(chargements.data.brouillons) : "—"}</strong>
          <span>Brouillons à compléter</span>
          <Icon name="chevron" size={16} />
        </Link>
        <Link to="/transports?statut=CLOTURE_EN_ATTENTE">
          <strong>{transports.data ? count(transports.data.aConfirmer) : "—"}</strong>
          <span>Clôtures à confirmer</span>
          <Icon name="chevron" size={16} />
        </Link>
      </div>

      <div className="dash-main-grid">
        <Card
          title="Transports actifs"
          subtitle="Les cinq transports actifs les plus récents."
          action={
            <Link className="text-link" to="/transports?actifs=true">
              Tout voir <Icon name="arrow" size={15} />
            </Link>
          }
        >
          <SectionBody state={transports} retry={refresh}>
            {(data) =>
              data.recent.length ? (
                <div className="table-scroll">
                  <table>
                    <thead>
                      <tr>
                        <th>Transport</th>
                        <th>Destination</th>
                        <th>Départ prévu</th>
                        <th>Statut</th>
                      </tr>
                    </thead>
                    <tbody>
                      {data.recent.map((t) => (
                        <tr key={t.id}>
                          <td>
                            <Link
                              className="dash-row-link"
                              to={`/transports/${encodeURIComponent(t.id)}`}
                            >
                              {shortId(t.id)}
                            </Link>
                            <Link
                              className="dash-sub-link"
                              to={`/camions/${encodeURIComponent(t.camionId)}`}
                            >
                              Camion {shortId(t.camionId)}
                            </Link>
                          </td>
                          <td>{t.destination || "À préciser"}</td>
                          <td>{date(t.departPrevu)}</td>
                          <td>
                            <Badge>{transportLabels[t.statut] || t.statut}</Badge>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              ) : (
                <div className="dash-empty">
                  <Icon name="route" size={30} />
                  <p>Aucun transport actif pour le moment.</p>
                  <Link to="/transports" className="text-link">
                    Consulter les transports →
                  </Link>
                </div>
              )
            }
          </SectionBody>
        </Card>
        <Card title="Les transports en un regard" subtitle="Répartition de tous les transports.">
          <SectionBody state={transports} retry={refresh}>
            {(data) => {
              const total = data.actifs + data.livres + data.annules;
              const activeEnd = total ? (data.actifs / total) * 100 : 0;
              const deliveredEnd = total ? ((data.actifs + data.livres) / total) * 100 : 0;
              return (
                <div className="dash-distribution">
                  <div
                    className={`dash-donut ${total ? "" : "is-empty"}`}
                    aria-hidden="true"
                    style={
                      {
                        "--active-end": `${activeEnd}%`,
                        "--delivered-end": `${deliveredEnd}%`,
                      } as CSSProperties
                    }
                  >
                    <div>
                      <strong>{count(total)}</strong>
                      <span>au total</span>
                    </div>
                  </div>
                  {total === 0 && <p className="dash-muted">Aucun transport enregistré.</p>}
                  <ul className="dash-legend">
                    {[
                      {
                        label: "Actifs",
                        value: data.actifs,
                        color: "#12988c",
                        to: "/transports?actifs=true",
                      },
                      {
                        label: "Livrés",
                        value: data.livres,
                        color: "#344e68",
                        to: "/transports?statut=LIVRE",
                      },
                      {
                        label: "Annulés",
                        value: data.annules,
                        color: "#cbd5df",
                        to: "/transports?statut=ANNULE",
                      },
                    ].map((item) => (
                      <li key={item.label}>
                        <Link to={item.to}>
                          <span>
                            <i style={{ background: item.color }} />
                            {item.label}
                          </span>
                          <strong>{count(item.value)}</strong>
                        </Link>
                      </li>
                    ))}
                  </ul>
                </div>
              );
            }}
          </SectionBody>
        </Card>
      </div>

      <div className="dash-bottom-grid">
        <Card
          title="Derniers chargements"
          subtitle="De la préparation à la validation."
          action={
            <Link className="text-link" to="/chargements">
              Tout voir <Icon name="arrow" size={15} />
            </Link>
          }
        >
          <SectionBody state={chargements} retry={refresh}>
            {(data) =>
              data.recent.length ? (
                <ul className="dash-recent">
                  {data.recent.map((c) => (
                    <li key={c.id}>
                      <Link to={`/chargements/${encodeURIComponent(c.id)}`}>
                        <span className="dash-list-icon">
                          <Icon name="box" />
                        </span>
                        <span className="dash-recent-text">
                          <strong>
                            {c.origine || "Origine à préciser"} →{" "}
                            {c.destination || "Destination à préciser"}
                          </strong>
                          <small>
                            {shortId(c.id)} · {formatKg(c.poidsTotalKg)}
                          </small>
                        </span>
                        <Badge>{c.statut === "VALIDE" ? "Validé" : "Brouillon"}</Badge>
                      </Link>
                    </li>
                  ))}
                </ul>
              ) : (
                <div className="dash-empty">
                  <Icon name="box" size={30} />
                  <p>Aucun chargement enregistré.</p>
                </div>
              )
            }
          </SectionBody>
        </Card>
        <Card
          title="Derniers bons de livraison"
          subtitle="Les documents de vos expéditions."
          action={
            <Link className="text-link" to="/bons">
              Tout voir <Icon name="arrow" size={15} />
            </Link>
          }
        >
          <SectionBody state={bons} retry={refresh}>
            {(data) =>
              data.recent.length ? (
                <ul className="dash-recent">
                  {data.recent.map((b) => (
                    <li key={b.id}>
                      <Link to={`/bons/${encodeURIComponent(b.id)}`}>
                        <span className="dash-list-icon dash-list-file">
                          <Icon name="file" />
                        </span>
                        <span className="dash-recent-text">
                          <strong>{b.numero}</strong>
                          <small>
                            {b.destination || "Destination à préciser"} · {formatKg(b.poidsTotalKg)}
                          </small>
                        </span>
                        <Badge>{b.statut === "LIVRE" ? "Livré" : "Émis"}</Badge>
                      </Link>
                    </li>
                  ))}
                </ul>
              ) : (
                <div className="dash-empty">
                  <Icon name="file" size={30} />
                  <p>Aucun bon de livraison enregistré.</p>
                </div>
              )
            }
          </SectionBody>
        </Card>
      </div>
      <p className="dash-source">
        <Icon name="check" size={14} />
        Données des quatre modules · Utilisez « Actualiser » pour mettre à jour la vue.
      </p>
    </div>
  );
}
