import { useEffect, useMemo, useState, type FormEvent } from "react";
import { Link, useNavigate, useParams, useSearchParams } from "react-router-dom";
import {
  api,
  ApiError,
  errorOf,
  formatKg,
  shortId,
  type Bon,
  type BonsPage,
  type Chargement,
  type Resume,
} from "./api";
import { useAuth } from "./auth";
import { Badge, Button, Card, Field, Icon } from "./components/ui";
import { ErrorBanner, Loading, useToast } from "./components/live";
import "./bons.css";

const PAGE_SIZE = 20;

function BonHistory({ refresh }: { refresh: number }) {
  const [page, setPage] = useState(0);
  const [data, setData] = useState<BonsPage | null>(null);
  const [error, setError] = useState<ApiError | null>(null);
  const [version, setVersion] = useState(0);
  useEffect(() => {
    const controller = new AbortController();
    setData(null);
    setError(null);
    api
      .bons(page, controller.signal)
      .then((result) => {
        if (!controller.signal.aborted) setData(result);
      })
      .catch((cause) => {
        if (!controller.signal.aborted) setError(errorOf(cause));
      });
    return () => controller.abort();
  }, [page, refresh, version]);
  return (
    <div className="workflow-next">
      <Card
        title="Bons émis"
        subtitle="Les documents enregistrés et leur statut de réception."
        action={
          <Button secondary onClick={() => setVersion((v) => v + 1)}>
            Actualiser les bons
          </Button>
        }
      >
        <ErrorBanner error={error} retry={() => setVersion((v) => v + 1)} />
        {!data && !error ? (
          <Loading />
        ) : (
          data && (
            <>
              {data.content.length ? (
                <div className="table-scroll">
                  <table>
                    <thead>
                      <tr>
                        <th>Bon</th>
                        <th>Destination</th>
                        <th>Poids</th>
                        <th>Statut</th>
                        <th>Créé le</th>
                      </tr>
                    </thead>
                    <tbody>
                      {data.content.map((bon) => (
                        <tr key={bon.id}>
                          <td>
                            <Link className="inline-link" to={`/bons/${bon.id}`} title={bon.numero}>
                              {bon.numero.slice(0, 15)}
                            </Link>
                          </td>
                          <td>{bon.destination}</td>
                          <td>{formatKg(bon.poidsTotalKg)}</td>
                          <td>
                            <Badge>{bon.statut === "LIVRE" ? "Livré" : "Émis"}</Badge>
                          </td>
                          <td>
                            {bon.createdAt
                              ? new Date(bon.createdAt).toLocaleDateString("fr-FR")
                              : "—"}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              ) : (
                <div className="form-body">
                  <p>Aucun bon émis pour le moment.</p>
                </div>
              )}
              <div className="table-footer">
                <span>{data.totalElements} bon(s)</span>
                <div className="workflow-actions">
                  <Button secondary disabled={page === 0} onClick={() => setPage((p) => p - 1)}>
                    Bons précédents
                  </Button>
                  <span>
                    Page {page + 1} / {Math.max(1, data.totalPages)}
                  </span>
                  <Button
                    secondary
                    disabled={page + 1 >= data.totalPages}
                    onClick={() => setPage((p) => p + 1)}
                  >
                    Bons suivants
                  </Button>
                </div>
              </div>
            </>
          )
        )}
      </Card>
    </div>
  );
}

function PageHeading({ title, detail = false }: { title: string; detail?: boolean }) {
  return (
    <div className="welcome inner-welcome bons-heading">
      <div>
        <span className="eyebrow">
          BONS DE LIVRAISON <span className="screen-code">BL{detail ? "02" : "01"}</span>
        </span>
        <h1>
          {title}
          <span className="title-dot">.</span>
        </h1>
        <p>
          {detail
            ? "Document émis depuis un chargement validé, conservé comme snapshot."
            : "Émettez un document de livraison à partir d’un chargement validé."}
        </p>
      </div>
      {detail && (
        <Link className="btn secondary" to="/bons">
          <Icon name="arrow" size={16} /> Retour aux bons
        </Link>
      )}
    </div>
  );
}

export function BonsList() {
  const { canEdit } = useAuth();
  const navigate = useNavigate();
  const toast = useToast();
  const [params] = useSearchParams();
  const [rows, setRows] = useState<Resume[]>([]);
  const [offset, setOffset] = useState(0);
  const [refresh, setRefresh] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<ApiError | null>(null);
  const [selectedId, setSelectedId] = useState(params.get("chargementId") || "");
  const [searchId, setSearchId] = useState("");
  const [selected, setSelected] = useState<Chargement | null>(null);
  const [previewLoading, setPreviewLoading] = useState(false);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    const controller = new AbortController();
    setLoading(true);
    setError(null);
    api
      .list(offset, controller.signal)
      .then((result) => {
        if (controller.signal.aborted) return;
        setRows(result);
      })
      .catch((cause) => {
        if (!controller.signal.aborted) setError(errorOf(cause));
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false);
      });
    return () => controller.abort();
  }, [offset, refresh]);

  useEffect(() => {
    if (!selectedId) {
      setSelected(null);
      return;
    }
    const controller = new AbortController();
    setSelected(null);
    setError(null);
    setPreviewLoading(true);
    api
      .detail(selectedId, controller.signal)
      .then((data) => {
        if (!controller.signal.aborted) setSelected(data);
      })
      .catch((cause) => {
        if (!controller.signal.aborted) setError(errorOf(cause));
      })
      .finally(() => {
        if (!controller.signal.aborted) setPreviewLoading(false);
      });
    return () => controller.abort();
  }, [selectedId]);

  const validLoads = useMemo(() => rows.filter((row) => row.statut === "VALIDE"), [rows]);

  async function emit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!canEdit || !selected || selected.id !== selectedId || selected.statut !== "VALIDE" || busy)
      return;
    setBusy(true);
    setError(null);
    try {
      const bon = await api.creerBon(selected.id);
      toast(`Bon ${bon.numero} émis`);
      navigate(`/bons/${encodeURIComponent(bon.id)}`);
    } catch (cause) {
      const apiError = errorOf(cause);
      setError(apiError);
      if (apiError.status === 409) setRefresh((value) => value + 1);
    } finally {
      setBusy(false);
    }
  }

  function openBon(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const id = searchId.trim();
    if (id) navigate(`/bons/${encodeURIComponent(id)}`);
  }

  return (
    <div className="bons-page">
      <PageHeading title="Bons de livraison" />
      <ErrorBanner error={error} retry={() => setRefresh((value) => value + 1)} />
      <BonHistory refresh={refresh} />

      <div className="bons-layout">
        <section className="bons-main-column">
          <Card id="emettre-bon" className="bons-issue-card">
            <div className="bons-issue-banner">
              <div className="bons-banner-icon">
                <Icon name="file" size={24} />
              </div>
              <div>
                <span className="bons-kicker">ÉMISSION SÉCURISÉE</span>
                <h2>Un bon prêt pour la livraison</h2>
                <p>Le bon reprend les adresses et les marchandises du chargement validé.</p>
              </div>
              <div className="bons-step-mark">
                <span>01</span>
                <i />
                <span>02</span>
                <i />
                <span>03</span>
              </div>
            </div>
            <form className="bons-issue-form" onSubmit={emit}>
              {!canEdit && (
                <div className="bons-readonly-note">
                  <Icon name="alert" size={16} /> Votre rôle permet la consultation, pas l’émission
                  d’un bon.
                </div>
              )}
              <div className="bons-form-row">
                <Field
                  label="Chargement validé"
                  hint="Seuls les chargements au statut VALIDE sont disponibles."
                >
                  <select
                    value={selectedId}
                    onChange={(event) => {
                      setSelectedId(event.target.value);
                      setError(null);
                    }}
                    disabled={!canEdit || loading || busy}
                  >
                    <option value="">Choisir un chargement…</option>
                    {selectedId && !validLoads.some((row) => row.id === selectedId) && (
                      <option value={selectedId}>CH-{shortId(selectedId)} · sélectionné</option>
                    )}
                    {validLoads.map((row) => (
                      <option key={row.id} value={row.id}>
                        {shortId(row.id)} · {row.origine || "Origine"} →{" "}
                        {row.destination || "Destination"}
                      </option>
                    ))}
                  </select>
                </Field>
                <Button
                  type="submit"
                  icon="plus"
                  disabled={
                    !canEdit || !selected || selected.statut !== "VALIDE" || busy || previewLoading
                  }
                >
                  {busy ? "Émission…" : "Émettre le bon"}
                </Button>
              </div>

              {previewLoading && <Loading text="Lecture du chargement…" />}
              {selected && !previewLoading && (
                <div className="bons-load-preview">
                  <div className="bons-preview-route">
                    <span className="bons-route-point">
                      <i />
                      {selected.origine || "—"}
                    </span>
                    <span className="bons-route-line" aria-hidden="true" />
                    <span className="bons-route-point destination">
                      <i />
                      {selected.destination || "—"}
                    </span>
                  </div>
                  <div className="bons-preview-facts">
                    <div>
                      <span>DESTINATAIRE</span>
                      <strong>{selected.destinataireNom || "Non renseigné"}</strong>
                    </div>
                    <div>
                      <span>MARCHANDISES</span>
                      <strong>{selected.lignes.length} ligne(s)</strong>
                    </div>
                    <div>
                      <span>POIDS TOTAL</span>
                      <strong>{formatKg(selected.poidsTotalKg)}</strong>
                    </div>
                  </div>
                  <p className="bons-snapshot-note">
                    <Icon name="check" size={15} /> Le bon conservera une copie stable de ces
                    informations.
                  </p>
                </div>
              )}

              {!loading && !error && validLoads.length === 0 && (
                <div className="bons-empty-inline">
                  <Icon name="box" size={19} />
                  <span>
                    Aucun chargement validé sur cette page. Validez d’abord un chargement pour
                    pouvoir émettre son bon.
                  </span>
                </div>
              )}
            </form>
          </Card>

          <Card
            className="bons-loads-card"
            title="Chargements prêts"
            subtitle="Sélectionnez un chargement validé pour préparer son bon."
            action={
              <span className="bons-count">
                <i />
                {validLoads.length} disponible(s)
              </span>
            }
          >
            {loading ? (
              <Loading text="Chargement des expéditions…" />
            ) : error ? null : validLoads.length === 0 ? (
              <div className="bons-empty-state">
                <span className="bons-empty-icon">
                  <Icon name="box" size={26} />
                </span>
                <h3>Rien à émettre pour le moment</h3>
                <p>Les chargements apparaîtront ici après leur validation.</p>
                <Link className="inline-link" to="/chargements">
                  Ouvrir les chargements
                </Link>
              </div>
            ) : (
              <div className="bons-load-list">
                {validLoads.map((row) => (
                  <button
                    key={row.id}
                    type="button"
                    className={`bons-load-row ${selectedId === row.id ? "selected" : ""}`}
                    onClick={() => setSelectedId(row.id)}
                  >
                    <span className="bons-load-icon">
                      <Icon name="box" size={17} />
                    </span>
                    <span className="bons-load-text">
                      <strong>
                        {row.origine || "Origine inconnue"}
                        <i> → </i>
                        {row.destination || "Destination inconnue"}
                      </strong>
                      <small>
                        CH-{shortId(row.id)} · {formatKg(row.poidsTotalKg)}
                      </small>
                    </span>
                    <Badge>Validé</Badge>
                    <Icon name="chevron" size={16} />
                  </button>
                ))}
              </div>
            )}
            {!loading && (
              <div className="bons-pagination">
                <span>Page de chargements {Math.floor(offset / PAGE_SIZE) + 1}</span>
                <div>
                  <Button
                    secondary
                    disabled={offset === 0 || busy}
                    onClick={() => {
                      setOffset(Math.max(0, offset - PAGE_SIZE));
                      setSelectedId("");
                    }}
                  >
                    Précédent
                  </Button>
                  <Button
                    secondary
                    disabled={rows.length < PAGE_SIZE || busy}
                    onClick={() => {
                      setOffset(offset + PAGE_SIZE);
                      setSelectedId("");
                    }}
                  >
                    Suivant
                  </Button>
                </div>
              </div>
            )}
          </Card>
        </section>

        <aside className="bons-side-column">
          <Card
            className="bons-lookup-card"
            title="Consulter un bon"
            subtitle="Ouvrez un bon connu par son identifiant."
          >
            <form onSubmit={openBon}>
              <Field label="Identifiant du bon">
                <input
                  value={searchId}
                  onChange={(event) => setSearchId(event.target.value)}
                  placeholder="ex. 7f2a…"
                />
              </Field>
              <Button secondary type="submit" icon="search" disabled={!searchId.trim()}>
                Rechercher
              </Button>
            </form>
            <p className="bons-lookup-foot">
              Les bons émis sont immuables. Leur preuve de réception est enregistrée depuis le suivi
              du transport.
            </p>
          </Card>
          <Card className="bons-help-card">
            <span className="bons-help-icon">
              <Icon name="layers" size={19} />
            </span>
            <h3>Du chargement au bon</h3>
            <p>
              Le numéro du bon est généré automatiquement. Les lignes et les coordonnées restent
              figées dans le document émis.
            </p>
            <Link to="/chargements">
              Voir les chargements <Icon name="arrow" size={14} />
            </Link>
          </Card>
        </aside>
      </div>
    </div>
  );
}

export function BonDetail() {
  const { id = "" } = useParams();
  const [bon, setBon] = useState<Bon | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<ApiError | null>(null);
  const [version, setVersion] = useState(0);
  const { canEdit } = useAuth();
  useEffect(() => {
    const controller = new AbortController();
    setLoading(true);
    setBon(null);
    setError(null);
    api
      .bon(id, controller.signal)
      .then((data) => {
        if (!controller.signal.aborted) setBon(data);
      })
      .catch((cause) => {
        if (!controller.signal.aborted) setError(errorOf(cause));
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false);
      });
    return () => controller.abort();
  }, [id, version]);

  return (
    <div className="bons-page">
      <PageHeading title={bon ? bon.numero : "Détail du bon"} detail />
      <ErrorBanner error={error} retry={() => setVersion((v) => v + 1)} />
      {bon && !loading && (
        <div className="workflow-actions workflow-next">
          <Link className="btn secondary" to={`/chargements/${bon.chargementId}`}>
            Voir le chargement
          </Link>
          {canEdit && bon.statut === "EMIS" && (
            <Link className="btn" to={`/transports?bonId=${encodeURIComponent(bon.id)}`}>
              Planifier le transport <Icon name="arrow" size={16} />
            </Link>
          )}
          {bon.transportId && (
            <Link className="btn" to={`/transports/${bon.transportId}`}>
              Voir le transport
            </Link>
          )}
          <Button secondary onClick={() => setVersion((v) => v + 1)}>
            Actualiser le bon
          </Button>
        </div>
      )}
      {loading ? (
        <Loading text="Ouverture du bon…" />
      ) : !bon ? null : (
        <div className="bons-detail-grid">
          <section>
            <Card className="bons-document-card">
              <div className="bons-document-head">
                <div>
                  <span className="bons-kicker">BON DE LIVRAISON</span>
                  <h2>{bon.numero}</h2>
                  <code>{bon.id}</code>
                </div>
                <Badge>{bon.statut === "LIVRE" ? "Livré" : "Émis"}</Badge>
              </div>
              <div className="bons-document-route">
                <div>
                  <span>ORIGINE</span>
                  <strong>{bon.origine}</strong>
                </div>
                <Icon name="arrow" size={19} />
                <div>
                  <span>DESTINATION</span>
                  <strong>{bon.destination}</strong>
                </div>
              </div>
              <div className="bons-document-facts">
                <div>
                  <span>DESTINATAIRE</span>
                  <strong>{bon.destinataireNom}</strong>
                  <small>{bon.destinataireTelephone || "Téléphone non renseigné"}</small>
                </div>
                <div>
                  <span>POIDS TOTAL</span>
                  <strong>{formatKg(bon.poidsTotalKg)}</strong>
                  <small>Chargement CH-{shortId(bon.chargementId)}</small>
                </div>
                <div>
                  <span>CAMION PRÉVU</span>
                  <strong>{shortId(bon.camionSouhaiteId)}</strong>
                  <small>Référence du chargement</small>
                </div>
              </div>
              <div className="bons-lines-heading">
                <div>
                  <h3>Marchandises</h3>
                  <p>{bon.lignes.length} ligne(s) incluse(s) dans le snapshot</p>
                </div>
              </div>
              <div className="table-scroll">
                <table className="bons-lines-table">
                  <thead>
                    <tr>
                      <th>Désignation</th>
                      <th>Quantité</th>
                      <th>Poids unitaire</th>
                      <th>Poids de ligne</th>
                    </tr>
                  </thead>
                  <tbody>
                    {bon.lignes.map((line) => (
                      <tr key={line.id}>
                        <td>
                          <strong>{line.designation}</strong>
                        </td>
                        <td>{line.quantite}</td>
                        <td>{formatKg(line.poidsUnitaireKg)}</td>
                        <td>{formatKg(line.poidsUnitaireKg * line.quantite)}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
              <div className="bons-total-strip">
                <span>POIDS TOTAL</span>
                <strong>{formatKg(bon.poidsTotalKg)}</strong>
              </div>
            </Card>
          </section>
          <aside>
            <Card
              title="Suivi de réception"
              subtitle="La preuve officielle est enregistrée à la clôture du transport."
            >
              {bon.statut === "LIVRE" ? (
                <div className="bons-proof">
                  <Badge>Livré</Badge>
                  <dl>
                    <div>
                      <dt>Réceptionnaire</dt>
                      <dd>{bon.recepteur}</dd>
                    </div>
                    <div>
                      <dt>Date de réception</dt>
                      <dd>{bon.dateReception}</dd>
                    </div>
                    {bon.commentaire && (
                      <div>
                        <dt>Commentaire</dt>
                        <dd>{bon.commentaire}</dd>
                      </div>
                    )}
                    <div>
                      <dt>Transport</dt>
                      <dd>{bon.transportId}</dd>
                    </div>
                  </dl>
                </div>
              ) : (
                <div className="bons-proof-pending">
                  <span>
                    <Icon name="clock" size={20} />
                  </span>
                  <strong>En attente de réception</strong>
                  <p>La preuve sera ajoutée par Transports après confirmation de la livraison.</p>
                </div>
              )}
            </Card>
            <Card className="bons-integrity-card">
              <Icon name="check" size={17} />
              <div>
                <strong>Document figé</strong>
                <p>Ce bon est une copie stable du chargement au moment de son émission.</p>
              </div>
            </Card>
          </aside>
        </div>
      )}
    </div>
  );
}
