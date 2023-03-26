package com.mycompany.database;

import com.mycompany.financial_api.Company;
import java.util.Collections;import java.util.List;

public class Dataset {
  private int id;
  private List<Company> companies;

  public Dataset(int id, List<Company> companies) {
    this.id = id;
    this.companies = companies;
  }

  public List<Company> getCompanies() {
    return companies;
  }

  public void setCompanies(List<Company> companies) {
    this.companies = companies;
  }

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }
}
