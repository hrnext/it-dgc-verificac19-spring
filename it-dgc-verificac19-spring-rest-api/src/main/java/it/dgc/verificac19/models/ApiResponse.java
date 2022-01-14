/**
 * 
 */
package it.dgc.verificac19.models;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import it.dgc.verificac19.model.CertificateSimple;

public class ApiResponse {

	private MessageCode messageCode;
	private CertificateSimple certificate;
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "CET")
	private LocalDateTime serverTime = LocalDateTime.now();
	
	private enum MessageCode {
		OK, UPDATE_DRL_IN_PROGRESS, CANNOT_DECODE_IMAGE
	}
	

	public CertificateSimple getCertificate() {
		return certificate;
	}
	
	public MessageCode getMessageCode() {
		return messageCode;
	}
	
	public LocalDateTime getServerTime() {
		return serverTime;
	}
	
	private void setMessageCode(MessageCode messageCode) {
		this.messageCode = messageCode;
	}
	
	private void setCertificate(CertificateSimple certificate) {
		this.certificate = certificate;
	}
	
	public static ApiResponse buildOkResponse(CertificateSimple certificate) {
		ApiResponse toReturn = new ApiResponse();
		toReturn.setCertificate(certificate);
		toReturn.setMessageCode(MessageCode.OK);
		return toReturn;
	}
	
	public static ApiResponse buildUpdatingResponse() {
		ApiResponse toReturn = new ApiResponse();
		toReturn.setMessageCode(MessageCode.UPDATE_DRL_IN_PROGRESS);
		return toReturn;
	}
	
	public static ApiResponse buildDecodeError() {
		ApiResponse toReturn = new ApiResponse();
		toReturn.setMessageCode(MessageCode.CANNOT_DECODE_IMAGE);
		return toReturn;
	}
	
	
	
}
	
