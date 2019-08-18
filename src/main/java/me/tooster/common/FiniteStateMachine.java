package me.tooster.common;

import org.jetbrains.annotations.NotNull;

// TODO: change ContextT to Object varargs and InputT maybe also ???
// TODO: right now doin the hubFSM to manage ready/not ready players
/**
 * Abstract finite state machine that holds current state and has auto-go feature.
 *
 * @param <InputT>   Input type for machine states
 * @param <ContextT> Context type for machine states
 */
public abstract class FiniteStateMachine<InputT, ContextT> {
    private State<InputT, ContextT> initialState;
    private State<InputT, ContextT> currentState;
    /**
     * Enables auto-advance mode of FSM, so that it won't wait for input from user - external call to
     * <code>process()</code>. Instead it supplies itself with input and context objects passed to the function at
     * the beginning of auto phase. To alter parameters, simply modify the content of input or context
     */
    private boolean                 autoNext = false;

    public FiniteStateMachine(@NotNull State<InputT, ContextT> initialState) {
        this.initialState = initialState;
        reset();
    }

    /**
     * Advances the state of FSM. It will keep processing with Hub and CompiledCommand until <code>disableAuto()</code>
     * is called. To change the parameters passed to next state serverIn auto mode, use setters for Hub and CompiledCommand.
     *
     * @param input   input for the FSM
     * @param context represents the context serverIn which the state machine is updated. All data for state update should
     *                be included serverIn context. Used to contain data for FSM to use
     */
    // FIXME: Add lock() to lock the process() of FSM and executeIf(State, <lambda>) to execute actions atomically
    //  and lock if FSM is serverIn given state
    public synchronized void process(InputT input, ContextT context) {
        do {
            if(currentState == null) throw new RuntimeException("FSM halted.");
            State<InputT, ContextT> nextState = currentState.process(input, context);
            if (currentState != nextState) {
                currentState.onExit(nextState, context);
                State<InputT, ContextT> previousState = currentState;
                currentState = nextState;
                nextState.onEnter(previousState, context);
            }
        } while (autoNext);
    }

    public void process(InputT input) { process(input, null); }

    /**
     * Sets the auto mode. Without auto mode, the FSM loop is as follows:
     * <pre>
     *     1. wait for user to invoke `process(i, c)`
     *     2. |update FSM with i, c|
     *     3. if auto is enabled goto 2. otherwise goto 1.
     * </pre>
     *
     * @param enabled sets the auto mode
     */
    public void setAuto(boolean enabled) {autoNext = enabled;}

    /**
     * Resets the state machine to initial state and turn's off the auto mode.
     * At first triggers the onExit event of current state
     * Triggers the onEnter event of initial state with prevState and context set to null
     */
    public synchronized void reset() {
        autoNext = false;
        forceState(initialState, null);
    }

    /**
     * Forcibly changes the state of FSM.
     * Triggers exit/enter events with all null prev/next state which can be used
     * to determine initial entry/finishing exit transition of state machine.
     *
     * @param newState new state to set
     */
    public synchronized void forceState(State<InputT, ContextT> newState, ContextT context) {
        if(currentState != null) currentState.onExit(null, context);
        currentState = newState;
        if(currentState != null) currentState.onEnter(null, context);
    }

    /**
     * @return returns current state
     */
    public State<InputT, ContextT> getCurrentState() { return currentState; }

    /**
     * Interface for States.
     *
     * @param <InputT>   Input that State accepts
     * @param <ContextT> context that State accepts
     */
    public interface State<InputT, ContextT> {
        /**
         * Method invoked when new state is entered.
         * Happens after <code>onExit()</code> on previous state.
         * Machine current state is set to state that's being entered.
         * <code>prevState</code> is null if machine enters the initial state.
         *
         * @param context context for machine
         */
        default void onEnter(State<InputT, ContextT> prevState, ContextT context) {}

        /**
         * Processes the CompiledCommand inside State. Returns next state to go to, or this if state doesn't change
         *
         * @param input   CompiledCommand to process
         * @param context context for machine
         * @return next state or this if state stays the same
         */
        State<InputT, ContextT> process(InputT input, ContextT context);

        /**
         * Method invoked when exiting a state to enter other state.
         * Happens before <code>onEnter()</code> on next state.
         * Machine current state is set to state that's being exited.
         * <code>nextState</code> is null to distinguish machine finish.
         *
         * @param context context for machine
         */
        default void onExit(State<InputT, ContextT> nextState, ContextT context) {}
    }
}
