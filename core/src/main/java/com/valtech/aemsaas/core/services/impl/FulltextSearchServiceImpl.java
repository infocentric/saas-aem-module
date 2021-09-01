package com.valtech.aemsaas.core.services.impl;

import com.google.gson.JsonObject;
import com.valtech.aemsaas.core.models.request.SearchRequestGet;
import com.valtech.aemsaas.core.models.responses.search.ResponseBody;
import com.valtech.aemsaas.core.models.responses.search.ResponseBodyParseStrategy;
import com.valtech.aemsaas.core.models.responses.search.ResponseHeader;
import com.valtech.aemsaas.core.models.responses.search.ResponseHeaderParseStrategy;
import com.valtech.aemsaas.core.models.responses.search.SearchResponse;
import com.valtech.aemsaas.core.models.search.FulltextSearchGetQuery;
import com.valtech.aemsaas.core.models.search.results.FulltextSearchResults;
import com.valtech.aemsaas.core.services.FulltextSearchService;
import com.valtech.aemsaas.core.services.SearchRequestExecutorService;
import com.valtech.aemsaas.core.services.SearchServiceConnectionConfigurationService;
import com.valtech.aemsaas.core.services.impl.SearchServiceConnectionConfigurationServiceImpl.Configuration;
import com.valtech.aemsaas.core.utils.HttpResponseParser;
import com.valtech.aemsaas.core.utils.search.FulltextSearchGetQueryStringConstructor;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@Slf4j
@Component(name = "Search as a Service - Fulltext Search Service",
    service = FulltextSearchService.class)
@Designate(ocd = Configuration.class)
public class FulltextSearchServiceImpl implements FulltextSearchService {

  @Reference
  private SearchServiceConnectionConfigurationService searchServiceConnectionConfigurationService;

  @Reference
  private SearchRequestExecutorService searchRequestExecutorService;

  private Configuration configuration;

  @Override
  public Optional<FulltextSearchResults> getResults(String index, List<FulltextSearchGetQuery> queries) {
    String queryString = FulltextSearchGetQueryStringConstructor.builder()
        .queries(queries)
        .build()
        .getQueryString();
    String requestUrl = String.format("%s%s%s%s%s",
        searchServiceConnectionConfigurationService.getBaseUrl(),
        configuration.fulltextSearchService_apiBaseUrl(), index,
        configuration.fulltextSearchService_apiAction(),
        queryString);
    Optional<SearchResponse> searchResponse = searchRequestExecutorService.execute(new SearchRequestGet(requestUrl));
    ResponseHeader responseHeader = searchResponse.flatMap(sR -> sR.get(new ResponseHeaderParseStrategy()))
        .orElse(null);
    ResponseBody responseBody = searchResponse.flatMap(sR -> sR.get(new ResponseBodyParseStrategy())).orElse(null);
    return Optional.of(FulltextSearchResults.builder().totalResultsFound(responseBody.getNumFound())
        .currentResultPage(responseBody.getStart()).build());
  }

  @Activate
  @Modified
  private void activate(Configuration configuration) {
    this.configuration = configuration;
  }

  @ObjectClassDefinition(name = "Search as a Service - Fulltext Search Service Configuration",
      description = "Fulltext Search Api specific details.")
  public @interface Configuration {

    @AttributeDefinition(name = "Api base path",
        description = "Api base path",
        type = AttributeType.STRING)
    String fulltextSearchService_apiBaseUrl() default "/api/v3";

    @AttributeDefinition(name = "Api action",
        description = "What kind of action should be defined",
        type = AttributeType.STRING)
    String fulltextSearchService_apiAction() default "/search";

    @AttributeDefinition(name = "Rows max limit.",
        description = "Maximum number of results per page allowed.",
        type = AttributeType.INTEGER)
    int fulltextSearchService_rowsMasLimit() default 9999;

  }
}