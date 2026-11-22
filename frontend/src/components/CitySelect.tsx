import { useEffect, useMemo, useState } from "react";
import {
  Combobox,
  ComboboxButton,
  ComboboxInput,
  ComboboxOption,
  ComboboxOptions,
  Description,
  Field,
  Label,
} from "@headlessui/react";
import { Icon } from "./ui";
import "./city-select.css";

type Location = {
  id: string;
  name: string;
  province: string;
  arabic: string;
  search: string;
  value: string;
};
const normalize = (text: string) =>
  text
    .normalize("NFD")
    .replace(/[\u0300-\u036f\u064b-\u065f]/g, "")
    .toLocaleLowerCase("fr")
    .replace(/[’'\s-]+/g, " ")
    .trim();
let catalogue: Promise<Location[]> | undefined;
function loadCatalogue() {
  catalogue ??= import("../data/moroccan-locations.json")
    .then(({ default: rows }) => {
      const names = new Map<string, number>();
      rows.forEach((row) =>
        names.set(normalize(row.name), (names.get(normalize(row.name)) || 0) + 1),
      );
      const popular = [
        "Casablanca",
        "Rabat",
        "Agadir",
        "Marrakech",
        "Tanger",
        "Fès",
        "Meknès",
        "Kénitra",
        "Salé",
        "Oujda",
        "Tétouan",
        "Safi",
        "El Jadida",
        "Dakhla",
      ];
      return rows
        .map((row) => ({
          ...row,
          value: names.get(normalize(row.name))! > 1 ? `${row.name} (${row.province})` : row.name,
          search: normalize(`${row.name} ${row.province} ${row.arabic}`),
        }))
        .sort((a, b) => {
          const ai = popular.indexOf(a.name),
            bi = popular.indexOf(b.name);
          return (
            (ai < 0 ? popular.length : ai) - (bi < 0 ? popular.length : bi) ||
            a.name.localeCompare(b.name, "fr")
          );
        });
    })
    .catch((error) => {
      catalogue = undefined;
      throw error;
    });
  return catalogue;
}

export function CitySelect({
  label,
  name,
  value,
  onChange,
  disabled = false,
  error,
}: {
  label: string;
  name: string;
  value: string;
  onChange: (value: string) => void;
  disabled?: boolean;
  error?: string;
}) {
  const [locations, setLocations] = useState<Location[]>([]);
  const [query, setQuery] = useState("");
  const [failed, setFailed] = useState(false);
  const [attempt, setAttempt] = useState(0);
  useEffect(() => {
    let active = true;
    setFailed(false);
    loadCatalogue().then(
      (rows) => {
        if (active) setLocations(rows);
      },
      () => {
        if (active) setFailed(true);
      },
    );
    return () => {
      active = false;
    };
  }, [attempt]);
  const filtered = useMemo(() => {
    const terms = normalize(query).split(" ").filter(Boolean);
    if (!terms.length) return locations;
    return locations
      .filter((row) => terms.every((term) => row.search.includes(term)))
      .sort(
        (a, b) =>
          Number(normalize(b.name).startsWith(normalize(query))) -
          Number(normalize(a.name).startsWith(normalize(query))),
      );
  }, [locations, query]);
  const selected =
    locations.find((row) => row.value === value) ||
    (value
      ? { id: `existing:${value}`, name: value, value, province: "", arabic: "", search: "" }
      : null);
  return (
    <Field as="div" className="field city-field" disabled={disabled}>
      <Label className="city-label">{label}</Label>
      <Combobox
        immediate
        by="id"
        value={selected}
        onChange={(row: Location | null) => onChange(row?.value || "")}
        onClose={() => setQuery("")}
        virtual={{ options: filtered }}
        disabled={disabled}
      >
        <div className="city-control">
          <span className="city-pin" aria-hidden="true">
            <Icon name="pin" size={17} />
          </span>
          <ComboboxInput
            name={name}
            displayValue={(row: Location | null) => row?.value || ""}
            onChange={(event) => setQuery(event.target.value)}
            placeholder="Rechercher une ville…"
            autoComplete="off"
            spellCheck={false}
            maxLength={500}
            aria-invalid={!!error}
          />
          <ComboboxButton
            className="city-toggle"
            aria-label={`Afficher les villes pour ${label.toLocaleLowerCase("fr")}`}
          >
            <Icon name="chevron" size={15} />
          </ComboboxButton>
        </div>
        <ComboboxOptions
          anchor={{ to: "bottom start", gap: 7, padding: 12 }}
          modal={false}
          className={`city-options ${filtered.length ? "" : "city-options-empty"}`}
        >
          {({ option }: { option: Location }) => (
            <ComboboxOption value={option} className="city-option">
              {({ selected: isSelected }) => (
                <>
                  <span className="city-option-pin">
                    <Icon name="pin" size={16} />
                  </span>
                  <span className="city-option-text">
                    <strong>{option.name}</strong>
                    <small>{option.province}</small>
                  </span>
                  {isSelected && <Icon name="check" size={17} />}
                </>
              )}
            </ComboboxOption>
          )}
        </ComboboxOptions>
      </Combobox>
      <Description as="small" className="city-description" aria-live="polite">
        {failed ? (
          <>
            La liste est indisponible.{" "}
            <button type="button" onClick={() => setAttempt((v) => v + 1)}>
              Réessayer
            </button>
          </>
        ) : !locations.length ? (
          "Chargement des villes…"
        ) : query && !filtered.length ? (
          "Aucune ville trouvée. Essayez un autre nom."
        ) : query ? (
          `${filtered.length.toLocaleString("fr-FR")} résultat(s) · Choisissez dans la liste`
        ) : (
          "Villes et communes du Maroc · Saisissez pour rechercher"
        )}
      </Description>
      {error && <Description as="em">{error}</Description>}
    </Field>
  );
}
