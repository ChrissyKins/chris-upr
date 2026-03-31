import { useState, useCallback, useMemo, useEffect, useRef } from 'react';
import LocationCarousel from './components/LocationCarousel';
import DownloadsPage from './components/DownloadsPage';
import FindReplace from './components/FindReplace';
import { exportJSON, parseJSON } from './data/templateParser';
import { getDefaultCrystalEncounters, getDefaultSlotsForArea, getDefaultCrystalTrainers } from './data/crystalEncounters';
import { ALL_TYPES } from './data/pokemonFilters';
import { PokemonFilterContext } from './data/pokemonFilterContext';
import { pushState, undo, canUndo } from './data/undoHistory';
import { POKEMON_BY_NAME } from './data/pokemon';
import { getDefaultMoveset } from './components/TrainerEditor';
import './App.css';

const SAVE_KEY = 'pkcrystal_editor_save';
const SAVE_VERSION = 2; // v2: collapsed time-of-day

function loadSavedState() {
  try {
    const raw = localStorage.getItem(SAVE_KEY);
    if (!raw) return null;
    const saved = JSON.parse(raw);
    if (!saved.areas || !saved.trainers) return null;
    // Migrate old saves: collapse time-of-day areas
    if ((saved.version || 1) < 2) {
      saved.areas = saved.areas.filter(a => {
        const name = a.name || '';
        return !name.includes('(Morning)') && !name.includes('(Night)');
      }).map(a => {
        const name = a.name || '';
        if (name.includes('(Day)')) {
          return { ...a, name: name.replace(' (Day)', ''), originalName: name };
        }
        return a;
      });
      saved.version = 2;
    }
    return saved;
  } catch (e) { /* ignore corrupt data */ }
  return null;
}

function saveState(areas, trainers) {
  try {
    localStorage.setItem(SAVE_KEY, JSON.stringify({ areas, trainers, version: SAVE_VERSION }));
  } catch (e) { /* storage full, ignore */ }
}

function App() {
  const [areas, setAreas] = useState(() => {
    const saved = loadSavedState();
    return saved ? saved.areas : getDefaultCrystalEncounters();
  });
  const [trainers, setTrainers] = useState(() => {
    const saved = loadSavedState();
    return saved ? saved.trainers : getDefaultCrystalTrainers();
  });
  const [fileName, setFileName] = useState('custom_encounters.json');
  const [page, setPage] = useState('home');
  const isInitial = useRef(true);

  // Update browser title based on page
  useEffect(() => {
    const titles = {
      home: "Chris' Pokemon Randomiser",
      downloads: "Downloads - Chris' Pokemon Randomiser",
      editor: "Pokemon Crystal - Chris' Randomiser",
    };
    document.title = titles[page] || "Chris' Pokemon Randomiser";
  }, [page]);

  // Push initial state to undo history
  useEffect(() => {
    pushState(areas, trainers);
  }, []);

  // Auto-save and push undo on every change (skip initial)
  useEffect(() => {
    if (isInitial.current) {
      isInitial.current = false;
      return;
    }
    saveState(areas, trainers);
    pushState(areas, trainers);
  }, [areas, trainers]);

  // Ctrl+Z handler
  useEffect(() => {
    function handleKeyDown(e) {
      if ((e.ctrlKey || e.metaKey) && e.key === 'z') {
        if (e.target.tagName === 'INPUT' || e.target.tagName === 'TEXTAREA') return;
        e.preventDefault();
        const prev = undo();
        if (prev) {
          isInitial.current = true; // prevent double-push
          setAreas(prev.areas);
          setTrainers(prev.trainers);
          saveState(prev.areas, prev.trainers);
          // Reset flag after state settles
          setTimeout(() => { isInitial.current = false; }, 0);
        }
      }
    }
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, []);

  // Global filters
  const [filterTypes, setFilterTypes] = useState([]);
  const [filterStage, setFilterStage] = useState('any');
  const [filterLegendary, setFilterLegendary] = useState('any');
  const [filterEvolved, setFilterEvolved] = useState('any');
  const [filterBstRanges, setFilterBstRanges] = useState([]);

  const filters = useMemo(() => ({
    types: filterTypes,
    stage: filterStage,
    legendary: filterLegendary,
    evolved: filterEvolved,
    bstRanges: filterBstRanges,
  }), [filterTypes, filterStage, filterLegendary, filterEvolved, filterBstRanges]);

  function toggleType(type) {
    setFilterTypes(prev =>
      prev.includes(type) ? prev.filter(t => t !== type) : [...prev, type]
    );
  }

  function handleFileImport(e) {
    const file = e.target.files[0];
    if (!file) return;
    setFileName(file.name);
    const reader = new FileReader();
    reader.onload = (ev) => {
      const { areas: parsedAreas, trainers: parsedTrainers } = parseJSON(ev.target.result);
      if (parsedAreas.length > 0) setAreas(parsedAreas);
      if (parsedTrainers.length > 0) setTrainers(parsedTrainers);
    };
    reader.readAsText(file);
    e.target.value = '';
  }

  function handleExport() {
    if (!areas) return;
    const json = exportJSON(areas, trainers);
    const text = JSON.stringify(json, null, 2);
    const blob = new Blob([text], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = fileName || 'custom_encounters.json';
    a.click();
    URL.revokeObjectURL(url);
  }

  const handleSlotChange = useCallback((areaIndex, slotIndex, newSlot) => {
    setAreas(prev => {
      const next = [...prev];
      next[areaIndex] = {
        ...next[areaIndex],
        slots: next[areaIndex].slots.map((s, i) => i === slotIndex ? newSlot : s),
      };
      return next;
    });
  }, []);

  const handleResetArea = useCallback((areaIndices) => {
    setAreas(prev => {
      const next = [...prev];
      for (const idx of areaIndices) {
        const defaults = getDefaultSlotsForArea(idx);
        if (defaults) {
          next[idx] = { ...next[idx], slots: defaults };
        }
      }
      return next;
    });
  }, []);

  const handleTrainerPokemonChange = useCallback((trainerIndex, pokeIndex, newPoke) => {
    setTrainers(prev => {
      const next = [...prev];
      const idx = next.findIndex(t => t.index === trainerIndex);
      if (idx >= 0) {
        next[idx] = {
          ...next[idx],
          pokemon: next[idx].pokemon.map((p, i) => i === pokeIndex ? newPoke : p),
        };
      }
      return next;
    });
  }, []);

  function handleResetAll() {
    if (!window.confirm('Reset everything to defaults? This cannot be undone.')) return;
    localStorage.removeItem(SAVE_KEY);
    setAreas(getDefaultCrystalEncounters());
    setTrainers(getDefaultCrystalTrainers());
  }

  function handleFindReplace(findName, replaceName, inEncounters, inTrainers) {
    const findUpper = findName.toUpperCase();

    if (inEncounters) {
      setAreas(prev => prev.map(area => ({
        ...area,
        slots: area.slots.map(slot => {
          if (slot.pokemonName && slot.pokemonName.toUpperCase() === findUpper) {
            return { ...slot, pokemonName: replaceName, isRandom: false };
          }
          return slot;
        }),
      })));
    }

    if (inTrainers) {
      setTrainers(prev => prev.map(trainer => ({
        ...trainer,
        pokemon: trainer.pokemon.map(poke => {
          if (poke.pokemonName && poke.pokemonName.toUpperCase() === findUpper) {
            return { ...poke, pokemonName: replaceName, isRandom: false };
          }
          return poke;
        }),
      })));
    }
  }

  function handleRandomizeAll(pool, inEncounters, inTrainers) {
    if (inEncounters) {
      setAreas(prev => prev.map(area => ({
        ...area,
        slots: area.slots.map(slot => {
          const randPoke = pool[Math.floor(Math.random() * pool.length)];
          return { ...slot, pokemonName: randPoke.name, isRandom: false };
        }),
      })));
    }
    if (inTrainers) {
      setTrainers(prev => prev.map(trainer => ({
        ...trainer,
        pokemon: trainer.pokemon.map(poke => {
          const randPoke = pool[Math.floor(Math.random() * pool.length)];
          const pokemonId = POKEMON_BY_NAME[randPoke.name.toUpperCase()]?.id || 0;
          const moves = pokemonId ? getDefaultMoveset(pokemonId, poke.level) : null;
          return { ...poke, pokemonName: randPoke.name, isRandom: false, moves };
        }),
      })));
    }
  }

  const nav = (
    <div className="nav">
      <a href="#" onClick={(e) => { e.preventDefault(); setPage('home'); }}
        style={page === 'home' ? { fontWeight: 'bold' } : {}}>[Home]</a>
      {' '}
      <a href="#" onClick={(e) => { e.preventDefault(); setPage('downloads'); }}
        style={page === 'downloads' ? { fontWeight: 'bold' } : {}}>[Downloads]</a>
    </div>
  );

  if (page === 'home') {
    return (
      <div className="app">
        <h1>Chris' Pokemon Randomiser</h1>
        {nav}
        <hr />
        <h3>Encounter & Trainer Editors</h3>
        <ul>
          <li><a href="#" onClick={(e) => { e.preventDefault(); setPage('editor'); }}>Pokemon Crystal</a></li>
        </ul>
      </div>
    );
  }

  if (page === 'downloads') {
    return (
      <div className="app">
        <h1>Downloads</h1>
        {nav}
        <DownloadsPage />
      </div>
    );
  }

  const hasFilters = filterTypes.length > 0 || filterStage !== 'any' || filterLegendary !== 'any' || filterEvolved !== 'any' || filterBstRanges.length > 0;

  const BST_RANGES = [
    { label: '<200', min: 0, max: 199 },
    { label: '200-299', min: 200, max: 299 },
    { label: '300-399', min: 300, max: 399 },
    { label: '400-499', min: 400, max: 499 },
    { label: '500+', min: 500, max: 999 },
  ];

  function toggleBstRange(range) {
    setFilterBstRanges(prev => {
      const key = range.label;
      const has = prev.some(r => r.label === key);
      return has ? prev.filter(r => r.label !== key) : [...prev, range];
    });
  }

  return (
    <PokemonFilterContext.Provider value={filters}>
      <div className="app">
        <h1>Pokemon Crystal - Encounter / Trainer Editor</h1>
        {nav}
        <hr />

        <div className="toolbar">
          <div className="toolbar-right">
            <label className="file-label">
              [<span className="link">Load File</span>]
              <input type="file" accept=".json" onChange={handleFileImport} hidden />
            </label>
            {' '}
            <a href="#" onClick={(e) => { e.preventDefault(); handleExport(); }}>[Save File]</a>
            {' '}
            <a href="#" onClick={(e) => { e.preventDefault(); handleResetAll(); }}>[Reset to Defaults]</a>
          </div>
        </div>

        <hr />

        <div className="toolbar">
          <div className="toolbar-right">
            <FindReplace areas={areas} trainers={trainers} onReplace={handleFindReplace} onRandomizeAll={handleRandomizeAll} />
          </div>
        </div>

        <hr />

        <div className="global-filters">
          <div className="filter-row">
            <span className="filter-label">Type:</span>
            {ALL_TYPES.map(t => (
              <a
                key={t}
                href="#"
                className={`type-filter-btn ${filterTypes.includes(t) ? 'active' : ''}`}
                data-type={t.toLowerCase()}
                onClick={(e) => { e.preventDefault(); toggleType(t); }}
              >{t.charAt(0) + t.slice(1).toLowerCase()}</a>
            ))}
          </div>
          <div className="filter-row">
            <span className="filter-label">Stage:</span>
            {[['any', 'Any'], ['1', 'Basic'], ['2', 'Stage 2'], ['3', 'Stage 3']].map(([val, label]) => (
              <a
                key={val}
                href="#"
                className={`filter-btn ${filterStage === val ? 'active' : ''}`}
                onClick={(e) => { e.preventDefault(); setFilterStage(val); }}
              >{label}</a>
            ))}
            <span className="filter-label" style={{ marginLeft: 12 }}>Evolved:</span>
            {[['any', 'Any'], ['fully', 'Fully Evolved'], ['not_fully', 'Not Fully Evolved']].map(([val, label]) => (
              <a
                key={val}
                href="#"
                className={`filter-btn ${filterEvolved === val ? 'active' : ''}`}
                onClick={(e) => { e.preventDefault(); setFilterEvolved(val); }}
              >{label}</a>
            ))}
            <span className="filter-label" style={{ marginLeft: 12 }}>Legendary:</span>
            {[['any', 'Any'], ['none', 'None'], ['only', 'Only']].map(([val, label]) => (
              <a
                key={val}
                href="#"
                className={`filter-btn ${filterLegendary === val ? 'active' : ''}`}
                onClick={(e) => { e.preventDefault(); setFilterLegendary(val); }}
              >{label}</a>
            ))}
          </div>
          <div className="filter-row">
            <span className="filter-label">BST:</span>
            {BST_RANGES.map(r => (
              <a
                key={r.label}
                href="#"
                className={`filter-btn ${filterBstRanges.some(b => b.label === r.label) ? 'active' : ''}`}
                onClick={(e) => { e.preventDefault(); toggleBstRange(r); }}
              >{r.label}</a>
            ))}
            {hasFilters && (
              <>
                {' | '}
                <a href="#" onClick={(e) => {
                  e.preventDefault();
                  setFilterTypes([]);
                  setFilterStage('any');
                  setFilterLegendary('any');
                  setFilterEvolved('any');
                  setFilterBstRanges([]);
                }}>[Clear Filters]</a>
              </>
            )}
          </div>
        </div>

        <hr />

        <LocationCarousel
          areas={areas}
          trainers={trainers}
          onSlotChange={handleSlotChange}
          onResetArea={handleResetArea}
          onTrainerPokemonChange={handleTrainerPokemonChange}
        />
      </div>
    </PokemonFilterContext.Provider>
  );
}

export default App;
