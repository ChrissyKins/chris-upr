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

import com.dabomstew.pkrandom.pokemon.*;
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
        Map<Integer, String[]> parsedTrainerDialogue = new LinkedHashMap<>(); // index -> [seenText, beatenText]
        Map<Integer, String> parsedTrainerNames = new LinkedHashMap<>(); // index -> new name
        Map<Integer, String> parsedClassNames = new LinkedHashMap<>(); // classId -> new class name
        Map<Integer, Integer> parsedClassSprites = new LinkedHashMap<>(); // classId -> spriteSourceClassId

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
                    boolean isStarters = areaName.equals("[STATIC] Starters") || areaName.equals("New Bark Town Starters");

                    int slotsStart = areaObj.indexOf("\"slots\"");
                    if (slotsStart < 0) continue;
                    int slotArrStart = areaObj.indexOf('[', slotsStart);
                    if (slotArrStart < 0) continue;
                    int slotArrEnd = findMatchingBracket(areaObj, slotArrStart);
                    List<String> slotObjects = splitJsonArray(areaObj.substring(slotArrStart, slotArrEnd + 1));

                    if (isStatic || isStarters) {
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

        // Parse class names array
        int cnIdx = content.indexOf("\"classNames\"");
        if (cnIdx >= 0) {
            int arrStart = content.indexOf('[', cnIdx);
            if (arrStart >= 0) {
                int arrEnd = findMatchingBracket(content, arrStart);
                String cnArr = content.substring(arrStart, arrEnd + 1);
                for (String cnObj : splitJsonArray(cnArr)) {
                    int classId = extractJsonInt(cnObj, "classId", -1);
                    String className = extractJsonString(cnObj, "name");
                    if (classId >= 0 && className != null) {
                        parsedClassNames.put(classId, className);
                    }
                }
            }
        }

        // Parse class sprites array
        int csIdx = content.indexOf("\"classSprites\"");
        if (csIdx >= 0) {
            int arrStart = content.indexOf('[', csIdx);
            if (arrStart >= 0) {
                int arrEnd = findMatchingBracket(content, arrStart);
                for (String csObj : splitJsonArray(content.substring(arrStart, arrEnd + 1))) {
                    int classId = extractJsonInt(csObj, "classId", -1);
                    int spriteFrom = extractJsonInt(csObj, "spriteFrom", -1);
                    if (classId >= 0 && spriteFrom >= 0) {
                        parsedClassSprites.put(classId, spriteFrom);
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

                    // Parse trainer name (rename)
                    String trainerName = extractJsonString(trainerObj, "trainerName");
                    if (trainerName != null) parsedTrainerNames.put(index, trainerName);

                    // Parse dialogue text
                    String seenText = unescapeJsonString(extractJsonString(trainerObj, "seenText"));
                    String beatenText = unescapeJsonString(extractJsonString(trainerObj, "beatenText"));
                    String afterText = unescapeJsonString(extractJsonString(trainerObj, "afterText"));
                    if (seenText != null || afterText != null) parsedTrainerDialogue.put(index, new String[]{seenText, beatenText, afterText});

                    int pokeStart = trainerObj.indexOf("\"pokemon\"");
                    if (pokeStart < 0 && seenText == null) continue;
                    if (pokeStart < 0) { parsedTrainers.put(index, new ArrayList<>()); continue; }
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
                        tpd.heldItem = extractJsonInt(pokeObj, "item", -1);
                        // Parse custom moves array: "moves": [1, 33, 55, 0]
                        int movesStart = pokeObj.indexOf("\"moves\"");
                        if (movesStart >= 0) {
                            int moveArrStart = pokeObj.indexOf('[', movesStart);
                            if (moveArrStart >= 0) {
                                int moveArrEnd = findMatchingBracket(pokeObj, moveArrStart);
                                String moveArr = pokeObj.substring(moveArrStart + 1, moveArrEnd).trim();
                                if (!moveArr.isEmpty()) {
                                    tpd.moves = new int[4];
                                    String[] moveParts = moveArr.split(",");
                                    for (int m = 0; m < Math.min(moveParts.length, 4); m++) {
                                        try { tpd.moves[m] = Integer.parseInt(moveParts[m].trim()); }
                                        catch (NumberFormatException e) { /* leave as 0 */ }
                                    }
                                }
                            }
                        }
                        pokes.add(tpd);
                    }
                    parsedTrainers.put(index, pokes);
                }
            }
        }

        // Parse TM moves array: [{"tm": 1, "moveId": 55}, ...]
        Map<Integer, Integer> parsedTMs = new LinkedHashMap<>();
        int tmIdx = content.indexOf("\"tms\"");
        if (tmIdx >= 0) {
            int arrStart = content.indexOf('[', tmIdx);
            if (arrStart >= 0) {
                int arrEnd = findMatchingBracket(content, arrStart);
                List<String> tmObjects = splitJsonArray(content.substring(arrStart, arrEnd + 1));
                for (String tmObj : tmObjects) {
                    int tmNum = extractJsonInt(tmObj, "tm", -1);
                    int moveId = extractJsonInt(tmObj, "moveId", -1);
                    if (tmNum > 0 && moveId >= 0) {
                        parsedTMs.put(tmNum, moveId);
                    }
                }
            }
        }

        // Parse move tutor moves array: [{"index": 0, "moveId": 55}, ...]
        Map<Integer, Integer> parsedMoveTutors = new LinkedHashMap<>();
        int mtIdx = content.indexOf("\"moveTutors\"");
        if (mtIdx >= 0) {
            int arrStart = content.indexOf('[', mtIdx);
            if (arrStart >= 0) {
                int arrEnd = findMatchingBracket(content, arrStart);
                List<String> mtObjects = splitJsonArray(content.substring(arrStart, arrEnd + 1));
                for (String mtObj : mtObjects) {
                    int idx = extractJsonInt(mtObj, "index", -1);
                    int moveId = extractJsonInt(mtObj, "moveId", -1);
                    if (idx >= 0 && moveId >= 0) {
                        parsedMoveTutors.put(idx, moveId);
                    }
                }
            }
        }

        // Parse in-game trades: [{"index": 0, "givenPokemon": 25, "requestedPokemon": 100, "nickname": "SPARKY", "level": 10, "item": 0}, ...]
        List<TradeData> parsedTrades = new ArrayList<>();
        int tdIdx = content.indexOf("\"trades\"");
        if (tdIdx >= 0) {
            int arrStart = content.indexOf('[', tdIdx);
            if (arrStart >= 0) {
                int arrEnd = findMatchingBracket(content, arrStart);
                List<String> tdObjects = splitJsonArray(content.substring(arrStart, arrEnd + 1));
                for (String tdObj : tdObjects) {
                    TradeData td = new TradeData();
                    td.index = extractJsonInt(tdObj, "index", -1);
                    if (td.index < 0) continue;
                    td.givenPokemonId = extractJsonInt(tdObj, "givenPokemon", 0);
                    td.requestedPokemonId = extractJsonInt(tdObj, "requestedPokemon", 0);
                    td.nickname = extractJsonString(tdObj, "nickname");
                    td.otName = extractJsonString(tdObj, "otName");
                    td.item = extractJsonInt(tdObj, "item", -1);
                    parsedTrades.add(td);
                }
            }
        }

        // Parse shop items: [{"index": 0, "name": "Cherrygrove City", "items": [4, 11, 30]}, ...]
        Map<Integer, ShopData> parsedShops = new LinkedHashMap<>();
        int shIdx = content.indexOf("\"shops\"");
        if (shIdx >= 0) {
            int arrStart = content.indexOf('[', shIdx);
            if (arrStart >= 0) {
                int arrEnd = findMatchingBracket(content, arrStart);
                List<String> shObjects = splitJsonArray(content.substring(arrStart, arrEnd + 1));
                for (String shObj : shObjects) {
                    int idx = extractJsonInt(shObj, "index", -1);
                    if (idx < 0) continue;
                    ShopData sd = new ShopData();
                    sd.name = extractJsonString(shObj, "name");
                    sd.items = new ArrayList<>();
                    int itemsStart = shObj.indexOf("\"items\"");
                    if (itemsStart >= 0) {
                        int itemArrStart = shObj.indexOf('[', itemsStart);
                        if (itemArrStart >= 0) {
                            int itemArrEnd = findMatchingBracket(shObj, itemArrStart);
                            String itemArr = shObj.substring(itemArrStart + 1, itemArrEnd).trim();
                            if (!itemArr.isEmpty()) {
                                for (String itemStr : itemArr.split(",")) {
                                    itemStr = itemStr.trim();
                                    if (!itemStr.isEmpty()) {
                                        try { sd.items.add(Integer.parseInt(itemStr)); }
                                        catch (NumberFormatException e) { /* skip */ }
                                    }
                                }
                            }
                        }
                    }
                    parsedShops.put(idx, sd);
                }
            }
        }

        // Parse field items: [{"index": 0, "item": 33}, ...]
        Map<Integer, Integer> parsedFieldItems = new LinkedHashMap<>();
        int fiIdx = content.indexOf("\"fieldItems\"");
        if (fiIdx >= 0) {
            int arrStart = content.indexOf('[', fiIdx);
            if (arrStart >= 0) {
                int arrEnd = findMatchingBracket(content, arrStart);
                List<String> fiObjects = splitJsonArray(content.substring(arrStart, arrEnd + 1));
                for (String fiObj : fiObjects) {
                    int idx = extractJsonInt(fiObj, "index", -1);
                    int itemId = extractJsonInt(fiObj, "item", -1);
                    if (idx >= 0 && itemId >= 0) {
                        parsedFieldItems.put(idx, itemId);
                    }
                }
            }
        }

        // Parse learnsets: {"pokemonId": [{"moveId": 33, "level": 1}, ...], ...}
        Map<Integer, List<LearnsetEntry>> parsedLearnsets = new LinkedHashMap<>();
        int lsIdx = content.indexOf("\"learnsets\"");
        if (lsIdx >= 0) {
            int objStart = content.indexOf('{', lsIdx + 11);
            if (objStart >= 0) {
                int objEnd = findMatchingBracket(content, objStart);
                String lsContent = content.substring(objStart + 1, objEnd).trim();
                // Parse key-value pairs where keys are pokemon IDs and values are move arrays
                int pos = 0;
                while (pos < lsContent.length()) {
                    int keyStart = lsContent.indexOf('"', pos);
                    if (keyStart < 0) break;
                    int keyEnd = lsContent.indexOf('"', keyStart + 1);
                    if (keyEnd < 0) break;
                    String keyStr = lsContent.substring(keyStart + 1, keyEnd);
                    int pokemonId;
                    try { pokemonId = Integer.parseInt(keyStr); }
                    catch (NumberFormatException e) { pos = keyEnd + 1; continue; }

                    int moveArrStart = lsContent.indexOf('[', keyEnd);
                    if (moveArrStart < 0) break;
                    int moveArrEnd = findMatchingBracket(lsContent, moveArrStart);
                    List<String> moveObjects = splitJsonArray(lsContent.substring(moveArrStart, moveArrEnd + 1));
                    List<LearnsetEntry> entries = new ArrayList<>();
                    for (String moveObj : moveObjects) {
                        LearnsetEntry le = new LearnsetEntry();
                        le.moveId = extractJsonInt(moveObj, "moveId", -1);
                        le.level = extractJsonInt(moveObj, "level", 1);
                        if (le.moveId >= 0) {
                            entries.add(le);
                        }
                    }
                    if (!entries.isEmpty()) {
                        parsedLearnsets.put(pokemonId, entries);
                    }
                    pos = moveArrEnd + 1;
                }
            }
        }

        // Parse item prices: [{"item": 5, "price": 200}, ...]
        Map<Integer, Integer> parsedPrices = new LinkedHashMap<>();
        int prIdx = content.indexOf("\"prices\"");
        if (prIdx >= 0) {
            int arrStart = content.indexOf('[', prIdx);
            if (arrStart >= 0) {
                int arrEnd = findMatchingBracket(content, arrStart);
                List<String> prObjects = splitJsonArray(content.substring(arrStart, arrEnd + 1));
                for (String prObj : prObjects) {
                    int itemId = extractJsonInt(prObj, "item", -1);
                    int price = extractJsonInt(prObj, "price", -1);
                    if (itemId > 0 && price >= 0) {
                        parsedPrices.put(itemId, price);
                    }
                }
            }
        }

        // Parse Pokemon edits: [{"id": 25, "type1": "ELECTRIC", "type2": null, "hp": 35, "atk": 55, ...}, ...]
        List<PokemonEditData> parsedPokemonEdits = new ArrayList<>();
        int peIdx = content.indexOf("\"pokemonEdits\"");
        if (peIdx >= 0) {
            int arrStart = content.indexOf('[', peIdx);
            if (arrStart >= 0) {
                int arrEnd = findMatchingBracket(content, arrStart);
                List<String> peObjects = splitJsonArray(content.substring(arrStart, arrEnd + 1));
                for (String peObj : peObjects) {
                    PokemonEditData ped = new PokemonEditData();
                    ped.id = extractJsonInt(peObj, "id", -1);
                    if (ped.id < 0) continue;
                    ped.name = extractJsonString(peObj, "name");
                    ped.type1 = extractJsonString(peObj, "type1");
                    ped.type2 = extractJsonString(peObj, "type2");
                    ped.hp = extractJsonInt(peObj, "hp", -1);
                    ped.attack = extractJsonInt(peObj, "atk", -1);
                    ped.defense = extractJsonInt(peObj, "def", -1);
                    ped.spatk = extractJsonInt(peObj, "spatk", -1);
                    ped.spdef = extractJsonInt(peObj, "spdef", -1);
                    ped.speed = extractJsonInt(peObj, "speed", -1);
                    parsedPokemonEdits.add(ped);
                }
            }
        }

        // Parse evolution edits: [{"fromId": 66, "toId": 67, "method": "LEVEL", "extraInfo": 28}, ...]
        List<EvolutionEditData> parsedEvolutionEdits = new ArrayList<>();
        int evIdx = content.indexOf("\"evolutionEdits\"");
        if (evIdx >= 0) {
            int arrStart = content.indexOf('[', evIdx);
            if (arrStart >= 0) {
                int arrEnd = findMatchingBracket(content, arrStart);
                List<String> evObjects = splitJsonArray(content.substring(arrStart, arrEnd + 1));
                for (String evObj : evObjects) {
                    EvolutionEditData eed = new EvolutionEditData();
                    eed.fromId = extractJsonInt(evObj, "fromId", -1);
                    eed.toId = extractJsonInt(evObj, "toId", -1);
                    if (eed.fromId < 0 || eed.toId < 0) continue;
                    eed.method = extractJsonString(evObj, "method");
                    eed.extraInfo = extractJsonInt(evObj, "extraInfo", 0);
                    parsedEvolutionEdits.add(eed);
                }
            }
        }

        // Apply parsed encounter data to original encounter structure
        Set<String> matchedJsonAreas = new HashSet<>();
        List<EncounterSet> result = new ArrayList<>();
        for (EncounterSet original : originalEncounters) {
            EncounterSet modified = new EncounterSet();
            modified.rate = original.rate;
            modified.displayName = original.displayName;
            modified.offset = original.offset;
            modified.bannedPokemon = original.bannedPokemon;

            List<SlotData> parsed = findArea(parsedAreas, original.displayName);
            if (parsed == null) {
                modified.encounters = new ArrayList<>(original.encounters);
            } else {
                matchedJsonAreas.add(findAreaKey(parsedAreas, original.displayName));
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

        // Error on JSON areas that couldn't match any ROM area
        for (String jsonArea : parsedAreas.keySet()) {
            if (!matchedJsonAreas.contains(jsonArea)) {
                // Morning/Night variants are expected extras when useTimeOfDay is off
                String upper = jsonArea.toUpperCase();
                if (upper.endsWith(" (MORNING)") || upper.endsWith(" (NIGHT)")) {
                    String dayName = jsonArea.substring(0, jsonArea.lastIndexOf('(')) + "(Day)";
                    if (matchedJsonAreas.contains(dayName) || matchedJsonAreas.contains(dayName.replace("(Day)", "(DAY)"))) {
                        continue; // Day variant was matched, Morning/Night are just unused extras
                    }
                    // Check case-insensitively
                    boolean dayMatched = false;
                    for (String matched : matchedJsonAreas) {
                        if (matched.toUpperCase().equals(upper.substring(0, upper.lastIndexOf('(')) + "(DAY)")) {
                            dayMatched = true;
                            break;
                        }
                    }
                    if (dayMatched) continue;
                }
                errors.add("JSON area not recognized by ROM: " + jsonArea);
            }
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
                String customName = parsedTrainerNames.get(orig.index);
                modified.name = (customName != null) ? customName : orig.name;
                modified.fullDisplayName = (customName != null)
                    ? orig.fullDisplayName.replace(orig.name, customName)
                    : orig.fullDisplayName;
                modified.tag = orig.tag;
                modified.poketype = orig.poketype;
                modified.importantTrainer = orig.importantTrainer;
                modified.multiBattleStatus = orig.multiBattleStatus;
                modified.forceStarterPosition = orig.forceStarterPosition;
                modified.requiresUniqueHeldItems = orig.requiresUniqueHeldItems;

                // Apply custom dialogue if present
                String[] dialogue = parsedTrainerDialogue.get(orig.index);
                if (dialogue != null) {
                    if (dialogue[0] != null) modified.seenText = dialogue[0];
                    if (dialogue[1] != null) modified.beatenText = dialogue[1];
                    if (dialogue.length > 2 && dialogue[2] != null) modified.afterText = dialogue[2];
                }

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
                            if (tpd.heldItem >= 0) {
                                tp.heldItem = tpd.heldItem;
                            }
                            if (tpd.moves != null) {
                                tp.moves = Arrays.copyOf(tpd.moves, 4);
                                tp.resetMoves = false;
                            }
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
                customTrainers,
                parsedTMs.isEmpty() ? null : parsedTMs,
                parsedMoveTutors.isEmpty() ? null : parsedMoveTutors,
                parsedTrades.isEmpty() ? null : parsedTrades,
                parsedShops.isEmpty() ? null : parsedShops,
                parsedFieldItems.isEmpty() ? null : parsedFieldItems,
                parsedPrices.isEmpty() ? null : parsedPrices,
                parsedLearnsets.isEmpty() ? null : parsedLearnsets,
                parsedPokemonEdits.isEmpty() ? null : parsedPokemonEdits,
                parsedEvolutionEdits.isEmpty() ? null : parsedEvolutionEdits,
                new HashSet<>(parsedAreas.keySet()),
                buildTrainerIndices(parsedTrainers, parsedTrainerDialogue),
                parsedClassNames,
                parsedClassSprites);
    }

    /**
     * Tries to find an area by name, with fallbacks for time-of-day mismatches.
     * The JSON may have "(Day)" suffixed names while the ROM may not (or vice versa).
     */
    private static List<SlotData> findArea(Map<String, List<SlotData>> parsedAreas, String romName) {
        // Build case-insensitive lookup
        Map<String, List<SlotData>> upper = new LinkedHashMap<>();
        for (Map.Entry<String, List<SlotData>> e : parsedAreas.entrySet()) {
            upper.put(e.getKey().toUpperCase(), e.getValue());
        }
        String key = romName.toUpperCase();

        // Exact match (case-insensitive)
        List<SlotData> result = upper.get(key);
        if (result != null) return result;

        // ROM has no suffix (useTimeOfDay=false), JSON has "(Day)" suffix
        result = upper.get(key + " (DAY)");
        if (result != null) return result;

        // ROM has suffix (useTimeOfDay=true), JSON doesn't
        String[] suffixes = {" (DAY)", " (MORNING)", " (NIGHT)"};
        for (String suffix : suffixes) {
            if (key.endsWith(suffix)) {
                result = upper.get(key.substring(0, key.length() - suffix.length()));
                if (result != null) return result;
            }
        }

        return null;
    }

    /**
     * Returns the actual key in parsedAreas that matched, for tracking which JSON areas were used.
     */
    private static String findAreaKey(Map<String, List<SlotData>> parsedAreas, String romName) {
        String key = romName.toUpperCase();
        for (String jsonKey : parsedAreas.keySet()) {
            String jk = jsonKey.toUpperCase();
            if (jk.equals(key)) return jsonKey;
            if (jk.equals(key + " (DAY)")) return jsonKey;
            String[] suffixes = {" (DAY)", " (MORNING)", " (NIGHT)"};
            for (String suffix : suffixes) {
                if (key.endsWith(suffix) && jk.equals(key.substring(0, key.length() - suffix.length()))) {
                    return jsonKey;
                }
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
        // Skip whitespace after colon to check for null
        int afterColon = colon + 1;
        while (afterColon < json.length() && json.charAt(afterColon) == ' ') afterColon++;
        if (afterColon < json.length() && json.charAt(afterColon) == 'n') return null; // null value
        int qStart = json.indexOf('"', colon + 1);
        if (qStart < 0) return null;
        // Find closing quote, handling escaped quotes
        int qEnd = qStart + 1;
        while (qEnd < json.length()) {
            if (json.charAt(qEnd) == '"' && json.charAt(qEnd - 1) != '\\') break;
            qEnd++;
        }
        if (qEnd >= json.length()) return null;
        return json.substring(qStart + 1, qEnd);
    }

    /**
     * Unescapes JSON string escapes (\\  -> \, \\\" -> \") so that the
     * game's text control codes (\n, \p, \l, \e, \r) are preserved as
     * literal backslash sequences for translateString().
     */
    private static String unescapeJsonString(String s) {
        if (s == null) return null;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == '\\' && i + 1 < s.length()) {
                char next = s.charAt(i + 1);
                if (next == '\\' || next == '"' || next == '/') {
                    sb.append(next);
                    i++;
                } else {
                    sb.append(s.charAt(i));
                }
            } else {
                sb.append(s.charAt(i));
            }
        }
        return sb.toString();
    }

    private static Set<Integer> buildTrainerIndices(Map<Integer, List<TrainerPokemonData>> parsedTrainers,
                                                     Map<Integer, String[]> parsedDialogue) {
        Set<Integer> indices = new HashSet<>(parsedTrainers.keySet());
        indices.addAll(parsedDialogue.keySet());
        return indices;
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

    /**
     * Overlays custom encounter data onto an existing (possibly randomized) encounter list.
     * Only areas whose names appear in customAreaNames are overwritten; all other areas are untouched.
     */
    public static void overlayCustomEncounters(ParseResult parseResult, List<EncounterSet> currentEncounters, Random random) {
        for (EncounterSet es : currentEncounters) {
            if (es.displayName == null) continue;
            // Check if this area has custom data (match using same logic as findArea)
            boolean hasCustom = false;
            EncounterSet customEs = null;
            for (EncounterSet parsed : parseResult.encounters) {
                if (parsed.displayName != null && areaNameMatches(es.displayName, parsed.displayName, parseResult.customAreaNames)) {
                    customEs = parsed;
                    hasCustom = true;
                    break;
                }
            }
            if (hasCustom && customEs != null) {
                for (int i = 0; i < es.encounters.size() && i < customEs.encounters.size(); i++) {
                    Encounter src = customEs.encounters.get(i);
                    Encounter dst = es.encounters.get(i);
                    dst.pokemon = src.pokemon;
                    dst.level = src.level;
                    dst.maxLevel = src.maxLevel;
                }
            }
        }
    }

    /**
     * Checks whether a ROM area name matches one from the custom file, considering time-of-day variants.
     */
    private static boolean areaNameMatches(String romName, String parsedName, Set<String> customAreaNames) {
        String rk = romName.toUpperCase();
        String pk = parsedName.toUpperCase();
        if (rk.equals(pk)) return true;
        // ROM has no suffix, JSON has "(Day)"
        if ((rk + " (DAY)").equals(pk)) return true;
        // ROM has suffix, JSON doesn't
        String[] suffixes = {" (DAY)", " (MORNING)", " (NIGHT)"};
        for (String suffix : suffixes) {
            if (rk.endsWith(suffix)) {
                String base = rk.substring(0, rk.length() - suffix.length());
                if (base.equals(pk)) return true;
            }
        }
        return false;
    }

    /**
     * Overlays custom trainer data onto an existing (possibly randomized) trainer list.
     * Only trainers whose indices appear in customTrainerIndices are overwritten; all others are untouched.
     */
    public static void overlayCustomTrainers(ParseResult parseResult, List<Trainer> currentTrainers, Random random) {
        if (parseResult.customTrainers == null) return;
        // Build a lookup of custom trainers by index
        Map<Integer, Trainer> customByIndex = new LinkedHashMap<>();
        for (Trainer ct : parseResult.customTrainers) {
            if (parseResult.customTrainerIndices.contains(ct.index)) {
                customByIndex.put(ct.index, ct);
            }
        }
        for (Trainer current : currentTrainers) {
            Trainer custom = customByIndex.get(current.index);
            if (custom != null) {
                // If custom trainer has explicit moves, enable the flag and fill defaults
                if (hasExplicitMoves(custom)) {
                    current.setPokemonHaveCustomMoves(true);
                    for (TrainerPokemon tp : custom.pokemon) {
                        if (isZeroMoves(tp.moves)) {
                            tp.resetMoves = true;
                        }
                    }
                }
                if (!custom.pokemon.isEmpty()) {
                    current.pokemon.clear();
                    for (TrainerPokemon tp : custom.pokemon) {
                        current.pokemon.add(tp.copy());
                    }
                }
                // Overlay dialogue text if custom trainer has it
                if (custom.seenText != null) {
                    current.seenText = custom.seenText;
                }
                if (custom.beatenText != null) {
                    current.beatenText = custom.beatenText;
                }
                if (custom.afterText != null) {
                    current.afterText = custom.afterText;
                }
            }
        }
    }

    private static boolean hasExplicitMoves(Trainer tr) {
        for (TrainerPokemon tp : tr.pokemon) {
            if (!isZeroMoves(tp.moves)) return true;
        }
        return false;
    }

    private static boolean isZeroMoves(int[] moves) {
        if (moves == null) return true;
        for (int m : moves) if (m != 0) return false;
        return true;
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
        int heldItem = -1;
        int[] moves;
    }

    public static class StaticSlotData {
        public Pokemon pokemon;
        public int level;
        public int romIndex;
    }

    static class TradeData {
        int index;
        int givenPokemonId;
        int requestedPokemonId;
        String nickname;
        String otName;
        int item = -1;
    }

    static class ShopData {
        String name;
        List<Integer> items;
    }

    static class LearnsetEntry {
        int moveId;
        int level;
    }

    public static class PokemonEditData {
        public int id;
        public String name;
        public String type1;
        public String type2;
        public int hp = -1;
        public int attack = -1;
        public int defense = -1;
        public int spatk = -1;
        public int spdef = -1;
        public int speed = -1;
    }

    public static class EvolutionEditData {
        public int fromId;
        public int toId;
        public String method;
        public int extraInfo;
    }

    public static class ParseResult {
        public final List<EncounterSet> encounters;
        public final List<String> errors;
        public final List<String> warnings;
        public final List<Pokemon> customStarters;
        public final List<StaticSlotData> customStatics;
        public final List<Trainer> customTrainers;
        public final Map<Integer, Integer> customTMs;
        public final Map<Integer, Integer> customMoveTutors;
        public final List<TradeData> customTrades;
        public final Map<Integer, ShopData> customShops;
        public final Map<Integer, Integer> customFieldItems;
        public final Map<Integer, Integer> customPrices;
        public final Map<Integer, List<LearnsetEntry>> customLearnsets;
        public final List<PokemonEditData> customPokemonEdits;
        public final List<EvolutionEditData> customEvolutionEdits;
        public final Set<String> customAreaNames;
        public final Set<Integer> customTrainerIndices;
        public final Map<Integer, String> customClassNames;
        public final Map<Integer, Integer> customClassSprites;

        public ParseResult(List<EncounterSet> encounters, List<String> errors, List<String> warnings,
                           List<Pokemon> customStarters, List<StaticSlotData> customStatics,
                           List<Trainer> customTrainers,
                           Map<Integer, Integer> customTMs, Map<Integer, Integer> customMoveTutors,
                           List<TradeData> customTrades, Map<Integer, ShopData> customShops,
                           Map<Integer, Integer> customFieldItems,
                           Map<Integer, Integer> customPrices,
                           Map<Integer, List<LearnsetEntry>> customLearnsets,
                           List<PokemonEditData> customPokemonEdits,
                           List<EvolutionEditData> customEvolutionEdits,
                           Set<String> customAreaNames, Set<Integer> customTrainerIndices,
                           Map<Integer, String> customClassNames,
                           Map<Integer, Integer> customClassSprites) {
            this.encounters = encounters;
            this.errors = errors;
            this.warnings = warnings;
            this.customStarters = customStarters;
            this.customStatics = customStatics;
            this.customTrainers = customTrainers;
            this.customTMs = customTMs;
            this.customMoveTutors = customMoveTutors;
            this.customTrades = customTrades;
            this.customShops = customShops;
            this.customFieldItems = customFieldItems;
            this.customPrices = customPrices;
            this.customLearnsets = customLearnsets;
            this.customPokemonEdits = customPokemonEdits;
            this.customEvolutionEdits = customEvolutionEdits;
            this.customAreaNames = customAreaNames;
            this.customTrainerIndices = customTrainerIndices;
            this.customClassNames = customClassNames;
            this.customClassSprites = customClassSprites;
        }

        public boolean hasErrors() {
            return !errors.isEmpty();
        }

        public boolean hasWarnings() {
            return !warnings.isEmpty();
        }
    }
}
