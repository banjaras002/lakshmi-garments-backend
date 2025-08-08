package com.lakshmigarments.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.lakshmigarments.model.BatchStatus;

public interface BatchStatusRepository extends JpaRepository<BatchStatus, Long> {
	
	Optional<BatchStatus> findByName(String name);

}
