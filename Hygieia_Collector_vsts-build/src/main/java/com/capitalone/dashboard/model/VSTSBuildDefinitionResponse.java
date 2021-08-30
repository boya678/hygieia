package com.capitalone.dashboard.model;

import java.util.List;

public class VSTSBuildDefinitionResponse {

	private long count;
	private List<VSTSBuildDefinition> value;

	public long getCount() {
		return count;
	}

	public void setCount(long count) {
		this.count = count;
	}

	public List<VSTSBuildDefinition> getValue() {
		return value;
	}

	public void setValue( List<VSTSBuildDefinition> value) {
		this.value = value;
	}
}
