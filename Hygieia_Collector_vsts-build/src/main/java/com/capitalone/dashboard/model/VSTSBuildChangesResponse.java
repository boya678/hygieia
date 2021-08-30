package com.capitalone.dashboard.model;

import java.util.List;

public class VSTSBuildChangesResponse {
	private long count;
	private List<VSTSBuildChanges> value;

	public long getCount() {
		return count;
	}

	public void setCount(long count) {
		this.count = count;
	}

	public List<VSTSBuildChanges> getValue() {
		return value;
	}

	public void setValue( List<VSTSBuildChanges> value) {
		this.value = value;
	}
}
