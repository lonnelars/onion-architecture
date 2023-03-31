package com.mycompany.app;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mycompany.database.DatabaseClient;
import com.mycompany.financial_api.Company;
import com.mycompany.financial_api.FinancialAPIClient;
import com.mycompany.services.DatasetService;
import java.time.Instant;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;import static spark.Spark.*;

public class App {
  private static final Logger logger = LoggerFactory.getLogger(App.class);

  private static final DatasetService datasetService;
  private static final Gson gson;
  private static final String json = "application/json";

  private static final FinancialAPIClient financialAPIClient;

  private static final DatabaseClient databaseClient;

  static {
    try {
      financialAPIClient = new FinancialAPIClient();
      databaseClient = new DatabaseClient();
      datasetService = new DatasetService(financialAPIClient, databaseClient);
    } catch (Exception e) {
      logger.atError().log("could not create dependencies: {}", e);
      throw new RuntimeException(e);
    }

    var gsonBuilder = new GsonBuilder();
    gsonBuilder.registerTypeAdapter(Instant.class, new InstantTypeAdapter());
    gson = gsonBuilder.create();
  }

  public static void main(String[] args) {
    var port = System.getenv("PORT");
    if (port != null) {
      port(Integer.parseInt(port));
    }
    get(
        "/ping",
        (req, res) -> {
          logger.atInfo().log("GET /ping");
          return "pong";
        });
    get(
        "/datasets/:id",
        (request, response) -> {
          var id = Integer.parseInt(request.params("id"));
          var opt = datasetService.getDataset(id);
          return opt.map(
                  src -> {
                    response.type(json);
                    return gson.toJson(src);
                  })
              .orElseGet(
                  () -> {
                    response.status(404);
                    return "dataset with id " + id + " does not exist";
                  });
        });
    get(
        "/datasets",
        (request, response) -> {
          var list = datasetService.getDatasetIds();
          response.type(json);
          return gson.toJson(list);
        });
    post(
        "/datasets",
        (request, response) -> {
          try {
            logger.atInfo().log("POST /datasets");
            var id = datasetService.newDataset(request.body());
            response.header("Location", "/datasets/" + id);
            response.status(201);
            return "";
          } catch (ValidationException e) {
            logger.atError().log("error: ", e);
            response.status(400);
            return e.getMessage();
          }
        });
    get(
        "/recommendation",
        (request, response) -> {
          logger.atInfo().log("GET /recommendation");
          var id = request.queryParams("dataset");
          var companyListOpt = datasetService.getRecommendation(id);
          return companyListOpt
              .map(
                  companyList ->
                      companyList.stream().map(Company::getSymbol).collect(Collectors.toList()))
              .map(
                  companyList -> {
                    response.type(json);
                    return gson.toJson(companyList);
                  })
              .orElseGet(() -> "invalid dataset id: " + id);
        });
  }
}
