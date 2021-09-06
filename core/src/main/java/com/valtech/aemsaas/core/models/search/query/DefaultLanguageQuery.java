package com.valtech.aemsaas.core.models.search.query;

import java.util.Collections;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

/**
 * Default implementation of {@link LanguageQuery}. It uses "lang" as the parameter key.
 */
public final class DefaultLanguageQuery implements LanguageQuery {

  private static final String KEY = "lang";

  private final NameValuePair languageQuery;

  /**
   * Constructs a language query.
   * @param value the language in 2 letter format.
   */
  public DefaultLanguageQuery(String value) {
    if (StringUtils.isBlank(value)) {
      throw new IllegalArgumentException("Language value must not be blank.");
    }
    languageQuery = new BasicNameValuePair(KEY, value);
  }

  @Override
  public List<NameValuePair> getEntries() {
    return Collections.singletonList(languageQuery);
  }
}
