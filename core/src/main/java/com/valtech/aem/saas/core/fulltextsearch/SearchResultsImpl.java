package com.valtech.aem.saas.core.fulltextsearch;


import static com.valtech.aem.saas.core.fulltextsearch.SearchResultsImpl.RESOURCE_TYPE;

import com.adobe.cq.export.json.ComponentExporter;
import com.adobe.cq.export.json.ExporterConstants;
import com.day.cq.i18n.I18n;
import com.day.cq.wcm.api.Page;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.valtech.aem.saas.api.caconfig.SearchConfiguration;
import com.valtech.aem.saas.api.caconfig.SearchFilterConfiguration;
import com.valtech.aem.saas.api.fulltextsearch.FulltextSearchGetRequestPayload;
import com.valtech.aem.saas.api.fulltextsearch.FulltextSearchResults;
import com.valtech.aem.saas.api.fulltextsearch.FulltextSearchService;
import com.valtech.aem.saas.api.fulltextsearch.Result;
import com.valtech.aem.saas.api.fulltextsearch.Search;
import com.valtech.aem.saas.api.fulltextsearch.SearchResults;
import com.valtech.aem.saas.api.fulltextsearch.Suggestion;
import com.valtech.aem.saas.core.common.request.RequestWrapper;
import com.valtech.aem.saas.core.common.resource.ResourceWrapper;
import com.valtech.aem.saas.core.http.response.Highlighting;
import com.valtech.aem.saas.core.query.DefaultLanguageQuery;
import com.valtech.aem.saas.core.query.DefaultTermQuery;
import com.valtech.aem.saas.core.query.FiltersQuery;
import com.valtech.aem.saas.core.query.HighlightingTagQuery;
import com.valtech.aem.saas.core.query.PaginationQuery;
import com.valtech.aem.saas.core.util.StringToInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.caconfig.ConfigurationBuilder;
import org.apache.sling.jcr.resource.api.JcrResourceConstants;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Exporter;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.ScriptVariable;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;

@Model(adaptables = SlingHttpServletRequest.class,
    adapters = {SearchResults.class, ComponentExporter.class},
    defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL,
    resourceType = RESOURCE_TYPE)
@Exporter(name = ExporterConstants.SLING_MODEL_EXPORTER_NAME, extensions = ExporterConstants.SLING_MODEL_EXTENSION)
public class SearchResultsImpl implements SearchResults {

  public static final String RESOURCE_TYPE = "saas-aem-module/components/saas/searchresults";
  public static final String QUERY_PARAM_START = "start";
  public static final int DEFAULT_START_PAGE = 0;
  public static final int DEFAULT_RESULTS_PER_PAGE = 10;
  public static final String SEARCH_TERM = "q";
  public static final String QUERY_PARAM_ROWS = "rows";
  public static final String I18N_KEY_LOAD_MORE_BUTTON_LABEL = "com.valtech.aem.saas.core.search.loadmore.button.label";

  @Self
  private SlingHttpServletRequest request;

  @OSGiService
  private FulltextSearchService fulltextSearchService;

  @OSGiService
  private FulltextSearchConfigurationService fulltextSearchConfigurationService;

  @ScriptVariable
  private Page currentPage;

  @JsonInclude(Include.NON_EMPTY)
  @Getter
  private String term;

  @JsonInclude(Include.NON_EMPTY)
  @Getter
  private int startPage;

  @JsonInclude(Include.NON_EMPTY)
  @Getter
  private int resultsPerPage;

  @JsonInclude(Include.NON_NULL)
  @Getter
  private List<Result> results;

  @JsonInclude(Include.NON_NULL)
  @Getter
  private int resultsTotal;

  @Getter
  private boolean showLoadMoreButton;

  @JsonInclude(Include.NON_NULL)
  @Getter
  private Suggestion suggestion;

  private int configuredResultsPerPage;

  @Getter
  @ValueMapValue(name = JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY)
  private String exportedType;

  private I18n i18n;

  @JsonIgnore
  @Getter
  private int loadMoreRows;

  @PostConstruct
  private void init() {
    getParentSearchComponent().ifPresent(parentSearch -> {
      configuredResultsPerPage = parentSearch.getResultsPerPage();
      Optional.ofNullable(request.adaptTo(RequestWrapper.class))
          .ifPresent(requestWrapper -> {
            i18n = requestWrapper.getI18n();
            requestWrapper.getParameter(SEARCH_TERM).ifPresent(searchTerm -> {
              term = searchTerm;
              startPage = requestWrapper.getParameter(QUERY_PARAM_START)
                  .map(start -> new StringToInteger(start).asInt())
                  .map(OptionalInt::getAsInt)
                  .orElse(DEFAULT_START_PAGE);
              resultsPerPage = resolveResultsPerPage(requestWrapper);
              //todo: remove this when ICSAAS-315 is done - currently utilized only for demo purpose
              loadMoreRows = resultsPerPage + configuredResultsPerPage;
              SearchConfiguration searchConfiguration = request.getResource().adaptTo(ConfigurationBuilder.class)
                  .as(SearchConfiguration.class);
              FulltextSearchGetRequestPayload fulltextSearchGetRequestPayload =
                  DefaultFulltextSearchRequestPayload.builder(new DefaultTermQuery(searchTerm),
                          new DefaultLanguageQuery(getLanguage()))
                      .optionalQuery(
                          new PaginationQuery(startPage, resultsPerPage,
                              fulltextSearchConfigurationService.getRowsMaxLimit()))
                      .optionalQuery(new HighlightingTagQuery(Highlighting.HIGHLIGHTING_TAG_NAME))
                      .optionalQuery(FiltersQuery.builder()
                          .filterEntries(Arrays.stream(searchConfiguration.searchFilters()).collect(Collectors.toMap(
                              SearchFilterConfiguration::name, SearchFilterConfiguration::value))).build())
                      .build();
              Optional<FulltextSearchResults> fulltextSearchResults = fulltextSearchService.getFulltextSearchConsumerService(
                      searchConfiguration.index(),
                      FulltextSearchConfigurationFactory.builder()
                          .enableAutoSuggest(parentSearch.isAutoSuggestEnabled())
                          .enableBestBets(parentSearch.isBestBetsEnabled())
                          .build().getConfiguration())
                  .getResults(fulltextSearchGetRequestPayload);
              results = fulltextSearchResults.map(FulltextSearchResults::getResults).orElse(Collections.emptyList());
              resultsTotal = fulltextSearchResults.map(FulltextSearchResults::getTotalResultsFound).orElse(0);
              showLoadMoreButton = !results.isEmpty() && results.size() < resultsTotal;
              suggestion = fulltextSearchResults.map(FulltextSearchResults::getSuggestion).orElse(null);
            });
          });
    });
  }

  private int resolveResultsPerPage(RequestWrapper requestWrapper) {
    return requestWrapper.getParameter(QUERY_PARAM_ROWS)
        .map(s -> new StringToInteger(s).asInt())
        .map(OptionalInt::getAsInt)
        .orElse(configuredResultsPerPage);
  }

  @JsonIgnore
  @Override
  public String getLoadMoreButtonText() {
    return Optional.ofNullable(i18n).map(t -> t.get(I18N_KEY_LOAD_MORE_BUTTON_LABEL)).orElse(StringUtils.EMPTY);
  }

  private Optional<Search> getParentSearchComponent() {
    return Optional.ofNullable(request.getResource().adaptTo(ResourceWrapper.class))
        .flatMap(r -> r.getParentWithResourceType(SearchImpl.RESOURCE_TYPE))
        .map(r -> r.adaptTo(Search.class));
  }

  private String getLanguage() {
    return currentPage.getLanguage().getLanguage();
  }
}
