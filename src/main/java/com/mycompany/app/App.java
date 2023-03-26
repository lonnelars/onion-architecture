package com.mycompany.app;

import static spark.Spark.get;
import static spark.Spark.post;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycompany.services.DatasetService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class App {
  private static final Logger logger = LoggerFactory.getLogger(App.class);

  private static final DatasetService datasetService;
  private static final ObjectMapper objectMapper = new ObjectMapper();

  static {
    try {
      datasetService = new DatasetService();
    } catch (Exception e) {
      logger.atError().log("could not create dependencies: {}", e);
      throw new RuntimeException(e);
    }
  }

  public static void main(String[] args) {
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
                  value -> {
                    try {
                      return objectMapper.writeValueAsString(value);
                    } catch (JsonProcessingException e) {
                      throw new RuntimeException(e);
                    }
                  })
              .orElseGet(
                  () -> {
                    response.status(404);
                    return "dataset with id " + id + " does not exist";
                  });
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
          } catch (DatasetService.ValidationException e) {
            logger.atError().log("error: ", e);
            response.status(400);
            return e.getMessage();
          }
        });
  }
}
