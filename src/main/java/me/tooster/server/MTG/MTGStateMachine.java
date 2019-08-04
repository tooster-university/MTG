package me.tooster.server.MTG;

import me.tooster.common.Command;
import me.tooster.common.FiniteStateMachine;

public class MTGStateMachine extends FiniteStateMachine<Command.Compiled<MTGCommand>, Player> {
    public MTGStateMachine() { super(null); }
}
