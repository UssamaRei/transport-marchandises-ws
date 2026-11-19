import { useEffect, useState, type FormEvent, type ReactNode } from "react";
import { Link, NavLink, Outlet, useNavigate, useOutletContext, useParams } from "react-router-dom";
import {
  api,
  ApiError,
  errorOf,
  formatKg,
  shortId,
  statusLabel,
  type Chargement,
  type Resume,
  type Infos,
  type Ligne,
  type LigneInput,
  type Camion,
} from "./api";
import { useAuth } from "./auth";
import { Badge, Button, Card, Field, Icon, Notice } from "./components/ui";
import { ErrorBanner, Loading, Modal, useDirtyGuard, useToast } from "./components/live";

const tabs = [
  { slug: "", label: "Gestion du brouillon", code: "CH01" },
  { slug: "/marchandises", label: "Marchandises", code: "CH02" },
  { slug: "/poids", label: "Poids total", code: "CH03" },
  { slug: "/validation", label: "Validation", code: "CH04" },
];
function Heading({
  title,
  description,
  action,
  code,
}: {
  title: string;
  description: string;
  action?: ReactNode;
  code: string;
}) {
  return (
    <div className="welcome inner-welcome">
      <div>
        <span className="eyebrow">
          CHARGEMENTS <span className="screen-code">{code}</span>
        </span>
        <h1>
          {title}
          <span className="title-dot">.</span>
        </h1>
        <p>{description}</p>
      </div>
      {action}
    </div>
  );
}

export function ChargementsList() {
  const { canEdit } = useAuth();
  const [rows, setRows] = useState<Resume[]>([]);
  const [offset, setOffset] = useState(0);
  const [version, reload] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<ApiError | null>(null);
  const [query, setQuery] = useState("");
  const [status, setStatus] = useState("TOUS");
  useEffect(() => {
    const controller = new AbortController();
    setLoading(true);
    setError(null);
    api
      .list(offset, controller.signal)
      .then((data) => {
        if (!controller.signal.aborted) setRows(data);
      })
      .catch((error) => {
        if (!controller.signal.aborted) {
          setRows([]);
          setError(errorOf(error));
        }
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false);
      });
    return () => controller.abort();
  }, [offset, version]);
  const search = query.trim().toLocaleLowerCase("fr");
  const visible = rows.filter(
    (row) =>
      (status === "TOUS" || row.statut === status) &&
      [row.id, row.origine, row.destination].some((value) =>
        value?.toLocaleLowerCase("fr").includes(search),
      ),
  );
  return (
    <>
      <Heading
        code="CH05"
        title="Vos chargements"
        description="Des brouillons à la validation, retrouvez vos marchandises."
        action={
          canEdit && (
            <Link className="btn" to="/chargements/nouveau">
              <Icon name="plus" size={17} />
              Nouveau chargement
            </Link>
          )
        }
      />
      <ErrorBanner error={error} retry={() => reload((value) => value + 1)} />
      <Card
        title="Tous les chargements"
        subtitle="Préparez vos expéditions et suivez leur validation."
        action={
          <Button secondary disabled={loading} onClick={() => reload((value) => value + 1)}>
            <Icon name="clock" size={16} />
            Actualiser
          </Button>
        }
      >
        <div className="table-tools">
          <label className="live-search">
            <Icon name="search" size={17} />
            <input
              aria-label="Rechercher dans cette page"
              placeholder="Rechercher dans cette page…"
              value={query}
              onChange={(event) => setQuery(event.target.value)}
            />
          </label>
          <select
            aria-label="Filtrer par statut"
            value={status}
            onChange={(event) => setStatus(event.target.value)}
          >
            <option value="TOUS">Tous les statuts</option>
            <option value="BROUILLON">Brouillons</option>
            <option value="VALIDE">Validés</option>
          </select>
        </div>
        {loading ? (
          <Loading />
        ) : !error && rows.length === 0 ? (
          <div className="live-empty">
            <span className="live-empty-icon">
              <Icon name="box" size={34} />
            </span>
            <h2>{offset ? "Aucun autre chargement" : "Votre prochaine expédition commence ici"}</h2>
            <p>
              {offset
                ? "Revenez à la page précédente pour retrouver vos chargements."
                : "Créez un brouillon, ajoutez vos marchandises, puis choisissez un camion adapté."}
            </p>
            {canEdit && !offset && (
              <Link className="btn" to="/chargements/nouveau">
                <Icon name="plus" size={17} />
                Créer un chargement
              </Link>
            )}
          </div>
        ) : (
          !error && (
            <div className="table-scroll">
              <table>
                <thead>
                  <tr>
                    <th>Identifiant</th>
                    <th>Origine</th>
                    <th>Destination</th>
                    <th>Poids total</th>
                    <th>Statut</th>
                    <th>
                      <span className="sr-only">Actions</span>
                    </th>
                  </tr>
                </thead>
                <tbody>
                  {visible.map((row) => (
                    <tr key={row.id}>
                      <td>
                        <Link
                          title={row.id}
                          to={`/chargements/${row.id}`}
                          aria-label={`Ouvrir le chargement ${shortId(row.id)}`}
                        >
                          <strong>{shortId(row.id)}</strong>
                        </Link>
                      </td>
                      <td>{row.origine || "À renseigner"}</td>
                      <td>{row.destination || "À renseigner"}</td>
                      <td>{formatKg(row.poidsTotalKg)}</td>
                      <td>
                        <Badge>{statusLabel(row.statut)}</Badge>
                      </td>
                      <td>
                        <Link className="text-link" to={`/chargements/${row.id}`}>
                          Voir le détail <Icon name="chevron" size={14} />
                        </Link>
                      </td>
                    </tr>
                  ))}
                  {visible.length === 0 && (
                    <tr>
                      <td colSpan={6} className="live-no-result">
                        Aucun résultat dans cette page. Modifiez vos filtres.
                      </td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
          )
        )}
        <div className="table-footer">
          <span>
            {loading
              ? "Chargement…"
              : error
                ? "Liste indisponible"
                : `${visible.length} résultat${visible.length > 1 ? "s" : ""} affiché${visible.length > 1 ? "s" : ""} · page ${offset / 20 + 1}`}
            <small className="live-page-hint">Recherche et filtres sur cette page.</small>
          </span>
          <div className="button-group">
            <Button
              secondary
              disabled={!offset || loading}
              onClick={() => {
                setOffset((value) => Math.max(0, value - 20));
                setQuery("");
              }}
            >
              Précédent
            </Button>
            <Button
              secondary
              disabled={loading || !!error || rows.length < 20}
              onClick={() => {
                setOffset((value) => value + 20);
                setQuery("");
              }}
            >
              Suivant
            </Button>
          </div>
        </div>
      </Card>
    </>
  );
}

type DetailContext = {
  data: Chargement;
  setData: (value: Chargement) => void;
  refresh: () => Promise<void>;
};
const useDetail = () => useOutletContext<DetailContext>();
export function DetailLayout() {
  const { id = "" } = useParams();
  const [data, setData] = useState<Chargement | null>(null);
  const [error, setError] = useState<ApiError | null>(null);
  const [version, retry] = useState(0);
  useEffect(() => {
    const controller = new AbortController();
    setError(null);
    setData(null);
    api
      .detail(id, controller.signal)
      .then((value) => {
        if (!controller.signal.aborted) setData(value);
      })
      .catch((error) => {
        if (!controller.signal.aborted) setError(errorOf(error));
      });
    return () => controller.abort();
  }, [id, version]);
  const refresh = async () => {
    const updated = await api.detail(id);
    setData(updated);
  };
  return (
    <>
      <Heading
        code="CH"
        title={data?.id === id ? `Chargement ${shortId(id)}` : "Détail du chargement"}
        description={
          data?.id === id
            ? `${data.origine || "Origine à renseigner"} → ${data.destination || "Destination à renseigner"}`
            : "Retrouvez les informations de votre expédition."
        }
        action={
          <Link className="btn secondary" to="/chargements">
            Retour à la liste
          </Link>
        }
      />
      <ErrorBanner
        error={error}
        retry={error?.status === 404 ? undefined : () => retry((value) => value + 1)}
      />
      {data?.id === id ? (
        <>
          <div className="live-detail-meta">
            <span className="live-reference">
              Identifiant : <code>{data.id}</code>
            </span>
            <Badge>{statusLabel(data.statut)}</Badge>
          </div>
          <nav className="screen-tabs" aria-label="Étapes du chargement">
            {tabs.map((tab) => (
              <NavLink
                key={tab.code}
                end
                to={`/chargements/${id}${tab.slug}`}
                className={({ isActive }) => (isActive ? "active" : "")}
              >
                <small>{tab.code}</small>
                {tab.label}
              </NavLink>
            ))}
          </nav>
          <Outlet context={{ data, setData, refresh } satisfies DetailContext} />
        </>
      ) : (
        !error && <Loading text="Ouverture du chargement…" />
      )}
    </>
  );
}

const formValues = (data?: Infos) => ({
  origine: data?.origine || "",
  destination: data?.destination || "",
  destinataireNom: data?.destinataireNom || "",
  destinataireTelephone: data?.destinataireTelephone || "",
});
export function NewChargement() {
  const { canEdit } = useAuth();
  return (
    <>
      <Heading
        code="CH01"
        title="Nouveau chargement"
        description="Préparez votre expédition avant d’ajouter les marchandises."
        action={
          <Link className="btn secondary" to="/chargements">
            Retour à la liste
          </Link>
        }
      />
      {canEdit ? (
        <InfosEditor />
      ) : (
        <Notice warning>Votre compte permet uniquement la consultation.</Notice>
      )}
    </>
  );
}
export function ChargementInfos() {
  const { data, setData } = useDetail();
  return <InfosEditor key={data.id} data={data} saved={setData} />;
}
function InfosEditor({ data, saved }: { data?: Chargement; saved?: (value: Chargement) => void }) {
  const { canEdit } = useAuth();
  const navigate = useNavigate();
  const toast = useToast();
  const [form, setForm] = useState(() => formValues(data));
  const [baseline, setBaseline] = useState(() => formValues(data));
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<ApiError | null>(null);
  const [deleting, setDeleting] = useState(false);
  const readonly = !canEdit || data?.statut === "VALIDE";
  const dirty = !readonly && JSON.stringify(form) !== JSON.stringify(baseline);
  const allowNavigation = useDirtyGuard(dirty);
  async function save(continueToGoods: boolean) {
    if (busy || readonly) return;
    setBusy(true);
    setError(null);
    try {
      const payload: Infos = {
        origine: form.origine.trim() || null,
        destination: form.destination.trim() || null,
        destinataireNom: form.destinataireNom.trim() || null,
        destinataireTelephone: form.destinataireTelephone.trim() || null,
      };
      const result = data ? await api.update(data.id, payload) : await api.create(payload);
      setBaseline(formValues(result));
      setForm(formValues(result));
      saved?.(result);
      toast(data ? "Les informations ont été enregistrées." : "Votre brouillon a été créé.");
      if (!data || continueToGoods) {
        allowNavigation();
        navigate(`/chargements/${result.id}${continueToGoods ? "/marchandises" : ""}`, {
          replace: !data,
        });
      }
    } catch (error) {
      setError(errorOf(error));
    } finally {
      setBusy(false);
    }
  }
  async function remove() {
    if (!data || busy) return;
    setBusy(true);
    setError(null);
    try {
      await api.delete(data.id);
      allowNavigation();
      toast("Le brouillon et ses marchandises ont été supprimés.");
      navigate("/chargements");
    } catch (error) {
      setError(errorOf(error));
      setDeleting(false);
    } finally {
      setBusy(false);
    }
  }
  const inputs = [
    { name: "origine" as const, label: "Ville d’origine", max: 500, placeholder: "Ex. Agadir" },
    {
      name: "destination" as const,
      label: "Ville de destination",
      max: 500,
      placeholder: "Ex. Casablanca",
    },
    {
      name: "destinataireNom" as const,
      label: "Nom du destinataire",
      max: 160,
      placeholder: "Entreprise ou personne à livrer",
    },
    {
      name: "destinataireTelephone" as const,
      label: "Téléphone (facultatif)",
      max: 40,
      placeholder: "Ex. +212 6…",
    },
  ];
  return (
    <>
      <ErrorBanner error={error} />
      <Card
        title="Informations de l’expédition"
        subtitle={
          readonly
            ? "Consultez les informations enregistrées."
            : "Un brouillon peut être complété progressivement."
        }
        action={data && <Badge>{statusLabel(data.statut)}</Badge>}
      >
        <form
          onSubmit={(event) => {
            event.preventDefault();
            void save(false);
          }}
        >
          <div className="form-body">
            <div className="form-grid">
              {inputs.map((input) => (
                <Field key={input.name} label={input.label}>
                  <input
                    name={input.name}
                    type={input.name === "destinataireTelephone" ? "tel" : "text"}
                    maxLength={input.max}
                    placeholder={input.placeholder}
                    value={form[input.name]}
                    disabled={readonly || busy}
                    onChange={(event) =>
                      setForm((value) => ({ ...value, [input.name]: event.target.value }))
                    }
                    aria-invalid={!!error?.fields[input.name]}
                    aria-describedby={error?.fields[input.name] ? `error-${input.name}` : undefined}
                  />
                  {error?.fields[input.name] && (
                    <em id={`error-${input.name}`}>{error.fields[input.name]}</em>
                  )}
                </Field>
              ))}
            </div>
            {data?.statut === "VALIDE" ? (
              <Notice>
                Ce chargement est validé. Ses informations et ses marchandises ne peuvent plus être
                modifiées.
              </Notice>
            ) : !canEdit ? (
              <Notice>Votre compte dispose d’un accès en lecture seule.</Notice>
            ) : (
              <p className="live-muted">
                L’origine, la destination et le destinataire seront nécessaires pour la validation.
              </p>
            )}
          </div>
          <div className="form-footer">
            {data && !readonly ? (
              <Button
                danger
                secondary
                icon="trash"
                disabled={busy}
                onClick={() => setDeleting(true)}
              >
                Supprimer le brouillon
              </Button>
            ) : (
              <span className="live-muted">
                {dirty
                  ? "Modifications non enregistrées"
                  : data
                    ? "Informations enregistrées"
                    : "Statut initial : brouillon"}
              </span>
            )}
            <div className="button-group">
              {!readonly && (
                <Button type="submit" secondary disabled={busy || (!!data && !dirty)}>
                  {busy ? "Enregistrement…" : data ? "Enregistrer" : "Créer le brouillon"}
                </Button>
              )}
              {readonly ? (
                <Link className="btn" to={`/chargements/${data?.id}/marchandises`}>
                  Voir les marchandises <Icon name="arrow" size={16} />
                </Link>
              ) : (
                <Button
                  disabled={busy}
                  onClick={() => {
                    const formElement = document.activeElement?.closest("form");
                    if (!formElement || formElement.reportValidity()) void save(true);
                  }}
                >
                  Enregistrer et continuer <Icon name="arrow" size={16} />
                </Button>
              )}
            </div>
          </div>
        </form>
      </Card>
      {deleting && (
        <Modal title="Supprimer ce brouillon ?" close={() => setDeleting(false)} busy={busy}>
          <p>
            Le brouillon {data && shortId(data.id)} et toutes ses lignes de marchandises seront
            supprimés. Cette action est définitive.
          </p>
          <div className="live-dialog-actions">
            <Button secondary disabled={busy} onClick={() => setDeleting(false)}>
              Annuler
            </Button>
            <Button
              danger
              disabled={busy}
              onClick={() => {
                void remove();
              }}
            >
              {busy ? "Suppression…" : "Confirmer la suppression"}
            </Button>
          </div>
        </Modal>
      )}
    </>
  );
}

function GoodsTable({
  data,
  editable = false,
  edit,
  remove,
  busy = false,
}: {
  data: Chargement;
  editable?: boolean;
  edit?: (value: Ligne) => void;
  remove?: (value: Ligne) => void;
  busy?: boolean;
}) {
  return (
    <div className="table-scroll">
      <table>
        <thead>
          <tr>
            <th>Désignation</th>
            <th>Quantité</th>
            <th>Poids unitaire</th>
            <th>Poids de ligne</th>
            {editable && (
              <th>
                <span className="sr-only">Actions</span>
              </th>
            )}
          </tr>
        </thead>
        <tbody>
          {data.lignes.map((line) => (
            <tr key={line.id}>
              <td className="live-goods-name">
                <strong>{line.designation}</strong>
              </td>
              <td>{line.quantite}</td>
              <td>{formatKg(line.poidsUnitaireKg)}</td>
              <td>{formatKg((Math.round(line.poidsUnitaireKg * 1000) * line.quantite) / 1000)}</td>
              {editable && (
                <td>
                  <div className="button-group">
                    <button
                      type="button"
                      className="icon-button"
                      disabled={busy}
                      aria-label={`Modifier ${line.designation}`}
                      onClick={() => edit?.(line)}
                    >
                      <Icon name="edit" size={17} />
                    </button>
                    <button
                      type="button"
                      className="icon-button live-danger"
                      disabled={busy}
                      aria-label={`Retirer ${line.designation}`}
                      onClick={() => remove?.(line)}
                    >
                      <Icon name="trash" size={17} />
                    </button>
                  </div>
                </td>
              )}
            </tr>
          ))}
          {data.lignes.length === 0 && (
            <tr>
              <td colSpan={editable ? 5 : 4} className="live-no-result">
                Aucune marchandise. Ajoutez une première ligne à ce chargement.
              </td>
            </tr>
          )}
        </tbody>
      </table>
    </div>
  );
}
export function Marchandises() {
  const { data, setData } = useDetail();
  const { canEdit } = useAuth();
  const toast = useToast();
  const [editor, setEditor] = useState<Ligne | "new" | null>(null);
  const [removing, setRemoving] = useState<Ligne | null>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<ApiError | null>(null);
  const editable = canEdit && data.statut === "BROUILLON";
  async function remove() {
    if (!removing || busy) return;
    setBusy(true);
    setError(null);
    try {
      setData(await api.deleteLigne(data.id, removing.id));
      setRemoving(null);
      toast("La marchandise a été retirée. Le poids total est à jour.");
    } catch (error) {
      setError(errorOf(error));
      setRemoving(null);
    } finally {
      setBusy(false);
    }
  }
  return (
    <>
      <ErrorBanner error={error} />
      <Card
        title="Marchandises du chargement"
        subtitle="Ajoutez les articles et leurs quantités. Le poids total se met à jour après chaque modification."
        action={
          editable && (
            <Button icon="plus" disabled={busy} onClick={() => setEditor("new")}>
              Ajouter une marchandise
            </Button>
          )
        }
      >
        <GoodsTable
          data={data}
          editable={editable}
          edit={setEditor}
          remove={setRemoving}
          busy={busy}
        />
        <div className="live-weight-footer">
          <span>Poids total du chargement</span>
          <strong>{formatKg(data.poidsTotalKg)}</strong>
        </div>
      </Card>
      {!editable && (
        <div className="live-spaced">
          <Notice>
            {data.statut === "VALIDE"
              ? "Les marchandises d’un chargement validé sont conservées en lecture seule."
              : "Votre compte permet uniquement la consultation des marchandises."}
          </Notice>
        </div>
      )}
      <div className="live-page-actions">
        <Link className="btn secondary" to={`/chargements/${data.id}`}>
          Informations de l’expédition
        </Link>
        <Link className="btn" to={`/chargements/${data.id}/poids`}>
          Voir le poids total <Icon name="arrow" size={16} />
        </Link>
      </div>
      {editor && (
        <GoodsEditor
          data={data}
          line={editor === "new" ? undefined : editor}
          close={() => setEditor(null)}
          saved={(value) => {
            setData(value);
            setEditor(null);
            toast("La marchandise a été enregistrée. Le poids total est à jour.");
          }}
        />
      )}
      {removing && (
        <Modal title="Retirer cette marchandise ?" close={() => setRemoving(null)} busy={busy}>
          <p>La ligne « {removing.designation} » sera retirée et le poids total sera recalculé.</p>
          <div className="live-dialog-actions">
            <Button secondary disabled={busy} onClick={() => setRemoving(null)}>
              Annuler
            </Button>
            <Button
              danger
              disabled={busy}
              onClick={() => {
                void remove();
              }}
            >
              {busy ? "Suppression…" : "Retirer la marchandise"}
            </Button>
          </div>
        </Modal>
      )}
    </>
  );
}
function GoodsEditor({
  data,
  line,
  close,
  saved,
}: {
  data: Chargement;
  line?: Ligne;
  close: () => void;
  saved: (value: Chargement) => void;
}) {
  const [designation, setDesignation] = useState(line?.designation || "");
  const [quantite, setQuantite] = useState(String(line?.quantite || 1));
  const [poids, setPoids] = useState(line ? String(line.poidsUnitaireKg) : "");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<ApiError | null>(null);
  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (busy) return;
    setBusy(true);
    setError(null);
    try {
      const payload: LigneInput = {
        designation: designation.trim(),
        quantite: Number(quantite),
        poidsUnitaireKg: Number(poids),
      };
      saved(
        line
          ? await api.updateLigne(data.id, line.id, payload)
          : await api.addLigne(data.id, payload),
      );
    } catch (error) {
      setError(errorOf(error));
    } finally {
      setBusy(false);
    }
  }
  return (
    <Modal
      title={line ? "Modifier la marchandise" : "Ajouter une marchandise"}
      close={close}
      busy={busy}
    >
      <ErrorBanner error={error} />
      <form
        onSubmit={(event) => {
          void submit(event);
        }}
      >
        <Field label="Désignation">
          <input
            autoFocus
            required
            maxLength={240}
            value={designation}
            disabled={busy}
            onChange={(event) => setDesignation(event.target.value)}
            placeholder="Ex. Cartons de produits alimentaires"
          />
        </Field>
        <div className="form-grid">
          <Field label="Quantité">
            <input
              required
              type="number"
              min={1}
              max={2147483647}
              step={1}
              value={quantite}
              disabled={busy}
              onChange={(event) => setQuantite(event.target.value)}
            />
          </Field>
          <Field label="Poids unitaire (kg)" hint="Jusqu’à trois décimales.">
            <input
              required
              type="number"
              min="0.001"
              max="99999999999.999"
              step="0.001"
              value={poids}
              disabled={busy}
              onChange={(event) => setPoids(event.target.value)}
              placeholder="Ex. 12,5"
            />
          </Field>
        </div>
        <div className="live-dialog-actions">
          <Button secondary disabled={busy} onClick={close}>
            Annuler
          </Button>
          <Button type="submit" disabled={busy || !designation.trim()}>
            {busy
              ? "Enregistrement…"
              : line
                ? "Enregistrer les modifications"
                : "Ajouter la marchandise"}
          </Button>
        </div>
      </form>
    </Modal>
  );
}
export function PoidsTotal() {
  const { data } = useDetail();
  return (
    <>
      <Card
        title="Calcul du poids total"
        subtitle="Le poids de chaque ligne correspond à la quantité multipliée par le poids unitaire."
      >
        <GoodsTable data={data} />
        <div className="live-total-panel">
          <div>
            <span>POIDS TOTAL DU CHARGEMENT</span>
            <strong>{formatKg(data.poidsTotalKg)}</strong>
            <small>
              {data.lignes.length} ligne{data.lignes.length > 1 ? "s" : ""} de marchandise
              {data.lignes.length > 1 ? "s" : ""}
            </small>
          </div>
          <Icon name="box" size={48} />
        </div>
      </Card>
      <div className="live-page-actions">
        <Link className="btn secondary" to={`/chargements/${data.id}/marchandises`}>
          Retour aux marchandises
        </Link>
        <Link className="btn" to={`/chargements/${data.id}/validation`}>
          Vérifier le chargement <Icon name="arrow" size={16} />
        </Link>
      </div>
    </>
  );
}

export function Validation() {
  const { data, setData } = useDetail();
  const { canEdit } = useAuth();
  const toast = useToast();
  const [trucks, setTrucks] = useState<Camion[]>([]);
  const [selected, setSelected] = useState("");
  const [page, setPage] = useState(0);
  const [pages, setPages] = useState(0);
  const [reload, retry] = useState(0);
  const [loading, setLoading] = useState(false);
  const [truckError, setTruckError] = useState<ApiError | null>(null);
  const [error, setError] = useState<ApiError | null>(null);
  const [busy, setBusy] = useState(false);
  const [confirm, setConfirm] = useState(false);
  useEffect(() => {
    if (data.statut === "VALIDE" || !canEdit) return;
    const controller = new AbortController();
    setLoading(true);
    setTruckError(null);
    api
      .camions(page, controller.signal)
      .then((result) => {
        if (controller.signal.aborted) return;
        setTrucks((previous) =>
          page === 0
            ? result.items
            : [
                ...previous,
                ...result.items.filter(
                  (item) => !previous.some((existing) => existing.id === item.id),
                ),
              ],
        );
        setPages(result.totalPages);
      })
      .catch((error) => {
        if (!controller.signal.aborted) setTruckError(errorOf(error));
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false);
      });
    return () => controller.abort();
  }, [page, reload, data.statut, canEdit]);
  const truck = trucks.find((value) => value.id === selected);
  const conditions = [
    { text: "Origine renseignée", ok: !!data.origine?.trim() },
    { text: "Destination renseignée", ok: !!data.destination?.trim() },
    { text: "Destinataire renseigné", ok: !!data.destinataireNom?.trim() },
    { text: "Au moins une ligne de marchandise", ok: data.lignes.length > 0 },
  ];
  const fits =
    !!truck && Math.round(data.poidsTotalKg * 1000) <= Math.round(truck.capaciteKg * 1000);
  async function validate() {
    if (!truck || busy) return;
    setBusy(true);
    setError(null);
    try {
      setData(await api.validate(data.id, truck.id));
      setConfirm(false);
      toast("Le chargement a été validé.");
    } catch (error) {
      setConfirm(false);
      setError(errorOf(error));
    } finally {
      setBusy(false);
    }
  }
  return (
    <>
      <ErrorBanner error={error} />
      {data.statut === "VALIDE" ? (
        <Card
          title="Chargement validé"
          subtitle="Les marchandises et les informations de l’expédition sont maintenant figées."
          action={<Badge>Validé</Badge>}
        >
          <div className="form-body">
            <Notice>Votre chargement est prêt pour l’émission d’un bon de livraison.</Notice>
            {canEdit && (
              <Link
                className="btn workflow-next"
                to={`/bons?chargementId=${encodeURIComponent(data.id)}`}
              >
                Préparer le bon de livraison <Icon name="arrow" size={16} />
              </Link>
            )}
            <div className="live-facts">
              <div>
                <span>Trajet</span>
                <strong>
                  {data.origine} → {data.destination}
                </strong>
              </div>
              <div>
                <span>Poids total</span>
                <strong>{formatKg(data.poidsTotalKg)}</strong>
              </div>
              <div>
                <span>Destinataire</span>
                <strong>{data.destinataireNom}</strong>
              </div>
              <div>
                <span>Identifiant du camion retenu</span>
                <code>{data.camionSouhaiteId}</code>
              </div>
            </div>
          </div>
        </Card>
      ) : (
        <>
          <Card
            title="Vérifications avant validation"
            subtitle="Vérifiez vos informations et choisissez un camion adapté."
          >
            <div className="form-body">
              <ul className="live-checklist">
                {conditions.map((condition) => (
                  <li key={condition.text} className={condition.ok ? "checked" : ""}>
                    <Icon name={condition.ok ? "check" : "clock"} size={17} />
                    {condition.text}
                    <span>{condition.ok ? "Complet" : "À compléter"}</span>
                  </li>
                ))}
              </ul>
            </div>
          </Card>
          {canEdit ? (
            <div className="live-spaced">
              <Card
                title="Choisir le camion"
                subtitle={`Poids à transporter : ${formatKg(data.poidsTotalKg)}. Seuls les camions disponibles sont proposés.`}
              >
                <div className="form-body">
                  <ErrorBanner error={truckError} retry={() => retry((value) => value + 1)} />
                  <Field label="Camion disponible">
                    <select
                      value={selected}
                      disabled={busy || (loading && trucks.length === 0)}
                      onChange={(event) => setSelected(event.target.value)}
                    >
                      <option value="">
                        {loading && !trucks.length
                          ? "Chargement des camions…"
                          : "Sélectionnez un camion"}
                      </option>
                      {trucks.map((value) => (
                        <option key={value.id} value={value.id}>
                          {value.immatriculation} · {formatKg(value.capaciteKg)}
                          {Math.round(value.capaciteKg * 1000) <
                          Math.round(data.poidsTotalKg * 1000)
                            ? " · capacité insuffisante"
                            : ""}
                        </option>
                      ))}
                    </select>
                  </Field>
                  {!loading && !truckError && trucks.length === 0 && (
                    <Notice warning>
                      Aucun camion disponible pour le moment. Vous pouvez conserver votre brouillon
                      et revenir plus tard.
                    </Notice>
                  )}
                  {page + 1 < pages && (
                    <Button
                      secondary
                      disabled={loading || busy}
                      onClick={() => setPage((value) => value + 1)}
                    >
                      {loading ? "Chargement…" : "Afficher plus de camions"}
                    </Button>
                  )}
                  {truck && (
                    <div className="live-truck-summary">
                      <div>
                        <Icon name="truck" size={25} />
                        <strong>{truck.immatriculation}</strong>
                        <Badge>{fits ? "Capacité adaptée" : "Capacité insuffisante"}</Badge>
                      </div>
                      <p>
                        Capacité : {formatKg(truck.capaciteKg)} · Chargement :{" "}
                        {formatKg(data.poidsTotalKg)}
                      </p>
                      <progress
                        value={Math.min(data.poidsTotalKg, truck.capaciteKg)}
                        max={truck.capaciteKg}
                        aria-label="Utilisation de la capacité du camion"
                      />
                      {!fits && (
                        <p className="live-danger">
                          Choisissez un camion avec une capacité supérieure ou égale au poids du
                          chargement.
                        </p>
                      )}
                    </div>
                  )}
                  <p className="live-muted">
                    La disponibilité sera vérifiée à la validation. Le camion sera réservé lors de
                    la planification du transport.
                  </p>
                </div>
                <div className="form-footer">
                  <span className="live-muted">
                    La validation rend le chargement non modifiable.
                  </span>
                  <Button
                    icon="check"
                    disabled={busy || !truck || !fits || !conditions.every((value) => value.ok)}
                    onClick={() => setConfirm(true)}
                  >
                    Valider le chargement
                  </Button>
                </div>
              </Card>
            </div>
          ) : (
            <div className="live-spaced">
              <Notice>
                Votre rôle permet la consultation. La validation est réservée à un agent ou un
                administrateur.
              </Notice>
            </div>
          )}
        </>
      )}
      <div className="live-page-actions">
        <Link className="btn secondary" to={`/chargements/${data.id}/poids`}>
          Retour au poids total
        </Link>
        <Link className="text-link" to="/chargements">
          Tous les chargements <Icon name="arrow" size={15} />
        </Link>
      </div>
      {confirm && (
        <Modal title="Valider ce chargement ?" close={() => setConfirm(false)} busy={busy}>
          <p>
            Vous allez valider {formatKg(data.poidsTotalKg)} de marchandises avec le camion{" "}
            <strong>{truck?.immatriculation}</strong>. Les informations et les lignes ne pourront
            plus être modifiées.
          </p>
          <div className="live-dialog-actions">
            <Button secondary disabled={busy} onClick={() => setConfirm(false)}>
              Annuler
            </Button>
            <Button
              disabled={busy}
              onClick={() => {
                void validate();
              }}
            >
              {busy ? "Vérification en cours…" : "Confirmer la validation"}
            </Button>
          </div>
        </Modal>
      )}
    </>
  );
}
