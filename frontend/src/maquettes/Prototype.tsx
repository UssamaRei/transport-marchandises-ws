import { useEffect, useState, type ReactNode } from "react";

type IconName =
  | "grid"
  | "truck"
  | "box"
  | "file"
  | "route"
  | "search"
  | "bell"
  | "chevron"
  | "arrow"
  | "plus"
  | "check"
  | "clock"
  | "close"
  | "download"
  | "filter"
  | "calendar"
  | "dots"
  | "help"
  | "logout"
  | "alert"
  | "pin"
  | "settings"
  | "layers"
  | "edit"
  | "trash";
function Icon({
  name,
  size = 20,
  ...props
}: {
  name: IconName;
  size?: number;
  className?: string;
}) {
  const paths: Record<IconName, ReactNode> = {
    grid: (
      <>
        <rect x="3" y="3" width="7" height="7" rx="1.5" />
        <rect x="14" y="3" width="7" height="7" rx="1.5" />
        <rect x="3" y="14" width="7" height="7" rx="1.5" />
        <rect x="14" y="14" width="7" height="7" rx="1.5" />
      </>
    ),
    truck: (
      <>
        <path d="M3 6h11v11H3zM14 10h4l3 4v3h-7" />
        <circle cx="7" cy="18" r="2" />
        <circle cx="18" cy="18" r="2" />
      </>
    ),
    box: (
      <>
        <path d="m12 3 9 5-9 5-9-5 9-5ZM3 8v10l9 5 9-5V8M12 13v10M7.5 5.5l9 5" />
      </>
    ),
    file: (
      <>
        <path d="M13 3H5v18h14V9l-6-6ZM13 3v6h6M8 13h8M8 17h5" />
      </>
    ),
    route: (
      <>
        <circle cx="6" cy="5" r="2" />
        <circle cx="18" cy="19" r="2" />
        <path d="M6 7v5a3 3 0 0 0 3 3h6a3 3 0 0 0 0-6h-3M18 17v-3" />
      </>
    ),
    search: (
      <>
        <circle cx="10.5" cy="10.5" r="6.5" />
        <path d="m16 16 4 4" />
      </>
    ),
    bell: (
      <>
        <path d="M5 16h14l-2-3V9a5 5 0 0 0-10 0v4l-2 3ZM10 20h4" />
      </>
    ),
    chevron: <path d="m9 5 7 7-7 7" />,
    arrow: <path d="M4 12h15m-6-6 6 6-6 6" />,
    plus: <path d="M12 5v14M5 12h14" />,
    check: <path d="m5 12 4 4L19 6" />,
    clock: (
      <>
        <circle cx="12" cy="12" r="9" />
        <path d="M12 7v5l3 2" />
      </>
    ),
    close: <path d="m6 6 12 12M6 18 18 6" />,
    download: (
      <>
        <path d="M12 3v12m-5-5 5 5 5-5M4 16v5h16v-5" />
      </>
    ),
    filter: (
      <>
        <path d="M4 7h16M7 12h10M10 17h4" />
        <circle cx="9" cy="7" r="2" fill="currentColor" stroke="none" />
        <circle cx="15" cy="12" r="2" fill="currentColor" stroke="none" />
      </>
    ),
    calendar: (
      <>
        <rect x="3" y="5" width="18" height="16" rx="2" />
        <path d="M7 3v4M17 3v4M3 10h18M7 14h3M14 14h3M7 18h3" />
      </>
    ),
    dots: (
      <>
        <circle cx="5" cy="12" r="1" />
        <circle cx="12" cy="12" r="1" />
        <circle cx="19" cy="12" r="1" />
      </>
    ),
    help: (
      <>
        <circle cx="12" cy="12" r="9" />
        <path d="M9 9a3 3 0 0 1 6 0c0 2-3 2-3 5M12 17v.1" />
      </>
    ),
    logout: (
      <>
        <path d="M10 4H4v16h6M9 12h12m-5-5 5 5-5 5" />
      </>
    ),
    alert: (
      <>
        <path d="m12 3 10 18H2L12 3ZM12 9v5M12 17v.1" />
      </>
    ),
    pin: (
      <>
        <path d="M19 10c0 5-7 11-7 11S5 15 5 10a7 7 0 1 1 14 0Z" />
        <circle cx="12" cy="10" r="2" />
      </>
    ),
    settings: (
      <>
        <circle cx="12" cy="12" r="4" />
        <path d="m9 3-1 3-3 1-2 3 2 3v4l4 1 3 3 3-3 4-1v-4l2-3-2-3-3-1-1-3H9Z" />
      </>
    ),
    layers: (
      <>
        <path d="m12 3 10 5-10 5L2 8l10-5ZM3 13l9 5 9-5M3 18l9 5 9-5" />
      </>
    ),
    edit: (
      <>
        <path d="m4 16 12-12 4 4L8 20H4v-4ZM13 7l4 4" />
      </>
    ),
    trash: (
      <>
        <path d="M3 6h18M9 6V3h6v3M5 6l1 15h12l1-15M10 10v7M14 10v7" />
      </>
    ),
  };
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.65"
      strokeLinecap="round"
      strokeLinejoin="round"
      {...props}
    >
      {paths[name]}
    </svg>
  );
}

const groups = [
  {
    prefix: "G",
    label: "Vue d’ensemble",
    icon: "grid" as IconName,
    screens: [
      "Tableau de bord",
      "Vue globale des expéditions",
      "Dossier d’une expédition",
      "Recherche globale",
      "Éléments à traiter",
    ],
  },
  {
    prefix: "CA",
    label: "Camions",
    icon: "truck" as IconName,
    screens: [
      "Consultation du parc",
      "Ajout et modification",
      "État et maintenance",
      "Réservation d’un camion",
      "Gestion des réservations",
    ],
  },
  {
    prefix: "CH",
    label: "Chargements",
    icon: "box" as IconName,
    screens: [
      "Gestion du brouillon",
      "Gestion des marchandises",
      "Calcul du poids total",
      "Validation du chargement",
      "Consultation des chargements",
    ],
  },
  {
    prefix: "BL",
    label: "Bons de livraison",
    icon: "file" as IconName,
    screens: [
      "Liste des bons",
      "Émission d’un bon",
      "Détail d’un bon",
      "Aperçu du document",
      "Confirmation de réception",
    ],
  },
  {
    prefix: "TR",
    label: "Suivi des transports",
    icon: "route" as IconName,
    screens: [
      "Planification d’un transport",
      "Consultation des transports",
      "Déclaration du départ",
      "Événements de suivi",
      "Arrivée et livraison",
    ],
  },
];
const initialTransports = [
  {
    ref: "TR-2024-048",
    from: "Agadir",
    to: "Casablanca",
    plate: "45872-A-33",
    driver: "Youssef Amrani",
    initials: "YA",
    date: "18 juin, 08:30",
    status: "En cours",
    weight: "8 500",
    recipient: "Atlas Distribution",
  },
  {
    ref: "TR-2024-047",
    from: "Marrakech",
    to: "Agadir",
    plate: "32156-B-26",
    driver: "Karim Benali",
    initials: "KB",
    date: "18 juin, 10:00",
    status: "Planifié",
    weight: "4 200",
    recipient: "Souss Négoce",
  },
  {
    ref: "TR-2024-046",
    from: "Casablanca",
    to: "Marrakech",
    plate: "78943-A-33",
    driver: "Omar El Fassi",
    initials: "OE",
    date: "18 juin, 07:00",
    status: "En cours",
    weight: "6 750",
    recipient: "Ménara Logistique",
  },
  {
    ref: "TR-2024-045",
    from: "Agadir",
    to: "Marrakech",
    plate: "12389-D-26",
    driver: "Hassan Idrissi",
    initials: "HI",
    date: "17 juin, 14:00",
    status: "Livré",
    weight: "3 600",
    recipient: "Marchés du Sud",
  },
  {
    ref: "TR-2024-044",
    from: "Casablanca",
    to: "Agadir",
    plate: "56721-B-33",
    driver: "Mehdi Alami",
    initials: "MA",
    date: "17 juin, 09:30",
    status: "Livré",
    weight: "9 100",
    recipient: "Agadir Primeurs",
  },
];
const initialTrucks = [
  {
    plate: "45872-A-33",
    model: "Volvo FH 460",
    capacity: 12000,
    state: "En service",
    availability: "Réservé",
  },
  {
    plate: "32156-B-26",
    model: "Renault D Wide",
    capacity: 7500,
    state: "En service",
    availability: "Disponible",
  },
  {
    plate: "78943-A-33",
    model: "Mercedes Actros",
    capacity: 10000,
    state: "En service",
    availability: "Réservé",
  },
  {
    plate: "12389-D-26",
    model: "Volvo FL 280",
    capacity: 6000,
    state: "En service",
    availability: "Disponible",
  },
  {
    plate: "56721-B-33",
    model: "Renault T 480",
    capacity: 15000,
    state: "Maintenance",
    availability: "Indisponible",
  },
];
function Badge({ children }: { children: ReactNode }) {
  const s = String(children);
  return (
    <span
      className={`badge ${/Livré|Disponible|Validé|Confirmée|En service|Reçu|Libérée/.test(s) ? "green" : /cours|Émis|Réservé/.test(s) ? "blue" : /Planifié|Brouillon|attente/.test(s) ? "amber" : /Maintenance|Indisponible|Annulé/.test(s) ? "red" : "neutral"}`}
    >
      <i />
      {children}
    </span>
  );
}
function Button({
  children,
  onClick,
  secondary = false,
  danger = false,
  disabled = false,
  icon,
}: {
  children: ReactNode;
  onClick?: () => void;
  secondary?: boolean;
  danger?: boolean;
  disabled?: boolean;
  icon?: IconName;
}) {
  return (
    <button
      className={`btn ${secondary ? "secondary" : ""} ${danger ? "danger" : ""}`}
      onClick={onClick}
      disabled={disabled}
    >
      {icon && <Icon name={icon} size={17} />}
      {children}
    </button>
  );
}
function Card({
  title,
  subtitle,
  action,
  children,
  className = "",
}: {
  title?: string;
  subtitle?: string;
  action?: ReactNode;
  children: ReactNode;
  className?: string;
}) {
  return (
    <section className={`card ${className}`}>
      {title && (
        <div className="card-heading">
          <div>
            <h2>{title}</h2>
            {subtitle && <p>{subtitle}</p>}
          </div>
          {action}
        </div>
      )}
      {children}
    </section>
  );
}
function Field({ label, children, hint }: { label: string; children: ReactNode; hint?: string }) {
  return (
    <label className="field">
      <span>{label}</span>
      {children}
      {hint && <small>{hint}</small>}
    </label>
  );
}
function Notice({ children, warning = false }: { children: ReactNode; warning?: boolean }) {
  return (
    <div className={`notice ${warning ? "warning" : ""}`}>
      <Icon name={warning ? "alert" : "check"} size={18} />
      <div>{children}</div>
    </div>
  );
}
function App() {
  const [screen, setScreen] = useState("G01");
  const [expanded, setExpanded] = useState("");
  const [gallery, setGallery] = useState(false);
  const [notifications, setNotifications] = useState(false);
  const [toast, setToast] = useState("");
  const [query, setQuery] = useState("");
  const [filter, setFilter] = useState("Tous les statuts");
  const [period, setPeriod] = useState("Cette semaine");
  const [confirm, setConfirm] = useState<{
    title: string;
    text: string;
    action: () => void;
  } | null>(null);
  const [detail, setDetail] = useState<string | null>(null);
  const [trucks, setTrucks] = useState(initialTrucks);
  const [selectedTruck, setSelectedTruck] = useState("32156-B-26");
  const [plate, setPlate] = useState("");
  const [capacity, setCapacity] = useState("");
  const [truckState, setTruckState] = useState("En service");
  const [errors, setErrors] = useState(false);
  const [origin, setOrigin] = useState("Agadir");
  const [destination, setDestination] = useState("Casablanca");
  const [recipient, setRecipient] = useState("Atlas Distribution");
  const [phone, setPhone] = useState("+212 661 234 567");
  const [goods, setGoods] = useState([
    { name: "Cartons de produits alimentaires", qty: 10, weight: 12.5 },
    { name: "Caisses de pièces détachées", qty: 5, weight: 20 },
  ]);
  const [goodEditor, setGoodEditor] = useState<number | null>(null);
  const [goodName, setGoodName] = useState("");
  const [goodQty, setGoodQty] = useState("1");
  const [goodWeight, setGoodWeight] = useState("");
  const [loadSaved, setLoadSaved] = useState(true);
  const [validated, setValidated] = useState(false);
  const [issued, setIssued] = useState(false);
  const [reserved, setReserved] = useState(false);
  const [reservationTruck, setReservationTruck] = useState("");
  const [departed, setDeparted] = useState(false);
  const [planned, setPlanned] = useState(false);
  const [cancelled, setCancelled] = useState(false);
  const [arrived, setArrived] = useState(false);
  const [received, setReceived] = useState(false);
  const [released, setReleased] = useState(false);
  const [receiver, setReceiver] = useState("Mohamed Bennani");
  const [receiveDate, setReceiveDate] = useState("2024-06-18");
  const [departDate, setDepartDate] = useState("2024-06-19T08:30");
  const [eventType, setEventType] = useState("Point de passage");
  const [eventText, setEventText] = useState("");
  const [eventDate, setEventDate] = useState("2024-06-18T11:45");
  const [events, setEvents] = useState([
    {
      title: "Départ d’Agadir",
      text: "Prise en charge des marchandises par Youssef Amrani.",
      date: "18 juin 2024 · 08:30",
    },
    {
      title: "Réservation confirmée",
      text: "Le camion 45872-A-33 a été affecté au transport.",
      date: "17 juin 2024 · 16:15",
    },
  ]);
  const group = groups.find(
    (g) => screen.startsWith(g.prefix) && (g.prefix !== "G" || screen.startsWith("G")),
  )!;
  const screenIndex = Number(screen.slice(group.prefix.length)) - 1;
  const title = group.screens[screenIndex];
  const total = goods.reduce((sum, item) => sum + item.qty * item.weight, 0);
  const truck = trucks.find((t) => t.plate === selectedTruck)!;
  const transportPlate = reservationTruck || selectedTruck;
  const transportTruck = trucks.find((t) => t.plate === transportPlate)!;
  const delivered = arrived && received && released;
  const fmt = (n: number) => n.toLocaleString("fr-FR");
  const notify = (text: string) => {
    setToast(text);
    window.setTimeout(() => setToast(""), 4500);
  };
  const go = (id: string) => {
    setScreen(id);
    setQuery("");
    setFilter("Tous les statuts");
    setGallery(false);
    setDetail(null);
    setErrors(false);
    window.scrollTo(0, 0);
  };
  const openTruck = (p: string) => {
    setSelectedTruck(p);
    go("CA03");
  };
  const reserve = () => {
    if (truck.availability !== "Disponible" || reserved || total > truck.capacity) {
      notify("Réservation impossible : vérifiez la disponibilité et la capacité.");
      return;
    }
    setReserved(true);
    setReservationTruck(selectedTruck);
    setReleased(false);
    setTrucks(
      trucks.map((t) => (t.plate === selectedTruck ? { ...t, availability: "Réservé" } : t)),
    );
    notify("Réservation du camion confirmée.");
  };
  const release = () => {
    setReleased(true);
    setReserved(false);
    setTrucks(
      trucks.map((t) => (t.plate === transportPlate ? { ...t, availability: "Disponible" } : t)),
    );
    notify("Le camion a été libéré.");
    setConfirm(null);
  };
  const saveTruck = () => {
    if (
      !plate.trim() ||
      Number(capacity) <= 0 ||
      trucks.some((t) => t.plate === plate && t.plate !== detail)
    ) {
      setErrors(true);
      return;
    }
    if (detail && trucks.find((t) => t.plate === detail)?.availability === "Réservé") {
      notify("Un véhicule réservé ne peut pas être modifié pendant son affectation.");
      return;
    }
    const data = {
      plate: plate.toUpperCase(),
      model: detail
        ? trucks.find((t) => t.plate === detail)?.model || "Camion porteur"
        : "Camion porteur",
      capacity: Number(capacity),
      state: truckState,
      availability: truckState === "Maintenance" ? "Indisponible" : "Disponible",
    };
    setTrucks(detail ? trucks.map((t) => (t.plate === detail ? data : t)) : [...trucks, data]);
    if (detail === selectedTruck) setSelectedTruck(data.plate);
    if (detail === reservationTruck) setReservationTruck(data.plate);
    go("CA01");
    notify("Le camion a été enregistré.");
  };
  const download = () => {
    const content = `BON DE LIVRAISON BL-2024-049\nChargement CH-2024-049\nOrigine : ${origin}\nDestination : ${destination}\nDestinataire : ${recipient}\n\n${goods.map((g) => `${g.name} | ${g.qty} × ${g.weight} kg | ${g.qty * g.weight} kg`).join("\n")}\n\nPoids total : ${total} kg\nRéceptionnaire : ${received ? receiver : "________________"}`;
    const url = URL.createObjectURL(new Blob([content], { type: "text/plain;charset=utf-8" }));
    const a = document.createElement("a");
    a.href = url;
    a.download = "BL-2024-049.txt";
    a.click();
    URL.revokeObjectURL(url);
    notify("Le récapitulatif du bon a été téléchargé.");
  };
  const editGood = (index: number) => {
    const g = goods[index];
    setGoodName(g?.name || "");
    setGoodQty(String(g?.qty || 1));
    setGoodWeight(g ? String(g.weight) : "");
    setGoodEditor(index);
  };
  useEffect(() => {
    const handleKey = (event: KeyboardEvent) => {
      if ((event.metaKey || event.ctrlKey) && event.key.toLowerCase() === "k") {
        event.preventDefault();
        go("G04");
      }
      if (event.key === "Escape") {
        setGallery(false);
        setNotifications(false);
        setConfirm(null);
        setGoodEditor(null);
        setDetail(null);
      }
    };
    window.addEventListener("keydown", handleKey);
    return () => window.removeEventListener("keydown", handleKey);
  }, []);
  const goodsTable = (editable = false) => (
    <>
      <div className="table-scroll">
        <table>
          <thead>
            <tr>
              <th>Désignation</th>
              <th>Quantité</th>
              <th>Poids unitaire</th>
              <th>Poids de ligne</th>
              {editable && <th />}
            </tr>
          </thead>
          <tbody>
            {goods.map((g, i) => (
              <tr key={i}>
                <td>
                  <span className="item-icon">
                    <Icon name="box" size={17} />
                  </span>
                  <strong>{g.name}</strong>
                </td>
                <td>{g.qty}</td>
                <td>{fmt(g.weight)} kg</td>
                <td>
                  <strong>{fmt(g.qty * g.weight)} kg</strong>
                </td>
                {editable && (
                  <td>
                    <div className="row-actions">
                      <button title="Modifier" disabled={validated} onClick={() => editGood(i)}>
                        <Icon name="edit" size={17} />
                      </button>
                      <button
                        title="Retirer"
                        disabled={validated}
                        onClick={() => setGoods(goods.filter((_, n) => i !== n))}
                      >
                        <Icon name="trash" size={17} />
                      </button>
                    </div>
                  </td>
                )}
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <div className="weight-total">
        <span>Poids total du chargement</span>
        <strong>
          {fmt(total)} <small>kg</small>
        </strong>
      </div>
    </>
  );
  const journey = (
    <div className="journey">
      <div>
        <span className="route-dot" />
        <small>ORIGINE</small>
        <strong>{origin}</strong>
        <p>Zone industrielle Tassila</p>
      </div>
      <div className="journey-line">
        <Icon name="truck" size={24} />
        <span>462 km · environ 5 h 30</span>
      </div>
      <div>
        <span className="route-dot end" />
        <small>DESTINATION</small>
        <strong>{destination}</strong>
        <p>Zone logistique Aïn Sebaâ</p>
      </div>
    </div>
  );
  const timeline = (
    <div className="timeline">
      {events.map((e, i) => (
        <div className="timeline-item" key={i}>
          <span className={i === 0 ? "current" : ""}>
            <Icon name={i === 0 ? "pin" : "check"} size={14} />
          </span>
          <div>
            <small>{e.date}</small>
            <h3>{e.title}</h3>
            <p>{e.text}</p>
          </div>
        </div>
      ))}
    </div>
  );
  const transportRows = initialTransports.filter(
    (t) =>
      `${t.ref} ${t.from} ${t.to} ${t.plate}`.toLowerCase().includes(query.toLowerCase()) &&
      (filter === "Tous les statuts" || t.status === filter),
  );
  const transportTable = (
    <div className="table-scroll">
      <table className="transport-table">
        <thead>
          <tr>
            <th>RÉFÉRENCE</th>
            <th>TRAJET</th>
            <th>CAMION / CHAUFFEUR</th>
            <th>DÉPART PRÉVU</th>
            <th>STATUT</th>
            <th />
          </tr>
        </thead>
        <tbody>
          {transportRows.map((t) => (
            <tr key={t.ref} onClick={() => setDetail(t.ref)} className="clickable">
              <td>
                <strong className="ref">{t.ref}</strong>
                <small>{t.weight} kg</small>
              </td>
              <td>
                <span className="table-route">
                  <i />
                  {t.from}
                </span>
                <span className="table-route destination">
                  <i />
                  {t.to}
                </span>
              </td>
              <td>
                <span className="truck-plate">{t.plate}</span>
                <small>{t.driver}</small>
              </td>
              <td className="date-cell">
                {t.date}
                <small>2024</small>
              </td>
              <td>
                <Badge>{t.status}</Badge>
              </td>
              <td>
                <button
                  className="icon-button"
                  aria-label={`Détail ${t.ref}`}
                  onClick={(e) => {
                    e.stopPropagation();
                    setDetail(t.ref);
                  }}
                >
                  <Icon name="chevron" size={17} />
                </button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
      {transportRows.length === 0 && (
        <div className="empty">Aucun transport ne correspond à votre recherche.</div>
      )}
    </div>
  );
  const tableTools = (
    <div className="table-tools">
      <label className="search-input">
        <Icon name="search" size={17} />
        <input
          placeholder="Rechercher une référence, un trajet…"
          value={query}
          onChange={(e) => setQuery(e.target.value)}
        />
      </label>
      <div className="select-wrap">
        <Icon name="filter" size={16} />
        <select
          aria-label="Filtrer par statut"
          value={filter}
          onChange={(e) => setFilter(e.target.value)}
        >
          {["Tous les statuts", "En cours", "Planifié", "Livré"].map((s) => (
            <option key={s}>{s}</option>
          ))}
        </select>
      </div>
    </div>
  );
  const activities = [
    {
      icon: "check" as IconName,
      title: "Livraison confirmée",
      text: "TR-2024-045 · Agadir → Marrakech",
      time: "Il y a 12 min",
      color: "green",
    },
    {
      icon: "route" as IconName,
      title: "Un transport a pris le départ",
      text: "TR-2024-048 · Youssef Amrani",
      time: "Il y a 35 min",
      color: "blue",
    },
    {
      icon: "file" as IconName,
      title: "Bon de livraison émis",
      text: "BL-2024-052 · Atlas Distribution",
      time: "Il y a 1 h",
      color: "purple",
    },
    {
      icon: "box" as IconName,
      title: "Chargement à valider",
      text: "CH-2024-049 · 225 kg",
      time: "Il y a 2 h",
      color: "amber",
    },
  ];
  const dashboard = (
    <>
      <div className="welcome">
        <div>
          <span className="eyebrow">VOTRE ACTIVITÉ, EN UN COUP D’ŒIL</span>
          <h1>
            Tableau de bord<span className="title-dot">.</span>
          </h1>
          <p>Bonjour Amine, voici ce qui se passe sur votre réseau aujourd’hui.</p>
        </div>
        <div className="page-actions">
          <label className="date-selector">
            <Icon name="calendar" size={17} />
            <select value={period} onChange={(e) => setPeriod(e.target.value)}>
              <option>Cette semaine</option>
              <option>Ce mois-ci</option>
              <option>Aujourd’hui</option>
            </select>
            <Icon name="chevron" size={13} />
          </label>
          <Button icon="plus" onClick={() => go("TR01")}>
            Nouveau transport
          </Button>
        </div>
      </div>
      <div className="metric-grid">
        {[
          {
            label: "Camions disponibles",
            value: trucks.filter((t) => t.availability === "Disponible").length + 10,
            sub: "sur 24 camions dans votre parc",
            change: "+2",
            text: "depuis hier",
            icon: "truck" as IconName,
            color: "teal",
            screen: "CA01",
            bars: [25, 45, 35, 55, 45, 70, 56, 78, 65, 90],
          },
          {
            label: "Chargements à valider",
            value: validated ? "07" : "08",
            sub: "prêts pour la prochaine étape",
            change: "3",
            text: "à traiter en priorité",
            icon: "box" as IconName,
            color: "orange",
            screen: "CH05",
            bars: [35, 55, 40, 65, 50, 75, 55, 62, 42, 52],
          },
          {
            label: "Transports en cours",
            value: departed ? 19 : 18,
            sub: "sur les routes du Maroc",
            change: "+4",
            text: "cette semaine",
            icon: "route" as IconName,
            color: "blue",
            screen: "TR02",
            bars: [25, 38, 30, 48, 45, 60, 50, 76, 64, 88],
          },
          {
            label: "Livraisons terminées",
            value:
              (period === "Ce mois-ci" ? 486 : period === "Aujourd’hui" ? 16 : 124) +
              (delivered ? 1 : 0),
            sub:
              period === "Ce mois-ci"
                ? "depuis le début du mois"
                : period === "Aujourd’hui"
                  ? "sur la journée en cours"
                  : "sur la période sélectionnée",
            change: "+12,8 %",
            text: "vs période précédente",
            icon: "check" as IconName,
            color: "green",
            screen: "G02",
            bars: [20, 30, 45, 38, 55, 60, 52, 78, 68, 92],
          },
        ].map((m) => (
          <button className={`metric-card ${m.color}`} key={m.label} onClick={() => go(m.screen)}>
            <div className="metric-top">
              <span>{m.label}</span>
              <span className="metric-icon">
                <Icon name={m.icon} size={20} />
              </span>
            </div>
            <div className="metric-middle">
              <strong>{m.value}</strong>
              <div className="spark-bars">
                {m.bars.map((b, i) => (
                  <i key={i} style={{ height: `${b}%` }} />
                ))}
              </div>
            </div>
            <p>{m.sub}</p>
            <div className="metric-bottom">
              <b>
                {m.color !== "orange" && <span>↗ </span>}
                {m.change}
              </b>
              <span>{m.text}</span>
            </div>
          </button>
        ))}
      </div>
      <div className="dashboard-columns">
        <div className="dashboard-main">
          <Card
            title="Aperçu des transports"
            subtitle="L’évolution de vos expéditions au fil de la semaine."
            action={
              <span className="chart-key">
                <i />
                Transports <b>Cette semaine</b>
              </span>
            }
          >
            <div className="chart">
              <div className="chart-axis">
                <span>30</span>
                <span>20</span>
                <span>10</span>
                <span>0</span>
              </div>
              <div className="chart-body">
                <div className="chart-grid">
                  <i />
                  <i />
                  <i />
                  <i />
                </div>
                <div className="bar-columns">
                  {["Lun", "Mar", "Mer", "Jeu", "Ven", "Sam", "Dim"].map((day, i) => (
                    <div className={`bar-column ${i === 3 ? "active" : ""}`} key={day}>
                      <div className="bar-pair">
                        <div style={{ height: `${[46, 65, 54, 81, 68, 36, 24][i]}%` }} />
                        <div style={{ height: `${[29, 44, 35, 57, 48, 23, 14][i]}%` }} />
                        {i === 3 && (
                          <div className="chart-tooltip">
                            <strong>
                              {period === "Aujourd’hui"
                                ? "16"
                                : period === "Ce mois-ci"
                                  ? "486"
                                  : "24"}{" "}
                              transports
                            </strong>
                            <span>Jeudi 20 juin</span>
                          </div>
                        )}
                      </div>
                      <span>{day}</span>
                    </div>
                  ))}
                </div>
              </div>
            </div>
            <div className="chart-footer">
              <span>
                <i className="legend-dot teal" />
                Livrés
              </span>
              <span>
                <i className="legend-dot pale" />
                En cours
              </span>
              <p>
                <span className="up">↗ 18,6 %</span> par rapport à la semaine dernière
              </p>
            </div>
          </Card>
        </div>
        <Card
          title="Activité récente"
          action={<span className="live-indicator">En direct</span>}
          className="activity-card"
        >
          <div className="activity-list">
            {activities.map((a, i) => (
              <button
                className="activity"
                key={a.title}
                onClick={() => go(["TR05", "TR03", "BL03", "CH04"][i])}
              >
                <span className={`activity-icon ${a.color}`}>
                  <Icon name={a.icon} size={17} />
                </span>
                <div>
                  <h3>{a.title}</h3>
                  <p>{a.text}</p>
                  <small>{a.time}</small>
                </div>
              </button>
            ))}
          </div>
          <button className="card-bottom-link" onClick={() => go("G05")}>
            Voir toute l’activité <Icon name="arrow" size={16} />
          </button>
        </Card>
      </div>
      <Card
        title="Transports récents"
        subtitle="Gardez un œil sur vos dernières opérations."
        action={
          <button className="text-link" onClick={() => go("TR02")}>
            Tous les transports <Icon name="arrow" size={16} />
          </button>
        }
      >
        {transportTable}
        <div className="table-footer">
          <span>Affichage de 5 transports sur 48</span>
          <div className="table-summary">
            <i />
            Données mises à jour à l’instant
          </div>
        </div>
      </Card>
      <div className="bottom-banner">
        <span className="banner-icon">
          <Icon name="alert" size={20} />
        </span>
        <div>
          <strong>Un réseau fluide commence par les bonnes actions.</strong>
          <p>
            3 éléments nécessitent votre attention pour garder vos opérations sur la bonne voie.
          </p>
        </div>
        <button onClick={() => go("G05")}>
          Voir les éléments à traiter <Icon name="arrow" size={17} />
        </button>
      </div>
    </>
  );
  function renderScreen() {
    if (screen === "G01") return dashboard;
    switch (screen) {
      case "CA01":
        return (
          <Card
            title="Votre parc de camions"
            subtitle={`${trucks.length} véhicules · Capacité et disponibilité en temps réel`}
            action={
              <Button
                icon="plus"
                onClick={() => {
                  setPlate("");
                  setCapacity("");
                  go("CA02");
                }}
              >
                Ajouter un camion
              </Button>
            }
          >
            <div className="table-tools">
              <label className="search-input">
                <Icon name="search" size={17} />
                <input
                  placeholder="Rechercher une immatriculation…"
                  value={query}
                  onChange={(e) => setQuery(e.target.value)}
                />
              </label>
              <select value={filter} onChange={(e) => setFilter(e.target.value)}>
                <option>Tous les statuts</option>
                <option>Disponible</option>
                <option>Réservé</option>
                <option>Indisponible</option>
              </select>
            </div>
            <div className="table-scroll">
              <table>
                <thead>
                  <tr>
                    <th>Immatriculation</th>
                    <th>Modèle</th>
                    <th>Capacité</th>
                    <th>État</th>
                    <th>Disponibilité</th>
                    <th />
                  </tr>
                </thead>
                <tbody>
                  {trucks
                    .filter(
                      (t) =>
                        t.plate.toLowerCase().includes(query.toLowerCase()) &&
                        (filter === "Tous les statuts" || filter === t.availability),
                    )
                    .map((t) => (
                      <tr key={t.plate}>
                        <td>
                          <strong className="ref">{t.plate}</strong>
                        </td>
                        <td>{t.model}</td>
                        <td>{fmt(t.capacity)} kg</td>
                        <td>
                          <Badge>{t.state}</Badge>
                        </td>
                        <td>
                          <Badge>{t.availability}</Badge>
                        </td>
                        <td>
                          <button className="text-link" onClick={() => openTruck(t.plate)}>
                            Voir le détail <Icon name="chevron" size={14} />
                          </button>
                        </td>
                      </tr>
                    ))}
                </tbody>
              </table>
            </div>
          </Card>
        );
      case "CA02":
        return (
          <div className="form-layout">
            <Card
              title="Informations du camion"
              subtitle="Renseignez les informations de votre véhicule."
            >
              <div className="form-body">
                <Field label="Immatriculation *">
                  <input
                    placeholder="Ex. 45872-A-33"
                    value={plate}
                    onChange={(e) => setPlate(e.target.value)}
                  />
                  {errors &&
                    (!plate || trucks.some((t) => t.plate === plate && t.plate !== detail)) && (
                      <em>Renseignez une immatriculation unique.</em>
                    )}
                </Field>
                <Field label="Capacité maximale (kg) *">
                  <input
                    type="number"
                    min="1"
                    placeholder="Ex. 12 000"
                    value={capacity}
                    onChange={(e) => setCapacity(e.target.value)}
                  />
                  {errors && Number(capacity) <= 0 && (
                    <em>La capacité doit être supérieure à zéro.</em>
                  )}
                </Field>
                <Field label="État du véhicule">
                  <select value={truckState} onChange={(e) => setTruckState(e.target.value)}>
                    <option>En service</option>
                    <option>Maintenance</option>
                  </select>
                </Field>
              </div>
              <div className="form-footer">
                <Button secondary onClick={() => go("CA01")}>
                  Annuler
                </Button>
                <Button icon="check" onClick={saveTruck}>
                  Enregistrer le camion
                </Button>
              </div>
            </Card>
            <div>
              <Notice>
                La disponibilité d’un camion est automatiquement déterminée par son état et ses
                réservations actives.
              </Notice>
            </div>
          </div>
        );
      case "CA03":
        return (
          <>
            <Card
              title={truck.plate}
              subtitle={truck.model}
              action={
                <Button
                  secondary
                  icon="edit"
                  onClick={() => {
                    setPlate(truck.plate);
                    setCapacity(String(truck.capacity));
                    setTruckState(truck.state);
                    go("CA02");
                    setDetail(truck.plate);
                  }}
                >
                  Modifier la fiche
                </Button>
              }
            >
              <div className="truck-hero">
                <div className="truck-illustration">
                  <Icon name="truck" size={100} />
                </div>
                <div className="info-grid">
                  <div>
                    <small>CAPACITÉ MAXIMALE</small>
                    <strong>{fmt(truck.capacity)} kg</strong>
                  </div>
                  <div>
                    <small>ÉTAT DU VÉHICULE</small>
                    <Badge>{truck.state}</Badge>
                  </div>
                  <div>
                    <small>DISPONIBILITÉ</small>
                    <Badge>{truck.availability}</Badge>
                  </div>
                  <div>
                    <small>DERNIÈRE RÉVISION</small>
                    <strong>02 juin 2024</strong>
                  </div>
                </div>
              </div>
              <div className="form-footer">
                <Button secondary onClick={() => go("CA01")}>
                  Retour au parc
                </Button>
                <Button
                  disabled={truck.availability === "Réservé"}
                  onClick={() => {
                    setTrucks(
                      trucks.map((t) =>
                        t.plate === selectedTruck
                          ? {
                              ...t,
                              state: t.state === "Maintenance" ? "En service" : "Maintenance",
                              availability:
                                t.state === "Maintenance" ? "Disponible" : "Indisponible",
                            }
                          : t,
                      ),
                    );
                    notify("L’état du véhicule a été mis à jour.");
                  }}
                >
                  {truck.state === "Maintenance" ? "Remettre en service" : "Mettre en maintenance"}
                </Button>
              </div>
            </Card>
            {truck.availability === "Réservé" && (
              <Notice warning>
                Un transport actif ou une réservation empêche la mise en maintenance. Libérez
                d’abord le camion à la fin de son transport.
              </Notice>
            )}
          </>
        );
      case "CA04":
        return (
          <Card
            title="Affecter un camion"
            subtitle="Une seule réservation active est autorisée par véhicule."
          >
            <div className="form-body">
              <Field label="Transport">
                <select>
                  <option>TR-2024-049 · Agadir → Casablanca</option>
                </select>
              </Field>
              <Field label="Camion">
                <select value={selectedTruck} onChange={(e) => setSelectedTruck(e.target.value)}>
                  {trucks.map((t) => (
                    <option key={t.plate} value={t.plate}>
                      {t.plate} — {t.availability} — {fmt(t.capacity)} kg
                    </option>
                  ))}
                </select>
              </Field>
              <div className="summary-strip">
                <div>
                  <small>POIDS PRÉVU</small>
                  <strong>{fmt(total)} kg</strong>
                </div>
                <div>
                  <small>CAPACITÉ</small>
                  <strong>{fmt(truck.capacity)} kg</strong>
                </div>
                <Badge>{reserved ? "Confirmée" : truck.availability}</Badge>
              </div>
              {(truck.availability !== "Disponible" || reserved || total > truck.capacity) && (
                <Notice warning>
                  {reserved
                    ? "Une réservation active existe déjà pour ce transport."
                    : total > truck.capacity
                      ? "La capacité du camion est insuffisante."
                      : "Ce camion est indisponible ou possède déjà une réservation active."}
                </Notice>
              )}
            </div>
            <div className="form-footer">
              <Button secondary onClick={() => go("CA05")}>
                Voir les réservations
              </Button>
              <Button
                disabled={truck.availability !== "Disponible" || reserved || total > truck.capacity}
                onClick={reserve}
              >
                Confirmer la réservation
              </Button>
            </div>
          </Card>
        );
      case "CA05":
        return (
          <Card
            title="Réservations des camions"
            subtitle="Consultez les affectations actives et l’historique."
            action={
              <Button icon="plus" onClick={() => go("CA04")}>
                Nouvelle réservation
              </Button>
            }
          >
            <table>
              <thead>
                <tr>
                  <th>Camion</th>
                  <th>Transport</th>
                  <th>Trajet</th>
                  <th>Statut</th>
                  <th />
                </tr>
              </thead>
              <tbody>
                <tr>
                  <td>
                    <strong>{transportPlate}</strong>
                  </td>
                  <td>TR-2024-049</td>
                  <td>
                    {origin} → {destination}
                  </td>
                  <td>
                    <Badge>{released ? "Libérée" : reserved ? "Confirmée" : "En attente"}</Badge>
                  </td>
                  <td>
                    <Button
                      secondary
                      disabled={!reserved || (departed && !arrived)}
                      onClick={() =>
                        setConfirm({
                          title: "Libérer ce camion ?",
                          text: "Le véhicule redeviendra disponible pour une nouvelle réservation.",
                          action: release,
                        })
                      }
                    >
                      Libérer le camion
                    </Button>
                  </td>
                </tr>
                <tr>
                  <td>45872-A-33</td>
                  <td>TR-2024-048</td>
                  <td>Agadir → Casablanca</td>
                  <td>
                    <Badge>Confirmée</Badge>
                  </td>
                  <td>
                    <span className="muted">Transport en cours</span>
                  </td>
                </tr>
                <tr>
                  <td>12389-D-26</td>
                  <td>TR-2024-045</td>
                  <td>Agadir → Marrakech</td>
                  <td>
                    <Badge>Libérée</Badge>
                  </td>
                  <td>17 juin 2024</td>
                </tr>
              </tbody>
            </table>
            {departed && !arrived && (
              <div className="form-body">
                <Notice warning>Le camion ne peut pas être libéré avant son arrivée.</Notice>
              </div>
            )}
          </Card>
        );
      case "CH01":
        return (
          <Card
            title="Chargement CH-2024-049"
            subtitle="Préparez votre expédition avant d’ajouter les marchandises."
            action={<Badge>{validated ? "Validé" : "Brouillon"}</Badge>}
          >
            <div className="form-body">
              <div className="form-grid">
                <Field label="Ville d’origine *">
                  <input
                    disabled={validated}
                    value={origin}
                    onChange={(e) => setOrigin(e.target.value)}
                  />
                </Field>
                <Field label="Ville de destination *">
                  <input
                    disabled={validated}
                    value={destination}
                    onChange={(e) => setDestination(e.target.value)}
                  />
                </Field>
                <Field label="Nom du destinataire *">
                  <input
                    disabled={validated}
                    value={recipient}
                    onChange={(e) => setRecipient(e.target.value)}
                  />
                </Field>
                <Field label="Téléphone (facultatif)">
                  <input
                    disabled={validated}
                    type="tel"
                    value={phone}
                    onChange={(e) => setPhone(e.target.value)}
                  />
                </Field>
              </div>
              {validated && (
                <Notice>Ce chargement est validé. Il ne peut plus être modifié ni supprimé.</Notice>
              )}
              {errors && (
                <Notice warning>
                  Veuillez renseigner l’origine, la destination et le destinataire.
                </Notice>
              )}
            </div>
            <div className="form-footer">
              <Button
                danger
                secondary
                disabled={validated || !loadSaved}
                icon="trash"
                onClick={() =>
                  setConfirm({
                    title: "Supprimer le brouillon ?",
                    text: "Cette action supprimera les informations et les marchandises de ce brouillon.",
                    action: () => {
                      setOrigin("");
                      setDestination("");
                      setRecipient("");
                      setPhone("");
                      setGoods([]);
                      setLoadSaved(false);
                      setConfirm(null);
                      notify("Le brouillon a été supprimé.");
                    },
                  })
                }
              >
                Supprimer
              </Button>
              <div className="button-group">
                <Button
                  secondary
                  disabled={validated}
                  onClick={() => {
                    if (!origin || !destination || !recipient) {
                      setErrors(true);
                      return;
                    }
                    setLoadSaved(true);
                    notify("Le brouillon a été enregistré.");
                  }}
                >
                  {loadSaved ? "Enregistrer" : "Créer"}
                </Button>
                <Button
                  onClick={() => {
                    if (!origin || !destination || !recipient) {
                      setErrors(true);
                      return;
                    }
                    setLoadSaved(true);
                    go("CH02");
                  }}
                >
                  Marchandises <Icon name="arrow" size={16} />
                </Button>
              </div>
            </div>
          </Card>
        );
      case "CH02":
        return (
          <>
            <Card
              title="Marchandises du chargement"
              subtitle="CH-2024-049 · Agadir → Casablanca"
              action={
                <Button icon="plus" disabled={validated} onClick={() => editGood(goods.length)}>
                  Ajouter une marchandise
                </Button>
              }
            >
              {goodsTable(true)}
            </Card>
            {validated && (
              <Notice>Chargement validé : les marchandises sont en lecture seule.</Notice>
            )}
            <div className="standalone-actions">
              <Button secondary onClick={() => go("CH01")}>
                Retour au brouillon
              </Button>
              <Button onClick={() => go("CH03")}>
                Vérifier le poids <Icon name="arrow" size={16} />
              </Button>
            </div>
          </>
        );
      case "CH03":
        return (
          <>
            <Card
              title="Un poids calculé en toute transparence"
              subtitle="Le poids total est automatiquement calculé depuis les marchandises."
            >
              <div className="calculation">
                {goods.map((g, i) => (
                  <div className="calculation-line" key={i}>
                    <span className="calc-icon">
                      <Icon name="box" size={26} />
                    </span>
                    <div>
                      <h3>{g.name}</h3>
                      <p>
                        {g.qty} {i === 0 ? "cartons" : "caisses"} × {fmt(g.weight)} kg
                      </p>
                    </div>
                    <span>=</span>
                    <strong>
                      {fmt(g.qty * g.weight)} <small>kg</small>
                    </strong>
                  </div>
                ))}
                <div className="calculation-total">
                  <div>
                    <small>POIDS TOTAL DU CHARGEMENT</small>
                    <p>
                      <Icon name="check" size={16} />
                      Calcul automatique · Lecture seule
                    </p>
                  </div>
                  <strong>
                    {fmt(total)} <span>kg</span>
                  </strong>
                </div>
              </div>
            </Card>
            <div className="standalone-actions">
              <Button secondary onClick={() => go("CH02")}>
                Modifier les marchandises
              </Button>
              <Button onClick={() => go("CH04")}>
                Passer à la validation <Icon name="arrow" size={16} />
              </Button>
            </div>
          </>
        );
      case "CH04":
        return (
          <>
            <Card
              title="Résumé du chargement"
              action={<Badge>{validated ? "Validé" : "Brouillon"}</Badge>}
            >
              {journey}
              {goodsTable()}
              <div className="form-body">
                <Field label="Camion envisagé">
                  <select
                    disabled={validated}
                    value={selectedTruck}
                    onChange={(e) => setSelectedTruck(e.target.value)}
                  >
                    {trucks.map((t) => (
                      <option key={t.plate} value={t.plate}>
                        {t.plate} · {fmt(t.capacity)} kg · {t.availability} · {t.state}
                      </option>
                    ))}
                  </select>
                </Field>
                {truck.availability !== "Disponible" && !validated && (
                  <Notice warning>
                    Le camion sélectionné est indisponible. Sélectionnez un autre véhicule.
                  </Notice>
                )}
                {total > truck.capacity && (
                  <Notice warning>
                    Le poids dépasse la capacité du camion de {fmt(total - truck.capacity)} kg.
                  </Notice>
                )}
                <Notice>
                  La validation verrouille le chargement, mais ne réserve pas le camion. La
                  réservation se fait lors de la planification.
                </Notice>
                {(!origin.trim() || !destination.trim() || !recipient.trim() || !goods.length) && (
                  <Notice warning>
                    Complétez l’origine, la destination, le destinataire et au moins une marchandise
                    avant de valider.
                  </Notice>
                )}
              </div>
              <div className="form-footer">
                <Button secondary onClick={() => go("CH02")}>
                  Consulter les marchandises
                </Button>
                <Button
                  disabled={
                    validated ||
                    truck.availability !== "Disponible" ||
                    total > truck.capacity ||
                    !goods.length ||
                    !origin.trim() ||
                    !destination.trim() ||
                    !recipient.trim()
                  }
                  onClick={() => {
                    setValidated(true);
                    notify("Chargement validé. Vous pouvez maintenant émettre le bon.");
                  }}
                >
                  {validated ? "Chargement validé" : "Valider le chargement"}
                </Button>
              </div>
            </Card>
            {validated && (
              <div className="standalone-actions">
                <span />
                <Button onClick={() => go("BL02")}>
                  Émettre le bon de livraison <Icon name="arrow" size={16} />
                </Button>
              </div>
            )}
          </>
        );
      case "CH05":
        return (
          <Card
            title="Tous les chargements"
            subtitle="Des brouillons à la validation, retrouvez vos marchandises."
            action={
              <Button icon="plus" onClick={() => go("CH01")}>
                Ouvrir le brouillon
              </Button>
            }
          >
            <table>
              <thead>
                <tr>
                  <th>Identifiant</th>
                  <th>Origine</th>
                  <th>Destination</th>
                  <th>Poids</th>
                  <th>Statut</th>
                  <th />
                </tr>
              </thead>
              <tbody>
                {loadSaved && (
                  <tr>
                    <td>
                      <strong className="ref">CH-2024-049</strong>
                    </td>
                    <td>{origin}</td>
                    <td>{destination}</td>
                    <td>{fmt(total)} kg</td>
                    <td>
                      <Badge>{validated ? "Validé" : "Brouillon"}</Badge>
                    </td>
                    <td>
                      <button className="text-link" onClick={() => setDetail("CH-2024-049")}>
                        Voir le détail <Icon name="chevron" size={14} />
                      </button>
                    </td>
                  </tr>
                )}
                {initialTransports.slice(0, 4).map((t, i) => (
                  <tr key={t.ref}>
                    <td>
                      <strong className="ref">CH-2024-0{48 - i}</strong>
                    </td>
                    <td>{t.from}</td>
                    <td>{t.to}</td>
                    <td>{t.weight} kg</td>
                    <td>
                      <Badge>Validé</Badge>
                    </td>
                    <td>
                      <button className="text-link" onClick={() => setDetail(`CH-2024-0${48 - i}`)}>
                        Voir le détail <Icon name="chevron" size={14} />
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </Card>
        );
      case "BL01":
        return (
          <Card
            title="Bons de livraison"
            subtitle="Les documents qui accompagnent vos expéditions."
            action={
              <Button icon="plus" onClick={() => go("BL02")}>
                Émettre un bon
              </Button>
            }
          >
            <div className="table-tools">
              <label className="search-input">
                <Icon name="search" size={17} />
                <input
                  value={query}
                  onChange={(e) => setQuery(e.target.value)}
                  placeholder="Rechercher un bon ou un destinataire…"
                />
              </label>
            </div>
            <table>
              <thead>
                <tr>
                  <th>Numéro du bon</th>
                  <th>Chargement associé</th>
                  <th>Destinataire</th>
                  <th>Statut</th>
                  <th />
                </tr>
              </thead>
              <tbody>
                {[
                  ...(issued
                    ? [
                        {
                          ref: "BL-2024-049",
                          load: "CH-2024-049",
                          recipient,
                          status: received ? "Reçu" : "Émis",
                        },
                      ]
                    : []),
                  ...initialTransports.map((t, i) => ({
                    ref: `BL-2024-0${48 - i}`,
                    load: `CH-2024-0${48 - i}`,
                    recipient: t.recipient,
                    status: t.status === "Livré" ? "Reçu" : "Émis",
                  })),
                ]
                  .filter((t) =>
                    `${t.ref} ${t.recipient}`.toLowerCase().includes(query.toLowerCase()),
                  )
                  .map((t) => (
                    <tr key={t.ref}>
                      <td>
                        <strong className="ref">{t.ref}</strong>
                      </td>
                      <td>{t.load}</td>
                      <td>{t.recipient}</td>
                      <td>
                        <Badge>{t.status}</Badge>
                      </td>
                      <td>
                        <button
                          className="text-link"
                          onClick={() => {
                            if (t.ref === "BL-2024-049") go("BL03");
                            else setDetail(t.ref);
                          }}
                        >
                          Consulter <Icon name="chevron" size={14} />
                        </button>
                      </td>
                    </tr>
                  ))}
              </tbody>
            </table>
          </Card>
        );
      case "BL02":
        return (
          <Card
            title="Émettre un bon de livraison"
            subtitle="Un bon unique pour chaque chargement validé."
          >
            <div className="form-body">
              <Field label="Chargement">
                <select>
                  <option>
                    CH-2024-049 · {origin} → {destination} · {validated ? "Validé" : "Brouillon"}
                  </option>
                </select>
              </Field>
              {!validated && (
                <Notice warning>
                  Ce chargement n’est pas encore validé.{" "}
                  <button className="inline-link" onClick={() => go("CH04")}>
                    Valider le chargement
                  </button>
                </Notice>
              )}
              {issued && (
                <Notice warning>
                  Le bon BL-2024-049 existe déjà pour ce chargement. Une seconde émission est
                  impossible.
                </Notice>
              )}
            </div>
            {journey}
            {goodsTable()}
            <div className="form-body">
              <p>
                Destinataire : <strong>{recipient}</strong> · {phone}
              </p>
            </div>
            <div className="form-footer">
              <Button secondary onClick={() => go("BL01")}>
                Retour à la liste
              </Button>
              <Button
                disabled={!validated || issued}
                onClick={() => {
                  setIssued(true);
                  notify("Le bon BL-2024-049 a été émis.");
                  go("BL03");
                }}
              >
                Émettre le bon
              </Button>
            </div>
          </Card>
        );
      case "BL03":
        return (
          <>
            <Card
              title="BL-2024-049"
              subtitle={`Chargement CH-2024-049 · Émis le 18 juin 2024`}
              action={<Badge>{received ? "Reçu" : issued ? "Émis" : "Aperçu"}</Badge>}
            >
              {journey}
              <div className="form-body">
                <div className="info-grid">
                  <div>
                    <small>DESTINATAIRE</small>
                    <strong>{recipient}</strong>
                  </div>
                  <div>
                    <small>TÉLÉPHONE</small>
                    <strong>{phone || "Non renseigné"}</strong>
                  </div>
                </div>
              </div>
              {goodsTable()}
            </Card>
            <Notice>Les informations de ce bon sont présentées en lecture seule.</Notice>
            <div className="standalone-actions">
              <Button secondary icon="file" onClick={() => go("BL04")}>
                Aperçu du document
              </Button>
              <Button disabled={!issued} onClick={() => go("BL05")}>
                Confirmer la réception
              </Button>
            </div>
          </>
        );
      case "BL04":
        return (
          <>
            <div className="document-toolbar">
              <span>
                <Icon name="file" size={18} />
                BL-2024-049 · Format A4
              </span>
              <div className="button-group">
                <Button secondary onClick={() => window.print()}>
                  Imprimer
                </Button>
                <Button icon="download" onClick={download}>
                  Télécharger
                </Button>
              </div>
            </div>
            <div className="paper">
              <div className="paper-header">
                <div className="brand paper-brand">
                  <span className="logo-mark">
                    <Icon name="route" size={24} />
                  </span>
                  transit<span className="brand-dot">.</span>
                </div>
                <div>
                  <h2>BON DE LIVRAISON</h2>
                  <p>N° BL-2024-049</p>
                  <small>Émis le 18 juin 2024</small>
                </div>
              </div>
              <div className="paper-addresses">
                <div>
                  <small>EXPÉDITEUR</small>
                  <h3>Transit Logistique</h3>
                  <p>
                    Zone industrielle Tassila
                    <br />
                    80000 {origin}, Maroc
                  </p>
                </div>
                <div>
                  <small>DESTINATAIRE</small>
                  <h3>{recipient}</h3>
                  <p>
                    Zone logistique Aïn Sebaâ
                    <br />
                    20000 {destination}, Maroc
                    <br />
                    {phone}
                  </p>
                </div>
              </div>
              <div className="paper-reference">
                Référence chargement : <strong>CH-2024-049</strong>
              </div>
              {goodsTable()}
              <div className="signature">
                <div>
                  <h3>Réception des marchandises</h3>
                  <p>
                    Nom :{" "}
                    {received
                      ? receiver
                      : "................................................................"}
                  </p>
                  <p>
                    Date :{" "}
                    {received
                      ? receiveDate
                      : ".................................................................."}
                  </p>
                </div>
                <div>
                  <small>SIGNATURE ET CACHET</small>
                  <div />
                </div>
              </div>
              <div className="paper-bottom">
                Transit Logistique · Agadir, Maroc<span>1 / 1</span>
              </div>
            </div>
          </>
        );
      case "BL05":
        return (
          <Card
            title="Preuve de réception"
            subtitle="BL-2024-049 · Atlas Distribution"
            action={<Badge>{received ? "Reçu" : "En attente"}</Badge>}
          >
            <div className="form-body">
              {!issued && (
                <Notice warning>
                  Émettez d’abord le bon de livraison depuis un chargement validé.
                </Notice>
              )}
              <div className="form-grid">
                <Field label="Nom du réceptionnaire *">
                  <input
                    disabled={received}
                    value={receiver}
                    onChange={(e) => setReceiver(e.target.value)}
                  />
                </Field>
                <Field label="Date de réception *">
                  <input
                    disabled={received}
                    type="date"
                    value={receiveDate}
                    onChange={(e) => setReceiveDate(e.target.value)}
                  />
                </Field>
              </div>
              {received && (
                <Notice>
                  Réception confirmée par <strong>{receiver}</strong> le{" "}
                  {new Date(receiveDate + "T12:00").toLocaleDateString("fr-FR")}. Cette preuve est
                  associée au bon BL-2024-049.
                </Notice>
              )}
            </div>
            <div className="form-footer">
              <Button secondary onClick={() => go("BL03")}>
                Consulter le bon
              </Button>
              <Button
                disabled={received || !issued || !receiver.trim() || !receiveDate}
                onClick={() => {
                  setReceived(true);
                  notify("La preuve de réception a été enregistrée.");
                }}
              >
                {received ? "Réception confirmée" : "Confirmer la réception"}
              </Button>
            </div>
          </Card>
        );
      case "TR01":
        return (
          <>
            <Card
              title="Préparer le prochain départ"
              subtitle="Associez un bon de livraison et un camion à votre transport."
            >
              <div className="form-body">
                <div className="form-grid">
                  <Field label="Bon de livraison">
                    <select>
                      <option>BL-2024-049 · {issued ? "Émis" : "À émettre"}</option>
                    </select>
                  </Field>
                  <Field label="Date de départ prévue *">
                    <input
                      type="datetime-local"
                      disabled={planned}
                      value={departDate}
                      onChange={(e) => setDepartDate(e.target.value)}
                    />
                  </Field>
                </div>
                <Field label="Camion à réserver">
                  <select
                    disabled={planned || reserved}
                    value={transportPlate}
                    onChange={(e) => setSelectedTruck(e.target.value)}
                  >
                    {trucks.map((t) => (
                      <option key={t.plate} value={t.plate}>
                        {t.plate} · {fmt(t.capacity)} kg · {t.availability}
                      </option>
                    ))}
                  </select>
                </Field>
                {!issued && (
                  <Notice warning>
                    Un bon émis est nécessaire.{" "}
                    <button className="inline-link" onClick={() => go(validated ? "BL02" : "CH04")}>
                      {validated ? "Émettre le bon" : "Valider le chargement"}
                    </button>
                  </Notice>
                )}
                {(total > transportTruck.capacity ||
                  (transportTruck.availability !== "Disponible" && !reserved)) && (
                  <Notice warning>
                    Camion indisponible ou capacité insuffisante. Choisissez un autre véhicule.
                  </Notice>
                )}
              </div>
              {journey}
              <div className="summary-strip">
                <div>
                  <small>POIDS DU CHARGEMENT</small>
                  <strong>{fmt(total)} kg</strong>
                </div>
                <div>
                  <small>CAPACITÉ DU CAMION</small>
                  <strong>{fmt(transportTruck.capacity)} kg</strong>
                </div>
                {planned && !cancelled && <Badge>Confirmée</Badge>}
              </div>
              {planned && !cancelled && (
                <div className="form-body">
                  <Notice>
                    Transport TR-2024-049 planifié. La réservation du camion {transportPlate} est
                    confirmée.
                  </Notice>
                </div>
              )}
              {cancelled && (
                <div className="form-body">
                  <Notice warning>Ce transport a été annulé et son camion libéré.</Notice>
                </div>
              )}
              <div className="form-footer">
                <Button
                  secondary
                  danger
                  disabled={!planned || departed || cancelled}
                  onClick={() =>
                    setConfirm({
                      title: "Annuler ce transport ?",
                      text: "Le transport sera annulé et la réservation du camion sera libérée.",
                      action: () => {
                        setCancelled(true);
                        setPlanned(false);
                        release();
                        setReservationTruck("");
                        notify("Transport annulé et camion libéré.");
                      },
                    })
                  }
                >
                  Annuler le transport
                </Button>
                <Button
                  disabled={
                    planned ||
                    !issued ||
                    !departDate ||
                    total > transportTruck.capacity ||
                    (transportTruck.availability !== "Disponible" && !reserved)
                  }
                  onClick={() => {
                    if (!reserved) reserve();
                    setPlanned(true);
                    setCancelled(false);
                    notify("Transport planifié et réservation confirmée.");
                  }}
                >
                  Planifier le transport
                </Button>
              </div>
            </Card>
            {planned && !cancelled && (
              <div className="standalone-actions">
                <span />
                <Button onClick={() => go("TR03")}>
                  Préparer le départ <Icon name="arrow" size={16} />
                </Button>
              </div>
            )}
          </>
        );
      case "TR02":
        return (
          <Card
            title="Tous les transports"
            subtitle="Suivez les opérations, de la planification à la livraison."
            action={
              <Button icon="plus" onClick={() => go("TR01")}>
                Planifier un transport
              </Button>
            }
          >
            {tableTools}
            {planned && (
              <div className="new-transport" onClick={() => go("TR03")}>
                <strong>TR-2024-049</strong>
                <span>
                  {origin} → {destination}
                </span>
                <span>{selectedTruck}</span>
                <Badge>{delivered ? "Livré" : departed ? "En cours" : "Planifié"}</Badge>
                <Icon name="chevron" size={16} />
              </div>
            )}
            {transportTable}
            <div className="table-footer">{transportRows.length} transports affichés</div>
          </Card>
        );
      case "TR03":
        return (
          <>
            <Card
              title="Départ du transport TR-2024-049"
              action={<Badge>{departed ? "En cours" : cancelled ? "Annulé" : "Planifié"}</Badge>}
            >
              {journey}
              <div className="form-body">
                <div className="info-grid">
                  <div>
                    <small>CAMION</small>
                    <strong>{transportPlate}</strong>
                  </div>
                  <div>
                    <small>RÉSERVATION</small>
                    <Badge>{reserved ? "Confirmée" : "En attente"}</Badge>
                  </div>
                  <div>
                    <small>POIDS EMBARQUÉ</small>
                    <strong>{fmt(total)} kg</strong>
                  </div>
                  <div>
                    <small>DÉPART PRÉVU</small>
                    <strong>{new Date(departDate).toLocaleString("fr-FR")}</strong>
                  </div>
                </div>
                {(!reserved || !planned) && (
                  <Notice warning>
                    Le départ nécessite un transport planifié et une réservation confirmée.{" "}
                    <button className="inline-link" onClick={() => go("TR01")}>
                      Planifier et réserver un camion
                    </button>
                  </Notice>
                )}
                {departed && (
                  <Notice>Le départ a été déclaré. Votre transport est maintenant en cours.</Notice>
                )}
              </div>
              <div className="form-footer">
                <Button secondary disabled={departed} onClick={() => go("TR01")}>
                  Modifier la planification
                </Button>
                <Button
                  disabled={!reserved || !planned || departed || cancelled}
                  onClick={() => {
                    setDeparted(true);
                    setEvents([
                      {
                        title: `Départ de ${origin}`,
                        text: `Départ confirmé du camion ${transportPlate}.`,
                        date: "19 juin 2024 · 08:30",
                      },
                      ...events,
                    ]);
                    notify("Départ déclaré. Bonne route !");
                  }}
                >
                  Déclarer le départ
                </Button>
              </div>
            </Card>
            <div className="standalone-actions">
              <span />
              <Button secondary onClick={() => go("TR04")}>
                Consulter le suivi <Icon name="arrow" size={16} />
              </Button>
            </div>
          </>
        );
      case "TR04":
        return (
          <div className="form-layout">
            <Card title="Ajouter un événement" subtitle="TR-2024-049 · Journal du transport">
              <div className="form-body">
                <Field label="Type d’événement">
                  <select value={eventType} onChange={(e) => setEventType(e.target.value)}>
                    <option>Point de passage</option>
                    <option>Pause du chauffeur</option>
                    <option>Retard signalé</option>
                    <option>Incident de trajet</option>
                  </select>
                </Field>
                <Field label="Description *">
                  <textarea
                    rows={4}
                    value={eventText}
                    onChange={(e) => setEventText(e.target.value)}
                    placeholder="Ex. Passage à proximité de Marrakech, circulation fluide."
                  />
                </Field>
                <Field label="Date et heure *">
                  <input
                    type="datetime-local"
                    value={eventDate}
                    onChange={(e) => setEventDate(e.target.value)}
                  />
                </Field>
                {!departed && (
                  <Notice warning>
                    Déclarez le départ avant d’ajouter des événements à ce transport.
                  </Notice>
                )}
              </div>
              <div className="form-footer">
                <Button secondary onClick={() => go("TR03")}>
                  Voir le départ
                </Button>
                <Button
                  disabled={!departed || !eventText.trim() || !eventDate || arrived}
                  icon="plus"
                  onClick={() => {
                    setEvents([
                      {
                        title: eventType,
                        text: eventText,
                        date: new Date(eventDate).toLocaleString("fr-FR"),
                      },
                      ...events,
                    ]);
                    setEventText("");
                    notify("Événement ajouté à la chronologie.");
                  }}
                >
                  Ajouter l’événement
                </Button>
              </div>
            </Card>
            <Card title="Chronologie du transport">
              {timeline}
              <div className="form-body">
                <Button secondary onClick={() => go("TR05")}>
                  Déclarer l’arrivée <Icon name="arrow" size={16} />
                </Button>
              </div>
            </Card>
          </div>
        );
      case "TR05":
        return (
          <>
            <Card
              title="Dernière étape : la livraison"
              subtitle="TR-2024-049 · Chaque étape est confirmée séparément."
              action={<Badge>{delivered ? "Livré" : departed ? "En cours" : "Planifié"}</Badge>}
            >
              {journey}
              <div className="delivery-steps">
                <div>
                  <span className={arrived ? "done" : ""}>
                    {arrived ? <Icon name="check" size={20} /> : "1"}
                  </span>
                  <section>
                    <h3>Arrivée à destination</h3>
                    <p>Confirmez l’arrivée du camion à {destination}.</p>
                  </section>
                  <Button
                    secondary
                    disabled={!departed || arrived}
                    onClick={() => {
                      setArrived(true);
                      notify("L’arrivée à destination a été enregistrée.");
                    }}
                  >
                    {arrived ? "Arrivée confirmée" : "Confirmer l’arrivée"}
                  </Button>
                </div>
                <div>
                  <span className={received ? "done" : ""}>
                    {received ? <Icon name="check" size={20} /> : "2"}
                  </span>
                  <section>
                    <h3>Preuve de réception</h3>
                    <div className="form-grid">
                      <Field label="Réceptionnaire">
                        <input
                          disabled={received}
                          value={receiver}
                          onChange={(e) => setReceiver(e.target.value)}
                        />
                      </Field>
                      <Field label="Date de réception">
                        <input
                          disabled={received}
                          type="date"
                          value={receiveDate}
                          onChange={(e) => setReceiveDate(e.target.value)}
                        />
                      </Field>
                    </div>
                  </section>
                  <Button
                    secondary
                    disabled={!arrived || received || !receiver.trim() || !receiveDate}
                    onClick={() => {
                      setReceived(true);
                      notify("Preuve de réception enregistrée.");
                    }}
                  >
                    {received ? "Preuve enregistrée" : "Confirmer la réception"}
                  </Button>
                </div>
                <div>
                  <span className={released ? "done" : ""}>
                    {released ? <Icon name="check" size={20} /> : "3"}
                  </span>
                  <section>
                    <h3>Libération du camion</h3>
                    <p>{transportPlate} sera à nouveau disponible dans votre parc.</p>
                  </section>
                  <Button
                    secondary
                    disabled={!arrived || !received || released}
                    onClick={() =>
                      setConfirm({
                        title: "Libérer le camion ?",
                        text: "La livraison sera terminée et le camion disponible pour une nouvelle mission.",
                        action: release,
                      })
                    }
                  >
                    {released ? "Camion libéré" : "Libérer le camion"}
                  </Button>
                </div>
              </div>
            </Card>
            {delivered ? (
              <Notice>
                Transport LIVRÉ. Arrivée confirmée, preuve de réception enregistrée et camion
                libéré.
              </Notice>
            ) : (
              <Notice warning>
                La livraison finale nécessite l’arrivée, une preuve de réception et la libération du
                camion.
              </Notice>
            )}
          </>
        );
      case "G02":
        return (
          <Card
            title="Toutes vos expéditions, une seule vue"
            subtitle="Accédez aux documents et véhicules liés à chaque opération."
          >
            <table>
              <thead>
                <tr>
                  <th>Expédition / trajet</th>
                  <th>Chargement</th>
                  <th>Bon de livraison</th>
                  <th>Camion</th>
                  <th>Transport</th>
                </tr>
              </thead>
              <tbody>
                <tr>
                  <td>
                    <button className="text-link" onClick={() => go("G03")}>
                      EXP-2024-049
                    </button>
                    <small>
                      {origin} → {destination}
                    </small>
                  </td>
                  <td>
                    <button className="inline-link" onClick={() => go("CH05")}>
                      CH-2024-049
                    </button>
                  </td>
                  <td>
                    <button className="inline-link" onClick={() => go(issued ? "BL03" : "BL02")}>
                      {issued ? "BL-2024-049" : "À émettre"}
                    </button>
                  </td>
                  <td>
                    <button className="inline-link" onClick={() => openTruck(selectedTruck)}>
                      {selectedTruck}
                    </button>
                  </td>
                  <td>
                    <button className="inline-link" onClick={() => go(planned ? "TR03" : "TR01")}>
                      {planned ? "TR-2024-049" : "À planifier"}
                    </button>
                  </td>
                </tr>
                {initialTransports.map((t, i) => (
                  <tr key={t.ref}>
                    <td>
                      <button className="text-link" onClick={() => setDetail(t.ref)}>
                        EXP-2024-0{48 - i}
                      </button>
                      <small>
                        {t.from} → {t.to}
                      </small>
                    </td>
                    <td>
                      <button
                        className="inline-link"
                        onClick={() => setDetail(`CH-2024-0${48 - i}`)}
                      >
                        CH-2024-0{48 - i}
                      </button>
                    </td>
                    <td>
                      <button
                        className="inline-link"
                        onClick={() => setDetail(`BL-2024-0${48 - i}`)}
                      >
                        BL-2024-0{48 - i}
                      </button>
                    </td>
                    <td>
                      <button className="inline-link" onClick={() => openTruck(t.plate)}>
                        {t.plate}
                      </button>
                    </td>
                    <td>
                      <button className="inline-link" onClick={() => setDetail(t.ref)}>
                        {t.ref}
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </Card>
        );
      case "G03":
        return (
          <>
            <Card
              title="Expédition EXP-2024-049"
              subtitle={`${recipient} · ${origin} → ${destination}`}
              action={
                <Badge>{delivered ? "Livré" : departed ? "En cours" : "En préparation"}</Badge>
              }
            >
              <div className="progress-steps">
                {["Préparation", "Validation", "Bon émis", "Transport", "Livraison"].map((s, i) => (
                  <div
                    key={s}
                    className={
                      i === 0 ||
                      (i === 1 && validated) ||
                      (i === 2 && issued) ||
                      (i === 3 && departed) ||
                      (i === 4 && delivered)
                        ? "complete"
                        : ""
                    }
                  >
                    <span>
                      <Icon name={i === 4 ? "pin" : "check"} size={18} />
                    </span>
                    <strong>{s}</strong>
                  </div>
                ))}
              </div>
            </Card>
            <div className="dossier-grid">
              {[
                {
                  title: "Chargement",
                  ref: "CH-2024-049",
                  icon: "box" as IconName,
                  status: validated ? "Validé" : "Brouillon",
                  info: `${goods.length} marchandises · ${fmt(total)} kg`,
                  to: "CH05",
                },
                {
                  title: "Camion",
                  ref: transportPlate,
                  icon: "truck" as IconName,
                  status: reserved ? "Réservé" : transportTruck.availability,
                  info: `${transportTruck.model} · ${fmt(transportTruck.capacity)} kg`,
                  to: "CA03",
                },
                {
                  title: "Bon de livraison",
                  ref: issued ? "BL-2024-049" : "À émettre",
                  icon: "file" as IconName,
                  status: received ? "Reçu" : issued ? "Émis" : "En attente",
                  info: recipient,
                  to: issued ? "BL03" : "BL02",
                },
                {
                  title: "Transport",
                  ref: planned ? "TR-2024-049" : "À planifier",
                  icon: "route" as IconName,
                  status: delivered ? "Livré" : departed ? "En cours" : "Planifié",
                  info: `${origin} → ${destination}`,
                  to: planned ? "TR03" : "TR01",
                },
              ].map((item) => (
                <Card
                  title={item.title}
                  key={item.title}
                  action={<Icon name={item.icon} size={23} />}
                >
                  <div className="dossier-content">
                    <h2>{item.ref}</h2>
                    <p>{item.info}</p>
                    <Badge>{item.status}</Badge>
                    <button className="text-link" onClick={() => go(item.to)}>
                      Ouvrir le dossier <Icon name="arrow" size={16} />
                    </button>
                  </div>
                </Card>
              ))}
            </div>
          </>
        );
      case "G04":
        return (
          <>
            <div className="global-search">
              <Icon name="search" size={24} />
              <input
                autoFocus
                value={query}
                onChange={(e) => setQuery(e.target.value)}
                placeholder="Une immatriculation, un bon, un transport…"
              />
              <kbd>Entrée</kbd>
            </div>
            <p className="search-description">
              Retrouvez un élément dans l’ensemble de votre réseau.
            </p>
            <div className="search-results">
              {groups.slice(1).map((g) => {
                const items =
                  g.prefix === "CA"
                    ? trucks.map((t) => ({
                        ref: t.plate,
                        text: `${t.model} · ${fmt(t.capacity)} kg`,
                        to: "CA03",
                      }))
                    : initialTransports.map((t, i) => ({
                        ref: `${g.prefix}-2024-0${48 - i}`,
                        text: `${t.from} → ${t.to} · ${t.recipient}`,
                        to: `${g.prefix === "CH" ? "CH05" : g.prefix === "BL" ? "BL03" : "TR02"}`,
                      }));
                const results = items.filter((i) =>
                  `${i.ref} ${i.text}`.toLowerCase().includes(query.toLowerCase()),
                );
                return (
                  <Card title={g.label} subtitle={`${results.length} résultat(s)`} key={g.prefix}>
                    {results.length ? (
                      results.slice(0, 3).map((i) => (
                        <button
                          className="search-result"
                          key={i.ref}
                          onClick={() => (g.prefix === "CA" ? openTruck(i.ref) : setDetail(i.ref))}
                        >
                          <Icon name={g.icon} size={21} />
                          <span>
                            <strong>{i.ref}</strong>
                            <small>{i.text}</small>
                          </span>
                          <Icon name="arrow" size={17} />
                        </button>
                      ))
                    ) : (
                      <p className="empty">Aucun résultat dans ce module.</p>
                    )}
                  </Card>
                );
              })}
            </div>
          </>
        );
      case "G05":
        return (
          <Card
            title="Votre liste d’actions"
            subtitle="Traitez les points de vigilance pour fluidifier vos opérations."
            action={
              <span className="count-pill">
                {(validated ? 0 : 1) +
                  trucks.filter((t) => t.state === "Maintenance").length +
                  (departed ? 0 : 1)}{" "}
                à traiter
              </span>
            }
          >
            <div className="task-list">
              {!validated && (
                <div>
                  <span className="activity-icon amber">
                    <Icon name="box" />
                  </span>
                  <section>
                    <h3>Un chargement attend votre validation</h3>
                    <p>CH-2024-049 · {fmt(total)} kg · Le chargement est encore en brouillon.</p>
                  </section>
                  <Badge>À valider</Badge>
                  <Button secondary onClick={() => go("CH04")}>
                    Valider le chargement <Icon name="arrow" size={16} />
                  </Button>
                </div>
              )}
              {trucks
                .filter((t) => t.state === "Maintenance")
                .map((t) => (
                  <div key={t.plate}>
                    <span className="activity-icon red">
                      <Icon name="truck" />
                    </span>
                    <section>
                      <h3>Camion indisponible</h3>
                      <p>{t.plate} · Maintenance en cours, aucune réservation possible.</p>
                    </section>
                    <Badge>Maintenance</Badge>
                    <Button secondary onClick={() => openTruck(t.plate)}>
                      Consulter la fiche <Icon name="arrow" size={16} />
                    </Button>
                  </div>
                ))}
              {!departed && (
                <div>
                  <span className="activity-icon blue">
                    <Icon name="route" />
                  </span>
                  <section>
                    <h3>Un transport attend son départ</h3>
                    <p>
                      TR-2024-049 ·{" "}
                      {reserved
                        ? "Réservation confirmée, départ à déclarer."
                        : "La réservation du camion doit être confirmée."}
                    </p>
                  </section>
                  <Badge>En attente</Badge>
                  <Button secondary onClick={() => go(reserved ? "TR03" : "TR01")}>
                    Préparer le départ <Icon name="arrow" size={16} />
                  </Button>
                </div>
              )}
            </div>
          </Card>
        );
      default:
        return null;
    }
  }
  const selectedTransport = initialTransports.find(
    (t) =>
      t.ref === detail ||
      t.ref.replace("TR", "CH") === detail ||
      t.ref.replace("TR", "BL") === detail,
  );
  return (
    <div className="app-shell">
      <aside className="sidebar">
        <button className="brand" onClick={() => go("G01")} aria-label="Transit, accueil">
          <span className="logo-mark">
            <Icon name="route" size={25} />
          </span>
          transit<span className="brand-dot">.</span>
        </button>
        <div className="workspace">
          <span className="workspace-avatar">TL</span>
          <div>
            <strong>Transit Logistique</strong>
            <small>Espace de travail</small>
          </div>
          <Icon name="chevron" size={14} />
        </div>
        <div className="nav-label">ESPACE DE GESTION</div>
        <nav>
          <button
            aria-label="Tableau de bord"
            className={`nav-item ${screen === "G01" ? "active" : ""}`}
            onClick={() => {
              go("G01");
              setExpanded("");
            }}
          >
            <Icon name="grid" />
            <span>Tableau de bord</span>
          </button>
          <button
            aria-label="Expéditions"
            className={`nav-item ${["G02", "G03"].includes(screen) ? "active" : ""}`}
            onClick={() => {
              go("G02");
              setExpanded("G");
            }}
          >
            <Icon name="layers" />
            <span>Expéditions</span>
            <span className="nav-count">48</span>
          </button>
          <div className="nav-divider" />
          <div className="nav-label">VOS MODULES</div>
          {groups.slice(1).map((g) => (
            <div key={g.prefix}>
              <button
                aria-label={g.label}
                className={`nav-item ${group.prefix === g.prefix ? "active" : ""}`}
                onClick={() => {
                  setExpanded(expanded === g.prefix ? "" : g.prefix);
                  go(g.prefix + (g.prefix === "CH" ? "05" : g.prefix === "TR" ? "02" : "01"));
                }}
              >
                <Icon name={g.icon} />
                <span>{g.label}</span>
                <Icon name="chevron" size={13} className={expanded === g.prefix ? "rotated" : ""} />
              </button>
              {expanded === g.prefix && (
                <div className="subnav">
                  {g.screens.map((s, i) => (
                    <button
                      key={s}
                      className={screen === `${g.prefix}0${i + 1}` ? "selected" : ""}
                      onClick={() => go(`${g.prefix}0${i + 1}`)}
                    >
                      {s}
                    </button>
                  ))}
                </div>
              )}
            </div>
          ))}
          <div className="nav-divider" />
          <button
            aria-label="À traiter"
            className={`nav-item ${screen === "G05" ? "active" : ""}`}
            onClick={() => go("G05")}
          >
            <Icon name="alert" />
            <span>À traiter</span>
            <span className="nav-count orange-count">
              {(validated ? 0 : 1) +
                trucks.filter((t) => t.state === "Maintenance").length +
                (departed ? 0 : 1)}
            </span>
          </button>
        </nav>
        <div className="sidebar-bottom">
          <div className="help-card">
            <span className="help-icon">
              <Icon name="help" size={19} />
            </span>
            <h3>Chaque étape, sous contrôle.</h3>
            <p>Du premier chargement à la dernière livraison.</p>
            <button onClick={() => setGallery(true)}>
              Explorer l’application <Icon name="arrow" size={15} />
            </button>
          </div>
          <button
            aria-label="Parcourir les 25 écrans"
            className="nav-item prototype-button"
            onClick={() => setGallery(true)}
          >
            <Icon name="layers" size={18} />
            <span>Parcourir les 25 écrans</span>
          </button>
          <div className="sidebar-footer">
            <span className="online-dot" />
            Tous les systèmes opérationnels
          </div>
        </div>
      </aside>
      <div className="main-shell">
        <header className="topbar">
          <div className="breadcrumbs">
            <span>Espace de gestion</span>
            <Icon name="chevron" size={12} />
            <strong>{screen === "G01" ? "Tableau de bord" : group.label}</strong>
            {screen !== "G01" && (
              <>
                <Icon name="chevron" size={12} />
                <span className="breadcrumb-id">{screen}</span>
              </>
            )}
          </div>
          <div className="topbar-right">
            <button className="top-search" onClick={() => go("G04")}>
              <Icon name="search" size={17} />
              <span>Rechercher…</span>
              <kbd>⌘ K</kbd>
            </button>
            <div className="notification-wrap">
              <button
                className="icon-button notification-button"
                aria-label="Notifications"
                onClick={() => setNotifications(!notifications)}
              >
                <Icon name="bell" />
                <i />
              </button>
              {notifications && (
                <div className="notification-popover">
                  <h3>Vos notifications</h3>
                  <p>3 opérations nécessitent votre attention.</p>
                  <button
                    onClick={() => {
                      go("G05");
                      setNotifications(false);
                    }}
                  >
                    Consulter les éléments à traiter <Icon name="arrow" size={15} />
                  </button>
                </div>
              )}
            </div>
            <div className="topbar-divider" />
            <button
              className="profile"
              onClick={() =>
                notify("Amine El Mansouri · Responsable logistique · Espace de démonstration")
              }
            >
              <span className="avatar">AE</span>
              <span>
                <strong>Amine El Mansouri</strong>
                <small>Responsable logistique</small>
              </span>
              <Icon name="chevron" size={13} />
            </button>
          </div>
        </header>
        <main data-screen={screen} data-section={group.label}>
          {screen !== "G01" && (
            <>
              <div className="welcome inner-welcome">
                <div>
                  <span className="eyebrow">
                    {group.label.toUpperCase()} <span className="screen-code">{screen}</span>
                  </span>
                  <h1>
                    {title}
                    <span className="title-dot">.</span>
                  </h1>
                  <p>
                    {group.prefix === "G"
                      ? "Toute votre activité logistique, connectée et accessible."
                      : "Des opérations claires, à chaque étape de votre expédition."}
                  </p>
                </div>
                <button className="screen-picker" onClick={() => setGallery(true)}>
                  <Icon name="grid" size={16} />
                  Tous les écrans <span>25</span>
                </button>
              </div>
              <div className="screen-tabs">
                {group.screens.map((s, i) => (
                  <button
                    key={s}
                    className={i === screenIndex ? "active" : ""}
                    onClick={() => go(`${group.prefix}0${i + 1}`)}
                  >
                    <small>
                      {group.prefix}0{i + 1}
                    </small>
                    {s}
                  </button>
                ))}
              </div>
            </>
          )}
          {renderScreen()}
          <footer className="main-footer">
            <span>© 2024 Transit. La logistique, simplement.</span>
            <span>
              Conçu pour aller plus loin.
              <i />
            </span>
          </footer>
        </main>
      </div>
      {toast && (
        <div className="toast" role="status">
          <span>
            <Icon name="check" size={18} />
          </span>
          {toast}
          <button aria-label="Fermer" onClick={() => setToast("")}>
            <Icon name="close" size={16} />
          </button>
        </div>
      )}
      {gallery && (
        <div className="modal-backdrop" onClick={() => setGallery(false)}>
          <div
            className="gallery-modal"
            role="dialog"
            aria-modal="true"
            aria-label="Les 25 écrans"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="modal-heading">
              <div>
                <span className="eyebrow">LE PROTOTYPE TRANSIT</span>
                <h2>25 écrans. Un réseau connecté.</h2>
                <p>Explorez les cinq sections de votre application.</p>
              </div>
              <button className="icon-button" aria-label="Fermer" onClick={() => setGallery(false)}>
                <Icon name="close" />
              </button>
            </div>
            <div className="gallery-groups">
              {groups.map((g) => (
                <div key={g.prefix}>
                  <h3>
                    <Icon name={g.icon} size={19} />
                    {g.label}
                    <span>5 écrans</span>
                  </h3>
                  {g.screens.map((s, i) => (
                    <button
                      key={s}
                      className={screen === `${g.prefix}0${i + 1}` ? "selected" : ""}
                      onClick={() => go(`${g.prefix}0${i + 1}`)}
                    >
                      <small>
                        {g.prefix}0{i + 1}
                      </small>
                      <span>{s}</span>
                      <Icon name="arrow" size={15} />
                    </button>
                  ))}
                </div>
              ))}
            </div>
            <div className="gallery-note">
              <Icon name="help" size={16} />
              Prototype de spécifications · Données de démonstration conservées pendant cette
              visite.
            </div>
          </div>
        </div>
      )}
      {confirm && (
        <div className="modal-backdrop">
          <div
            className="confirm-modal"
            role="alertdialog"
            aria-modal="true"
            aria-label={confirm.title}
          >
            <span className="confirmation-icon">
              <Icon name="alert" size={26} />
            </span>
            <h2>{confirm.title}</h2>
            <p>{confirm.text}</p>
            <div className="button-group">
              <Button secondary onClick={() => setConfirm(null)}>
                Annuler
              </Button>
              <Button onClick={confirm.action}>Confirmer</Button>
            </div>
          </div>
        </div>
      )}
      {goodEditor !== null && (
        <div className="modal-backdrop">
          <div
            className="confirm-modal goods-modal"
            role="dialog"
            aria-modal="true"
            aria-label="Marchandise"
          >
            <div className="modal-heading">
              <h2>
                {goodEditor === goods.length
                  ? "Ajouter une marchandise"
                  : "Modifier la marchandise"}
              </h2>
              <button
                className="icon-button"
                aria-label="Fermer"
                onClick={() => setGoodEditor(null)}
              >
                <Icon name="close" />
              </button>
            </div>
            <Field label="Désignation *">
              <input value={goodName} onChange={(e) => setGoodName(e.target.value)} />
            </Field>
            <div className="form-grid">
              <Field label="Quantité *">
                <input
                  type="number"
                  min="1"
                  step="1"
                  value={goodQty}
                  onChange={(e) => setGoodQty(e.target.value)}
                />
              </Field>
              <Field label="Poids unitaire (kg) *">
                <input
                  type="number"
                  min="0.01"
                  step="0.1"
                  value={goodWeight}
                  onChange={(e) => setGoodWeight(e.target.value)}
                />
              </Field>
            </div>
            <Button
              disabled={
                !goodName.trim() ||
                Number(goodQty) <= 0 ||
                !Number.isInteger(Number(goodQty)) ||
                Number(goodWeight) <= 0
              }
              onClick={() => {
                const next = [...goods];
                next[goodEditor] = {
                  name: goodName,
                  qty: Number(goodQty),
                  weight: Number(goodWeight),
                };
                setGoods(next);
                setGoodEditor(null);
                notify("La marchandise a été enregistrée.");
              }}
            >
              Enregistrer la marchandise
            </Button>
          </div>
        </div>
      )}
      {detail && !screen.startsWith("CA") && (
        <div className="drawer-backdrop" onClick={() => setDetail(null)}>
          <aside className="detail-drawer" onClick={(e) => e.stopPropagation()}>
            <div className="modal-heading">
              <div>
                <span className="eyebrow">DÉTAIL DE L’OPÉRATION</span>
                <h2>{detail}</h2>
              </div>
              <button
                className="icon-button"
                aria-label="Fermer le détail"
                onClick={() => setDetail(null)}
              >
                <Icon name="close" />
              </button>
            </div>
            <Badge>
              {selectedTransport
                ? detail.startsWith("CH")
                  ? "Validé"
                  : detail.startsWith("BL")
                    ? "Émis"
                    : selectedTransport.status
                : validated
                  ? "Validé"
                  : "Brouillon"}
            </Badge>
            <div className="drawer-journey">
              <Icon name="pin" />
              <div>
                <strong>{selectedTransport?.from || origin}</strong>
                <span>↓</span>
                <strong>{selectedTransport?.to || destination}</strong>
              </div>
            </div>
            <div className="drawer-info">
              <small>DESTINATAIRE</small>
              <strong>{selectedTransport?.recipient || recipient}</strong>
              <small>POIDS TOTAL</small>
              <strong>{selectedTransport ? selectedTransport.weight : fmt(total)} kg</strong>
              {selectedTransport && (
                <>
                  <small>CAMION</small>
                  <strong>{selectedTransport.plate}</strong>
                </>
              )}
            </div>
            <h3>{detail.startsWith("TR") ? "Chronologie des événements" : "Marchandises"}</h3>
            {detail.startsWith("TR") ? (
              <div className="timeline">
                <div className="timeline-item">
                  <span className="current">
                    <Icon name="pin" size={14} />
                  </span>
                  <div>
                    <small>{selectedTransport?.date || "18 juin, 08:30"}</small>
                    <h3>
                      {selectedTransport?.status === "Livré"
                        ? "Livraison confirmée"
                        : selectedTransport?.status === "Planifié"
                          ? "Départ planifié"
                          : "Départ confirmé"}
                    </h3>
                    <p>
                      {selectedTransport?.from} → {selectedTransport?.to}
                    </p>
                  </div>
                </div>
                <div className="timeline-item">
                  <span>
                    <Icon name="check" size={14} />
                  </span>
                  <div>
                    <small>La veille · 16:15</small>
                    <h3>Réservation confirmée</h3>
                    <p>Camion {selectedTransport?.plate} affecté au transport.</p>
                  </div>
                </div>
              </div>
            ) : selectedTransport ? (
              <div className="drawer-goods">
                <Icon name="box" />
                <div>
                  <strong>Palettes de produits alimentaires</strong>
                  <p>Poids total : {selectedTransport.weight} kg</p>
                </div>
              </div>
            ) : (
              goods.map((g, i) => (
                <div className="drawer-goods" key={i}>
                  <Icon name="box" />
                  <div>
                    <strong>{g.name}</strong>
                    <p>
                      {g.qty} × {fmt(g.weight)} kg = {fmt(g.qty * g.weight)} kg
                    </p>
                  </div>
                </div>
              ))
            )}
            <Notice>
              {selectedTransport || validated
                ? "Informations en lecture seule. Les documents validés ne sont plus modifiables."
                : "Ce chargement est en brouillon et peut encore être modifié."}
            </Notice>
            {!selectedTransport && (
              <Button onClick={() => go(validated ? "CH04" : "CH01")}>
                {validated ? "Consulter le chargement" : "Modifier le brouillon"}
              </Button>
            )}
          </aside>
        </div>
      )}
    </div>
  );
}

export default App;
