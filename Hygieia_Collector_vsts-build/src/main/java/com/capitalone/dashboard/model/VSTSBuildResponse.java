package com.capitalone.dashboard.model;

import java.util.List;

public class VSTSBuildResponse {
	
	private long count;
	private List<VSTSBuild> value;

	public long getCount() {
		return count;
	}

	public void setCount(long count) {
		this.count = count;
	}

	public List<VSTSBuild> getValue() {
		return value;
	}

	public void setValue( List<VSTSBuild> value) {
		this.value = value;
	}
}
