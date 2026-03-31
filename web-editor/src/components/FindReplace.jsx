import { useState, useMemo } from 'react';
import PokemonPicker, { getFilteredPokemonList } from './PokemonPicker';
import { usePokemonFilters } from '../data/pokemonFilterContext';
import { getDefaultMoveset } from './TrainerEditor';

export default function FindReplace({ areas, trainers, onReplace, onRandomizeAll }) {
  const [panel, setPanel] = useState(null); // null, 'find', 'randomize'
  const [findName, setFindName] = useState('');
  const [replaceName, setReplaceName] = useState('');
  const [inEncounters, setInEncounters] = useState(true);
  const [inTrainers, setInTrainers] = useState(true);
  const globalFilters = usePokemonFilters();

  const { encounterCount, trainerCount } = useMemo(() => {
    if (!findName) return { encounterCount: 0, trainerCount: 0 };
    const upper = findName.toUpperCase();
    let ec = 0, tc = 0;
    for (const area of areas) {
      for (const slot of area.slots) {
        if (slot.pokemonName && slot.pokemonName.toUpperCase() === upper) ec++;
      }
    }
    for (const trainer of trainers) {
      for (const poke of trainer.pokemon) {
        if (poke.pokemonName && poke.pokemonName.toUpperCase() === upper) tc++;
      }
    }
    return { encounterCount: ec, trainerCount: tc };
  }, [findName, areas, trainers]);

  const totalCount = (inEncounters ? encounterCount : 0) + (inTrainers ? trainerCount : 0);

  function handleReplace() {
    if (!findName || !replaceName || totalCount === 0) return;
    onReplace(findName, replaceName, inEncounters, inTrainers);
    setPanel(null);
    setFindName('');
    setReplaceName('');
  }

  function handleRandomize() {
    const pool = getFilteredPokemonList(globalFilters);
    if (pool.length === 0) return;
    if (!window.confirm(`Randomize ${inEncounters && inTrainers ? 'encounters + trainers' : inEncounters ? 'encounters' : 'trainers'} using ${pool.length} filtered Pokemon?`)) return;
    onRandomizeAll(pool, inEncounters, inTrainers);
    setPanel(null);
  }

  function close() {
    setPanel(null);
    setFindName('');
    setReplaceName('');
  }

  const pool = getFilteredPokemonList(globalFilters);

  return (
    <div>
      <a href="#" onClick={(e) => { e.preventDefault(); setPanel(panel === 'find' ? null : 'find'); }}>[Find & Replace]</a>
      {' '}
      <a href="#" onClick={(e) => { e.preventDefault(); setPanel(panel === 'randomize' ? null : 'randomize'); }}>[Randomize All]</a>

      {panel === 'find' && (
        <div className="find-replace">
          <div className="find-replace-row">
            <span className="find-replace-label">Find:</span>
            <PokemonPicker value={findName} onChange={setFindName} />
            {findName && <span className="find-replace-count">({encounterCount} encounters, {trainerCount} trainers)</span>}
          </div>
          <div className="find-replace-row">
            <span className="find-replace-label">Replace with:</span>
            <PokemonPicker value={replaceName} onChange={setReplaceName} />
          </div>
          <div className="find-replace-row">
            <span className="find-replace-label">In:</span>
            <label><input type="checkbox" checked={inEncounters} onChange={() => setInEncounters(!inEncounters)} /> Encounters</label>
            {' '}
            <label><input type="checkbox" checked={inTrainers} onChange={() => setInTrainers(!inTrainers)} /> Trainers</label>
          </div>
          <div className="find-replace-row">
            <a href="#" onClick={(e) => { e.preventDefault(); handleReplace(); }}
              className={!findName || !replaceName || totalCount === 0 ? 'disabled-link' : ''}
            >[Replace All ({totalCount})]</a>
            {' '}
            <a href="#" onClick={(e) => { e.preventDefault(); close(); }}>[Cancel]</a>
          </div>
        </div>
      )}

      {panel === 'randomize' && (
        <div className="find-replace">
          <div className="find-replace-row">
            <span className="find-replace-label">Apply to:</span>
            <label><input type="checkbox" checked={inEncounters} onChange={() => setInEncounters(!inEncounters)} /> Encounters</label>
            {' '}
            <label><input type="checkbox" checked={inTrainers} onChange={() => setInTrainers(!inTrainers)} /> Trainers</label>
          </div>
          <div className="find-replace-row">
            <span className="find-replace-count">Using {pool.length} Pokemon from current filters</span>
          </div>
          <div className="find-replace-row">
            <a href="#" onClick={(e) => { e.preventDefault(); handleRandomize(); }}
              className={pool.length === 0 || (!inEncounters && !inTrainers) ? 'disabled-link' : ''}
            >[Randomize]</a>
            {' '}
            <a href="#" onClick={(e) => { e.preventDefault(); close(); }}>[Cancel]</a>
          </div>
        </div>
      )}
    </div>
  );
}
