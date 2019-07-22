package me.tooster.server;


import me.tooster.common.CommandException;
import me.tooster.common.Formatter;
import me.tooster.server.MTG.GameStateMachine;

import java.util.*;

import static me.tooster.server.ServerCommand.*;

/**
 * Hub manages connected players and
 */
public class Hub {

    private final HubStateMachine  stageFSM = new HubStateMachine();
    private       GameStateMachine gameFSM;

    private final List<User>           players  = new ArrayList<>(); // players connected to session
    private int                        ID       = 0;                                             // ID for objects. Collective for players and cards
    private final Map<Integer, Object> mappings = new HashMap<>();

    //------------------------------------------------------------------------------------------------------------------

     Hub() {}
    /// Represents the stages of game aka preparation for duel etc.


    /**
     * @return stage finite state machine for the hub
     */
    HubStateMachine getStageFSM() { return stageFSM; }

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
    boolean addPlayer(User player) {

        if (players.stream().anyMatch(p -> p.getNick().equals(player.getNick())))
            throw new IllegalArgumentException("User with given nick and tag was already added. WTF.");
        if (stageFSM.getCurrentState() != HubStateMachine.Stage.PREPARE) // players cannot connect
            return false;

        players.add(player);
        player.setHub(this);
        player.scParser.setCommands(
                LIST_DECKS,
                SELECT_DECK,
                SHOW_DECK,
                READY);
        broadcast(" User " + player.getNick() + " joined the hub. " + "Current players: " + players.size());
        player.transmit("Use HELP anytime to see available commands.");

        return true;
    }

    /**
     * @return List of players connected to this hub.
     */
    List<User> getPlayers() { return players; }

    /**
     * Sends message to all players
     *
     * @param msg message to send
     */
    void broadcast(String msg) {
        for (User p : players)
            p.transmit(Formatter.broadcast(msg));
    }

    /**
     * issues a command on this hub. Right now acts as a proxy to the HubStateMachine
     *
     * @param player input author
     * @param input input to process.
     */
    void process(User player, String input){
        System.err.println(player + input);
        try {
            ServerCommand.Parsed scc = ServerCommand.parse(input);
        } catch (CommandException e) {
            e.printStackTrace();
        }
        // FIXME: AFK handle

//        // player cry for help always enabled
//        if (cc.getCommand() == Parser.Command.HELP && !cc.isInternal()) {
//            cc.getPlayer().transmit(
//                    Formatter.response(Formatter.list(cc.getPlayer().getEnabledCommands().stream()
//                            .filter(c -> c.aliases().length > 0)
//                            .map(c -> "[" + c.aliases()[1] + "]\t" + c.aliases()[0])
//                            .toArray()))
//            );
//        } else
//            stageFSM.process(cc, this);
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
