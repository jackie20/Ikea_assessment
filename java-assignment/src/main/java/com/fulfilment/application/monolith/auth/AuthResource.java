package com.fulfilment.application.monolith.auth;

import jakarta.annotation.security.PermitAll;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.Map;

@Path("/auth")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthResource {

  @Inject UserService userService;
  @Inject JwtService jwtService;

  @POST
  @Path("/login")
  @PermitAll
  public Response login(LoginRequest request) {
    if (request == null || request.username == null || request.password == null) {
      return Response.status(400).entity(Map.of("error", "username and password are required")).build();
    }
    return userService.authenticate(request.username, request.password)
        .map(data -> {
          String token = jwtService.generate(request.username, data[1]);
          return Response.ok(Map.of(
              "token", token,
              "type", "Bearer",
              "expiresIn", JwtService.EXPIRES_IN_SECONDS,
              "username", request.username,
              "role", data[1]
          )).build();
        })
        .orElseGet(() ->
            Response.status(401).entity(Map.of("error", "Invalid username or password")).build()
        );
  }
}
