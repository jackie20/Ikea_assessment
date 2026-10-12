package com.fulfilment.application.monolith.warehouses.adapters.restapi;

import com.fulfilment.application.monolith.warehouses.adapters.database.WarehouseRepository;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseNotFoundException;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseValidationException;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.ArchiveWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.CreateWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.ReplaceWarehouseOperation;
import com.warehouse.api.WarehouseResource;
import com.warehouse.api.beans.WarehousePage;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import java.math.BigInteger;
import jakarta.ws.rs.core.MediaType;
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
  public WarehousePage listAllWarehousesUnits(
      @QueryParam("page") @DefaultValue("0") BigInteger page,
      @QueryParam("size") @DefaultValue("20") BigInteger size) {
    int p = page == null ? 0 : page.intValue();
    int s = (size == null || size.intValue() < 1) ? 20 : size.intValue();
    List<com.warehouse.api.beans.Warehouse> content =
        warehouseRepository.getPage(p, s).stream().map(this::toApiWarehouse).toList();
    long total = warehouseRepository.countActive();
    WarehousePage wp = new WarehousePage();
    wp.setContent(content);
    wp.setPage(p);
    wp.setSize(s);
    wp.setTotalElements(total);
    wp.setTotalPages(s > 0 ? (int) Math.ceil((double) total / s) : 0);
    return wp;
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

  @GET
  @Path("export")
  @Produces("text/csv")
  public Response exportToCsv() {
    LOG.infof("GET /warehouse/export");
    StringBuilder csv = new StringBuilder("businessUnitCode,location,capacity,stock\n");
    for (Warehouse w : warehouseRepository.getAll()) {
      csv.append(escape(w.businessUnitCode)).append(',')
          .append(escape(w.location)).append(',')
          .append(w.capacity).append(',')
          .append(w.stock).append('\n');
    }
    return Response.ok(csv.toString())
        .header("Content-Disposition", "attachment; filename=\"warehouses.csv\"")
        .type(MediaType.valueOf("text/csv"))
        .build();
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

  private String escape(String value) {
    if (value == null) return "";
    if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
      return "\"" + value.replace("\"", "\"\"") + "\"";
    }
    return value;
  }
}
