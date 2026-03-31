import { useState } from 'react';
import PokemonPicker, { getFilteredPokemonList } from './PokemonPicker';
import PasteLink from './PasteLink';
import { POKEMON_BY_NAME } from '../data/pokemon';
import { getLearnset, getGameMoves } from '../data/gameData';
import { getTrainerSpriteUrl } from '../data/trainerSprites';
import { copyPokemon, getClipboard } from '../data/clipboard';
import { usePokemonFilters } from '../data/pokemonFilterContext';
import { getDefaultTrainerPokemon } from '../data/crystalEncounters';

/**
 * Compute the default moveset a Pokemon would have at a given level.
 * Mimics how the game works: learn moves in order, keeping the last 4.
 */
export function getDefaultMoveset(pokemonId, level) {
  if (!pokemonId) return [];
  const learnset = getLearnset(pokemonId);
  const learned = [];
  for (const entry of learnset) {
    if (entry.level <= level) {
      learned.push(entry.move);
      if (learned.length > 4) {
        learned.shift();
      }
    }
  }
  while (learned.length < 4) learned.push(null);
  return learned;
}

function getPokemonId(name) {
  if (!name) return 0;
  const obj = POKEMON_BY_NAME[name.toUpperCase()];
  return obj?.id || 0;
}

export default function TrainerEditor({ trainer, trainerIndex, onPokemonChange }) {
  const tagLabel = trainer.tag ? ` [${trainer.tag}]` : '';
  const spriteUrl = getTrainerSpriteUrl(trainer.classId);
  const globalFilters = usePokemonFilters();

  function handleRandomize(e) {
    e.preventDefault();
    e.stopPropagation();
    const pool = getFilteredPokemonList(globalFilters);
    if (pool.length === 0) return;
    trainer.pokemon.forEach((poke, i) => {
      const randPoke = pool[Math.floor(Math.random() * pool.length)];
      const moves = getDefaultMoveset(randPoke.id, poke.level);
      onPokemonChange(trainerIndex, i, { ...poke, pokemonName: randPoke.name, isRandom: false, moves });
    });
  }

  function handlePokemonNameChange(pokeIdx, name) {
    const poke = trainer.pokemon[pokeIdx];
    const pokemonId = getPokemonId(name);
    const moves = pokemonId ? getDefaultMoveset(pokemonId, poke.level) : null;
    onPokemonChange(trainerIndex, pokeIdx, {
      ...poke,
      pokemonName: name,
      isRandom: !name,
      moves,
    });
  }

  function handleLevelChange(pokeIdx, e) {
    const val = parseInt(e.target.value);
    if (!isNaN(val) && val >= 1 && val <= 100) {
      const poke = trainer.pokemon[pokeIdx];
      onPokemonChange(trainerIndex, pokeIdx, { ...poke, level: val });
    }
  }

  return (
    <div className="trainer-editor">
      <div className="trainer-layout">
        {spriteUrl && (
          <img
            src={spriteUrl}
            alt={trainer.displayName}
            className="trainer-sprite"
            onError={(e) => { e.target.style.display = 'none'; }}
          />
        )}
        <div className="trainer-content">
          <div className="trainer-name-line">
            <b>{trainer.displayName}</b>
            <span className="trainer-tag">{tagLabel}</span>
            {' '}
            <a href="#" onClick={handleRandomize}>[Randomize]</a>
          </div>
          <div className="trainer-pokemon-list">
            {trainer.pokemon.map((poke, i) => {
              const defaults = getDefaultTrainerPokemon(trainerIndex);
              const defaultPoke = defaults ? defaults[i] : null;
              return <TrainerPokemonSlot
                key={i}
                poke={poke}
                defaultPoke={defaultPoke}
                pokeIdx={i}
                trainerIndex={trainerIndex}
                onPokemonNameChange={handlePokemonNameChange}
                onLevelChange={handleLevelChange}
                onPokemonChange={onPokemonChange}
              />;
            })}
          </div>
        </div>
      </div>
    </div>
  );
}

function TrainerPokemonSlot({ poke, defaultPoke, pokeIdx, trainerIndex, onPokemonNameChange, onLevelChange, onPokemonChange }) {
  const [expanded, setExpanded] = useState(false);
  const [justCopied, setJustCopied] = useState(false);

  const pokemonId = getPokemonId(poke.pokemonName);
  const learnset = pokemonId ? getLearnset(pokemonId) : [];
  const allMoves = getGameMoves();

  function handleMoveChange(moveSlot, moveName) {
    const currentMoves = poke.moves ? [...poke.moves] : [null, null, null, null];
    while (currentMoves.length < 4) currentMoves.push(null);
    currentMoves[moveSlot] = moveName || null;
    const hasAnyMove = currentMoves.some(m => m);
    onPokemonChange(trainerIndex, pokeIdx, {
      ...poke,
      moves: hasAnyMove ? currentMoves : null,
    });
  }

  function handleResetMoves() {
    const moves = pokemonId ? getDefaultMoveset(pokemonId, poke.level) : null;
    onPokemonChange(trainerIndex, pokeIdx, { ...poke, moves });
  }

  function handleCopy(e) {
    e.preventDefault();
    copyPokemon({ pokemonName: poke.pokemonName, isRandom: poke.isRandom });
    setJustCopied(true);
    setTimeout(() => setJustCopied(false), 800);
  }

  function handlePaste(e) {
    e.preventDefault();
    const clip = getClipboard();
    if (!clip) return;
    const newPokemonId = getPokemonId(clip.pokemonName);
    const moves = newPokemonId ? getDefaultMoveset(newPokemonId, poke.level) : null;
    onPokemonChange(trainerIndex, pokeIdx, {
      ...poke,
      pokemonName: clip.pokemonName,
      isRandom: clip.isRandom,
      moves,
    });
  }

  const currentMoves = poke.moves || [];
  const displayMoves = currentMoves.filter(Boolean);

  return (
    <div className={`trainer-poke-slot ${poke.isRandom || !poke.pokemonName ? 'slot-random' : ''}`}>
      <div className="trainer-poke-main">
        <span className="slot-number">#{pokeIdx + 1}</span>
        {' '}
        <PokemonPicker value={poke.pokemonName} onChange={(name) => onPokemonNameChange(pokeIdx, name)} />
        {' '}
        <span className="trainer-poke-actions">
          <a href="#" onClick={handleCopy}>{justCopied ? '(copied!)' : '(copy)'}</a>
          <PasteLink onPaste={handlePaste} />
        </span>
        {' '}
        <span className="level-inputs">
          Lv<input
            type="number"
            min="1"
            max="100"
            value={poke.level}
            onChange={(e) => onLevelChange(pokeIdx, e)}
            className="level-input"
          />
        </span>
        {displayMoves.length > 0 && (
          <span className="trainer-moves-preview">
            {' '}{displayMoves.join(' / ')}
          </span>
        )}
        {pokemonId > 0 && (
          <a href="#" className="trainer-expand-btn" onClick={(e) => {
            e.preventDefault();
            setExpanded(!expanded);
          }}>{expanded ? '[-]' : '[+]'}</a>
        )}
        {defaultPoke && (
          <span className="slot-actions">
            <a href="#" onClick={(e) => {
              e.preventDefault();
              onPokemonChange(trainerIndex, pokeIdx, { ...defaultPoke });
            }}>(reset)</a>
          </span>
        )}
      </div>

      {expanded && pokemonId > 0 && (
        <div className="trainer-poke-details">
          <div className="trainer-moves-editor">
            <span className="trainer-detail-label">
              Moves
              {' '}<a href="#" onClick={(e) => { e.preventDefault(); handleResetMoves(); }}>(reset to default)</a>
            </span>
            {[0, 1, 2, 3].map(slot => (
              <div key={slot} className="trainer-move-slot">
                <MovePicker
                  value={currentMoves[slot] || ''}
                  learnset={learnset}
                  allMoves={allMoves}
                  level={poke.level}
                  onChange={(name) => handleMoveChange(slot, name)}
                />
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}

function MovePicker({ value, learnset, allMoves, level, onChange }) {
  const [isOpen, setIsOpen] = useState(false);
  const [search, setSearch] = useState('');

  const learnableMoves = learnset
    .filter(m => m.level <= level)
    .sort((a, b) => b.level - a.level);

  const moveById = {};
  for (const m of allMoves) {
    moveById[m.id] = m;
  }

  const options = search
    ? allMoves.filter(m => m.name && m.name.toLowerCase().includes(search.toLowerCase()))
    : learnableMoves.map(lm => moveById[lm.moveId]).filter(Boolean);

  const currentMove = value ? allMoves.find(m => m.name === value) : null;

  return (
    <div className="move-picker">
      <div className="move-picker-display" onClick={() => setIsOpen(!isOpen)}>
        {currentMove ? (
          <>
            <span className="move-type-badge" data-type={currentMove.type?.toLowerCase()}>{currentMove.type || '?'}</span>
            <span className="move-name">{currentMove.name}</span>
            {currentMove.power > 0 && <span className="move-power">({currentMove.power})</span>}
          </>
        ) : (
          <span className="move-empty">-- empty --</span>
        )}
        <span className="dropdown-arrow">▾</span>
      </div>

      {isOpen && (
        <div className="move-dropdown">
          <input
            type="text"
            className="pokemon-search"
            placeholder="Search moves... (or browse learnable)"
            value={search}
            onChange={e => setSearch(e.target.value)}
            autoFocus
            onBlur={() => setTimeout(() => setIsOpen(false), 200)}
          />
          <div className="move-list">
            <div className="pokemon-option" onClick={() => { onChange(''); setIsOpen(false); setSearch(''); }}>
              <span className="move-empty">-- clear --</span>
            </div>
            {options.map((m) => (
              <div
                key={m.id}
                className={`pokemon-option ${value === m.name ? 'selected' : ''}`}
                onClick={() => { onChange(m.name); setIsOpen(false); setSearch(''); }}
              >
                <span className="move-type-badge" data-type={m.type?.toLowerCase()}>{m.type || '?'}</span>
                {' '}{m.name}
                {m.power > 0 && <span className="move-power"> ({m.power})</span>}
              </div>
            ))}
            {options.length === 0 && (
              <div className="pokemon-option"><span className="move-empty">No moves found</span></div>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
