import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { beforeEach, expect, it, vi } from "vitest";
const { rights } = vi.hoisted(() => ({
  rights: { canEdit: false, canRead: true, name: "Test", role: "LECTEUR" },
}));
vi.mock("../../src/auth", () => ({ useAuth: () => rights, keycloak: {} }));
import { api, ApiError } from "../../src/api";
import { CamionsList, CamionDetail } from "../../src/Camions";
import { ChargementsList } from "../../src/Chargements";
import { BonsList, BonDetail } from "../../src/Bons";
import { TransportsList, TransportDetail } from "../../src/Transports";

const truck = {
  id: "truck-1",
  immatriculation: "12345-A-1",
  capaciteKg: 1000,
  etat: "EN_SERVICE",
  archive: false,
  disponible: true,
  createdAt: "2026-11-23T10:00:00Z",
};
const load = {
  id: "load-1",
  origine: "Agadir",
  destination: "Fès",
  poidsTotalKg: 300,
  statut: "VALIDE" as const,
};
const bon = {
  id: "bon-1",
  numero: "BL-TEST-1",
  chargementId: "load-1",
  camionSouhaiteId: "truck-1",
  origine: "Agadir",
  destination: "Fès",
  destinataireNom: "Client fictif",
  destinataireTelephone: null,
  poidsTotalKg: 300,
  statut: "LIVRE" as const,
  transportId: "trip-1",
  recepteur: "Client fictif",
  dateReception: "2026-11-23",
  commentaire: "Reçu",
  createdAt: null,
  lignes: [],
};
const trip = {
  id: "trip-1",
  bonId: "bon-1",
  camionId: "truck-1",
  destination: "Fès",
  poidsKg: 300,
  departPrevu: null,
  statut: "LIVRE",
  reservationId: "r-1",
  departReel: null,
  arriveeReelle: null,
  preuveConfirmee: true,
  liberationConfirmee: true,
  syncEtat: "OK",
};
function show(element: React.ReactNode, url = "/", path = "*") {
  return render(
    <MemoryRouter initialEntries={[url]}>
      <Routes>
        <Route path={path} element={element} />
      </Routes>
    </MemoryRouter>,
  );
}
beforeEach(() => {
  rights.canEdit = false;
  vi.spyOn(api, "flotte").mockResolvedValue({
    items: [truck],
    page: 0,
    size: 20,
    totalElements: 1,
    totalPages: 1,
  });
  vi.spyOn(api, "camion").mockResolvedValue(truck);
  vi.spyOn(api, "list").mockResolvedValue([load]);
  vi.spyOn(api, "bons").mockResolvedValue({
    content: [bon],
    number: 0,
    totalElements: 1,
    totalPages: 1,
  });
  vi.spyOn(api, "bon").mockResolvedValue(bon);
  vi.spyOn(api, "transports").mockResolvedValue({
    content: [trip],
    number: 0,
    totalElements: 1,
    totalPages: 1,
  });
  vi.spyOn(api, "transport").mockResolvedValue(trip);
  vi.spyOn(api, "evenementsTransport").mockResolvedValue({ content: [], totalElements: 0 });
});

it("affiche la flotte en lecture seule sans action de création", async () => {
  show(<CamionsList />);
  expect(await screen.findByRole("link", { name: truck.immatriculation })).toHaveAttribute(
    "href",
    "/camions/truck-1",
  );
  expect(screen.queryByRole("button", { name: "Ajouter un camion" })).toBeNull();
  expect(screen.getByRole("button", { name: "Suivant" })).toBeDisabled();
});
it("relance la consultation avec le filtre de disponibilité choisi", async () => {
  show(<CamionsList />);
  await screen.findByText(truck.immatriculation);
  await userEvent.selectOptions(screen.getByRole("combobox", { name: "Disponibilité" }), "true");
  await waitFor(() =>
    expect(api.flotte).toHaveBeenLastCalledWith(0, "true", expect.any(AbortSignal)),
  );
});
it("propose une reprise après panne Camions", async () => {
  vi.mocked(api.flotte).mockRejectedValueOnce(new ApiError("Service indisponible"));
  show(<CamionsList />);
  expect(await screen.findByRole("alert")).toHaveTextContent("Service indisponible");
  await userEvent.click(screen.getByRole("button", { name: "Réessayer" }));
  expect(await screen.findByText(truck.immatriculation)).toBeVisible();
});
it("affiche une flotte vide sans inventer de camion", async () => {
  vi.mocked(api.flotte).mockResolvedValue({
    items: [],
    page: 0,
    size: 20,
    totalElements: 0,
    totalPages: 0,
  });
  show(<CamionsList />);
  expect(await screen.findByText("Aucun camion à afficher")).toBeVisible();
  expect(screen.getByRole("button", { name: "Suivant" })).toBeDisabled();
});
it("la fiche camion LECTEUR masque la modification", async () => {
  show(<CamionDetail />, "/camions/truck-1", "/camions/:id");
  expect(await screen.findByRole("heading", { name: "12345-A-1." })).toBeVisible();
  expect(screen.queryByRole("button", { name: "Modifier le camion" })).toBeNull();
  expect(screen.getByText("Disponible", { exact: true })).toBeVisible();
});
it("filtre les chargements par destination sans changer les données", async () => {
  show(<ChargementsList />);
  await screen.findByText("Fès", { exact: true });
  await userEvent.type(
    screen.getByRole("textbox", { name: "Rechercher dans cette page" }),
    "Rabat",
  );
  expect(screen.queryByText("Fès", { exact: true })).toBeNull();
  expect(api.list).toHaveBeenCalledOnce();
});
it("un LECTEUR ne peut pas ouvrir l’action Nouveau chargement depuis la liste", async () => {
  show(<ChargementsList />);
  await screen.findByText("Fès", { exact: true });
  expect(screen.queryByRole("link", { name: "Nouveau chargement" })).toBeNull();
});
it("le rôle AGENT dispose de l’action Nouveau chargement", async () => {
  rights.canEdit = true;
  show(<ChargementsList />);
  expect(await screen.findByRole("link", { name: "Nouveau chargement" })).toHaveAttribute(
    "href",
    "/chargements/nouveau",
  );
});
it("consulte les bons émis sans permettre leur émission au LECTEUR", async () => {
  show(<BonsList />);
  expect(await screen.findByRole("link", { name: "BL-TEST-1" })).toHaveAttribute(
    "href",
    "/bons/bon-1",
  );
  expect(screen.getByRole("button", { name: "Émettre le bon" })).toBeDisabled();
});
it("affiche la preuve de réception stockée dans le bon", async () => {
  show(<BonDetail />, "/bons/bon-1", "/bons/:id");
  expect(await screen.findByRole("heading", { name: "BL-TEST-1." })).toBeVisible();
  expect(screen.getAllByText("Client fictif").length).toBeGreaterThan(0);
  expect(screen.getByText("Reçu", { exact: true })).toBeVisible();
});
it("signale un bon introuvable et permet de réessayer", async () => {
  vi.mocked(api.bon).mockRejectedValue(new ApiError("Bon introuvable", 404));
  show(<BonDetail />, "/bons/missing", "/bons/:id");
  expect(await screen.findByRole("alert")).toHaveTextContent("Bon introuvable");
  await userEvent.click(screen.getByRole("button", { name: "Réessayer" }));
  await waitFor(() => expect(api.bon).toHaveBeenCalledTimes(2));
});
it("affiche les transports et applique le filtre de statut de la route", async () => {
  show(<TransportsList />, "/transports?statut=LIVRE");
  expect(await screen.findByRole("link", { name: "trip-1" })).toHaveAttribute(
    "href",
    "/transports/trip-1",
  );
  expect(api.transports).toHaveBeenCalledWith(0, expect.any(AbortSignal), "", "LIVRE", false);
  expect(screen.queryByRole("button", { name: "Planifier" })).toBeNull();
});
it("consulte un transport livré sans action d’écriture au LECTEUR", async () => {
  show(<TransportDetail />, "/transports/trip-1", "/transports/:id");
  expect(await screen.findByText("LIVRE", { exact: true })).toBeVisible();
  expect(screen.queryByRole("button", { name: "Confirmer la livraison" })).toBeNull();
});
