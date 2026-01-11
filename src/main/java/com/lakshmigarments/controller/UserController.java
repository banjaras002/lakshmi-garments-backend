package com.lakshmigarments.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.lakshmigarments.dto.request.UserCreateRequest;
import com.lakshmigarments.dto.request.UserUpdateRequest;
import com.lakshmigarments.dto.response.UserResponse;
import com.lakshmigarments.service.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

	private static final Logger LOGGER = LoggerFactory.getLogger(UserController.class);
	private final UserService userService;

	@GetMapping
	public ResponseEntity<Page<UserResponse>> getPaginatedUsers(
	        @PageableDefault(size = 7, sort = "firstName", direction = Sort.Direction.DESC) Pageable pageable,
	        @RequestParam(required = false) String search,
	        @RequestParam(required = false) List<String> roles,
	        @RequestParam(required = false) List<Boolean> isActive) {

	    LOGGER.info("Fetching all users with filters - Search: {}, Roles: {}", search, roles);

	    Page<UserResponse> users = userService.getPaginatedUsers(pageable, search, roles, isActive);
	    return new ResponseEntity<>(users, HttpStatus.OK);
	}

	@PostMapping
	public ResponseEntity<UserResponse> createUser(@RequestBody @Valid UserCreateRequest userCreateRequest) {
		LOGGER.info("Creating user with username: {}", userCreateRequest.getUsername());
		UserResponse userResponse = userService.createUser(userCreateRequest);
		return new ResponseEntity<>(userResponse, HttpStatus.CREATED);
	}

	@PutMapping("/{id}")
	public ResponseEntity<UserResponse> updateUser(@PathVariable Long id, 
			@RequestBody @Valid UserUpdateRequest userUpdateRequest) {
		LOGGER.info("Updating user with username: {}", userUpdateRequest.getUsername());
		UserResponse updatedUser = userService.updateUser(id, userUpdateRequest);
		return ResponseEntity.ok(updatedUser);
	}
	
	@PatchMapping("/{id}/reset-password")
	public ResponseEntity<String> resetPassword(@PathVariable Long id) {
		LOGGER.info("Resetting password for the user with ID {}", id);
	    String newPassword = userService.adminResetPassword(id);
	    return ResponseEntity.ok(newPassword);
	}
	
}
