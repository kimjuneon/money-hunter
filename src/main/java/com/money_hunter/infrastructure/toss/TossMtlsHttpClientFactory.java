package com.money_hunter.infrastructure.toss;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Duration;
import java.util.Base64;
import java.util.Collection;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;

import com.money_hunter.infrastructure.config.TossApiProperties;

class TossMtlsHttpClientFactory {
	private final TossApiProperties properties;
	private HttpClient httpClient;

	TossMtlsHttpClientFactory(TossApiProperties properties) {
		this.properties = properties;
	}

	synchronized HttpClient get() {
		if (httpClient == null) {
			httpClient = HttpClient.newBuilder()
					.connectTimeout(properties.normalizedConnectTimeout())
					.sslContext(sslContext())
					.build();
		}
		return httpClient;
	}

	private SSLContext sslContext() {
		try {
			Certificate[] certificates = certificates(requiredPem(
					properties.mtlsCertificatePem(),
					properties.mtlsCertificatePath(),
					"토스 mTLS 인증서가 필요해요."
			));
			PrivateKey privateKey = privateKey(requiredPem(
					properties.mtlsPrivateKeyPem(),
					properties.mtlsPrivateKeyPath(),
					"토스 mTLS 개인키가 필요해요."
			));

			KeyStore keyStore = KeyStore.getInstance("PKCS12");
			keyStore.load(null);
			keyStore.setKeyEntry("toss-client", privateKey, new char[0], certificates);

			KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
			keyManagerFactory.init(keyStore, new char[0]);

			SSLContext context = SSLContext.getInstance("TLS");
			context.init(keyManagerFactory.getKeyManagers(), null, null);
			return context;
		} catch (GeneralSecurityException | IOException exception) {
			throw new IllegalStateException("토스 mTLS 설정을 초기화하지 못했어요.", exception);
		}
	}

	private String requiredPem(String rawPem, String path, String message) throws IOException {
		if (rawPem != null && !rawPem.isBlank()) {
			return rawPem.replace("\\n", "\n");
		}
		if (path != null && !path.isBlank()) {
			return Files.readString(Path.of(path), StandardCharsets.UTF_8);
		}
		throw new IllegalStateException(message);
	}

	private Certificate[] certificates(String pem) throws GeneralSecurityException {
		CertificateFactory factory = CertificateFactory.getInstance("X.509");
		Collection<? extends Certificate> certificates = factory.generateCertificates(
				new ByteArrayInputStream(pem.getBytes(StandardCharsets.UTF_8))
		);
		return certificates.toArray(Certificate[]::new);
	}

	private PrivateKey privateKey(String pem) throws GeneralSecurityException {
		if (pem.contains("BEGIN RSA PRIVATE KEY")) {
			throw new IllegalStateException("토스 mTLS 개인키는 PKCS#8 형식(BEGIN PRIVATE KEY)으로 등록해야 해요.");
		}
		String normalized = pem
				.replace("-----BEGIN PRIVATE KEY-----", "")
				.replace("-----END PRIVATE KEY-----", "")
				.replaceAll("\\s", "");
		byte[] keyBytes = Base64.getDecoder().decode(normalized);
		PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
		try {
			return KeyFactory.getInstance("RSA").generatePrivate(keySpec);
		} catch (GeneralSecurityException rsaException) {
			return KeyFactory.getInstance("EC").generatePrivate(keySpec);
		}
	}

	Duration requestTimeout() {
		return properties.normalizedRequestTimeout();
	}
}
