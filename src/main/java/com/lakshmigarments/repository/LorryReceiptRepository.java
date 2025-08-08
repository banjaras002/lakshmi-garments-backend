package com.lakshmigarments.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.lakshmigarments.model.LorryReceipt;

@Repository
public interface LorryReceiptRepository extends JpaRepository<LorryReceipt, Long> {

	List<LorryReceipt> findByInvoiceId(Long id);
}
