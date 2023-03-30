package com.mycompany.financial_api;

import com.google.gson.Gson;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import okhttp3.OkHttpClient;
import okhttp3.Request;

public class FinancialAPIClient {
  private final List<Company> companies;
  private static final String api = "https://larlonboubrsjpexqzzolyfl.z1.web.core.windows.net";

  private static final OkHttpClient client = new OkHttpClient();

  public FinancialAPIClient() throws Exception {
    URI companyDatabase =
        Objects.requireNonNull(getClass().getClassLoader().getResource("obx.json")).toURI();
    String fileContents = Files.readString(Paths.get(companyDatabase));
    Gson gson = new Gson();
    companies = List.of(gson.fromJson(fileContents, Company[].class));
  }

  public List<String> symbols() throws IOException {
    var request = new Request.Builder().url(api + "/all.json").build();
    try (var response = client.newCall(request).execute()) {
      var body = response.body().string();
      return List.of(new Gson().fromJson(body, String[].class));
    }
  }

  public Optional<Company> financialData(String symbol) {
    return companies.stream().filter(company -> company.symbol.equalsIgnoreCase(symbol)).findAny();
  }
}
