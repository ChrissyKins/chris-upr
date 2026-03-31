// Pokemon Crystal static/gift encounters
// These are fixed Pokemon encounters, not random wild encounters
// romIndex values correspond to the order in Gen2RomHandler.getStaticPokemon()
// Starters use starterIndex instead (separate ROM mechanism)

export const STATIC_ENCOUNTERS = [
  {
    category: 'Starters',
    isStarters: true,
    entries: [
      { label: 'Starter 1', pokemon: 'Chikorita', level: 5, starterIndex: 0 },
      { label: 'Starter 2', pokemon: 'Cyndaquil', level: 5, starterIndex: 1 },
      { label: 'Starter 3', pokemon: 'Totodile', level: 5, starterIndex: 2 },
    ],
  },
  {
    category: 'Gift Pokemon',
    entries: [
      { label: 'Togepi Egg', pokemon: 'Togepi', level: 1, romIndex: 14 },
      { label: 'Eevee (Bill)', pokemon: 'Eevee', level: 20, romIndex: 16 },
      { label: 'Shuckie (Shuckle)', pokemon: 'Shuckle', level: 15, romIndex: 12 },
      { label: 'Tyrogue (Karate King)', pokemon: 'Tyrogue', level: 10, romIndex: 13 },
      { label: 'Kenya (Spearow)', pokemon: 'Spearow', level: 10, romIndex: 15 },
      { label: 'Odd Egg', pokemon: 'Pichu', level: 1, romIndex: -1 },
    ],
  },
  {
    category: 'Overworld Pokemon',
    entries: [
      { label: 'Red Gyarados', pokemon: 'Gyarados', level: 30, romIndex: 5 },
      { label: 'Sudowoodo', pokemon: 'Sudowoodo', level: 20, romIndex: 6 },
      { label: 'Snorlax', pokemon: 'Snorlax', level: 50, romIndex: 7 },
      { label: 'Lapras (Union Cave)', pokemon: 'Lapras', level: 20, romIndex: 0 },
      { label: 'Electrode 1 (Rocket HQ)', pokemon: 'Electrode', level: 23, romIndex: 1 },
      { label: 'Electrode 2 (Rocket HQ)', pokemon: 'Electrode', level: 23, romIndex: 2 },
      { label: 'Electrode 3 (Rocket HQ)', pokemon: 'Electrode', level: 23, romIndex: 3 },
    ],
  },
  {
    category: 'Legendaries',
    entries: [
      { label: 'Suicune', pokemon: 'Suicune', level: 40, romIndex: 19 },
      { label: 'Raikou (Roaming)', pokemon: 'Raikou', level: 40, romIndex: 17 },
      { label: 'Entei (Roaming)', pokemon: 'Entei', level: 40, romIndex: 18 },
      { label: 'Ho-Oh', pokemon: 'Ho-Oh', level: 60, romIndex: 8 },
      { label: 'Lugia', pokemon: 'Lugia', level: 60, romIndex: 4 },
    ],
  },
  {
    category: 'Game Corner',
    entries: [
      { label: 'Abra (Goldenrod)', pokemon: 'Abra', level: 5, romIndex: 20 },
      { label: 'Ekans (Goldenrod)', pokemon: 'Ekans', level: 5, romIndex: 21 },
      { label: 'Dratini (Goldenrod)', pokemon: 'Dratini', level: 5, romIndex: 22 },
      { label: 'Mr. Mime (Celadon)', pokemon: 'Mr. Mime', level: 5, romIndex: 23 },
      { label: 'Eevee (Celadon)', pokemon: 'Eevee', level: 5, romIndex: 24 },
      { label: 'Porygon (Celadon)', pokemon: 'Porygon', level: 5, romIndex: 25 },
    ],
  },
];
