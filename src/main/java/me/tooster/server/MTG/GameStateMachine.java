package me.tooster.server.MTG;

import me.tooster.common.FiniteStateMachine;
import me.tooster.server.Hub;
import me.tooster.server.User;

public class GameStateMachine extends FiniteStateMachine<MTGCommand.Parsed, Hub> {

    private User    winner   = null;

    public GameStateMachine() { super(Phase.DECK_SETUP);   }

    public User getWinner() { return winner; }

    enum Phase implements State<MTGCommand.Parsed, Hub> {
        // game prepare gamePhase
        DECK_SETUP {
            @Override
            public Phase process(MTGCommand.Parsed cc, Hub hub) {return this;}
        }, // players modify the library with sideboard
        DRAW_HAND {
            @Override
            public Phase process(MTGCommand.Parsed cc, Hub hub) {return this;}
        }, // players draw hands and mulligan

        // game gamePhase
        UNTAP {
            @Override
            public Phase process(MTGCommand.Parsed cc, Hub hub) {return this;}
        },
        UPKEEP {
            @Override
            public Phase process(MTGCommand.Parsed cc, Hub hub) {return this;}
        },
        DRAW {
            @Override
            public Phase process(MTGCommand.Parsed cc, Hub hub) {return this;}
        },

        MAIN_1 {
            @Override
            public Phase process(MTGCommand.Parsed cc, Hub hub) {return this;}
        },

        COMBAT_BEGIN {
            @Override
            public Phase process(MTGCommand.Parsed cc, Hub hub) {return this;}
        },
        COMBAT_ATTACKERS {
            @Override
            public Phase process(MTGCommand.Parsed cc, Hub hub) {return this;}
        },

        COMBAT_BLOCKERS {
            @Override
            public Phase process(MTGCommand.Parsed cc, Hub hub) {return this;}
        },

        COMBAT_FIRST_STRIKE_DAMAGE {
            @Override
            public Phase process(MTGCommand.Parsed cc, Hub hub) {return this;}
        },
        COMBAT_DAMAGE {
            @Override
            public Phase process(MTGCommand.Parsed cc, Hub hub) {return this;}
        },
        COMBAT_END {
            @Override
            public Phase process(MTGCommand.Parsed cc, Hub hub) {return this;}
        },

        MAIN_2 {
            @Override
            public Phase process(MTGCommand.Parsed cc, Hub hub) {return this;}
        },

        END_STEP {
            @Override
            public Phase process(MTGCommand.Parsed cc, Hub hub) {return this;}
        },
        CLEANUP_STEP {
            @Override
            public Phase process(MTGCommand.Parsed cc, Hub hub) {return this;}
        };
    }
}
