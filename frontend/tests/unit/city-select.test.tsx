import { fireEvent, render, screen } from "@testing-library/react";
import { expect, it, vi } from "vitest";
import { CitySelect } from "../../src/components/CitySelect";

async function show(props: Partial<React.ComponentProps<typeof CitySelect>> = {}) {
  render(
    <CitySelect label="Destination" name="destination" value="" onChange={vi.fn()} {...props} />,
  );
  await screen.findByText("Villes et communes du Maroc · Saisissez pour rechercher");
  return screen.getByRole("combobox", { name: "Destination" });
}
it("charge le catalogue et relie le champ à son libellé", async () => {
  expect(await show()).toHaveAttribute("autocomplete", "off");
  expect(
    screen.getByRole("button", { name: "Afficher les villes pour destination" }),
  ).toBeEnabled();
});
it("reconnaît une recherche sans accents", async () => {
  const input = await show();
  fireEvent.change(input, { target: { value: "fes" } });
  expect(await screen.findByText(/résultat\(s\) · Choisissez dans la liste/)).toBeVisible();
});
it("reconnaît une recherche en arabe", async () => {
  const input = await show();
  fireEvent.change(input, { target: { value: "الدار البيضاء" } });
  expect(await screen.findByText(/résultat\(s\) · Choisissez dans la liste/)).toBeVisible();
});
it("explique une recherche inconnue", async () => {
  const input = await show();
  fireEvent.change(input, { target: { value: "zzzz-inconnue" } });
  expect(await screen.findByText("Aucune ville trouvée. Essayez un autre nom.")).toBeVisible();
});
it("conserve une ancienne valeur absente du catalogue", async () => {
  expect(await show({ value: "Ancienne destination" })).toHaveValue("Ancienne destination");
});
it("désactive le champ d’une fiche non modifiable", async () => {
  expect(await show({ value: "Agadir", disabled: true })).toBeDisabled();
});
it("annonce une erreur de saisie sur le champ", async () => {
  expect(await show({ error: "Choisissez une ville" })).toHaveAttribute("aria-invalid", "true");
  expect(screen.getByText("Choisissez une ville")).toBeVisible();
});
