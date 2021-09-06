package com.valtech.aemsaas.core.models.request;

import org.apache.http.client.methods.HttpUriRequest;

/**
 * Represents a base type for a search request.
 */
public interface SearchRequest {

  /**
   * Gets the prepared http request.
   *
   * @return http request.
   */
  HttpUriRequest getRequest();
}
