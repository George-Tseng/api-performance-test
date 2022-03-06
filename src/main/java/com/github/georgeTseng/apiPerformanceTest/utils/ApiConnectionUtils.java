package com.github.georgeTseng.apiPerformanceTest.utils;

import com.github.georgeTseng.apiPerformanceTest.ApiPerformanceTestApplication.HeadersParamKey;
import com.github.georgeTseng.apiPerformanceTest.ApiPerformanceTestApplication.HeadersParamValue;
import com.github.georgeTseng.apiPerformanceTest.enums.SupportedHttpMethod;
import com.github.georgeTseng.apiPerformanceTest.model.PerformanceRequestData;
import com.github.georgeTseng.apiPerformanceTest.model.PerformanceTestData;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.Optional;
import org.apache.commons.codec.Charsets;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class ApiConnectionUtils {

  public final static Integer HTTP_OK_STATUS = 200;
  private final static String ERROR_PARAM_KEY = "error";

  public static PerformanceTestData getApiConnectResult(PerformanceRequestData requestData) throws UnsupportedEncodingException,
      ClientProtocolException,
      IOException {

    Type jsonRefType = new TypeToken<Map<String, Object>>() {}.getType();

    Logger apiConnectionUtilsLogger = LoggerFactory.getLogger(ApiConnectionUtils.class);

    Map<String, Object> responseDatas = new HashMap<>();

    /* 取出Http方法 */
    SupportedHttpMethod targetHttpMethod = requestData.getHttpMethod();

    /* 取出content-type */
    String contentType = requestData.getContentType();

    CloseableHttpClient client = HttpClients.createDefault();

    /* 取得api回應物件 */
    CloseableHttpResponse response;
    if (SupportedHttpMethod.GET == targetHttpMethod && HeadersParamValue.JSON.getValue().equals(contentType)) {
      response = getApiConnectByGet(client, requestData);
    } else if (SupportedHttpMethod.POST == targetHttpMethod && HeadersParamValue.JSON.getValue().equals(contentType)) {
      response = getApiConnectByJsonPost(client, requestData);
    } else if (SupportedHttpMethod.POST == targetHttpMethod && HeadersParamValue.FORM_URI.getValue().equals(contentType)) {
      response = getApiConnectByFormPost(client, requestData);
    } else {
      apiConnectionUtilsLogger.error("本程式上不支援此種組合: {} : {} ", targetHttpMethod, contentType);
      responseDatas.put(ERROR_PARAM_KEY, "本程式上不支援此種組合: " + targetHttpMethod + ":" + contentType);

      return PerformanceTestData.builder()
          .statusCode(400)
          .responseDatas(responseDatas)
          .build();
    }

    /* 確認 status code */
    int statusCode = response.getStatusLine().getStatusCode();
    if (HTTP_OK_STATUS != statusCode) {
      apiConnectionUtilsLogger.error("link ng..., http status = {} ", statusCode);
    }

    /* 取出回應裡的entity物件 */
    HttpEntity responseEntity = response.getEntity();
    Header encodingHeader = responseEntity.getContentEncoding();

    /* 取得回傳之編碼 */
    Charset responseEncoding = encodingHeader == null ? StandardCharsets.UTF_8 :
        Charsets.toCharset(encodingHeader.getValue());

    /* 轉出回傳之json string */
    String responseJsonString = EntityUtils.toString(responseEntity, responseEncoding);

    /* 轉成map */
    responseDatas = new Gson().fromJson(responseJsonString, jsonRefType);

    return PerformanceTestData.builder()
        .statusCode(statusCode)
        .responseDatas(responseDatas)
        .build();

  }

  public static CloseableHttpResponse getApiConnectByGet(CloseableHttpClient client, PerformanceRequestData requestData) throws UnsupportedEncodingException, ClientProtocolException,
      IOException {

    Map<String, Object> otherParams = requestData.getOtherParams();

    /* 取出目標url */
    String targetUri = requestData.getUrl();
    String finalTargetUri = createFinalGetUrl(targetUri, otherParams);

    /* 指定 目標URL，使用 GET 方法進行，並設定 Headers */
    HttpGet httpGet = createHttpGetObject(finalTargetUri, requestData);

    /* 設定 回應物件 後執行 */
    return client.execute(httpGet);

  }

  public static CloseableHttpResponse getApiConnectByJsonPost(CloseableHttpClient client, PerformanceRequestData requestData) throws UnsupportedEncodingException, ClientProtocolException,
      IOException {

    /* 取出目標url */
    String targetUri = requestData.getUrl();

    /* 指定 目標URL，使用 POST 方法進行，並設定 Headers */
    HttpPost httpPost = createHttpPostObject(targetUri, requestData);

    /* 產生 json string */
    String requestJsonString = createRequestJsonString(requestData.getOtherParams());

    /* 產生並設定 StringEntity */
    StringEntity httpEntity = new StringEntity(requestJsonString);
    httpPost.setEntity(httpEntity);

    /* 設定 回應物件 後執行 */
    return client.execute(httpPost);
  }

  public static CloseableHttpResponse getApiConnectByFormPost(CloseableHttpClient client, PerformanceRequestData requestData) throws UnsupportedEncodingException, ClientProtocolException,
      IOException {

    /* 取出目標url */
    String targetUri = requestData.getUrl();

    /* 指定 目標URL，使用 POST 方法進行，並設定 Headers */
    HttpPost httpPost = createHttpPostObject(targetUri, requestData);

    /* 產生 json string */
    String requestJsonString = createRequestJsonString(requestData.getOtherParams());

    /* 產生並設定 StringEntity */
    StringEntity httpEntity = new StringEntity(requestJsonString);
    httpPost.setEntity(httpEntity);

    /* 設定 回應物件 後執行 */
    return client.execute(httpPost);
  }

  public static String createFinalGetUrl(String targetUri, Map<String, Object> otherParams) {

    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append(targetUri);

    /* 有其他參數，添加在?後 */
    if (Optional.ofNullable(otherParams).isPresent() && otherParams.size() > 0) {
      stringBuilder.append("?");

      for (Map.Entry<String, Object> otherParam: otherParams.entrySet()) {
        stringBuilder.append(otherParam.getKey()).append("=").append(otherParam.getValue()).append("&");
      }

      /* 移除最後多出來的& */
      stringBuilder.deleteCharAt(stringBuilder.length() - 1);
    }

    return stringBuilder.toString();
  }

  public static HttpGet createHttpGetObject(String targetUri, PerformanceRequestData requestData) {

    /* 指定 目標URL，使用 GET 方法進行 */
    HttpGet httpGet = new HttpGet(targetUri);

    /* 設定一般Headers */
    String contentType = requestData.getContentType();
    if (StringUtils.isNotBlank(contentType)) {
      httpGet.setHeader(HeadersParamKey.CONTENT_TYPE.getKey(), contentType);
    }

    String authorization = requestData.getAuthorization();
    if (StringUtils.isNotBlank(authorization)) {
      httpGet.setHeader(HeadersParamKey.AUTHORIZATION.getKey(), authorization);
    }

    String accept = requestData.getAccept();
    if (StringUtils.isNotBlank(accept)) {
      httpGet.setHeader(HeadersParamKey.ACCEPT.getKey(), accept);
    }

    Map<String, Object> otherHeadersParams = requestData.getOtherHeadersParams();
    /* 設定其他Headers */
    if (Optional.ofNullable(otherHeadersParams).isPresent() && otherHeadersParams.size() > 0) {
      for (Map.Entry<String, Object> headersParam : otherHeadersParams.entrySet()) {
        httpGet.setHeader(headersParam.getKey(), (String) headersParam.getValue());
      }
    }

    return httpGet;
  }

  public static HttpPost createHttpPostObject(String targetUri, PerformanceRequestData requestData) {

    /* 指定 目標URL，使用 GET 方法進行 */
    HttpPost httpPost = new HttpPost(targetUri);

    /* 設定一般Headers */
    String contentType = requestData.getContentType();
    if (StringUtils.isNotBlank(contentType)) {
      httpPost.setHeader(HeadersParamKey.CONTENT_TYPE.getKey(), contentType);
    }

    String authorization = requestData.getAuthorization();
    if (StringUtils.isNotBlank(authorization)) {
      httpPost.setHeader(HeadersParamKey.AUTHORIZATION.getKey(), authorization);
    }

    String accept = requestData.getAccept();
    if (StringUtils.isNotBlank(accept)) {
      httpPost.setHeader(HeadersParamKey.ACCEPT.getKey(), accept);
    }

    /* 設定其他Headers */
    Map<String, Object> otherHeadersParams = requestData.getOtherHeadersParams();
    if (Optional.ofNullable(otherHeadersParams).isPresent() && otherHeadersParams.size() > 0) {
      for (Map.Entry<String, Object> headersParam : otherHeadersParams.entrySet()) {
        httpPost.setHeader(headersParam.getKey(), (String) headersParam.getValue());
      }
    }

    return httpPost;
  }

  public static String createRequestJsonString(Map<String, Object> otherParams) {

    StringBuilder stringBuilder = new StringBuilder();

    /* 有其他參數時 */
    if (Optional.ofNullable(otherParams).isPresent() && otherParams.size() > 0) {

      /* json開頭 */
      stringBuilder.append("{");

      /* 設定其他參數 */
      for (Map.Entry<String, Object> otherParam: otherParams.entrySet()) {
        stringBuilder.append("\"").append(otherParam.getKey()).append("\":\"").append(otherParam.getValue()).append("\",");
      }

      /* 去除最後一個, */
      stringBuilder.deleteCharAt(stringBuilder.length() - 1);

      /* json結尾 */
      stringBuilder.append("}");
    }

    return stringBuilder.toString();
  }

}
