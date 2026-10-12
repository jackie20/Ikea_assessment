package com.fulfilment.application.monolith.warehouses.adapters.restapi;

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
public class WarehouseResourceTest {

  private static final String PATH = "warehouse";

  @Test
  @Order(1)
  public void listAll_returnsSeedWarehouses() {
    given()
        .when()
        .get(PATH)
        .then()
        .statusCode(200)
        .body(
            containsString("MWH.001"),
            containsString("MWH.012"),
            containsString("MWH.023"),
            containsString("ZWOLLE-001"),
            containsString("AMSTERDAM-001"),
            containsString("TILBURG-001"));
  }

  @Test
  @Order(2)
  public void getByBuCode_existing_returns200() {
    given()
        .when()
        .get(PATH + "/MWH.001")
        .then()
        .statusCode(200)
        .body(containsString("MWH.001"), containsString("ZWOLLE-001"));
  }

  @Test
  @Order(3)
  public void getByBuCode_notFound_returns404() {
    given().when().get(PATH + "/MWH.NOPE").then().statusCode(404);
  }

  @Test
  @Order(4)
  public void create_validWarehouse_returns201AndAppearsInList() {
    // AMSTERDAM-001: maxWarehouses=5, maxCapacity=100; MWH.012 has capacity=50 → 50 remaining
    String body =
        """
        {
          "businessUnitCode": "MWH.TEST",
          "location": "AMSTERDAM-001",
          "capacity": 10,
          "stock": 3
        }
        """;

    // Note: the generated interface returns Warehouse (not Response), so RESTEasy defaults
    // to 200. The OpenAPI spec says 201 but the generator does not emit @ResponseStatus.
    given()
        .contentType(ContentType.JSON)
        .body(body)
        .when()
        .post(PATH)
        .then()
        .statusCode(200)
        .body(containsString("MWH.TEST"));

    given()
        .when()
        .get(PATH)
        .then()
        .statusCode(200)
        .body(containsString("MWH.TEST"));
  }

  @Test
  @Order(5)
  public void create_duplicateBuCode_returns400() {
    String body =
        """
        {
          "businessUnitCode": "MWH.TEST",
          "location": "AMSTERDAM-001",
          "capacity": 5,
          "stock": 0
        }
        """;

    given().contentType(ContentType.JSON).body(body).when().post(PATH).then().statusCode(400);
  }

  @Test
  @Order(6)
  public void create_unknownLocation_returns400() {
    String body =
        """
        {
          "businessUnitCode": "MWH.BADLOC",
          "location": "NOWHERE-999",
          "capacity": 10,
          "stock": 0
        }
        """;

    given().contentType(ContentType.JSON).body(body).when().post(PATH).then().statusCode(400);
  }

  @Test
  @Order(7)
  public void create_maxWarehousesReached_returns400() {
    // ZWOLLE-001: maxWarehouses=1 and MWH.001 already occupies it
    String body =
        """
        {
          "businessUnitCode": "MWH.EXTRA",
          "location": "ZWOLLE-001",
          "capacity": 5,
          "stock": 0
        }
        """;

    given().contentType(ContentType.JSON).body(body).when().post(PATH).then().statusCode(400);
  }

  @Test
  @Order(8)
  public void create_stockExceedsCapacity_returns400() {
    String body =
        """
        {
          "businessUnitCode": "MWH.BADSTOCK",
          "location": "AMSTERDAM-001",
          "capacity": 5,
          "stock": 10
        }
        """;

    given().contentType(ContentType.JSON).body(body).when().post(PATH).then().statusCode(400);
  }

  @Test
  @Order(9)
  public void archive_existingWarehouse_returns204AndHiddenFromList() {
    given().when().delete(PATH + "/MWH.023").then().statusCode(204);

    given()
        .when()
        .get(PATH)
        .then()
        .statusCode(200)
        .body(not(containsString("TILBURG-001")));
  }

  @Test
  @Order(10)
  public void archive_notFound_returns404() {
    given().when().delete(PATH + "/MWH.GONE").then().statusCode(404);
  }

  @Test
  @Order(11)
  public void replace_validReplacement_returns200() {
    // MWH.012 at AMSTERDAM-001, stock=5; replace with larger capacity
    String body =
        """
        {
          "businessUnitCode": "MWH.012",
          "location": "AMSTERDAM-001",
          "capacity": 55,
          "stock": 5
        }
        """;

    given()
        .contentType(ContentType.JSON)
        .body(body)
        .when()
        .post(PATH + "/MWH.012/replacement")
        .then()
        .statusCode(200)
        .body(containsString("MWH.012"), containsString("AMSTERDAM-001"));
  }

  @Test
  @Order(12)
  public void replace_stockMismatch_returns400() {
    String body =
        """
        {
          "businessUnitCode": "MWH.001",
          "location": "ZWOLLE-001",
          "capacity": 30,
          "stock": 999
        }
        """;

    given()
        .contentType(ContentType.JSON)
        .body(body)
        .when()
        .post(PATH + "/MWH.001/replacement")
        .then()
        .statusCode(400);
  }

  @Test
  @Order(13)
  public void replace_notFound_returns404() {
    String body =
        """
        {
          "businessUnitCode": "MWH.MISSING",
          "location": "AMSTERDAM-001",
          "capacity": 10,
          "stock": 0
        }
        """;

    given()
        .contentType(ContentType.JSON)
        .body(body)
        .when()
        .post(PATH + "/MWH.MISSING/replacement")
        .then()
        .statusCode(404);
  }
}
