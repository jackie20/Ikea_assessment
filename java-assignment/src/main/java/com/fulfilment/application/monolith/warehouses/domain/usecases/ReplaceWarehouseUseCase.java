package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseNotFoundException;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseValidationException;
import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import com.fulfilment.application.monolith.warehouses.domain.ports.ReplaceWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@ApplicationScoped
public class ReplaceWarehouseUseCase implements ReplaceWarehouseOperation {

  private final WarehouseStore warehouseStore;
  private final LocationResolver locationResolver;

  public ReplaceWarehouseUseCase(WarehouseStore warehouseStore, LocationResolver locationResolver) {
    this.warehouseStore = warehouseStore;
    this.locationResolver = locationResolver;
  }

  @Override
  @Transactional
  public void replace(Warehouse newWarehouse) {
    Warehouse existing =
        warehouseStore.findByBusinessUnitCode(newWarehouse.businessUnitCode);
    if (existing == null) {
      throw new WarehouseNotFoundException(newWarehouse.businessUnitCode);
    }

    // New warehouse must carry exactly the same stock as the one being replaced
    if (!Objects.equals(newWarehouse.stock, existing.stock)) {
      throw new WarehouseValidationException(
          "New warehouse stock must match existing stock of " + existing.stock);
    }

    // New warehouse capacity must be able to hold the existing stock
    if (newWarehouse.capacity < existing.stock) {
      throw new WarehouseValidationException(
          "New warehouse capacity must accommodate existing stock of " + existing.stock);
    }

    Location location = locationResolver.resolveByIdentifier(newWarehouse.location);
    if (location == null) {
      throw new WarehouseValidationException("Location not found: " + newWarehouse.location);
    }

    // Capacity check: exclude the warehouse being replaced from the total
    List<Warehouse> othersAtLocation =
        warehouseStore.getAll().stream()
            .filter(
                w ->
                    w.location.equals(newWarehouse.location)
                        && !w.businessUnitCode.equals(existing.businessUnitCode))
            .toList();

    int otherCapacity = othersAtLocation.stream().mapToInt(w -> w.capacity).sum();
    if (otherCapacity + newWarehouse.capacity > location.maxCapacity) {
      throw new WarehouseValidationException(
          "Total capacity would exceed location maximum of " + location.maxCapacity);
    }

    // Archive the existing warehouse, then create the replacement under the same BU code
    existing.archivedAt = LocalDateTime.now();
    warehouseStore.update(existing);

    newWarehouse.createdAt = LocalDateTime.now();
    warehouseStore.create(newWarehouse);
  }
}
