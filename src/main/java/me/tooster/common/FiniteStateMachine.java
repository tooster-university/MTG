package me.tooster.common;

public abstract class FiniteStateMachine<C, I> {
    private State<C, I> currentState;
    private boolean autoNext = false;

    protected FiniteStateMachine(State<C, I> initialState) {currentState = initialState;}

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
            State<C, I> nextState = currentState.process(input, context);
            if (currentState != nextState) {
                currentState.onExit(nextState, context);
                State<C, I> previousState = currentState;
                currentState = nextState;
                nextState.onEnter(previousState, context);
            }
        } while (autoNext);
    }

    /**
     * @return returns current state
     */
    public State<C, I> getCurrentState() { return currentState; }

    /**
     * Enables auto-advance mode of FSM, so that it won't wait for input command (external call to <code>process()
     * </code>.
     */
    void enableAuto() {autoNext = true;}

    /**
     * Disables auto-advance mode of FSM, so next state update will be triggered only with some input command.
     */
    void disableAuto() {autoNext = false;}

    /**
     * Interface for States.
     * @param <C> context that State accepts
     * @param <I> Input that State accepts
     */
    public interface State<C, I> {
        /**
         * Processes the CompiledCommand inside State. Returns next state to go to, or this if state doesn't change
         *
         * @param input      CompiledCommand to process
         * @param context context for machine
         * @return next state or this if state stays the same
         */
        State<C, I> process(I input, C context);

        /**
         * Method invoked when new state is entered.
         * Always invoked after previous <code>onExit()</code>
         * It is not invoked in machine's initial state
         *
         * @param context context for machine
         */
        default void onEnter(State<C, I> prevState, C context) {}

        /**
         * Method invoked when exiting a state to enter other state.
         * Always invoked before process state's <code>onEnter()</code>
         *
         * @param context context for machine
         */
        default void onExit(State<C, I> nextState, C context) {}

    }

}
