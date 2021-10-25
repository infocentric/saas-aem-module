package com.valtech.aem.saas.core.autocomplete;

import static com.valtech.aem.saas.core.fulltextsearch.SearchModelImpl.RESOURCE_TYPE;

import com.day.cq.wcm.api.Page;
import com.google.gson.Gson;
import com.valtech.aem.saas.api.autocomplete.AutocompleteResponse;
import com.valtech.aem.saas.api.caconfig.SearchConfiguration;
import com.valtech.aem.saas.api.fulltextsearch.SearchModel;
import com.valtech.aem.saas.api.typeahead.dto.TypeaheadPayloadDTO;
import com.valtech.aem.saas.api.typeahead.TypeaheadService;
import com.valtech.aem.saas.core.common.request.RequestWrapper;
import com.valtech.aem.saas.core.common.response.JsonResponseCommitter;
import com.valtech.aem.saas.core.fulltextsearch.SearchTabModelImpl;
import com.valtech.aem.saas.api.typeahead.dto.DefaultTypeaheadPayloadDTO;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import javax.annotation.PostConstruct;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.caconfig.ConfigurationBuilder;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.ScriptVariable;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;

/**
 * Search component sling model that handles autocomplete requests.
 */
@Model(adaptables = SlingHttpServletRequest.class,
    adapters = AutocompleteResponse.class,
    defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL,
    resourceType = RESOURCE_TYPE)
public class AutocompleteJsonResponse implements AutocompleteResponse {

  @Self
  private SlingHttpServletRequest request;

  @SlingObject
  private SlingHttpServletResponse response;

  @ScriptVariable
  private Page currentPage;

  @OSGiService
  private TypeaheadService typeaheadService;

  @PostConstruct
  private void init() {
    if (!response.isCommitted()) {
      Optional.ofNullable(request.adaptTo(RequestWrapper.class))
          .ifPresent(requestWrapper ->
              requestWrapper.getParameter(SearchTabModelImpl.SEARCH_TERM).ifPresent(text -> {
                SearchConfiguration searchConfiguration = request.getResource().adaptTo(ConfigurationBuilder.class)
                    .as(SearchConfiguration.class);
                if (StringUtils.isNotBlank(searchConfiguration.index())) {
                  TypeaheadPayloadDTO payload = DefaultTypeaheadPayloadDTO.builder()
                      .text(text)
                      .language(getLanguage())
                      .filters(getSearch().map(SearchModel::getFilters).orElse(Collections.emptySet()))
                      .build();
                  List<String> results = typeaheadService.getResults(searchConfiguration.index(), payload);
                  new JsonResponseCommitter(response).flush(printWriter -> new Gson().toJson(results, printWriter));
                }
              }));
    }
  }

  private Optional<SearchModel> getSearch() {
    return Optional.ofNullable(request.getResource().adaptTo(SearchModel.class));
  }

  private String getLanguage() {
    return currentPage.getLanguage().getLanguage();
  }

}
