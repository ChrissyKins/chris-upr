import { useState, useRef, useEffect, useMemo } from 'react';
import { POKEMON, POKEMON_BY_NAME, getSpriteUrl } from '../data/pokemon';
import { getGamePokemon } from '../data/gameData';
import { getEvolutionStage, isLegendary, filterPokemon } from '../data/pokemonFilters';
import { usePokemonFilters } from '../data/pokemonFilterContext';

// Build enriched Pokemon list once
let _enriched = null;
function getEnrichedPokemon() {
  if (!_enriched) {
    const gameData = getGamePokemon();
    const gameMap = {};
    for (const g of gameData) gameMap[g.id] = g;
    _enriched = POKEMON.map(p => ({
      ...p,
      type1: gameMap[p.id]?.type1 || null,
      type2: gameMap[p.id]?.type2 || null,
      bst: gameMap[p.id]?.bst || 0,
      stage: getEvolutionStage(p.id),
      legendary: isLegendary(p.id),
    }));
  }
  return _enriched;
}

export function getFilteredPokemonList(filters) {
  return filterPokemon(getEnrichedPokemon(), filters);
}

export default function PokemonPicker({ value, onChange }) {
  const [search, setSearch] = useState('');
  const [isOpen, setIsOpen] = useState(false);
  const [highlightIndex, setHighlightIndex] = useState(0);
  const wrapperRef = useRef(null);
  const listRef = useRef(null);
  const globalFilters = usePokemonFilters();

  const allPokemon = getEnrichedPokemon();

  const selectedPokemon = value
    ? POKEMON_BY_NAME[value.toUpperCase()] || null
    : null;

  const filtered = useMemo(() => {
    let list = filterPokemon(allPokemon, globalFilters);
    if (search) {
      list = list.filter(p => p.name.toLowerCase().includes(search.toLowerCase()));
    }
    return list;
  }, [search, globalFilters, allPokemon]);

  useEffect(() => {
    if (!isOpen) return;
    function handleClickOutside(e) {
      if (wrapperRef.current && !wrapperRef.current.contains(e.target)) {
        setIsOpen(false);
      }
    }
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, [isOpen]);

  useEffect(() => {
    setHighlightIndex(0);
  }, [search, globalFilters]);

  useEffect(() => {
    if (isOpen && listRef.current) {
      const highlighted = listRef.current.children[highlightIndex + 1];
      if (highlighted) {
        highlighted.scrollIntoView({ block: 'nearest' });
      }
    }
  }, [highlightIndex, isOpen]);

  function handleKeyDown(e) {
    if (!isOpen) {
      if (e.key === 'ArrowDown' || e.key === 'Enter') {
        setIsOpen(true);
        e.preventDefault();
      }
      return;
    }
    if (e.key === 'ArrowDown') {
      setHighlightIndex(i => Math.min(i + 1, filtered.length - 1));
      e.preventDefault();
    } else if (e.key === 'ArrowUp') {
      setHighlightIndex(i => Math.max(i - 1, 0));
      e.preventDefault();
    } else if (e.key === 'Enter') {
      if (filtered[highlightIndex]) {
        selectPokemon(filtered[highlightIndex]);
      }
      e.preventDefault();
    } else if (e.key === 'Escape') {
      setIsOpen(false);
    }
  }

  function selectPokemon(pokemon) {
    onChange(pokemon.name);
    setSearch('');
    setIsOpen(false);
  }

  function handleClear() {
    onChange('');
    setSearch('');
  }

  // Use native wheel events (not React synthetic) so preventDefault works
  const dropdownRef = useRef(null);

  useEffect(() => {
    const el = wrapperRef.current;
    if (!el) return;

    function onWheel(e) {
      if (isOpen) {
        // When dropdown is open, contain scrolling within the list
        const list = listRef.current;
        if (list && list.contains(e.target)) {
          const atTop = list.scrollTop === 0 && e.deltaY < 0;
          const atBottom = list.scrollTop + list.clientHeight >= list.scrollHeight && e.deltaY > 0;
          if (atTop || atBottom) {
            e.preventDefault();
          }
        } else {
          e.preventDefault();
        }
        e.stopPropagation();
        return;
      }
      // When closed, scroll wheel cycles Pokemon
      e.preventDefault();
      e.stopPropagation();
      const currentIdx = selectedPokemon ? filtered.findIndex(p => p.id === selectedPokemon.id) : -1;
      if (e.deltaY < 0) {
        const nextIdx = currentIdx + 1 < filtered.length ? currentIdx + 1 : 0;
        onChange(filtered[nextIdx].name);
      } else {
        const prevIdx = currentIdx > 0 ? currentIdx - 1 : filtered.length - 1;
        onChange(filtered[prevIdx].name);
      }
    }

    el.addEventListener('wheel', onWheel, { passive: false });
    return () => el.removeEventListener('wheel', onWheel);
  }, [isOpen, selectedPokemon, filtered, onChange]);

  return (
    <div className="pokemon-picker" ref={wrapperRef}>
      <div className="pokemon-picker-display" onMouseDown={(e) => { e.preventDefault(); setIsOpen(!isOpen); }}>
        {selectedPokemon ? (
          <>
            <img
              src={getSpriteUrl(selectedPokemon.id)}
              alt={selectedPokemon.name}
              className="pokemon-sprite-small"
            />
            <span className="pokemon-name">{selectedPokemon.name}</span>
            <button className="clear-btn" onClick={(e) => { e.stopPropagation(); handleClear(); }} title="Set to RANDOM">x</button>
          </>
        ) : (
          <span className="random-label">RANDOM</span>
        )}
        <span className="dropdown-arrow">v</span>
      </div>

      {isOpen && (
        <div className="pokemon-dropdown">
          <input
            type="text"
            className="pokemon-search"
            placeholder="Search Pokemon..."
            value={search}
            onChange={e => setSearch(e.target.value)}
            onKeyDown={handleKeyDown}
            autoFocus
          />
          <div className="pokemon-list" ref={listRef}>
            <div
              className={`pokemon-option ${!value ? 'selected' : ''}`}
              onClick={() => { onChange(''); setIsOpen(false); setSearch(''); }}
            >
              <span className="random-option-label">RANDOM</span>
            </div>
            {filtered.map((p, i) => (
              <div
                key={p.id}
                className={`pokemon-option ${i === highlightIndex ? 'highlighted' : ''} ${selectedPokemon?.id === p.id ? 'selected' : ''}`}
                onClick={() => selectPokemon(p)}
                onMouseEnter={() => setHighlightIndex(i)}
              >
                <img src={getSpriteUrl(p.id)} alt={p.name} className="pokemon-sprite-tiny" />
                <span>#{String(p.id).padStart(3, '0')} {p.name}</span>
              </div>
            ))}
            {filtered.length === 0 && (
              <div className="pokemon-option"><span className="random-option-label">No matches</span></div>
            )}
          </div>
          <div className="pokemon-filter-count">{filtered.length} Pokemon</div>
        </div>
      )}
    </div>
  );
}
