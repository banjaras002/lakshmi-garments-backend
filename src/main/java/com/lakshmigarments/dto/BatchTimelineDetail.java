package com.lakshmigarments.dto;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BatchTimelineDetail {

	private String message;
	private String timeTakenFromPrevious;
//	private String assignedBy;
//	private String performedBy;
	private LocalDateTime performedAt;
	private String stage;
//	private String receivedBy;
	
}
