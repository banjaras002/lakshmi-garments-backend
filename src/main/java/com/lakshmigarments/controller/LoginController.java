package com.lakshmigarments.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.lakshmigarments.dto.LoginRequest;
import com.lakshmigarments.model.User;
import com.lakshmigarments.repository.UserRepository;

@RestController
@RequestMapping("/login")
@CrossOrigin(origins = "*")
public class LoginController {
	
	private UserRepository userRepository;
	
	public LoginController(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

    @PostMapping
    public ResponseEntity<User> login(@RequestBody LoginRequest request) {
    	
    	User user = userRepository.findByName(request.getUsername()).orElse(null);
    	
    	if (user == null) {
    		return ResponseEntity.status(404).body(user);
		} else {
			User authenticatedUser = userRepository.findByNameAndPassword(request.getUsername(), request.getPassword()).orElse(null);
			if (authenticatedUser == null) {
				return ResponseEntity.status(401).body(authenticatedUser);
			} else {
				return ResponseEntity.status(200).body(authenticatedUser);
			}
		}
    }
}
