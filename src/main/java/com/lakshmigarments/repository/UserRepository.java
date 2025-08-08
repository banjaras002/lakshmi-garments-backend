package com.lakshmigarments.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.lakshmigarments.model.User;

public interface UserRepository extends JpaRepository<User, Long>  {

	Optional<User> findByName(String name);
	
	Optional<User> findByNameAndPassword(String name, String password);
	
}
