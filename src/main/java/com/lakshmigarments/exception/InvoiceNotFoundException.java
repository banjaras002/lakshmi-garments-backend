package com.lakshmigarments.exception;

public class InvoiceNotFoundException extends RuntimeException {

	private static final long serialVersionUID = -7419156941784901473L;
	
	public InvoiceNotFoundException(String message) {
		super(message);
	}
}
