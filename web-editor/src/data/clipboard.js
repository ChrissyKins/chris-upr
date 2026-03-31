// Shared clipboard for copy/paste between encounter slots and trainer Pokemon
let _clipboard = null;
let _listeners = [];

export function copyPokemon(data) {
  _clipboard = { ...data };
  _listeners.forEach(fn => fn());
}

export function getClipboard() {
  return _clipboard;
}

export function hasClipboard() {
  return _clipboard !== null;
}

export function onClipboardChange(fn) {
  _listeners.push(fn);
  return () => { _listeners = _listeners.filter(l => l !== fn); };
}
