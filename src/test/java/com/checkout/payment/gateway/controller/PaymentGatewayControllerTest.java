package com.checkout.payment.gateway.controller;


import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.model.PostPaymentResponse;
import com.checkout.payment.gateway.repository.PaymentsRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@SpringBootTest
@AutoConfigureMockMvc
class PaymentGatewayControllerTest {

  @Autowired
  private MockMvc mvc;
  
  @Autowired
  private PaymentsRepository paymentsRepository;


  @BeforeEach
  void setUp() {
    // Clear repository before each test if needed
    // This ensures test isolation
  }

  @Test
  void whenPaymentWithIdExistThenCorrectPaymentIsReturned() throws Exception {
    // Given: A payment exists in the repository
    PostPaymentResponse payment = new PostPaymentResponse();
    payment.setId(UUID.randomUUID());
    payment.setAmount(1050);
    payment.setCurrency("USD");
    payment.setStatus(PaymentStatus.AUTHORIZED);
    payment.setCardNumberLastFour(4321);

    paymentsRepository.add(payment);

    // When: We request the payment by ID
    // Then: We should get the correct payment details
    mvc.perform(MockMvcRequestBuilders.get("/payment/" + payment.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(payment.getId().toString()))
        .andExpect(jsonPath("$.status").value(payment.getStatus().getName()))
        .andExpect(jsonPath("$.cardNumberLastFour").value(payment.getCardNumberLastFour()))
        .andExpect(jsonPath("$.currency").value(payment.getCurrency()))
        .andExpect(jsonPath("$.amount").value(payment.getAmount()));
  }

  @Test
  void whenPaymentWithIdDoesNotExistThen404IsReturned() throws Exception {
    // Given: A random UUID that doesn't exist in the repository
    UUID nonExistentId = UUID.randomUUID();

    // When: We request a payment that doesn't exist
    // Then: We should get a 404 with an error message
    mvc.perform(MockMvcRequestBuilders.get("/payment/" + nonExistentId))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").exists());
  }

  @Test
  void whenValidPaymentRequestIsSubmittedThenPaymentIsProcessed() throws Exception {
    // Given: A valid payment request
    String paymentRequest = """
        {
          "card_number": "4111111111111111",
          "expiry_date": "12/2025",
          "currency": "USD",
          "amount": 1050,
          "cvv": 123
        }
        """;

    // When: We submit the payment request
    // Then: Payment should be processed successfully
    mvc.perform(MockMvcRequestBuilders.post("/")
            .contentType(MediaType.APPLICATION_JSON)
            .content(paymentRequest))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").exists())
        .andExpect(jsonPath("$.status").exists())
        .andExpect(jsonPath("$.cardNumberLastFour").value(1111))
        .andExpect(jsonPath("$.currency").value("USD"))
        .andExpect(jsonPath("$.amount").value(1050));
  }

  @Test
  void whenInvalidCardNumberIsProvidedThenBadRequestIsReturned() throws Exception {
    // Given: A payment request with invalid card number (too short)
    String paymentRequest = """
        {
          "card_number": "4111",
          "expiry_date": "12/2025",
          "currency": "USD",
          "amount": 1050,
          "cvv": 123
        }
        """;

    // When: We submit the invalid payment request
    // Then: We should get a 400 Bad Request
    mvc.perform(MockMvcRequestBuilders.post("/")
            .contentType(MediaType.APPLICATION_JSON)
            .content(paymentRequest))
        .andExpect(status().isBadRequest());
  }

  @Test
  void whenInvalidCurrencyIsProvidedThenBadRequestIsReturned() throws Exception {
    // Given: A payment request with unsupported currency
    String paymentRequest = """
        {
          "card_number": "4111111111111111",
          "expiry_date": "12/2025",
          "currency": "JPY",
          "amount": 1050,
          "cvv": 123
        }
        """;

    // When: We submit the payment with unsupported currency
    // Then: We should get a 400 Bad Request
    mvc.perform(MockMvcRequestBuilders.post("/")
            .contentType(MediaType.APPLICATION_JSON)
            .content(paymentRequest))
        .andExpect(status().isBadRequest());
  }

  @Test
  void whenExpiredCardIsProvidedThenBadRequestIsReturned() throws Exception {
    // Given: A payment request with expired card
    String paymentRequest = """
        {
          "card_number": "4111111111111111",
          "expiry_date": "01/2020",
          "currency": "USD",
          "amount": 1050,
          "cvv": 123
        }
        """;

    // When: We submit the payment with expired card
    // Then: We should get a 400 Bad Request
    mvc.perform(MockMvcRequestBuilders.post("/")
            .contentType(MediaType.APPLICATION_JSON)
            .content(paymentRequest))
        .andExpect(status().isBadRequest());
  }

  @Test
  void whenInvalidCvvLengthIsProvidedThenBadRequestIsReturned() throws Exception {
    // Given: A payment request with invalid CVV (too short)
    String paymentRequest = """
        {
          "card_number": "4111111111111111",
          "expiry_date": "12/2025",
          "currency": "USD",
          "amount": 1050,
          "cvv": 12
        }
        """;

    // When: We submit the payment with invalid CVV
    // Then: We should get a 400 Bad Request
    mvc.perform(MockMvcRequestBuilders.post("/")
            .contentType(MediaType.APPLICATION_JSON)
            .content(paymentRequest))
        .andExpect(status().isBadRequest());
  }
}
