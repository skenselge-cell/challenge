package com.checkout.payment.gateway.service;

import com.checkout.payment.gateway.exception.EventProcessingException;
import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.model.PostPaymentRequest;
import com.checkout.payment.gateway.model.PostPaymentResponse;
import com.checkout.payment.gateway.repository.PaymentsRepository;
import java.time.Year;
import java.time.YearMonth;
import java.util.Set;
import java.util.UUID;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.annotation.Value;

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
    
    // Validate the payment request
    validatePaymentRequest(paymentRequest);
    
    // Call the acquiring bank
    BankResponse bankResponse = callAcquiringBank(paymentRequest);
    
    // Create payment response
    PostPaymentResponse paymentResponse = new PostPaymentResponse();
    paymentResponse.setId(UUID.randomUUID());
    paymentResponse.setStatus(bankResponse.isAuthorized() ? PaymentStatus.AUTHORIZED : PaymentStatus.DECLINED);
    paymentResponse.setCardNumberLastFour(getLastFourDigits(paymentRequest.getCardNumber()));
    paymentResponse.setExpiryMonth(paymentRequest.getExpiryMonth());
    paymentResponse.setExpiryYear(paymentRequest.getExpiryYear());
    paymentResponse.setCurrency(paymentRequest.getCurrency());
    paymentResponse.setAmount(paymentRequest.getAmount());
    
    // Store the payment
    paymentsRepository.add(paymentResponse);
    
    LOG.info("Payment processed with ID: {} and status: {}", paymentResponse.getId(), paymentResponse.getStatus());
    
    return paymentResponse;
  }

  private void validatePaymentRequest(PostPaymentRequest request) {
    // Card number validation
    if (request.getCardNumber() == null || request.getCardNumber().isEmpty()) {
      throw new EventProcessingException("Card number is required");
    }
    
    if (request.getCardNumber().length() < 14 || request.getCardNumber().length() > 19) {
      throw new EventProcessingException("Card number must be between 14 and 19 characters long");
    }
    
    if (!request.getCardNumber().matches("\\d+")) {
      throw new EventProcessingException("Card number must only contain numeric characters");
    }

    // Expiry month validation
    if (request.getExpiryMonth() < 1 || request.getExpiryMonth() > 12) {
      throw new EventProcessingException("Expiry month must be between 1 and 12");
    }
    
    // Expiry year validation - must be in the future
    int currentYear = Year.now().getValue();
    int currentMonth = YearMonth.now().getMonthValue();
    
    if (request.getExpiryYear() < currentYear) {
      throw new EventProcessingException("Expiry year must be in the future");
    }
    
    // Validate that the combination of expiry month + year is in the future
    if (request.getExpiryYear() == currentYear && request.getExpiryMonth() < currentMonth) {
      throw new EventProcessingException("Card has expired - expiry date must be in the future");
    }
    
    // Currency validation
    if (request.getCurrency() == null || request.getCurrency().isEmpty()) {
      throw new EventProcessingException("Currency is required");
    }
    
    if (request.getCurrency().length() != 3) {
      throw new EventProcessingException("Currency must be 3 characters");
    }
    
    // Validate against a whitelist of ISO currency codes (limiting to 3 as per requirement)
    Set<String> allowedCurrencies = Set.of("GBP", "EUR", "USD");
    if (!allowedCurrencies.contains(request.getCurrency().toUpperCase())) {
      throw new EventProcessingException("Currency must be one of: USD, EUR, GBP");
    }
    
    // Amount validation
    if (request.getAmount() <= 0) {
      throw new EventProcessingException("Amount must be greater than zero");
    }
    
    // CVV validation
    String cvvStr = String.valueOf(request.getCvv());
    if (cvvStr.length() < 3 || cvvStr.length() > 4) {
      throw new EventProcessingException("CVV must be 3-4 characters long");
    }
    
    if (!cvvStr.matches("\\d+")) {
      throw new EventProcessingException("CVV must only contain numeric characters");
    }
  }
  
  private BankResponse callAcquiringBank(PostPaymentRequest request) {
    try {
      // Call the bank simulator

      String bankUrl = bankSimulatorUrl + "/payments";
      BankResponse response = restTemplate.postForObject(bankUrl, request, BankResponse.class);
      
      LOG.debug("Bank response: {}", response);
      return response;
      
    } catch (Exception e) {
      LOG.error("Error calling acquiring bank", e);
      throw new EventProcessingException("Failed to process payment with bank");
    }
  }
  
  private String getLastFourDigits(String cardNumber) {
    if (cardNumber == null || cardNumber.length() < 4) {
      return "";
    }
    return cardNumber.substring(cardNumber.length() - 4);
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
