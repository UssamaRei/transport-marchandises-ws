import { test, expect, type Page } from "@playwright/test";
import fs from "node:fs";
import path from "node:path";
import { execFileSync } from "node:child_process";

const root = path.resolve(import.meta.dirname, "../..");
const privateFile = path.join(root, ".secrets/systeme-test.env");
const output = path.join(root, ".tools/frontend-systeme");
const values = Object.fromEntries(
  fs
    .readFileSync(privateFile, "utf8")
    .split(/\r?\n/)
    .filter((line) => line.includes("="))
    .map((line) => {
      const index = line.indexOf("=");
      return [line.slice(0, index), line.slice(index + 1)];
    }),
);
let camionId = "",
  chargementId = "",
  bonId = "",
  transportId = "",
  agentToken = "";
const identifier = `UI-${Date.now()}`;

test.describe.configure({ mode: "serial" });
test.beforeAll(() => {
  if (process.env.E2E_ISOLATED_STACK !== "1")
    throw new Error(
      "Use only the disposable integration stack. Set E2E_ISOLATED_STACK=1 after starting it.",
    );
  fs.mkdirSync(output, { recursive: true });
});

async function login(page: Page, role: "AGENT" | "LECTEUR") {
  await page.goto("/camions");
  await page.getByRole("button", { name: "Se connecter", exact: true }).click();
  await expect(page).toHaveURL(/^http:\/\/localhost:18090\//);
  await page.locator("#username").fill(role.toLowerCase() + "-ws");
  await page.locator("#password").fill(values[`WS_${role}_PASSWORD`]);
  page.on("request", (request) => {
    if (role === "AGENT" && new URL(request.url()).pathname.startsWith("/api/v1/"))
      agentToken = request.headers()["authorization"] || agentToken;
  });
  await page.locator("#kc-login").click();
  await expect(page.getByRole("heading", { name: "Vos camions." })).toBeVisible();
}

async function shot(page: Page, name: string) {
  await page.screenshot({ path: path.join(output, name + ".png"), fullPage: true });
}
async function api(page: Page, url: string, method = "GET", data?: unknown) {
  const response = await page.request.fetch(url, {
    method,
    headers: { Authorization: agentToken },
    ...(data ? { data } : {}),
  });
  expect(response.ok(), `${method} ${url}: ${response.status()}`).toBeTruthy();
  return response.json();
}
function stack(action: "stop" | "start", service: string) {
  execFileSync(
    "docker",
    [
      "compose",
      "-p",
      "ws-systeme-test",
      "--env-file",
      privateFile,
      "-f",
      path.join(root, "tests/integration/compose.yaml"),
      action,
      ...(action === "stop" ? ["-t", "1"] : []),
      service,
    ],
    { cwd: root, stdio: "pipe" },
  );
}
async function awaitBons(page: Page) {
  await expect
    .poll(
      async () => {
        try {
          return (await page.request.get("http://127.0.0.1:18083/health")).status();
        } catch {
          return 0;
        }
      },
      { timeout: 90000 },
    )
    .toBe(200);
}

test("parcours complet à travers les quatre modules", async ({ page }) => {
  const errors: string[] = [];
  page.on("pageerror", (error) => errors.push(error.message));
  page.on("dialog", (dialog) => dialog.accept());
  await login(page, "AGENT");
  await page.getByRole("button", { name: "Ajouter un camion", exact: true }).click();
  await page.getByLabel("Immatriculation", { exact: true }).fill(identifier);
  await page.getByLabel("Capacité (kg)", { exact: true }).fill("100");
  const createdTruck = page.waitForResponse(
    (r) => new URL(r.url()).pathname === "/api/v1/camions" && r.request().method() === "POST",
  );
  await page.getByRole("button", { name: "Créer le camion", exact: true }).click();
  camionId = (await (await createdTruck).json()).id;
  await page.getByRole("link", { name: identifier, exact: true }).click();
  await page.getByRole("button", { name: "Modifier le camion", exact: true }).click();
  await page.getByLabel("Capacité (kg)", { exact: true }).fill("150");
  await page.getByRole("button", { name: "Enregistrer le camion", exact: true }).click();
  await expect(page.getByRole("dialog")).toHaveCount(0);
  await expect(page.getByText("150 kg", { exact: true })).toBeVisible();
  await shot(page, "01-camion");

  await page
    .getByRole("navigation", { name: "Navigation principale" })
    .getByRole("link", { name: "Chargements", exact: true })
    .click();
  await page.getByRole("link", { name: "Nouveau chargement", exact: true }).click();
  await page.getByLabel("Ville d’origine", { exact: true }).fill("Agadir");
  await page.getByLabel("Ville de destination", { exact: true }).fill("Casablanca");
  await page.getByLabel("Nom du destinataire", { exact: true }).fill(identifier);
  await page.getByRole("button", { name: "Enregistrer et continuer", exact: true }).click();
  await expect(
    page.getByRole("heading", { name: "Marchandises du chargement", exact: true }),
  ).toBeVisible();
  chargementId = page.url().match(/chargements\/([^/]+)/)![1];
  await page.getByRole("button", { name: "Ajouter une marchandise", exact: true }).click();
  await page.getByLabel("Désignation", { exact: true }).fill("Cartons de démonstration");
  await page.getByLabel("Quantité", { exact: true }).fill("10");
  await page.getByLabel("Poids unitaire (kg)", { exact: true }).fill("10");
  await page.getByRole("button", { name: "Ajouter la marchandise", exact: true }).click();
  await expect(page.getByRole("dialog")).toHaveCount(0);
  await page.getByRole("link", { name: "Voir le poids total", exact: true }).click();
  await page.getByRole("link", { name: "Vérifier le chargement", exact: true }).click();
  await page.getByLabel("Camion disponible", { exact: true }).selectOption(camionId);
  await page.getByRole("button", { name: "Valider le chargement", exact: true }).click();
  await page.getByRole("button", { name: "Confirmer la validation", exact: true }).click();
  await expect(page.getByRole("heading", { name: "Chargement validé", exact: true })).toBeVisible();
  await shot(page, "02-chargement-valide");

  await page.getByRole("link", { name: "Préparer le bon de livraison" }).click();
  await expect(page.getByLabel("Chargement validé", { exact: true })).toHaveValue(chargementId);
  const emitted = page.waitForResponse(
    (r) => new URL(r.url()).pathname === "/api/v1/bons" && r.request().method() === "POST",
  );
  await page.getByRole("button", { name: "Émettre le bon", exact: true }).click();
  const bon = await (await emitted).json();
  bonId = bon.id;
  await expect(page).toHaveURL(new RegExp(`/bons/${bonId}$`));
  await expect(page.getByRole("heading", { name: bon.numero + ".", exact: true })).toBeVisible();
  await shot(page, "03-bon-emis");

  await page.getByRole("link", { name: "Planifier le transport" }).click();
  await expect(page.getByLabel("Bon de livraison", { exact: true })).toHaveValue(bonId);
  await page.getByLabel("Camion disponible", { exact: true }).selectOption(camionId);
  await page.getByLabel("Départ prévu", { exact: true }).fill("2026-11-19T10:00");
  const planned = page.waitForResponse(
    (r) => new URL(r.url()).pathname === "/api/v1/transports" && r.request().method() === "POST",
  );
  await page.getByRole("button", { name: "Planifier", exact: true }).click();
  const transport = await (await planned).json();
  transportId = transport.id;
  await expect(page).toHaveURL(new RegExp(`/transports/${transportId}$`));
  await expect(page.getByRole("heading", { name: "Détail du transport.", exact: true })).toBeVisible();
  await expect(page.locator("p").filter({ hasText: /^Statut\s*:\s*PLANIFIE$/ })).toBeVisible();
  await shot(page, "04-transport-planifie");
  await page.getByLabel("Date et heure du départ réel", { exact: true }).fill("2026-11-19T10:00");
  await page.getByRole("button", { name: "Déclarer le départ", exact: true }).click();
  await expect(page.locator("p").filter({ hasText: /^Statut\s*:\s*EN_COURS$/ })).toBeVisible();
  await page
    .getByLabel("Date et heure de l'arrivée réelle", { exact: true })
    .fill("2026-11-19T11:00");
  await page.getByLabel("Description", { exact: true }).fill("Passage au point de contrôle");
  await page.getByRole("button", { name: "Ajouter l'événement", exact: true }).click();
  await expect(page.getByText("Passage au point de contrôle", { exact: true })).toBeVisible();
  await page
    .getByLabel("Date et heure de l'arrivée réelle", { exact: true })
    .fill("2026-11-19T12:00");
  await page.getByRole("button", { name: "Déclarer l'arrivée", exact: true }).click();
  await expect(page.locator("p").filter({ hasText: /^Statut\s*:\s*CLOTURE_EN_ATTENTE$/ })).toBeVisible();
  await page.getByLabel("Réceptionnaire", { exact: true }).fill("Client de démonstration");
  await page.getByLabel("Date de réception", { exact: true }).fill("2026-11-19");
  await page.getByLabel("Commentaire", { exact: true }).fill("Livraison complète");
  await page.getByRole("button", { name: "Confirmer la livraison", exact: true }).click();
  await expect(page.locator("p").filter({ hasText: /^Statut\s*:\s*LIVRE$/ })).toBeVisible();
  await shot(page, "05-transport-livre");
  await page.getByRole("link", { name: "Consulter le bon", exact: true }).click();
  await expect(page.getByText("Client de démonstration", { exact: true })).toBeVisible();
  await shot(page, "06-preuve-livraison");
  await page.getByRole("navigation").getByRole("link", { name: "Camions", exact: true }).click();
  await page.getByRole("link", { name: identifier, exact: true }).click();
  await expect(page.getByRole("heading", { name: identifier + ".", exact: true })).toBeVisible();
  await expect(page.getByText("Disponible", { exact: true })).toBeVisible();
  const persisted = await api(page, `/api/v1/transports/${transportId}`);
  expect(persisted.preuveConfirmee && persisted.liberationConfirmee).toBeTruthy();
  expect((await api(page, `/api/v1/bons/${bonId}`)).statut).toBe("LIVRE");
  expect((await api(page, `/api/v1/camions/${camionId}`)).disponible).toBe(true);
  expect(errors).toEqual([]);
});

test("reprise d’une réception après panne et rechargement de page", async ({ page }) => {
  await login(page, "AGENT");
  page.on("dialog", (dialog) => dialog.accept());
  const load = await api(page, "/api/v1/chargements", "POST", {
    origine: "Agadir",
    destination: "Rabat",
    destinataireNom: "Reprise UI",
  });
  await api(page, `/api/v1/chargements/${load.id}/lignes`, "POST", {
    designation: "Colis",
    quantite: 1,
    poidsUnitaireKg: 10,
  });
  await api(page, `/api/v1/chargements/${load.id}/validation`, "POST", { camionId });
  const bon = await api(page, "/api/v1/bons", "POST", { chargementId: load.id });
  const trip = await api(page, "/api/v1/transports", "POST", {
    bonId: bon.id,
    camionId,
    departPrevu: "2026-11-19T10:00:00Z",
    operationKey: `ui-retry:${Date.now()}`,
  });
  await api(page, `/api/v1/transports/${trip.id}/depart`, "POST", {
    departReel: "2026-11-19T10:00:00Z",
  });
  await api(page, `/api/v1/transports/${trip.id}/arrivee`, "POST", {
    arriveeReelle: "2026-11-19T11:00:00Z",
  });
  await page.goto(`/transports/${trip.id}`);
  await page.getByLabel("Réceptionnaire", { exact: true }).fill("Réception sauvegardée");
  await page.getByLabel("Date de réception", { exact: true }).fill("2026-11-19");
  await page.getByLabel("Commentaire", { exact: true }).fill("Même preuve après reprise");
  try {
    stack("stop", "bons");
    await page.getByRole("button", { name: "Confirmer la livraison", exact: true }).click();
    await expect(page.getByText(/Vérifiez l'état et les événements/)).toBeVisible();
    await page.reload();
    await expect(page.getByLabel("Réceptionnaire", { exact: true })).toHaveValue(
      "Réception sauvegardée",
    );
    await expect(page.getByLabel("Réceptionnaire", { exact: true })).toBeDisabled();
  } finally {
    stack("start", "bons");
    await awaitBons(page);
  }
  await page.getByRole("button", { name: "Confirmer la livraison", exact: true }).click();
  await expect(page.locator("p").filter({ hasText: /^Statut\s*:\s*LIVRE$/ })).toBeVisible();
  const proof = await api(page, `/api/v1/bons/${bon.id}`);
  expect(proof.recepteur).toBe("Réception sauvegardée");
  expect(proof.commentaire).toBe("Même preuve après reprise");
});

test("lecture seule, navigation, responsive et erreurs", async ({ page }) => {
  const errors: string[] = [];
  page.on("pageerror", (error) => errors.push(error.message));
  await login(page, "LECTEUR");
  await expect(page.getByRole("button", { name: "Ajouter un camion", exact: true })).toHaveCount(0);
  await page.goto(`/camions/${camionId}`);
  await expect(page.getByRole("heading", { name: identifier + "." })).toBeVisible();
  await expect(page.getByRole("button", { name: "Modifier le camion", exact: true })).toHaveCount(
    0,
  );
  for (const route of [
    "/camions",
    "/bons",
    `/bons/${bonId}`,
    "/transports",
    `/transports/${transportId}`,
  ]) {
    await page.goto(route);
    await expect(page.getByRole("main")).toBeVisible();
    await expect(page.locator(".live-loading")).toHaveCount(0);
    await page.setViewportSize({ width: 390, height: 844 });
    await expect
      .poll(() => page.evaluate(() => document.documentElement.scrollWidth <= innerWidth + 1))
      .toBe(true);
    await shot(page, "mobile-" + route.replace(/\W/g, "-").slice(0, 32));
    await page.setViewportSize({ width: 1440, height: 1000 });
  }
  await expect(
    page.getByRole("button", { name: "Confirmer la livraison", exact: true }),
  ).toHaveCount(0);
  await page.goto("/bons");
  await expect(page.getByRole("button", { name: "Émettre le bon", exact: true })).toBeDisabled();
  await page.goto("/transports");
  await expect(page.getByRole("button", { name: "Planifier", exact: true })).toHaveCount(0);
  await page.goto("/bons/unknown-ui-id");
  await expect(page.getByRole("alert")).toBeVisible();
  await page.getByRole("button", { name: "Réessayer", exact: true }).click();
  await expect(page.getByRole("alert")).toBeVisible();
  await page.getByRole("button", { name: "Se déconnecter", exact: true }).click();
  await expect(page.getByRole("button", { name: "Se connecter", exact: true })).toBeVisible();
  expect(errors).toEqual([]);
});
