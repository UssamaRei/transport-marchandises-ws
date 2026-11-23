import { test, expect, type Page } from "@playwright/test";
import fs from "node:fs";
import path from "node:path";

const root = path.resolve(import.meta.dirname, "../..");
const privateFile =
  process.env.E2E_AUTH_ENV ||
  path.join(
    root,
    process.env.E2E_ISOLATED_STACK === "1" ? ".secrets/systeme-test.env" : ".secrets/auth.env",
  );
const values = Object.fromEntries(
  fs
    .readFileSync(privateFile, "utf8")
    .split(/\r?\n/)
    .filter((line) => line.includes("="))
    .map((line) => [line.slice(0, line.indexOf("=")), line.slice(line.indexOf("=") + 1)]),
);
const output = path.join(root, ".tools/dashboard-qa");

async function login(page: Page, role: "AGENT" | "LECTEUR") {
  await page.goto("/");
  await page.getByRole("button", { name: "Se connecter", exact: true }).click();
  await page.locator("#username").fill(role.toLowerCase() + "-ws");
  await page.locator("#password").fill(values[`WS_${role}_PASSWORD`]);
  await page.locator("#kc-login").click();
  await expect(page.getByRole("heading", { name: "Tableau de bord." })).toBeVisible();
  await expect(page.getByRole("button", { name: "Actualiser", exact: true })).toBeEnabled();
}

test("dashboard: real counters, read-only navigation and mobile layout", async ({ page }) => {
  fs.mkdirSync(output, { recursive: true });
  const errors: string[] = [];
  const writes: string[] = [];
  const metrics: Record<string, number> = {};
  page.on("pageerror", (error) => errors.push(error.message));
  page.on("request", (request) => {
    if (new URL(request.url()).pathname.startsWith("/api/v1/") && request.method() !== "GET")
      writes.push(request.method());
  });
  page.on("response", async (response) => {
    const url = new URL(response.url());
    if (!response.ok() || !url.pathname.startsWith("/api/v1/")) return;
    const data = await response.json();
    if (url.pathname === "/api/v1/camions" && url.searchParams.get("disponible") === "true")
      metrics.truck = data.totalElements;
    if (url.pathname === "/api/v1/chargements/statistiques") metrics.box = data.brouillons;
    if (url.pathname === "/api/v1/transports" && url.searchParams.get("actifs") === "true")
      metrics.route = data.totalElements;
    if (url.pathname === "/api/v1/bons" && !url.searchParams.has("statut"))
      metrics.file = data.totalElements;
  });
  await login(page, "LECTEUR");
  await expect(page.getByRole("alert")).toHaveCount(0);
  await expect.poll(() => Object.keys(metrics).length).toBe(4);
  for (const [key, value] of Object.entries(metrics))
    await expect(page.getByTestId(`metric-${key}`)).toHaveText(value.toLocaleString("fr-FR"));
  await expect(page.getByRole("link", { name: "Nouveau chargement", exact: true })).toHaveCount(0);
  await page.screenshot({ path: path.join(output, "01-dashboard-desktop.png"), fullPage: true });
  await page.setViewportSize({ width: 390, height: 844 });
  expect(
    await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth + 1),
  ).toBeTruthy();
  await page.screenshot({ path: path.join(output, "02-dashboard-mobile.png"), fullPage: true });
  await page.setViewportSize({ width: 1440, height: 1000 });
  await page.locator(".dash-metric-truck").click();
  await expect(page.getByLabel("Disponibilité", { exact: true })).toHaveValue("true");
  await page.getByRole("link", { name: "Transit, accueil" }).click();
  await page.locator(".dash-metric-route").click();
  await expect(page).toHaveURL(/actifs=true/);
  await expect(page.getByRole("heading", { name: "Transports sélectionnés" })).toBeVisible();
  await page.goto("/dashboard");
  await expect(page).toHaveURL(/\/$/);
  await expect(page.getByRole("heading", { name: "Tableau de bord." })).toBeVisible();
  await expect(page.getByRole("button", { name: "Actualiser", exact: true })).toBeEnabled();
  await page.getByRole("link", { name: "Livrés", exact: false }).last().click();
  await expect(page).toHaveURL(/statut=LIVRE/);
  expect(errors).toEqual([]);
  expect(writes).toEqual([]);
});

test("dashboard: partial service failure, retry and empty data", async ({ page }) => {
  await login(page, "LECTEUR");
  await page.route("**/api/v1/transports?*", (route) =>
    route.fulfill({
      status: 503,
      contentType: "application/json",
      body: JSON.stringify({ message: "Transport temporairement indisponible" }),
    }),
  );
  await page.getByRole("button", { name: "Actualiser", exact: true }).click();
  await expect(page.getByTestId("metric-route")).toHaveText("—");
  await expect(page.getByTestId("metric-box")).not.toHaveText("—");
  await expect(
    page.getByText("Certaines données sont indisponibles.", { exact: false }),
  ).toBeVisible();
  await page.screenshot({
    path: path.join(output, "03-dashboard-erreur-partielle.png"),
    fullPage: true,
  });
  await page.unroute("**/api/v1/transports?*");
  await page.getByRole("button", { name: "Actualiser", exact: true }).click();
  await expect(page.getByTestId("metric-route")).not.toHaveText("—");
  await expect(page.getByRole("button", { name: "Actualiser", exact: true })).toBeEnabled();
  // Browser-only fixtures: no database writes and no container changes.
  await page.route("**/api/v1/**", (route) => {
    const pathname = new URL(route.request().url()).pathname;
    const body = pathname.endsWith("/statistiques")
      ? { total: 0, brouillons: 0, valides: 0 }
      : pathname === "/api/v1/chargements"
        ? []
        : { items: [], content: [], totalElements: 0, totalPages: 0, number: 0, page: 0, size: 1 };
    return route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(body),
    });
  });
  await page.getByRole("button", { name: "Actualiser", exact: true }).click();
  for (const key of ["truck", "box", "route", "file"])
    await expect(page.getByTestId(`metric-${key}`)).toHaveText("0");
  await expect(page.getByText("Aucun transport actif pour le moment.")).toBeVisible();
  await expect(page.locator(".dash-donut.is-empty")).toBeVisible();
  expect(await page.locator(".live-dashboard").innerText()).not.toMatch(/NaN|undefined/);
  await page.screenshot({ path: path.join(output, "04-dashboard-vide.png"), fullPage: true });
});

test("dashboard: agent can open the loading creation form", async ({ page }) => {
  await login(page, "AGENT");
  const action = page.getByRole("link", { name: "Nouveau chargement", exact: true });
  await expect(action).toBeVisible();
  await page.screenshot({ path: path.join(output, "05-dashboard-agent.png"), fullPage: true });
  await action.click();
  await expect(page).toHaveURL(/\/chargements\/nouveau$/);
  // Do not submit: dashboard QA must stay read-only on the shared database.
});

test("dashboard: populated transport table and delivery notes", async ({ page }) => {
  // Only the browser receives these fixtures; the shared database is untouched.
  await page.route("**/api/v1/**", (route) => {
    const url = new URL(route.request().url());
    const loading = {
      id: "loading-fixture",
      origine: "Agadir",
      destination: "Rabat",
      poidsTotalKg: 2500,
      statut: "VALIDE",
    };
    const active = ["EN_COURS", "CLOTURE_EN_ATTENTE"].map((statut, i) => ({
      id: `transport-${i}`,
      camionId: `camion-${i}`,
      destination: "Casablanca — dépôt central",
      departPrevu: "2026-11-19T08:30:00",
      statut,
    }));
    const content = url.searchParams.get("actifs") === "true" ? active : [];
    const transportTotal =
      url.searchParams.get("actifs") === "true"
        ? 2
        : url.searchParams.get("statut") === "LIVRE"
          ? 6
          : 1;
    const body = url.pathname.endsWith("/statistiques")
      ? { total: 9, brouillons: 4, valides: 5 }
      : url.pathname.endsWith("/chargements")
        ? [loading]
        : url.pathname.endsWith("/camions")
          ? { items: [], totalElements: url.searchParams.has("disponible") ? 3 : 6 }
          : url.pathname.endsWith("/bons")
            ? {
                content: [
                  {
                    id: "bon-fixture",
                    numero: "BL-2026-001",
                    destination: "Rabat",
                    poidsTotalKg: 2500,
                    statut: "EMIS",
                  },
                ],
                totalElements: 1,
              }
            : { content, totalElements: transportTotal, totalPages: 1, number: 0 };
    return route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(body),
    });
  });
  await login(page, "LECTEUR");
  await expect(page.getByTestId("metric-route")).toHaveText("2");
  await expect(page.locator(".dash-donut strong")).toHaveText("9");
  await expect(page.locator(".live-dashboard tbody tr")).toHaveCount(2);
  await expect(page.getByText("En cours", { exact: true })).toBeVisible();
  await expect(page.getByText("Clôture en attente", { exact: true })).toBeVisible();
  await expect(page.getByRole("link", { name: /BL-2026-001/ })).toHaveAttribute(
    "href",
    "/bons/bon-fixture",
  );
  await page.screenshot({
    path: path.join(output, "06-dashboard-donnees-simulees.png"),
    fullPage: true,
  });
  await page.setViewportSize({ width: 390, height: 844 });
  expect(
    await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth + 1),
  ).toBeTruthy();
});
