package com.mycompany.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycompany.database.DatabaseClient;
import com.mycompany.financial_api.Company;
import com.mycompany.financial_api.FinancialAPIClient;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DatasetService {
  private final FinancialAPIClient financialAPIClient;
  private final Logger logger = LoggerFactory.getLogger(DatasetService.class);
  private final DatabaseClient databaseClient = new DatabaseClient();
  private static final ObjectMapper objectMapper = new ObjectMapper();

  public DatasetService() throws Exception {
    financialAPIClient = new FinancialAPIClient();
  }

  public int newDataset(String body)throws ValidationException {
    validatePostDatasetRequest(body);
    var symbols = financialAPIClient.symbols();
    var companies =
        symbols.stream()
            .map(
                symbol -> {
                  Optional<Company> opt = financialAPIClient.financialData(symbol);
                  if (opt.isEmpty()) {
                    logger.atWarn().log("could not get financial data for {}", symbol);
                  }
                  return opt;
                })
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toList());
    return databaseClient.saveDataset(companies);
  }

  private void validatePostDatasetRequest(String body) throws ValidationException {
    try {
      var newDatasetBody = objectMapper.readValue(body, NewDatasetBody.class);
      if (newDatasetBody.tickers.isEmpty()){
        throw new ValidationException("tickers is empty");
      }
    } catch (JsonProcessingException e) {
      throw new ValidationException("not valid", e);
    }
  }

  public static class ValidationException extends Exception {
    public ValidationException(String message, Throwable cause) {
      super(message, cause);
    }

    public ValidationException(String message) {
      super(message);
    }
  }
}
