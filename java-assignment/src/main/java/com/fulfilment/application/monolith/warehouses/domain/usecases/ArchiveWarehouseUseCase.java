package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.ArchiveWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import org.jboss.logging.Logger;

@ApplicationScoped
public class ArchiveWarehouseUseCase implements ArchiveWarehouseOperation {

  private static final Logger LOG = Logger.getLogger(ArchiveWarehouseUseCase.class);

  private final WarehouseStore warehouseStore;

  public ArchiveWarehouseUseCase(WarehouseStore warehouseStore) {
    this.warehouseStore = warehouseStore;
  }

  @Override
  @Transactional
  public void archive(Warehouse warehouse) {
    LOG.infof("Archiving warehouse: %s", warehouse.businessUnitCode);
    warehouse.archivedAt = LocalDateTime.now();
    warehouseStore.update(warehouse);
    LOG.infof("Warehouse archived: %s at %s", warehouse.businessUnitCode, warehouse.archivedAt);
  }
}
