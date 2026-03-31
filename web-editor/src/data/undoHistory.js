// Simple undo history for areas + trainers state
const MAX_HISTORY = 50;
let _history = [];
let _index = -1;

export function pushState(areas, trainers) {
  // Discard any redo states
  _history = _history.slice(0, _index + 1);
  _history.push({ areas, trainers });
  if (_history.length > MAX_HISTORY) {
    _history.shift();
  }
  _index = _history.length - 1;
}

export function undo() {
  if (_index <= 0) return null;
  _index--;
  return _history[_index];
}

export function canUndo() {
  return _index > 0;
}
