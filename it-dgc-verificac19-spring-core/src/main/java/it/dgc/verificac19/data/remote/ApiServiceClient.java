package it.dgc.verificac19.data.remote;

import java.io.File;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import okhttp3.Cache;
import okhttp3.Call.Factory;
import okhttp3.CertificatePinner;
import okhttp3.CipherSuite;
import okhttp3.ConnectionSpec;
import okhttp3.OkHttpClient;
import okhttp3.OkHttpClient.Builder;
import okhttp3.TlsVersion;
import okhttp3.logging.HttpLoggingInterceptor;
import okhttp3.logging.HttpLoggingInterceptor.Level;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

@Service
public class ApiServiceClient {

  private static final Logger LOG = LoggerFactory.getLogger(ApiServiceClient.class);

  private static final long CONNECT_TIMEOUT = 30L;

  @Value(value = "${config.base_url}")
  private String baseUrl;

  @Value(value = "${config.server_host}")
  private String serverHost;

  @Value(value = "${config.certificate_sha}")
  private String certificateSha;

  private ApiService apiService;

  @PostConstruct
  public void init() {

    Retrofit retrofit = new Retrofit.Builder().baseUrl(baseUrl).callFactory(callFactory())
        .addConverterFactory(GsonConverterFactory.create()).build();

    this.apiService = retrofit.create(ApiService.class);
  }

  private Factory callFactory() {

    // See test result on ssl labs
    ConnectionSpec spec =
        new ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS).tlsVersions(TlsVersion.TLS_1_2)
            .cipherSuites(CipherSuite.TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256,
                CipherSuite.TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384)
            .build();


    Builder builder = new OkHttpClient().newBuilder().cache(provideCache()) // Cache
        .connectionSpecs(Arrays.asList(spec)).addInterceptor(new HeaderInterceptor())
        .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS) // Timeout
        .readTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS); // Timeout

    if (LOG.isDebugEnabled()) {
      HttpLoggingInterceptor logger = new HttpLoggingInterceptor();
      logger.setLevel(Level.BODY);
      builder.addInterceptor(logger);
    }

    // Certificate
    builder
        .certificatePinner(new CertificatePinner.Builder().add(serverHost, certificateSha).build());

    return builder.build();

  }

  private Cache provideCache() {
    long cacheSize = 10 * 1024 * 1024; // 10MB
    return new Cache(new File(System.getProperty("java.io.tmpdir")), cacheSize);
  }

  /**
   * @return the apiService
   */
  public ApiService getApiCertificate() {
    return apiService;
  }

}
