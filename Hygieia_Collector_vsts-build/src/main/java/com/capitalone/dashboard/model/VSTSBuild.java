package com.capitalone.dashboard.model;

public class VSTSBuild {
	
	private String startTime;
	private String finishTime;
	private String status;
	private String result;
	private RequestedBuild requestedFor;
	private String buildNumber;
	private LinksBuild _links;
	private String id;
	
	public String getStartTime() {
		return startTime;
	}
	public void setStartTime(String startTime) {
		this.startTime = startTime;
	}
	public String getFinishTime() {
		return finishTime;
	}
	public void setFinishTime(String finishTime) {
		this.finishTime = finishTime;
	}
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	public String getResult() {
		return result;
	}
	public void setResult(String result) {
		this.result = result;
	}
	public RequestedBuild getRequestedFor() {
		return requestedFor;
	}
	public void setRequestedFor(RequestedBuild requestedFor) {
		this.requestedFor = requestedFor;
	}
	public String getBuildNumber() {
		return buildNumber;
	}
	public void setBuildNumber(String buildNumber) {
		this.buildNumber = buildNumber;
	}
	public LinksBuild get_links() {
		return _links;
	}
	public void set_links(LinksBuild _links) {
		this._links = _links;
	}
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	
	

}
