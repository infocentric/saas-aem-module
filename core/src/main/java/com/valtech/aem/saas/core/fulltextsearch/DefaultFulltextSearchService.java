package com.valtech.aem.saas.core.fulltextsearch;

import com.valtech.aem.saas.api.fulltextsearch.FulltextSearchGetRequestPayload;
import com.valtech.aem.saas.api.fulltextsearch.FulltextSearchResults;
import com.valtech.aem.saas.api.fulltextsearch.FulltextSearchService;
import com.valtech.aem.saas.api.fulltextsearch.Result;
import com.valtech.aem.saas.core.common.saas.SaasIndexValidator;
import com.valtech.aem.saas.core.fulltextsearch.DefaultFulltextSearchService.Configuration;
import com.valtech.aem.saas.core.http.client.SearchRequestExecutorService;
import com.valtech.aem.saas.core.http.client.SearchServiceConnectionConfigurationService;
import com.valtech.aem.saas.core.http.request.SearchRequestGet;
import com.valtech.aem.saas.core.http.response.FallbackHighlighting;
import com.valtech.aem.saas.core.http.response.Highlighting;
import com.valtech.aem.saas.core.http.response.HighlightingDataExtractionStrategy;
import com.valtech.aem.saas.core.http.response.ResponseBody;
import com.valtech.aem.saas.core.http.response.ResponseBodyDataExtractionStrategy;
import com.valtech.aem.saas.core.http.response.ResponseHeaderDataExtractionStrategy;
import com.valtech.aem.saas.core.http.response.SearchResponse;
import com.valtech.aem.saas.core.http.response.SearchResult;
import com.valtech.aem.saas.core.http.response.SuggestionDataExtractionStrategy;
import com.valtech.aem.saas.core.util.LoggedOptional;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.NonNull;
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
    service = {FulltextSearchService.class, FulltextSearchConfigurationService.class})
@Designate(ocd = Configuration.class)
public class DefaultFulltextSearchService implements
    FulltextSearchService,
    FulltextSearchConfigurationService {

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
  public Optional<FulltextSearchResults> getResults(@NonNull String index,
      @NonNull FulltextSearchGetRequestPayload fulltextSearchGetRequestPayload,
      boolean enableAutoSuggest,
      boolean enableBestBets) {
    SaasIndexValidator.getInstance().validate(index);
    String requestUrl = getRequestUrl(index, fulltextSearchGetRequestPayload);
    log.debug("Search GET Request: {}", requestUrl);
    Optional<SearchResponse> searchResponse = searchRequestExecutorService.execute(new SearchRequestGet(requestUrl));
    if (searchResponse.isPresent()) {
      printResponseHeaderInLog(searchResponse.get());
      return getFulltextSearchResults(searchResponse.get(), enableAutoSuggest, enableBestBets);
    }
    return Optional.empty();
  }

  private String getRequestUrl(String index, FulltextSearchGetRequestPayload fulltextSearchGetRequestPayload) {
    return String.format("%s%s",
        getApiUrl(index),
        fulltextSearchGetRequestPayload.getPayload());
  }

  private Optional<FulltextSearchResults> getFulltextSearchResults(SearchResponse searchResponse,
      boolean enableAutoSuggest,
      boolean enableBestBets) {
    Optional<ResponseBody> responseBody = searchResponse.get(new ResponseBodyDataExtractionStrategy());
    if (responseBody.isPresent()) {
      Highlighting highlighting = searchResponse.get(new HighlightingDataExtractionStrategy())
          .orElse(FallbackHighlighting.getInstance());
      Stream<Result> results = getProcessedResults(responseBody.get().getDocs(), highlighting);
      if (enableBestBets) {
        log.debug("Best bets is enabled. Results will be sorted so that best bet results are on top.");
        results = results.sorted(Comparator.comparing(Result::isBestBet).reversed());
      }
      FulltextSearchResults.FulltextSearchResultsBuilder fulltextSearchResultsBuilder =
          FulltextSearchResults.builder()
              .totalResultsFound(responseBody.get().getNumFound())
              .currentResultPage(responseBody.get().getStart())
              .results(results.collect(Collectors.toList()));
      if (enableAutoSuggest) {
        log.debug("Auto suggest is enabled.");
        searchResponse.get(new SuggestionDataExtractionStrategy()).flatMap(suggestion -> LoggedOptional.of(suggestion,
                logger -> logger.debug("No suggestion has been found in search response")))
            .ifPresent(fulltextSearchResultsBuilder::suggestion);
      }
      return Optional.of(fulltextSearchResultsBuilder.build());
    } else {
      log.error("No response body is found.");
    }
    return Optional.empty();
  }

  private Stream<Result> getProcessedResults(List<SearchResult> searchResults,
      Highlighting highlighting) {
    return searchResults.stream()
        .map(searchResult -> getResult(searchResult, highlighting));
  }

  private Result getResult(SearchResult searchResult, Highlighting highlighting) {
    return Result.builder()
        .url(searchResult.getUrl())
        .title(new HighlightedTitleResolver(searchResult, highlighting).getTitle())
        .description(new HighlightedDescriptionResolver(searchResult, highlighting).getDescription())
        .bestBet(searchResult.isElevated())
        .build();
  }

  private void printResponseHeaderInLog(SearchResponse searchResponse) {
    searchResponse.get(new ResponseHeaderDataExtractionStrategy())
        .ifPresent(header -> log.debug("Response Header: {}", header));
  }

  private String getApiUrl(String index) {
    return String.format("%s%s/%s%s",
        searchServiceConnectionConfigurationService.getBaseUrl(),
        configuration.fulltextSearchService_apiVersion(),
        index,
        configuration.fulltextSearchService_apiAction());
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
    String DEFAULT_API_ACTION = "/search";
    String DEFAULT_API_VERSION_PATH = "/api/v3"; // NOSONAR

    @AttributeDefinition(name = "Api base path",
        description = "Api base path",
        type = AttributeType.STRING)
    String fulltextSearchService_apiVersion() default DEFAULT_API_VERSION_PATH; // NOSONAR

    @AttributeDefinition(name = "Api action",
        description = "What kind of action should be defined",
        type = AttributeType.STRING)
    String fulltextSearchService_apiAction() default DEFAULT_API_ACTION; // NOSONAR

    @AttributeDefinition(name = "Rows max limit.",
        description = "Maximum number of results per page allowed.",
        type = AttributeType.INTEGER)
    int fulltextSearchService_rowsMaxLimit() default DEFAULT_ROWS_MAX_LIMIT; // NOSONAR

  }
}
