package com.checkout.payment.gateway.service;

import com.checkout.payment.gateway.exception.EventProcessingException;
import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.model.PostPaymentRequest;
import com.checkout.payment.gateway.model.PostPaymentResponse;
import com.checkout.payment.gateway.repository.PaymentsRepository;
import java.util.UUID;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

@Service
public class PaymentGatewayService {

  private static final Logger LOG = LoggerFactory.getLogger(PaymentGatewayService.class);

  private final PaymentsRepository paymentsRepository;
  private final RestTemplate restTemplate;
  
  @Value("${bank.simulator.url:http://localhost:8080}")
  private String bankSimulatorUrl;

  public PaymentGatewayService(PaymentsRepository paymentsRepository, RestTemplate restTemplate) {
    this.paymentsRepository = paymentsRepository;
    this.restTemplate = restTemplate;
  }

  public PostPaymentResponse getPaymentById(UUID id) {
    LOG.debug("Requesting access to to payment with ID {}", id);
    return paymentsRepository.get(id).orElseThrow(() -> new EventProcessingException("Invalid ID"));
  }

  public PostPaymentResponse processPayment(PostPaymentRequest paymentRequest) {
    LOG.debug("Processing payment request");
    
    // Step 1: Validate the payment request
    validatePaymentRequest(paymentRequest);
    
    // Step 2: Call the acquiring bank
    BankResponse bankResponse = callAcquiringBank(paymentRequest);
    
    // Step 3: Create payment response
    PostPaymentResponse paymentResponse = new PostPaymentResponse();
    paymentResponse.setId(UUID.randomUUID());
    paymentResponse.setStatus(bankResponse.isAuthorized() ? PaymentStatus.AUTHORIZED : PaymentStatus.DECLINED);
    paymentResponse.setCardNumberLastFour(getLastFourDigits(paymentRequest.getCardNumber()));
    paymentResponse.setExpiryMonth(paymentRequest.getExpiryMonth());
    paymentResponse.setExpiryYear(paymentRequest.getExpiryYear());
    paymentResponse.setCurrency(paymentRequest.getCurrency());
    paymentResponse.setAmount(paymentRequest.getAmount());
    
    // Step 4: Store the payment
    paymentsRepository.add(paymentResponse);
    
    LOG.info("Payment processed with ID: {} and status: {}", paymentResponse.getId(), paymentResponse.getStatus());
    
    return paymentResponse;
  }

  private void validatePaymentRequest(PostPaymentRequest request) {
    if (request.getCardNumber() == null || request.getCardNumber().isEmpty()) {
      throw new EventProcessingException("Card number is required");
    }

    if (!request.getCardNumber().matches("\\d{14,19}")) {
      throw new EventProcessingException("Card number must be between 14 and 19 digits");
    }

    if (request.getExpiryMonth() < 1 || request.getExpiryMonth() > 12) {
      throw new EventProcessingException("Expiry month must be between 1 and 12");
    }

    if (request.getExpiryYear() < 2024) {
      throw new EventProcessingException("Card has expired");
    }

    if (request.getCurrency() == null || request.getCurrency().isEmpty()) {
      throw new EventProcessingException("Currency is required");
    }

    if (!request.getCurrency().matches("[A-Z]{3}")) {
      throw new EventProcessingException("Currency must be a 3-letter code");
    }

    if (request.getAmount() <= 0) {
      throw new EventProcessingException("Amount must be greater than zero");
    }


    // CVV validation for int type
    String cvvStr = String.valueOf(request.getCvv());
    if (cvvStr.length() < 3 || cvvStr.length() > 4) {
      throw new EventProcessingException("CVV must be 3 or 4 digits");
    }
  }
  
  private BankResponse callAcquiringBank(PostPaymentRequest request) {
    try {
      // Prepare bank request
      BankRequest bankRequest = new BankRequest();
      bankRequest.setCardNumber(request.getCardNumber());
      bankRequest.setExpiryDate(String.format("%02d/%d", request.getExpiryMonth(), request.getExpiryYear()));
      bankRequest.setCurrency(request.getCurrency());
      bankRequest.setAmount(request.getAmount());
      bankRequest.setCvv(String.valueOf(request.getCvv()));
      
      // Call the bank simulator

      String bankUrl = bankSimulatorUrl + "/payments";
      BankResponse response = restTemplate.postForObject(bankUrl, bankRequest, BankResponse.class);
      
      LOG.debug("Bank response: {}", response);
      return response;
      
    } catch (Exception e) {
      LOG.error("Error calling acquiring bank", e);
      throw new EventProcessingException("Failed to process payment with bank");
    }
  }
  
  private int getLastFourDigits(String cardNumber) {
    if (cardNumber == null || cardNumber.length() < 4) {
      return 0;
    }
    return Integer.parseInt(cardNumber.substring(cardNumber.length() - 4));
  }
  
  // Inner classes for bank communication
  private static class BankRequest {
    @JsonProperty("card_number")
    private String card_number;
    @JsonProperty("expiry_date")
    private String expiry_date;
    @JsonProperty("currency")
    private String currency;
    @JsonProperty("amount")
    private int amount;
    @JsonProperty("cvv")
    private String cvv;
    
    // Getters and setters
    public String getCardNumber() { return card_number; }
    public void setCardNumber(String cardNumber) { this.card_number = cardNumber; }
    public String getExpiryDate() { return expiry_date; }
    public void setExpiryDate(String expiryDate) { this.expiry_date = expiryDate; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public int getAmount() { return amount; }
    public void setAmount(int amount) { this.amount = amount; }
    public String getCvv() { return cvv; }
    public void setCvv(String cvv) { this.cvv = cvv; }
  }
  
  private static class BankResponse {
    @JsonProperty("authorized")
    private boolean authorized;
    @JsonProperty("authorization_code")
    private String authorization_code;
    
    // Getters and setters
    public boolean isAuthorized() { return authorized; }
    public void setAuthorized(boolean authorized) { this.authorized = authorized; }
    public String getAuthorizationCode() { return authorization_code; }
    public void setAuthorizationCode(String authorizationCode) { this.authorization_code = authorizationCode; }
  }
}
