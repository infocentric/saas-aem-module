package com.valtech.aemsaas.core.models.request;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;

@Slf4j
@Builder
public class SearchRequestPost implements SearchRequest {

  private final String url;

  @Singular
  private final List<NameValuePair> postParameters;

  @Override
  public @NonNull HttpUriRequest getRequest() {
    HttpPost httpPost = new HttpPost(url);
    createRequestPayload().ifPresent(httpPost::setEntity);
    return httpPost;
  }

  private Optional<HttpEntity> createRequestPayload() {
    try {
      log.debug("Creating the payload with the following parameters: {}", postParameters);
      return Optional.of(new UrlEncodedFormEntity(postParameters, StandardCharsets.UTF_8.name()));
    } catch (UnsupportedEncodingException e) {
      log.error("Failed to create POST request payload", e);
    }
    return Optional.empty();
  }
}
