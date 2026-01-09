package com.lakshmigarments.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
public class BatchDetailDTO {
	
	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	public static class SubCategoryQuantityDTO {
		
		private Long id;
		
		private String subCategoryName;
		
		private Long quantity;
	};

	private String batchSerialCode;
	private DamageDTO damages;
	private SubCategoryQuantityDTO availableQuantities;
	private SubCategoryCountDTO quantitiesWithEmployees;
	private ItemResponseDTO itemsWithEmployees;
	
}
