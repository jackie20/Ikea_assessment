package com.fulfilment.application.monolith.products;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@QuarkusTest
@TestSecurity(user = "admin", roles = {"admin", "user"})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ProductResourceTest {

  private static final String PATH = "product";

  // --- getSingle ---

  @Test
  @Order(1)
  public void getSingle_notFound_returns404() {
    given().when().get(PATH + "/9999").then().statusCode(404);
  }

  // --- create ---

  @Test
  @Order(2)
  public void create_withIdAlreadySet_returns422() {
    String body = """
        { "id": 100, "name": "SHOULD-FAIL", "stock": 0 }
        """;
    given().contentType(ContentType.JSON).body(body).when().post(PATH).then().statusCode(422);
  }

  @Test
  @Order(3)
  public void create_validProduct_returns201() {
    String body = """
        { "name": "TEST-PRODUCT", "stock": 3 }
        """;
    given()
        .contentType(ContentType.JSON)
        .body(body)
        .when()
        .post(PATH)
        .then()
        .statusCode(201)
        .body(containsString("TEST-PRODUCT"));
  }

  // --- update ---

  @Test
  @Order(4)
  public void update_withoutName_returns422() {
    String body = """
        { "stock": 5 }
        """;
    given()
        .contentType(ContentType.JSON)
        .body(body)
        .when()
        .put(PATH + "/2")
        .then()
        .statusCode(422);
  }

  @Test
  @Order(5)
  public void update_notFound_returns404() {
    String body = """
        { "name": "UPDATED", "stock": 1 }
        """;
    given()
        .contentType(ContentType.JSON)
        .body(body)
        .when()
        .put(PATH + "/9999")
        .then()
        .statusCode(404);
  }

  @Test
  @Order(6)
  public void update_validProduct_returns200() {
    String body = """
        { "name": "KALLAX-UPDATED", "stock": 10 }
        """;
    given()
        .contentType(ContentType.JSON)
        .body(body)
        .when()
        .put(PATH + "/2")
        .then()
        .statusCode(200)
        .body(containsString("KALLAX-UPDATED"));
  }

  // --- delete ---

  @Test
  @Order(7)
  public void delete_notFound_returns404() {
    given().when().delete(PATH + "/9999").then().statusCode(404);
  }
}
