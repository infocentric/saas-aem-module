package com.valtech.aem.saas.core.typeahead;

import com.valtech.aem.saas.api.typeahead.TypeaheadPayload;
import com.valtech.aem.saas.api.typeahead.TypeaheadService;
import com.valtech.aem.saas.core.common.saas.SaasIndexValidator;
import com.valtech.aem.saas.core.http.client.SearchRequestExecutorService;
import com.valtech.aem.saas.core.http.client.SearchServiceConnectionConfigurationService;
import com.valtech.aem.saas.core.http.request.SearchRequestGet;
import com.valtech.aem.saas.core.http.response.SearchResponse;
import com.valtech.aem.saas.core.http.response.TypeaheadDataExtractionStrategy;
import com.valtech.aem.saas.core.indexing.DefaultIndexUpdateService.Configuration;
import com.valtech.aem.saas.core.query.DefaultLanguageQuery;
import com.valtech.aem.saas.core.query.FiltersQuery;
import com.valtech.aem.saas.core.query.GetQueryStringConstructor;
import com.valtech.aem.saas.core.query.TypeaheadTextQuery;
import java.util.Collections;
import java.util.List;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@Slf4j
@Component(name = "Search as a Service - Typeahead Service",
    service = TypeaheadService.class)
@Designate(ocd = Configuration.class)
public class DefaultTypeaheadService implements TypeaheadService {

  @Reference
  private SearchServiceConnectionConfigurationService searchServiceConnectionConfigurationService;

  @Reference
  private SearchRequestExecutorService searchRequestExecutorService;

  private Configuration configuration;

  @Override
  public List<String> getResults(@NonNull String index, @NonNull TypeaheadPayload typeaheadPayload) {
    SaasIndexValidator.getInstance().validate(index);
    if (StringUtils.isBlank(typeaheadPayload.getText())) {
      throw new IllegalArgumentException("Typeahead payload should contain a search text.");
    }
    if (StringUtils.isBlank(typeaheadPayload.getLanguage())) {
      throw new IllegalArgumentException("Typeahead payload should contain a language.");
    }
    SearchRequestGet searchRequestGet = new SearchRequestGet(getApiUrl(index) + getQueryString(typeaheadPayload));
    return searchRequestExecutorService.execute(searchRequestGet)
        .filter(SearchResponse::isSuccess)
        .flatMap(response -> response.get(new TypeaheadDataExtractionStrategy(typeaheadPayload.getLanguage())))
        .orElse(Collections.emptyList());
  }

  private String getQueryString(@NonNull TypeaheadPayload typeaheadPayload) {
    return GetQueryStringConstructor.builder()
        .query(new TypeaheadTextQuery(typeaheadPayload.getText()))
        .query(new DefaultLanguageQuery(typeaheadPayload.getLanguage()))
        .query(FiltersQuery.builder()
            .filters(typeaheadPayload.getFilters()).build())
        .build()
        .getQueryString();
  }

  private String getApiUrl(String index) {
    return String.format("%s%s/%s%s",
        searchServiceConnectionConfigurationService.getBaseUrl(),
        configuration.typeaheadService_apiVersionPath(),
        index,
        configuration.typeaheadService_apiAction());
  }

  @Activate
  @Modified
  private void activate(Configuration configuration) {
    this.configuration = configuration;
  }

  @ObjectClassDefinition(name = "Search as a Service - Typeahead Service Configuration",
      description = "Typeahead Api specific details.")
  public @interface Configuration {

    String DEFAULT_API_ACTION = "/typeahead";
    String DEFAULT_API_VERSION_PATH = "/api/v3"; //NOSONAR

    @AttributeDefinition(name = "Api version path",
        description = "Path designating the api version",
        type = AttributeType.STRING)
    String typeaheadService_apiVersionPath() default DEFAULT_API_VERSION_PATH; //NOSONAR

    @AttributeDefinition(name = "Api action",
        description = "Path designating the action",
        type = AttributeType.STRING)
    String typeaheadService_apiAction() default DEFAULT_API_ACTION; //NOSONAR

  }
}