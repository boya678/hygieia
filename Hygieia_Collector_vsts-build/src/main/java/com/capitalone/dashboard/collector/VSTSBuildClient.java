package com.capitalone.dashboard.collector;

import java.util.List;

import com.capitalone.dashboard.model.Build;
import com.capitalone.dashboard.model.VSTSBuildJob;

/**
 * Client for fetching job and build information from Bamboo
 */
public interface VSTSBuildClient {

    /**
     * Finds all of the configured jobs for a given instance and returns the set of
     * builds for each job. At a minimum, the number and url of each Build will be
     * populated.
     *
     * @param instanceUrl the URL for the Bamboo instance
     * @return a summary of every build for each job on the instance
     */
    List<VSTSBuildJob> getInstanceJobs(String instanceUrl);

    /**
     * Fetch full populated build information for a build.
     *
     * @param buildUrl the url of the build
     * @param instanceUrl
     * @return a Build instance or null
     */
    List<Build> getBuildDetails(String instanceUrl, VSTSBuildJob job);

}
