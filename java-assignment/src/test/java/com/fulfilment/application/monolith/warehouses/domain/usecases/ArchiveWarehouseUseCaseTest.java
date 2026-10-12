package com.fulfilment.application.monolith.warehouses.domain.usecases;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.verify;

import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ArchiveWarehouseUseCaseTest {

  @Mock private WarehouseStore warehouseStore;

  private ArchiveWarehouseUseCase useCase;

  @BeforeEach
  void setUp() {
    useCase = new ArchiveWarehouseUseCase(warehouseStore);
  }

  @Test
  void archive_setsArchivedAtAndDelegatesToStore() {
    Warehouse warehouse = new Warehouse();
    warehouse.businessUnitCode = "MWH.001";
    warehouse.location = "ZWOLLE-001";
    warehouse.capacity = 40;
    warehouse.stock = 10;

    useCase.archive(warehouse);

    ArgumentCaptor<Warehouse> captor = forClass(Warehouse.class);
    verify(warehouseStore).update(captor.capture());

    assertNotNull(captor.getValue().archivedAt, "archivedAt must be set after archiving");
  }

  @Test
  void archive_doesNotAlterOtherFields() {
    Warehouse warehouse = new Warehouse();
    warehouse.businessUnitCode = "MWH.002";
    warehouse.location = "AMSTERDAM-001";
    warehouse.capacity = 50;
    warehouse.stock = 20;

    useCase.archive(warehouse);

    ArgumentCaptor<Warehouse> captor = forClass(Warehouse.class);
    verify(warehouseStore).update(captor.capture());

    Warehouse captured = captor.getValue();
    assert "MWH.002".equals(captured.businessUnitCode);
    assert "AMSTERDAM-001".equals(captured.location);
    assert captured.capacity == 50;
    assert captured.stock == 20;
  }
}
