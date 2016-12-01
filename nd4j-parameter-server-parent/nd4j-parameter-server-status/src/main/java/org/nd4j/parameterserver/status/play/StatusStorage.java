package org.nd4j.parameterserver.status.play;

import org.nd4j.parameterserver.model.SubscriberState;

/**
 * An interface for storing information
 * about the status of a {@link org.nd4j.parameterserver.ParameterServerSubscriber}
 *
 * @author Adam Gibson
 */
public interface StatusStorage {
    /**
     * Get the state given an id.
     * The integer represents a stream id
     * for a given {@link org.nd4j.parameterserver.ParameterServerSubscriber}.
     *
     * A {@link SubscriberState} is supposed to be 1 to 1 mapping
     * for a stream and a {@link io.aeron.driver.MediaDriver}.
     * @param id the id of the state to get
     * @return the subscriber state for the given id or none
     * if it doesn't exist
     */
    SubscriberState getState(int id);

    /**
     * Update the state for storage
     * @param subscriberState the subscriber state to update
     */
    void updateState(SubscriberState subscriberState);
}
