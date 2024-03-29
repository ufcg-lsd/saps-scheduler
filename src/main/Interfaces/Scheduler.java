package saps.scheduler.interfaces;

import java.util.List;

import saps.common.core.model.SapsImage;

public interface Scheduler {

    /** This function schedules up to tasks. */
    public void schedule();

    /**
    * This function retrieves consistency between the information present in Catalog and Arrebol, and
    * starts the list of submitted jobs.
    */
    public void recovery();

    /**
    * This function checks if each submitted job was finished. If exists finished jobs, for each job
    * is updates state in Catalog and removes a job by list of submitted jobs to Arrebol.
    */
    public void checker();

    /*
     * These methods above shouldn't be public. 
     * We'll change they visibility as soon as we finish testing
     */
    public List<SapsImage> selectTasks();

}