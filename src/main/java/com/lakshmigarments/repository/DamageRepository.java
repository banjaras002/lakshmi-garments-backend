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

	@Query(value = """
			  SELECT COALESCE(SUM(d.quantity), 0)
			  FROM damages d
			  JOIN jobwork_receipt_items jwri
			    ON d.jobwork_receipt_item_id = jwri.id
			  JOIN jobwork_receipts jwr
			    ON jwri.jobwork_receipt_id = jwr.id
			  JOIN jobworks jw
			    ON jwr.jobwork_id = jw.id
			  JOIN batches b
			    ON jw.batch_id = b.id
			  WHERE d.damage_type = :damageType
			    AND jw.jobwork_type = :jobworkType
			    AND b.serial_code = :serialCode
			""", nativeQuery = true)
	Long getDamagedQuantity(@Param("serialCode") String serialCode, @Param("damageType") String damageType,
			@Param("jobworkType") String jobworkType);

	@Query(value = "SELECT COALESCE(SUM(d.quantity), 0)\r\n" + "FROM damages d\r\n"
			+ "JOIN jobwork_receipt_items jwri\r\n" + "  ON d.jobwork_receipt_item_id = jwri.id\r\n"
			+ "JOIN jobwork_receipts jwr\r\n" + "  ON jwri.jobwork_receipt_id = jwr.id\r\n" + "JOIN jobworks jw\r\n"
			+ "  ON jwr.jobwork_id = jw.id\r\n" + "JOIN batches b\r\n" + "  ON jw.batch_id = b.id\r\n"
			+ "JOIN items i\r\n" + "  ON jwri.item_id = i.id\r\n" + "WHERE d.damage_type = :damageType\r\n"
			+ "  AND jw.jobwork_type = :jobworkType\r\n" + "  AND b.serial_code = :serialCode\r\n"
			+ "  AND i.name = :itemName\r\n" + "", nativeQuery = true)
	Long getDamagedQuantity(String serialCode, String damageType, String jobworkType, String itemName);

}
