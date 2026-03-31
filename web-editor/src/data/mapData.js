// Johto and Kanto map layout data
// Positions are approximate grid coordinates matching the game's town map
// x: 0-100, y: 0-100 (percentage-based for responsive scaling)

export const MAP_LOCATIONS = [
  // === JOHTO ===
  // Eastern Johto
  { name: 'New Bark Town', x: 90, y: 52, region: 'johto', type: 'town' },
  { name: 'Route 29', x: 78, y: 52, region: 'johto', type: 'route' },
  { name: 'Cherrygrove City', x: 66, y: 52, region: 'johto', type: 'town' },
  { name: 'Route 30', x: 66, y: 42, region: 'johto', type: 'route' },
  { name: 'Route 31', x: 74, y: 36, region: 'johto', type: 'route' },
  { name: 'Violet City', x: 66, y: 30, region: 'johto', type: 'town' },
  { name: 'Sprout Tower', x: 66, y: 24, region: 'johto', type: 'dungeon', match: 'Sprout Tower' },

  // Southern Johto
  { name: 'Route 32', x: 66, y: 42, region: 'johto', type: 'route', offsetX: -8 },
  { name: 'Ruins of Alph', x: 58, y: 36, region: 'johto', type: 'dungeon', match: 'Ruins of Alph' },
  { name: 'Union Cave', x: 58, y: 52, region: 'johto', type: 'dungeon', match: 'Union Cave' },
  { name: 'Route 33', x: 50, y: 58, region: 'johto', type: 'route' },
  { name: 'Azalea Town', x: 42, y: 58, region: 'johto', type: 'town' },
  { name: 'Slowpoke Well', x: 42, y: 64, region: 'johto', type: 'dungeon', match: 'Slowpoke Well' },
  { name: 'Ilex Forest', x: 34, y: 58, region: 'johto', type: 'dungeon' },
  { name: 'Route 34', x: 34, y: 48, region: 'johto', type: 'route' },

  // Central Johto
  { name: 'Goldenrod City', x: 34, y: 36, region: 'johto', type: 'town' },
  { name: 'Route 35', x: 34, y: 26, region: 'johto', type: 'route' },
  { name: 'National Park', x: 34, y: 18, region: 'johto', type: 'route' },
  { name: 'Route 36', x: 48, y: 26, region: 'johto', type: 'route' },
  { name: 'Route 37', x: 48, y: 18, region: 'johto', type: 'route' },
  { name: 'Ecruteak City', x: 42, y: 10, region: 'johto', type: 'town' },
  { name: 'Burned Tower', x: 38, y: 6, region: 'johto', type: 'dungeon', match: 'Burned Tower' },
  { name: 'Bell Tower', x: 46, y: 6, region: 'johto', type: 'dungeon', match: 'Bell Tower' },

  // Western Johto
  { name: 'Route 38', x: 26, y: 10, region: 'johto', type: 'route' },
  { name: 'Route 39', x: 18, y: 18, region: 'johto', type: 'route' },
  { name: 'Olivine City', x: 14, y: 26, region: 'johto', type: 'town' },
  { name: 'Route 40', x: 10, y: 36, region: 'johto', type: 'route' },
  { name: 'Route 41', x: 10, y: 48, region: 'johto', type: 'route' },
  { name: 'Cianwood City', x: 6, y: 58, region: 'johto', type: 'town' },
  { name: 'Whirl Islands', x: 10, y: 42, region: 'johto', type: 'dungeon', match: 'Whirl Islands' },

  // Northern Johto
  { name: 'Route 42', x: 60, y: 10, region: 'johto', type: 'route' },
  { name: 'Mt. Mortar', x: 56, y: 6, region: 'johto', type: 'dungeon', match: 'Mt. Mortar' },
  { name: 'Mahogany Town', x: 72, y: 10, region: 'johto', type: 'town' },
  { name: 'Route 43', x: 72, y: 2, region: 'johto', type: 'route' },
  { name: 'Lake of Rage', x: 80, y: 2, region: 'johto', type: 'route' },
  { name: 'Route 44', x: 82, y: 10, region: 'johto', type: 'route' },
  { name: 'Ice Path', x: 88, y: 6, region: 'johto', type: 'dungeon', match: 'Ice Path' },
  { name: 'Blackthorn City', x: 90, y: 14, region: 'johto', type: 'town' },
  { name: "Dragon's Den", x: 94, y: 20, region: 'johto', type: 'dungeon', match: "Dragon's Den" },

  // Connecting routes
  { name: 'Route 45', x: 90, y: 28, region: 'johto', type: 'route' },
  { name: 'Route 46', x: 82, y: 36, region: 'johto', type: 'route' },
  { name: 'Dark Cave', x: 78, y: 28, region: 'johto', type: 'dungeon', match: 'Dark Cave' },

  // Silver Cave
  { name: 'Silver Cave', x: 95, y: 42, region: 'johto', type: 'dungeon', match: 'Silver Cave' },
];

// Connections between locations (for drawing path lines)
export const MAP_CONNECTIONS = [
  ['New Bark Town', 'Route 29'],
  ['Route 29', 'Cherrygrove City'],
  ['Cherrygrove City', 'Route 30'],
  ['Route 30', 'Route 31'],
  ['Route 31', 'Violet City'],
  ['Violet City', 'Sprout Tower'],
  ['Violet City', 'Route 32'],
  ['Route 32', 'Ruins of Alph'],
  ['Route 32', 'Union Cave'],
  ['Union Cave', 'Route 33'],
  ['Route 33', 'Azalea Town'],
  ['Azalea Town', 'Slowpoke Well'],
  ['Azalea Town', 'Ilex Forest'],
  ['Ilex Forest', 'Route 34'],
  ['Route 34', 'Goldenrod City'],
  ['Goldenrod City', 'Route 35'],
  ['Route 35', 'National Park'],
  ['Goldenrod City', 'Route 36'],
  ['Route 36', 'Route 37'],
  ['Route 37', 'Ecruteak City'],
  ['Ecruteak City', 'Burned Tower'],
  ['Ecruteak City', 'Bell Tower'],
  ['Ecruteak City', 'Route 38'],
  ['Route 38', 'Route 39'],
  ['Route 39', 'Olivine City'],
  ['Olivine City', 'Route 40'],
  ['Route 40', 'Whirl Islands'],
  ['Route 40', 'Route 41'],
  ['Route 41', 'Cianwood City'],
  ['Ecruteak City', 'Route 42'],
  ['Route 42', 'Mt. Mortar'],
  ['Route 42', 'Mahogany Town'],
  ['Mahogany Town', 'Route 43'],
  ['Route 43', 'Lake of Rage'],
  ['Mahogany Town', 'Route 44'],
  ['Route 44', 'Ice Path'],
  ['Ice Path', 'Blackthorn City'],
  ['Blackthorn City', "Dragon's Den"],
  ['Blackthorn City', 'Route 45'],
  ['Route 45', 'Route 46'],
  ['Route 46', 'Dark Cave'],
  ['Dark Cave', 'Violet City'],
  ['Blackthorn City', 'Silver Cave'],
];
