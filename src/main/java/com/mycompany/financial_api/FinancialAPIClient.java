package com.mycompany.financial_api;

import com.google.gson.Gson;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class FinancialAPIClient {
  private final List<Company> companies;

  public FinancialAPIClient() throws Exception {
    URI companyDatabase =
        Objects.requireNonNull(getClass().getClassLoader().getResource("obx.json")).toURI();
    String fileContents = Files.readString(Paths.get(companyDatabase));
    Gson gson = new Gson();
    companies = List.of(gson.fromJson(fileContents, Company[].class));
  }

  public List<String> symbols() {
    return companies.stream().map(company -> company.symbol).collect(Collectors.toList());
  }

  public Optional<Company> financialData(String symbol) {
    return companies.stream().filter(company -> company.symbol.equalsIgnoreCase(symbol)).findAny();
  }
}
