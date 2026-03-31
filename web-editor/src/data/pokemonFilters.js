// Pokemon filter helpers using game data
import { getGamePokemon, getGameEvolutions } from './gameData';

let _cache = null;

function buildCache() {
  if (_cache) return _cache;

  const pokemon = getGamePokemon();
  const evolutions = getGameEvolutions();

  // Build evolution stage map
  const evolvesTo = {};   // fromId -> [toId]
  const evolvesFrom = {}; // toId -> fromId
  for (const e of evolutions) {
    if (!evolvesTo[e.fromId]) evolvesTo[e.fromId] = [];
    evolvesTo[e.fromId].push(e.toId);
    evolvesFrom[e.toId] = e.fromId;
  }

  const stageMap = {};  // pokemonId -> 1, 2, or 3
  for (const p of pokemon) {
    const id = p.id;
    if (!(id in evolvesFrom) && !(id in evolvesTo)) {
      stageMap[id] = 1; // standalone (no evolution chain)
    } else if (!(id in evolvesFrom)) {
      stageMap[id] = 1; // base form
    } else if (!(evolvesFrom[id] in evolvesFrom)) {
      stageMap[id] = 2; // second stage
    } else {
      stageMap[id] = 3; // third stage
    }
  }

  // Gen 2 legendaries
  const LEGENDARIES = new Set([
    144, 145, 146, // Birds
    150, 151,       // Mewtwo, Mew
    243, 244, 245, // Beasts
    249, 250,       // Lugia, Ho-Oh
    251,            // Celebi
  ]);

  // Build "fully evolved" set: Pokemon that don't evolve into anything
  const fullyEvolved = new Set();
  for (const p of pokemon) {
    if (!(p.id in evolvesTo)) {
      fullyEvolved.add(p.id);
    }
  }

  _cache = { stageMap, legendaries: LEGENDARIES, fullyEvolved };
  return _cache;
}

export function getEvolutionStage(pokemonId) {
  return buildCache().stageMap[pokemonId] || 1;
}

export function isLegendary(pokemonId) {
  return buildCache().legendaries.has(pokemonId);
}

export function isFullyEvolved(pokemonId) {
  return buildCache().fullyEvolved.has(pokemonId);
}

export const ALL_TYPES = [
  'NORMAL', 'FIRE', 'WATER', 'GRASS', 'ELECTRIC', 'ICE',
  'FIGHTING', 'POISON', 'GROUND', 'FLYING', 'PSYCHIC',
  'BUG', 'ROCK', 'GHOST', 'DRAGON', 'DARK', 'STEEL',
];

/**
 * Filter a Pokemon list based on criteria.
 */
export function filterPokemon(pokemonList, filters) {
  const { stageMap, legendaries, fullyEvolved } = buildCache();

  return pokemonList.filter(p => {
    // Type filter (multi-select: Pokemon must match at least one selected type)
    if (filters.types && filters.types.length > 0) {
      const hasMatch = filters.types.some(t => {
        const tu = t.toUpperCase();
        return p.type1 === tu || p.type2 === tu;
      });
      if (!hasMatch) return false;
    }

    // Evolution stage filter
    if (filters.stage && filters.stage !== 'any') {
      const stage = stageMap[p.id] || 1;
      if (stage !== parseInt(filters.stage)) return false;
    }

    // Legendary filter
    if (filters.legendary === 'none' && legendaries.has(p.id)) return false;
    if (filters.legendary === 'only' && !legendaries.has(p.id)) return false;

    // Fully evolved filter
    if (filters.evolved === 'fully' && !fullyEvolved.has(p.id)) return false;
    if (filters.evolved === 'not_fully' && fullyEvolved.has(p.id)) return false;

    // BST range filter (multi-select: Pokemon must fall in at least one selected range)
    if (filters.bstRanges && filters.bstRanges.length > 0) {
      const inRange = filters.bstRanges.some(r => p.bst >= r.min && p.bst <= r.max);
      if (!inRange) return false;
    }

    return true;
  });
}
