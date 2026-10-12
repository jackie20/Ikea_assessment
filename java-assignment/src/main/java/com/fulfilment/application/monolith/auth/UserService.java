package com.fulfilment.application.monolith.auth;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.Map;
import java.util.Optional;

@ApplicationScoped
public class UserService {

  private static final Map<String, String[]> USERS = Map.of(
      "admin", new String[]{"admin123", "admin"},
      "user",  new String[]{"user123",  "user"}
  );

  /** Returns [password, role] if the username exists and password matches, else empty. */
  public Optional<String[]> authenticate(String username, String password) {
    String[] data = USERS.get(username);
    if (data != null && data[0].equals(password)) {
      return Optional.of(data);
    }
    return Optional.empty();
  }
}
