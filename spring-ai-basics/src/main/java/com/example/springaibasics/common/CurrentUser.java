package com.example.springaibasics.common;

import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

/**
 * STAND-IN FOR SPRING SECURITY. In a real app the user comes from the authenticated
 * principal, never from a header the client controls. It is a header here only so the
 * demo has no login screen. The point: every concept below asks THIS class "who is
 * calling?" and never trusts an id from the request body or from the model.
 */
@Component
public class CurrentUser {

	/** user -> tenant. Two users share tenant "acme"; carol is in "globex". */
	private static final Map<String, String> TENANT_OF = Map.of(
			"alice", "acme",
			"bob", "acme",
			"carol", "globex");

	private final HttpServletRequest request;

	public CurrentUser(HttpServletRequest request) {
		this.request = request;
	}

	public String id() {
		String user = request.getHeader("X-User");
		if (user == null || !TENANT_OF.containsKey(user)) {
			throw new IllegalArgumentException("Unknown user. Send X-User: alice | bob | carol");
		}
		return user;
	}

	public String tenant() {
		return TENANT_OF.get(id());
	}
}
