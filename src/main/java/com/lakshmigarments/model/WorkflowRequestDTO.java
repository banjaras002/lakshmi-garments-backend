package com.lakshmigarments.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WorkflowRequestDTO {

	private String requestType;
	private String requestStatus;
	private String payload;
	private String remarks;
	private String systemComments;
	
}
