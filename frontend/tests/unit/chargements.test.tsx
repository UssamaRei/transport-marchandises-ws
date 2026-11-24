import { useState } from "react";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter, Outlet, Route, Routes } from "react-router-dom";
import { beforeEach, expect, it, vi } from "vitest";
const { rights } = vi.hoisted(() => ({ rights: { canEdit: true } }));
vi.mock("../../src/auth", () => ({ useAuth: () => rights, keycloak: {} }));
import { api, ApiError, type Chargement } from "../../src/api";
import { Marchandises, NewChargement, PoidsTotal, Validation } from "../../src/Chargements";
const initial: Chargement = {
  id: "load-1",
  origine: "Agadir",
  destination: "Fès",
  destinataireNom: "Client",
  destinataireTelephone: null,
  poidsTotalKg: 100,
  statut: "BROUILLON",
  camionSouhaiteId: null,
  createdAt: "2026-11-23T10:00:00Z",
  version: 0,
  lignes: [{ id: "line-1", designation: "Cartons", quantite: 10, poidsUnitaireKg: 10 }],
};
function show(element: React.ReactNode, overrides: Partial<Chargement> = {}) {
  function Context() {
    const [data, setData] = useState({ ...initial, ...overrides });
    return <Outlet context={{ data, setData, refresh: vi.fn() }} />;
  }
  render(
    <MemoryRouter>
      <Routes>
        <Route element={<Context />}>
          <Route path="/" element={element} />
        </Route>
      </Routes>
    </MemoryRouter>,
  );
}
beforeEach(() => {
  rights.canEdit = true;
  vi.spyOn(api, "camions").mockResolvedValue({
    items: [
      {
        id: "truck-1",
        immatriculation: "12345-A-1",
        capaciteKg: 100,
        etat: "EN_SERVICE",
        archive: false,
        disponible: true,
        createdAt: "",
      },
    ],
    page: 0,
    size: 20,
    totalPages: 1,
    totalElements: 1,
  });
  vi.spyOn(api, "validate").mockResolvedValue({
    ...initial,
    statut: "VALIDE",
    camionSouhaiteId: "truck-1",
  });
});
async function chooseTruck() {
  await userEvent.selectOptions(
    await screen.findByRole("combobox", { name: "Camion disponible" }),
    "truck-1",
  );
}
it("affiche les quantités et le poids total fourni par le service", () => {
  show(<PoidsTotal />);
  expect(screen.getByRole("cell", { name: "Cartons" })).toBeVisible();
  expect(screen.getAllByText("100 kg").length).toBeGreaterThan(0);
});
it("autorise la limite exacte de capacité après confirmation", async () => {
  show(<Validation />);
  await chooseTruck();
  const button = screen.getByRole("button", { name: "Valider le chargement" });
  expect(button).toBeEnabled();
  await userEvent.click(button);
  expect(api.validate).not.toHaveBeenCalled();
  await userEvent.click(screen.getByRole("button", { name: "Confirmer la validation" }));
  expect(await screen.findByRole("heading", { name: "Chargement validé" })).toBeVisible();
  expect(api.validate).toHaveBeenCalledWith("load-1", "truck-1");
});
it("bloque un dépassement de capacité de 0,001 kg", async () => {
  show(<Validation />, { poidsTotalKg: 100.001 });
  await chooseTruck();
  expect(screen.getByRole("button", { name: "Valider le chargement" })).toBeDisabled();
  expect(api.validate).not.toHaveBeenCalled();
});
it("bloque la validation quand le destinataire manque", async () => {
  show(<Validation />, { destinataireNom: " " });
  await chooseTruck();
  expect(screen.getByRole("button", { name: "Valider le chargement" })).toBeDisabled();
});
it("explique un refus serveur sans afficher un faux succès", async () => {
  vi.mocked(api.validate).mockRejectedValue(new ApiError("Camion réservé entre-temps", 409));
  show(<Validation />);
  await chooseTruck();
  await userEvent.click(screen.getByRole("button", { name: "Valider le chargement" }));
  await userEvent.click(screen.getByRole("button", { name: "Confirmer la validation" }));
  expect(await screen.findByRole("alert")).toHaveTextContent("Camion réservé entre-temps");
  expect(screen.queryByRole("heading", { name: "Chargement validé" })).toBeNull();
});
it("ajoute une ligne et affiche le poids recalculé par le service", async () => {
  const add = vi
    .spyOn(api, "addLigne")
    .mockResolvedValue({
      ...initial,
      poidsTotalKg: 150,
      lignes: [
        ...initial.lignes,
        { id: "line-2", designation: "Sacs", quantite: 2, poidsUnitaireKg: 25 },
      ],
    });
  show(<Marchandises />);
  await userEvent.click(screen.getByRole("button", { name: "Ajouter une marchandise" }));
  await userEvent.type(screen.getByRole("textbox", { name: "Désignation" }), "Sacs");
  const qty = screen.getByRole("spinbutton", { name: "Quantité" });
  await userEvent.clear(qty);
  await userEvent.type(qty, "2");
  await userEvent.type(screen.getByRole("spinbutton", { name: "Poids unitaire (kg)" }), "25");
  await userEvent.click(screen.getByRole("button", { name: "Ajouter la marchandise" }));
  expect(await screen.findByRole("cell", { name: "Sacs" })).toBeVisible();
  expect(add).toHaveBeenCalledWith("load-1", {
    designation: "Sacs",
    quantite: 2,
    poidsUnitaireKg: 25,
  });
  expect(screen.getByText("150 kg", { exact: true })).toBeVisible();
});
it("ne propose pas de modification d’un chargement déjà validé", () => {
  show(<Marchandises />, { statut: "VALIDE" });
  expect(screen.queryByRole("button", { name: "Ajouter une marchandise" })).toBeNull();
});
it("une ouverture directe de création reste en lecture seule pour LECTEUR", () => {
  rights.canEdit = false;
  show(<NewChargement />);
  expect(screen.getByText("Votre compte permet uniquement la consultation.")).toBeVisible();
  expect(screen.queryByRole("button", { name: "Créer le brouillon" })).toBeNull();
});
