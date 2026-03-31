import { POKEMON_BY_NAME, POKEMON_BY_ID } from './pokemon';
import { expandTimeOfDay } from './gameData';

function nameToId(name) {
  if (!name) return 0;
  const pk = POKEMON_BY_NAME[name.toUpperCase()];
  return pk ? pk.id : 0;
}

function idToName(id) {
  if (!id || id === 0) return { name: '', isRandom: true };
  const pk = POKEMON_BY_ID[id];
  return pk ? { name: pk.name, isRandom: false } : { name: '', isRandom: true };
}

/**
 * Export structured data as JSON using pokemon IDs.
 * 0 = RANDOM (let the randomizer pick).
 */
export function exportJSON(areas, trainers) {
  const expandedAreas = expandTimeOfDay(areas);

  const jsonAreas = expandedAreas.map(area => {
    const isStatic = area.name.startsWith('[STATIC]');
    const isStarters = area.isStarters || area.name === '[STATIC] Starters';

    return {
      name: area.name,
      slots: area.slots.map(slot => {
        const entry = {};

        if (isStatic && !isStarters && slot.romIndex >= 0) {
          entry.slot = slot.romIndex + 1;
        } else if (isStarters) {
          entry.slot = slot.starterIndex >= 0 ? slot.starterIndex + 1 : slot.slotNum;
        } else {
          entry.slot = slot.slotNum;
        }

        if (slot.label) entry.label = slot.label;

        entry.pokemon = slot.isRandom || !slot.pokemonName ? 0 : nameToId(slot.pokemonName);
        entry.level = slot.level;
        if (slot.maxLevel > 0) entry.maxLevel = slot.maxLevel;

        return entry;
      }),
    };
  });

  const jsonTrainers = (trainers || []).map(trainer => ({
    index: trainer.index,
    name: trainer.displayName,
    ...(trainer.tag ? { tag: trainer.tag } : {}),
    pokemon: trainer.pokemon.map(poke => {
      const entry = {
        slot: poke.slotNum,
        pokemon: poke.isRandom || !poke.pokemonName ? 0 : nameToId(poke.pokemonName),
        level: poke.level,
      };
      if (poke.item) entry.item = poke.item;
      if (poke.moves && poke.moves.length > 0) entry.moves = poke.moves;
      return entry;
    }),
  }));

  return {
    format: 'pokemon-crystal-custom',
    version: 2,
    encounters: jsonAreas,
    trainers: jsonTrainers,
  };
}

/**
 * Parse a JSON object back into { areas, trainers }.
 * Pokemon are referenced by national dex ID; 0 = RANDOM.
 */
export function parseJSON(json) {
  const data = typeof json === 'string' ? JSON.parse(json) : json;

  const areas = (data.encounters || []).map(area => {
    const isStatic = area.name.startsWith('[STATIC]');
    const isStarters = area.name === '[STATIC] Starters';

    return {
      name: area.name,
      ...(isStarters ? { isStarters: true } : {}),
      slots: (area.slots || []).map((slot, i) => {
        const resolved = idToName(slot.pokemon);
        const entry = {
          slotNum: isStatic ? i + 1 : (slot.slot || i + 1),
          pokemonName: resolved.name,
          isRandom: resolved.isRandom,
          level: slot.level || 5,
          maxLevel: slot.maxLevel || 0,
        };
        if (slot.label) entry.label = slot.label;
        if (isStatic && !isStarters) {
          entry.romIndex = (slot.slot || i + 1) - 1;
          entry.starterIndex = -1;
        }
        if (isStarters) {
          entry.starterIndex = (slot.slot || i + 1) - 1;
          entry.romIndex = -1;
        }
        return entry;
      }),
    };
  });

  const trainers = (data.trainers || []).map(trainer => ({
    index: trainer.index,
    displayName: trainer.name,
    tag: trainer.tag || null,
    pokemon: (trainer.pokemon || []).map(poke => {
      const resolved = idToName(poke.pokemon);
      return {
        slotNum: poke.slot,
        pokemonName: resolved.name,
        isRandom: resolved.isRandom,
        level: poke.level || 5,
        item: poke.item || null,
        moves: poke.moves || null,
      };
    }),
  }));

  return { areas, trainers };
}
