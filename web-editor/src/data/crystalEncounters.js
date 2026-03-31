// Default Pokemon Crystal encounter and trainer data
// Now powered by the unified game data export
import { getGameEncounters, getGameTrainers, getDefaultTrainerPokemon } from './gameData';
import { STATIC_ENCOUNTERS } from './staticEncounters';

let _cached = null;

function ensureCache() {
  if (!_cached) {
    const areas = getGameEncounters();

    // Append static encounter areas (kept separate since they have special UI handling)
    for (const group of STATIC_ENCOUNTERS) {
      areas.push({
        name: `[STATIC] ${group.category}`,
        isStarters: group.isStarters || false,
        slots: group.entries.map((e, i) => ({
          slotNum: i + 1,
          chance: '',
          label: e.label,
          pokemonName: e.pokemon,
          isRandom: false,
          level: e.level,
          maxLevel: 0,
          romIndex: e.romIndex ?? -1,
          starterIndex: e.starterIndex ?? -1,
        })),
      });
    }

    _cached = { areas, trainers: getGameTrainers() };
  }
}

export function getDefaultCrystalEncounters() {
  ensureCache();
  return _cached.areas.map(a => ({ ...a, slots: a.slots.map(s => ({ ...s })) }));
}

export function getDefaultSlotsForArea(areaIndex) {
  ensureCache();
  if (areaIndex < 0 || areaIndex >= _cached.areas.length) return null;
  return _cached.areas[areaIndex].slots.map(s => ({ ...s }));
}

export function getDefaultCrystalTrainers() {
  ensureCache();
  return _cached.trainers.map(t => ({
    ...t,
    pokemon: t.pokemon.map(p => ({ ...p })),
  }));
}

export { getDefaultTrainerPokemon };
