package com.fulfilment.application.monolith.warehouses.domain.usecases;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseNotFoundException;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseValidationException;
import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReplaceWarehouseUseCaseTest {

  @Mock private WarehouseStore warehouseStore;
  @Mock private LocationResolver locationResolver;

  private ReplaceWarehouseUseCase useCase;

  @BeforeEach
  void setUp() {
    useCase = new ReplaceWarehouseUseCase(warehouseStore, locationResolver);
  }

  // --- positive ---

  @Test
  void replace_validReplacement_archivesOldAndCreatesNew() {
    Warehouse existing = warehouse("MWH.001", "ZWOLLE-001", 30, 10);
    Warehouse newWarehouse = warehouse("MWH.001", "ZWOLLE-001", 35, 10);

    when(warehouseStore.findByBusinessUnitCode("MWH.001")).thenReturn(existing);
    when(locationResolver.resolveByIdentifier("ZWOLLE-001"))
        .thenReturn(new Location("ZWOLLE-001", 1, 40));
    when(warehouseStore.getAll()).thenReturn(Collections.emptyList());

    useCase.replace(newWarehouse);

    verify(warehouseStore).update(existing); // archive
    verify(warehouseStore).create(newWarehouse); // new
  }

  // --- negative ---

  @Test
  void replace_warehouseNotFound_throwsNotFoundException() {
    when(warehouseStore.findByBusinessUnitCode("MWH.MISSING")).thenReturn(null);

    Warehouse newWarehouse = warehouse("MWH.MISSING", "ZWOLLE-001", 30, 10);

    assertThrows(WarehouseNotFoundException.class, () -> useCase.replace(newWarehouse));
    verify(warehouseStore, never()).update(any());
    verify(warehouseStore, never()).create(any());
  }

  @Test
  void replace_stockMismatch_throwsValidationException() {
    Warehouse existing = warehouse("MWH.001", "ZWOLLE-001", 30, 10);
    when(warehouseStore.findByBusinessUnitCode("MWH.001")).thenReturn(existing);

    Warehouse newWarehouse = warehouse("MWH.001", "ZWOLLE-001", 30, 5); // stock 5 != 10

    assertThrows(WarehouseValidationException.class, () -> useCase.replace(newWarehouse));
    verify(warehouseStore, never()).create(any());
  }

  @Test
  void replace_newCapacityBelowExistingStock_throwsValidationException() {
    Warehouse existing = warehouse("MWH.001", "ZWOLLE-001", 30, 20);
    when(warehouseStore.findByBusinessUnitCode("MWH.001")).thenReturn(existing);

    Warehouse newWarehouse = warehouse("MWH.001", "ZWOLLE-001", 15, 20); // capacity 15 < stock 20

    assertThrows(WarehouseValidationException.class, () -> useCase.replace(newWarehouse));
    verify(warehouseStore, never()).create(any());
  }

  @Test
  void replace_invalidLocation_throwsValidationException() {
    Warehouse existing = warehouse("MWH.001", "ZWOLLE-001", 30, 10);
    when(warehouseStore.findByBusinessUnitCode("MWH.001")).thenReturn(existing);
    when(locationResolver.resolveByIdentifier("UNKNOWN")).thenReturn(null);

    Warehouse newWarehouse = warehouse("MWH.001", "UNKNOWN", 30, 10);

    assertThrows(WarehouseValidationException.class, () -> useCase.replace(newWarehouse));
    verify(warehouseStore, never()).create(any());
  }

  @Test
  void replace_newCapacityExceedsLocationMax_throwsValidationException() {
    Warehouse existing = warehouse("MWH.001", "ZWOLLE-001", 30, 10);
    when(warehouseStore.findByBusinessUnitCode("MWH.001")).thenReturn(existing);
    when(locationResolver.resolveByIdentifier("ZWOLLE-001"))
        .thenReturn(new Location("ZWOLLE-001", 1, 40));
    when(warehouseStore.getAll()).thenReturn(Collections.emptyList());

    Warehouse newWarehouse = warehouse("MWH.001", "ZWOLLE-001", 50, 10); // 50 > 40

    assertThrows(WarehouseValidationException.class, () -> useCase.replace(newWarehouse));
    verify(warehouseStore, never()).create(any());
  }

  // --- helper ---

  private static Warehouse warehouse(String buCode, String location, int capacity, int stock) {
    var w = new Warehouse();
    w.businessUnitCode = buCode;
    w.location = location;
    w.capacity = capacity;
    w.stock = stock;
    return w;
  }
}
