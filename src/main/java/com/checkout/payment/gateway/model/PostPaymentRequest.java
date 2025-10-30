package com.checkout.payment.gateway.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;

public class PostPaymentRequest implements Serializable {

  @JsonIgnore
  private int expiryMonth;
  @JsonIgnore
  private int expiryYear;
  @JsonProperty("currency")
  private String currency;
  @JsonProperty("amount")
  private int amount;
  @JsonProperty("cvv")
  private int cvv;
  @JsonProperty("card_number")
  private String cardNumber;


  public String getCardNumber() { return cardNumber; }
  public void setCardNumber(String cardNumber) { this.cardNumber = cardNumber; }

  public int getExpiryMonth() {
    return expiryMonth;
  }

  public void setExpiryMonth(int expiryMonth) {
    this.expiryMonth = expiryMonth;
  }

  public int getExpiryYear() {
    return expiryYear;
  }

  public void setExpiryYear(int expiryYear) {
    this.expiryYear = expiryYear;
  }

  public String getCurrency() {
    return currency;
  }

  public void setCurrency(String currency) {
    this.currency = currency;
  }

  public int getAmount() {
    return amount;
  }

  public void setAmount(int amount) {
    this.amount = amount;
  }

  public int getCvv() {
    return cvv;
  }

  public void setCvv(int cvv) {
    this.cvv = cvv;
  }

  @JsonProperty("expiry_date")
  public String getExpiryDate() {
    return String.format("%d/%d", expiryMonth, expiryYear);
  }

  @JsonProperty("expiry_date")
  public void setExpiryDate(String expiryDate) {
    if(expiryDate != null && !expiryDate.isEmpty()) {
      String[] parts = expiryDate.split("/");
      if(parts.length == 2) {
        try {
          this.expiryMonth = Integer.parseInt(parts[0]);
          this.expiryYear = Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
          throw new IllegalArgumentException("Invalid expiry date format");
        }

      } else  {
        throw new IllegalArgumentException("Invalid expiry date format. Expected MM/YYYY");
      }
    }
  }

  @Override
  public String toString() {
    return "PostPaymentRequest{" +
        "cardNumber=" + cardNumber +
        ", expiryMonth=" + expiryMonth +
        ", expiryYear=" + expiryYear +
        ", currency='" + currency + '\'' +
        ", amount=" + amount +
        ", cvv=" + cvv +
        '}';
  }
}
