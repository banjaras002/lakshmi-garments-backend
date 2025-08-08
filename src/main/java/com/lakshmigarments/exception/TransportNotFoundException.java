package com.lakshmigarments.exception;

public class TransportNotFoundException extends RuntimeException {

	private static final long serialVersionUID = 6398698581928176321L;
	
	public TransportNotFoundException(String message) {
		super(message);
	}
}
