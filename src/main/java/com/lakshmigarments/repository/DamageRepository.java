package com.lakshmigarments.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.lakshmigarments.model.Damage;
import com.lakshmigarments.model.DamageType;
import com.lakshmigarments.model.JobworkType;

public interface DamageRepository extends JpaRepository<Damage, Long> {

//    @Query("SELECT d FROM Damage d WHERE d.jobwork.batch.id = :batchId")
//    List<Damage> findAllByBatchId(@Param("batchId") Long batchId);
	
//	@Query(value =  "SELECT COALESCE(SUM(quantity), 0) FROM damages d, jobwork_receipts jr"
//			+ "")
	
	@Query(value = "SELECT COALESCE(SUM(quantity), 0) FROM jobworks jw, jobwork_receipts jwr, damages d, batches b "
			+ " WHERE d.jobwork_receipt_id = jwr.id AND damage_type = :damageType AND jwr.jobwork_id = jw.id AND "
			+ " jw.batch_id = b.id AND jw.jobwork_type = :jobworkType AND b.serial_code = :serialCode", nativeQuery = true)
	Long getDamagedQuantity(String serialCode, String damageType, String jobworkType);
	
	@Query(value = "SELECT COALESCE(SUM(quantity), 0) FROM jobworks jw, jobwork_receipts jwr, damages d, batches b, items i, jobwork_receipt_items jwri"
			+ " WHERE d.jobwork_receipt_id = jwr.id AND damage_type = :damageType AND jwr.jobwork_id = jw.id AND "
			+ " jw.batch_id = b.id AND jw.jobwork_type = :jobworkType AND b.serial_code = :serialCode AND i.name = :itemName AND i.id = jwri.item_id", nativeQuery = true)
	Long getDamagedQuantity(String serialCode, String damageType, String jobworkType, String itemName);

}
