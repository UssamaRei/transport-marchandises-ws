import {
  Children,
  cloneElement,
  isValidElement,
  useId,
  type ReactNode,
  type ButtonHTMLAttributes,
} from "react";

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
      aria-hidden="true"
      focusable="false"
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
  secondary = false,
  danger = false,
  icon,
  className = "",
  type = "button",
  ...props
}: ButtonHTMLAttributes<HTMLButtonElement> & {
  secondary?: boolean;
  danger?: boolean;
  icon?: IconName;
}) {
  return (
    <button
      type={type}
      className={`btn ${secondary ? "secondary" : ""} ${danger ? "danger" : ""} ${className}`}
      {...props}
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
  const id = useId();
  return (
    <label className="field">
      <span id={id}>{label}</span>
      {Children.map(children, (child) => {
        if (
          !isValidElement<Record<string, unknown>>(child) ||
          typeof child.type !== "string" ||
          !["input", "select", "textarea"].includes(child.type)
        )
          return child;
        return cloneElement(child, {
          "aria-labelledby": id,
          "aria-describedby":
            [child.props["aria-describedby"], hint ? `${id}-hint` : ""].filter(Boolean).join(" ") ||
            undefined,
        });
      })}
      {hint && <small id={`${id}-hint`}>{hint}</small>}
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

export { Icon, Badge, Button, Card, Field, Notice };
export type { IconName };
