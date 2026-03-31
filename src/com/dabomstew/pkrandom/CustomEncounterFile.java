package com.dabomstew.pkrandom;

/*----------------------------------------------------------------------------*/
/*--  CustomEncounterFile.java - parses JSON custom encounter/trainer files  --*/
/*--                                                                        --*/
/*--  Part of "Universal Pokemon Randomizer ZX" by the UPR-ZX team          --*/
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

import com.dabomstew.pkrandom.pokemon.Encounter;
import com.dabomstew.pkrandom.pokemon.EncounterSet;
import com.dabomstew.pkrandom.pokemon.Pokemon;
import com.dabomstew.pkrandom.pokemon.Trainer;
import com.dabomstew.pkrandom.pokemon.TrainerPokemon;
import com.dabomstew.pkrandom.romhandlers.RomHandler;

import java.io.*;
import java.util.*;

public class CustomEncounterFile {

    /**
     * Parses a JSON encounter/trainer file exported by the web editor.
     * Pokemon are referenced by national dex ID; 0 = RANDOM.
     */
    public static ParseResult parseFile(File inputFile, RomHandler romHandler, boolean useTimeOfDay) throws IOException {
        List<EncounterSet> originalEncounters = romHandler.getEncounters(useTimeOfDay);
        List<Trainer> originalTrainers = romHandler.getTrainers();
        List<Pokemon> allPokemon = romHandler.getPokemon();

        String content = new String(java.nio.file.Files.readAllBytes(inputFile.toPath()), "UTF-8");
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        Map<String, List<SlotData>> parsedAreas = new LinkedHashMap<>();
        List<Pokemon> customStarters = null;
        List<StaticSlotData> customStatics = new ArrayList<>();
        Map<Integer, List<TrainerPokemonData>> parsedTrainers = new LinkedHashMap<>();

        // Parse encounters array
        int encIdx = content.indexOf("\"encounters\"");
        if (encIdx >= 0) {
            int arrStart = content.indexOf('[', encIdx);
            if (arrStart >= 0) {
                int arrEnd = findMatchingBracket(content, arrStart);
                String encArr = content.substring(arrStart, arrEnd + 1);

                List<String> areaObjects = splitJsonArray(encArr);
                for (String areaObj : areaObjects) {
                    String areaName = extractJsonString(areaObj, "name");
                    if (areaName == null) continue;

                    boolean isStatic = areaName.startsWith("[STATIC]");
                    boolean isStarters = areaName.equals("[STATIC] Starters");

                    int slotsStart = areaObj.indexOf("\"slots\"");
                    if (slotsStart < 0) continue;
                    int slotArrStart = areaObj.indexOf('[', slotsStart);
                    if (slotArrStart < 0) continue;
                    int slotArrEnd = findMatchingBracket(areaObj, slotArrStart);
                    List<String> slotObjects = splitJsonArray(areaObj.substring(slotArrStart, slotArrEnd + 1));

                    if (isStatic) {
                        for (String slotObj : slotObjects) {
                            int slotNum = extractJsonInt(slotObj, "slot", 1);
                            int pokeId = extractJsonInt(slotObj, "pokemon", 0);
                            int level = extractJsonInt(slotObj, "level", 5);

                            if (isStarters) {
                                if (customStarters == null) {
                                    customStarters = new ArrayList<>(Arrays.asList(null, null, null));
                                }
                                if (pokeId > 0) {
                                    Pokemon pk = lookupById(allPokemon, pokeId);
                                    if (pk != null && slotNum >= 1 && slotNum <= 3) {
                                        customStarters.set(slotNum - 1, pk);
                                    } else if (pk == null) {
                                        errors.add("Unknown Pokemon ID " + pokeId + " in " + areaName);
                                    }
                                }
                            } else {
                                StaticSlotData ssd = new StaticSlotData();
                                if (pokeId == 0) {
                                    ssd.pokemon = null;
                                } else {
                                    Pokemon pk = lookupById(allPokemon, pokeId);
                                    if (pk == null) {
                                        errors.add("Unknown Pokemon ID " + pokeId + " in " + areaName);
                                        continue;
                                    }
                                    ssd.pokemon = pk;
                                }
                                ssd.level = level;
                                ssd.romIndex = slotNum - 1;
                                customStatics.add(ssd);
                            }
                        }
                    } else {
                        List<SlotData> slots = new ArrayList<>();
                        for (String slotObj : slotObjects) {
                            SlotData sd = new SlotData();
                            sd.slotNum = extractJsonInt(slotObj, "slot", slots.size() + 1);
                            int pokeId = extractJsonInt(slotObj, "pokemon", 0);
                            if (pokeId == 0) {
                                sd.pokemon = null;
                                sd.isRandom = true;
                            } else {
                                Pokemon pk = lookupById(allPokemon, pokeId);
                                if (pk == null) {
                                    errors.add("Unknown Pokemon ID " + pokeId + " in " + areaName);
                                    continue;
                                }
                                sd.pokemon = pk;
                            }
                            sd.level = extractJsonInt(slotObj, "level", 5);
                            sd.maxLevel = extractJsonInt(slotObj, "maxLevel", 0);
                            slots.add(sd);
                        }
                        parsedAreas.put(areaName, slots);
                    }
                }
            }
        }

        // Parse trainers array
        int trIdx = content.indexOf("\"trainers\"");
        if (trIdx >= 0) {
            int arrStart = content.indexOf('[', trIdx);
            if (arrStart >= 0) {
                int arrEnd = findMatchingBracket(content, arrStart);
                String trArr = content.substring(arrStart, arrEnd + 1);
                List<String> trainerObjects = splitJsonArray(trArr);

                for (String trainerObj : trainerObjects) {
                    int index = extractJsonInt(trainerObj, "index", -1);
                    if (index < 0) continue;

                    int pokeStart = trainerObj.indexOf("\"pokemon\"");
                    if (pokeStart < 0) continue;
                    int pokeArrStart = trainerObj.indexOf('[', pokeStart);
                    if (pokeArrStart < 0) continue;
                    int pokeArrEnd = findMatchingBracket(trainerObj, pokeArrStart);
                    List<String> pokeObjects = splitJsonArray(trainerObj.substring(pokeArrStart, pokeArrEnd + 1));

                    List<TrainerPokemonData> pokes = new ArrayList<>();
                    for (String pokeObj : pokeObjects) {
                        TrainerPokemonData tpd = new TrainerPokemonData();
                        tpd.slotNum = extractJsonInt(pokeObj, "slot", pokes.size() + 1);
                        int pokeId = extractJsonInt(pokeObj, "pokemon", 0);
                        if (pokeId == 0) {
                            tpd.pokemon = null;
                            tpd.isRandom = true;
                        } else {
                            Pokemon pk = lookupById(allPokemon, pokeId);
                            if (pk == null) {
                                errors.add("Unknown Pokemon ID " + pokeId + " in trainer #" + index);
                                continue;
                            }
                            tpd.pokemon = pk;
                        }
                        tpd.level = extractJsonInt(pokeObj, "level", 5);
                        pokes.add(tpd);
                    }
                    parsedTrainers.put(index, pokes);
                }
            }
        }

        // Apply parsed encounter data to original encounter structure
        List<EncounterSet> result = new ArrayList<>();
        for (EncounterSet original : originalEncounters) {
            EncounterSet modified = new EncounterSet();
            modified.rate = original.rate;
            modified.displayName = original.displayName;
            modified.offset = original.offset;
            modified.bannedPokemon = original.bannedPokemon;

            List<SlotData> parsed = findArea(parsedAreas, original.displayName);
            if (parsed == null) {
                warnings.add("Area not found in JSON: " + original.displayName + " - keeping original");
                modified.encounters = new ArrayList<>(original.encounters);
            } else {
                for (int i = 0; i < original.encounters.size(); i++) {
                    Encounter enc = new Encounter();
                    Encounter origEnc = original.encounters.get(i);
                    if (i < parsed.size() && parsed.get(i) != null) {
                        SlotData sd = parsed.get(i);
                        enc.pokemon = sd.pokemon;
                        enc.level = sd.level;
                        enc.maxLevel = sd.maxLevel;
                    } else {
                        enc.pokemon = origEnc.pokemon;
                        enc.level = origEnc.level;
                        enc.maxLevel = origEnc.maxLevel;
                    }
                    modified.encounters.add(enc);
                }
            }
            result.add(modified);
        }

        // Apply parsed trainer data to original trainer structure
        List<Trainer> customTrainers = null;
        if (!parsedTrainers.isEmpty()) {
            customTrainers = new ArrayList<>();
            for (Trainer orig : originalTrainers) {
                Trainer modified = new Trainer();
                modified.offset = orig.offset;
                modified.index = orig.index;
                modified.trainerclass = orig.trainerclass;
                modified.name = orig.name;
                modified.fullDisplayName = orig.fullDisplayName;
                modified.tag = orig.tag;
                modified.poketype = orig.poketype;
                modified.importantTrainer = orig.importantTrainer;
                modified.multiBattleStatus = orig.multiBattleStatus;
                modified.forceStarterPosition = orig.forceStarterPosition;
                modified.requiresUniqueHeldItems = orig.requiresUniqueHeldItems;

                List<TrainerPokemonData> parsedPokes = parsedTrainers.get(orig.index);
                if (parsedPokes != null && !parsedPokes.isEmpty()) {
                    for (int i = 0; i < orig.pokemon.size(); i++) {
                        TrainerPokemon tp = orig.pokemon.get(i).copy();
                        if (i < parsedPokes.size()) {
                            TrainerPokemonData tpd = parsedPokes.get(i);
                            if (tpd.pokemon != null) {
                                boolean speciesChanged = tp.pokemon != tpd.pokemon;
                                tp.pokemon = tpd.pokemon;
                                if (speciesChanged && orig.pokemonHaveCustomMoves()) {
                                    tp.resetMoves = true;
                                }
                            } else {
                                tp.pokemon = null;
                                tp.resetMoves = true;
                            }
                            tp.level = tpd.level;
                        }
                        modified.pokemon.add(tp);
                    }
                } else {
                    for (TrainerPokemon tp : orig.pokemon) {
                        modified.pokemon.add(tp.copy());
                    }
                }
                customTrainers.add(modified);
            }
        }

        return new ParseResult(result, errors, warnings,
                customStarters, customStatics.isEmpty() ? null : customStatics,
                customTrainers);
    }

    /**
     * Tries to find an area by name, with fallbacks for time-of-day mismatches.
     * The JSON may have "(Day)" suffixed names while the ROM may not (or vice versa).
     */
    private static List<SlotData> findArea(Map<String, List<SlotData>> parsedAreas, String romName) {
        // Exact match
        List<SlotData> result = parsedAreas.get(romName);
        if (result != null) return result;

        // ROM has no suffix (useTimeOfDay=false), JSON has "(Day)" suffix
        result = parsedAreas.get(romName + " (Day)");
        if (result != null) return result;

        // ROM has suffix (useTimeOfDay=true), JSON doesn't
        String[] suffixes = {" (Day)", " (Morning)", " (Night)"};
        for (String suffix : suffixes) {
            if (romName.endsWith(suffix)) {
                result = parsedAreas.get(romName.substring(0, romName.length() - suffix.length()));
                if (result != null) return result;
            }
        }

        return null;
    }

    private static Pokemon lookupById(List<Pokemon> allPokemon, int id) {
        if (id >= 1 && id < allPokemon.size()) {
            return allPokemon.get(id);
        }
        return null;
    }

    // ── Simple JSON helpers (no library needed) ──

    private static String extractJsonString(String json, String key) {
        String search = "\"" + key + "\"";
        int idx = json.indexOf(search);
        if (idx < 0) return null;
        int colon = json.indexOf(':', idx + search.length());
        if (colon < 0) return null;
        int qStart = json.indexOf('"', colon + 1);
        if (qStart < 0) return null;
        int qEnd = json.indexOf('"', qStart + 1);
        if (qEnd < 0) return null;
        return json.substring(qStart + 1, qEnd);
    }

    private static int extractJsonInt(String json, String key, int defaultVal) {
        String search = "\"" + key + "\"";
        int idx = json.indexOf(search);
        if (idx < 0) return defaultVal;
        int colon = json.indexOf(':', idx + search.length());
        if (colon < 0) return defaultVal;
        StringBuilder num = new StringBuilder();
        for (int i = colon + 1; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '-' || (c >= '0' && c <= '9')) {
                num.append(c);
            } else if (num.length() > 0) {
                break;
            }
        }
        if (num.length() == 0) return defaultVal;
        try { return Integer.parseInt(num.toString()); }
        catch (NumberFormatException e) { return defaultVal; }
    }

    private static int findMatchingBracket(String s, int openIdx) {
        char open = s.charAt(openIdx);
        char close = open == '[' ? ']' : '}';
        int depth = 1;
        boolean inString = false;
        for (int i = openIdx + 1; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '"' && (i == 0 || s.charAt(i - 1) != '\\')) {
                inString = !inString;
            } else if (!inString) {
                if (c == open) depth++;
                else if (c == close) { depth--; if (depth == 0) return i; }
            }
        }
        return s.length() - 1;
    }

    private static List<String> splitJsonArray(String arr) {
        List<String> items = new ArrayList<>();
        String inner = arr.substring(1, arr.length() - 1).trim();
        if (inner.isEmpty()) return items;

        int depth = 0;
        boolean inString = false;
        int start = 0;
        for (int i = 0; i < inner.length(); i++) {
            char c = inner.charAt(i);
            if (c == '"' && (i == 0 || inner.charAt(i - 1) != '\\')) {
                inString = !inString;
            } else if (!inString) {
                if (c == '{' || c == '[') depth++;
                else if (c == '}' || c == ']') depth--;
                else if (c == ',' && depth == 0) {
                    items.add(inner.substring(start, i).trim());
                    start = i + 1;
                }
            }
        }
        String last = inner.substring(start).trim();
        if (!last.isEmpty()) items.add(last);
        return items;
    }

    /**
     * Fills in any RANDOM (null pokemon) slots in the encounter list.
     */
    public static void fillRandomSlots(List<EncounterSet> encounters, RomHandler romHandler, Random random) {
        List<Pokemon> validPokemon = buildValidPokemonList(romHandler);
        for (EncounterSet es : encounters) {
            for (Encounter enc : es.encounters) {
                if (enc.pokemon == null) {
                    enc.pokemon = validPokemon.get(random.nextInt(validPokemon.size()));
                }
            }
        }
    }

    /**
     * Fills in any RANDOM (null pokemon) slots in the trainer list.
     */
    public static void fillRandomTrainerSlots(List<Trainer> trainers, RomHandler romHandler, Random random) {
        List<Pokemon> validPokemon = buildValidPokemonList(romHandler);
        for (Trainer tr : trainers) {
            for (TrainerPokemon tp : tr.pokemon) {
                if (tp.pokemon == null) {
                    tp.pokemon = validPokemon.get(random.nextInt(validPokemon.size()));
                }
            }
        }
    }

    private static List<Pokemon> buildValidPokemonList(RomHandler romHandler) {
        List<Pokemon> allPokemon = romHandler.getPokemon();
        List<Pokemon> validPokemon = new ArrayList<>();
        for (int i = 1; i < allPokemon.size(); i++) {
            if (allPokemon.get(i) != null) {
                validPokemon.add(allPokemon.get(i));
            }
        }
        return validPokemon;
    }

    // Internal data classes

    static class SlotData {
        Pokemon pokemon;
        boolean isRandom;
        int level;
        int maxLevel;
        int slotNum;
    }

    static class TrainerPokemonData {
        Pokemon pokemon;
        boolean isRandom;
        int level;
        int slotNum;
    }

    public static class StaticSlotData {
        public Pokemon pokemon;
        public int level;
        public int romIndex;
    }

    public static class ParseResult {
        public final List<EncounterSet> encounters;
        public final List<String> errors;
        public final List<String> warnings;
        public final List<Pokemon> customStarters;
        public final List<StaticSlotData> customStatics;
        public final List<Trainer> customTrainers;

        public ParseResult(List<EncounterSet> encounters, List<String> errors, List<String> warnings,
                           List<Pokemon> customStarters, List<StaticSlotData> customStatics,
                           List<Trainer> customTrainers) {
            this.encounters = encounters;
            this.errors = errors;
            this.warnings = warnings;
            this.customStarters = customStarters;
            this.customStatics = customStatics;
            this.customTrainers = customTrainers;
        }

        public boolean hasErrors() {
            return !errors.isEmpty();
        }

        public boolean hasWarnings() {
            return !warnings.isEmpty();
        }
    }
}
