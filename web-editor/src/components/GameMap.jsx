import { useState, useMemo } from 'react';
import { MAP_LOCATIONS, MAP_CONNECTIONS } from '../data/mapData';

function getLocationCompletion(locationName, areas) {
  // Find all areas that belong to this location
  const matchName = locationName;
  const matching = areas.filter(a => {
    const areaLoc = a.name
      .replace(/ Grass\/Cave \((Morning|Day|Night)\)$/, '')
      .replace(/ Surfing$/, '');
    return areaLoc === matchName;
  });

  // Also check for "match" prefix (e.g., "Sprout Tower" matches "Sprout Tower 2F", etc.)
  const mapLoc = MAP_LOCATIONS.find(l => l.name === locationName);
  const matchPrefix = mapLoc?.match || locationName;
  const prefixMatching = areas.filter(a => {
    const areaLoc = a.name
      .replace(/ Grass\/Cave \((Morning|Day|Night)\)$/, '')
      .replace(/ Surfing$/, '');
    return areaLoc.startsWith(matchPrefix) && !matching.includes(a);
  });

  const allMatching = [...matching, ...prefixMatching];
  if (allMatching.length === 0) return { total: 0, filled: 0, ratio: -1 };

  const total = allMatching.reduce((s, a) => s + a.slots.length, 0);
  const filled = allMatching.reduce((s, a) =>
    s + a.slots.filter(sl => sl.pokemonName && !sl.isRandom).length, 0);

  return { total, filled, ratio: total > 0 ? filled / total : 0 };
}

function getNodeColor(ratio) {
  if (ratio < 0) return '#333'; // no encounters here
  if (ratio === 0) return '#555'; // empty
  if (ratio < 1) return '#f0c040'; // partial
  return '#4ade80'; // complete
}

export default function GameMap({ areas, onLocationClick }) {
  const [hoveredLoc, setHoveredLoc] = useState(null);
  const [tooltip, setTooltip] = useState(null);

  const completions = useMemo(() => {
    const map = {};
    MAP_LOCATIONS.forEach(loc => {
      map[loc.name] = getLocationCompletion(loc.name, areas);
    });
    return map;
  }, [areas]);

  const posLookup = useMemo(() => {
    const map = {};
    MAP_LOCATIONS.forEach(loc => {
      map[loc.name] = loc;
    });
    return map;
  }, []);

  function handleClick(loc) {
    // Find the first matching area name and scroll to it
    const mapLoc = MAP_LOCATIONS.find(l => l.name === loc.name);
    const matchPrefix = mapLoc?.match || loc.name;
    onLocationClick(matchPrefix);
  }

  function handleMouseEnter(loc, e) {
    setHoveredLoc(loc.name);
    const comp = completions[loc.name];
    const rect = e.currentTarget.getBoundingClientRect();
    setTooltip({
      name: loc.name,
      x: rect.left + rect.width / 2,
      y: rect.top - 8,
      comp,
    });
  }

  function handleMouseLeave() {
    setHoveredLoc(null);
    setTooltip(null);
  }

  // SVG dimensions
  const W = 800;
  const H = 550;
  const PAD = 40;

  function toSvg(x, y) {
    return {
      sx: PAD + (x / 100) * (W - PAD * 2),
      sy: PAD + (y / 100) * (H - PAD * 2),
    };
  }

  return (
    <div className="game-map-container">
      <div className="map-legend">
        <span className="legend-item"><span className="legend-dot" style={{ background: '#555' }} /> Empty</span>
        <span className="legend-item"><span className="legend-dot" style={{ background: '#f0c040' }} /> In Progress</span>
        <span className="legend-item"><span className="legend-dot" style={{ background: '#4ade80' }} /> Complete</span>
      </div>

      <svg viewBox={`0 0 ${W} ${H}`} className="game-map-svg">
        {/* Region labels */}
        <text x={W * 0.35} y={25} className="region-label">JOHTO</text>

        {/* Connection lines */}
        {MAP_CONNECTIONS.map(([from, to], i) => {
          const fromLoc = posLookup[from];
          const toLoc = posLookup[to];
          if (!fromLoc || !toLoc) return null;
          const { sx: x1, sy: y1 } = toSvg(fromLoc.x, fromLoc.y);
          const { sx: x2, sy: y2 } = toSvg(toLoc.x, toLoc.y);
          return (
            <line
              key={i}
              x1={x1} y1={y1} x2={x2} y2={y2}
              stroke="#2a3a5a"
              strokeWidth="2"
            />
          );
        })}

        {/* Location nodes */}
        {MAP_LOCATIONS.map(loc => {
          const { sx, sy } = toSvg(loc.x, loc.y);
          const comp = completions[loc.name];
          const color = getNodeColor(comp?.ratio ?? -1);
          const isHovered = hoveredLoc === loc.name;
          const radius = loc.type === 'town' ? 10 : loc.type === 'dungeon' ? 7 : 6;

          return (
            <g
              key={loc.name}
              onClick={() => handleClick(loc)}
              onMouseEnter={(e) => handleMouseEnter(loc, e)}
              onMouseLeave={handleMouseLeave}
              style={{ cursor: 'pointer' }}
            >
              {/* Glow effect for hovered */}
              {isHovered && (
                <circle cx={sx} cy={sy} r={radius + 4} fill={color} opacity="0.3" />
              )}

              {/* Main node */}
              <circle
                cx={sx} cy={sy} r={radius}
                fill={color}
                stroke={isHovered ? '#fff' : '#1a1a2e'}
                strokeWidth={isHovered ? 2 : 1}
              />

              {/* Town names always visible, others on hover */}
              {(loc.type === 'town' || isHovered) && (
                <text
                  x={sx}
                  y={sy + radius + 14}
                  textAnchor="middle"
                  className="location-label"
                  fill={isHovered ? '#fff' : '#888'}
                  fontSize={loc.type === 'town' ? 11 : 9}
                  fontWeight={loc.type === 'town' ? 600 : 400}
                >
                  {loc.name}
                </text>
              )}
            </g>
          );
        })}
      </svg>

      {/* Tooltip */}
      {tooltip && (
        <div
          className="map-tooltip"
          style={{
            left: tooltip.x,
            top: tooltip.y,
          }}
        >
          <strong>{tooltip.name}</strong>
          {tooltip.comp && tooltip.comp.total > 0 && (
            <span className="tooltip-progress">
              {tooltip.comp.filled}/{tooltip.comp.total} slots
            </span>
          )}
          {tooltip.comp && tooltip.comp.total === 0 && (
            <span className="tooltip-progress">No wild encounters</span>
          )}
        </div>
      )}
    </div>
  );
}
