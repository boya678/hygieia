package com.capitalone.dashboard.collector;


import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.capitalone.dashboard.model.Build;
import com.capitalone.dashboard.model.Collector;
import com.capitalone.dashboard.model.CollectorItem;
import com.capitalone.dashboard.model.CollectorType;
import com.capitalone.dashboard.model.VSTSBuildCollector;
import com.capitalone.dashboard.model.VSTSBuildJob;
import com.capitalone.dashboard.repository.BaseCollectorRepository;
import com.capitalone.dashboard.repository.BuildRepository;
import com.capitalone.dashboard.repository.ComponentRepository;
import com.capitalone.dashboard.repository.VSTSBuildCollectorRepository;
import com.capitalone.dashboard.repository.VSTSBuildJobRepository;

/**
 * CollectorTask that fetches Build information from Bamboo
 */
@Component
public class VSTSBuildCollectorTask extends CollectorTask<VSTSBuildCollector> {
  
    private static final Logger LOG = LoggerFactory.getLogger(VSTSBuildCollectorTask.class);

    private final VSTSBuildCollectorRepository vSTSBuildCollectorRepository;
    private final VSTSBuildJobRepository vSTSBuildJobRepository;
    private final BuildRepository buildRepository;
    private final VSTSBuildClient vSTSBuildClient;
    private final VSTSBuildSettings vSTSBuildSettings;
    private final ComponentRepository dbComponentRepository;

    @Autowired
    public VSTSBuildCollectorTask(TaskScheduler taskScheduler,
                               VSTSBuildCollectorRepository vSTSBuildCollectorRepository,
                               VSTSBuildJobRepository vSTSBuildJobRepository,
                               BuildRepository buildRepository, VSTSBuildClient vSTSBuildClient,
                               VSTSBuildSettings vSTSBuildSettings,
                               ComponentRepository dbComponentRepository) {
        super(taskScheduler, "VSTSBuild");
        this.vSTSBuildCollectorRepository = vSTSBuildCollectorRepository;
        this.vSTSBuildJobRepository = vSTSBuildJobRepository;
        this.buildRepository = buildRepository;
        this.vSTSBuildClient = vSTSBuildClient;
        this.vSTSBuildSettings = vSTSBuildSettings;
        this.dbComponentRepository = dbComponentRepository;
    }

    @Override
    public VSTSBuildCollector getCollector() {
        return VSTSBuildCollector.prototype(vSTSBuildSettings.getServers(), vSTSBuildSettings.getNiceNames());
    }

    @Override
    public BaseCollectorRepository<VSTSBuildCollector> getCollectorRepository() {
        return vSTSBuildCollectorRepository;
    }

    @Override
    public String getCron() {
        return vSTSBuildSettings.getCron();
    }

    @Override
    public void collect(VSTSBuildCollector collector) {
    	LOG.info("Starting Collector: 07/05/2019" );
        long start = System.currentTimeMillis();
        Set<ObjectId> udId = new HashSet<>();
        udId.add(collector.getId());
        List<VSTSBuildJob> existingJobs = vSTSBuildJobRepository.findByCollectorIdIn(udId);
        List<VSTSBuildJob> activeJobs = new ArrayList<>();
        List<String> activeServers = new ArrayList<>();
        activeServers.addAll(collector.getBuildServers());

        cleanDuplicates(existingJobs);
        clean(collector, existingJobs);

        for (String instanceUrl : collector.getBuildServers()) {
            logBanner(instanceUrl);
            try {
                List<VSTSBuildJob> buildsByInstanceUrl = vSTSBuildClient.getInstanceJobs(instanceUrl);
                log("Fetched jobs", start);
                activeJobs.addAll(buildsByInstanceUrl);
                addNewJobs(buildsByInstanceUrl, existingJobs, collector);
                addNewBuilds(enabledJobs(collector, instanceUrl), instanceUrl);
                log("Finished", start);
            } catch (Exception rce) {
                activeServers.remove(instanceUrl); // since it was a rest exception, we will not delete this job  and wait for
                // rest exceptions to clear up at a later run.
                log("Error getting jobs for: " + instanceUrl, start);
            }
        }
        // Delete jobs that will be no longer collected because servers have moved etc.
        deleteUnwantedJobs(activeJobs, existingJobs, activeServers, collector);
    }

    private void cleanDuplicates(List<VSTSBuildJob> existingJobs) {
    	
    	List<VSTSBuildJob> jobsList = new ArrayList<>();
    	List<VSTSBuildJob> duplicatesJobs = new ArrayList<>();
    	
		for (VSTSBuildJob vstsBuildJob : existingJobs) {
			if (!jobsList.contains(vstsBuildJob)) {
				jobsList.add(vstsBuildJob);
			}else {
				VSTSBuildJob addedJob = jobsList.get(jobsList.indexOf(vstsBuildJob));
				if (!addedJob.isEnabled() && vstsBuildJob.isEnabled()) {
					jobsList.remove(addedJob);
					jobsList.add(vstsBuildJob);
					vstsBuildJob = addedJob;
				}
				
				duplicatesJobs.add(vstsBuildJob);
			}
		}
		
		if (!duplicatesJobs.isEmpty()) {
			vSTSBuildJobRepository.delete(duplicatesJobs);			
		}
	}

	/**
     * Clean up unused bamboo/jenkins collector items
     *
     * @param collector    the {@link VSTSBuildCollector}
     * @param existingJobs
     */
    private void clean(Collector collector, List<VSTSBuildJob> existingJobs) {
		Set<ObjectId> uniqueIDs = searchIdCollector(collector);
		saveAndSwitchJobs(collector, existingJobs, uniqueIDs);
	}
    
    private Set<ObjectId> searchIdCollector(Collector collector) {
		Set<ObjectId> uniqueIDs = new HashSet<ObjectId>();
		for (com.capitalone.dashboard.model.Component comp : dbComponentRepository.findAll()) {
			if (comp.getCollectorItems() != null && !comp.getCollectorItems().isEmpty()) {
				List<CollectorItem> itemList = comp.getCollectorItems().get(CollectorType.Build);
				uniqueIDs.addAll(addCollectorIdsActive(collector, itemList));
			}
		}
		return uniqueIDs;
	}

	private Set<ObjectId> addCollectorIdsActive(Collector collector, List<CollectorItem> itemList) {
		Set<ObjectId> uniqueIDs = new HashSet<ObjectId>();
		if (itemList != null) {
			for (CollectorItem ci : itemList) {
				if (ci != null && ci.getCollectorId().equals(collector.getId())) {
					uniqueIDs.add(ci.getId());
				}
			}
		}
		return uniqueIDs;
	}

	private void saveAndSwitchJobs(Collector collector, List<VSTSBuildJob> existingRepos, Set<ObjectId> uniqueIDs) {
		List<VSTSBuildJob> stateChangeJobList = new ArrayList<>();
		Set<ObjectId> idSet = new HashSet<>();
		idSet.add(collector.getId());
		for (VSTSBuildJob job : existingRepos) {
			if ((job.isEnabled() && !uniqueIDs.contains(job.getId()))
					|| (!job.isEnabled() && uniqueIDs.contains(job.getId()))) {
				
				boolean flag = uniqueIDs.contains(job.getId());
                job.setEnabled(flag);
				stateChangeJobList.add(job);
			}
		}
		if (!CollectionUtils.isEmpty(stateChangeJobList)) {
			vSTSBuildJobRepository.save(stateChangeJobList);
		}
	}

    /**
     * Delete orphaned job collector items
     *
     * @param activeJobs
     * @param existingJobs
     * @param activeServers
     * @param collector
     */
    private void deleteUnwantedJobs(List<VSTSBuildJob> activeJobs, List<VSTSBuildJob> existingJobs, List<String> activeServers, VSTSBuildCollector collector) {

        List<VSTSBuildJob> deleteJobList = new ArrayList<>();
        for (VSTSBuildJob job : existingJobs) {
            if (job.isPushed()) continue; // build servers that push jobs will not be in active servers list by design

            // if we have a collector item for the job in repository but it's build server is not what we collect, remove it.
            if (!collector.getBuildServers().contains(job.getInstanceUrl())) {
                deleteJobList.add(job);
            }

            //if the collector id of the collector item for the job in the repo does not match with the collector ID, delete it.
            if (!job.getCollectorId().equals(collector.getId())) {
                deleteJobList.add(job);
            }

            // this is to handle jobs that have been deleted from build servers. Will get 404 if we don't delete them.
            if (activeServers.contains(job.getInstanceUrl()) && !activeJobs.contains(job)) {
                deleteJobList.add(job);
            }

        }
        if (!CollectionUtils.isEmpty(deleteJobList)) {
            vSTSBuildJobRepository.delete(deleteJobList);
        }
    }

    /**
     * Iterates over the enabled build jobs and adds new builds to the database.
     *
     * @param enabledJobs list of enabled {@link VSTSBuildJob}s
     * @param buildsByJob maps a {@link VSTSBuildJob} to a set of {@link Build}s.
     */
    private void addNewBuilds(List<VSTSBuildJob> enabledJobs, String instanceUrl) {
    	
        long start = System.currentTimeMillis();
        log("Enabled Jobs", start, enabledJobs.size());
        List<Build> newBuildsList = new ArrayList<>();
        for (VSTSBuildJob job : enabledJobs) {
            if (job.isPushed()) {
                LOG.info("Job Pushed already: " + job.getJobName());
                continue;
            }
            
            newBuildsList.addAll(vSTSBuildClient.getBuildDetails(instanceUrl, job));
        }
        
        newBuildsList.removeAll(Collections.singleton(null));
        
        if (!newBuildsList.isEmpty()) {
        	buildRepository.save(newBuildsList);
		}
        
        log("New builds", start, newBuildsList.size());
    }

    /**
     * Adds new {@link VSTSBuildJob}s to the database as disabled jobs.
     *
     * @param jobs         list of {@link VSTSBuildJob}s
     * @param existingJobs
     * @param collector    the {@link VSTSBuildCollector}
     */
    private void addNewJobs(List<VSTSBuildJob> jobs, List<VSTSBuildJob> existingJobs, VSTSBuildCollector collector) {
        long start = System.currentTimeMillis();
        int count = 0;

        List<VSTSBuildJob> newJobs = new ArrayList<>();
        for (VSTSBuildJob job : jobs) {
            VSTSBuildJob existing = null;
            if (!CollectionUtils.isEmpty(existingJobs) && (existingJobs.contains(job))) {
                existing = existingJobs.get(existingJobs.indexOf(job));
            }
            
            String niceName = getNiceName(job, collector);
            if (existing == null) {
                job.setCollectorId(collector.getId());
                job.setEnabled(false); // Do not enable for collection. Will be enabled when added to dashboard
                job.setDescription(job.getJobName());
                if (StringUtils.isNotEmpty(niceName)) {
                    job.setNiceName(niceName);
                }
                newJobs.add(job);
                count++;
            } else {
            	if (StringUtils.isEmpty(existing.getNiceName()) && StringUtils.isNotEmpty(niceName)) {
	                existing.setNiceName(niceName);
            	}
				if (StringUtils.isNotEmpty(job.getJobName()) && 
						(!job.getJobName().equals(existing.getDescription())
						|| !job.getJobName().equals(existing.getJobName()))) {
            		existing.setDescription(job.getJobName());
            		existing.setJobName(job.getJobName());
				}
            	vSTSBuildJobRepository.save(existing);
            }
            
        }
        //save all in one shot
        if (!CollectionUtils.isEmpty(newJobs)) {
            vSTSBuildJobRepository.save(newJobs);
        }
        log("New jobs", start, count);
    }

	private String getNiceName(VSTSBuildJob job, VSTSBuildCollector collector) {
		if (CollectionUtils.isEmpty(collector.getBuildServers()))
			return "";
		List<String> servers = collector.getBuildServers();
		List<String> niceNames = collector.getNiceNames();
		if (CollectionUtils.isEmpty(niceNames))
			return "";
		for (int i = 0; i < servers.size(); i++) {
			if (servers.get(i).equalsIgnoreCase(job.getInstanceUrl()) && (niceNames.size() > i)) {
				return niceNames.get(i);
			}
		}
		return "";
	}

    private List<VSTSBuildJob> enabledJobs(VSTSBuildCollector collector,
                                        String instanceUrl) {
        return vSTSBuildJobRepository.findEnabledJobs(collector.getId(),
                instanceUrl);
    }

    @SuppressWarnings("unused")
    private VSTSBuildJob getExistingJob(VSTSBuildCollector collector, VSTSBuildJob job) {
        return vSTSBuildJobRepository.findJob(collector.getId(),
                job.getInstanceUrl(), job.getJobName());
    }

}
