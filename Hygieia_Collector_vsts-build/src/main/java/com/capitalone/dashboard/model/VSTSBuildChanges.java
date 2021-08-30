package com.capitalone.dashboard.model;

public class VSTSBuildChanges {
	
	private String message;
	private RequestedBuild author;
	private String timestamp;
	private String id;
	private String location;
	
	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}
	public RequestedBuild getAuthor() {
		return author;
	}
	public void setAuthor(RequestedBuild author) {
		this.author = author;
	}
	public String getTimestamp() {
		return timestamp;
	}
	public void setTimestamp(String timestamp) {
		this.timestamp = timestamp;
	}
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getLocation() {
		return location;
	}
	public void setLocation(String location) {
		this.location = location;
	}
	

}
