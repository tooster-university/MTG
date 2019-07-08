package me.tooster.server;


import me.tooster.common.MessageFormatter;
import me.tooster.common.Parser;
import me.tooster.server.MTG.GameStateMachine;
import me.tooster.common.CommandException;

import java.util.*;

/**
 * Hub manages connected players and
 */
public class Hub {

    private final StageStateMachine stageFSM = new StageStateMachine();
    private GameStateMachine gameFSM;

    private final List<Player> players = new ArrayList<>(); // players connected to session
    private int ID = 0;                                             // ID for objects. Collective for players and cards
    private final Map<Integer, Object> mappings = new HashMap<>();

    //------------------------------------------------------------------------------------------------------------------

     Hub() {}
    /// Represents the stages of game aka preparation for duel etc.


    /**
     * @return stage finite state machine for the hub
     */
    StageStateMachine getStageFSM() { return stageFSM; }

    /**
     * @return game state machine or null if there is no game right now.
     */
    GameStateMachine getGameFSM() {return gameFSM;}

    void setGameFSM(GameStateMachine gameFSM) { this.gameFSM = gameFSM; }

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
        if (stageFSM.getCurrentState() != StageStateMachine.Stage.PREPARE) // players cannot connect
            return false;

        players.add(player);
        player.setHub(this);
        player.setCommands(
                Parser.Command.LIST_DECKS,
                Parser.Command.SELECT_DECK,
                Parser.Command.SHOW_DECK,
                Parser.Command.READY);
        broadcast(" Player " + player.getNick() + " joined the hub. " + "Current players: " + players.size());
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
    void broadcast(String msg) {
        for (Player p : players)
            p.transmit(MessageFormatter.broadcast(msg));
    }

    /**
     * issues a command on this hub. Right now acts as a proxy to the StageStateMachine
     *
     * @param cc compiled command from parser.
     */
    void issueCommand(Parser.CompiledCommand cc) throws CommandException {
        System.err.println(cc.toString());
        // FIXME: AFK handle

        // player cry for help always enabled
        if (cc.getCommand() == Parser.Command.HELP && !cc.isInternal()) {
            cc.getPlayer().transmit(
                    MessageFormatter.response(MessageFormatter.list(cc.getPlayer().getEnabledCommands().stream()
                            .filter(c -> c.getAliases().length > 0)
                            .map(c -> "[" + c.getAliases()[1] + "]\t" + c.getAliases()[0])
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
     * @return process unique ID for
     */
    public int nextID() { return ++ID; }
}
