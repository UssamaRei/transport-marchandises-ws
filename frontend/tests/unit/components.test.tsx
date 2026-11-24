import { act, fireEvent, render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { expect, it, vi } from "vitest";
vi.mock("../../src/auth", () => ({ keycloak: {} }));
import { ApiError } from "../../src/api";
import { ErrorBanner, Loading, Modal, ToastProvider, useToast } from "../../src/components/live";
import { Button, Field } from "../../src/components/ui";

it("annonce les erreurs et déduplique les messages de champs", async () => {
  const retry = vi.fn();
  render(
    <ErrorBanner
      error={new ApiError("Saisie invalide", 400, { a: "Obligatoire", b: "Obligatoire" })}
      retry={retry}
    />,
  );
  expect(screen.getByRole("alert")).toHaveFocus();
  expect(screen.getAllByText("Obligatoire")).toHaveLength(1);
  await userEvent.click(screen.getByRole("button", { name: "Réessayer" }));
  expect(retry).toHaveBeenCalledOnce();
});
it("n’affiche pas d’alerte en absence d’erreur", () => {
  render(<ErrorBanner error={null} />);
  expect(screen.queryByRole("alert")).toBeNull();
});
it("annonce le chargement aux lecteurs d’écran", () => {
  render(<Loading text="Lecture des bons…" />);
  expect(screen.getByRole("status")).toHaveTextContent("Lecture des bons…");
});
it("relie le libellé et l’aide au champ de saisie", () => {
  render(
    <Field label="Poids" hint="En kilogrammes">
      <input aria-describedby="extra" />
      <span>kg</span>
    </Field>,
  );
  const input = screen.getByRole("textbox", { name: "Poids" });
  expect(input.getAttribute("aria-describedby")).toContain("extra");
  expect(input).toHaveAccessibleDescription("En kilogrammes");
});
it("un bouton désactivé ne déclenche pas une seconde écriture", async () => {
  const save = vi.fn();
  render(
    <Button disabled onClick={save}>
      Enregistrer
    </Button>,
  );
  await userEvent.click(screen.getByRole("button"));
  expect(save).not.toHaveBeenCalled();
});
it("une fenêtre occupée interdit la fermeture par Échap ou bouton", () => {
  const close = vi.fn();
  render(
    <Modal title="Enregistrement" busy close={close}>
      <p>En cours</p>
    </Modal>,
  );
  expect(screen.getByRole("button", { name: "Fermer" })).toBeDisabled();
  fireEvent(screen.getByRole("dialog"), new Event("cancel", { cancelable: true }));
  expect(close).not.toHaveBeenCalled();
});
it("une fenêtre disponible accepte Échap", () => {
  const close = vi.fn();
  render(
    <Modal title="Modification" close={close}>
      <p>Contenu</p>
    </Modal>,
  );
  fireEvent(screen.getByRole("dialog"), new Event("cancel", { cancelable: true }));
  expect(close).toHaveBeenCalledOnce();
});
function Notify() {
  const notify = useToast();
  return <button onClick={() => notify("Chargement enregistré")}>Notifier</button>;
}
it("ferme une notification sans quitter la page", async () => {
  render(
    <ToastProvider>
      <Notify />
    </ToastProvider>,
  );
  await userEvent.click(screen.getByText("Notifier"));
  expect(screen.getByRole("status")).toHaveTextContent("Chargement enregistré");
  await userEvent.click(screen.getByRole("button", { name: "Fermer la notification" }));
  expect(screen.queryByRole("status")).toBeNull();
});
it("retire automatiquement une notification après son délai", () => {
  vi.useFakeTimers();
  render(
    <ToastProvider>
      <Notify />
    </ToastProvider>,
  );
  fireEvent.click(screen.getByText("Notifier"));
  act(() => vi.advanceTimersByTime(5500));
  expect(screen.queryByRole("status")).toBeNull();
});
