package com.fulfilment.application.monolith.auth;

import io.smallrye.jwt.build.Jwt;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.Duration;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class JwtService {

  @ConfigProperty(name = "mp.jwt.verify.issuer")
  String issuer;

  static final long EXPIRES_IN_SECONDS = 3600L;

  public String generate(String username, String role) {
    return Jwt.issuer(issuer)
        .upn(username)
        .groups(role)
        .expiresIn(Duration.ofSeconds(EXPIRES_IN_SECONDS))
        .sign();
  }
}
