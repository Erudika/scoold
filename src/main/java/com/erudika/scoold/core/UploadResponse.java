package com.erudika.scoold.core;

public class UploadResponse {
	private String downloadUrl;

	public UploadResponse(String downloadUrl) {

		this.downloadUrl = downloadUrl;
	}

	public String getDownloadUrl() {
		return downloadUrl;
	}

	public void setDownloadUrl(String downloadUrl) {
		this.downloadUrl = downloadUrl;
	}
}
