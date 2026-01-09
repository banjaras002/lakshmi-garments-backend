package com.lakshmigarments.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class JobworkReceiptItemDTO {

	private String itemName;
	private Long returnedQuantity;
	private List<DamageDTO> damages;
	private Long purchasedQuantity;
	private Double purchaseCost;
	private Double wage;
	private Long damagedQuantity;
	
}
