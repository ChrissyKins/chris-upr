import { createContext, useContext } from 'react';

export const PokemonFilterContext = createContext({
  types: [],         // empty = any
  stage: 'any',      // 'any', '1', '2', '3'
  legendary: 'any',  // 'any', 'none', 'only'
  evolved: 'any',    // 'any', 'fully', 'not_fully'
  bstRanges: [],     // array of { min, max } — empty = any
});

export function usePokemonFilters() {
  return useContext(PokemonFilterContext);
}
