package me.tooster.common;

/**
 * Abstract finite state machine that holds current state and has auto-go feature.
 *
 * @param <InputT> Input type for machine states
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

    public FiniteStateMachine(State<InputT, ContextT> initialState) { this.initialState = currentState = initialState; }

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
     * Resets the state machine to initial state and turn's off the auto mode;
     */
    public synchronized void reset() {
        currentState = initialState;
        autoNext = false;
    }

    /**
     * Forcibly changes the state of FSM.
     *
     * @param newState new state to set
     */
    public synchronized void forceState(State<InputT, ContextT> newState) { currentState = newState; }

    /**
     * @return returns current state
     */
    public State<InputT, ContextT> getCurrentState() { return currentState; }

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
         * It is not invoked serverIn machine's initial state
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
