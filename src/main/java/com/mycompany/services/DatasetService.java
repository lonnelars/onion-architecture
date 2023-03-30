package com.mycompany.services;

import static java.util.Comparator.comparingDouble;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.mycompany.app.ValidationException;
import com.mycompany.database.DatabaseClient;
import com.mycompany.database.Dataset;
import com.mycompany.financial_api.Company;
import com.mycompany.financial_api.FinancialAPIClient;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DatasetService {
  private final FinancialAPIClient financialAPIClient;
  private final Logger logger = LoggerFactory.getLogger(DatasetService.class);
  private final DatabaseClient databaseClient = new DatabaseClient();
  private static final Gson gson = new Gson();

  public DatasetService() throws Exception {
    financialAPIClient = new FinancialAPIClient();
  }

  public int newDataset(String body) throws ValidationException, IOException {
    var newDataset = parsePostDatasetRequest(body);
    List<String> symbols;
    if (newDataset.getSymbols().stream().anyMatch(symbol -> symbol.equals(":all"))) {
      symbols = financialAPIClient.symbols();
    } else {
      symbols = newDataset.getSymbols();
    }
    var companies =
        symbols.stream()
            .map(
                symbol -> {
                  Optional<Company> opt;
                  try {
                    opt = financialAPIClient.financialData(symbol);
                    if (opt.isEmpty()) {
                      logger.atWarn().log("could not get financial data for {}", symbol);
                    }
                    return opt;
                  } catch (IOException e) {
                    logger.atError().log("communication error with financial api: ", e);
                    throw new RuntimeException(e);
                  }
                })
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toList());
    if (companies.size() != symbols.size()) {
      throw new IOException("could not get data for all requested companies");
    }

    return databaseClient.saveDataset(companies);
  }

  private NewDataset parsePostDatasetRequest(String body) throws ValidationException {
    try {
      var newDatasetBody = gson.fromJson(body, NewDataset.class);
      if (newDatasetBody == null) {
        throw new ValidationException(
            "Body of request is missing. To create a new dataset, I need a list of symbols or the special value `:all` in the `symbols` field.");
      }
      if (newDatasetBody.getSymbols().isEmpty()) {
        throw new ValidationException("`symbols` is empty");
      }
      return newDatasetBody;
    } catch (JsonSyntaxException e) {
      throw new ValidationException("not valid", e);
    }
  }

  public Optional<Dataset> getDataset(int id) {
    return databaseClient.getDataset(id);
  }

  public List<Integer> getDatasetIds() {
    return databaseClient.getDatasetIds();
  }

  public Optional<List<Company>> getRecommendation(String id) {
    return parseDatasetId(id).flatMap(databaseClient::getDataset).map(this::top5Companies);
  }

  private List<Company> top5Companies(Dataset dataset) {
    return dataset.getCompanies().stream()
        .sorted(comparingDouble(DatasetService::priceToEarnings))
        .limit(5)
        .collect(Collectors.toList());
  }

  private static double priceToEarnings(Company company1) {
    return company1.getPrice() / company1.getEarningsPerShare();
  }

  private Optional<Integer> parseDatasetId(String id) {
    try {
      return Optional.of(Integer.parseInt(id));
    } catch (NumberFormatException e) {
      return Optional.empty();
    }
  }
}
