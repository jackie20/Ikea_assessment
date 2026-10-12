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
import org.jboss.logging.Logger;

@ApplicationScoped
public class ReplaceWarehouseUseCase implements ReplaceWarehouseOperation {

  private static final Logger LOG = Logger.getLogger(ReplaceWarehouseUseCase.class);

  private final WarehouseStore warehouseStore;
  private final LocationResolver locationResolver;

  public ReplaceWarehouseUseCase(WarehouseStore warehouseStore, LocationResolver locationResolver) {
    this.warehouseStore = warehouseStore;
    this.locationResolver = locationResolver;
  }

  @Override
  @Transactional
  public void replace(Warehouse newWarehouse) {
    LOG.infof("Replacing warehouse with business unit code: %s", newWarehouse.businessUnitCode);

    Warehouse existing = warehouseStore.findByBusinessUnitCode(newWarehouse.businessUnitCode);
    if (existing == null) {
      LOG.warnf("Rejected replace: warehouse not found: %s", newWarehouse.businessUnitCode);
      throw new WarehouseNotFoundException(newWarehouse.businessUnitCode);
    }

    if (!Objects.equals(newWarehouse.stock, existing.stock)) {
      LOG.warnf("Rejected replace: stock mismatch — expected %d, got %d for BU '%s'",
          existing.stock, newWarehouse.stock, newWarehouse.businessUnitCode);
      throw new WarehouseValidationException(
          "New warehouse stock must match existing stock of " + existing.stock);
    }

    if (newWarehouse.capacity < existing.stock) {
      LOG.warnf("Rejected replace: capacity %d cannot accommodate stock %d for BU '%s'",
          newWarehouse.capacity, existing.stock, newWarehouse.businessUnitCode);
      throw new WarehouseValidationException(
          "New warehouse capacity must accommodate existing stock of " + existing.stock);
    }

    Location location = locationResolver.resolveByIdentifier(newWarehouse.location);
    if (location == null) {
      LOG.warnf("Rejected replace: unknown location '%s'", newWarehouse.location);
      throw new WarehouseValidationException("Location not found: " + newWarehouse.location);
    }

    List<Warehouse> othersAtLocation =
        warehouseStore.getAll().stream()
            .filter(w -> w.location.equals(newWarehouse.location)
                && !w.businessUnitCode.equals(existing.businessUnitCode))
            .toList();

    int otherCapacity = othersAtLocation.stream().mapToInt(w -> w.capacity).sum();
    if (otherCapacity + newWarehouse.capacity > location.maxCapacity) {
      LOG.warnf("Rejected replace: capacity %d + %d exceeds location max %d for '%s'",
          otherCapacity, newWarehouse.capacity, location.maxCapacity, newWarehouse.location);
      throw new WarehouseValidationException(
          "Total capacity would exceed location maximum of " + location.maxCapacity);
    }

    existing.archivedAt = LocalDateTime.now();
    warehouseStore.update(existing);

    newWarehouse.createdAt = LocalDateTime.now();
    warehouseStore.create(newWarehouse);
    LOG.infof("Warehouse replaced: archived %s, created new at %s",
        existing.businessUnitCode, newWarehouse.location);
  }
}
