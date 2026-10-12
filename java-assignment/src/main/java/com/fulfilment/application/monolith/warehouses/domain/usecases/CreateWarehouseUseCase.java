package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseValidationException;
import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.CreateWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import org.jboss.logging.Logger;

@ApplicationScoped
public class CreateWarehouseUseCase implements CreateWarehouseOperation {

  private static final Logger LOG = Logger.getLogger(CreateWarehouseUseCase.class);

  private final WarehouseStore warehouseStore;
  private final LocationResolver locationResolver;

  public CreateWarehouseUseCase(WarehouseStore warehouseStore, LocationResolver locationResolver) {
    this.warehouseStore = warehouseStore;
    this.locationResolver = locationResolver;
  }

  @Override
  @Transactional
  public void create(Warehouse warehouse) {
    LOG.infof("Creating warehouse with business unit code: %s", warehouse.businessUnitCode);

    if (warehouseStore.findByBusinessUnitCode(warehouse.businessUnitCode) != null) {
      LOG.warnf("Rejected create: business unit code already exists: %s",
          warehouse.businessUnitCode);
      throw new WarehouseValidationException(
          "Business unit code already exists: " + warehouse.businessUnitCode);
    }

    Location location = locationResolver.resolveByIdentifier(warehouse.location);
    if (location == null) {
      LOG.warnf("Rejected create: unknown location '%s'", warehouse.location);
      throw new WarehouseValidationException("Location not found: " + warehouse.location);
    }

    List<Warehouse> activeAtLocation =
        warehouseStore.getAll().stream()
            .filter(w -> w.location.equals(warehouse.location))
            .toList();

    if (activeAtLocation.size() >= location.maxNumberOfWarehouses) {
      LOG.warnf("Rejected create: max warehouses (%d) reached for location '%s'",
          location.maxNumberOfWarehouses, warehouse.location);
      throw new WarehouseValidationException(
          "Maximum number of warehouses reached for location: " + warehouse.location);
    }

    int existingCapacity = activeAtLocation.stream().mapToInt(w -> w.capacity).sum();
    if (existingCapacity + warehouse.capacity > location.maxCapacity) {
      LOG.warnf("Rejected create: capacity %d + %d exceeds location max %d for '%s'",
          existingCapacity, warehouse.capacity, location.maxCapacity, warehouse.location);
      throw new WarehouseValidationException(
          "Total capacity would exceed location maximum of " + location.maxCapacity);
    }

    if (warehouse.stock != null && warehouse.capacity != null
        && warehouse.stock > warehouse.capacity) {
      LOG.warnf("Rejected create: stock %d exceeds capacity %d for BU '%s'",
          warehouse.stock, warehouse.capacity, warehouse.businessUnitCode);
      throw new WarehouseValidationException("Stock cannot exceed warehouse capacity");
    }

    warehouse.createdAt = LocalDateTime.now();
    warehouseStore.create(warehouse);
    LOG.infof("Warehouse created successfully: %s at %s", warehouse.businessUnitCode,
        warehouse.location);
  }
}
