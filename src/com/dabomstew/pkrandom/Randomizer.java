package com.dabomstew.pkrandom;

/*----------------------------------------------------------------------------*/
/*--  Randomizer.java - Can randomize a file based on settings.             --*/
/*--                    Output varies by seed.                              --*/
/*--                                                                        --*/
/*--  Part of "Universal Pokemon Randomizer ZX" by the UPR-ZX team          --*/
/*--  Originally part of "Universal Pokemon Randomizer" by Dabomstew        --*/
/*--  Pokemon and any associated names and the like are                     --*/
/*--  trademark and (C) Nintendo 1996-2020.                                 --*/
/*--                                                                        --*/
/*--  The custom code written here is licensed under the terms of the GPL:  --*/
/*--                                                                        --*/
/*--  This program is free software: you can redistribute it and/or modify  --*/
/*--  it under the terms of the GNU General Public License as published by  --*/
/*--  the Free Software Foundation, either version 3 of the License, or     --*/
/*--  (at your option) any later version.                                   --*/
/*--                                                                        --*/
/*--  This program is distributed in the hope that it will be useful,       --*/
/*--  but WITHOUT ANY WARRANTY; without even the implied warranty of        --*/
/*--  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the          --*/
/*--  GNU General Public License for more details.                          --*/
/*--                                                                        --*/
/*--  You should have received a copy of the GNU General Public License     --*/
/*--  along with this program. If not, see <http://www.gnu.org/licenses/>.  --*/
/*----------------------------------------------------------------------------*/

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.*;

import com.dabomstew.pkrandom.pokemon.*;
import com.dabomstew.pkrandom.romhandlers.Gen1RomHandler;
import com.dabomstew.pkrandom.romhandlers.Gen2RomHandler;
import com.dabomstew.pkrandom.romhandlers.RomHandler;

// Can randomize a file based on settings. Output varies by seed.
public class Randomizer {

    private static final String NEWLINE = System.getProperty("line.separator");

    private final Settings settings;
    private final RomHandler romHandler;
    private final ResourceBundle bundle;
    private final boolean saveAsDirectory;

    public Randomizer(Settings settings, RomHandler romHandler, ResourceBundle bundle, boolean saveAsDirectory) {
        this.settings = settings;
        this.romHandler = romHandler;
        this.bundle = bundle;
        this.saveAsDirectory = saveAsDirectory;
    }

    public int randomize(final String filename) {
        return randomize(filename, new PrintStream(new OutputStream() {
            @Override
            public void write(int b) {
            }
        }));
    }

    public int randomize(final String filename, final PrintStream log) {
        long seed = RandomSource.pickSeed();
        // long seed = 123456789;    // TESTING
        return randomize(filename, log, seed);
    }

    public int randomize(final String filename, final PrintStream log, long seed) {

        final long startTime = System.currentTimeMillis();
        RandomSource.seed(seed);

        int checkValue = 0;

        log.println("Randomizer Version: " + Version.VERSION_STRING);
        log.println("Random Seed: " + seed);
        log.println("Settings String: " + Version.VERSION + settings.toString());
        log.println();

        // All possible changes that can be logged
        boolean movesUpdated = false;
        boolean movesChanged = false;
        boolean movesetsChanged = false;
        boolean pokemonTraitsChanged = false;
        boolean startersChanged = false;
        boolean evolutionsChanged = false;
        boolean trainersChanged = false;
        boolean trainerMovesetsChanged = false;
        boolean staticsChanged = false;
        boolean totemsChanged = false;
        boolean wildsChanged = false;
        boolean tmMovesChanged = false;
        boolean moveTutorMovesChanged = false;
        boolean tradesChanged = false;
        boolean tmsHmsCompatChanged = false;
        boolean tutorCompatChanged = false;
        boolean shopsChanged = false;

        // Limit Pokemon
        // 1. Set Pokemon pool according to limits (or lack thereof)
        // 2. If limited, remove evolutions that are outside of the pool

        romHandler.setPokemonPool(settings);

        if (settings.isLimitPokemon()) {
            romHandler.removeEvosForPokemonPool();
        }

        // Parse custom file early so all sections can use it
        String customEncounterPath = settings.getCustomEncounterFilePath();
        CustomEncounterFile.ParseResult customParseResult = null;
        if (customEncounterPath != null && !customEncounterPath.isEmpty()) {
            try {
                boolean useTimeOfDay = settings.isUseTimeBasedEncounters();
                File customFile = new File(customEncounterPath);
                customParseResult = CustomEncounterFile.parseFile(customFile, romHandler, useTimeOfDay);

                if (customParseResult.hasErrors()) {
                    log.println("Custom File Errors:");
                    for (String error : customParseResult.errors) {
                        log.println("  " + error);
                    }
                }
                if (customParseResult.hasWarnings()) {
                    log.println("Custom File Warnings:");
                    for (String warning : customParseResult.warnings) {
                        log.println("  " + warning);
                    }
                }
            } catch (IOException e) {
                log.println("Error reading custom file: " + e.getMessage());
                log.println("Falling back to normal settings.");
                customEncounterPath = null;
                customParseResult = null;
            }
        }

        // Move updates & data changes
        // 1. Update moves to a future generation
        // 2. Randomize move stats

        if (settings.isUpdateMoves()) {
            romHandler.initMoveUpdates();
            romHandler.updateMoves(settings);
            movesUpdated = true;
        }

        if (movesUpdated) {
            logMoveUpdates(log);
        }

        if (settings.isRandomizeMovePowers()) {
            romHandler.randomizeMovePowers();
            movesChanged = true;
        }

        if (settings.isRandomizeMoveAccuracies()) {
            romHandler.randomizeMoveAccuracies();
            movesChanged = true;
        }

        if (settings.isRandomizeMovePPs()) {
            romHandler.randomizeMovePPs();
            movesChanged = true;
        }

        if (settings.isRandomizeMoveTypes()) {
            romHandler.randomizeMoveTypes();
            movesChanged = true;
        }

        if (settings.isRandomizeMoveCategory() && romHandler.hasPhysicalSpecialSplit()) {
            romHandler.randomizeMoveCategory();
            movesChanged = true;
        }

        // Misc Tweaks
        if (settings.getCurrentMiscTweaks() != MiscTweak.NO_MISC_TWEAKS) {
            romHandler.applyMiscTweaks(settings);
        }

        // Update base stats to a future generation
        if (settings.isUpdateBaseStats()) {
            romHandler.updatePokemonStats(settings);
            pokemonTraitsChanged = true;
        }

        // Standardize EXP curves
        if (settings.isStandardizeEXPCurves()) {
            romHandler.standardizeEXPCurves(settings);
        }

        // Pokemon Types
        if (settings.getTypesMod() != Settings.TypesMod.UNCHANGED) {
            romHandler.randomizePokemonTypes(settings);
            pokemonTraitsChanged = true;
        }

        // Wild Held Items
        if (settings.isRandomizeWildPokemonHeldItems()) {
            romHandler.randomizeWildHeldItems(settings);
            pokemonTraitsChanged = true;
        }

        // Random Evos
        // Applied after type to pick new evos based on new types.

        if (settings.getEvolutionsMod() == Settings.EvolutionsMod.RANDOM) {
            romHandler.randomizeEvolutions(settings);
            evolutionsChanged = true;
        } else if (settings.getEvolutionsMod() == Settings.EvolutionsMod.RANDOM_EVERY_LEVEL) {
            romHandler.randomizeEvolutionsEveryLevel(settings);
            evolutionsChanged = true;
        }

        if (evolutionsChanged) {
            logEvolutionChanges(log);
        }

        // Base stat randomization
        switch (settings.getBaseStatisticsMod()) {
            case SHUFFLE:
                romHandler.shufflePokemonStats(settings);
                pokemonTraitsChanged = true;
                break;
            case RANDOM:
                romHandler.randomizePokemonStats(settings);
                pokemonTraitsChanged = true;
                break;
            default:
                break;
        }

        // Abilities
        if (settings.getAbilitiesMod() == Settings.AbilitiesMod.RANDOMIZE) {
            romHandler.randomizeAbilities(settings);
            pokemonTraitsChanged = true;
        }

        // Log Pokemon traits (stats, abilities, etc) if any have changed
        if (pokemonTraitsChanged) {
            logPokemonTraitChanges(log);
        } else {
            log.println("Pokemon base stats & type: unchanged" + NEWLINE);
        }

        for (Pokemon pkmn : romHandler.getPokemon()) {
            if (pkmn != null) {
                checkValue = addToCV(checkValue, pkmn.hp, pkmn.attack, pkmn.defense, pkmn.speed, pkmn.spatk,
                        pkmn.spdef, pkmn.ability1, pkmn.ability2, pkmn.ability3);
            }
        }

        // Trade evolutions removal
        if (settings.isChangeImpossibleEvolutions()) {
            romHandler.removeImpossibleEvolutions(settings);
        }

        // Easier evolutions
        if (settings.isMakeEvolutionsEasier()) {
            romHandler.condenseLevelEvolutions(40, 30);
            romHandler.makeEvolutionsEasier(settings);
        }

        // Remove time-based evolutions
        if (settings.isRemoveTimeBasedEvolutions()) {
            romHandler.removeTimeBasedEvolutions();
        }

        // Log everything afterwards, so that "impossible evolutions" can account for "easier evolutions"
        if (settings.isChangeImpossibleEvolutions()) {
            log.println("--Removing Impossible Evolutions--");
            logUpdatedEvolutions(log, romHandler.getImpossibleEvoUpdates(), romHandler.getEasierEvoUpdates());
        }

        if (settings.isMakeEvolutionsEasier()) {
            log.println("--Making Evolutions Easier--");
            if (!(romHandler instanceof Gen1RomHandler)) {
                log.println("Friendship evolutions now take 160 happiness (was 220).");
            }
            logUpdatedEvolutions(log, romHandler.getEasierEvoUpdates(), null);
        }

        if (settings.isRemoveTimeBasedEvolutions()) {
            log.println("--Removing Timed-Based Evolutions--");
            logUpdatedEvolutions(log, romHandler.getTimeBasedEvoUpdates(), null);
        }

        // Starter Pokemon
        // Applied after type to update the strings correctly based on new types
        switch(settings.getStartersMod()) {
            case CUSTOM:
                romHandler.customStarters(settings);
                startersChanged = true;
                break;
            case COMPLETELY_RANDOM:
                romHandler.randomizeStarters(settings);
                startersChanged = true;
                break;
            case RANDOM_WITH_TWO_EVOLUTIONS:
                romHandler.randomizeBasicTwoEvosStarters(settings);
                startersChanged = true;
                break;
            default:
                break;
        }
        if (settings.isRandomizeStartersHeldItems() && !(romHandler instanceof Gen1RomHandler)) {
            romHandler.randomizeStarterHeldItems(settings);
        }

        if (startersChanged) {
            logStarters(log);
        }

        // Move Data Log
        // Placed here so it matches its position in the randomizer interface
        if (movesChanged) {
            logMoveChanges(log);
        } else if (!movesUpdated) {
            log.println("Move Data: Unchanged." + NEWLINE);
        }

        // Movesets
        // 1. Randomize movesets
        // 2. Reorder moves by damage
        // Note: "Metronome only" is handled after trainers instead

        if (settings.getMovesetsMod() != Settings.MovesetsMod.UNCHANGED &&
                settings.getMovesetsMod() != Settings.MovesetsMod.METRONOME_ONLY) {
            romHandler.randomizeMovesLearnt(settings);
            romHandler.randomizeEggMoves(settings);
            movesetsChanged = true;
        }

        if (settings.isReorderDamagingMoves()) {
            romHandler.orderDamagingMovesByDamage();
            movesetsChanged = true;
        }

        // Apply custom learnsets from file
        if (customParseResult != null && customParseResult.customLearnsets != null) {
            Map<Integer, List<MoveLearnt>> currentMovesets = romHandler.getMovesLearnt();
            for (Map.Entry<Integer, List<CustomEncounterFile.LearnsetEntry>> entry : customParseResult.customLearnsets.entrySet()) {
                int pokemonId = entry.getKey();
                List<CustomEncounterFile.LearnsetEntry> entries = entry.getValue();
                List<MoveLearnt> newMoves = new ArrayList<>();
                for (CustomEncounterFile.LearnsetEntry le : entries) {
                    MoveLearnt ml = new MoveLearnt();
                    ml.move = le.moveId;
                    ml.level = le.level;
                    newMoves.add(ml);
                }
                currentMovesets.put(pokemonId, newMoves);
            }
            romHandler.setMovesLearnt(currentMovesets);
            movesetsChanged = true;
            log.println("Custom learnsets applied from file.");
            log.println();
        }

        // Show the new movesets if applicable
        if (movesetsChanged) {
            logMovesetChanges(log);
        } else if (settings.getMovesetsMod() == Settings.MovesetsMod.METRONOME_ONLY) {
            log.println("Pokemon Movesets: Metronome Only." + NEWLINE);
        } else {
            log.println("Pokemon Movesets: Unchanged." + NEWLINE);
        }

        // TMs

        if (!(settings.getMovesetsMod() == Settings.MovesetsMod.METRONOME_ONLY)
                && settings.getTmsMod() == Settings.TMsMod.RANDOM) {
            romHandler.randomizeTMMoves(settings);
            tmMovesChanged = true;
        }

        // Apply custom TM moves from file (overrides randomized TMs)
        if (customParseResult != null && customParseResult.customTMs != null) {
            List<Integer> tmMoves = romHandler.getTMMoves();
            for (Map.Entry<Integer, Integer> entry : customParseResult.customTMs.entrySet()) {
                int tmNum = entry.getKey();
                int moveId = entry.getValue();
                if (tmNum >= 1 && tmNum <= tmMoves.size()) {
                    tmMoves.set(tmNum - 1, moveId);
                }
            }
            romHandler.setTMMoves(tmMoves);
            tmMovesChanged = true;
            log.println("Custom TM moves applied from file.");
            log.println();
        }

        if (tmMovesChanged) {
            checkValue = logTMMoves(log, checkValue);
        } else if (settings.getMovesetsMod() == Settings.MovesetsMod.METRONOME_ONLY) {
            log.println("TM Moves: Metronome Only." + NEWLINE);
        } else {
            log.println("TM Moves: Unchanged." + NEWLINE);
        }

        // TM/HM compatibility
        // 1. Randomize TM/HM compatibility
        // 2. Ensure levelup move sanity
        // 3. Follow evolutions
        // 4. Full HM compatibility
        // 5. Copy to cosmetic forms

        switch (settings.getTmsHmsCompatibilityMod()) {
            case COMPLETELY_RANDOM:
            case RANDOM_PREFER_TYPE:
                romHandler.randomizeTMHMCompatibility(settings);
                tmsHmsCompatChanged = true;
                break;
            case FULL:
                romHandler.fullTMHMCompatibility();
                tmsHmsCompatChanged = true;
                break;
            default:
                break;
        }

        if (settings.isTmLevelUpMoveSanity()) {
            romHandler.ensureTMCompatSanity();
            if (settings.isTmsFollowEvolutions()) {
                romHandler.ensureTMEvolutionSanity();
            }
            tmsHmsCompatChanged = true;
        }

        if (settings.isFullHMCompat()) {
            romHandler.fullHMCompatibility();
            tmsHmsCompatChanged = true;
        }

        // Copy TM/HM compatibility to cosmetic formes if it was changed at all, and log changes
        if (tmsHmsCompatChanged) {
            romHandler.copyTMCompatibilityToCosmeticFormes();
            logTMHMCompatibility(log);
        }

        // Move Tutors
        if (romHandler.hasMoveTutors()) {

            List<Integer> oldMtMoves = romHandler.getMoveTutorMoves();

            if (!(settings.getMovesetsMod() == Settings.MovesetsMod.METRONOME_ONLY)
                    && settings.getMoveTutorMovesMod() == Settings.MoveTutorMovesMod.RANDOM) {

                romHandler.randomizeMoveTutorMoves(settings);
                moveTutorMovesChanged = true;
            }

            // Apply custom move tutor moves from file
            if (customParseResult != null && customParseResult.customMoveTutors != null) {
                List<Integer> mtMoves = romHandler.getMoveTutorMoves();
                for (Map.Entry<Integer, Integer> entry : customParseResult.customMoveTutors.entrySet()) {
                    int idx = entry.getKey();
                    int moveId = entry.getValue();
                    if (idx >= 0 && idx < mtMoves.size()) {
                        mtMoves.set(idx, moveId);
                    }
                }
                romHandler.setMoveTutorMoves(mtMoves);
                moveTutorMovesChanged = true;
                log.println("Custom Move Tutor moves applied from file.");
                log.println();
            }

            if (moveTutorMovesChanged) {
                checkValue = logMoveTutorMoves(log, checkValue, oldMtMoves);
            } else if (settings.getMovesetsMod() == Settings.MovesetsMod.METRONOME_ONLY) {
                log.println("Move Tutor Moves: Metronome Only." + NEWLINE);
            } else {
                log.println("Move Tutor Moves: Unchanged." + NEWLINE);
            }

            // Move Tutor Compatibility
            // 1. Randomize MT compatibility
            // 2. Ensure levelup move sanity
            // 3. Follow evolutions
            // 4. Copy to cosmetic forms

            switch (settings.getMoveTutorsCompatibilityMod()) {
                case COMPLETELY_RANDOM:
                case RANDOM_PREFER_TYPE:
                    romHandler.randomizeMoveTutorCompatibility(settings);
                    tutorCompatChanged = true;
                    break;
                case FULL:
                    romHandler.fullMoveTutorCompatibility();
                    tutorCompatChanged = true;
                    break;
                default:
                    break;
            }

            if (settings.isTutorLevelUpMoveSanity()) {
                romHandler.ensureMoveTutorCompatSanity();
                if (settings.isTutorFollowEvolutions()) {
                    romHandler.ensureMoveTutorEvolutionSanity();
                }
                tutorCompatChanged = true;
            }

            // Copy move tutor compatibility to cosmetic formes if it was changed at all
            if (tutorCompatChanged) {
                romHandler.copyMoveTutorCompatibilityToCosmeticFormes();
                logTutorCompatibility(log);
            }

        }

        // Apply custom Pokemon edits (stats/types) from file — done early so all downstream logic uses new stats
        if (customParseResult != null && customParseResult.customPokemonEdits != null) {
            List<Pokemon> allPokemon = romHandler.getPokemon();
            for (CustomEncounterFile.PokemonEditData ped : customParseResult.customPokemonEdits) {
                Pokemon pk = (ped.id >= 1 && ped.id < allPokemon.size()) ? allPokemon.get(ped.id) : null;
                if (pk == null) {
                    log.println("Warning: Unknown Pokemon ID " + ped.id + " in pokemonEdits, skipping.");
                    continue;
                }
                if (ped.name != null) pk.name = ped.name;
                if (ped.type1 != null) {
                    try { pk.primaryType = Type.valueOf(ped.type1); }
                    catch (IllegalArgumentException e) { log.println("Warning: Unknown type '" + ped.type1 + "' for Pokemon " + pk.name); }
                }
                if (ped.type2 != null) {
                    try { pk.secondaryType = Type.valueOf(ped.type2); }
                    catch (IllegalArgumentException e) { log.println("Warning: Unknown type '" + ped.type2 + "' for Pokemon " + pk.name); }
                } else if (ped.type1 != null) {
                    // If type1 is set but type2 is explicitly null, clear the secondary type
                    pk.secondaryType = null;
                }
                if (ped.hp >= 0) pk.hp = ped.hp;
                if (ped.attack >= 0) pk.attack = ped.attack;
                if (ped.defense >= 0) pk.defense = ped.defense;
                if (ped.spatk >= 0) pk.spatk = ped.spatk;
                if (ped.spdef >= 0) pk.spdef = ped.spdef;
                if (ped.speed >= 0) pk.speed = ped.speed;
            }
            pokemonTraitsChanged = true;
            log.println("Custom Pokemon edits applied from file.");
            log.println();
        }

        // Apply custom evolution edits from file — done early so downstream logic uses new evolutions
        if (customParseResult != null && customParseResult.customEvolutionEdits != null) {
            List<Pokemon> allPokemon = romHandler.getPokemon();
            for (CustomEncounterFile.EvolutionEditData eed : customParseResult.customEvolutionEdits) {
                Pokemon from = (eed.fromId >= 1 && eed.fromId < allPokemon.size()) ? allPokemon.get(eed.fromId) : null;
                Pokemon to = (eed.toId >= 1 && eed.toId < allPokemon.size()) ? allPokemon.get(eed.toId) : null;
                if (from == null || to == null) {
                    log.println("Warning: Unknown Pokemon ID in evolutionEdits (from=" + eed.fromId + ", to=" + eed.toId + "), skipping.");
                    continue;
                }
                // Find existing evolution from->to and modify it, or add a new one
                EvolutionType evoType = EvolutionType.LEVEL;
                if (eed.method != null) {
                    try { evoType = EvolutionType.valueOf(eed.method); }
                    catch (IllegalArgumentException e) { log.println("Warning: Unknown evolution method '" + eed.method + "', defaulting to LEVEL"); }
                }
                boolean found = false;
                for (Evolution evo : from.evolutionsFrom) {
                    if (evo.to.number == to.number) {
                        evo.type = evoType;
                        evo.extraInfo = eed.extraInfo;
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    Evolution newEvo = new Evolution(from, to, false, evoType, eed.extraInfo);
                    from.evolutionsFrom.add(newEvo);
                    to.evolutionsTo.add(newEvo);
                }
            }
            evolutionsChanged = true;
            log.println("Custom evolution edits applied from file.");
            log.println();
        }

        // Trainer Pokemon
        // 1. Add extra Trainer Pokemon
        // 2. Set trainers to be double battles and add extra Pokemon if necessary
        // 3. Randomize Trainer Pokemon
        // 4. Modify rivals to carry starters
        // 5. Force Trainer Pokemon to be fully evolved

        // Step 1: Always run standard trainer modifications first
        if (settings.getAdditionalRegularTrainerPokemon() > 0
                || settings.getAdditionalImportantTrainerPokemon() > 0
                || settings.getAdditionalBossTrainerPokemon() > 0) {
            romHandler.addTrainerPokemon(settings);
            trainersChanged = true;
        }

        if (settings.isDoubleBattleMode()) {
            romHandler.doubleBattleMode();
            trainersChanged = true;
        }

        switch(settings.getTrainersMod()) {
            case RANDOM:
            case DISTRIBUTED:
            case MAINPLAYTHROUGH:
            case TYPE_THEMED:
            case TYPE_THEMED_ELITE4_GYMS:
                romHandler.randomizeTrainerPokes(settings);
                trainersChanged = true;
                break;
            default:
                if (settings.isTrainersLevelModified()) {
                    romHandler.onlyChangeTrainerLevels(settings);
                    trainersChanged = true;
                }
                break;
        }

        if ((settings.getTrainersMod() != Settings.TrainersMod.UNCHANGED
                || settings.getStartersMod() != Settings.StartersMod.UNCHANGED)
                && settings.isRivalCarriesStarterThroughout()) {
            romHandler.rivalCarriesStarter();
            trainersChanged = true;
        }

        if (settings.isTrainersForceFullyEvolved()) {
            romHandler.forceFullyEvolvedTrainerPokes(settings);
            trainersChanged = true;
        }

        if (settings.isBetterTrainerMovesets()) {
            romHandler.pickTrainerMovesets(settings);
            trainersChanged = true;
            trainerMovesetsChanged = true;
        }

        if (settings.isRandomizeHeldItemsForBossTrainerPokemon()
                || settings.isRandomizeHeldItemsForImportantTrainerPokemon()
                || settings.isRandomizeHeldItemsForRegularTrainerPokemon()) {
            romHandler.randomizeTrainerHeldItems(settings);
            trainersChanged = true;
        }

        // Step 2: Overlay custom trainers on top (only trainer indices present in the JSON are overwritten)
        if (customParseResult != null && customParseResult.customTrainers != null
                && !customParseResult.customTrainerIndices.isEmpty()) {
            CustomEncounterFile.fillRandomTrainerSlots(customParseResult.customTrainers, romHandler, new Random(seed));
            List<Trainer> currentTrainers = romHandler.getTrainers();
            // Load dialogue offsets before overlay so write knows where to put text
            if (romHandler instanceof Gen2RomHandler) {
                ((Gen2RomHandler) romHandler).loadTrainerDialogue(currentTrainers);
            }
            CustomEncounterFile.overlayCustomTrainers(customParseResult, currentTrainers, new Random(seed));
            romHandler.setTrainers(currentTrainers, false);
            // Apply custom class sprites
            if (customParseResult.customClassSprites != null && !customParseResult.customClassSprites.isEmpty()
                    && romHandler instanceof Gen2RomHandler) {
                ((Gen2RomHandler) romHandler).swapTrainerClassSprites(customParseResult.customClassSprites);
            }
            // Apply custom class names
            if (customParseResult.customClassNames != null && !customParseResult.customClassNames.isEmpty()) {
                List<String> classNames = romHandler.getTrainerClassNames();
                for (Map.Entry<Integer, String> entry : customParseResult.customClassNames.entrySet()) {
                    int classId = entry.getKey();
                    if (classId >= 0 && classId < classNames.size()) {
                        classNames.set(classId, entry.getValue());
                    }
                }
                romHandler.setTrainerClassNames(classNames);
            }
            // Write modified dialogue text to ROM (separate from trainer data bank)
            if (romHandler instanceof Gen2RomHandler) {
                ((Gen2RomHandler) romHandler).writeTrainerDialogue(currentTrainers);
            }
            trainersChanged = true;
            log.println("Custom trainers overlaid from file (" + customParseResult.customTrainerIndices.size() + " trainers).");
            log.println();
        }

        List<String> originalTrainerNames = getTrainerNames();
        boolean trainerNamesChanged = false;

        // Trainer names & class names randomization
        if (romHandler.canChangeTrainerText()) {
            if (settings.isRandomizeTrainerClassNames()) {
                romHandler.randomizeTrainerClassNames(settings);
                trainersChanged = true;
                trainerNamesChanged = true;
            }

            if (settings.isRandomizeTrainerNames()) {
                romHandler.randomizeTrainerNames(settings);
                trainersChanged = true;
                trainerNamesChanged = true;
            }
        }

        if (trainersChanged) {
            maybeLogTrainerChanges(log, originalTrainerNames, trainerNamesChanged, trainerMovesetsChanged);
        } else {
            log.println("Trainers: Unchanged." + NEWLINE);
        }

        // Apply metronome only mode now that trainers have been dealt with
        if (settings.getMovesetsMod() == Settings.MovesetsMod.METRONOME_ONLY) {
            romHandler.metronomeOnlyMode();
        }

        List<Trainer> trainers = romHandler.getTrainers();
        for (Trainer t : trainers) {
            for (TrainerPokemon tpk : t.pokemon) {
                checkValue = addToCV(checkValue, tpk.level, tpk.pokemon.number);
            }
        }

        // Static Pokemon
        if (romHandler.canChangeStaticPokemon()) {
            List<StaticEncounter> oldStatics = romHandler.getStaticPokemon();
            if (settings.getStaticPokemonMod() != Settings.StaticPokemonMod.UNCHANGED) { // Legendary for L
                romHandler.randomizeStaticPokemon(settings);
                staticsChanged = true;
            } else if (settings.isStaticLevelModified()) {
                romHandler.onlyChangeStaticLevels(settings);
                staticsChanged = true;
            }

            if (staticsChanged) {
                checkValue = logStaticPokemon(log, checkValue, oldStatics);
            } else {
                log.println("Static Pokemon: Unchanged." + NEWLINE);
            }
        }

        // Totem Pokemon
        if (romHandler.generationOfPokemon() == 7) {
            List<TotemPokemon> oldTotems = romHandler.getTotemPokemon();
            if (settings.getTotemPokemonMod() != Settings.TotemPokemonMod.UNCHANGED ||
                    settings.getAllyPokemonMod() != Settings.AllyPokemonMod.UNCHANGED ||
                    settings.getAuraMod() != Settings.AuraMod.UNCHANGED ||
                    settings.isRandomizeTotemHeldItems() ||
                    settings.isTotemLevelsModified()) {

                romHandler.randomizeTotemPokemon(settings);
                totemsChanged = true;
            }

            if (totemsChanged) {
                checkValue = logTotemPokemon(log, checkValue, oldTotems);
            } else {
                log.println("Totem Pokemon: Unchanged." + NEWLINE);
            }
        }

        // Wild Pokemon
        // 1. Update catch rates
        // 2. Randomize Wild Pokemon

        if (settings.isUseMinimumCatchRate()) {
            romHandler.changeCatchRates(settings);
        }

        // Step 1: Always run standard wild Pokemon randomization first
        switch (settings.getWildPokemonMod()) {
            case RANDOM:
                romHandler.randomEncounters(settings);
                wildsChanged = true;
                break;
            case AREA_MAPPING:
                romHandler.area1to1Encounters(settings);
                wildsChanged = true;
                break;
            case GLOBAL_MAPPING:
                romHandler.game1to1Encounters(settings);
                wildsChanged = true;
                break;
            default:
                if (settings.isWildLevelsModified()) {
                    romHandler.onlyChangeWildLevels(settings);
                    wildsChanged = true;
                }
                break;
        }

        // Step 2: Overlay custom encounters on top (only areas present in the JSON are overwritten)
        if (customParseResult != null && !customParseResult.customAreaNames.isEmpty()) {
            boolean useTimeOfDay = settings.isUseTimeBasedEncounters();
            List<EncounterSet> currentEncounters = romHandler.getEncounters(useTimeOfDay);

            // Fill in any RANDOM encounter slots in the custom data
            CustomEncounterFile.fillRandomSlots(customParseResult.encounters, romHandler, new Random(seed));

            // Overlay only the areas that had custom data
            CustomEncounterFile.overlayCustomEncounters(customParseResult, currentEncounters, new Random(seed));

            romHandler.setEncounters(useTimeOfDay, currentEncounters);
            wildsChanged = true;
            log.println("Custom encounters overlaid from file (" + customParseResult.customAreaNames.size() + " areas).");
            log.println();

            // Apply custom starters if specified in the file
            if (customParseResult.customStarters != null) {
                List<Pokemon> starters = customParseResult.customStarters;
                boolean allSet = starters.stream().allMatch(p -> p != null);
                if (allSet) {
                    List<Pokemon> romStarters = romHandler.getStarters();
                    for (int i = 0; i < Math.min(starters.size(), romStarters.size()); i++) {
                        romStarters.set(i, starters.get(i));
                    }
                    romHandler.setStarters(romStarters);
                    startersChanged = true;
                    log.println("Custom Starters from file:");
                    for (int i = 0; i < starters.size(); i++) {
                        log.println("  Starter " + (i + 1) + ": " + starters.get(i).name);
                    }
                    log.println();
                }
            }

            // Apply custom static Pokemon if specified in the file
            if (customParseResult.customStatics != null && romHandler.canChangeStaticPokemon()) {
                List<StaticEncounter> origStatics = romHandler.getStaticPokemon();
                for (CustomEncounterFile.StaticSlotData ssd : customParseResult.customStatics) {
                    if (ssd.romIndex >= 0 && ssd.romIndex < origStatics.size() && ssd.pokemon != null) {
                        StaticEncounter se = origStatics.get(ssd.romIndex);
                        se.pkmn = ssd.pokemon;
                        se.level = ssd.level;
                    }
                }
                romHandler.setStaticPokemon(origStatics);
                staticsChanged = true;
                log.println("Custom Static Pokemon from file:");
                for (CustomEncounterFile.StaticSlotData ssd : customParseResult.customStatics) {
                    if (ssd.pokemon != null) {
                        log.println("  Static " + ssd.romIndex + ": " + ssd.pokemon.name + " Lv" + ssd.level);
                    }
                }
                log.println();
            }
        }

        if (wildsChanged) {
            logWildPokemonChanges(log);
        } else {
            log.println("Wild Pokemon: Unchanged." + NEWLINE);
        }

        boolean useTimeBasedEncounters = settings.isUseTimeBasedEncounters() ||
                (settings.getWildPokemonMod() == Settings.WildPokemonMod.UNCHANGED && settings.isWildLevelsModified());
        List<EncounterSet> encounters = romHandler.getEncounters(useTimeBasedEncounters);
        for (EncounterSet es : encounters) {
            for (Encounter e : es.encounters) {
                checkValue = addToCV(checkValue, e.level, e.pokemon.number);
            }
        }


        // In-game trades

        List<IngameTrade> oldTrades = romHandler.getIngameTrades();
        switch(settings.getInGameTradesMod()) {
            case RANDOMIZE_GIVEN:
            case RANDOMIZE_GIVEN_AND_REQUESTED:
                romHandler.randomizeIngameTrades(settings);
                tradesChanged = true;
                break;
            default:
                break;
        }

        // Apply custom in-game trades from file
        if (customParseResult != null && customParseResult.customTrades != null) {
            List<IngameTrade> trades = romHandler.getIngameTrades();
            List<Pokemon> allPokemon = romHandler.getPokemon();
            for (CustomEncounterFile.TradeData td : customParseResult.customTrades) {
                if (td.index >= 0 && td.index < trades.size()) {
                    IngameTrade trade = trades.get(td.index);
                    if (td.givenPokemonId > 0) {
                        Pokemon pk = (td.givenPokemonId < allPokemon.size()) ? allPokemon.get(td.givenPokemonId) : null;
                        if (pk != null) trade.givenPokemon = pk;
                        else log.println("Warning: Unknown given Pokemon ID " + td.givenPokemonId + " in trade #" + td.index);
                    }
                    if (td.requestedPokemonId > 0) {
                        Pokemon pk = (td.requestedPokemonId < allPokemon.size()) ? allPokemon.get(td.requestedPokemonId) : null;
                        if (pk != null) trade.requestedPokemon = pk;
                        else log.println("Warning: Unknown requested Pokemon ID " + td.requestedPokemonId + " in trade #" + td.index);
                    }
                    if (td.nickname != null) trade.nickname = td.nickname;
                    if (td.otName != null) trade.otName = td.otName;
                    if (td.item >= 0) trade.item = td.item;
                }
            }
            romHandler.setIngameTrades(trades);
            tradesChanged = true;
            log.println("Custom in-game trades applied from file.");
            log.println();
        }

        if (tradesChanged) {
            logTrades(log, oldTrades);
        }

        // Field Items
        switch(settings.getFieldItemsMod()) {
            case SHUFFLE:
                romHandler.shuffleFieldItems();
                break;
            case RANDOM:
            case RANDOM_EVEN:
                romHandler.randomizeFieldItems(settings);
                break;
            default:
                break;
        }

        // Apply custom field items from file
        if (customParseResult != null && customParseResult.customFieldItems != null) {
            List<Integer> fieldItems = romHandler.getRegularFieldItems();
            for (Map.Entry<Integer, Integer> entry : customParseResult.customFieldItems.entrySet()) {
                int idx = entry.getKey();
                int itemId = entry.getValue();
                if (idx >= 0 && idx < fieldItems.size()) {
                    fieldItems.set(idx, itemId);
                }
            }
            romHandler.setRegularFieldItems(fieldItems);
            log.println("Custom field items applied from file.");
            log.println();
        }

        // Shops

        switch(settings.getShopItemsMod()) {
            case SHUFFLE:
                romHandler.shuffleShopItems();
                shopsChanged = true;
                break;
            case RANDOM:
                romHandler.randomizeShopItems(settings);
                shopsChanged = true;
                break;
            default:
                break;
        }

        // Apply custom shop items from file
        if (customParseResult != null && customParseResult.customShops != null && romHandler.hasShopRandomization()) {
            Map<Integer, Shop> shops = romHandler.getShopItems();
            for (Map.Entry<Integer, CustomEncounterFile.ShopData> entry : customParseResult.customShops.entrySet()) {
                int shopIdx = entry.getKey();
                CustomEncounterFile.ShopData sd = entry.getValue();
                Shop shop = shops.get(shopIdx);
                if (shop != null && sd.items != null && !sd.items.isEmpty()) {
                    shop.items = new ArrayList<>(sd.items);
                } else if (shop == null) {
                    log.println("Warning: Unknown shop index " + shopIdx + ", skipping.");
                }
            }
            romHandler.setShopItems(shops);
            shopsChanged = true;
            log.println("Custom shop items applied from file.");
            log.println();
        }

        if (shopsChanged) {
            logShops(log);
        }

        // Apply custom item prices from file
        if (customParseResult != null && customParseResult.customPrices != null && romHandler instanceof Gen2RomHandler) {
            Gen2RomHandler gen2 = (Gen2RomHandler) romHandler;
            for (Map.Entry<Integer, Integer> entry : customParseResult.customPrices.entrySet()) {
                gen2.setItemPrice(entry.getKey(), entry.getValue());
            }
            log.println("Custom item prices applied from file.");
            log.println();
        }

        // Pickup Items
        if (settings.getPickupItemsMod() == Settings.PickupItemsMod.RANDOM) {
            romHandler.randomizePickupItems(settings);
            logPickupItems(log);
        }

        // Test output for placement history
        // romHandler.renderPlacementHistory();

        // Intro Pokemon...
        romHandler.randomizeIntroPokemon();

        // Record check value?
        romHandler.writeCheckValueToROM(checkValue);

        // Save
        if (saveAsDirectory) {
            romHandler.saveRomDirectory(filename);
        } else {
            romHandler.saveRomFile(filename, seed);
        }

        // Log tail
        String gameName = romHandler.getROMName();
        if (romHandler.hasGameUpdateLoaded()) {
            gameName = gameName + " (" + romHandler.getGameUpdateVersion() + ")";
        }
        log.println("------------------------------------------------------------------");
        log.println("Randomization of " + gameName + " completed.");
        log.println("Time elapsed: " + (System.currentTimeMillis() - startTime) + "ms");
        log.println("RNG Calls: " + RandomSource.callsSinceSeed());
        log.println("------------------------------------------------------------------");
        log.println();

        // Diagnostics
        log.println("--ROM Diagnostics--");
        if (!romHandler.isRomValid()) {
            log.println(bundle.getString("Log.InvalidRomLoaded"));
        }
        romHandler.printRomDiagnostics(log);

        return checkValue;
    }

    private int logMoveTutorMoves(PrintStream log, int checkValue, List<Integer> oldMtMoves) {
        log.println("--Move Tutor Moves--");
        List<Integer> newMtMoves = romHandler.getMoveTutorMoves();
        List<Move> moves = romHandler.getMoves();
        for (int i = 0; i < newMtMoves.size(); i++) {
            log.printf("%-10s -> %-10s" + NEWLINE, moves.get(oldMtMoves.get(i)).name,
                    moves.get(newMtMoves.get(i)).name);
            checkValue = addToCV(checkValue, newMtMoves.get(i));
        }
        log.println();
        return checkValue;
    }

    private int logTMMoves(PrintStream log, int checkValue) {
        log.println("--TM Moves--");
        List<Integer> tmMoves = romHandler.getTMMoves();
        List<Move> moves = romHandler.getMoves();
        for (int i = 0; i < tmMoves.size(); i++) {
            log.printf("TM%02d %s" + NEWLINE, i + 1, moves.get(tmMoves.get(i)).name);
            checkValue = addToCV(checkValue, tmMoves.get(i));
        }
        log.println();
        return checkValue;
    }

    private void logTrades(PrintStream log, List<IngameTrade> oldTrades) {
        log.println("--In-Game Trades--");
        List<IngameTrade> newTrades = romHandler.getIngameTrades();
        int size = oldTrades.size();
        for (int i = 0; i < size; i++) {
            IngameTrade oldT = oldTrades.get(i);
            IngameTrade newT = newTrades.get(i);
            log.printf("Trade %-11s -> %-11s the %-11s        ->      %-11s -> %-15s the %s" + NEWLINE,
                    oldT.requestedPokemon != null ? oldT.requestedPokemon.fullName() : "Any",
                    oldT.nickname, oldT.givenPokemon.fullName(),
                    newT.requestedPokemon != null ? newT.requestedPokemon.fullName() : "Any",
                    newT.nickname, newT.givenPokemon.fullName());
        }
        log.println();
    }

    private void logMovesetChanges(PrintStream log) {
        log.println("--Pokemon Movesets--");
        List<String> movesets = new ArrayList<>();
        Map<Integer, List<MoveLearnt>> moveData = romHandler.getMovesLearnt();
        Map<Integer, List<Integer>> eggMoves = romHandler.getEggMoves();
        List<Move> moves = romHandler.getMoves();
        List<Pokemon> pkmnList = romHandler.getPokemonInclFormes();
        int i = 1;
        for (Pokemon pkmn : pkmnList) {
            if (pkmn == null || pkmn.actuallyCosmetic) {
                continue;
            }
            StringBuilder evoStr = new StringBuilder();
            try {
                evoStr.append(" -> ").append(pkmn.evolutionsFrom.get(0).to.fullName());
            } catch (Exception e) {
                evoStr.append(" (no evolution)");
            }

            StringBuilder sb = new StringBuilder();

            if (romHandler instanceof Gen1RomHandler) {
                sb.append(String.format("%03d %s", i, pkmn.fullName()))
                        .append(evoStr).append(System.getProperty("line.separator"))
                        .append(String.format("HP   %-3d", pkmn.hp)).append(System.getProperty("line.separator"))
                        .append(String.format("ATK  %-3d", pkmn.attack)).append(System.getProperty("line.separator"))
                        .append(String.format("DEF  %-3d", pkmn.defense)).append(System.getProperty("line.separator"))
                        .append(String.format("SPEC %-3d", pkmn.special)).append(System.getProperty("line.separator"))
                        .append(String.format("SPE  %-3d", pkmn.speed)).append(System.getProperty("line.separator"));
            } else {
                sb.append(String.format("%03d %s", i, pkmn.fullName()))
                        .append(evoStr).append(System.getProperty("line.separator"))
                        .append(String.format("HP  %-3d", pkmn.hp)).append(System.getProperty("line.separator"))
                        .append(String.format("ATK %-3d", pkmn.attack)).append(System.getProperty("line.separator"))
                        .append(String.format("DEF %-3d", pkmn.defense)).append(System.getProperty("line.separator"))
                        .append(String.format("SPA %-3d", pkmn.spatk)).append(System.getProperty("line.separator"))
                        .append(String.format("SPD %-3d", pkmn.spdef)).append(System.getProperty("line.separator"))
                        .append(String.format("SPE %-3d", pkmn.speed)).append(System.getProperty("line.separator"));
            }

            i++;

            List<MoveLearnt> data = moveData.get(pkmn.number);
            for (MoveLearnt ml : data) {
                try {
                    if (ml.level == 0) {
                        sb.append("Learned upon evolution: ")
                                .append(moves.get(ml.move).name).append(System.getProperty("line.separator"));
                    } else {
                        sb.append("Level ")
                                .append(String.format("%-2d", ml.level))
                                .append(": ")
                                .append(moves.get(ml.move).name).append(System.getProperty("line.separator"));
                    }
                } catch (NullPointerException ex) {
                    sb.append("invalid move at level").append(ml.level);
                }
            }
            List<Integer> eggMove = eggMoves.get(pkmn.number);
            if (eggMove != null && eggMove.size() != 0) {
                sb.append("Egg Moves:").append(System.getProperty("line.separator"));
                for (Integer move : eggMove) {
                    sb.append(" - ").append(moves.get(move).name).append(System.getProperty("line.separator"));
                }
            }

            movesets.add(sb.toString());
        }
        Collections.sort(movesets);
        for (String moveset : movesets) {
            log.println(moveset);
        }
        log.println();
    }

    private void logMoveUpdates(PrintStream log) {
        log.println("--Move Updates--");
        List<Move> moves = romHandler.getMoves();
        Map<Integer, boolean[]> moveUpdates = romHandler.getMoveUpdates();
        for (int moveID : moveUpdates.keySet()) {
            boolean[] changes = moveUpdates.get(moveID);
            Move mv = moves.get(moveID);
            List<String> nonTypeChanges = new ArrayList<>();
            if (changes[0]) {
                nonTypeChanges.add(String.format("%d power", mv.power));
            }
            if (changes[1]) {
                nonTypeChanges.add(String.format("%d PP", mv.pp));
            }
            if (changes[2]) {
                nonTypeChanges.add(String.format("%.00f%% accuracy", mv.hitratio));
            }
            String logStr = "Made " + mv.name;
            // type or not?
            if (changes[3]) {
                logStr += " be " + mv.type + "-type";
                if (nonTypeChanges.size() > 0) {
                    logStr += " and";
                }
            }
            if (changes[4]) {
                if (mv.category == MoveCategory.PHYSICAL) {
                    logStr += " a Physical move";
                } else if (mv.category == MoveCategory.SPECIAL) {
                    logStr += " a Special move";
                } else if (mv.category == MoveCategory.STATUS) {
                    logStr += " a Status move";
                }
            }
            if (nonTypeChanges.size() > 0) {
                logStr += " have ";
                if (nonTypeChanges.size() == 3) {
                    logStr += nonTypeChanges.get(0) + ", " + nonTypeChanges.get(1) + " and " + nonTypeChanges.get(2);
                } else if (nonTypeChanges.size() == 2) {
                    logStr += nonTypeChanges.get(0) + " and " + nonTypeChanges.get(1);
                } else {
                    logStr += nonTypeChanges.get(0);
                }
            }
            log.println(logStr);
        }
        log.println();
    }

    private void logEvolutionChanges(PrintStream log) {
        log.println("--Randomized Evolutions--");
        List<Pokemon> allPokes = romHandler.getPokemonInclFormes();
        for (Pokemon pk : allPokes) {
            if (pk != null && !pk.actuallyCosmetic) {
                int numEvos = pk.evolutionsFrom.size();
                if (numEvos > 0) {
                    StringBuilder evoStr = new StringBuilder(pk.evolutionsFrom.get(0).toFullName());
                    for (int i = 1; i < numEvos; i++) {
                        if (i == numEvos - 1) {
                            evoStr.append(" and ").append(pk.evolutionsFrom.get(i).toFullName());
                        } else {
                            evoStr.append(", ").append(pk.evolutionsFrom.get(i).toFullName());
                        }
                    }
                    log.printf("%-15s -> %-15s" + NEWLINE, pk.fullName(), evoStr.toString());
                }
            }
        }

        log.println();
    }

    private void logPokemonTraitChanges(final PrintStream log) {
        List<Pokemon> allPokes = romHandler.getPokemonInclFormes();
        String[] itemNames = romHandler.getItemNames();
        // Log base stats & types
        log.println("--Pokemon Base Stats & Types--");
        if (romHandler instanceof Gen1RomHandler) {
            log.println("NUM|NAME      |TYPE             |  HP| ATK| DEF| SPE|SPEC");
            for (Pokemon pkmn : allPokes) {
                if (pkmn != null) {
                    String typeString = pkmn.primaryType == null ? "???" : pkmn.primaryType.toString();
                    if (pkmn.secondaryType != null) {
                        typeString += "/" + pkmn.secondaryType.toString();
                    }
                    log.printf("%3d|%-10s|%-17s|%4d|%4d|%4d|%4d|%4d" + NEWLINE, pkmn.number, pkmn.fullName(), typeString,
                            pkmn.hp, pkmn.attack, pkmn.defense, pkmn.speed, pkmn.special );
                }

            }
        } else {
            String nameSp = "      ";
            String nameSpFormat = "%-13s";
            String abSp = "    ";
            String abSpFormat = "%-12s";
            if (romHandler.generationOfPokemon() == 5) {
                nameSp = "         ";
            } else if (romHandler.generationOfPokemon() == 6) {
                nameSp = "            ";
                nameSpFormat = "%-16s";
                abSp = "      ";
                abSpFormat = "%-14s";
            } else if (romHandler.generationOfPokemon() >= 7) {
                nameSp = "            ";
                nameSpFormat = "%-16s";
                abSp = "        ";
                abSpFormat = "%-16s";
            }

            log.print("NUM|NAME" + nameSp + "|TYPE             |  HP| ATK| DEF|SATK|SDEF| SPD");
            int abils = romHandler.abilitiesPerPokemon();
            for (int i = 0; i < abils; i++) {
                log.print("|ABILITY" + (i + 1) + abSp);
            }
            log.print("|ITEM");
            log.println();
            int i = 0;
            for (Pokemon pkmn : allPokes) {
                if (pkmn != null && !pkmn.actuallyCosmetic) {
                    i++;
                    String typeString = pkmn.primaryType == null ? "???" : pkmn.primaryType.toString();
                    if (pkmn.secondaryType != null) {
                        typeString += "/" + pkmn.secondaryType.toString();
                    }
                    log.printf("%3d|" + nameSpFormat + "|%-17s|%4d|%4d|%4d|%4d|%4d|%4d", i, pkmn.fullName(), typeString,
                            pkmn.hp, pkmn.attack, pkmn.defense, pkmn.spatk, pkmn.spdef, pkmn.speed);
                    if (abils > 0) {
                        log.printf("|" + abSpFormat + "|" + abSpFormat, romHandler.abilityName(pkmn.ability1),
                                pkmn.ability1 == pkmn.ability2 ? "--" : romHandler.abilityName(pkmn.ability2));
                        if (abils > 2) {
                            log.printf("|" + abSpFormat, romHandler.abilityName(pkmn.ability3));
                        }
                    }
                    log.print("|");
                    if (pkmn.guaranteedHeldItem > 0) {
                        log.print(itemNames[pkmn.guaranteedHeldItem] + " (100%)");
                    } else {
                        int itemCount = 0;
                        if (pkmn.commonHeldItem > 0) {
                            itemCount++;
                            log.print(itemNames[pkmn.commonHeldItem] + " (common)");
                        }
                        if (pkmn.rareHeldItem > 0) {
                            if (itemCount > 0) {
                                log.print(", ");
                            }
                            itemCount++;
                            log.print(itemNames[pkmn.rareHeldItem] + " (rare)");
                        }
                        if (pkmn.darkGrassHeldItem > 0) {
                            if (itemCount > 0) {
                                log.print(", ");
                            }
                            log.print(itemNames[pkmn.darkGrassHeldItem] + " (dark grass only)");
                        }
                    }
                    log.println();
                }

            }
        }
        log.println();
    }

    private void logTMHMCompatibility(final PrintStream log) {
        log.println("--TM Compatibility--");
        Map<Pokemon, boolean[]> compat = romHandler.getTMHMCompatibility();
        List<Integer> tmHMs = new ArrayList<>(romHandler.getTMMoves());
        tmHMs.addAll(romHandler.getHMMoves());
        List<Move> moveData = romHandler.getMoves();

        logCompatibility(log, compat, tmHMs, moveData, true);
    }

    private void logTutorCompatibility(final PrintStream log) {
        log.println("--Move Tutor Compatibility--");
        Map<Pokemon, boolean[]> compat = romHandler.getMoveTutorCompatibility();
        List<Integer> tutorMoves = romHandler.getMoveTutorMoves();
        List<Move> moveData = romHandler.getMoves();

        logCompatibility(log, compat, tutorMoves, moveData, false);
    }

    private void logCompatibility(final PrintStream log, Map<Pokemon, boolean[]> compat, List<Integer> moveList,
                                  List<Move> moveData, boolean includeTMNumber) {
        int tmCount = romHandler.getTMCount();
        for (Map.Entry<Pokemon, boolean[]> entry : compat.entrySet()) {
            Pokemon pkmn = entry.getKey();
            if (pkmn.actuallyCosmetic) continue;
            boolean[] flags = entry.getValue();

            String nameSpFormat = "%-14s";
            if (romHandler.generationOfPokemon() >= 6) {
                nameSpFormat = "%-17s";
            }
            log.printf("%3d " + nameSpFormat, pkmn.number, pkmn.fullName() + " ");

            for (int i = 1; i < flags.length; i++) {
                String moveName = moveData.get(moveList.get(i - 1)).name;
                if (moveName.length() == 0) {
                    moveName = "(BLANK)";
                }
                int moveNameLength = moveName.length();
                if (flags[i]) {
                    if (includeTMNumber) {
                        if (i <= tmCount) {
                            log.printf("|TM%02d %" + moveNameLength + "s ", i, moveName);
                        } else {
                            log.printf("|HM%02d %" + moveNameLength + "s ", i-tmCount, moveName);
                        }
                    } else {
                        log.printf("|%" + moveNameLength + "s ", moveName);
                    }
                } else {
                    if (includeTMNumber) {
                        log.printf("| %" + (moveNameLength+4) + "s ", "-");
                    } else {
                        log.printf("| %" + (moveNameLength-1) + "s ", "-");
                    }
                }
            }
            log.println("|");
        }
        log.println("");
    }

    private void logUpdatedEvolutions(final PrintStream log, Set<EvolutionUpdate> updatedEvolutions,
                                      Set<EvolutionUpdate> otherUpdatedEvolutions) {
        for (EvolutionUpdate evo: updatedEvolutions) {
            if (otherUpdatedEvolutions != null && otherUpdatedEvolutions.contains(evo)) {
                log.println(evo.toString() + " (Overwritten by \"Make Evolutions Easier\", see below)");
            } else {
                log.println(evo.toString());
            }
        }
        log.println();
    }

    private void logStarters(final PrintStream log) {

        switch(settings.getStartersMod()) {
            case CUSTOM:
                log.println("--Custom Starters--");
                break;
            case COMPLETELY_RANDOM:
                log.println("--Random Starters--");
                break;
            case RANDOM_WITH_TWO_EVOLUTIONS:
                log.println("--Random 2-Evolution Starters--");
                break;
            default:
                break;
        }

        List<Pokemon> starters = romHandler.getPickedStarters();
        int i = 1;
        for (Pokemon starter: starters) {
            log.println("Set starter " + i + " to " + starter.fullName());
            i++;
        }
        log.println();
    }

    private void logWildPokemonChanges(final PrintStream log) {

        log.println("--Wild Pokemon--");
        boolean useTimeBasedEncounters = settings.isUseTimeBasedEncounters() ||
                (settings.getWildPokemonMod() == Settings.WildPokemonMod.UNCHANGED && settings.isWildLevelsModified());
        List<EncounterSet> encounters = romHandler.getEncounters(useTimeBasedEncounters);
        int idx = 0;
        for (EncounterSet es : encounters) {
            idx++;
            log.print("Set #" + idx + " ");
            if (es.displayName != null) {
                log.print("- " + es.displayName + " ");
            }
            log.print("(rate=" + es.rate + ")");
            log.println();
            for (Encounter e : es.encounters) {
                StringBuilder sb = new StringBuilder();
                if (e.isSOS) {
                    String stringToAppend;
                    switch (e.sosType) {
                        case RAIN:
                            stringToAppend = "Rain SOS: ";
                            break;
                        case HAIL:
                            stringToAppend = "Hail SOS: ";
                            break;
                        case SAND:
                            stringToAppend = "Sand SOS: ";
                            break;
                        default:
                            stringToAppend = "  SOS: ";
                            break;
                    }
                    sb.append(stringToAppend);
                }
                sb.append(e.pokemon.fullName()).append(" Lv");
                if (e.maxLevel > 0 && e.maxLevel != e.level) {
                    sb.append("s ").append(e.level).append("-").append(e.maxLevel);
                } else {
                    sb.append(e.level);
                }
                String whitespaceFormat = romHandler.generationOfPokemon() == 7 ? "%-31s" : "%-25s";
                log.print(String.format(whitespaceFormat, sb));
                StringBuilder sb2 = new StringBuilder();
                if (romHandler instanceof Gen1RomHandler) {
                    sb2.append(String.format("HP %-3d ATK %-3d DEF %-3d SPECIAL %-3d SPEED %-3d", e.pokemon.hp, e.pokemon.attack, e.pokemon.defense, e.pokemon.special, e.pokemon.speed));
                } else {
                    sb2.append(String.format("HP %-3d ATK %-3d DEF %-3d SPATK %-3d SPDEF %-3d SPEED %-3d", e.pokemon.hp, e.pokemon.attack, e.pokemon.defense, e.pokemon.spatk, e.pokemon.spdef, e.pokemon.speed));
                }
                log.print(sb2);
                log.println();
            }
            log.println();
        }
        log.println();
    }

    private void maybeLogTrainerChanges(final PrintStream log, List<String> originalTrainerNames, boolean trainerNamesChanged, boolean logTrainerMovesets) {
        log.println("--Trainers Pokemon--");
        List<Trainer> trainers = romHandler.getTrainers();
        for (Trainer t : trainers) {
            log.print("#" + t.index + " ");
            String originalTrainerName = originalTrainerNames.get(t.index);
            String currentTrainerName = "";
            if (t.fullDisplayName != null) {
                currentTrainerName = t.fullDisplayName;
            } else if (t.name != null) {
                currentTrainerName = t.name;
            }
            if (!currentTrainerName.isEmpty()) {
                if (trainerNamesChanged) {
                    log.printf("(%s => %s)", originalTrainerName, currentTrainerName);
                } else {
                    log.printf("(%s)", currentTrainerName);
                }
            }
            if (t.offset != 0) {
                log.printf("@%X", t.offset);
            }

            String[] itemNames = romHandler.getItemNames();
            if (logTrainerMovesets) {
                log.println();
                for (TrainerPokemon tpk : t.pokemon) {
                    List<Move> moves = romHandler.getMoves();
                    log.printf(tpk.toString(), itemNames[tpk.heldItem]);
                    log.print(", Ability: " + romHandler.abilityName(romHandler.getAbilityForTrainerPokemon(tpk)));
                    log.print(" - ");
                    boolean first = true;
                    for (int move : tpk.moves) {
                        if (move != 0) {
                            if (!first) {
                                log.print(", ");
                            }
                            log.print(moves.get(move).name);
                            first = false;
                        }
                    }
                    log.println();
                }
            } else {
                log.print(" - ");
                boolean first = true;
                for (TrainerPokemon tpk : t.pokemon) {
                    if (!first) {
                        log.print(", ");
                    }
                    log.printf(tpk.toString(), itemNames[tpk.heldItem]);
                    first = false;
                }
            }
            log.println();
        }
        log.println();
    }

    private int logStaticPokemon(final PrintStream log, int checkValue, List<StaticEncounter> oldStatics) {

        List<StaticEncounter> newStatics = romHandler.getStaticPokemon();

        log.println("--Static Pokemon--");
        Map<String, Integer> seenPokemon = new TreeMap<>();
        for (int i = 0; i < oldStatics.size(); i++) {
            StaticEncounter oldP = oldStatics.get(i);
            StaticEncounter newP = newStatics.get(i);
            checkValue = addToCV(checkValue, newP.pkmn.number);
            String oldStaticString = oldP.toString(settings.isStaticLevelModified());
            log.print(oldStaticString);
            if (seenPokemon.containsKey(oldStaticString)) {
                int amount = seenPokemon.get(oldStaticString);
                log.print("(" + (++amount) + ")");
                seenPokemon.put(oldStaticString, amount);
            } else {
                seenPokemon.put(oldStaticString, 1);
            }
            log.println(" => " + newP.toString(settings.isStaticLevelModified()));
        }
        log.println();

        return checkValue;
    }

    private int logTotemPokemon(final PrintStream log, int checkValue, List<TotemPokemon> oldTotems) {

        List<TotemPokemon> newTotems = romHandler.getTotemPokemon();

        String[] itemNames = romHandler.getItemNames();
        log.println("--Totem Pokemon--");
        for (int i = 0; i < oldTotems.size(); i++) {
            TotemPokemon oldP = oldTotems.get(i);
            TotemPokemon newP = newTotems.get(i);
            checkValue = addToCV(checkValue, newP.pkmn.number);
            log.println(oldP.pkmn.fullName() + " =>");
            log.printf(newP.toString(),itemNames[newP.heldItem]);
        }
        log.println();

        return checkValue;
    }

    private void logMoveChanges(final PrintStream log) {

        log.println("--Move Data--");
        log.print("NUM|NAME           |TYPE    |POWER|ACC.|PP");
        if (romHandler.hasPhysicalSpecialSplit()) {
            log.print(" |CATEGORY");
        }
        log.println();
        List<Move> allMoves = romHandler.getMoves();
        for (Move mv : allMoves) {
            if (mv != null) {
                String mvType = (mv.type == null) ? "???" : mv.type.toString();
                log.printf("%3d|%-15s|%-8s|%5d|%4d|%3d", mv.internalId, mv.name, mvType, mv.power,
                        (int) mv.hitratio, mv.pp);
                if (romHandler.hasPhysicalSpecialSplit()) {
                    log.printf("| %s", mv.category.toString());
                }
                log.println();
            }
        }
        log.println();
    }

    private void logShops(final PrintStream log) {
        String[] itemNames = romHandler.getItemNames();
        log.println("--Shops--");
        Map<Integer, Shop> shopsDict = romHandler.getShopItems();
        for (int shopID : shopsDict.keySet()) {
            Shop shop = shopsDict.get(shopID);
            log.printf("%s", shop.name);
            log.println();
            List<Integer> shopItems = shop.items;
            for (int shopItemID : shopItems) {
                log.printf("- %5s", itemNames[shopItemID]);
                log.println();
            }
            
            log.println();
        }
        log.println();
    }

    private void logPickupItems(final PrintStream log) {
        List<PickupItem> pickupItems = romHandler.getPickupItems();
        String[] itemNames = romHandler.getItemNames();
        log.println("--Pickup Items--");
        for (int levelRange = 0; levelRange < 10; levelRange++) {
            int startingLevel = (levelRange * 10) + 1;
            int endingLevel = (levelRange + 1) * 10;
            log.printf("Level %s-%s", startingLevel, endingLevel);
            log.println();
            TreeMap<Integer, List<String>> itemListPerProbability = new TreeMap<>();
            for (PickupItem pickupItem : pickupItems) {
                int probability = pickupItem.probabilities[levelRange];
                if (itemListPerProbability.containsKey(probability)) {
                    itemListPerProbability.get(probability).add(itemNames[pickupItem.item]);
                } else if (probability > 0) {
                    List<String> itemList = new ArrayList<>();
                    itemList.add(itemNames[pickupItem.item]);
                    itemListPerProbability.put(probability, itemList);
                }
            }
            for (Map.Entry<Integer, List<String>> itemListPerProbabilityEntry : itemListPerProbability.descendingMap().entrySet()) {
                int probability = itemListPerProbabilityEntry.getKey();
                List<String> itemList = itemListPerProbabilityEntry.getValue();
                String itemsString = String.join(", ", itemList);
                log.printf("%d%%: %s", probability, itemsString);
                log.println();
            }
            log.println();
        }
        log.println();
    }

    private List<String> getTrainerNames() {
        List<String> trainerNames = new ArrayList<>();
        trainerNames.add(""); // for index 0
        List<Trainer> trainers = romHandler.getTrainers();
        for (Trainer t : trainers) {
            if (t.fullDisplayName != null) {
                trainerNames.add(t.fullDisplayName);
            } else if (t.name != null) {
                trainerNames.add(t.name);
            } else {
                trainerNames.add("");
            }
        }
        return trainerNames;
    }

    
    private static int addToCV(int checkValue, int... values) {
        for (int value : values) {
            checkValue = Integer.rotateLeft(checkValue, 3);
            checkValue ^= value;
        }
        return checkValue;
    }
}