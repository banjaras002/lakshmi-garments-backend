package com.lakshmigarments.service.impl;

import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.lakshmigarments.dto.PaydayDTO;
import com.lakshmigarments.model.JobworkReceipt;
import com.lakshmigarments.repository.JobworkReceiptRepository;
import com.lakshmigarments.repository.JobworkRepository;
import com.lakshmigarments.service.PaydayService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PaydayServiceImpl implements PaydayService {

	private final Logger LOGGER = LoggerFactory.getLogger(PaydayServiceImpl.class);

	private final JobworkReceiptRepository receiptRepository;

	@Override
	public Page<PaydayDTO> getAllPayday(String employeeName, LocalDateTime fromDate, LocalDateTime toDate,
			Pageable pageable) {

		LOGGER.info("Fetching payday summary | employeeName={}, fromDate={}, toDate={}", employeeName, fromDate,
				toDate);

		return receiptRepository.getPaydaySummary(fromDate, toDate, pageable);
	}
}
