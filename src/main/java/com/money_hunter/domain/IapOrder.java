package com.money_hunter.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "iap_orders")
public class IapOrder {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "order_id", nullable = false, unique = true)
	private String orderId;

	@Column(name = "user_key", nullable = false)
	private String userKey;

	@Column(name = "product_id", nullable = false)
	private String productId;

	@Column(name = "product_type", nullable = false)
	private String productType;

	@Column(name = "granted_at")
	private Instant grantedAt;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	protected IapOrder() {
	}

	public IapOrder(String orderId, String userKey, String productId, String productType, Instant createdAt) {
		this.orderId = orderId;
		this.userKey = userKey;
		this.productId = productId;
		this.productType = productType;
		this.createdAt = createdAt;
	}

	public String getOrderId() {
		return orderId;
	}

	public String getUserKey() {
		return userKey;
	}

	public String getProductId() {
		return productId;
	}

	public String getProductType() {
		return productType;
	}

	public boolean isGranted() {
		return grantedAt != null;
	}

	public void markGranted(Instant grantedAt) {
		if (this.grantedAt == null) {
			this.grantedAt = grantedAt;
		}
	}
}
