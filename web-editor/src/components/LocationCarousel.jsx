import { useState, useMemo, useEffect } from 'react';
import { getImageForArea, getFloorImage } from '../data/locationImages';
import { PROGRESSION_ORDER } from '../data/progressionOrder';
import { getTrainerLocation } from '../data/trainerLocationMap';
import { getDefaultSlotsForArea } from '../data/crystalEncounters';
import { usePokemonFilters } from '../data/pokemonFilterContext';
import { getFilteredPokemonList } from './PokemonPicker';
import SlotEditor from './SlotEditor';
import TrainerEditor from './TrainerEditor';

// Build progression lookup
const PROGRESSION_MAP = {};
PROGRESSION_ORDER.forEach((name, i) => { PROGRESSION_MAP[name.toUpperCase()] = i; });

function groupAreasByLocation(areas, trainers) {
  const groups = [];
  const groupMap = {};

  for (let i = 0; i < areas.length; i++) {
    const area = areas[i];
    const loc = getLocationKey(area.name);

    if (!groupMap[loc]) {
      groupMap[loc] = { location: loc, subAreas: [], trainers: [], image: getImageForArea(area.name) };
      groups.push(groupMap[loc]);
    }

    groupMap[loc].subAreas.push({ ...area, originalIndex: i });
  }

  // Build a case-insensitive lookup for groupMap
  const groupMapUpper = {};
  for (const key of Object.keys(groupMap)) {
    groupMapUpper[key.toUpperCase()] = groupMap[key];
  }

  // Add trainers to their location groups (or create new groups for trainer-only locations)
  if (trainers && trainers.length > 0) {
    for (const trainer of trainers) {
      const location = getTrainerLocation(trainer.index, trainers);
      if (location) {
        const upperLoc = location.toUpperCase();
        let group = groupMapUpper[upperLoc];
        if (!group) {
          // Create a new group for locations with trainers but no encounters (e.g. gyms)
          group = { location: location, subAreas: [], trainers: [], image: getImageForArea(location) };
          groupMap[location] = group;
          groupMapUpper[upperLoc] = group;
          groups.push(group);
        }
        group.trainers.push(trainer);
      }
    }
  }

  // Sort by game progression order
  groups.sort((a, b) => {
    const aKey = a.location.toUpperCase();
    const bKey = b.location.toUpperCase();
    const aIsSpecial = /Fishing|Headbutt|Bug Catching/i.test(aKey);
    const bIsSpecial = /Fishing|Headbutt|Bug Catching/i.test(bKey);
    const aIsStatic = aKey.startsWith('[STATIC]');
    const bIsStatic = bKey.startsWith('[STATIC]');

    function tier(isStatic, isSpecial) {
      if (isSpecial) return 2;
      if (isStatic) return 1;
      return 0;
    }
    const aTier = tier(aIsStatic, aIsSpecial);
    const bTier = tier(bIsStatic, bIsSpecial);
    if (aTier !== bTier) return aTier - bTier;
    if (aTier > 0) return a.location.localeCompare(b.location);

    const aIdx = PROGRESSION_MAP[aKey] ?? 9999;
    const bIdx = PROGRESSION_MAP[bKey] ?? 9999;
    if (aIdx !== bIdx) return aIdx - bIdx;
    return a.location.localeCompare(b.location);
  });

  return groups;
}

function getLocationKey(areaName) {
  if (areaName.startsWith('[STATIC]')) return areaName;
  return areaName
    .replace(/ Grass\/Cave.*$/, '')
    .replace(/ Surfing$/, '')
    .trim();
}

function getEncounterLabel(areaName) {
  if (areaName.includes('Grass/Cave')) {
    const match = areaName.match(/Grass\/Cave\s*(.*)$/);
    return match && match[1] ? `Grass/Cave (${match[1]})` : 'Grass/Cave';
  }
  if (areaName.includes('Surfing')) return 'Surfing';
  if (areaName.startsWith('[STATIC]')) return areaName;
  return areaName;
}

export default function LocationCarousel({ areas, trainers, onSlotChange, onResetArea, onTrainerPokemonChange }) {
  const groups = useMemo(() => groupAreasByLocation(areas, trainers), [areas, trainers]);
  const [currentIndex, setCurrentIndex] = useState(0);
  const globalFilters = usePokemonFilters();

  function getRandomPokemon() {
    const pool = getFilteredPokemonList(globalFilters);
    if (pool.length === 0) return null;
    return pool[Math.floor(Math.random() * pool.length)];
  }

  const groupOptions = useMemo(() => groups.map((g, i) => {
    const filled = g.subAreas.reduce((s, a) => s + a.slots.filter(sl => sl.pokemonName && !sl.isRandom).length, 0);
    const total = g.subAreas.reduce((s, a) => s + a.slots.length, 0);
    const trainerCount = g.trainers.length;
    const check = filled === total ? ' *' : filled > 0 ? ' ~' : '';
    const trainerLabel = trainerCount > 0 ? ` [${trainerCount} trainers]` : '';
    return <option key={i} value={i}>{g.location}{check}{trainerLabel}</option>;
  }), [groups]);

  const group = groups[currentIndex];

  function goNext() {
    setCurrentIndex(i => Math.min(i + 1, groups.length - 1));
  }

  function goPrev() {
    setCurrentIndex(i => Math.max(i - 1, 0));
  }

  useEffect(() => {
    function handleKey(e) {
      if (e.target.tagName === 'INPUT' || e.target.tagName === 'TEXTAREA' || e.target.tagName === 'SELECT') return;
      if (e.key === 'ArrowLeft') goPrev();
      if (e.key === 'ArrowRight') goNext();
    }
    window.addEventListener('keydown', handleKey);
    return () => window.removeEventListener('keydown', handleKey);
  }, []);

  if (!group) return null;

  const filledCount = group.subAreas.reduce(
    (sum, a) => sum + a.slots.filter(s => s.pokemonName && !s.isRandom).length, 0
  );
  const totalCount = group.subAreas.reduce((sum, a) => sum + a.slots.length, 0);

  function handleReset() {
    if (!window.confirm(`Reset ${group.location} to default encounters?`)) return;
    onResetArea(group.subAreas.map(a => a.originalIndex));
  }

  return (
    <div>
      {/* Navigation */}
      <div className="carousel-nav">
        <div className="carousel-jump">
          Jump to:{' '}
          <select
            value={currentIndex}
            onChange={(e) => setCurrentIndex(parseInt(e.target.value))}
            className="carousel-jump-select"
          >
            {groupOptions}
          </select>
        </div>

        <div className="carousel-nav-row">
          <button onClick={goPrev} disabled={currentIndex === 0}>&lt; Prev</button>
          {' '}
          <b>{currentIndex + 1}</b> / {groups.length}
          {' '}
          <button onClick={goNext} disabled={currentIndex === groups.length - 1}>Next &gt;</button>
        </div>

        <h2 className="carousel-title">{group.location}</h2>
        <div>
          {filledCount}/{totalCount} slots filled
          {group.trainers.length > 0 && ` | ${group.trainers.length} trainer(s)`}
          {' | '}
          <a href="#" onClick={(e) => { e.preventDefault(); handleReset(); }}>[Reset Area]</a>
        </div>
      </div>

      {/* All sub-areas shown flat */}
      <div className="carousel-slots">
        {group.subAreas.map((subArea, subIdx) => {
          let label;
          if (group.subAreas.length <= 1) {
            label = 'Encounters';
          } else {
            const baseLabel = getEncounterLabel(subArea.name);
            const sameLabel = group.subAreas.filter(a => getEncounterLabel(a.name) === baseLabel);
            if (sameLabel.length > 1) {
              const floorNum = sameLabel.indexOf(subArea) + 1;
              label = `${baseLabel} (Floor ${floorNum})`;
            } else {
              label = baseLabel;
            }
          }

          // Get floor-specific image, or fall back to location image
          const baseLabel2 = getEncounterLabel(subArea.name);
          const sameLabel2 = group.subAreas.filter(a => getEncounterLabel(a.name) === baseLabel2);
          const floorIdx = sameLabel2.indexOf(subArea);
          const floorImg = getFloorImage(group.location, floorIdx) || group.image;

          // Check if previous sub-area already showed this same image
          let prevImg = null;
          if (subIdx > 0) {
            const prevArea = group.subAreas[subIdx - 1];
            const prevLabel = getEncounterLabel(prevArea.name);
            const prevSame = group.subAreas.filter(a => getEncounterLabel(a.name) === prevLabel);
            const prevFloorIdx = prevSame.indexOf(prevArea);
            prevImg = getFloorImage(group.location, prevFloorIdx) || group.image;
          }
          const showImg = floorImg && floorImg !== prevImg;

          return (
          <div key={subArea.originalIndex}>
            <div className="carousel-area-label">
              {label}
              {' '}
              <a href="#" onClick={(e) => {
                e.preventDefault();
                subArea.slots.forEach((slot, i) => {
                  const randPoke = getRandomPokemon();
                  if (randPoke) onSlotChange(subArea.originalIndex, i, { ...slot, pokemonName: randPoke.name, isRandom: false });
                });
              }}>[Randomize]</a>
            </div>
            {showImg && (
              <div className="carousel-image-container">
                <img
                  src={floorImg}
                  alt={label}
                  className="carousel-image"
                  onError={(e) => { e.target.style.display = 'none'; }}
                />
              </div>
            )}
            {subArea.slots.map((slot, i) => {
              const defaults = getDefaultSlotsForArea(subArea.originalIndex);
              const defaultSlot = defaults ? defaults[i] : null;
              return <SlotEditor
                key={`${subArea.originalIndex}-${i}`}
                slot={slot}
                defaultSlot={defaultSlot}
                onChange={(newSlot) => onSlotChange(subArea.originalIndex, i, newSlot)}
              />;
            })}
          </div>
          );
        })}

        {/* Trainers for this location */}
        {group.trainers.length > 0 && onTrainerPokemonChange && (
          <div>
            {group.subAreas.length === 0 && group.image && (
              <div className="carousel-image-container">
                <img src={group.image} alt={group.location} className="carousel-image"
                  onError={(e) => { e.target.style.display = 'none'; }} />
              </div>
            )}
            <div className="carousel-area-label">Trainers</div>
            {group.trainers.map((trainer) => (
              <TrainerEditor
                key={trainer.index}
                trainer={trainer}
                trainerIndex={trainer.index}
                onPokemonChange={onTrainerPokemonChange}
              />
            ))}
          </div>
        )}
      </div>

    </div>
  );
}
