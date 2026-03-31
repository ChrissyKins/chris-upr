import { useState } from 'react';
import { getClipboard } from '../data/clipboard';
import { POKEMON_BY_NAME, getSpriteUrl } from '../data/pokemon';

export default function PasteLink({ onPaste }) {
  const [hovering, setHovering] = useState(false);
  const [mousePos, setMousePos] = useState({ x: 0, y: 0 });

  const clip = getClipboard();
  const clipPokemon = clip?.pokemonName ? POKEMON_BY_NAME[clip.pokemonName.toUpperCase()] : null;

  function handleMouseMove(e) {
    setMousePos({ x: e.clientX, y: e.clientY });
  }

  return (
    <>
      <a
        href="#"
        onClick={onPaste}
        onMouseEnter={() => setHovering(true)}
        onMouseLeave={() => setHovering(false)}
        onMouseMove={handleMouseMove}
      >(paste)</a>
      {hovering && clipPokemon && (
        <img
          src={getSpriteUrl(clipPokemon.id)}
          alt={clipPokemon.name}
          className="paste-sprite-hover"
          style={{
            position: 'fixed',
            left: mousePos.x + 12,
            top: mousePos.y - 28,
            pointerEvents: 'none',
          }}
        />
      )}
    </>
  );
}
