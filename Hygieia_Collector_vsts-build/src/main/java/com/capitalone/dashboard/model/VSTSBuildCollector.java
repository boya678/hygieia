package com.capitalone.dashboard.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.util.CollectionUtils;

/**
 * Extension of Collector that stores current build server configuration.
 */
public class VSTSBuildCollector extends Collector {
    private List<String> buildServers = new ArrayList<>();
    private List<String> niceNames = new ArrayList<>();

    public List<String> getBuildServers() {
        return buildServers;
    }

    public List<String> getNiceNames() {
        return niceNames;
    }

    public void setNiceNames(List<String> niceNames) {
        this.niceNames = niceNames;
    }

    public void setBuildServers(List<String> buildServers) {
        this.buildServers = buildServers;
    }

    public static VSTSBuildCollector prototype(List<String> buildServers, List<String> niceNames) {
        VSTSBuildCollector protoType = new VSTSBuildCollector();
        protoType.setName("VSTSBuild");
        protoType.setCollectorType(CollectorType.Build);
        protoType.setOnline(true);
        protoType.setEnabled(true);
        protoType.getBuildServers().addAll(buildServers);
        if (!CollectionUtils.isEmpty(niceNames)) {
            protoType.getNiceNames().addAll(niceNames);
        }
        Map<String, Object> options = new HashMap<>();
        options.put(VSTSBuildJob.INSTANCE_URL,"");
        options.put(VSTSBuildJob.JOB_URL,"");
        options.put(VSTSBuildJob.JOB_NAME,"");
        protoType.setAllFields(options);
        protoType.setUniqueFields(options);
        return protoType;
    }
}
