package com.fulfilment.application.monolith.warehouses.adapters.restapi;

import com.fulfilment.application.monolith.warehouses.adapters.database.WarehouseRepository;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseNotFoundException;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseValidationException;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.ArchiveWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.CreateWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.ReplaceWarehouseOperation;
import com.warehouse.api.WarehouseResource;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.util.List;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.ResponseStatus;

@RequestScoped
public class WarehouseResourceImpl implements WarehouseResource {

  private static final Logger LOG = Logger.getLogger(WarehouseResourceImpl.class);

  @Inject private WarehouseRepository warehouseRepository;
  @Inject private CreateWarehouseOperation createWarehouseOperation;
  @Inject private ArchiveWarehouseOperation archiveWarehouseOperation;
  @Inject private ReplaceWarehouseOperation replaceWarehouseOperation;

  @Override
  public List<com.warehouse.api.beans.Warehouse> listAllWarehousesUnits() {
    return warehouseRepository.getAll().stream().map(this::toApiWarehouse).toList();
  }

  @Override
  @ResponseStatus(201)
  public com.warehouse.api.beans.Warehouse createANewWarehouseUnit(
      @NotNull com.warehouse.api.beans.Warehouse data) {
    LOG.infof("POST /warehouse — creating BU '%s' at '%s'",
        data.getBusinessUnitCode(), data.getLocation());
    try {
      Warehouse domain = toDomainWarehouse(data);
      createWarehouseOperation.create(domain);
      return toApiWarehouse(domain);
    } catch (WarehouseValidationException e) {
      LOG.warnf("Warehouse creation rejected: %s", e.getMessage());
      throw new WebApplicationException(e.getMessage(), Response.Status.BAD_REQUEST);
    }
  }

  @Override
  public com.warehouse.api.beans.Warehouse getAWarehouseUnitByID(String id) {
    LOG.debugf("GET /warehouse/%s", id);
    Warehouse warehouse = warehouseRepository.findByBusinessUnitCode(id);
    if (warehouse == null) {
      LOG.warnf("Warehouse not found: %s", id);
      throw new WebApplicationException("Warehouse not found: " + id, Response.Status.NOT_FOUND);
    }
    return toApiWarehouse(warehouse);
  }

  @Override
  public void archiveAWarehouseUnitByID(String id) {
    LOG.infof("DELETE /warehouse/%s — archiving", id);
    Warehouse warehouse = warehouseRepository.findByBusinessUnitCode(id);
    if (warehouse == null) {
      LOG.warnf("Archive rejected: warehouse not found: %s", id);
      throw new WebApplicationException("Warehouse not found: " + id, Response.Status.NOT_FOUND);
    }
    archiveWarehouseOperation.archive(warehouse);
  }

  @Override
  public com.warehouse.api.beans.Warehouse replaceTheCurrentActiveWarehouse(
      String businessUnitCode, @NotNull com.warehouse.api.beans.Warehouse data) {
    LOG.infof("POST /warehouse/%s/replacement", businessUnitCode);
    try {
      Warehouse newWarehouse = toDomainWarehouse(data);
      newWarehouse.businessUnitCode = businessUnitCode;
      replaceWarehouseOperation.replace(newWarehouse);
      return toApiWarehouse(newWarehouse);
    } catch (WarehouseNotFoundException e) {
      LOG.warnf("Replace rejected: %s", e.getMessage());
      throw new WebApplicationException(e.getMessage(), Response.Status.NOT_FOUND);
    } catch (WarehouseValidationException e) {
      LOG.warnf("Replace rejected: %s", e.getMessage());
      throw new WebApplicationException(e.getMessage(), Response.Status.BAD_REQUEST);
    }
  }

  private Warehouse toDomainWarehouse(com.warehouse.api.beans.Warehouse api) {
    var domain = new Warehouse();
    domain.businessUnitCode = api.getBusinessUnitCode();
    domain.location = api.getLocation();
    domain.capacity = api.getCapacity();
    domain.stock = api.getStock();
    return domain;
  }

  private com.warehouse.api.beans.Warehouse toApiWarehouse(Warehouse domain) {
    var api = new com.warehouse.api.beans.Warehouse();
    api.setBusinessUnitCode(domain.businessUnitCode);
    api.setLocation(domain.location);
    api.setCapacity(domain.capacity);
    api.setStock(domain.stock);
    return api;
  }
}
