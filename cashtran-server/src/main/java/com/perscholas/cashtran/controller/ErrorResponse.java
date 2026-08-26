package com.perscholas.cashtran.controller;

import java.util.List;

/**
 * Standard error response returned by the API
 */
public class ErrorResponse {

  private String timestamp;
  private int status;
  private String error;
  private String message;
  private String path;
  private List<FieldError> errors;

  public ErrorResponse() {}

  public ErrorResponse(
      String timestamp,
      int status,
      String error,
      String message,
      String path,
      List<FieldError> errors) {
    this.timestamp = timestamp;
    this.status = status;
    this.error = error;
    this.message = message;
    this.path = path;
    this.errors = errors;
  }

  public String getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(String timestamp) {
    this.timestamp = timestamp;
  }

  public int getStatus() {
    return status;
  }

  public void setStatus(int status) {
    this.status = status;
  }

  public String getError() {
    return error;
  }

  public void setError(String error) {
    this.error = error;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }

  public List<FieldError> getErrors() {
    return errors;
  }

  public void setErrors(List<FieldError> errors) {
    this.errors = errors;
  }

  public static class FieldError {
    private String field;
    private Object rejectedValue;
    private String message;

    public FieldError() {}

    public FieldError(String field, Object rejectedValue, String message) {
      this.field = field;
      this.rejectedValue = rejectedValue;
      this.message = message;
    }

    public String getField() {
      return field;
    }

    public void setField(String field) {
      this.field = field;
    }

    public Object getRejectedValue() {
      return rejectedValue;
    }

    public void setRejectedValue(Object rejectedValue) {
      this.rejectedValue = rejectedValue;
    }

    public String getMessage() {
      return message;
    }

    public void setMessage(String message) {
      this.message = message;
    }
  }
}

