package com.github.georgeTseng.apiPerformanceTest.exception;

public class CustomApplicationException extends RuntimeException {

  private String errorMessage;

  private Throwable errorCause;

  private Object[] params;

  public CustomApplicationException(String errorMessage) {
    this.errorMessage = errorMessage;
  }

  public CustomApplicationException(String errorMessage, Throwable errorCause) {
    this.errorMessage = errorMessage;
    this.errorCause = errorCause;
  }

  public CustomApplicationException(String errorMessage, Throwable errorCause, Object[] params) {
    this.errorMessage = errorMessage;
    this.errorCause = errorCause;
    this.params = params;
  }

  public String getErrorMessage() {
    return this.errorMessage;
  }

  public Throwable getErrorCause() {
    return this.errorCause;
  }

  public Object[] getParams() {
    return this.params;
  }

}
