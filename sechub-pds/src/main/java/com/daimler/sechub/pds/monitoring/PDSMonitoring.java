package com.daimler.sechub.pds.monitoring;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.daimler.sechub.pds.PDSJSONConverter;
import com.daimler.sechub.pds.PDSJSONConverterException;
import com.daimler.sechub.pds.job.PDSJobStatusState;

public class PDSMonitoring {

    public static PDSMonitoring fromJSON(String json) throws PDSJSONConverterException {
        return PDSJSONConverter.get().fromJSON(PDSMonitoring.class, json);
    }

    public String toJSON() throws PDSJSONConverterException {
        return PDSJSONConverter.get().toJSON(this);
    }

    private Map<PDSJobStatusState, Long> jobs = new LinkedHashMap<>();
    private List<PDSClusterMember> members = new ArrayList<>();

    private String serverId;

    public String getServerId() {
        return serverId;
    }

    public void setServerId(String serverId) {
        this.serverId = serverId;
    }

    public Map<PDSJobStatusState, Long> getJobs() {
        return jobs;
    }

    public void setJobs(Map<PDSJobStatusState, Long> jobs) {
        this.jobs = jobs;
    }

    public List<PDSClusterMember> getMembers() {
        return members;
    }

    public void setMembers(List<PDSClusterMember> clusterMembers) {
        this.members = clusterMembers;
    }

}
