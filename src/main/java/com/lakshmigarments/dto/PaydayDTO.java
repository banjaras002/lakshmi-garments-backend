package com.lakshmigarments.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PaydayDTO {

	private String employeeName;
	private Long completedJobworkCount;
	private Long totalQuantities;
	private Long totalDamages;
	private Long totalSales;
	private Double wage;
	
}
