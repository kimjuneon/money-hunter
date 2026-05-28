package com.money_hunter.application;

public interface TossLoginClient {
	TossLoginUser login(String authorizationCode, String referrer);

	record TossLoginUser(String userKey) {
	}
}
