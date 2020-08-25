package com.bayamsell.api.server.handler;

import com.bayamsell.api.server.handler.ApiException;
import com.bayamsell.api.server.handler.ApiClient;
import com.bayamsell.api.server.handler.Configuration;
import com.bayamsell.api.server.handler.Pair;

import javax.ws.rs.core.GenericType;

import com.bayamsell.api.server.model.SendSmsRequest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", date = "2020-08-25T16:04:05.559456+02:00[Europe/Berlin]")
public class SmsApi {
  private ApiClient apiClient;

  public SmsApi() {
    this(Configuration.getDefaultApiClient());
  }

  public SmsApi(ApiClient apiClient) {
    this.apiClient = apiClient;
  }

  public ApiClient getApiClient() {
    return apiClient;
  }

  public void setApiClient(ApiClient apiClient) {
    this.apiClient = apiClient;
  }

  /**
   * Send sms
   * Send an SMS to the phone number provided
   * @param url Url of the sms api in the external server (required)
   * @param sendSmsRequest Send sms request (required)
   * @return a {@code Object}
   * @throws ApiException if fails to make API call
   */
  public Object sendSms(String url, SendSmsRequest sendSmsRequest) throws ApiException {
    Object localVarPostBody = sendSmsRequest;
    
    // verify the required parameter 'url' is set
    if (url == null) {
      throw new ApiException(400, "Missing the required parameter 'url' when calling sendSms");
    }
    
    // verify the required parameter 'sendSmsRequest' is set
    if (sendSmsRequest == null) {
      throw new ApiException(400, "Missing the required parameter 'sendSmsRequest' when calling sendSms");
    }
    
    // create path and map variables
    String localVarPath = "/{url}".replaceAll("\\{format\\}","json")
      .replaceAll("\\{" + "url" + "\\}", apiClient.escapeString(url.toString()));

    // query params
    List<Pair> localVarQueryParams = new ArrayList<Pair>();
    Map<String, String> localVarHeaderParams = new HashMap<String, String>();
    Map<String, String> localVarCookieParams = new HashMap<String, String>();
    Map<String, Object> localVarFormParams = new HashMap<String, Object>();


    
    
    
    final String[] localVarAccepts = {
      "application/json"
    };
    final String localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);

    final String[] localVarContentTypes = {
      "application/json"
    };
    final String localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

    String[] localVarAuthNames = new String[] { "basic_auth" };

    GenericType<Object> localVarReturnType = new GenericType<Object>() {};
    return apiClient.invokeAPI(localVarPath, "POST", localVarQueryParams, localVarPostBody, localVarHeaderParams, localVarCookieParams, localVarFormParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
      }
}
