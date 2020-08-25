# SmsApi

All URIs are relative to *http://localhost:8080*

Method | HTTP request | Description
------------- | ------------- | -------------
[**sendSms**](SmsApi.md#sendSms) | **POST** /{url} | Send sms



## sendSms

> Object sendSms(url, sendSmsRequest)

Send sms

Send an SMS to the phone number provided

### Example

```java
// Import classes:
import com.bayamsell.api.server.handler.ApiClient;
import com.bayamsell.api.server.handler.ApiException;
import com.bayamsell.api.server.handler.Configuration;
import com.bayamsell.api.server.handler.auth.*;
import com.bayamsell.api.server.handler.models.*;
import com.bayamsell.api.server.handler.SmsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8080");
        
        // Configure HTTP basic authorization: basic_auth
        HttpBasicAuth basic_auth = (HttpBasicAuth) defaultClient.getAuthentication("basic_auth");
        basic_auth.setUsername("YOUR USERNAME");
        basic_auth.setPassword("YOUR PASSWORD");

        SmsApi apiInstance = new SmsApi(defaultClient);
        String url = "url_example"; // String | Url of the sms api in the external server
        SendSmsRequest sendSmsRequest = new SendSmsRequest(); // SendSmsRequest | Send sms request
        try {
            Object result = apiInstance.sendSms(url, sendSmsRequest);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling SmsApi#sendSms");
            System.err.println("Status code: " + e.getCode());
            System.err.println("Reason: " + e.getResponseBody());
            System.err.println("Response headers: " + e.getResponseHeaders());
            e.printStackTrace();
        }
    }
}
```

### Parameters


Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **url** | **String**| Url of the sms api in the external server |
 **sendSmsRequest** | [**SendSmsRequest**](SendSmsRequest.md)| Send sms request |

### Return type

**Object**

### Authorization

[basic_auth](../README.md#basic_auth)

### HTTP request headers

- **Content-Type**: application/json
- **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | successful operation |  -  |
| **0** | successful operation |  -  |

