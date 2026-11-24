import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import { beforeEach, expect, it, vi } from "vitest";
const { rights } = vi.hoisted(() => ({
  rights: { canEdit: false, name: "Lecteur", role: "LECTEUR" },
}));
vi.mock("../../src/auth", () => ({ useAuth: () => rights, keycloak: {} }));
import Dashboard from "../../src/Dashboard";
import { dashboardApi } from "../../src/dashboard-api";
import { ApiError } from "../../src/api";
beforeEach(() => {
  rights.canEdit = false;
  vi.spyOn(dashboardApi, "camions").mockResolvedValue({ total: 12, disponibles: 7 });
  vi.spyOn(dashboardApi, "chargements").mockResolvedValue({
    total: 125,
    brouillons: 80,
    valides: 45,
    recent: [],
  });
  vi.spyOn(dashboardApi, "bons").mockResolvedValue({ total: 21, enAttente: 9, recent: [] });
  vi.spyOn(dashboardApi, "transports").mockResolvedValue({
    actifs: 4,
    livres: 20,
    annules: 2,
    aConfirmer: 1,
    recent: [],
  });
});
function show() {
  render(
    <MemoryRouter>
      <Dashboard />
    </MemoryRouter>,
  );
}
it("affiche les compteurs globaux fournis par chaque service", async () => {
  show();
  await waitFor(() => expect(screen.getByTestId("metric-truck")).toHaveTextContent("7"));
  expect(screen.getByTestId("metric-box")).toHaveTextContent("80");
  expect(screen.getByTestId("metric-route")).toHaveTextContent("4");
  expect(screen.getByTestId("metric-file")).toHaveTextContent("21");
});
it("un service en panne n’empêche pas les autres compteurs de s’afficher", async () => {
  vi.mocked(dashboardApi.bons).mockRejectedValue(new ApiError("Bons indisponible"));
  show();
  await waitFor(() => expect(screen.getByTestId("metric-file")).toHaveTextContent("—"));
  expect(screen.getByTestId("metric-truck")).toHaveTextContent("7");
  expect(screen.getAllByRole("alert").length).toBeGreaterThan(0);
});
it("actualise les quatre sections sans afficher de fausse donnée", async () => {
  show();
  await waitFor(() => expect(screen.getByRole("button", { name: "Actualiser" })).toBeEnabled());
  await userEvent.click(screen.getByRole("button", { name: "Actualiser" }));
  await waitFor(() => expect(dashboardApi.camions).toHaveBeenCalledTimes(2));
  expect(dashboardApi.bons).toHaveBeenCalledTimes(2);
});
it("masque le raccourci de création pour un LECTEUR", async () => {
  show();
  await waitFor(() => expect(screen.getByTestId("metric-truck")).toHaveTextContent("7"));
  expect(screen.queryByRole("link", { name: "Nouveau chargement" })).toBeNull();
});
it("propose le raccourci de création à un AGENT", async () => {
  rights.canEdit = true;
  show();
  expect(await screen.findByRole("link", { name: "Nouveau chargement" })).toHaveAttribute(
    "href",
    "/chargements/nouveau",
  );
});
