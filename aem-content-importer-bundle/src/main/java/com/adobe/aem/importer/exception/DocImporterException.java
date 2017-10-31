/*******************************************************************************
* Copyright (c) 2014 Adobe Systems Incorporated. All rights reserved.
*
* Licensed under the Apache License 2.0.
* http://www.apache.org/licenses/LICENSE-2.0
******************************************************************************/

package com.adobe.aem.importer.exception;

public class DocImporterException extends Exception {

	private static final long serialVersionUID = 5928379475910555908L;

	public enum AEM_IMPORTER_EXCEPTION_TYPE {
		ERROR_PARAMS, INVALID_ZIP_FILE, UNEXPECTED;

		public String toString() {
			switch (this) {
			case ERROR_PARAMS:
				return "Error params";

			case INVALID_ZIP_FILE:
				return "Invalid zip file";

			case UNEXPECTED:
				return "Unexpected error";
			default:
				return "";
			}
		};

	}

	private AEM_IMPORTER_EXCEPTION_TYPE type;
	private String msg;
	private Exception exception;

	public DocImporterException(AEM_IMPORTER_EXCEPTION_TYPE type, String message) {
		this.type = type;
		this.msg = message;
	}

	public DocImporterException(AEM_IMPORTER_EXCEPTION_TYPE type, String message, Exception e) {
		this.type = type;
		this.msg = message;
		this.exception = e;
	}

	@Override
	public String getMessage() {
		return type.toString() + ": " + msg;
	}

	public AEM_IMPORTER_EXCEPTION_TYPE getType() {
		return type;
	}

	public void setType(AEM_IMPORTER_EXCEPTION_TYPE type) {
		this.type = type;
	}

	public String getMsg() {
		return msg;
	}

	public void setMsg(String msg) {
		this.msg = msg;
	}

	public Exception getException() {
		return exception;
	}

	public void setException(Exception exception) {
		this.exception = exception;
	}

}
