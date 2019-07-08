package me.tooster.server.MTG;

import me.tooster.common.FiniteStateMachine;
import me.tooster.server.Hub;
import me.tooster.common.Parser;
import me.tooster.server.Player;

public class GameStateMachine extends FiniteStateMachine<Hub, Parser.CompiledCommand> {

    private boolean autoNext = false;
    private Player winner = null;

    public GameStateMachine() {
        super(Phase.DECK_SETUP);
    }

    public Player getWinner() { return winner; }

    enum Phase implements State<Hub, Parser.CompiledCommand> {
        // game prepare gamePhase
        DECK_SETUP {
            @Override
            public Phase process(Hub hub, Parser.CompiledCommand cc) {return this;}
        }, // players modify the library with sideboard
        DRAW_HAND {
            @Override
            public Phase process(Hub hub, Parser.CompiledCommand cc) {return this;}
        }, // players draw hands and mulligan

        // game gamePhase
        UNTAP {
            @Override
            public Phase process(Hub hub, Parser.CompiledCommand cc) {return this;}
        },
        UPKEEP {
            @Override
            public Phase process(Hub hub, Parser.CompiledCommand cc) {return this;}
        },
        DRAW {
            @Override
            public Phase process(Hub hub, Parser.CompiledCommand cc) {return this;}
        },

        MAIN_1 {
            @Override
            public Phase process(Hub hub, Parser.CompiledCommand cc) {return this;}
        },

        COMBAT_BEGIN {
            @Override
            public Phase process(Hub hub, Parser.CompiledCommand cc) {return this;}
        },
        COMBAT_ATTACKERS {
            @Override
            public Phase process(Hub hub, Parser.CompiledCommand cc) {return this;}
        },

        COMBAT_BLOCKERS {
            @Override
            public Phase process(Hub hub, Parser.CompiledCommand cc) {return this;}
        },

        COMBAT_FIRST_STRIKE_DAMAGE {
            @Override
            public Phase process(Hub hub, Parser.CompiledCommand cc) {return this;}
        },
        COMBAT_DAMAGE {
            @Override
            public Phase process(Hub hub, Parser.CompiledCommand cc) {return this;}
        },
        COMBAT_END {
            @Override
            public Phase process(Hub hub, Parser.CompiledCommand cc) {return this;}
        },

        MAIN_2 {
            @Override
            public Phase process(Hub hub, Parser.CompiledCommand cc) {return this;}
        },

        END_STEP {
            @Override
            public Phase process(Hub hub, Parser.CompiledCommand cc) {return this;}
        },
        CLEANUP_STEP {
            @Override
            public Phase process(Hub hub, Parser.CompiledCommand cc) {return this;}
        };
    }
}
