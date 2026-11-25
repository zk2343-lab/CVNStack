package com.cvnstack.service;

import io.lettuce.core.SslOptions;
import java.io.File;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.redis.LettuceClientOptionsBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Trusts a private Redis server certificate when Memorystore uses TLS.
 *
 * <p>AI-assisted.
 */
@Configuration
@ConditionalOnProperty(name = "REDIS_CA_CERT")
public class RedisCertificateConfig {

  /**
   * Installs the PEM at {@code REDIS_CA_CERT} as the Redis trust anchor.
   *
   * @param certificatePath path to the server CA file
   * @return Lettuce customizer that uses that certificate
   */
  @Bean
  public LettuceClientOptionsBuilderCustomizer redisServerCertificate(
      @Value("${REDIS_CA_CERT}") final String certificatePath) {
    File certificate = new File(certificatePath);
    if (!certificate.isFile()) {
      throw new IllegalStateException("REDIS_CA_CERT is not a file: " + certificatePath);
    }
    return builder -> builder.sslOptions(
        SslOptions.builder().jdkSslProvider().trustManager(certificate).build());
  }
}
