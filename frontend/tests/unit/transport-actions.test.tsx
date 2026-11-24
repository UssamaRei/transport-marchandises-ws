import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, expect, it, vi } from "vitest";
const { rights, post } = vi.hoisted(() => ({ rights: { canEdit: true }, post: vi.fn() }));
vi.mock("../../src/auth", () => ({ useAuth: () => rights }));
vi.mock("../../src/api", async (importOriginal) => ({
  ...(await importOriginal<typeof import("../../src/api")>()),
  request: post,
}));
import { ActionsTransport } from "../../src/TransportActions";
import type { Transport } from "../../src/api";
const trip: Transport = {
  id: "trip-1",
  bonId: "bon-1",
  camionId: "truck-1",
  destination: "Fès",
  poidsKg: 100,
  departPrevu: null,
  statut: "CLOTURE_EN_ATTENTE",
  reservationId: "r-1",
  departReel: null,
  arriveeReelle: null,
  preuveConfirmee: false,
  liberationConfirmee: false,
  syncEtat: "OK",
};
beforeEach(() => {
  rights.canEdit = true;
  post.mockReset().mockResolvedValue({});
  vi.spyOn(window, "confirm").mockReturnValue(true);
});
const changed = vi.fn();
function show(statut = trip.statut) {
  return render(<ActionsTransport transport={{ ...trip, statut }} onChanged={changed} />);
}
async function proof() {
  await userEvent.type(screen.getByRole("textbox", { name: "Réceptionnaire" }), "Client Test");
  fireEvent.change(screen.getByLabelText("Date de réception", { exact: true }), {
    target: { value: "2026-11-23" },
  });
}
it("interdit les actions au LECTEUR", () => {
  rights.canEdit = false;
  show();
  expect(screen.queryByRole("button")).toBeNull();
});
it("exige le réceptionnaire et la date avant toute requête", async () => {
  show();
  await userEvent.click(screen.getByRole("button", { name: "Confirmer la livraison" }));
  expect(screen.getByRole("status")).toHaveTextContent(
    "Réceptionnaire et date de réception obligatoires",
  );
  expect(post).not.toHaveBeenCalled();
});
it("ne transmet rien si la confirmation est annulée", async () => {
  vi.mocked(window.confirm).mockReturnValue(false);
  show();
  await proof();
  await userEvent.click(screen.getByRole("button", { name: "Confirmer la livraison" }));
  expect(post).not.toHaveBeenCalled();
});
it("envoie la preuve puis retire la sauvegarde après succès", async () => {
  show();
  await proof();
  await userEvent.click(screen.getByRole("button", { name: "Confirmer la livraison" }));
  await waitFor(() =>
    expect(post).toHaveBeenCalledWith("/api/v1/transports/trip-1/livraison", "POST", {
      receptionnaire: "Client Test",
      dateReception: "2026-11-23",
      commentaire: null,
    }),
  );
  expect(sessionStorage.getItem("transports.livraison.trip-1")).toBeNull();
  expect(screen.getByRole("status")).toHaveTextContent("Action enregistrée");
});
it("conserve la même preuve après coupure et après remontage de l’écran", async () => {
  post.mockRejectedValue(new Error("Service indisponible"));
  const view = show();
  await proof();
  await userEvent.click(screen.getByRole("button", { name: "Confirmer la livraison" }));
  expect(await screen.findByText(/Vérifiez l'état et les événements/)).toBeVisible();
  expect(JSON.parse(sessionStorage.getItem("transports.livraison.trip-1")!)).toMatchObject({
    receptionnaire: "Client Test",
  });
  expect(screen.getByRole("button", { name: "Confirmer la livraison" })).toBeDisabled();
  view.unmount();
  show();
  expect(screen.getByRole("textbox", { name: "Réceptionnaire" })).toHaveValue("Client Test");
  expect(screen.getByRole("textbox", { name: "Réceptionnaire" })).toBeDisabled();
});
it("ignore une sauvegarde locale corrompue sans bloquer l’écran", () => {
  sessionStorage.setItem("transports.livraison.trip-1", "{broken");
  show();
  expect(screen.getByRole("textbox", { name: "Réceptionnaire" })).toHaveValue("");
});
it("refuse un départ sans date valide avant de contacter le service", async () => {
  show("PLANIFIE");
  await userEvent.click(screen.getByRole("button", { name: "Déclarer le départ" }));
  expect(screen.getByRole("status")).toHaveTextContent("Saisissez une date et une heure valides");
  expect(post).not.toHaveBeenCalled();
});
it("exige une description pour un événement de suivi", async () => {
  show("EN_COURS");
  await userEvent.click(screen.getByRole("button", { name: "Ajouter l'événement" }));
  expect(screen.getByRole("status")).toHaveTextContent(
    "La description de l’événement est obligatoire",
  );
  expect(post).not.toHaveBeenCalled();
});
