package com.lakshmigarments.service;

import com.lakshmigarments.dto.request.UserCreateRequest;
import com.lakshmigarments.dto.response.UserResponse;

public interface UserService {
	
	UserResponse createUser(UserCreateRequest userCreateRequest);

}
