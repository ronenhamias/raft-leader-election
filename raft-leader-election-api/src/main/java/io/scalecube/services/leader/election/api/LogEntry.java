package io.scalecube.services.leader.election.api;

import java.io.Serializable;

public class LogEntry implements Serializable{

	private long term;

	public LogEntry(long term){
		this.term = term;
	}
	
	public long term(){
		return term;
	}
}
