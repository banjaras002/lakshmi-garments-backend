package com.lakshmigarments.exception;

public class CategoryNotFoundException extends RuntimeException {

	private static final long serialVersionUID = -5665256588630768494L;
	
	public CategoryNotFoundException(String message) {
		super(message);
	}

}
