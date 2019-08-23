package me.tooster.common;

import org.jetbrains.annotations.NotNull;

// TODO: right now doin the hubFSM to manage ready/not ready players

/**
 * Abstract class of finite state machine that holds current state and has auto-go feature and enter/exit state events
 *
 * @param <InputT> Input type for machine states
 * @param <FsmT>   Type of generic state machine
 * @param <StateT> state type subclass
 */
public abstract class FiniteStateMachine<StateT extends FiniteStateMachine.State<StateT, FsmT, InputT>,
        FsmT extends FiniteStateMachine, InputT> {
    private StateT  initialState;
    private StateT  currentState;

    /**
     * Enables auto-advance mode of FSM, so that it won't wait for input from user - external call to
     * <code>process()</code>. Instead it supplies itself with input and context objects passed to the function at
     * the beginning of auto phase. To alter parameters, simply modify the content of input or context
     */
    private boolean autoNext = false;

    /**
     * Private constructor for factory
     * @param initialState initial state of the machine
     */
    public FiniteStateMachine(@NotNull StateT initialState) {
        this.initialState = initialState;
    }

    /**
     * Starts the state machine from initial state triggering onEnter with prevState=null on initial state
     */
    public void start(){
        currentState = null;
        forceState(initialState);
    }

    /**
     * Stops the state machine from current state triggering onExit with nextState=null.
     */
    public void stop(){
        forceState(null);
    }

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
     * Wrapper function for state's process() function. Check overloading state machine to check the actual process() functions.
     * Advances the state of FSM. It will keep processing with the same input until <code>disableAuto()</code>
     * is called. Shared state can be passed in finite state machine or input objects.
     *
     * @param input input for the FSM
     */
    // FIXME: Add lock() to lock the process() of FSM and executeIf(State, <lambda>) to execute actions atomically
    //  and lock if FSM is serverIn given state
    @SuppressWarnings("unchecked")
    public synchronized void process(InputT... input) {
        do {
            if (currentState == null) throw new RuntimeException("FSM halted. ");
            StateT nextState = currentState.process((FsmT) this, input);
            if (currentState != nextState) {
                currentState.onExit((FsmT) this, nextState);
                StateT previousState = currentState;
                currentState = nextState;
                nextState.onEnter((FsmT) this, previousState);
            }
        } while (autoNext);
    }

    /**
     * @return returns current state; null if machine is halted
     */
    public StateT getCurrentState() { return currentState; }

    /**
     * Forcibly changes the state of FSM.
     * Triggers exit/enter events with all null prev/next state which can be used
     * to determine initial entry/finishing exit transition of state machine.
     *
     * @param newState new state to set
     */
    @SuppressWarnings("unchecked")
    public synchronized void forceState(StateT newState) {
        if (currentState != null) currentState.onExit((FsmT) this, null);
        currentState = newState;
        if (currentState != null) currentState.onEnter((FsmT) this, null);
    }

    /**
     * Interface for States.
     *
     * @param <InputT> Input that State accepts
     * @param <FsmT>   type of containing machine
     * @param <StateT> state type subclass
     */
    public interface State<StateT extends State<StateT, FsmT, InputT>, FsmT extends FiniteStateMachine, InputT> {
        /**
         * Processes the CompiledCommand inside State. Returns next state to go to, or this if state doesn't change
         *
         * @param input CompiledCommand to process
         * @param fsm   containing machine
         * @return next state or this if state stays the same
         */
        StateT process(FsmT fsm, InputT... input);

        /**
         * Method invoked when new state is entered.
         * Happens after <code>onExit()</code> on previous state.
         * Machine current state is set to state that's being entered.
         * <code>prevState</code> is null if machine enters the initial state.
         *
         * @param fsm       containing machine
         * @param prevState previous state before transition
         */
        default void onEnter(FsmT fsm, StateT prevState) {}

        /**
         * Method invoked when exiting a state to enter other state.
         * Happens before <code>onEnter()</code> on next state.
         * Machine current state is set to state that's being exited.
         * <code>nextState</code> is null to distinguish machine finish.
         *
         * @param fsm       containing machine
         * @param nextState next state to transition into
         */
        default void onExit(FsmT fsm, StateT nextState) {}
    }
}
