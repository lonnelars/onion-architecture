package com.mycompany.database;

import com.mycompany.financial_api.Company;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import org.jdbi.v3.core.Jdbi;

public class DatabaseClient {
  public int saveDataset(List<Company> companies) {
    var jdbi = Jdbi.create("jdbc:sqlite:database.db");
    return jdbi.inTransaction(
        transaction -> {
          transaction.execute("create table if not exists dataset(timestamp)");
          transaction.execute(
              "create table if not exists company (dataset_id references dataset(id), symbol, companyName, marketCap, sector, industry, beta, price, lastAnnualDividend, volume, exchange, exchangeShortName, country, isEtf, isActivelyTrading, earningsPerShare, bookValuePerShare, salesPerShare)");
          var dataset =
              transaction
                  .createUpdate("insert into dataset values(:timestamp)")
                  .bind("timestamp", Timestamp.from(Instant.now()))
                  .executeAndReturnGeneratedKeys()
                  .map(
                      (rs, ctx) ->
                          new Dataset(rs.getInt("last_insert_rowid()"), Collections.emptyList()));
          var id = dataset.one().getId();
          var batch =
              transaction.prepareBatch(
                  "insert into company values (:dataset_id, :symbol, :companyName, :marketCap, :sector, :industry, :beta, :price, :lastAnnualDividend, :volume, :exchange, :exchangeShortName, :country, :isEtf, :isActivelyTrading, :earningsPerShare, :bookValuePerShare, :salesPerShare)");
          companies.forEach(bean -> batch.bind("dataset_id", id).bindBean(bean).add());
          int[] counts = batch.execute();
          return id;
        });
  }
}
