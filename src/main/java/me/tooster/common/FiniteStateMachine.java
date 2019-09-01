package me.tooster.common;

import org.jetbrains.annotations.NotNull;

import java.util.function.BiConsumer;

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
    private StateT initialState;
    private StateT currentState;

    /**
     * Sets the auto model. The FSM workflow looks like that:
     * <pre>
     *     1. |update FSM with `this` and input `i`|
     *     2. if auto is enabled goto 1.
     * </pre>
     * in auto model input and fsm is passed as is to the `process` function
     *
     * @param enabled sets the auto model
     */
    public boolean autoProcess = false;

    /**
     * Private constructor for factory
     *
     * @param initialState initial state of the machine
     */
    public FiniteStateMachine(@NotNull StateT initialState) {
        this.initialState = initialState;
    }

    /**
     * Wrapper function for state's process() function. Go see the overloading state machine to check the actual process() functions.
     * Advances the state of FSM. It will keep processing with the same input until <code>disableAuto()</code> is called.
     * Shared state can be passed in finite state machine or input objects.
     *
     * @param input input for the FSM
     * @return returns the finite state machine object to enable linking the process(), lock() and unlock() calls
     */
    @SuppressWarnings("unchecked")
    public synchronized FiniteStateMachine<StateT, FsmT, InputT> process(InputT... input) {
        do {
            if (currentState == null) throw new RuntimeException("FSM halted. ");
            prepcocess(input);
            StateT previousState = currentState;
            try {
                StateT nextState = currentState.process((FsmT) this, input);
                if (currentState != nextState) {
                    currentState.onExit((FsmT) this, nextState);
                    currentState = nextState;
                    nextState.onEnter((FsmT) this, previousState);
                }
            } catch (AbortTransition abort) {
                abort.cleanupHandler.run();
                currentState = previousState;
            }
            postprocess(input);
        } while (autoProcess);
        return this;
    }

    /**
     * Starts the state machine from initial state triggering onEnter with prevState=null on initial state
     */
    public synchronized void start() {
        currentState = null;
        forceState(initialState);
    }

    /**
     * Stops the state machine from current state triggering onExit with nextState=null.
     */
    public void stop() {
        forceState(null);
    }

    /**
     * @return returns current state; null if machine is halted
     */
    public StateT getCurrentState() { return currentState; }

    /**
     * Forcibly changes the state of FSM.
     * Triggers exit/enter events of states, with null previous/next states which can be used
     * to determine initial entry/finishing exit transition of state machine.
     * AbortTransition exceptions are ignored and don't affect the forceState.
     *
     * @param newState new state to set
     */
    @SuppressWarnings("unchecked")
    public synchronized FiniteStateMachine<StateT, FsmT, InputT> forceState(StateT newState) {
        try { if (currentState != null) currentState.onExit((FsmT) this, null); } catch (AbortTransition ignored) {}
        currentState = newState;
        try { if (currentState != null) currentState.onEnter((FsmT) this, null); } catch (AbortTransition ignored) {}
        return this;
    }

    /**
     * Event run before every `process()` and `onEnter/onExit` events.
     * Useful to implement filters of some kind on groups of states without a need to write sub-state machines.
     *
     * @param input input that will be supplied to a state's `process()` method
     */
    public void prepcocess(InputT... input) {}

    /**
     * Event run after every `process()` and `onEnter/onExit` events.
     * Useful to implement filters of some kind on groups of states without a need to write sub-state machines.
     *
     * @param input input that will be supplied to a state's `process()` method
     */
    public void postprocess(InputT... input) {}

    /**
     * This throwable is a lightweight object that can be thrown at any point during transition events in FSM to break out of transi
     */
    public static class AbortTransition extends Throwable {
        public final Runnable cleanupHandler;

        /**
         * Creates new handler for cleaning up after throwing an AbortTransition exception
         *
         * @param cleanupHandler
         */
        public AbortTransition(Runnable cleanupHandler) {
            super(null, null, true, false);
            this.cleanupHandler = cleanupHandler == null ? () -> {} : cleanupHandler;
        }

        /**
         * Creates new transition abort object without cleanup handler.
         * See also {@link me.tooster.common.FiniteStateMachine.AbortTransition#AbortTransition(Runnable)}.
         */
        public AbortTransition() {this(null);}
    }

    //--------------------------------------------------------------------------------------------------------------------------------------

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
        default void onEnter(FsmT fsm, StateT prevState) throws AbortTransition {}

        /**
         * Method invoked when exiting a state to enter other state.
         * Happens before <code>onEnter()</code> on next state.
         * Machine current state is set to state that's being exited.
         * <code>nextState</code> is null to distinguish machine finish.
         *
         * @param fsm       containing machine
         * @param nextState next state to transition into
         */
        default void onExit(FsmT fsm, StateT nextState) throws AbortTransition {}
    }
}
