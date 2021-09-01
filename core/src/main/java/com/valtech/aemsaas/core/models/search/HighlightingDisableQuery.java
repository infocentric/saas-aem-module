package com.valtech.aemsaas.core.models.search;

import java.util.Collections;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

public final class HighlightingDisableQuery implements FulltextSearchGetQuery {

  static final String HIGHLIGHT_SWITCH = "hl";

  @Override
  public List<NameValuePair> getEntries() {
    return Collections.singletonList(new BasicNameValuePair(HIGHLIGHT_SWITCH, Boolean.FALSE.toString()));
  }
}