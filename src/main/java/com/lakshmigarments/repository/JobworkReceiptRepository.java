package com.lakshmigarments.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.lakshmigarments.dto.PaydayDTO;
import com.lakshmigarments.model.JobworkReceipt;
import com.lakshmigarments.model.JobworkType;

@Repository
public interface JobworkReceiptRepository extends JpaRepository<JobworkReceipt, Long> {

	List<JobworkReceipt> findByJobworkJobworkNumberIn(List<String> jobworkNumbers);
	
	@Query(value = "SELECT COALESCE(SUM(COALESCE(jri.damaged_quantity,0) + COALESCE(jri.purchase_quantity,0) + COALESCE(jri.received_quantity,0)),0) " +
            "FROM jobwork_receipt_items jri JOIN jobwork_receipts jr ON jri.jobwork_receipt_id = jr.id " +
            "WHERE jr.jobwork_id = :jobworkId", nativeQuery = true)
	Long findReturnedUnits(@Param("jobworkId") Long jobworkId);
	
	List<JobworkReceipt> findByJobworkBatchSerialCode(String serialCode);
	
	List<JobworkReceipt> findByJobworkBatchSerialCodeAndJobworkJobworkType(
			String serialCode, JobworkType jobworkType);

	
	@Query("""
		    SELECT new com.lakshmigarments.dto.PaydayDTO(
		        e.name,
		        COUNT(DISTINCT r.id),
		        COALESCE(SUM(i.receivedQuantity), 0),
		        COALESCE(SUM(i.damagedQuantity), 0),
		        COALESCE(SUM(i.purchaseQuantity), 0),
		        COALESCE(SUM(i.receivedQuantity * i.wagePerItem), 0)
		    )
		    FROM JobworkReceipt r
		    JOIN r.completedBy e
		    JOIN r.jobworkReceiptItems i
		    WHERE (:employeeName IS NULL 
		           OR LOWER(e.name) LIKE LOWER(CONCAT('%', :employeeName, '%')))
		      AND (:fromDate IS NULL OR r.receivedAt >= :fromDate)
		      AND (:toDate IS NULL OR r.receivedAt <= :toDate)
		    GROUP BY e.id, e.name
		    ORDER BY e.name ASC
		""")
		Page<PaydayDTO> getPaydaySummary(
		    @Param("employeeName") String employeeName,
		    @Param("fromDate") LocalDateTime fromDate,
		    @Param("toDate") LocalDateTime toDate,
		    Pageable pageable
		);

	
}
