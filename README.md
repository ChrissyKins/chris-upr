# Chris' Pokemon Randomiser v1.0.0

A custom Pokemon randomiser with a web-based encounter and trainer editor, tailored for Pokemon Crystal.

Forked from [Universal Pokemon Randomizer ZX](https://github.com/Ajarmar/universal-pokemon-randomizer-zx) by Ajarmar (originally by Dabomstew).

## Features

- **Custom Encounters** — choose exactly which Pokemon appear on every route
- **Custom Trainers** — edit every trainer's team, levels, and movesets
- **Web Editor** — visual editor with location images, Pokemon sprites, type filters, and more
- **Combined File** — one text file for both encounters and trainers
- **Game Data Export** — dump all ROM data (Pokemon, moves, items, trainers, learnsets, evolutions) as JSON

## Prerequisites

- **JDK 8** (1.8) — [Download from Adoptium](https://adoptium.net/temurin/releases/?version=8)
- **IntelliJ IDEA** (Community or Ultimate) — needed for the GUI form compiler

## Building

From the project root:

```bash
bash build.sh
```

This compiles the source, instruments the IntelliJ GUI forms, and outputs the JAR to the `launcher/` folder.

## Running

Double-click `launcher/launcher_WINDOWS.bat`, or from bash:

```bash
cd launcher
"/c/Program Files/Java/jdk1.8.0_202/bin/java" -Xmx4608M -jar PokeRandoZX.jar please-use-the-launcher
```

## Web Editor

```bash
cd web-editor
npm install
npm run dev
```

## Project Structure

```
src/
  com/dabomstew/pkrandom/
    romhandlers/       # ROM-specific logic (Gen2RomHandler.java = Crystal)
    constants/         # Game constants (Gen2Constants.java)
    config/            # Offset tables, character encodings
    newgui/            # Swing GUI
    pokemon/           # Data classes (Pokemon, Move, Trainer, etc.)
    patches/           # IPS patches
    CustomEncounterFile.java  # Template generation/parsing
    GameDataExporter.java     # JSON data export
    Randomizer.java           # Core randomization logic
    Settings.java             # Randomizer settings
web-editor/            # React web editor
launcher/              # Launcher scripts + output JAR
```

## Credits

- [Ajarmar](https://github.com/Ajarmar/universal-pokemon-randomizer-zx) — Universal Pokemon Randomizer ZX (upstream fork)
- Dabomstew — Original Universal Pokemon Randomizer
- darkeye, cleartonic — Significant contributions to UPR-ZX
