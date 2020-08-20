package com.openfocals.services.network.cloudintercept;

import com.openfocals.services.network.HTTPEndpointHandler;
import com.openfocals.services.network.HTTPHandler;

import java.util.Map;

import okio.Buffer;

public class CloudWeatherService {
    HTTPEndpointHandler.HTTPEndpoint endpoint = new HTTPEndpointHandler.HTTPEndpoint() {
        public boolean shouldHandle(String path) {
            return path.startsWith("/v1/weather");
        }

        public void handle(HTTPHandler h, String method, String path, Map<String, String> params, Map<String, String> headers, Buffer postdata) throws Exception
        {
            String latitude = params.get("latitude");
            String longitude = params.get("longitude");
            System.out.println("\n\nGot weather request: latitude=" + latitude + " longitude=" + longitude);

            String dummy_response = "{\"ok\":true,\"body\":{\"now\":{\"time\":\"2020-07-28T10:23:33-05:00\",\"condition\":\"clear-day\",\"temperature\":24.46,\"apparentTemperature\":24.63,\"probabilityOfPrecipitation\":0},\"today\":{\"temperatureHi\":30.36,\"temperatureLo\":22.16,\"apparentTemperatureHi\":30.91,\"apparentTemperatureLo\":22.8,\"probabilityOfPrecipitation\":0},\"extendedForecast\":{\"periods\":[{\"name\":\"Afternoon\",\"shortName\":\"Aft\",\"startTime\":\"2020-07-28T12:00:00-05:00\",\"endTime\":\"2020-07-28T17:59:59-05:00\",\"condition\":\"clear-day\",\"temperature\":29.25,\"apparentTemperature\":30.07,\"probabilityOfPrecipitation\":0.02},{\"name\":\"Evening\",\"shortName\":\"Eve\",\"startTime\":\"2020-07-28T18:00:00-05:00\",\"endTime\":\"2020-07-28T20:59:59-05:00\",\"condition\":\"clear-day\",\"temperature\":29.23,\"apparentTemperature\":30.15,\"probabilityOfPrecipitation\":0.01},{\"name\":\"Night\",\"shortName\":\"Night\",\"startTime\":\"2020-07-28T21:00:00-05:00\",\"endTime\":\"2020-07-29T05:59:59-05:00\",\"condition\":\"clear-night\",\"temperature\":25.14,\"apparentTemperature\":25.69,\"probabilityOfPrecipitation\":0.02}]},\"location\":{\"latitude\":41.884,\"longitude\":-87.6528},\"units\":\"si\",\"expiryDate\":\"2020-07-28T11:03:33-05:00\",\"precipitations\":[],\"radius\":5}}";


            h.sendResponse(200);
            h.sendHeader("Content-Type", "application/json; charset=utf-8");
            h.finishHeaders();
            h.sendContent(dummy_response);
            h.finishResponse();
        }
    };

    public void register(CloudMockService s) {
        s.getHttpEndpoints().registerEndpoint(endpoint);
    }
}
