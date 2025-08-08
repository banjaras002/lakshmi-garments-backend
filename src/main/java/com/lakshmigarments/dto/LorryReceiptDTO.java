package com.lakshmigarments.dto;

import java.util.List;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class LorryReceiptDTO {

	private Long id;
	
	private String LRNumber;
		
	private List<BaleDTO> baleDTOs;
}
