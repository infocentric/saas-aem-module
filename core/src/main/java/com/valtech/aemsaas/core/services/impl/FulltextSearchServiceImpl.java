package com.valtech.aemsaas.core.services.impl;

import com.valtech.aemsaas.core.models.request.SearchRequestGet;
import com.valtech.aemsaas.core.models.response.parse.HighlightingParseStrategy;
import com.valtech.aemsaas.core.models.response.parse.ResponseBodyParseStrategy;
import com.valtech.aemsaas.core.models.response.parse.ResponseHeaderParseStrategy;
import com.valtech.aemsaas.core.models.response.search.ResponseBody;
import com.valtech.aemsaas.core.models.response.search.ResponseHeader;
import com.valtech.aemsaas.core.models.response.search.SearchResponse;
import com.valtech.aemsaas.core.models.response.search.SearchResult;
import com.valtech.aemsaas.core.models.search.FulltextSearchOptionalGetQuery;
import com.valtech.aemsaas.core.models.search.LanguageQuery;
import com.valtech.aemsaas.core.models.search.TermQuery;
import com.valtech.aemsaas.core.models.search.results.FulltextSearchResults;
import com.valtech.aemsaas.core.models.search.results.Result;
import com.valtech.aemsaas.core.services.FulltextSearchConfigurationService;
import com.valtech.aemsaas.core.services.FulltextSearchService;
import com.valtech.aemsaas.core.services.SearchRequestExecutorService;
import com.valtech.aemsaas.core.services.SearchServiceConnectionConfigurationService;
import com.valtech.aemsaas.core.services.impl.SearchServiceConnectionConfigurationServiceImpl.Configuration;
import com.valtech.aemsaas.core.utils.search.FulltextSearchGetQueryStringConstructor;
import com.valtech.aemsaas.core.utils.search.results.HighlightedDescriptionResolver;
import com.valtech.aemsaas.core.utils.search.results.HighlightedTitleResolver;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
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
@Component(name = "Search as a Service - Fulltext Search Service",
    service = {FulltextSearchService.class, FulltextSearchConfigurationService.class})
@Designate(ocd = Configuration.class)
public class FulltextSearchServiceImpl implements FulltextSearchService, FulltextSearchConfigurationService {

  private static final String SEARCH_TERM_ALL = "*";

  @Reference
  private SearchServiceConnectionConfigurationService searchServiceConnectionConfigurationService;

  @Reference
  private SearchRequestExecutorService searchRequestExecutorService;

  private Configuration configuration;

  @Override
  public int getRowsMaxLimit() {
    return configuration.fulltextSearchService_rowsMaxLimit();
  }

  @Override
  public Optional<FulltextSearchResults> getResults(String index, String term, String language,
      List<FulltextSearchOptionalGetQuery> optionalQueries) {
    if (StringUtils.isBlank(index)) {
      throw new IllegalArgumentException("Index type is missing.");
    }
    if (StringUtils.isBlank(language)) {
      throw new IllegalArgumentException("Language parameter is missing.");
    }
    String queryString = FulltextSearchGetQueryStringConstructor.builder()
        .query(new TermQuery(getSafeTerm(term)))
        .query(new LanguageQuery(language))
        .queries(optionalQueries)
        .build()
        .getQueryString();
    String requestUrl = String.format("%s%s%s%s%s",
        searchServiceConnectionConfigurationService.getBaseUrl(),
        configuration.fulltextSearchService_apiBaseUrl(),
        index,
        configuration.fulltextSearchService_apiAction(),
        queryString);
    log.debug("Search GET Request: {}", requestUrl);
    Optional<SearchResponse> searchResponse = searchRequestExecutorService.execute(new SearchRequestGet(requestUrl));
    if (searchResponse.isPresent()) {
      Optional<ResponseHeader> responseHeader = searchResponse.flatMap(
          response -> response.get(new ResponseHeaderParseStrategy()));
      responseHeader.ifPresent(header -> log.debug("Response Header: {}", header));
      Optional<ResponseBody> responseBody = searchResponse.flatMap(
          response -> response.get(new ResponseBodyParseStrategy()));
      if (responseBody.isPresent()) {
        Map<String, Map<String, List<String>>> highlighting = searchResponse.flatMap(
            response -> response.get(new HighlightingParseStrategy())).orElse(Collections.emptyMap());
        return Optional.of(FulltextSearchResults.builder()
            .totalResultsFound(responseBody.get().getNumFound())
            .currentResultPage(responseBody.get().getStart())
            .results(getProcessedResults(responseBody.get().getDocs(), highlighting))
            .build());
      } else {
        log.error("No response body is found.");
      }
    } else {
      log.debug("No Search Response received");
    }

    return Optional.empty();
  }

  private String getSafeTerm(String term) {
    return StringUtils.isNotBlank(term) ? term : SEARCH_TERM_ALL;
  }

  private List<Result> getProcessedResults(List<SearchResult> searchResults,
      Map<String, Map<String, List<String>>> highlighting) {
    return searchResults.stream()
        .map(searchResult -> getResult(searchResult, highlighting))
        .collect(Collectors.toList());
  }

  private Result getResult(SearchResult searchResult, Map<String, Map<String, List<String>>> highlighting) {
    return Result.builder()
        .url(searchResult.getUrl())
        .title(new HighlightedTitleResolver(searchResult, highlighting).getTitle())
        .description(new HighlightedDescriptionResolver(searchResult, highlighting).getDescription())
        .build();
  }

  @Activate
  @Modified
  private void activate(Configuration configuration) {
    this.configuration = configuration;
  }

  @ObjectClassDefinition(name = "Search as a Service - Fulltext Search Service Configuration",
      description = "Fulltext Search Api specific details.")
  public @interface Configuration {

    int DEFAULT_ROWS_MAX_LIMIT = 9999;

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
    int fulltextSearchService_rowsMaxLimit() default DEFAULT_ROWS_MAX_LIMIT;

  }
}
