package com.money_hunter.application.dto.response;

import java.util.List;

import org.springframework.data.domain.Page;

public record AdminPaymentPageResponse(
		List<AdminPaymentResponse> content,
		int page,
		int size,
		long totalElements,
		int totalPages,
		boolean first,
		boolean last
) {
	public static AdminPaymentPageResponse from(Page<AdminPaymentResponse> page) {
		return new AdminPaymentPageResponse(
				page.getContent(),
				page.getNumber(),
				page.getSize(),
				page.getTotalElements(),
				page.getTotalPages(),
				page.isFirst(),
				page.isLast());
	}
}
