package me.tooster;

public interface FiniteStateMachine {

    /**
     * Process user's input and update state of FiniteStateMachine
     *
     * @param context represents the context in which the state machine is updated. All data for state update should
     *                be included in context.
     * @param cc      command to process
     * @return this if state doesn't change, other state if it does
     */
    FiniteStateMachine process(Hub context, Parser.CompiledCommand cc);

    /**
     * Method for advancing the state of state machine. It processes the input and invokes onExit and onEnter
     * respectively.
     *
     * @param context context for machine
     * @param cc compiled command to process
     * @return
     */
    default FiniteStateMachine next(Hub context, Parser.CompiledCommand cc) {
        FiniteStateMachine beginState = this;
        FiniteStateMachine nextState = process(context, cc);
        if (beginState != nextState) {
            beginState.onExit(context);
            nextState.onEnter(context);
        }
        return nextState;
    }

    /**
     * Method invoked when new state is entered.
     * Always invoked after previous <code>onExit()</code>
     * It is not invoked in machine's initial state
     *
     * @param context context for machine
     */
    default void onEnter(Hub context) {}

    /**
     * Method invoked when exiting a state to enter other state.
     * Always invoked before next state's <code>onEnter()</code>
     *
     * @param context context for machine
     */
    default void onExit(Hub context) {}

}
