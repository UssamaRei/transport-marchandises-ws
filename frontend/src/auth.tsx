import { createContext, useContext, useEffect, useState, type ReactNode } from "react";
import Keycloak from "keycloak-js";

export const keycloak = new Keycloak({
  url: import.meta.env.VITE_KEYCLOAK_URL || "http://localhost:8090",
  realm: import.meta.env.VITE_KEYCLOAK_REALM || "transport-ws",
  clientId: import.meta.env.VITE_KEYCLOAK_CLIENT_ID || "transport-web",
});

let initialization: Promise<boolean> | undefined;
type Session = {
  ready: boolean;
  authenticated: boolean;
  error: string;
  name: string;
  roles: string[];
};
const initial: Session = { ready: false, authenticated: false, error: "", name: "", roles: [] };
const AuthContext = createContext(initial);
function current(): Session {
  const claims = keycloak.tokenParsed;
  return {
    ready: true,
    authenticated: !!keycloak.authenticated,
    error: "",
    name: claims?.name || claims?.preferred_username || "Utilisateur",
    roles: Array.isArray(claims?.roles) ? claims.roles : [],
  };
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [session, setSession] = useState(initial);
  useEffect(() => {
    let active = true;
    const update = () => {
      if (active) setSession(current());
    };
    keycloak.onAuthSuccess = update;
    keycloak.onAuthRefreshSuccess = update;
    keycloak.onAuthLogout = update;
    keycloak.onAuthRefreshError = () => keycloak.clearToken();
    initialization ??= keycloak.init({
      onLoad: "check-sso",
      pkceMethod: "S256",
      checkLoginIframe: false,
      responseMode: "query",
    });
    const timeout = window.setTimeout(() => {
      if (active)
        setSession({ ...initial, ready: true, error: "Le serveur de connexion ne répond pas." });
    }, 20000);
    initialization
      .then(update)
      .catch(() => {
        if (active)
          setSession({
            ...initial,
            ready: true,
            error: "La connexion est indisponible. Vérifiez que Keycloak est démarré.",
          });
      })
      .finally(() => window.clearTimeout(timeout));
    const timer = window.setInterval(() => {
      if (keycloak.authenticated) void keycloak.updateToken(60).catch(() => keycloak.clearToken());
    }, 20000);
    return () => {
      active = false;
      window.clearTimeout(timeout);
      window.clearInterval(timer);
    };
  }, []);
  return <AuthContext.Provider value={session}>{children}</AuthContext.Provider>;
}
export function useAuth() {
  const session = useContext(AuthContext);
  return {
    ...session,
    canEdit: session.roles.some((role) => ["ADMIN", "AGENT"].includes(role)),
    canRead: session.roles.some((role) => ["ADMIN", "AGENT", "LECTEUR"].includes(role)),
    role:
      ["ADMIN", "AGENT", "LECTEUR"].find((role) => session.roles.includes(role)) ||
      "Sans rôle métier",
  };
}
export const login = () =>
  keycloak.login({ redirectUri: window.location.origin + window.location.pathname });
export const logout = () =>
  keycloak.logout({ redirectUri: window.location.origin + "/chargements" });
