package com.fulfilment.application.monolith.common;

import java.util.List;

public class PaginatedResponse<T> {

  public final List<T> content;
  public final int page;
  public final int size;
  public final long totalElements;
  public final int totalPages;

  public PaginatedResponse(List<T> content, int page, int size, long totalElements) {
    this.content = content;
    this.page = page;
    this.size = size;
    this.totalElements = totalElements;
    this.totalPages = size > 0 ? (int) Math.ceil((double) totalElements / size) : 0;
  }

  public static <T> PaginatedResponse<T> of(List<T> content, int page, int size, long totalElements) {
    return new PaginatedResponse<>(content, page, size, totalElements);
  }
}
