import { render, screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter, Routes, Route } from "react-router-dom";
import { beforeEach, expect, it, vi } from "vitest";
const { session, connect, disconnect } = vi.hoisted(() => ({
  session: {
    ready: true,
    authenticated: true,
    error: "",
    name: "Agent Test",
    role: "AGENT",
    canEdit: true,
    canRead: true,
  },
  connect: vi.fn(),
  disconnect: vi.fn(),
}));
vi.mock("../../src/auth", () => ({ useAuth: () => session, login: connect, logout: disconnect }));
import { AuthBoundary, Shell } from "../../src/Shell";
beforeEach(() => {
  Object.assign(session, {
    ready: true,
    authenticated: true,
    error: "",
    canEdit: true,
    role: "AGENT",
  });
});
function boundary() {
  render(
    <MemoryRouter>
      <Routes>
        <Route element={<AuthBoundary />}>
          <Route path="/" element={<p>Zone privée</p>} />
        </Route>
      </Routes>
    </MemoryRouter>,
  );
}
it("attend la vérification de session avant d’afficher la zone privée", () => {
  session.ready = false;
  boundary();
  expect(screen.getByRole("status")).toHaveTextContent("Vérification de votre session");
  expect(screen.queryByText("Zone privée")).toBeNull();
});
it("propose une connexion quand la session est absente", async () => {
  session.authenticated = false;
  boundary();
  await userEvent.click(screen.getByRole("button", { name: "Se connecter" }));
  expect(connect).toHaveBeenCalledOnce();
  expect(screen.queryByText("Zone privée")).toBeNull();
});
it("annonce une panne d’authentification et propose une reprise", () => {
  session.authenticated = false;
  session.error = "Keycloak indisponible";
  boundary();
  expect(screen.getByRole("alert")).toHaveTextContent("Keycloak indisponible");
  expect(screen.getByRole("button", { name: "Réessayer" })).toBeEnabled();
});
it("ouvre la zone privée après authentification", () => {
  boundary();
  expect(screen.getByText("Zone privée")).toBeVisible();
});
it.each(["/", "/camions/truck-1", "/bons/bon-1", "/transports/trip-1", "/chargements/load-1"])(
  "conserve les quatre modules dans la navigation de %s",
  (url) => {
    render(
      <MemoryRouter initialEntries={[url]}>
        <Shell />
      </MemoryRouter>,
    );
    const nav = screen.getByRole("navigation", { name: "Navigation principale" });
    expect(nav).toHaveTextContent("Camions");
    expect(nav).toHaveTextContent("Bons de livraison");
    expect(nav).toHaveTextContent("Suivi des transports");
    expect(nav).toHaveTextContent("Chargements");
  },
);
it("déclenche la déconnexion depuis l’espace de gestion", async () => {
  render(
    <MemoryRouter>
      <Shell />
    </MemoryRouter>,
  );
  await userEvent.click(screen.getByRole("button", { name: "Se déconnecter" }));
  expect(disconnect).toHaveBeenCalledOnce();
});

it("conserve les noms accessibles des liens quand le menu masque son texte", () => {
  render(
    <>
      <style>{".nav-item span { display: none; }"}</style>
      <MemoryRouter>
        <Shell />
      </MemoryRouter>
    </>,
  );
  const navigation = within(screen.getByRole("navigation", { name: "Navigation principale" }));
  for (const name of [
    "Tableau de bord",
    "Chargements",
    "Camions",
    "Bons de livraison",
    "Suivi des transports",
  ]) {
    expect(navigation.getByRole("link", { name })).toHaveAccessibleName(name);
  }
});
