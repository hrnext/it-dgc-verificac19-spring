package it.dgc.verificac19.data.remote;

import java.util.List;

import it.dgc.verificac19.data.remote.model.Rule;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;

/**
 *
 * This interface defines the REST endpoints to contact to get important data for the correct
 * operation of the app. All the requests made are GET ones.
 *
 */
public interface ApiService {

  @GET("signercertificate/update")
  Call<ResponseBody> getCertUpdate(@Header("x-resume-token") String contentRange);

  @GET("signercertificate/status")
  Call<List<String>> getCertStatus();

  @GET("settings")
  Call<List<Rule>> getValidationRules();
}
