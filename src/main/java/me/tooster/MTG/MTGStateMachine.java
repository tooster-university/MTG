package me.tooster.MTG;

import me.tooster.common.Command;
import me.tooster.common.FiniteStateMachine;
import me.tooster.server.User;

import static me.tooster.MTG.MTGCommand.*;
import static me.tooster.common.Command.*;


public class MTGStateMachine extends FiniteStateMachine<MTGStateMachine.State, MTGStateMachine, Command.Compiled<MTGCommand>> {

    private User[] usersInOrder;
    private int    userTurn;
    private int    userPriority;

    public MTGStateMachine(User[] usersInOrder) {
        super(State.DRAW_HAND);
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

    enum State implements FiniteStateMachine.State<MTGStateMachine.State, MTGStateMachine, Compiled<MTGCommand>>{

        DRAW_HAND, // mulligans phase

        // game gamePhase
        UNTAP ,
        UPKEEP ,
        DRAW ,

        MAIN_1 ,

        COMBAT_BEGIN ,
        COMBAT_ATTACKERS ,

        COMBAT_BLOCKERS ,

        COMBAT_FIRST_STRIKE_DAMAGE ,
        COMBAT_DAMAGE ,
        COMBAT_END ,

        MAIN_2 ,

        END_STEP ,
        CLEANUP_STEP ;

        @Override @Deprecated
        public State process(MTGStateMachine fsm, Command.Compiled<MTGCommand>... input) {
            return null;
        }

        @Override @Deprecated
        public void onEnter(MTGStateMachine fsm, State prevState) {

        }

        @Override @Deprecated
        public void onExit(MTGStateMachine fsm, State nextState) {

        }
    }
}
