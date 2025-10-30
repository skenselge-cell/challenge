package com.checkout.payment.gateway.exception;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EventProcessingException extends RuntimeException {

  private static final Logger LOG = LoggerFactory.getLogger(EventProcessingException.class);

  public EventProcessingException(String message) {
    super(message);
    LOG.error("EventProcessingException: {}", message);
  }
}
