// Maps trainer index -> location name
// Combines ROM-extracted locations (from game data export) with tag-based inference
import { getTrainerLocationMap } from './gameData';

// Tag -> location inference for trainers not found on map person events
const TAG_LOCATIONS = {
  'GYM1': 'Violet City',
  'GYM2': 'Azalea Town',
  'GYM3': 'Goldenrod City',
  'GYM4': 'Ecruteak City',
  'GYM5': 'Cianwood City',
  'GYM6': 'Olivine City',
  'GYM7': 'Mahogany Town',
  'GYM8': 'Blackthorn City',
  'GYM9': 'Pewter City',
  'GYM10': 'Cerulean City',
  'GYM11': 'Vermilion City',
  'GYM12': 'Celadon City',
  'GYM13': 'Fuchsia City',
  'GYM14': 'Saffron City',
  'GYM15': 'Seafoam Islands',
  'GYM16': 'Viridian City',
  'ELITE1': 'Indigo Plateau',
  'ELITE2': 'Indigo Plateau',
  'ELITE3': 'Indigo Plateau',
  'ELITE4': 'Indigo Plateau',
  'CHAMPION': 'Indigo Plateau',
  'UBER': 'Mt. Silver',
  'THEMED:SPROUTTOWER': 'Sprout Tower',
  'THEMED:ARIANA': 'Team Rocket HQ',
  'THEMED:PETREL': 'Team Rocket HQ',
  'THEMED:PROTON': 'Team Rocket HQ',
};

const RIVAL_LOCATIONS = {
  'RIVAL1': 'Cherrygrove City',
  'RIVAL2': 'Azalea Town',
  'RIVAL3': 'Burned Tower',
  'RIVAL4': 'Goldenrod City',
  'RIVAL5': 'Victory Road',
  'RIVAL6': 'Mt. Moon',
  'RIVAL7': 'Indigo Plateau',
};

// Normalize ROM location names to display names
const LOCATION_NORMALIZE = {
  'SPROUT TOWER': 'Sprout Tower',
  'UNION CAVE': 'Union Cave',
  'SLOWPOKE WELL': 'Slowpoke Well',
  'ILEX FOREST': 'Ilex Forest',
  'BURNED TOWER': 'Burned Tower',
  'BELL TOWER': 'Bell Tower',
  'LIGHTHOUSE': 'Olivine City',
  'WHIRL ISLANDS': 'Whirl Islands',
  'MT. MORTAR': 'Mt. Mortar',
  'ICE PATH': 'Ice Path',
  "DRAGON'S DEN": 'Blackthorn City',
  'DARK CAVE': 'Dark Cave',
  'VICTORY ROAD': 'Victory Road',
  'TOHJO FALLS': 'Tohjo Falls',
  'NATIONAL PARK': 'National Park',
  'MT. SILVER': 'Mt. Silver',
  'LAKE OF RAGE': 'Lake of Rage',
  'ROCK TUNNEL': 'Rock Tunnel',
  'MT. MOON': 'Mt. Moon',
  'POWER PLANT': 'Power Plant',
  'SEAFOAM ISLANDS': 'Seafoam Islands',
  'TEAM ROCKET HQ': 'Team Rocket HQ',
};

function titleCase(str) {
  return str.split(' ').map(w =>
    w.charAt(0).toUpperCase() + w.slice(1).toLowerCase()
  ).join(' ');
}

function normalizeLoc(romLoc) {
  if (!romLoc) return null;
  // Check normalize table first
  if (LOCATION_NORMALIZE[romLoc]) return LOCATION_NORMALIZE[romLoc];
  // Route names just need title case
  if (romLoc.startsWith('ROUTE ')) return titleCase(romLoc);
  // City/town names
  return LOCATION_NORMALIZE[romLoc] || titleCase(romLoc);
}

let _cache = null;

function buildMap(trainers) {
  if (_cache) return _cache;

  const romLocations = getTrainerLocationMap();
  const map = {};

  // 1. Apply ROM-extracted locations
  for (const [idx, loc] of Object.entries(romLocations)) {
    const normalized = normalizeLoc(loc);
    if (normalized) {
      map[parseInt(idx)] = normalized;
    }
  }

  // 2. Apply tag-based locations for unmapped trainers
  if (trainers) {
    for (const t of trainers) {
      if (map[t.index]) continue;
      if (!t.tag) continue;

      if (TAG_LOCATIONS[t.tag]) {
        map[t.index] = TAG_LOCATIONS[t.tag];
        continue;
      }

      const rivalMatch = t.tag.match(/^(RIVAL\d+)-/);
      if (rivalMatch && RIVAL_LOCATIONS[rivalMatch[1]]) {
        map[t.index] = RIVAL_LOCATIONS[rivalMatch[1]];
      }
    }
  }

  _cache = map;
  return map;
}

/**
 * Get the location name for a trainer by index.
 * Pass trainers array for tag-based inference.
 */
export function getTrainerLocation(trainerIndex, trainers) {
  return buildMap(trainers)[trainerIndex] || null;
}

/**
 * Group trainers by location.
 */
export function groupTrainersByLocation(trainers) {
  const map = buildMap(trainers);
  const groups = new Map();

  for (const trainer of trainers) {
    const location = map[trainer.index] || 'Other';
    if (!groups.has(location)) {
      groups.set(location, []);
    }
    groups.get(location).push(trainer);
  }

  return groups;
}
