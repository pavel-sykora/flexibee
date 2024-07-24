package net.testuje.app.flexibee.core.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.testuje.app.flexibee.core.api.FlexibeeClient.SSLConfig;
import net.testuje.app.flexibee.core.api.transformers.LocalDateAdapter;
import okhttp3.Credentials;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.jaxb.JaxbConverterFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

class RetrofitClientFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger("net.testuje.app.flexibee.core.api.http");

    public Retrofit createRetrofit(String apiBaseUrl, String username, String password, SSLConfig sslConfig) {
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(LocalDate.class, new LocalDateAdapter());

        Retrofit.Builder builder = new Retrofit.Builder()
                .baseUrl(apiBaseUrl)
                .addConverterFactory(JaxbConverterFactory.create())
                .addConverterFactory(GsonConverterFactory.create(gsonBuilder.create()));

        String authToken = Credentials.basic(username, password);
        builder.client(createOkHttpClient(authToken, sslConfig));

        return builder.build();
    }

    public <S> S createService(Class<S> serviceClass, Retrofit retrofit) {
        return retrofit.create(serviceClass);
    }

    private OkHttpClient createOkHttpClient(String authToken, SSLConfig sslConfig) {
        AuthenticationInterceptor interceptor = new AuthenticationInterceptor(authToken);
        OkHttpClient.Builder httpClient = new OkHttpClient.Builder();
        httpClient.addInterceptor(interceptor);
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor(LOGGER::debug);
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        httpClient.addInterceptor(loggingInterceptor);
        httpClient.followRedirects(true);
        httpClient.followSslRedirects(true);
        httpClient.readTimeout(30, TimeUnit.SECONDS);
        if (sslConfig != null) {
            configureSsl(httpClient, sslConfig);
        }
        return httpClient.build();
    }

    private void configureSsl(OkHttpClient.Builder httpClient, SSLConfig sslConfig) {
        try {
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(sslConfig.getKeyStore());
            TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();

            if (trustManagers.length != 1 || !(trustManagers[0] instanceof X509TrustManager)) {
                throw new IllegalStateException("Unexpected default trust managers:" + Arrays.toString(trustManagers));
            }

            X509TrustManager trustManager = (X509TrustManager) trustManagers[0];

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[]{trustManager}, null);
            SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

            httpClient.sslSocketFactory(sslSocketFactory, trustManager);

            if (sslConfig.getHostnameVerifier() != null) {
                httpClient.hostnameVerifier(sslConfig.getHostnameVerifier());
            }
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }

    }

    static class AuthenticationInterceptor implements Interceptor {
        private final String authToken;

        AuthenticationInterceptor(String token) {
            this.authToken = token;
        }

        public Response intercept(Chain chain) throws IOException {
            Request original = chain.request();
            Request.Builder builder = original.newBuilder().header("Authorization", authToken);
            Request request = builder.build();
            return chain.proceed(request);
        }
    }

}
