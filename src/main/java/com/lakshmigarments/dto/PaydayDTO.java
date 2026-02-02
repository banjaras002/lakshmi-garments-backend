package com.lakshmigarments.dto;

import java.util.List;

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
	private Long pendingJobworkCount; // Jobworks not in CLOSED or REASSIGNED status
	private List<String> pendingJobworks; // List of pending jobwork numbers
	private Long totalAcceptedQuantity; // Pieces completed
	private Double grossWage; // acceptedQuantity * wagePerItem
	private Long salesQuantity;
	private Double salesDeduction; // salesQuantity * salesPrice
	private Long unrepairableDamageQuantity;
	private Double unrepairableDamageDeduction; // unrepairableDamage * salesPrice
	private Long repairableDamageQuantity; // For reference only, no deduction
	private Long supplierDamageQuantity; // For reference only, no deduction
	private Double netWage; // grossWage - salesDeduction - unrepairableDamageDeduction
	
}
