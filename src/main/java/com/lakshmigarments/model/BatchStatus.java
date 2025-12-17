package com.lakshmigarments.model;

public enum BatchStatus {
	CREATED("Created"),
	WIP("Work In Progress"),
	PACKAGED("Packaged"),
	DISCARDED("Discarded"),
	PARTIAL_PACKED("Partial Packed");
	
	private final String value;
	
	BatchStatus(String value) {
		this.value = value;
	}
	
	public String getValue() {
		return value;
	}
	
}
