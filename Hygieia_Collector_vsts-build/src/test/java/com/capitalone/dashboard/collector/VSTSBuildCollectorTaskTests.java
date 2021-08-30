package com.capitalone.dashboard.collector;

import static org.mockito.Matchers.anyListOf;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bson.types.ObjectId;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.scheduling.TaskScheduler;

import com.capitalone.dashboard.model.Build;
import com.capitalone.dashboard.model.Component;
import com.capitalone.dashboard.model.VSTSBuildCollector;
import com.capitalone.dashboard.model.VSTSBuildJob;
import com.capitalone.dashboard.repository.BuildRepository;
import com.capitalone.dashboard.repository.ComponentRepository;
import com.capitalone.dashboard.repository.VSTSBuildCollectorRepository;
import com.capitalone.dashboard.repository.VSTSBuildJobRepository;

@RunWith(MockitoJUnitRunner.class)
public class VSTSBuildCollectorTaskTests {

    @Mock
    private TaskScheduler taskScheduler;
    @Mock
    private VSTSBuildCollectorRepository vSTSBuildCollectorRepository;
    @Mock
    private VSTSBuildJobRepository vSTSBuildJobRepository;
    @Mock
    private BuildRepository buildRepository;
    @Mock
    private VSTSBuildClient vSTSBuildClient;
    @Mock
    private VSTSBuildSettings vSTSBuildSettings;
    @Mock
    private ComponentRepository dbComponentRepository;

    @InjectMocks
    private VSTSBuildCollectorTask task;

    private static final String SERVER1 = "server1";
    private static final String NICENAME1 = "niceName1";

    @Test
    public void collect_noBuildServers_nothingAdded() {
        when(dbComponentRepository.findAll()).thenReturn(components());
        task.collect(new VSTSBuildCollector());
        verifyZeroInteractions(vSTSBuildClient, buildRepository);
    }

    @Test
    public void collect_noJobsOnServer_nothingAdded() {
        when(vSTSBuildClient.getInstanceJobs(SERVER1)).thenReturn(new ArrayList<VSTSBuildJob>());
        when(dbComponentRepository.findAll()).thenReturn(components());
        task.collect(collectorWithOneServer());

        verify(vSTSBuildClient).getInstanceJobs(SERVER1);
        verifyNoMoreInteractions(vSTSBuildClient, buildRepository);
    }

    @Test
    public void collect_twoJobs_jobsAdded() {
        when(vSTSBuildClient.getInstanceJobs(SERVER1)).thenReturn(twoJobsWithTwoBuilds(SERVER1, NICENAME1));
        when(dbComponentRepository.findAll()).thenReturn(components());
        List<VSTSBuildJob> vSTSBuildJobs = new ArrayList<>();
        VSTSBuildJob vSTSBuildJob = vSTSBuildJob("1", SERVER1, "JOB1_URL", NICENAME1);
        vSTSBuildJobs.add(vSTSBuildJob);
        when(vSTSBuildJobRepository.findEnabledJobs(null, "server1")).thenReturn(vSTSBuildJobs);
        task.collect(collectorWithOneServer());
        verify(vSTSBuildJobRepository, times(1)).save(anyListOf(VSTSBuildJob.class));
    }

    @Test
    public void collect_twoJobs_jobsAdded_random_order() {
        when(vSTSBuildClient.getInstanceJobs(SERVER1)).thenReturn(twoJobsWithTwoBuildsRandom(SERVER1, NICENAME1));
        when(dbComponentRepository.findAll()).thenReturn(components());
        List<VSTSBuildJob> vSTSBuildJobs = new ArrayList<>();
        VSTSBuildJob vSTSBuildJob = vSTSBuildJob("2", SERVER1, "JOB2_URL", NICENAME1);
        vSTSBuildJobs.add(vSTSBuildJob);
        when(vSTSBuildJobRepository.findEnabledJobs(null, "server1")).thenReturn(vSTSBuildJobs);
        task.collect(collectorWithOneServer());
        verify(vSTSBuildJobRepository, times(1)).save(anyListOf(VSTSBuildJob.class));
    }

    @Test
    public void collect_oneJob_exists_notAdded() {
        VSTSBuildCollector collector = collectorWithOneServer();
        VSTSBuildJob job = vSTSBuildJob("1", SERVER1, "JOB1_URL", NICENAME1);
        when(vSTSBuildClient.getInstanceJobs(SERVER1)).thenReturn(oneJobWithBuilds(job));
        when(vSTSBuildJobRepository.findJob(collector.getId(), SERVER1, job.getJobName()))
                .thenReturn(job);
        when(dbComponentRepository.findAll()).thenReturn(components());

        task.collect(collector);

        verify(vSTSBuildJobRepository, never()).save(job);
    }


    @Test
    public void delete_job() {
        VSTSBuildCollector collector = collectorWithOneServer();
        collector.setId(ObjectId.get());
        VSTSBuildJob job1 = vSTSBuildJob("1", SERVER1, "JOB1_URL", NICENAME1);
        job1.setCollectorId(collector.getId());
        VSTSBuildJob job2 = vSTSBuildJob("2", SERVER1, "JOB2_URL", NICENAME1);
        job2.setCollectorId(collector.getId());
        List<VSTSBuildJob> jobs = new ArrayList<>();
        jobs.add(job1);
        jobs.add(job2);
        Set<ObjectId> udId = new HashSet<>();
        udId.add(collector.getId());
        when(vSTSBuildClient.getInstanceJobs(SERVER1)).thenReturn(oneJobWithBuilds(job1));
        when(vSTSBuildJobRepository.findByCollectorIdIn(udId)).thenReturn(jobs);
        when(dbComponentRepository.findAll()).thenReturn(components());
        task.collect(collector);
        List<VSTSBuildJob> delete = new ArrayList<>();
        delete.add(job2);
        verify(vSTSBuildJobRepository, times(1)).delete(delete);
    }

    @Test
    public void delete_never_job() {
        VSTSBuildCollector collector = collectorWithOneServer();
        collector.setId(ObjectId.get());
        VSTSBuildJob job1 = vSTSBuildJob("1", SERVER1, "JOB1_URL", NICENAME1);
        job1.setCollectorId(collector.getId());
        List<VSTSBuildJob> jobs = new ArrayList<>();
        jobs.add(job1);
        Set<ObjectId> udId = new HashSet<>();
        udId.add(collector.getId());
        when(vSTSBuildClient.getInstanceJobs(SERVER1)).thenReturn(oneJobWithBuilds(job1));
        when(vSTSBuildJobRepository.findByCollectorIdIn(udId)).thenReturn(jobs);
        when(dbComponentRepository.findAll()).thenReturn(components());
        task.collect(collector);
        verify(vSTSBuildJobRepository, never()).delete(anyListOf(VSTSBuildJob.class));
    }

    @Test
    public void collect_jobNotEnabled_buildNotAdded() {
        VSTSBuildCollector collector = collectorWithOneServer();
        VSTSBuildJob job = vSTSBuildJob("1", SERVER1, "JOB1_URL", NICENAME1);
        Build build = build("1", "JOB1_1_URL");

        when(vSTSBuildClient.getInstanceJobs(SERVER1)).thenReturn(oneJobWithBuilds(job, build));
        when(dbComponentRepository.findAll()).thenReturn(components());
        task.collect(collector);

        verify(buildRepository, never()).save(build);
    }

    @Test
    public void collect_jobEnabled_buildExists_buildNotAdded() {
        VSTSBuildCollector collector = collectorWithOneServer();
        VSTSBuildJob job = vSTSBuildJob("1", SERVER1, "JOB1_URL", NICENAME1);
        Build build = build("1", "JOB1_1_URL");

        when(vSTSBuildClient.getInstanceJobs(SERVER1)).thenReturn(oneJobWithBuilds(job, build));
        when(vSTSBuildJobRepository.findEnabledJobs(collector.getId(), SERVER1))
                .thenReturn(Arrays.asList(job));
        when(buildRepository.findByCollectorItemIdAndNumber(job.getId(), build.getNumber())).thenReturn(build);
        when(dbComponentRepository.findAll()).thenReturn(components());
        task.collect(collector);

        verify(buildRepository, never()).save(build);
    }

//    @Test
//    public void collect_jobEnabled_newBuild_buildAdded() {
//        VSTSBuildCollector collector = collectorWithOneServer();
//        VSTSBuildJob job = vSTSBuildJob("1", SERVER1, "JOB1_URL", NICENAME1);
//        Build build = build("1", "JOB1_1_URL");
//
//        when(vSTSBuildClient.getInstanceJobs(SERVER1)).thenReturn(oneJobWithBuilds(job, build));
//        when(vSTSBuildJobRepository.findEnabledJobs(collector.getId(), SERVER1))
//                .thenReturn(Arrays.asList(job));
//        when(buildRepository.findByCollectorItemIdAndNumber(job.getId(), build.getNumber())).thenReturn(null);
//        when(vSTSBuildClient.getBuildDetails(build.getBuildUrl(), job.getInstanceUrl())).thenReturn(build);
//        when(dbComponentRepository.findAll()).thenReturn(components());
//        task.collect(collector);
//
//        verify(buildRepository, times(1)).save(build);
//    }

    private VSTSBuildCollector collectorWithOneServer() {
        return VSTSBuildCollector.prototype(Arrays.asList(SERVER1), Arrays.asList(NICENAME1));
    }

    private List<VSTSBuildJob> oneJobWithBuilds(VSTSBuildJob job, Build... builds) {
    	List<VSTSBuildJob> jobs = new ArrayList<>();
        jobs.add(job);
        return jobs;
    }

    private List<VSTSBuildJob> twoJobsWithTwoBuilds(String server, String niceName) {
    	List<VSTSBuildJob> jobs = new ArrayList<>();
        jobs.add(vSTSBuildJob("1", server, "JOB1_URL", niceName));
        jobs.add(vSTSBuildJob("2", server, "JOB2_URL", niceName));
        return jobs;
    }

    private List<VSTSBuildJob>twoJobsWithTwoBuildsRandom(String server, String niceName) {
    	List<VSTSBuildJob> jobs = new ArrayList<>();
        jobs.add(vSTSBuildJob("2", server, "JOB2_URL", niceName));
        jobs.add(vSTSBuildJob("1", server, "JOB1_URL", niceName));
        return jobs;
    }

    private VSTSBuildJob vSTSBuildJob(String jobName, String instanceUrl, String jobUrl, String niceName) {
        VSTSBuildJob job = new VSTSBuildJob();
        job.setJobName(jobName);
        job.setInstanceUrl(instanceUrl);
        job.setJobUrl(jobUrl);
        job.setNiceName(niceName);
        return job;
    }

    private Build build(String number, String url) {
        Build build = new Build();
        build.setNumber(number);
        build.setBuildUrl(url);
        return build;
    }

    private ArrayList<com.capitalone.dashboard.model.Component> components() {
        ArrayList<com.capitalone.dashboard.model.Component> cArray = new ArrayList<com.capitalone.dashboard.model.Component>();
        com.capitalone.dashboard.model.Component c = new Component();
        c.setId(new ObjectId());
        c.setName("COMPONENT1");
        c.setOwner("JOHN");
        cArray.add(c);
        return cArray;
    }
}
