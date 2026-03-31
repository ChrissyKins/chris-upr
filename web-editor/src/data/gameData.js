// Unified game data loaded from the comprehensive ROM export
// This replaces scattered data files (defaultEncounters.txt, staticEncounters.js, trainerLocations.json)
import rawData from './gamedata_byte.json';
import { POKEMON, POKEMON_BY_NAME } from './pokemon';

// ── Name resolution helpers ──

const ROM_TO_DISPLAY = {};
for (const p of POKEMON) {
  ROM_TO_DISPLAY[p.name.toUpperCase()] = p.name;
}
// Special cases from ROM names
ROM_TO_DISPLAY['NIDORAN?'] = 'Nidoran♀'; // default, context-dependent
ROM_TO_DISPLAY['MR.MIME'] = 'Mr. Mime';
ROM_TO_DISPLAY["FARFETCH'D"] = "Farfetch'd";
ROM_TO_DISPLAY['HO-OH'] = 'Ho-Oh';

function resolveRomName(romName) {
  if (!romName) return '';
  return ROM_TO_DISPLAY[romName.toUpperCase()] || romName;
}

// ── Cached data ──

let _pokemon = null;
let _trainers = null;
let _encounters = null;
let _statics = null;
let _moves = null;
let _items = null;
let _learnsets = null;
let _evolutions = null;
let _tms = null;
let _hms = null;
let _starters = null;
let _trainerLocations = null;

// ── Pokemon ──

export function getGamePokemon() {
  if (!_pokemon) {
    _pokemon = rawData.pokemon.map(p => ({
      id: p.id,
      name: resolveRomName(p.name),
      type1: p.type1,
      type2: p.type2,
      hp: p.hp, atk: p.atk, def: p.def,
      spatk: p.spatk, spdef: p.spdef, speed: p.speed,
      bst: p.bst,
    }));
  }
  return _pokemon;
}

// ── Trainers ──

export function getGameTrainers() {
  if (!_trainers) {
    _trainers = rawData.trainers.map(t => ({
      index: t.index,
      classId: t.classId,
      name: t.name,
      fullName: t.fullName,
      displayName: t.fullName || `${t.class || ''} ${t.name || ''}`.trim(),
      tag: t.tag,
      pokemon: t.pokemon.map((p, i) => {
        const pokeName = resolveRomName(p.pokemon);
        // Use ROM moves if present, otherwise compute default moveset from learnset
        let moves = null;
        if (p.moves && p.moves.length > 0) {
          moves = [...p.moves];
          while (moves.length < 4) moves.push(null);
        } else {
          // Compute default moveset for this Pokemon at this level
          const pokObj = POKEMON_BY_NAME[pokeName.toUpperCase()];
          if (pokObj) {
            const ls = rawData.learnsets[String(pokObj.id)];
            if (ls) {
              const learned = [];
              for (const entry of ls) {
                if (entry.level <= p.level) {
                  learned.push(entry.move);
                  if (learned.length > 4) learned.shift();
                }
              }
              while (learned.length < 4) learned.push(null);
              moves = learned;
            }
          }
        }
        return {
          slotNum: i + 1,
          pokemonName: pokeName,
          isRandom: false,
          level: p.level,
          item: p.item || null,
          moves,
        };
      }),
    }));
  }
  return _trainers.map(t => ({ ...t, pokemon: t.pokemon.map(p => ({ ...p })) }));
}

export function getDefaultTrainerPokemon(trainerIndex) {
  if (!_trainers) getGameTrainers();
  const t = _trainers.find(t => t.index === trainerIndex);
  if (!t) return null;
  return t.pokemon.map(p => ({ ...p }));
}

// ── Trainer locations ──

export function getTrainerLocationMap() {
  if (!_trainerLocations) {
    _trainerLocations = {};
    for (const [idx, loc] of Object.entries(rawData.trainerLocations)) {
      _trainerLocations[parseInt(idx)] = loc;
    }
  }
  return _trainerLocations;
}

// ── Encounters (converted to the area/slot format the editor expects) ──

export function getGameEncounters() {
  if (!_encounters) {
    const LAND_CHANCES = ['30%', '30%', '20%', '10%', '5%', '4%', '1%'];
    const SEA_CHANCES = ['60%', '30%', '10%'];

    // Collapse Day/Morning/Night into one area (show Day only, strip suffix)
    const collapsed = [];
    const seenBases = new Set();

    for (const es of rawData.encounters) {
      const name = es.name || '';
      // Skip Morning/Night variants — Day covers them
      if (name.includes('(Morning)') || name.includes('(Night)')) continue;

      // Strip "(Day)" suffix for display
      const displayName = name.replace(' (Day)', '');
      const nameLower = displayName.toLowerCase();
      let chances = [];
      if (nameLower.includes('grass') || nameLower.includes('cave')) chances = LAND_CHANCES;
      else if (nameLower.includes('surfing')) chances = SEA_CHANCES;

      collapsed.push({
        name: displayName,
        originalName: name,
        slots: es.encounters.map((enc, i) => ({
          slotNum: i + 1,
          chance: i < chances.length ? chances[i] : '',
          pokemonName: resolveRomName(enc.pokemon),
          isRandom: false,
          level: enc.level,
          maxLevel: enc.maxLevel || 0,
        })),
      });
    }

    _encounters = collapsed;
  }
  return _encounters.map(a => ({ ...a, slots: a.slots.map(s => ({ ...s })) }));
}

/**
 * Expand collapsed areas back to Day/Morning/Night variants for export.
 * Each area's slots are replicated to all three time variants.
 */
export function expandTimeOfDay(collapsedAreas) {
  const expanded = [];
  for (const area of collapsedAreas) {
    const baseName = area.name;
    // Check if this was originally a time-of-day area
    const origName = area.originalName || baseName;
    if (origName.includes('(Day)')) {
      // Create Day/Morning/Night variants with same slots
      for (const suffix of [' (Day)', ' (Morning)', ' (Night)']) {
        expanded.push({
          ...area,
          name: baseName + suffix,
          slots: area.slots.map(s => ({ ...s })),
        });
      }
    } else {
      expanded.push({ ...area, slots: area.slots.map(s => ({ ...s })) });
    }
  }
  return expanded;
}

// ── Static Pokemon ──

export function getGameStatics() {
  if (!_statics) {
    _statics = rawData.staticPokemon.map(s => ({
      index: s.index,
      pokemon: resolveRomName(s.pokemon),
      level: s.level,
    }));
  }
  return _statics.map(s => ({ ...s }));
}

// ── Moves ──

export function getGameMoves() {
  if (!_moves) {
    _moves = rawData.moves.map(m => ({
      id: m.id,
      name: m.name,
      type: m.type,
      power: m.power,
      accuracy: m.accuracy,
      pp: m.pp,
    }));
  }
  return _moves;
}

// ── Items ──

export function getGameItems() {
  if (!_items) {
    _items = rawData.items.map(item => ({
      id: item.id,
      name: item.name,
    }));
  }
  return _items;
}

// ── Learnsets ──

export function getLearnset(pokemonId) {
  if (!_learnsets) {
    _learnsets = {};
    for (const [id, moves] of Object.entries(rawData.learnsets)) {
      _learnsets[parseInt(id)] = moves.map(m => ({
        move: m.move,
        moveId: m.moveId,
        level: m.level,
      }));
    }
  }
  return _learnsets[pokemonId] || [];
}

// ── Evolutions ──

export function getGameEvolutions() {
  if (!_evolutions) {
    _evolutions = rawData.evolutions.map(e => ({
      from: resolveRomName(e.from),
      fromId: e.fromId,
      to: resolveRomName(e.to),
      toId: e.toId,
      method: e.method,
      detail: e.detail,
    }));
  }
  return _evolutions;
}

export function getEvolutionsFor(pokemonId) {
  return getGameEvolutions().filter(e => e.fromId === pokemonId);
}

export function getPreEvolutionOf(pokemonId) {
  return getGameEvolutions().find(e => e.toId === pokemonId) || null;
}

// ── TMs & HMs ──

export function getGameTMs() {
  if (!_tms) {
    _tms = rawData.tms.map(t => ({ tm: t.tm, move: t.move, moveId: t.moveId }));
  }
  return _tms;
}

export function getGameHMs() {
  if (!_hms) {
    _hms = rawData.hms.map(h => ({ hm: h.hm, move: h.move, moveId: h.moveId }));
  }
  return _hms;
}

// ── Starters ──

export function getGameStarters() {
  if (!_starters) {
    _starters = rawData.starters.map(s => ({
      slot: s.slot,
      pokemon: resolveRomName(s.pokemon),
      pokemonId: s.pokemonId,
    }));
  }
  return _starters;
}

// ── Metadata ──

export const GAME_NAME = rawData.game;
export const GAME_CODE = rawData.code;
export const GENERATION = rawData.generation;
