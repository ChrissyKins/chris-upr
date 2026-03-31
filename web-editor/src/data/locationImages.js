// Local image paths for Crystal locations
// Images downloaded from Bulbapedia (GS/Crystal era screenshots)
// Stored in public/images/

const IMG = '/images';

export const LOCATION_IMAGES = {
  // Towns & Cities
  'new bark town': `${IMG}/new_bark_town.png`,
  'cherrygrove city': `${IMG}/cherrygrove_city.png`,
  'violet city': `${IMG}/violet_city.png`,
  'azalea town': `${IMG}/azalea_town.png`,
  'goldenrod city': `${IMG}/goldenrod_city.png`,
  'ecruteak city': `${IMG}/ecruteak_city.png`,
  'olivine city': `${IMG}/olivine_city.png`,
  'cianwood city': `${IMG}/cianwood_city.png`,
  'mahogany town': `${IMG}/mahogany_town.png`,
  'blackthorn city': `${IMG}/blackthorn_city.png`,

  // Johto Routes
  'route 29': `${IMG}/route_29.png`,
  'route 30': `${IMG}/route_30.png`,
  'route 31': `${IMG}/route_31.png`,
  'route 32': `${IMG}/route_32.png`,
  'route 33': `${IMG}/route_33.png`,
  'route 34': `${IMG}/route_34.png`,
  'route 35': `${IMG}/route_35.png`,
  'route 36': `${IMG}/route_36.png`,
  'route 37': `${IMG}/route_37.png`,
  'route 38': `${IMG}/route_38.png`,
  'route 39': `${IMG}/route_39.png`,
  'route 40': `${IMG}/route_40.png`,
  'route 41': `${IMG}/route_40.png`, // No dedicated image, use Route 40 (adjacent sea route)
  'route 42': `${IMG}/route_42.png`,
  'route 43': `${IMG}/route_43.png`,
  'route 44': `${IMG}/route_44.png`,
  'route 45': `${IMG}/route_45.png`,
  'route 46': `${IMG}/route_46.png`,

  // Johto Dungeons & Landmarks
  'sprout tower': `${IMG}/sprout_tower.png`,
  'ruins of alph': `${IMG}/ruins_of_alph.png`,
  'union cave': `${IMG}/union_cave.png`,
  'slowpoke well': `${IMG}/slowpoke_well.png`,
  'ilex forest': `${IMG}/ilex_forest.png`,
  'national park': `${IMG}/national_park.png`,
  'burned tower': `${IMG}/burned_tower.png`,
  'tin tower': `${IMG}/bell_tower.png`,
  'bell tower': `${IMG}/bell_tower.png`,
  'whirl islands': `${IMG}/whirl_islands.png`,
  'mt.mortar': `${IMG}/mt_mortar.png`,
  'mt. mortar': `${IMG}/mt_mortar.png`,
  'lake of rage': `${IMG}/lake_of_rage.png`,
  'ice path': `${IMG}/ice_path.png`,
  'dark cave': `${IMG}/dark_cave_1.png`,
  "dragon's den": `${IMG}/dragons_den.png`,
  'silver cave': `${IMG}/silver_cave.png`,

  // Kanto Routes
  'route 1': `${IMG}/route_1.png`,
  'route 2': `${IMG}/route_2.png`,
  'route 3': `${IMG}/route_3.png`,
  'route 4': `${IMG}/route_4.png`,
  'route 5': `${IMG}/route_5.png`,
  'route 6': `${IMG}/route_6.png`,
  'route 7': `${IMG}/route_7.png`,
  'route 8': `${IMG}/route_8.png`,
  'route 9': `${IMG}/route_9.png`,
  'route 10': `${IMG}/route_10.png`,
  'route 11': `${IMG}/route_11.png`,
  'route 12': `${IMG}/route_12.png`,
  'route 13': `${IMG}/route_13.png`,
  'route 14': `${IMG}/route_14.png`,
  'route 15': `${IMG}/route_15.png`,
  'route 16': `${IMG}/route_16.png`,
  'route 17': `${IMG}/route_17.png`,
  'route 18': `${IMG}/route_18.png`,
  'route 19': `${IMG}/route_19.png`,
  'route 20': `${IMG}/route_20.png`,
  'route 21': `${IMG}/route_21.png`,
  'route 22': `${IMG}/route_22.png`,
  'route 24': `${IMG}/route_24.png`,
  'route 25': `${IMG}/route_25.png`,
  'route 26': `${IMG}/route_26.png`,
  'route 27': `${IMG}/route_27.png`,
  'route 28': `${IMG}/route_28.png`,

  // Kanto Towns & Cities
  'pallet town': `${IMG}/pallet_town.png`,
  'viridian city': `${IMG}/viridian_city.png`,
  'pewter city': `${IMG}/pewter_city.png`,
  'cerulean city': `${IMG}/cerulean_city.png`,
  'vermilion city': `${IMG}/vermilion_city.png`,
  'lavender town': `${IMG}/lavender_town.png`,
  'celadon city': `${IMG}/celadon_city.png`,
  'fuchsia city': `${IMG}/fuchsia_city.png`,
  'saffron city': `${IMG}/saffron_city.png`,
  'cinnabar island': `${IMG}/cinnabar_island.png`,

  // Kanto Dungeons & Landmarks
  'mt.moon': `${IMG}/mt_moon.png`,
  'rock tunnel': `${IMG}/rock_tunnel.png`,
  'victory road': `${IMG}/victory_road.png`,
  "diglett's cave": `${IMG}/digletts_cave.png`,
  'tohjo falls': `${IMG}/tohjo_falls.png`,
  'seafoam islands': `${IMG}/seafoam_islands.png`,

  // Fallbacks for grouped encounter types
  'fishing': `${IMG}/olivine_city.png`,
  'headbutt': `${IMG}/ilex_forest.png`,
  'bug catching contest': `${IMG}/national_park.png`,
};

// Per-floor images for multi-floor dungeons.
// Arrays are ordered to match the encounter file order (Grass/Cave entries).
// Surfing entries share the same location image.
export const FLOOR_IMAGES = {
  'sprout tower': [
    `${IMG}/sprout_tower.png`,       // 2F
    `${IMG}/sprout_tower_3f.png`,    // 3F
  ],
  'tin tower': [
    `${IMG}/bell_tower.png`,         // 2F
    `${IMG}/tin_tower_2f.png`,       // 3F
    `${IMG}/tin_tower_3f.png`,       // 4F
    `${IMG}/tin_tower_4f.png`,       // 5F
    `${IMG}/tin_tower_5f.png`,       // 6F
    `${IMG}/tin_tower_6f.png`,       // 7F
    `${IMG}/tin_tower_7f.png`,       // 8F
    `${IMG}/tin_tower_8f.png`,       // 9F
  ],
  'burned tower': [
    `${IMG}/burned_tower.png`,       // 1F
    `${IMG}/burned_tower_b1f.png`,   // B1F
  ],
  'ruins of alph': [
    `${IMG}/ruins_of_alph.png`,      // Outside
    `${IMG}/ruins_of_alph.png`,      // Inner (Unown chambers)
  ],
  'union cave': [
    `${IMG}/union_cave.png`,         // 1F
    `${IMG}/union_cave_b1f.png`,     // B1F
    `${IMG}/union_cave_b2f.png`,     // B2F
  ],
  'slowpoke well': [
    `${IMG}/slowpoke_well.png`,      // B1F
    `${IMG}/slowpoke_well_b2f.png`,  // B2F
  ],
  'mt.mortar': [
    `${IMG}/mt_mortar.png`,          // 1F Front
    `${IMG}/mt_mortar_back.png`,     // 1F Back
    `${IMG}/mt_mortar_b1f.png`,      // B1F
    `${IMG}/mt_mortar_2f.png`,       // 2F
  ],
  'ice path': [
    `${IMG}/ice_path.png`,           // 1F
    `${IMG}/ice_path_b1f.png`,       // B1F
    `${IMG}/ice_path_b2f.png`,       // B2F
    `${IMG}/ice_path_b3f.png`,       // B3F
    `${IMG}/ice_path_b2f.png`,       // B2F (Blackthorn side)
  ],
  'whirl islands': [
    `${IMG}/whirl_islands.png`,            // NW 1F
    `${IMG}/whirl_islands_nw.png`,         // NE 1F
    `${IMG}/whirl_islands_sw.png`,         // SW 1F
    `${IMG}/whirl_islands_se.png`,         // SE 1F
    `${IMG}/whirl_islands_b1f.png`,        // B1F
    `${IMG}/whirl_islands_b2f.png`,        // B2F
    `${IMG}/whirl_islands_b2f_inner.png`,  // B2F Inner
    `${IMG}/whirl_islands_basement.png`,   // Basement
  ],
  'silver cave': [
    `${IMG}/silver_cave.png`,        // 1F
    `${IMG}/silver_cave_2f.png`,     // 2F
    `${IMG}/silver_cave_3f.png`,     // 3F Room 1
    `${IMG}/silver_cave_3f.png`,     // 3F Room 2
  ],
  'dark cave': [
    `${IMG}/dark_cave_1.png`,        // Violet City side
    `${IMG}/dark_cave_2.png`,        // Blackthorn side
  ],
  'rock tunnel': [
    `${IMG}/rock_tunnel.png`,        // 1F
    `${IMG}/rock_tunnel_b1f.png`,    // B1F
  ],
};

/**
 * Get the floor image for a specific sub-area index within a location.
 * floorIndex is the index among same-type sub-areas (e.g. among Grass/Cave entries).
 */
export function getFloorImage(locationKey, floorIndex) {
  const key = locationKey.toLowerCase();
  const floors = FLOOR_IMAGES[key];
  if (!floors) return null;
  return floors[floorIndex] || floors[0] || null;
}

/**
 * Find the best matching image for an area name.
 * Handles both Title Case and UPPERCASE area names.
 */
export function getImageForArea(areaName) {
  // Strip encounter type suffix to get the location name
  const loc = areaName
    .replace(/ Grass\/Cave.*$/, '')
    .replace(/ Surfing$/, '')
    .replace(/ \d+F$/, '')
    .replace(/ B\d+F.*$/, '')
    .replace(/ Room \d+$/, '')
    .replace(/ \(.*\)$/, '')
    .trim()
    .toLowerCase();

  // Direct match
  if (LOCATION_IMAGES[loc]) return LOCATION_IMAGES[loc];

  // Prefix match
  for (const [key, url] of Object.entries(LOCATION_IMAGES)) {
    if (loc.startsWith(key)) return url;
  }

  // Special cases
  const lower = areaName.toLowerCase();
  if (lower.includes('fishing')) return LOCATION_IMAGES['fishing'];
  if (lower.includes('headbutt')) return LOCATION_IMAGES['headbutt'];
  if (lower.includes('bug catching')) return LOCATION_IMAGES['bug catching contest'];
  if (lower.includes('[static]')) return null; // static encounters use placeholder

  return null;
}
