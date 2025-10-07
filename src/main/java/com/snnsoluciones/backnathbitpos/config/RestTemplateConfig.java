package com.snnsoluciones.backnathbitpos.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {

    private static final Logger HTTP_LOG = LoggerFactory.getLogger("HTTP");

    @Bean
    public RestTemplate restTemplate() {
        // Base factory con timeouts
        HttpComponentsClientHttpRequestFactory base = new HttpComponentsClientHttpRequestFactory();
        base.setConnectTimeout(10_000);
        base.setReadTimeout(10_000);

        // Buffering para poder leer el body varias veces (interceptor + converters/error handler)
        ClientHttpRequestFactory buffering = new BufferingClientHttpRequestFactory(base);

        RestTemplate rt = new RestTemplate(buffering);
        rt.setInterceptors(List.of(new LoggingInterceptor()));
        rt.setErrorHandler(new BodyLoggingErrorHandler());
        return rt;
    }

    static class LoggingInterceptor implements ClientHttpRequestInterceptor {
        @Override
        public ClientHttpResponse intercept(
            org.springframework.http.HttpRequest request,
            byte[] body,
            ClientHttpRequestExecution execution) throws IOException {

            // —— Request —— //
            String reqBody = new String(body, StandardCharsets.UTF_8);
            HTTP_LOG.debug(">>> {} {}\n>>> Headers: {}\n>>> Body: {}",
                request.getMethod(), request.getURI(), request.getHeaders(), reqBody);

            // Ejecutar
            ClientHttpResponse resp = execution.execute(request, body);

            // —— Response —— //
            // Gracias al BufferingClientHttpRequestFactory podemos leer el body aquí
            // y el resto de la cadena (converters/error handler) podrá volver a leerlo.
            HttpStatusCode status = resp.getStatusCode();
            HttpHeaders headers = resp.getHeaders();
            String respBody = StreamUtils.copyToString(resp.getBody(), StandardCharsets.UTF_8);

            HTTP_LOG.debug("<<< {} \n<<< Headers: {}\n<<< Body: {}",
                status, headers, respBody);

            // Devolvemos la misma response (no hace falta “cachearla” manualmente).
            return resp;
        }
    }

    static class BodyLoggingErrorHandler extends DefaultResponseErrorHandler {

        // Spring 6+: sobreescribir la nueva firma evita el warning por método deprecado
        public void handleError(ClientHttpResponse response, HttpStatusCode statusCode) throws IOException {
            String body = StreamUtils.copyToString(response.getBody(), StandardCharsets.UTF_8);
            HttpHeaders headers = response.getHeaders();

            LoggerFactory.getLogger("HTTP")
                .error("Hacienda ERROR {}. Headers: {}. Body: {}", statusCode, headers, body);

            // Delegar en el comportamiento estándar (lanzará HttpClientErrorException/HttpServerErrorException)
            super.handleError(response);
        }
    }
}