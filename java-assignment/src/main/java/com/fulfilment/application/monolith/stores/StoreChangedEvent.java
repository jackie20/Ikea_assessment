package com.fulfilment.application.monolith.stores;

public class StoreChangedEvent {

  public enum Operation {
    CREATE,
    UPDATE
  }

  public final Store store;
  public final Operation operation;

  public StoreChangedEvent(Store store, Operation operation) {
    this.store = store;
    this.operation = operation;
  }
}
