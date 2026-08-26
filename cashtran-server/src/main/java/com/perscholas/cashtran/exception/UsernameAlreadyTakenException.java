package com.perscholas.cashtran.exception;

public class UsernameAlreadyTakenException extends RuntimeException {

  public UsernameAlreadyTakenException() {
    super("Username already taken");
  }
}

