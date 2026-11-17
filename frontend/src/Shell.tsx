import { Link, NavLink, Outlet, useLocation } from "react-router-dom";
import { login, logout, useAuth } from "./auth";
import { Button, Icon, type IconName } from "./components/ui";
import { Loading, ToastProvider } from "./components/live";

export function AuthBoundary() {
  const auth = useAuth();
  if (!auth.ready)
    return (
      <div className="live-auth-page">
        <Loading text="Vérification de votre session…" />
      </div>
    );
  if (auth.authenticated) return <Outlet />;
  return (
    <div className="live-auth-page">
      <div className="live-auth-card">
        <div className="brand">
          <span className="logo-mark">
            <Icon name="route" size={25} />
          </span>
          transit<span className="brand-dot">.</span>
        </div>
        <span className="eyebrow">GESTION DU TRANSPORT</span>
        <h1>
          Vos expéditions,
          <br />à chaque étape<span className="title-dot">.</span>
        </h1>
        <p>
          Retrouvez vos chargements, préparez vos marchandises et validez leur transport dans un
          même espace.
        </p>
        {auth.error ? (
          <>
            <div className="live-error" role="alert">
              {auth.error}
            </div>
            <Button onClick={() => window.location.reload()}>Réessayer</Button>
          </>
        ) : (
          <Button
            icon="arrow"
            onClick={() => {
              void login();
            }}
          >
            Se connecter
          </Button>
        )}
        <span className="live-auth-footnote">Connectez-vous avec votre compte d’équipe.</span>
        <Link to="/maquettes" className="text-link live-prototype-link">
          Consulter les maquettes de l’application <Icon name="chevron" size={13} />
        </Link>
      </div>
      <div className="live-auth-visual" aria-hidden="true">
        <div className="live-route">
          <span>Agadir</span>
          <i />
          <Icon name="truck" size={42} />
          <i />
          <span>Destination</span>
        </div>
        <div className="live-auth-box">
          <Icon name="box" size={68} />
        </div>
        <p>La logistique, simplement.</p>
      </div>
    </div>
  );
}

export function Shell() {
  const auth = useAuth();
  const location = useLocation();
  const isDetail = /^\/chargements\/[^/]+/.test(location.pathname);
  return (
    <ToastProvider>
      <div className="app-shell live-app">
        <a className="live-skip" href="#main-content">
          Aller au contenu
        </a>
        <aside className="sidebar">
          <Link className="brand" to="/chargements" aria-label="Transit, accueil">
            <span className="logo-mark">
              <Icon name="route" size={25} />
            </span>
            transit<span className="brand-dot">.</span>
          </Link>
          <div className="workspace">
            <span className="workspace-avatar">WS</span>
            <div>
              <strong>Transport de marchandises</strong>
              <small>Espace de l’équipe</small>
            </div>
          </div>
          <div className="nav-label">VOS MODULES</div>
          <nav aria-label="Navigation principale">
            <NavLink className="nav-item active" to="/chargements" aria-label="Chargements">
              <Icon name="box" />
              <span>Chargements</span>
            </NavLink>
            <div className="subnav">
              <NavLink
                end
                to="/chargements"
                className={({ isActive }) => (isActive ? "selected" : "")}
              >
                Tous les chargements
              </NavLink>
              {auth.canEdit && (
                <NavLink
                  to="/chargements/nouveau"
                  className={({ isActive }) => (isActive ? "selected" : "")}
                >
                  Nouveau brouillon
                </NavLink>
              )}
            </div>
            {(
              [
                { name: "Camions", icon: "truck" },
                { name: "Bons de livraison", icon: "file" },
                { name: "Suivi des transports", icon: "route" },
              ] as { name: string; icon: IconName }[]
            ).map((item) => (
              <div className="nav-item live-nav-pending" key={item.name}>
                <Icon name={item.icon} />
                <span>{item.name}</span>
                <small>Maquette</small>
              </div>
            ))}
          </nav>
          <div className="sidebar-bottom">
            <div className="help-card">
              <span className="help-icon">
                <Icon name="layers" />
              </span>
              <h3>Le projet en images.</h3>
              <p>Les autres modules sont disponibles dans les maquettes.</p>
              <Link to="/maquettes">
                Voir les 25 écrans <Icon name="arrow" size={15} />
              </Link>
            </div>
            <Link className="nav-item" to="/maquettes" aria-label="Consulter les maquettes">
              <Icon name="layers" size={18} />
              <span>Les 25 maquettes</span>
            </Link>
            <button
              className="nav-item"
              type="button"
              aria-label="Se déconnecter"
              onClick={() => {
                void logout();
              }}
            >
              <Icon name="logout" size={18} />
              <span>Se déconnecter</span>
            </button>
            <div className="sidebar-footer">Projet Web Services · 2026–2027</div>
          </div>
        </aside>
        <div className="main-shell">
          <header className="topbar">
            <div className="breadcrumbs">
              <span>Espace de gestion</span>
              <Icon name="chevron" size={12} />
              <Link to="/chargements">
                <strong>Chargements</strong>
              </Link>
              {isDetail && (
                <>
                  <Icon name="chevron" size={12} />
                  <span className="breadcrumb-id">
                    {location.pathname.endsWith("nouveau") ? "Nouveau" : "Détail"}
                  </span>
                </>
              )}
            </div>
            <div className="profile">
              <span className="avatar">{auth.name.slice(0, 2).toUpperCase()}</span>
              <span>
                <strong>{auth.name}</strong>
                <small>
                  {auth.role === "LECTEUR"
                    ? "Lecture seule"
                    : auth.role === "ADMIN"
                      ? "Administrateur"
                      : auth.role === "AGENT"
                        ? "Agent logistique"
                        : auth.role}
                </small>
              </span>
            </div>
          </header>
          <main id="main-content" className="live-content" tabIndex={-1}>
            {auth.canRead ? (
              <Outlet />
            ) : (
              <div className="card live-empty">
                <Icon name="alert" size={32} />
                <h1>Accès non attribué</h1>
                <p>
                  Votre compte est connecté, mais aucun rôle ne vous permet de consulter les
                  chargements. Contactez le responsable de l’équipe.
                </p>
              </div>
            )}
            <footer className="main-footer">
              <span>© {new Date().getFullYear()} Transit. La logistique, simplement.</span>
              <span>Gestion des chargements</span>
            </footer>
          </main>
        </div>
      </div>
    </ToastProvider>
  );
}
