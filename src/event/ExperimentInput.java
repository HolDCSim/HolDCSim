
package event;

import infrastructure.DataCenter;

import java.io.Serializable;

/**
 * ExperimentInput contains datacenter used in the experiment (and all the
 * components in the datacenter e.g., servers).
 *
 * @author David Meisner (meisner@umich.edu)
 */
public final class ExperimentInput implements Serializable {

    /**
     * The serialization id.
     */
    private static final long serialVersionUID = 1L;
    /**
     * The datacenter modeled in the simulation.
     */
    private DataCenter datacenter;

    /**
     * Creates a new ExperimentInput.
     */
    public ExperimentInput() {
    }

    /**
     * Sets the datacenter for the input.
     * @param dataCenter the datacenter
     */
    public void setDataCenter(final DataCenter dataCenter) {
        this.datacenter = dataCenter;
    }

    /**
     * Gets the input datacenter. Can be null if not set.
     * @return the datacenter
     */
    public DataCenter getDataCenter() {
        return this.datacenter;
    }

}
