package com.valtech.aem.saas.api.typeahead;

import java.util.List;
import lombok.NonNull;

/**
 * Represents a service that consumes the SaaS typeahead api.
 */
public interface TypeaheadService {

  /**
   * Retrieves typeahead results
   *
   * @param index            SaaS client index.
   * @param typeaheadPayload object containing typeahead query values.
   * @return List of string represented typeahead options. Empty list if no options are found.
   */
  List<String> getResults(@NonNull String index, @NonNull TypeaheadPayload typeaheadPayload);

}