package com.valtech.aemsaas.core.models.search;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

@RequiredArgsConstructor
public class HighlightingTagQuery implements FulltextSearchOptionalGetQuery {

  static final String HIGHLIGHT_PRE_TAG = "hlpre";
  static final String HIGHLIGHT_POST_TAG = "hlpost";

  private final String tagName;

  @Override
  public List<NameValuePair> getEntries() {
    return Optional.ofNullable(tagName).filter(StringUtils::isNotEmpty).map(tag -> {
      List<NameValuePair> entries = new ArrayList<>();
      entries.add(new BasicNameValuePair(HIGHLIGHT_PRE_TAG, tag));
      entries.add(new BasicNameValuePair(HIGHLIGHT_POST_TAG, tag));
      return entries;
    }).orElse(
        Collections.emptyList());
  }
}
