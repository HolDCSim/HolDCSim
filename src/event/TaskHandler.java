

package event;

import job.Task;


public interface TaskHandler {

    /**
     * Inserts a Task into the handler.
     *
     * @param time - the time the Task is inserted
     * @param Task - the Task to insert
     */
    void insertTask(double time, Task Task);

    /**
     * Removes a time from the handler.
     *
     * @param time - the time the Task is removed
     * @param Task - the Task to remove
     */
    void removeTask(final double time, final Task Task);

}
