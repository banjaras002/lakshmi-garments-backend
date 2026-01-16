package com.lakshmigarments.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Setter
@Getter
public class CreateCuttingJobworkRequest extends CreateJobworkRequest {
	private Long quantity;
}
