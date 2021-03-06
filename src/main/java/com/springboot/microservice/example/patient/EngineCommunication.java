package com.springboot.microservice.example.patient;

import java.util.ArrayList;

import com.declarativa.interprolog.SWISubprocessEngine;

public class EngineCommunication implements Runnable{
	private String G;
	private String T;
	private String GG;
	private Object[] solutions = new Object [100];
	private static SWISubprocessEngine engine;
	private String queryResult;
		
	private int assertionCount = 0;

	public EngineCommunication() {
		
		engine = new SWISubprocessEngine("/usr/bin");
		
		new Thread(this, "EngineT").start();
	}
	
	public void turnOff() {
		engine.shutdown();
	}


	public void run() {
		
	}
	

	public int query(String queryMsg, String queryResponseFormat) 
			throws Throwable{
		setGoal(queryMsg, queryResponseFormat);
        return resolve();
	}

	public void addFact(String assertion) {
		engine.command(assertion);
		updateAssertionCount(assertion);
	}	
	
	public String getQueryResult() {
		return queryResult;
	}
	
	private int resolve() throws Throwable{
		solutions = (Object[]) engine.deterministicGoal(GG,"[LM]")[0];
		ArrayList<Object> solutionList = new ArrayList<Object>();
		String queryResult = "Solution length: " + solutions.length 
				+ ".\n\n";
		for(int i=0; i < solutions.length; i++) {
			if (!solutionList.contains(solutions[i])) {
				solutionList.add(solutions[i]);
				queryResult += "(" + solutions[i] + ")\n\n";
			}
		}
		this.queryResult = queryResult;
		return solutions.length;
	}

	private void updateAssertionCount(String assertion) {
		if (assertion.startsWith("assert")) {
			assertionCount++;
		}
		if (assertion.startsWith("retract")) {
			assertionCount--;
		}
		return;		
	}

	public int getAssertionCount(){
		return assertionCount;
	}
	

	private void setGoal(String goal, String queryResponseFormat) {
		this.G = goal;
		this.T = queryResponseFormat;
		this.GG = "findall(TM, ("+G+",buildTermModel("+T+",TM)), L), "
				+ "ipObjectSpec('ArrayOfObject',L,LM)";
		return;
	}
	

	public ArrayList<Long> getTimes(String goal, String queryResponseFormat) {
		G = goal;
		T = queryResponseFormat;
		GG = "findall(TM, ("+G+",buildTermModel("+T+",TM)), L), "
				+ "ipObjectSpec('ArrayOfObject',L,LM)";
		solutions = (Object[]) engine.deterministicGoal(GG,"[LM]")[0];  // Results of query are stored here
		
		System.out.println("GET TIMES() SOLUTION FOR " + G);
		// Convert Object to String to Long
		ArrayList<Long> solutionList = new ArrayList<Long>();
		for(int i=0; i < solutions.length; i++) {
			String temp = solutions[i].toString();
			System.out.println(temp);
			if (!solutionList.contains(temp)) {
				
				solutionList.add(Long.parseLong(temp));
			}
		}
		
		return solutionList;
	}
	
	//Function to return list of string
	public ArrayList<String> lg(String goal, String queryResponseFormat) {
		G = goal;
		T = queryResponseFormat;
		GG = "findall(TM, ("+G+",buildTermModel("+T+",TM)), L), "
				+ "ipObjectSpec('ArrayOfObject',L,LM)";
		solutions = (Object[]) engine.deterministicGoal(GG,"[LM]")[0];  // Results of query are stored here
		
		System.out.println("GET TIMES() SOLUTION FOR " + G);
		// Convert Object to String
		ArrayList<String> solutionList = new ArrayList<String>();
		for(int i=0; i < solutions.length; i++) {
			String temp = solutions[i].toString();
			System.out.println(temp);
			if (!solutionList.contains(temp)) {
				
				solutionList.add(temp);
			}
		}
		
		return solutionList;
	}
	
	public long[][] getTimes2(String goal, String queryResponseFormat, int triggersNum){
		G = goal;
		T = queryResponseFormat;
		GG = "findall(TM, ("+G+",buildTermModel("+T+",TM)), L), "
				+ "ipObjectSpec('ArrayOfObject',L,LM)";
		solutions = (Object[]) engine.deterministicGoal(GG,"[LM]")[0];
		
		
		return new long[2][triggersNum];
	}
}

