package com.dabomstew.pkrandom;

/*----------------------------------------------------------------------------*/
/*--  GameDataExporter.java - exports comprehensive game data as JSON       --*/
/*--  for use by the web editor and other tools.                            --*/
/*--  Works through the RomHandler interface so it supports all games.      --*/
/*----------------------------------------------------------------------------*/

import com.dabomstew.pkrandom.pokemon.*;
import com.dabomstew.pkrandom.romhandlers.Gen2RomHandler;
import com.dabomstew.pkrandom.romhandlers.RomHandler;

import java.io.*;
import java.util.*;

public class GameDataExporter {

    /**
     * Exports all useful game data to a single JSON file.
     * Works with any game generation through the RomHandler interface.
     */
    public static void exportAll(RomHandler romHandler, File outputFile) throws IOException {
        try (PrintWriter out = new PrintWriter(new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(outputFile), "UTF-8")))) {
            out.println("{");

            // Metadata
            out.println("  \"game\": " + jsonStr(romHandler.getROMName()) + ",");
            out.println("  \"code\": " + jsonStr(romHandler.getROMCode()) + ",");
            out.println("  \"generation\": " + romHandler.generationOfPokemon() + ",");

            // Pokemon
            writePokemon(out, romHandler);
            out.println(",");

            // Moves
            writeMoves(out, romHandler);
            out.println(",");

            // Items
            writeItems(out, romHandler);
            out.println(",");

            // Trainer classes
            writeTrainerClasses(out, romHandler);
            out.println(",");

            // Trainers (with pokemon details)
            writeTrainers(out, romHandler);
            out.println(",");

            // Trainer locations (gen-specific)
            writeTrainerLocations(out, romHandler);
            out.println(",");

            // Encounters
            writeEncounters(out, romHandler);
            out.println(",");

            // Static Pokemon
            writeStaticPokemon(out, romHandler);
            out.println(",");

            // Learnsets (moves learnable by each Pokemon)
            writeLearnsets(out, romHandler);
            out.println(",");

            // Evolutions
            writeEvolutions(out, romHandler);
            out.println(",");

            // TMs and HMs
            writeTMsHMs(out, romHandler);
            out.println(",");

            // Starters
            writeStarters(out, romHandler);
            out.println(",");

            // In-game trades
            writeTrades(out, romHandler);
            out.println(",");

            // Shops
            writeShops(out, romHandler);
            out.println(",");

            // Field items
            writeFieldItems(out, romHandler);
            out.println(",");

            // Move tutors
            writeMoveTutors(out, romHandler);

            out.println();
            out.println("}");
        }
    }

    private static void writePokemon(PrintWriter out, RomHandler romHandler) {
        List<Pokemon> allPokemon = romHandler.getPokemon();
        out.println("  \"pokemon\": [");
        boolean first = true;
        for (int i = 1; i < allPokemon.size(); i++) {
            Pokemon pk = allPokemon.get(i);
            if (pk == null) continue;
            if (!first) out.println(",");
            first = false;
            out.print("    { \"id\": " + pk.number
                    + ", \"name\": " + jsonStr(pk.name)
                    + ", \"type1\": " + jsonStr(pk.primaryType != null ? pk.primaryType.toString() : null)
                    + ", \"type2\": " + jsonStr(pk.secondaryType != null ? pk.secondaryType.toString() : null)
                    + ", \"hp\": " + pk.hp
                    + ", \"atk\": " + pk.attack
                    + ", \"def\": " + pk.defense
                    + ", \"spatk\": " + pk.spatk
                    + ", \"spdef\": " + pk.spdef
                    + ", \"speed\": " + pk.speed
                    + ", \"bst\": " + (pk.hp + pk.attack + pk.defense + pk.spatk + pk.spdef + pk.speed)
                    + " }");
        }
        out.println();
        out.print("  ]");
    }

    private static void writeMoves(PrintWriter out, RomHandler romHandler) {
        List<Move> allMoves = romHandler.getMoves();
        out.println("  \"moves\": [");
        boolean first = true;
        for (int i = 1; i < allMoves.size(); i++) {
            Move mv = allMoves.get(i);
            if (mv == null) continue;
            if (!first) out.println(",");
            first = false;
            out.print("    { \"id\": " + mv.number
                    + ", \"name\": " + jsonStr(mv.name)
                    + ", \"type\": " + jsonStr(mv.type != null ? mv.type.toString() : null)
                    + ", \"power\": " + mv.power
                    + ", \"accuracy\": " + (int) mv.hitratio
                    + ", \"pp\": " + mv.pp
                    + " }");
        }
        out.println();
        out.print("  ]");
    }

    private static void writeItems(PrintWriter out, RomHandler romHandler) {
        String[] itemNames = romHandler.getItemNames();
        out.println("  \"items\": [");
        boolean first = true;
        for (int i = 1; i < itemNames.length; i++) {
            if (itemNames[i] == null || itemNames[i].isEmpty()) continue;
            if (!first) out.println(",");
            first = false;
            out.print("    { \"id\": " + i + ", \"name\": " + jsonStr(itemNames[i]) + " }");
        }
        out.println();
        out.print("  ]");
    }

    private static void writeTrainerClasses(PrintWriter out, RomHandler romHandler) {
        List<String> classNames = romHandler.getTrainerClassNames();
        out.println("  \"trainerClasses\": [");
        for (int i = 0; i < classNames.size(); i++) {
            String comma = (i < classNames.size() - 1) ? "," : "";
            out.println("    { \"id\": " + i + ", \"name\": " + jsonStr(classNames.get(i)) + " }" + comma);
        }
        out.print("  ]");
    }

    private static void writeTrainers(PrintWriter out, RomHandler romHandler) {
        List<Trainer> trainers = romHandler.getTrainers();
        List<Move> allMoves = romHandler.getMoves();
        String[] itemNames = romHandler.getItemNames();

        out.println("  \"trainers\": [");
        boolean first = true;
        for (Trainer t : trainers) {
            if (!first) out.println(",");
            first = false;

            // Build pokemon array
            StringBuilder pokes = new StringBuilder("[");
            boolean pfirst = true;
            for (TrainerPokemon tp : t.pokemon) {
                if (!pfirst) pokes.append(", ");
                pfirst = false;
                pokes.append("{ \"pokemon\": ").append(jsonStr(tp.pokemon != null ? tp.pokemon.name : null));
                pokes.append(", \"level\": ").append(tp.level);

                if (t.pokemonHaveItems() && tp.heldItem > 0 && tp.heldItem < itemNames.length) {
                    pokes.append(", \"item\": ").append(jsonStr(itemNames[tp.heldItem]));
                }

                if (t.pokemonHaveCustomMoves()) {
                    pokes.append(", \"moves\": [");
                    boolean mfirst = true;
                    for (int m = 0; m < 4; m++) {
                        if (tp.moves[m] > 0 && tp.moves[m] < allMoves.size() && allMoves.get(tp.moves[m]) != null) {
                            if (!mfirst) pokes.append(", ");
                            mfirst = false;
                            pokes.append(jsonStr(allMoves.get(tp.moves[m]).name));
                        }
                    }
                    pokes.append("]");
                }
                pokes.append(" }");
            }
            pokes.append("]");

            out.print("    { \"index\": " + t.index
                    + ", \"classId\": " + t.trainerclass
                    + ", \"name\": " + jsonStr(t.name)
                    + ", \"fullName\": " + jsonStr(t.fullDisplayName)
                    + ", \"tag\": " + jsonStr(t.tag)
                    + ", \"pokemon\": " + pokes
                    + " }");
        }
        out.println();
        out.print("  ]");
    }

    private static void writeTrainerLocations(PrintWriter out, RomHandler romHandler) {
        out.println("  \"trainerLocations\": {");

        if (romHandler instanceof Gen2RomHandler) {
            Gen2RomHandler gen2 = (Gen2RomHandler) romHandler;
            Map<Integer, String> locations = gen2.getTrainerLocationMap();
            boolean first = true;
            for (Map.Entry<Integer, String> entry : locations.entrySet()) {
                if (!first) out.println(",");
                first = false;
                out.print("    " + jsonStr(String.valueOf(entry.getKey())) + ": " + jsonStr(entry.getValue()));
            }
            if (!first) out.println();
        }
        // Other generations can add their own extraction here

        out.print("  }");
    }

    private static void writeEncounters(PrintWriter out, RomHandler romHandler) {
        List<EncounterSet> encounters = romHandler.getEncounters(true);
        out.println("  \"encounters\": [");
        boolean first = true;
        for (EncounterSet es : encounters) {
            if (!first) out.println(",");
            first = false;

            StringBuilder slots = new StringBuilder("[");
            boolean sfirst = true;
            for (Encounter enc : es.encounters) {
                if (!sfirst) slots.append(", ");
                sfirst = false;
                slots.append("{ \"pokemon\": ").append(jsonStr(enc.pokemon != null ? enc.pokemon.name : null));
                slots.append(", \"level\": ").append(enc.level);
                if (enc.maxLevel > 0) {
                    slots.append(", \"maxLevel\": ").append(enc.maxLevel);
                }
                slots.append(" }");
            }
            slots.append("]");

            out.print("    { \"name\": " + jsonStr(es.displayName)
                    + ", \"rate\": " + es.rate
                    + ", \"encounters\": " + slots
                    + " }");
        }
        out.println();
        out.print("  ]");
    }

    private static void writeStaticPokemon(PrintWriter out, RomHandler romHandler) {
        out.println("  \"staticPokemon\": [");
        if (romHandler.canChangeStaticPokemon()) {
            List<StaticEncounter> statics = romHandler.getStaticPokemon();
            boolean first = true;
            for (int i = 0; i < statics.size(); i++) {
                StaticEncounter se = statics.get(i);
                if (!first) out.println(",");
                first = false;
                out.print("    { \"index\": " + i
                        + ", \"pokemon\": " + jsonStr(se.pkmn != null ? se.pkmn.name : null)
                        + ", \"level\": " + se.level
                        + " }");
            }
            out.println();
        }
        out.print("  ]");
    }

    private static void writeEvolutions(PrintWriter out, RomHandler romHandler) {
        List<Pokemon> allPokemon = romHandler.getPokemon();
        String[] itemNames = romHandler.getItemNames();
        out.println("  \"evolutions\": [");
        boolean first = true;
        for (int i = 1; i < allPokemon.size(); i++) {
            Pokemon pk = allPokemon.get(i);
            if (pk == null || pk.evolutionsFrom.isEmpty()) continue;
            for (Evolution evo : pk.evolutionsFrom) {
                if (!first) out.println(",");
                first = false;
                String detail = "";
                // Add human-readable detail based on evolution type
                switch (evo.type) {
                    case LEVEL: detail = "Level " + evo.extraInfo; break;
                    case STONE:
                        detail = (evo.extraInfo > 0 && evo.extraInfo < itemNames.length)
                                ? itemNames[evo.extraInfo] : "Stone " + evo.extraInfo;
                        break;
                    case TRADE: detail = "Trade"; break;
                    case TRADE_ITEM:
                        detail = "Trade holding " + ((evo.extraInfo > 0 && evo.extraInfo < itemNames.length)
                                ? itemNames[evo.extraInfo] : "Item " + evo.extraInfo);
                        break;
                    case HAPPINESS: detail = "Happiness"; break;
                    case HAPPINESS_DAY: detail = "Happiness (Day)"; break;
                    case HAPPINESS_NIGHT: detail = "Happiness (Night)"; break;
                    case LEVEL_ATTACK_HIGHER: detail = "Level " + evo.extraInfo + " (Atk > Def)"; break;
                    case LEVEL_DEFENSE_HIGHER: detail = "Level " + evo.extraInfo + " (Def > Atk)"; break;
                    case LEVEL_ATK_DEF_SAME: detail = "Level " + evo.extraInfo + " (Atk = Def)"; break;
                    default: detail = evo.type.toString(); break;
                }
                out.print("    { \"from\": " + jsonStr(pk.name)
                        + ", \"fromId\": " + pk.number
                        + ", \"to\": " + jsonStr(evo.to.name)
                        + ", \"toId\": " + evo.to.number
                        + ", \"method\": " + jsonStr(evo.type.toString())
                        + ", \"detail\": " + jsonStr(detail)
                        + " }");
            }
        }
        out.println();
        out.print("  ]");
    }

    private static void writeTMsHMs(PrintWriter out, RomHandler romHandler) {
        List<Integer> tmMoves = romHandler.getTMMoves();
        List<Integer> hmMoves = romHandler.getHMMoves();
        List<Move> allMoves = romHandler.getMoves();

        out.println("  \"tms\": [");
        boolean first = true;
        for (int i = 0; i < tmMoves.size(); i++) {
            if (!first) out.println(",");
            first = false;
            int moveId = tmMoves.get(i);
            String moveName = (moveId > 0 && moveId < allMoves.size() && allMoves.get(moveId) != null)
                    ? allMoves.get(moveId).name : "???";
            out.print("    { \"tm\": " + (i + 1) + ", \"move\": " + jsonStr(moveName)
                    + ", \"moveId\": " + moveId + " }");
        }
        out.println();
        out.println("  ],");

        out.println("  \"hms\": [");
        first = true;
        for (int i = 0; i < hmMoves.size(); i++) {
            if (!first) out.println(",");
            first = false;
            int moveId = hmMoves.get(i);
            String moveName = (moveId > 0 && moveId < allMoves.size() && allMoves.get(moveId) != null)
                    ? allMoves.get(moveId).name : "???";
            out.print("    { \"hm\": " + (i + 1) + ", \"move\": " + jsonStr(moveName)
                    + ", \"moveId\": " + moveId + " }");
        }
        out.println();
        out.print("  ]");
    }

    private static void writeStarters(PrintWriter out, RomHandler romHandler) {
        List<Pokemon> starters = romHandler.getStarters();
        out.println("  \"starters\": [");
        boolean first = true;
        for (int i = 0; i < starters.size(); i++) {
            Pokemon pk = starters.get(i);
            if (!first) out.println(",");
            first = false;
            out.print("    { \"slot\": " + (i + 1)
                    + ", \"pokemon\": " + jsonStr(pk != null ? pk.name : null)
                    + ", \"pokemonId\": " + (pk != null ? pk.number : 0)
                    + " }");
        }
        out.println();
        out.print("  ]");
    }

    private static void writeLearnsets(PrintWriter out, RomHandler romHandler) {
        Map<Integer, List<MoveLearnt>> movesets = romHandler.getMovesLearnt();
        List<Move> allMoves = romHandler.getMoves();

        out.println("  \"learnsets\": {");
        boolean first = true;
        for (Map.Entry<Integer, List<MoveLearnt>> entry : movesets.entrySet()) {
            if (!first) out.println(",");
            first = false;

            StringBuilder moves = new StringBuilder("[");
            boolean mfirst = true;
            for (MoveLearnt ml : entry.getValue()) {
                if (!mfirst) moves.append(", ");
                mfirst = false;
                String moveName = (ml.move > 0 && ml.move < allMoves.size() && allMoves.get(ml.move) != null)
                        ? allMoves.get(ml.move).name : "???";
                moves.append("{ \"move\": ").append(jsonStr(moveName));
                moves.append(", \"moveId\": ").append(ml.move);
                moves.append(", \"level\": ").append(ml.level);
                moves.append(" }");
            }
            moves.append("]");

            out.print("    " + jsonStr(String.valueOf(entry.getKey())) + ": " + moves);
        }
        out.println();
        out.print("  }");
    }

    private static void writeTrades(PrintWriter out, RomHandler romHandler) {
        List<IngameTrade> trades = romHandler.getIngameTrades();
        String[] itemNames = romHandler.getItemNames();
        out.println("  \"trades\": [");
        boolean first = true;
        for (int i = 0; i < trades.size(); i++) {
            IngameTrade t = trades.get(i);
            if (!first) out.println(",");
            first = false;
            out.print("    { \"index\": " + i
                    + ", \"givenPokemon\": " + jsonStr(t.givenPokemon != null ? t.givenPokemon.name : null)
                    + ", \"givenPokemonId\": " + (t.givenPokemon != null ? t.givenPokemon.number : 0)
                    + ", \"requestedPokemon\": " + jsonStr(t.requestedPokemon != null ? t.requestedPokemon.name : null)
                    + ", \"requestedPokemonId\": " + (t.requestedPokemon != null ? t.requestedPokemon.number : 0)
                    + ", \"nickname\": " + jsonStr(t.nickname)
                    + ", \"otName\": " + jsonStr(t.otName)
                    + ", \"item\": " + t.item
                    + (t.item > 0 && t.item < itemNames.length ? ", \"itemName\": " + jsonStr(itemNames[t.item]) : "")
                    + " }");
        }
        out.println();
        out.print("  ]");
    }

    private static void writeShops(PrintWriter out, RomHandler romHandler) {
        out.println("  \"shops\": [");
        if (romHandler.hasShopRandomization()) {
            Map<Integer, Shop> shops = romHandler.getShopItems();
            String[] itemNames = romHandler.getItemNames();
            boolean first = true;
            for (Map.Entry<Integer, Shop> entry : shops.entrySet()) {
                if (!first) out.println(",");
                first = false;
                Shop shop = entry.getValue();
                StringBuilder items = new StringBuilder("[");
                boolean ifirst = true;
                for (int itemId : shop.items) {
                    if (!ifirst) items.append(", ");
                    ifirst = false;
                    String itemName = (itemId > 0 && itemId < itemNames.length) ? itemNames[itemId] : "???";
                    items.append("{ \"id\": ").append(itemId)
                            .append(", \"name\": ").append(jsonStr(itemName)).append(" }");
                }
                items.append("]");
                out.print("    { \"index\": " + entry.getKey()
                        + ", \"name\": " + jsonStr(shop.name)
                        + ", \"isMainGame\": " + shop.isMainGame
                        + ", \"items\": " + items
                        + " }");
            }
            out.println();
        }
        out.print("  ]");
    }

    private static void writeFieldItems(PrintWriter out, RomHandler romHandler) {
        List<Integer> fieldItems = romHandler.getRegularFieldItems();
        String[] itemNames = romHandler.getItemNames();

        // Get location names if available (Gen 2)
        List<String> locations = null;
        if (romHandler instanceof Gen2RomHandler) {
            locations = ((Gen2RomHandler) romHandler).getFieldItemLocations();
        }

        out.println("  \"fieldItems\": [");
        boolean first = true;
        for (int i = 0; i < fieldItems.size(); i++) {
            int itemId = fieldItems.get(i);
            if (!first) out.println(",");
            first = false;
            String itemName = (itemId > 0 && itemId < itemNames.length) ? itemNames[itemId] : "???";
            String location = (locations != null && i < locations.size()) ? locations.get(i) : null;
            out.print("    { \"index\": " + i + ", \"item\": " + itemId + ", \"name\": " + jsonStr(itemName)
                    + (location != null ? ", \"location\": " + jsonStr(location) : "") + " }");
        }
        out.println();
        out.print("  ]");
    }

    private static void writeMoveTutors(PrintWriter out, RomHandler romHandler) {
        out.println("  \"moveTutors\": [");
        if (romHandler.hasMoveTutors()) {
            List<Integer> mtMoves = romHandler.getMoveTutorMoves();
            List<Move> allMoves = romHandler.getMoves();
            boolean first = true;
            for (int i = 0; i < mtMoves.size(); i++) {
                if (!first) out.println(",");
                first = false;
                int moveId = mtMoves.get(i);
                String moveName = (moveId > 0 && moveId < allMoves.size() && allMoves.get(moveId) != null)
                        ? allMoves.get(moveId).name : "???";
                out.print("    { \"index\": " + i + ", \"move\": " + jsonStr(moveName)
                        + ", \"moveId\": " + moveId + " }");
            }
            out.println();
        }
        out.print("  ]");
    }

    private static String jsonStr(String s) {
        if (s == null) return "null";
        StringBuilder sb = new StringBuilder("\"");
        for (char c : s.toCharArray()) {
            if (c == '\\') sb.append("\\\\");
            else if (c == '"') sb.append("\\\"");
            else if (c == '\n') sb.append("\\n");
            else if (c == '\r') sb.append("\\r");
            else if (c == '\t') sb.append("\\t");
            else if (c >= 32 && c < 127) sb.append(c);
            else sb.append("?");
        }
        sb.append("\"");
        return sb.toString();
    }
}
