import { useState } from 'react';
import SlotEditor from './SlotEditor';
import { POKEMON } from '../data/pokemon';

export default function AreaEditor({ area, onSlotChange, areaIndex }) {
  const [collapsed, setCollapsed] = useState(false);
  const filledCount = area.slots.filter(s => s.pokemonName && !s.isRandom).length;

  function handleRandomize(e) {
    e.stopPropagation();
    area.slots.forEach((slot, i) => {
      const randPoke = POKEMON[Math.floor(Math.random() * POKEMON.length)];
      onSlotChange(areaIndex, i, { ...slot, pokemonName: randPoke.name, isRandom: false });
    });
  }

  return (
    <div className="area-editor">
      <div className="area-header" onClick={() => setCollapsed(!collapsed)}>
        <b>{collapsed ? '[+]' : '[-]'} {area.name}</b>
        {' '}
        <span>({filledCount}/{area.slots.length} filled)</span>
        {' '}
        <a href="#" onClick={(e) => { e.preventDefault(); handleRandomize(e); }}>[Randomize]</a>
      </div>

      {!collapsed && (
        <div className="area-slots">
          {area.slots.map((slot, i) => (
            <SlotEditor
              key={i}
              slot={slot}
              onChange={(newSlot) => onSlotChange(areaIndex, i, newSlot)}
            />
          ))}
        </div>
      )}
    </div>
  );
}
