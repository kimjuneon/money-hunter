package com.money_hunter.application;

import com.money_hunter.application.dto.response.AdminPaymentResponse;
import com.money_hunter.infrastructure.persistence.IapOrderRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminPaymentService {
	private static final int MAX_PAGE_SIZE = 100;

	private final IapOrderRepository iapOrderRepository;

	public AdminPaymentService(IapOrderRepository iapOrderRepository) {
		this.iapOrderRepository = iapOrderRepository;
	}

	@Transactional(readOnly = true)
	public Page<AdminPaymentResponse> search(int page, int size) {
		int safePage = Math.max(0, page);
		int safeSize = Math.max(1, Math.min(size, MAX_PAGE_SIZE));
		return iapOrderRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(safePage, safeSize))
				.map(AdminPaymentResponse::from);
	}
}
