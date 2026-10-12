package com.fulfilment.application.monolith.stores;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.core.IsNot.not;

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
public class StoreResourceTest {

  private static final String PATH = "store";

  @Test
  @Order(1)
  public void listAll_returnsSeedStores() {
    given()
        .when()
        .get(PATH)
        .then()
        .statusCode(200)
        .body(containsString("TONSTAD"), containsString("KALLAX"), containsString("BESTÅ"));
  }

  @Test
  @Order(2)
  public void getById_existing_returns200() {
    given().when().get(PATH + "/2").then().statusCode(200).body(containsString("KALLAX"));
  }

  @Test
  @Order(3)
  public void getById_notFound_returns404() {
    given().when().get(PATH + "/9999").then().statusCode(404);
  }

  @Test
  @Order(4)
  public void create_validStore_returns201() {
    String body = """
        { "name": "NEWSTORE", "quantityProductsInStock": 5 }
        """;

    given()
        .contentType(ContentType.JSON)
        .body(body)
        .when()
        .post(PATH)
        .then()
        .statusCode(201)
        .body(containsString("NEWSTORE"));
  }

  @Test
  @Order(5)
  public void create_withIdSet_returns422() {
    String body = """
        { "id": 999, "name": "BAD", "quantityProductsInStock": 0 }
        """;

    given().contentType(ContentType.JSON).body(body).when().post(PATH).then().statusCode(422);
  }

  @Test
  @Order(6)
  public void update_existingStore_returns200() {
    String body = """
        { "name": "KALLAX-UPDATED", "quantityProductsInStock": 99 }
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

  @Test
  @Order(7)
  public void update_withoutName_returns422() {
    String body = """
        { "quantityProductsInStock": 1 }
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
  @Order(8)
  public void patch_existingStore_returns200() {
    String body = """
        { "name": "KALLAX-PATCHED", "quantityProductsInStock": 7 }
        """;

    given()
        .contentType(ContentType.JSON)
        .body(body)
        .when()
        .patch(PATH + "/2")
        .then()
        .statusCode(200)
        .body(containsString("KALLAX-PATCHED"));
  }

  @Test
  @Order(9)
  public void delete_existingStore_returns204AndDisappearsFromList() {
    given().when().delete(PATH + "/3").then().statusCode(204);

    given()
        .when()
        .get(PATH)
        .then()
        .statusCode(200)
        .body(not(containsString("BESTÅ")));
  }

  @Test
  @Order(10)
  public void delete_notFound_returns404() {
    given().when().delete(PATH + "/9999").then().statusCode(404);
  }

  @Test
  @Order(11)
  public void update_notFound_returns404() {
    String body = """
        { "name": "GHOST-STORE", "quantityProductsInStock": 1 }
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
  @Order(12)
  public void patch_withoutName_returns422() {
    String body = """
        { "quantityProductsInStock": 5 }
        """;
    given()
        .contentType(ContentType.JSON)
        .body(body)
        .when()
        .patch(PATH + "/2")
        .then()
        .statusCode(422);
  }

  @Test
  @Order(13)
  public void patch_notFound_returns404() {
    String body = """
        { "name": "GHOST", "quantityProductsInStock": 1 }
        """;
    given()
        .contentType(ContentType.JSON)
        .body(body)
        .when()
        .patch(PATH + "/9999")
        .then()
        .statusCode(404);
  }
}
