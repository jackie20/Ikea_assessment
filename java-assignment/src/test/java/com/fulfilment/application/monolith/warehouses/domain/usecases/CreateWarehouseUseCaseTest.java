package com.fulfilment.application.monolith.warehouses.domain.usecases;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseValidationException;
import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CreateWarehouseUseCaseTest {

  @Mock private WarehouseStore warehouseStore;
  @Mock private LocationResolver locationResolver;

  private CreateWarehouseUseCase useCase;

  @BeforeEach
  void setUp() {
    useCase = new CreateWarehouseUseCase(warehouseStore, locationResolver);
  }

  // --- positive ---

  @Test
  void create_withValidWarehouse_persistsSuccessfully() {
    Warehouse warehouse = warehouse("MWH.NEW", "AMSTERDAM-001", 30, 10);
    when(warehouseStore.findByBusinessUnitCode("MWH.NEW")).thenReturn(null);
    when(locationResolver.resolveByIdentifier("AMSTERDAM-001"))
        .thenReturn(new Location("AMSTERDAM-001", 5, 100));
    when(warehouseStore.getAll()).thenReturn(Collections.emptyList());

    useCase.create(warehouse);

    verify(warehouseStore).create(warehouse);
  }

  @Test
  void create_atLocationWithExistingWarehouses_respectsRemainingCapacity() {
    Warehouse existing = warehouse("MWH.001", "AMSTERDAM-001", 30, 5);
    Warehouse newWarehouse = warehouse("MWH.NEW", "AMSTERDAM-001", 20, 0);

    when(warehouseStore.findByBusinessUnitCode("MWH.NEW")).thenReturn(null);
    when(locationResolver.resolveByIdentifier("AMSTERDAM-001"))
        .thenReturn(new Location("AMSTERDAM-001", 5, 100));
    when(warehouseStore.getAll()).thenReturn(List.of(existing));

    useCase.create(newWarehouse);

    verify(warehouseStore).create(newWarehouse);
  }

  // --- negative / validation ---

  @Test
  void create_duplicateBusinessUnitCode_throwsValidationException() {
    Warehouse existing = warehouse("MWH.001", "AMSTERDAM-001", 50, 10);
    when(warehouseStore.findByBusinessUnitCode("MWH.001")).thenReturn(existing);

    Warehouse duplicate = warehouse("MWH.001", "AMSTERDAM-001", 30, 0);

    assertThrows(WarehouseValidationException.class, () -> useCase.create(duplicate));
    verify(warehouseStore, never()).create(any());
  }

  @Test
  void create_unknownLocation_throwsValidationException() {
    when(warehouseStore.findByBusinessUnitCode("MWH.NEW")).thenReturn(null);
    when(locationResolver.resolveByIdentifier("UNKNOWN-001")).thenReturn(null);

    Warehouse warehouse = warehouse("MWH.NEW", "UNKNOWN-001", 20, 0);

    assertThrows(WarehouseValidationException.class, () -> useCase.create(warehouse));
    verify(warehouseStore, never()).create(any());
  }

  @Test
  void create_locationAtMaxWarehouses_throwsValidationException() {
    Warehouse existing = warehouse("MWH.001", "ZWOLLE-001", 30, 5);
    when(warehouseStore.findByBusinessUnitCode("MWH.NEW")).thenReturn(null);
    when(locationResolver.resolveByIdentifier("ZWOLLE-001"))
        .thenReturn(new Location("ZWOLLE-001", 1, 40)); // max 1 warehouse
    when(warehouseStore.getAll()).thenReturn(List.of(existing));

    Warehouse newWarehouse = warehouse("MWH.NEW", "ZWOLLE-001", 10, 0);

    assertThrows(WarehouseValidationException.class, () -> useCase.create(newWarehouse));
    verify(warehouseStore, never()).create(any());
  }

  @Test
  void create_exceedsLocationMaxCapacity_throwsValidationException() {
    Warehouse existing = warehouse("MWH.001", "AMSTERDAM-001", 80, 5);
    when(warehouseStore.findByBusinessUnitCode("MWH.NEW")).thenReturn(null);
    when(locationResolver.resolveByIdentifier("AMSTERDAM-001"))
        .thenReturn(new Location("AMSTERDAM-001", 5, 100));
    when(warehouseStore.getAll()).thenReturn(List.of(existing));

    Warehouse newWarehouse = warehouse("MWH.NEW", "AMSTERDAM-001", 30, 0); // 80+30 > 100

    assertThrows(WarehouseValidationException.class, () -> useCase.create(newWarehouse));
    verify(warehouseStore, never()).create(any());
  }

  @Test
  void create_stockExceedsCapacity_throwsValidationException() {
    when(warehouseStore.findByBusinessUnitCode("MWH.NEW")).thenReturn(null);
    when(locationResolver.resolveByIdentifier("AMSTERDAM-001"))
        .thenReturn(new Location("AMSTERDAM-001", 5, 100));
    when(warehouseStore.getAll()).thenReturn(Collections.emptyList());

    Warehouse warehouse = warehouse("MWH.NEW", "AMSTERDAM-001", 10, 20); // stock > capacity

    assertThrows(WarehouseValidationException.class, () -> useCase.create(warehouse));
    verify(warehouseStore, never()).create(any());
  }

  // --- helpers ---

  private static Warehouse warehouse(String buCode, String location, int capacity, int stock) {
    var w = new Warehouse();
    w.businessUnitCode = buCode;
    w.location = location;
    w.capacity = capacity;
    w.stock = stock;
    return w;
  }
}
