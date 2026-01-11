package com.lakshmigarments.dto.request;

import lombok.Data;

@Data
public class UserUpdateRequest {
	
	private String firstName;
	
	private String lastName;
	
	private String username;
		
	private String roleName;
	
	private Boolean isActive;

}
