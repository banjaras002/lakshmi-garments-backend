package com.lakshmigarments.exception;

public class SubCategoryNotFoundException extends RuntimeException {

	private static final long serialVersionUID = 7655841883625948818L;

	public SubCategoryNotFoundException(String message) {
		super(message);
	}
}
