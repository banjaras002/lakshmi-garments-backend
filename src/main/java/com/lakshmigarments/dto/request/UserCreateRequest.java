package com.lakshmigarments.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UserCreateRequest {
	
	@NotBlank(message = "First name is required")
	private String firstName;
	
	private String lastName;
	
	@NotBlank(message = "Username is required")
	private String username;
	
	@NotBlank(message = "Password is required")
	private String password;
	
	@NotBlank(message = "Role name is required")
	private String roleName;
	
	private Boolean isActive;

}
