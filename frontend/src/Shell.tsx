import { Link, NavLink, Outlet, useLocation } from "react-router-dom";
import { useEffect } from "react";
import { login, logout, useAuth } from "./auth";
import { Button, Icon } from "./components/ui";
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
          Gérez vos camions, préparez les marchandises et suivez chaque livraison dans un même
          espace.
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
  const isDashboard = location.pathname === "/";
  const isDetail = /^\/chargements\/[^/]+/.test(location.pathname);
  const isBonRoute = location.pathname.startsWith("/bons");
  const isTransportRoute = location.pathname.startsWith("/transports");
  const isCamionRoute = location.pathname.startsWith("/camions");
  const sectionPath = isDashboard
    ? "/"
    : isBonRoute
      ? "/bons"
      : isTransportRoute
        ? "/transports"
        : isCamionRoute
          ? "/camions"
          : "/chargements";
  const sectionLabel = isDashboard
    ? "Tableau de bord"
    : isBonRoute
      ? "Bons de livraison"
      : isTransportRoute
        ? "Suivi des transports"
        : isCamionRoute
          ? "Camions"
          : "Chargements";
  useEffect(() => {
    const query = new URLSearchParams(location.search);
    const id =
      isBonRoute && query.has("chargementId")
        ? "emettre-bon"
        : isTransportRoute && query.has("bonId")
          ? "planifier-transport"
          : "";
    if (id) document.getElementById(id)?.scrollIntoView({ block: "start" });
    else window.scrollTo(0, 0);
  }, [location.pathname, location.search, isBonRoute, isTransportRoute]);
  return (
    <ToastProvider>
      <div className="app-shell live-app">
        <a className="live-skip" href="#main-content">
          Aller au contenu
        </a>
        <aside className="sidebar">
          <Link className="brand" to="/" aria-label="Transit, accueil">
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
            <NavLink
              end
              to="/"
              aria-label="Tableau de bord"
              className={({ isActive }) => `nav-item ${isActive ? "active" : ""}`}
            >
              <Icon name="grid" />
              <span>Tableau de bord</span>
            </NavLink>
            <NavLink
              className={({ isActive }) => `nav-item ${isActive ? "active" : ""}`}
              to="/chargements"
              aria-label="Chargements"
            >
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

            <NavLink
              to="/camions"
              aria-label="Camions"
              className={({ isActive }) => `nav-item ${isActive ? "active" : ""}`}
            >
              <Icon name="truck" />
              <span>Camions</span>
            </NavLink>

            <NavLink
              to="/bons"
              aria-label="Bons de livraison"
              className={({ isActive }) => `nav-item ${isActive ? "active" : ""}`}
            >
              <Icon name="file" />
              <span>Bons de livraison</span>
            </NavLink>

            <NavLink
              to="/transports"
              aria-label="Suivi des transports"
              className={({ isActive }) => `nav-item ${isActive ? "active" : ""}`}
            >
              <Icon name="route" />
              <span>Suivi des transports</span>
            </NavLink>
          </nav>
          <div className="sidebar-bottom">
            <div className="help-card">
              <span className="help-icon">
                <Icon name="layers" />
              </span>
              <h3>Le projet en images.</h3>
              <p>Les maquettes donnent un aperçu des autres écrans du projet.</p>
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
              <Link to={sectionPath}>
                <strong>{sectionLabel}</strong>
              </Link>
              {(isDetail || location.pathname !== sectionPath) && (
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
                  Votre compte est connecté, mais aucun rôle ne vous permet de consulter les les
                  données. Contactez le responsable de l’équipe.
                </p>
              </div>
            )}
            <footer className="main-footer">
              <span>© {new Date().getFullYear()} Transit. La logistique, simplement.</span>
              <span>Transport de marchandises</span>
            </footer>
          </main>
        </div>
      </div>
    </ToastProvider>
  );
}
