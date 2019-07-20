package me.tooster.common;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Abstract finite state machine that holds current state and has auto-go feature.
 *
 * @param <I> Input type for machine states
 * @param <C> Context type for machine states
 */
public abstract class FiniteStateMachine<I, C> {
    private   State<I, C> currentState;
    /**
     * Enables auto-advance mode of FSM, so that it won't wait for input from user - external call to
     * <code>process()</code>. Instead it supplies itself with input and context objects passed to the function at
     * the beginning of auto phase. To alter parameters, simply modify the content of input or context
     */
    protected boolean     autoNext = false;

    public FiniteStateMachine(State<I, C> initialState) { currentState = initialState; }

    /**
     * Advances the state of FSM. It will keep processing with Hub and CompiledCommand until <code>disableAuto()</code>
     * is called. To change the parameters passed to next state in auto mode, use setters for Hub and CompiledCommand.
     *
     * @param input   input for the FSM
     * @param context represents the context in which the state machine is updated. All data for state update should
     *                be included in context. Used to contain data for FSM to use
     */
    // FIXME: Add lock() to lock the process() of FSM and executeIf(State, <lambda>) to execute actions atomically
    //  and lock if FSM is in given state
    public synchronized void process(I input, C context) {
        do {
            State<I, C> nextState = currentState.process(input, context);
            if (currentState != nextState) {
                currentState.onExit(nextState, context);
                State<I, C> previousState = currentState;
                currentState = nextState;
                nextState.onEnter(previousState, context);
            }
        } while (autoNext);
    }

    public void process(I input) { process(input, null); }

    public void restart() {}

    /**
     * @return returns current state
     */
    public State<I, C> getCurrentState() { return currentState; }

    /**
     * Interface for States.
     *
     * @param <I> Input that State accepts
     * @param <C> context that State accepts
     */
    public interface State<I, C> {
        /**
         * Processes the CompiledCommand inside State. Returns next state to go to, or this if state doesn't change
         *
         * @param input   CompiledCommand to process
         * @param context context for machine
         * @return next state or this if state stays the same
         */
        State<I, C> process(I input, C context);

        /**
         * Method invoked when new state is entered.
         * Always invoked after previous <code>onExit()</code>
         * It is not invoked in machine's initial state
         *
         * @param context context for machine
         */
        default void onEnter(State<I, C> prevState, C context) {}

        /**
         * Method invoked when exiting a state to enter other state.
         * Always invoked before process state's <code>onEnter()</code>
         *
         * @param context context for machine
         */
        default void onExit(State<I, C> nextState, C context) {}

    }

}
