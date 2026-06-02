package com.money_hunter.application.dto.response;

import java.util.List;

import org.springframework.data.domain.Page;

public record AdminPlayerPageResponse(
		List<AdminPlayerResponse> content,
		int page,
		int size,
		long totalElements,
		int totalPages,
		boolean first,
		boolean last
) {
	public static AdminPlayerPageResponse from(Page<AdminPlayerResponse> page) {
		return new AdminPlayerPageResponse(
				page.getContent(),
				page.getNumber(),
				page.getSize(),
				page.getTotalElements(),
				page.getTotalPages(),
				page.isFirst(),
				page.isLast());
	}
}
