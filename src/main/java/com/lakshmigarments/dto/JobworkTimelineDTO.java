package com.lakshmigarments.dto;

import com.lakshmigarments.dto.request.CreateDamageRequest;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class JobworkTimelineDTO {
	
	private JobworkResponseDTO jobworkDetail;
	private CreateDamageRequest damages;
	private Long salesQuantity;
	private Long salesCost;

}
