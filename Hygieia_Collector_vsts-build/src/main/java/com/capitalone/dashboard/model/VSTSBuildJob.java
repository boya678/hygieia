package com.capitalone.dashboard.model;

/**
 * CollectorItem extension to store the instance, build job and build url.
 */
public class VSTSBuildJob extends JobCollectorItem {
	
	protected static final String JOB_ID = "jobID";
	
    public String getJobId() {
        return (String) getOptions().get(JOB_ID);
    }

    public void setJobId(String jobId) {
        getOptions().put(JOB_ID, jobId);
    }

	@Override
    public boolean equals(Object o) {
        if (this == o) {
        	return true;
        }
        if (o == null || getClass() != o.getClass()) {
        	return false;
        }
        VSTSBuildJob other = (VSTSBuildJob) o;
        if (getJobId() == null) {
			if (other.getJobId() != null)
				return false;
		} else if (!getJobId().equals(other.getJobId()))
			return false;
//		if (getNiceName() != other.getNiceName())
//			return false;
		return true;
    }

    @Override
    public int hashCode() {
        int result = getInstanceUrl().hashCode();
        result = 31 * result + getJobId().hashCode();
        return result;
    }
    
}
