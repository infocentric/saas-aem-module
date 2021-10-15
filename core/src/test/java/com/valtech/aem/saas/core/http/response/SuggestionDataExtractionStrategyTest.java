package com.valtech.aem.saas.core.http.response;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.valtech.aem.saas.api.fulltextsearch.Suggestion;
import java.io.InputStreamReader;
import org.junit.jupiter.api.Test;

class SuggestionDataExtractionStrategyTest {

  @Test
  void testGetData() {
    SuggestionDataExtractionStrategy strategy = new SuggestionDataExtractionStrategy();
    assertThat(strategy.propertyName(), is(SuggestionDataExtractionStrategy.PN_SPELLCHECK));
    assertThat(strategy.getData(new JsonObject()).isPresent(), is(false));
    JsonObject response = new JsonParser().parse(new InputStreamReader(getClass().getResourceAsStream(
        "/__files/search/fulltext/spellcheck.json"))).getAsJsonObject();
    assertThat(strategy.getData(response).isPresent(), is(true));
    assertThat(strategy.getData(response).get(), instanceOf(
        Suggestion.class));

  }

  @Test
  void testGetData_withoutCollations() {
    testInvalidResponse("/__files/search/fulltext/spellcheck_wOutCollations.json");
  }

  @Test
  void testGetData_insufficientNumberOfCollationItems() {
    testInvalidResponse("/__files/search/fulltext/spellcheck_insufficientNumberOfCollationItems.json");
  }

  @Test
  void testGetData_secondCollationItemIncorrectFormat() {
    testInvalidResponse("/__files/search/fulltext/spellcheck_secondCollationItemIncorrectFormat.json");
  }

  private void testInvalidResponse(String responseFilePath) {
    SuggestionDataExtractionStrategy strategy = new SuggestionDataExtractionStrategy();
    assertThat(strategy.propertyName(), is(SuggestionDataExtractionStrategy.PN_SPELLCHECK));
    assertThat(strategy.getData(new JsonObject()).isPresent(), is(false));
    JsonObject response = new JsonParser().parse(
        new InputStreamReader(getClass().getResourceAsStream(responseFilePath))).getAsJsonObject();
    assertThat(strategy.getData(response).isPresent(), is(false));
  }
}
