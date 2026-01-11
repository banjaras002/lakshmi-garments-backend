package com.lakshmigarments.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.lakshmigarments.dto.request.UserCreateRequest;
import com.lakshmigarments.dto.request.UserUpdateRequest;
import com.lakshmigarments.dto.response.UserResponse;
import com.lakshmigarments.exception.DuplicateUsernameException;
import com.lakshmigarments.exception.RoleNotFoundException;
import com.lakshmigarments.exception.UserNotFoundException;
import com.lakshmigarments.model.Role;
import com.lakshmigarments.model.User;
import com.lakshmigarments.repository.RoleRepository;
import com.lakshmigarments.repository.UserRepository;
import com.lakshmigarments.repository.specification.UserSpecification;
import com.lakshmigarments.service.UserService;
import com.lakshmigarments.utility.PasswordGenerator;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(UserServiceImpl.class);
	private final UserRepository userRepository;
	private final RoleRepository roleRepository;
	private final ModelMapper modelMapper;
	private final PasswordEncoder passwordEncoder;

	
	// GET ALL
	public Page<UserResponse> getPaginatedUsers(Pageable pageable, String search, List<String> roles,
	       List<Boolean> userStatuses) {
		
		pageable = adjustPageableForNestedSort(pageable);

	    Specification<User> userSpecification = Specification.where(null);

	    // Filter by roles
	    if (roles != null && !roles.isEmpty()) {
	        userSpecification = userSpecification.and(UserSpecification.filterByRoles(roles));
	    }

	    // Filter by user status
	    if (userStatuses != null && !userStatuses.isEmpty()) {
	        userSpecification = userSpecification.and(UserSpecification.filterByUserStatus(userStatuses));
	    }
	    // Search filter
	    if (search != null && !search.isEmpty()) {
	    	userSpecification = userSpecification.and(UserSpecification.filterByName(search)
	                   .or(UserSpecification.filterByUsername(search))
	                   .or(UserSpecification.filterByLastName(search)));
	    }

	    Page<User> usersPage = userRepository.findAll(userSpecification, pageable);

	    List<UserResponse> userResponses = usersPage.getContent().stream()
	            .map(user -> modelMapper.map(user, UserResponse.class))
	            .collect(Collectors.toList());

	    LOGGER.debug("Fetched paginated users successfully");
	    return new PageImpl<>(userResponses, usersPage.getPageable(), usersPage.getTotalElements());
	}
	
	@Transactional
	public UserResponse updateUser(Long id, UserUpdateRequest userUpdateRequest) {
		
		String requestUsername = userUpdateRequest.getUsername();
		String requestedRole = userUpdateRequest.getRoleName(); 
		
		User user = userRepository.findById(id).orElseThrow(() -> {
			LOGGER.error("User with ID {} not found", id);
			return new UserNotFoundException("User not found with ID " + id);
		});
		
		if (userRepository.existsByUsernameAndIdNot(requestUsername, id)) {
			LOGGER.warn("User with username {} already exists", requestUsername);
			throw new DuplicateUsernameException("User with username '" + requestUsername + "' already exists");
		}
		
		// check if same role already
		if (!requestedRole.equals(user.getRole().getName())) {
			Role role = roleRepository.findByName(requestedRole).orElseThrow(() -> {
				LOGGER.error("Role with name {} not found", requestedRole);
				return new RoleNotFoundException("Role not found with name " + requestedRole);
			});
			user.setRole(role);
		}
		
		user.setFirstName(userUpdateRequest.getFirstName() != null ? 
				userUpdateRequest.getFirstName() : user.getFirstName());
		user.setLastName(userUpdateRequest.getLastName() != null ? 
				userUpdateRequest.getLastName() : user.getLastName());
		user.setUsername(userUpdateRequest.getUsername());
		user.setIsActive(userUpdateRequest.getIsActive() != null ? 
				userUpdateRequest.getIsActive() : user.getIsActive());
		
		User savedUser = userRepository.save(user);
		LOGGER.info("User updated successfully with ID: {}", savedUser.getId());
		return modelMapper.map(savedUser, UserResponse.class);
	}

	@Override
	@Transactional
	public UserResponse createUser(UserCreateRequest userCreateRequest) {	
	    Role role = roleRepository.findByName(userCreateRequest.getRoleName()).orElseThrow(() -> {
	        LOGGER.error("Role with name {} not found", userCreateRequest.getRoleName());
	        return new RoleNotFoundException("Role not found with name " + userCreateRequest.getRoleName());
	    });
	    
	    
	    if (userRepository.existsByUsername(userCreateRequest.getUsername())) {
	        LOGGER.error("User with username {} already exists", userCreateRequest.getUsername());
	        throw new DuplicateUsernameException("User with username '" + userCreateRequest.getUsername() + "' already exists");
	    }
	    
	    Boolean isActive = userCreateRequest.getIsActive() != null ? userCreateRequest.getIsActive() : true;
	    
	    User user = new User();
	    user.setFirstName(userCreateRequest.getFirstName());
	    user.setLastName(userCreateRequest.getLastName());
	    user.setUsername(userCreateRequest.getUsername());
	    user.setPassword(passwordEncoder.encode(userCreateRequest.getPassword()));
	    user.setIsActive(isActive);
	    user.setRole(role);
	    
	    User savedUser = userRepository.save(user);
	    
	    LOGGER.info("User created successfully with username: {}", savedUser.getUsername());
	    return modelMapper.map(savedUser, UserResponse.class);	
	}
	
	@Transactional
	public String adminResetPassword(Long id) {
		
		User user = userRepository.findById(id).orElseThrow(() -> {
			LOGGER.error("User with ID {} not found", id);
			return new RuntimeException("User not found with ID " + id);
		});

	    String tempPassword = PasswordGenerator.generateTemporaryPassword(8);

	    user.setPassword(passwordEncoder.encode(tempPassword));
	    // user.setRequiresPasswordChange(true); 

	    userRepository.save(user);
	    LOGGER.info("Admin reset password for user ID: {}", id);

	    return tempPassword; 
	}
	
	private Pageable adjustPageableForNestedSort(Pageable pageable) {

	    Sort adjustedSort = Sort.by(
	        pageable.getSort().stream()
	            .map(order -> {
	                return switch (order.getProperty()) {
	                    case "role", "roleName" ->
	                        new Sort.Order(order.getDirection(), "role.name");
	                    default ->
	                        order;
	                };
	            })
	            .toList()
	    );

	    return PageRequest.of(
	        pageable.getPageNumber(),
	        pageable.getPageSize(),
	        adjustedSort
	    );
	}

	
}
