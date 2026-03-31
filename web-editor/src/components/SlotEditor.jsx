import { useState } from 'react';
import PokemonPicker from './PokemonPicker';
import PasteLink from './PasteLink';
import { copyPokemon, getClipboard } from '../data/clipboard';

export default function SlotEditor({ slot, defaultSlot, onChange }) {
  const [justCopied, setJustCopied] = useState(false);

  function handlePokemonChange(name) {
    onChange({ ...slot, pokemonName: name, isRandom: !name });
  }

  function handleLevelChange(e) {
    const val = parseInt(e.target.value);
    if (!isNaN(val) && val >= 1 && val <= 100) {
      onChange({ ...slot, level: val });
    }
  }

  function handleMaxLevelChange(e) {
    const val = parseInt(e.target.value);
    if (!isNaN(val) && val >= 0 && val <= 100) {
      onChange({ ...slot, maxLevel: val });
    }
  }

  function handleCopy(e) {
    e.preventDefault();
    copyPokemon({ pokemonName: slot.pokemonName, isRandom: slot.isRandom });
    setJustCopied(true);
    setTimeout(() => setJustCopied(false), 800);
  }

  function handlePaste(e) {
    e.preventDefault();
    const clip = getClipboard();
    if (!clip) return;
    onChange({ ...slot, pokemonName: clip.pokemonName, isRandom: clip.isRandom });
  }

  function handleReset(e) {
    e.preventDefault();
    if (defaultSlot) {
      onChange({ ...slot, pokemonName: defaultSlot.pokemonName, isRandom: defaultSlot.isRandom, level: defaultSlot.level, maxLevel: defaultSlot.maxLevel || 0 });
    }
  }

  return (
    <div className={`slot-editor ${slot.isRandom || !slot.pokemonName ? 'slot-random' : ''}`}>
      <span className="slot-number">{slot.label || `Slot ${slot.slotNum}`}</span>
      {slot.chance && <span className="slot-chance">({slot.chance})</span>}
      {' '}
      <PokemonPicker value={slot.pokemonName} onChange={handlePokemonChange} />
      {' '}
      <span className="slot-actions">
        <a href="#" onClick={handleCopy}>{justCopied ? '(copied!)' : '(copy)'}</a>
        <PasteLink onPaste={handlePaste} />
      </span>
      {' '}
      <span className="level-inputs">
        Lv<input
          type="number"
          min="1"
          max="100"
          value={slot.level}
          onChange={handleLevelChange}
          className="level-input"
        />
        {slot.maxLevel > 0 && (
          <>
            -<input
              type="number"
              min="0"
              max="100"
              value={slot.maxLevel}
              onChange={handleMaxLevelChange}
              className="level-input"
            />
          </>
        )}
      </span>
      {defaultSlot && (
        <span className="slot-actions">
          <a href="#" onClick={handleReset}>(reset)</a>
        </span>
      )}
    </div>
  );
}
