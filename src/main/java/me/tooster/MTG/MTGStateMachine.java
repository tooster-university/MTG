package me.tooster.MTG;

import me.tooster.common.Command;
import me.tooster.common.FiniteStateMachine;
import me.tooster.server.User;

public class MTGStateMachine extends FiniteStateMachine<Command.Compiled<MTGCommand>, MTGStateMachine> {

    private User[] usersInOrder;
    private int    userTurn;
    private int    userPriority;

    public MTGStateMachine(User[] usersInOrder) {
        super(State.MULLIGANS);
        this.usersInOrder = usersInOrder;
        userTurn = userPriority = 0;
    }

    /**
     * Advance to the user with next priority
     * @return this priority's user after advance
     */
    public User nextPriority() {
        userPriority = (++userPriority) % usersInOrder.length;
        return usersInOrder[userPriority];
    }

    /**
     * Advance to the user with next turn
     * @return this turn's user after advance
     */
    public User nextTurn() {
        userTurn = (++userTurn) % usersInOrder.length;
        return usersInOrder[userTurn];
    }

    enum State implements FiniteStateMachine.State<Command.Compiled<MTGCommand>, MTGStateMachine> {
        MULLIGANS {
            @Override
            public FiniteStateMachine.State<Command.Compiled<MTGCommand>, MTGStateMachine> process(Command.Compiled<MTGCommand> input, MTGStateMachine context) {
                return null;
            }
        },
    }
}
