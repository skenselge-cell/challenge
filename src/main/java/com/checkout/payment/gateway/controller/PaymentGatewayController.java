package com.checkout.payment.gateway.controller;

import com.checkout.payment.gateway.exception.EventProcessingException;
import com.checkout.payment.gateway.model.PostPaymentRequest;
import com.checkout.payment.gateway.model.PostPaymentResponse;
import com.checkout.payment.gateway.service.PaymentGatewayService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController("api")
public class PaymentGatewayController {

  private final PaymentGatewayService paymentGatewayService;

  public PaymentGatewayController(PaymentGatewayService paymentGatewayService) {
    this.paymentGatewayService = paymentGatewayService;
  }

  @GetMapping("/payment/{id}")
  public ResponseEntity<PostPaymentResponse> getPostPaymentEventById(@PathVariable UUID id) {
    return new ResponseEntity<>(paymentGatewayService.getPaymentById(id), HttpStatus.OK);
  }

  @PostMapping
  public ResponseEntity<PostPaymentResponse> processPayment(@Valid @RequestBody PostPaymentRequest paymentRequest) {
    try {
      PostPaymentResponse response = paymentGatewayService.processPayment(paymentRequest);
      return ResponseEntity.ok(response);
    } catch (EventProcessingException e) {
      // Return 400 Bad Request for rejected payments
      return ResponseEntity.badRequest().build();
    }
  }
}
