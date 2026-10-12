package com.fulfilment.application.monolith.stores;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.TransactionPhase;
import jakarta.inject.Inject;

/**
 * Calls the legacy store manager only after the database transaction has successfully committed.
 * This prevents propagating data to the downstream system if our own write fails.
 */
@ApplicationScoped
public class StoreEventObserver {

  @Inject LegacyStoreManagerGateway legacyStoreManagerGateway;

  public void onStoreChanged(
      @Observes(during = TransactionPhase.AFTER_SUCCESS) StoreChangedEvent event) {
    if (event.operation == StoreChangedEvent.Operation.CREATE) {
      legacyStoreManagerGateway.createStoreOnLegacySystem(event.store);
    } else {
      legacyStoreManagerGateway.updateStoreOnLegacySystem(event.store);
    }
  }
}
