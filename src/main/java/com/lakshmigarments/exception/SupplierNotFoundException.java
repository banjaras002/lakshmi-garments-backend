package com.lakshmigarments.exception;

public class SupplierNotFoundException extends RuntimeException {

	private static final long serialVersionUID = -1291057072497733827L;

	public SupplierNotFoundException(String message) {
		super(message);
	}
}
