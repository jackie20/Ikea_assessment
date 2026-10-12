package com.fulfilment.application.monolith.warehouses.adapters.restapi;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.core.IsNot.not;

import io.quarkus.test.junit.QuarkusIntegrationTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@QuarkusIntegrationTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class WarehouseEndpointIT {

  private static final String PATH = "warehouse";

  @Test
  @Order(1)
  public void listAll_returnsAllSeedWarehouses() {
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
  public void getByBusinessUnitCode_existingWarehouse_returns200() {
    given()
        .when()
        .get(PATH + "/MWH.001")
        .then()
        .statusCode(200)
        .body(containsString("MWH.001"), containsString("ZWOLLE-001"));
  }

  @Test
  @Order(3)
  public void getByBusinessUnitCode_missingWarehouse_returns404() {
    given().when().get(PATH + "/MWH.NOPE").then().statusCode(404);
  }

  @Test
  @Order(4)
  public void create_validWarehouse_returns201() {
    String body =
        """
        {
          "businessUnitCode": "MWH.NEW",
          "location": "AMSTERDAM-001",
          "capacity": 10,
          "stock": 5
        }
        """;

    given()
        .contentType(ContentType.JSON)
        .body(body)
        .when()
        .post(PATH)
        .then()
        .statusCode(201)
        .body(containsString("MWH.NEW"));
  }

  @Test
  @Order(5)
  public void create_duplicateBusinessUnitCode_returns400() {
    String body =
        """
        {
          "businessUnitCode": "MWH.NEW",
          "location": "AMSTERDAM-001",
          "capacity": 10,
          "stock": 5
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
  public void archive_existingWarehouse_returns204AndDisappearsFromList() {
    given().when().delete(PATH + "/MWH.023").then().statusCode(204);

    given()
        .when()
        .get(PATH)
        .then()
        .statusCode(200)
        .body(not(containsString("TILBURG-001")));
  }

  @Test
  @Order(8)
  public void archive_missingWarehouse_returns404() {
    given().when().delete(PATH + "/MWH.GONE").then().statusCode(404);
  }

  @Test
  @Order(9)
  public void replace_validReplacement_archivedOldCreatesNew() {
    // MWH.012 is at AMSTERDAM-001, stock=5
    String body =
        """
        {
          "businessUnitCode": "MWH.012",
          "location": "AMSTERDAM-001",
          "capacity": 60,
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
  @Order(10)
  public void replace_stockMismatch_returns400() {
    // MWH.001 has stock=10; sending stock=99 must fail
    String body =
        """
        {
          "businessUnitCode": "MWH.001",
          "location": "ZWOLLE-001",
          "capacity": 40,
          "stock": 99
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
}
