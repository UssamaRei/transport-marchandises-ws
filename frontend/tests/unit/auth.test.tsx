import { act, render, screen } from "@testing-library/react";
import { beforeEach, expect, it, vi } from "vitest";
const { kc } = vi.hoisted(() => ({
  kc: {
    authenticated: true,
    tokenParsed: {} as Record<string, unknown>,
    init: vi.fn().mockResolvedValue(true),
    updateToken: vi.fn().mockResolvedValue(false),
    clearToken: vi.fn(),
    login: vi.fn(),
    logout: vi.fn(),
    onAuthSuccess: undefined as undefined | (() => void),
    onAuthLogout: undefined as undefined | (() => void),
    onAuthRefreshError: undefined as undefined | (() => void),
  },
}));
vi.mock("keycloak-js", () => ({
  default: class {
    constructor() {
      return kc;
    }
  },
}));
import { AuthProvider, login, logout, useAuth } from "../../src/auth";
function Session() {
  const auth = useAuth();
  return (
    <div>
      <span data-testid="ready">{String(auth.ready)}</span>
      <span data-testid="name">{auth.name}</span>
      <span data-testid="role">{auth.role}</span>
      <span data-testid="read">{String(auth.canRead)}</span>
      <span data-testid="edit">{String(auth.canEdit)}</span>
      <span data-testid="authenticated">{String(auth.authenticated)}</span>
    </div>
  );
}
beforeEach(() => {
  kc.authenticated = true;
  kc.tokenParsed = { preferred_username: "agent-ws", roles: [] };
  kc.clearToken.mockClear();
});
it.each([
  ["ADMIN", true, true],
  ["AGENT", true, true],
  ["LECTEUR", true, false],
  ["AUTRE", false, false],
])("applique les permissions du rôle %s", async (role, read, edit) => {
  kc.tokenParsed.roles = [role];
  render(
    <AuthProvider>
      <Session />
    </AuthProvider>,
  );
  expect(await screen.findByText("true", { selector: '[data-testid="ready"]' })).toBeVisible();
  expect(screen.getByTestId("read")).toHaveTextContent(String(read));
  expect(screen.getByTestId("edit")).toHaveTextContent(String(edit));
});
it("donne priorité au rôle ADMIN quand plusieurs rôles existent", async () => {
  kc.tokenParsed = { name: "Agent de test", roles: ["LECTEUR", "ADMIN"] };
  render(
    <AuthProvider>
      <Session />
    </AuthProvider>,
  );
  expect(await screen.findByText("Agent de test")).toBeVisible();
  expect(screen.getByTestId("role")).toHaveTextContent("ADMIN");
});
it("ne traite pas une claim roles mal formée comme une autorisation", async () => {
  kc.tokenParsed = { roles: "ADMIN" };
  render(
    <AuthProvider>
      <Session />
    </AuthProvider>,
  );
  expect(await screen.findByText("Sans rôle métier")).toBeVisible();
  expect(screen.getByTestId("edit")).toHaveTextContent("false");
});
it("répercute une déconnexion reçue de Keycloak", async () => {
  render(
    <AuthProvider>
      <Session />
    </AuthProvider>,
  );
  await screen.findByText("agent-ws");
  act(() => {
    kc.authenticated = false;
    kc.onAuthLogout?.();
  });
  expect(screen.getByTestId("authenticated")).toHaveTextContent("false");
});
it("efface le jeton après une erreur de rafraîchissement", async () => {
  render(
    <AuthProvider>
      <Session />
    </AuthProvider>,
  );
  await screen.findByText("agent-ws");
  act(() => kc.onAuthRefreshError?.());
  expect(kc.clearToken).toHaveBeenCalledOnce();
});
it("définit une redirection locale pour connexion et déconnexion", async () => {
  await login();
  await logout();
  expect(kc.login).toHaveBeenCalledWith({
    redirectUri: window.location.origin + window.location.pathname,
  });
  expect(kc.logout).toHaveBeenCalledWith({ redirectUri: window.location.origin + "/chargements" });
});
