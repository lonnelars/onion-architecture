package com.mycompany.services;

import java.util.List;

public class NewDatasetBody {
  public List<String> getTickers() {
    return tickers;
  }

  public void setTickers(List<String> tickers) {
    this.tickers = tickers;
  }

  List<String> tickers;
}
