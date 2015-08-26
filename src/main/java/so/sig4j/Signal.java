package so.sig4j;

import java.util.ArrayDeque;
import java.util.LinkedHashSet;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The base class of all signals.
 *
 * Beware that connecting slots to a signal 's' while emitting 's' is not
 * thread safe. On the other hand emitting 's' concurrently is thread safe.
 */
public abstract class Signal {
    // The thread pool used for {@link ConnectionType#QUEUED} connected slots.
    private static final ExecutorService THREAD_POOL =
            Executors.newSingleThreadExecutor(runnable -> {
                final Thread t = new Thread(runnable);
                // Non-daemon threads prevent program termination
                t.setDaemon(true);
                return t;
            });

    // The queue of {@link ConnectionType#DIRECT} connected slots.
    private Queue<Slot> direct = new ArrayDeque<>();

    // The queue of {@link ConnectionType#QUEUED} connected slots.
    private Queue<Slot> queued = new ArrayDeque<>();

    // The queue of dispatched slots {@see SlotDispatcher}.
    // (Use {@link LinkedHashSet} to keep iteration order)
    private Set<DispatcherAssociation> dispatched = new LinkedHashSet<>();

    /**
     * Connects the given slot using {@link ConnectionType#DIRECT}. This
     * function is equivalent to {@code connect(slot, ConnectionType#DIRECT)}.
     *
     * @param slot The slot to connect.
     * @throws IllegalArgumentException If {@code slot} is null.
     */
    protected void connect(final Slot slot) {
        if (slot == null) {
            throw new IllegalArgumentException("slot is null");
        }
        direct.add(slot);
    }

    /**
     * Connects the given slot according to {@link ConnectionType}.
     *
     * @param slot The slot to connect.
     * @param type The {@link ConnectionType} to use.
     * @throws IllegalArgumentException If {@code slot} or {@code type} is null.
     */
    protected void connect(final Slot slot, final ConnectionType type) {
        if (slot == null) {
            throw new IllegalArgumentException("slot is null");
        } else if (type == null) {
            throw new IllegalArgumentException("connection type is null");
        } else if (type == ConnectionType.DIRECT) {
            connect(slot);
        } else {
            queued.add(slot);
        }
    }

    /**
     * Connects the given slot and actuates it within the thread context
     * of the given {@link SlotDispatcher} if this signal is emitted.
     *
     * @param dispatcher The {@link SlotDispatcher} to use.
     * @param slot       The slot to connect.
     * @throws IllegalArgumentException If {@code dispatcher} or
     *                      {@code slot} is null.
     */
    protected void connect(final SlotDispatcher dispatcher, final Slot slot) {
        if (dispatcher == null) {
            throw new IllegalArgumentException("dispatcher is null");
        } else if (slot == null) {
            throw new IllegalArgumentException("slot is null");
        }
        dispatched.add(new DispatcherAssociation(dispatcher, slot));
    }

    /**
     * Emits this signal with the given arguments.
     *
     * @param args The arguments to use while emitting this signal.
     */
    protected void emit(final Object... args) {
        direct.forEach(slot -> actuate(slot, args));
        queued.forEach(slot -> THREAD_POOL.execute(
                () -> actuate(slot, args)));
        dispatched.forEach(da -> {
            final SlotActuation sa = new SlotActuation(da.slot, args);
            da.slotDispatcher.actuate(sa);
        });
    }

    /**
     * A callback function used to actuate a single slot.
     *
     * The implementer of this function does not need to create any threads
     * but cast down the given slot and actuate it with the given arguments.
     *
     * This function should not have any side effects to this class.
     *
     * @param slot The slot to actuate.
     * @param args The arguments of the actuated slot.
     */
    protected abstract void actuate(final Slot slot, final Object[] args);



    /**
     * Associates a connected slot with its dispatcher.
     */
    private static class DispatcherAssociation {
        private final SlotDispatcher slotDispatcher;
        private final Slot slot;

        private DispatcherAssociation(final SlotDispatcher sd, final Slot s) {
            slotDispatcher = sd;
            slot = s;
        }
    }

    /**
     * Represents an actual slot actuation. Use {@link #actuate()} to actuate
     * the slot with its arguments.
     */
    class SlotActuation {
        private final Slot slot;
        private final Object[] arguments;

        private SlotActuation(final Slot pSlot, final Object[] args) {
            slot = pSlot;
            arguments = args;
        }

        public void actuate() {
            Signal.this.actuate(slot, arguments);
        }
    }
}