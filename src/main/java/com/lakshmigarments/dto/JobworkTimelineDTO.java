package com.lakshmigarments.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class JobworkTimelineDTO {
	
	private JobworkResponseDTO jobworkDetail;
	private DamageDTO damages;
	private Long salesQuantity;
	private Long salesCost;

}
