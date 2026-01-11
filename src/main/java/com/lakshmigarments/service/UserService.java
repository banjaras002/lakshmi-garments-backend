package com.lakshmigarments.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.lakshmigarments.dto.request.UserCreateRequest;
import com.lakshmigarments.dto.request.UserUpdateRequest;
import com.lakshmigarments.dto.response.UserResponse;

public interface UserService {
	
	Page<UserResponse> getPaginatedUsers(Pageable pageable, String search,
			List<String> roles, List<Boolean> isActive);
	
	UserResponse createUser(UserCreateRequest userCreateRequest);
	
	UserResponse updateUser(Long id, UserUpdateRequest userUpdateRequest);
	
	String adminResetPassword(Long id);

}
