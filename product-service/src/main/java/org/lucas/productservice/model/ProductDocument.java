package org.lucas.productservice.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.math.BigDecimal;

@Document(indexName = "products")
public class ProductDocument {

  @Id
  private String id;
  @Field(type = FieldType.Text, analyzer = "brazilian")
  private String name;
  @Field(type = FieldType.Text, analyzer = "brazilian")
  private String description;
  @Field(type = FieldType.Double)
  private Double price;

  public ProductDocument() {
  }

  public ProductDocument(String id, String name, String description, Double price) {
    this.id = id;
    this.name = name;
    this.description = description;
    this.price = price;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public Double getPrice() {
    return price;
  }

  public void setPrice(Double price) {
    this.price = price;
  }
}
