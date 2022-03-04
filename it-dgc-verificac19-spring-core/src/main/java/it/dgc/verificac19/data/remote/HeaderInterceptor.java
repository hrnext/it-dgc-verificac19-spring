package it.dgc.verificac19.data.remote;

import java.io.IOException;
import java.util.Objects;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Request.Builder;
import okhttp3.Response;

/**
 *
 * This class defines the header interceptor to modify the HTTP requests properly.
 *
 */
public class HeaderInterceptor implements Interceptor {

  private final static String userAgent = "verificac19-spring/%s";

  private final static String cacheControl = "no-cache";

  @Override
  public Response intercept(Chain chain) throws IOException {

    Request request = addHeadersToRequest(chain.request());

    return chain.proceed(request);
  }

  /**
   *
   * This method adds headers to the given [Request] HTTP package in input and returns it.
   * @throws IOException 
   *
   */
  private Request addHeadersToRequest(Request request) throws IOException {
    Builder requestBuilder = request.newBuilder()
        .header("User-Agent", String.format(userAgent, this.getClass().getPackage().getImplementationVersion()))
        .header("Cache-Control", cacheControl);
    return requestBuilder.build();
  }

}
