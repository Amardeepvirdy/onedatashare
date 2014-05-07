package stork.feather;

/**
 * An abstract base class for anything which can serve as an element in a proxy
 * transfer pipeline. In particular, this is the base class for {@link Sink}
 * and {@link Tap}.
 * <p/>
 * This class implements a state machine which enforces call guards on
 * operations that control the flow of data in a proxy transfer according to
 * the state transition table below.
 * <pre>
 *  __________________________________________
 * |          ||          |         |         |
 * |       To || running  | paused  | stopped |
 * |  From    ||          |         |         |
 * |==========++==========+=========+=========|
 * | !started || start()  |         | stop()  |
 * |----------++----------+---------+---------|
 * |  running ||          | pause() | stop()  |
 * |----------++----------+---------+---------|
 * |  paused  || resume() |         | stop()  |
 * |__________||__________|_________|_________|
 * </pre>
 *
 * @param <R> The {@code Resource} type of this {@code PipeElement}'s {@code
 * root}.
 */
public abstract class PipeElement<R extends Resource> {
  private boolean started = false, paused = false, stopped = false;

  /** The root {@code Resource} of this {@code PipeElement}. */
  public final R root;

  public PipeElement(Resource root) { this.root = root; }

  /**
   * This is called once the pipeline has been assembled to start the flow of
   * data through this {@code PipeElement}. This method may return a {@code
   * Bell} which will ring when this {@code PipeElement} is ready, or {@code
   * null} to indicate that it is ready immediately.
   * <p/>
   * Exceptions thrown here or through the returned {@code Bell} will be
   * propagated through the pipeline, and this 
   * <p/>
   * No other transfer methods will be called until this method has completed
   * and the returned {@code Bell}, if non-{@code null}, has rung. This method
   * will be called at most once.
   *
   * @return A {@code Bell} which will ring when this {@code PipeElement} is
   * ready.
   */
  protected Bell<?> start() throws Exception { return null; }

  /**
   * This is called once the . Once stopped, the transfer cannot be
   * started again. This may be called before {@link #start()} if the transfer
   * is canceled before it starts.
   */
  protected void stop() { }

  /**
   * Pause the transfer temporarily. {@code resume()} should be called to
   * resume transfer after pausing. Implementors should assume this method will
   * only be called from a running state.
   */
  protected abstract void pause();

  /**
   * Resume the transfer after a pause. Implementors should assume this method
   * will only be called from a paused state.
   */
  protected abstract void resume();

  /**
   * Check if the transfer started; that is, if {@link #start()} has been
   * called. Note that this will still return {@code true} if the transfer has
   * been stopped, and will return {@code false} if the transfer was stopped
   * without {@code start()} having been called.
   *
   * @return {@code true} if the transfer started.
   */
  public final boolean started() { return started; }

  /**
   * Check if the transfer is running; that is, it has started and is neither
   * stopped nor paused.
   *
   * @return {@code true} if the transfer is running.
   */
  public final boolean running() { return started && !paused && !stopped; }

  /**
   * Check if the transfer has stopped. That is, check if {@link #stop()} has
   * been called.
   *
   * @return {@code true} if the transfer has stopped.
   */
  public final boolean stopped() { return stopped; }

  /**
   * Check if the transfer is paused, but not stopped. Another way of looking
   * at it is to check if {@code #resume()} will affect the state of the
   * transfer.
   *
   * @return {@code true} if the transfer is paused.
   */
  public final boolean paused() { return paused && !stopped; }

  /**
   * Initialize the transfer of data for the resource specified by {@code path}
   * relative to the root {@code Resource}. This simply delegates to {@link
   * #initialize(Relative)}.
   *
   * @param path the relative path to the resource which should be initialized.
   * @return A {@code Bell} which will ring when data for {@code path} is ready
   * to be drained, or {@code null} if data can begin being drained
   * immediately.
   */
  protected final Bell<?> initialize(Path path) {
    return initialize(root.selectRelative(path));
  }

  /**
   * Prepare the pipeline for the transfer of data for {@code resource}. This
   * must be called before any data is drained for {@code resource}.
   * <p/>
   * This method returns immediately, and initialization takes place
   * asynchronously. It may return a {@code Bell} which will be rung when the
   * initialization process is complete and slices for {@code path} may begun
   * being drained through this endpoint. It may also return {@code null} to
   * indicate that transmission may begin immediately.
   *
   * @param resource the resource which should be initialized.
   * @return A {@code Bell} which will ring when data for {@code resource} is ready
   * to be drained, or {@code null} if data can begin being drained
   * immediately.
   * @throws Exception (via bell) if the resource cannot be initialized.
   */
  protected abstract Bell<?> initialize(Relative<Resource> resource);

  /**
   * Drain a {@link Slice} through the pipeline for the resource with the given
   * {@code Path}. This delegates to {@link #drain(RelativeSlice)}.
   *
   * @param path the path corresponding to the resource the slice originated
   * from.
   * @param slice a {@code Slice} being drained through the pipeline.
   * @throws IllegalStateException if this method is called when the pipeline
   * has not been initialized.
   */
  protected final void drain(Path path, Slice slice) {
    drain(new RelativeResource(root, path), slice);
  }

  /**
   * Drain a {@link Slice} through the pipeline for the given {@code
   * RelativeResource}. This delegates to {@link #drain(RelativeSlice)}.
   *
   * @param resource the {@code RelativeResource} the slice originated from.
   * @param slice a {@code Slice} being drained through the pipeline.
   * @throws IllegalStateException if this method is called when the pipeline
   * has not been initialized.
   */
  protected final void drain(RelativeResource resouce, Slice slice) {
    drain(new RelativeSlice(resource, slice));
  }

  /**
   * Drain a {@link RelativeSlice} through the pipeline. This method returns as
   * soon as possible, with the actual I/O operation taking place
   * asynchronously.
   *
   * @param slice a {@code Slice} being drained through the pipeline.
   * @throws IllegalStateException if this method is called when the pipeline
   * has not been initialized.
   */
  protected abstract void drain(RelativeSlice slice);

  /**
   * Handle a {@code RelativeException} 
   */

  /**
   * Finalize the transfer of data for the specified resource. This method
   * should return immediately, and finalization should take place
   * asynchronously.
   *
   * @param resource the resource being finalized.
   * @throws IllegalStateException if this method is called when the pipeline
   * has not been initialized.
   */
  protected abstract void finalize(RelativeResource resource);

  /**
   * Check if the pipeline is capable of draining slices in arbitrary order.
   * The return value of this method should remain constant across calls.
   *
   * @return {@code true} if transmitting slices in arbitrary order is
   * supported.
   * @throws IllegalStateException if this method is called when the pipeline
   * has not been initialized.
   */
  protected abstract boolean random();

  /**
   * Get the number of distinct resources the pipeline may be in the process of
   * transferring simultaneously. Specifically, this value limits how many
   * times {@link #initialize(RelativeResource)} may be called before a
   * corresponding {@link #finalize(RelativeResource)} must be called to free
   * up a transfer slot.
   * <p/>
   * Returning a number less than or equal to zero indicates that an arbitrary
   * number of resources may be transferred concurrently.
   *
   * @return The number of data resources this sink can receive concurrently.
   * @throws IllegalStateException if this method is called when the pipeline
   * has not been initialized.
   */
  protected abstract int concurrency();

  /**
   * Determine whether this {@code PipeElement} is active. Every proxy
   * pipeline must have an active element. This will be used to determine
   * whether or not a {@code Pump} is required to extract data from the
   * pipeline {@code Tap}.
   *
   * @return {@code true} if this {@code PipeElement} is active; {@code false}
   * otherwise.
   */
  protected abstract boolean isActive();
}
