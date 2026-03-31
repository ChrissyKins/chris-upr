const RELEASES = [
  {
    version: 'v1.0.0',
    date: 'March 21, 2026',
    downloadUrl: '#',
    changelog: [
      'Initial release of Custom Pokemon Randomizer',
      'Custom wild encounter file support',
      'Web-based encounter editor',
    ],
  },
];

export default function DownloadsPage() {
  return (
    <div>
      <hr />
      {RELEASES.map((release) => (
        <div key={release.version} style={{ marginBottom: 20 }}>
          <h3>Custom Pokemon Randomizer {release.version}</h3>
          <div style={{ fontSize: '0.9em', color: '#555', marginBottom: 4 }}>{release.date}</div>
          <div style={{ marginBottom: 8 }}>
            <a href={release.downloadUrl}>[Download]</a>
          </div>
          <div>
            <b>Changelog:</b>
            <ul style={{ marginTop: 4 }}>
              {release.changelog.map((item, i) => (
                <li key={i}>{item}</li>
              ))}
            </ul>
          </div>
          <hr />
        </div>
      ))}
    </div>
  );
}
