package com.money_hunter.application;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class AdminLoginAttemptLimiter {
	private static final int MAX_FAILED_ATTEMPTS = 8;
	private static final Duration WINDOW = Duration.ofMinutes(15);

	private final ConcurrentMap<String, FailedAttemptWindow> attempts = new ConcurrentHashMap<>();
	private final Clock clock = Clock.systemUTC();

	public void requireAllowed(String key) {
		Instant now = clock.instant();
		FailedAttemptWindow window = attempts.get(key);
		if (window == null) {
			return;
		}
		if (window.isExpired(now)) {
			attempts.remove(key, window);
			return;
		}
		if (window.failures() >= MAX_FAILED_ATTEMPTS) {
			throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "관리자 로그인 시도가 너무 많아요. 잠시 후 다시 시도해 주세요.");
		}
	}

	public void recordFailure(String key) {
		Instant now = clock.instant();
		attempts.compute(key, (ignored, current) -> {
			if (current == null || current.isExpired(now)) {
				return new FailedAttemptWindow(1, now);
			}
			return current.increment();
		});
	}

	public void recordSuccess(String key) {
		attempts.remove(key);
	}

	private record FailedAttemptWindow(int failures, Instant firstFailedAt) {
		boolean isExpired(Instant now) {
			return firstFailedAt.plus(WINDOW).isBefore(now);
		}

		FailedAttemptWindow increment() {
			return new FailedAttemptWindow(failures + 1, firstFailedAt);
		}
	}
}
