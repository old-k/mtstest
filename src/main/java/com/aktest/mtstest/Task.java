package com.aktest.mtstest;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.UUID;

public class Task {
	private transient String uid;
	private String status;
	private Timestamp timestamp;
	public Task() {
		super();
		uid = UUID.randomUUID().toString();
		status = "created";
		timestamp = new Timestamp(new java.util.Date().getTime());
	}
	public Task(ResultSet rs) throws SQLException {
		uid = rs.getString(1);
		status = rs.getString(2);
		timestamp = rs.getTimestamp(3);
	}

	public String getUid() {
		return uid;
	}
	public void setUid(String uid) {
		this.uid = uid;
	}
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		timestamp = new Timestamp(new Date().getTime());
		this.status = status;
	}
	public Timestamp getTimestamp() {
		return timestamp;
	}
	public void setTimestamp(Timestamp timestamp) {
		this.timestamp = timestamp;
	}
	@Override
	public String toString() {
		return "Task [uid=" + uid + ", status=" + status + ", timestamp=" + timestamp + "]";
	}
}