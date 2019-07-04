package me.tooster;


import me.tooster.MTG.Deck;
import me.tooster.MTG.Player;
import me.tooster.exceptions.CardException;
import me.tooster.exceptions.CommandException;
import me.tooster.exceptions.DeckException;

import java.util.*;

/**
 * Hub manages connected players and
 */
public class Hub {

    private final StageFSM stageFSM = new StageFSM();       // Finite State Machine for the hub
    private GamePhaseFSM gamePhaseFSM;                              // FSM for the state. Only available during the
    // GAME stage
    private final List<Player> players = new ArrayList<>(); // players connected to session
    int ID = 0;                                             // ID for objects. Collective for players and cards
    private final Map<Integer, Object> mappings = new HashMap<>();

    //------------------------------------------------------------------------------------------------------------------

    public Hub() {}

    private static class StageFSM {

        private Stage stage = Stage.PREPARE;
        private boolean autoNext = false; // for engine to automatically trigger next() with AUTO command

        /**
         * Processes the command in a given hub being ti's context.
         * Synchronization on this method ensures
         *
         * @param hub Hub containing hub info
         * @param cc  Compiled command issued by player. You can assume, that parameters are correct and no error
         *            checking is needed.
         */
        synchronized void process(Hub hub, Parser.CompiledCommand cc) throws CommandException {
            cc.test(); // this bit here checks the compiled command for it's
            do {
                stage = stage.next(hub, cc);
                cc = new Parser.CompiledCommand(Parser.Command.AUTO);
            } while (autoNext); // autoNext will automatically advance the FSM untill flag is cleard
        }

    }

    enum Stage {
        PREPARE { // players can import decks and select a deck.

            @Override
            Stage next(Hub hub, Parser.CompiledCommand cc) {
                hub.stageFSM.autoNext = false;
                switch (cc.getCommand()) {
                    case AUTO: break;
                    case LIST_DECKS: {
                        String[] decks = ResourceManager.getInstance().getDecks().toArray(new String[]{});
                        Arrays.sort(decks);
                        cc.getPlayer().transmit(Utils.formatResponse("Decks:\n" + Utils.formatList(decks)));
                        return this;
                    }
                    case SHOW_DECK: {
                        Set<Map.Entry<String, Object>> cards =
                                ResourceManager.getInstance().getDeck(cc.getArg(0)).entrySet();
                        String[] strings =
                                cards.stream().map(e -> e.getKey() + " x" + e.getValue()).toArray(String[]::new);
                        Arrays.sort(strings);

                        cc.getPlayer().transmit(Utils.formatResponse("Cards in '" + cc.getArg(0) + "':\n"
                                + Utils.formatList(strings)));
                        return this;
                    }
                    case SELECT_DECK: {
                        try {
                            Deck deck = Deck.build(cc.getPlayer(), cc.getArg(1));
                        } catch (DeckException | CardException e) {
                            cc.getPlayer().transmit(e.getMessage());
                        }
                        return this;
                    }
                    case READY:
                        cc.getPlayer().getFlags().add(Player.Flag.READY);
                        // hub.players cannot be empty here
                        if (hub.players.stream().allMatch(p -> p.getFlags().contains(Player.Flag.READY))) {
                            hub.broadcast("Starting a game. " + hub.players.get(0) + " goes first.");
                            hub.gamePhaseFSM = new GamePhaseFSM();
                            return GAME;
                        } else {
                            hub.broadcast(Utils.formatAnnouncement("Waiting for all players to be ready."));
                            return this;
                        }
                }

                return GAME;
            }
        },
        // game phase with it's own state machine.
        GAME {
            @Override
            Stage next(Hub hub, Parser.CompiledCommand cc) {
                if (cc.getCommand() == Parser.Command.END_GAME) {
                    hub.stageFSM.autoNext = true;
                    return SUMMARY;
                }
                hub.gamePhaseFSM.next(hub, cc);
                return this; // same state
            }
        },
        SUMMARY {
            @Override
            Stage next(Hub hub, Parser.CompiledCommand cc) {
                hub.broadcast(Utils.formatAnnouncement("Winner: " + hub.gamePhaseFSM.getWinner().getNick()));
                hub.gamePhaseFSM = null;
                return PREPARE;
            }
        }; // game ended, status report and check match winning condition


        abstract Stage next(Hub hub, Parser.CompiledCommand cc);

        void onEnter(){};
    }
    /// Represents the stages of game aka preparation for duel etc.

    private static class GamePhaseFSM {

        private GamePhase gamePhase;
        private boolean autoNext = false;
        private Player winner = null;

        void next(Hub hub, Parser.CompiledCommand cc) {

            do {
                gamePhase = gamePhase.next(hub, cc);
                cc = new Parser.CompiledCommand(Parser.Command.AUTO);
            } while (autoNext);
        }

        Player getWinner() { return winner; }
    }

    private enum GamePhase {
        // game prepare gamePhase
        LIBRARY_SELECT {
            @Override
            GamePhase next(Hub hub, Parser.CompiledCommand cc) {
                hub.gamePhaseFSM.autoNext = false;
                return this;
            }
        }, // players modify the library with sideboard
        DRAW_HAND {
            @Override
            GamePhase next(Hub hub, Parser.CompiledCommand cc) {
                return this;
            }
        }, // players draw hands and mulligan

        // game gamePhase
        UNTAP {
            @Override
            GamePhase next(Hub hub, Parser.CompiledCommand cc) {
                return this;
            }
        },
        UPKEEP {
            @Override
            GamePhase next(Hub hub, Parser.CompiledCommand cc) {
                return this;
            }
        },
        DRAW {
            @Override
            GamePhase next(Hub hub, Parser.CompiledCommand cc) {
                return this;
            }
        },

        MAIN_1 {
            @Override
            GamePhase next(Hub hub, Parser.CompiledCommand cc) {
                return this;
            }
        },

        COMBAT_BEGIN {
            @Override
            GamePhase next(Hub hub, Parser.CompiledCommand cc) {
                return this;
            }
        },
        COMBAT_ATTACKERS {
            @Override
            GamePhase next(Hub hub, Parser.CompiledCommand cc) {
                return this;
            }
        },

        COMBAT_BLOCKERS {
            @Override
            GamePhase next(Hub hub, Parser.CompiledCommand cc) {
                return this;
            }
        },

        COMBAT_FIRST_STRIKE_DAMAGE {
            @Override
            GamePhase next(Hub hub, Parser.CompiledCommand cc) {
                return this;
            }
        },
        COMBAT_DAMAGE {
            @Override
            GamePhase next(Hub hub, Parser.CompiledCommand cc) {
                return this;
            }
        },
        COMBAT_END {
            @Override
            GamePhase next(Hub hub, Parser.CompiledCommand cc) {
                return this;
            }
        },

        MAIN_2 {
            @Override
            GamePhase next(Hub hub, Parser.CompiledCommand cc) {
                return this;
            }
        },

        END_STEP {
            @Override
            GamePhase next(Hub hub, Parser.CompiledCommand cc) {
                return this;
            }
        },
        CLEANUP_STEP {
            @Override
            GamePhase next(Hub hub, Parser.CompiledCommand cc) {
                return this;
            }
        };

        abstract GamePhase next(Hub hub, Parser.CompiledCommand cc);
    }

    /**
     * @return stage finite state machine for the hub
     */
    StageFSM getStageFSM() { return stageFSM; }


    /**
     * Adds player to Hub.
     * Sets up player's hub reference and his enabled commands.
     * Broadcasts players info about joining players.
     * Sends welcome message to player
     *
     * @param player player to add
     * @return true if player got connected, false otherwise
     * @throws IllegalArgumentException if somehow player with given name and tag already exists
     */
    boolean addPlayer(Player player) {

        if (players.stream().anyMatch(p -> p.getNick().equals(player.getNick())))
            throw new IllegalArgumentException("Player with given nick and tag was already added. WTF.");
        if (stageFSM.stage != Stage.PREPARE) // players cannot connect
            return false;

        players.add(player);
        player.setHub(this);
        player.setCommands(
                Parser.Command.LIST_DECKS,
                Parser.Command.SELECT_DECK,
                Parser.Command.SHOW_DECK,
                Parser.Command.READY);
        broadcast(" Player " + player.getNick() + " joined the hub. " +
                "Current players: " + players.size());
        player.transmit("Use HELP anytime to see available commands.");

        return true;
    }

    /**
     * @return List of players connected to this hub.
     */
    List<Player> getPlayers() { return players; }

    /**
     * Sends message to all players
     *
     * @param msg message to send
     */
    public void broadcast(String msg) {
        for (Player p : players)
            p.transmit(String.format("~ %s ~", msg));
    }

    /**
     * issues a command on this hub. Right now acts as a proxy to the StageFSM
     *
     * @param cc compiled command from parser.
     */
    public void issueCommand(Parser.CompiledCommand cc) throws CommandException {
        System.err.println(cc.toString());
        // FIXME: AFK handle

        // player cry for help always enabled
        if (cc.getCommand() == Parser.Command.HELP && !cc.isInternal()) {
            cc.getPlayer().transmit(
                    Utils.formatResponse(Utils.formatList(cc.getPlayer().getEnabledCommands().stream()
                            .filter(c -> c.aliases.length > 0)
                            .map(c -> "[" + c.aliases[1] + "]\t" + c.aliases[0])
                            .toArray()))
            );
        } else
            stageFSM.process(this, cc);
    }

    /**
     * Returns object in HUB that maps to it's unique ID
     *
     * @param ID ID of the queried object
     * @return querried object or null if not found
     */
    public Object getObject(int ID) { return mappings.get(ID); }

    /**
     * Creates new mapping for given ID
     *
     * @param ID ID for an object
     * @param o  object for a mapping
     */
    public void registerObject(int ID, Object o) { mappings.put(ID, o);}

    /**
     * Remove a mapping for the ID from this hub.
     *
     * @param ID ID of an object to remove the mapping
     */
    public void unregisterObject(int ID) { mappings.remove(ID); }

    /**
     * @return next unique ID for
     */
    public int nextID() { return ++ID; }
}
