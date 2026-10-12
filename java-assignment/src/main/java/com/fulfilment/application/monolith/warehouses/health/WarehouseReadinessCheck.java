package com.fulfilment.application.monolith.warehouses.health;

import com.fulfilment.application.monolith.warehouses.adapters.database.WarehouseRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;

@Readiness
@ApplicationScoped
public class WarehouseReadinessCheck implements HealthCheck {

  @Inject WarehouseRepository warehouseRepository;

  @Override
  public HealthCheckResponse call() {
    try {
      long count = warehouseRepository.count();
      return HealthCheckResponse.named("warehouse-store")
          .up()
          .withData("active-warehouses", count)
          .build();
    } catch (Exception e) {
      return HealthCheckResponse.named("warehouse-store")
          .down()
          .withData("error", e.getMessage())
          .build();
    }
  }
}
