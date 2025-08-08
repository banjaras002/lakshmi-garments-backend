package com.lakshmigarments.exception;

public class UserNotFoundException extends RuntimeException {

	private static final long serialVersionUID = 6398698581928176321L;
	
	public UserNotFoundException(String message) {
		super(message);
	}
}
