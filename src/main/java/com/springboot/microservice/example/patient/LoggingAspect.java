package com.springboot.microservice.example.patient;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Scanner;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;


@Aspect
@Configuration
public class LoggingAspect {
	
	private Logger logger = LoggerFactory.getLogger(this.getClass());
	static String localDBPath = 
			// "/home/sep/Desktop/src_microservice/spring-boot-microservice-patient-service/src/main/resources/local-db.txt";
			"/home/nickel/Desktop/test-system-1/spring-boot-microservice-patient-service/src/main/resources/local-db.txt";
	static String remoteDBPath =
			"/home/nickel/Desktop/test-system-1/spring-boot-microservice-patient-service/src/main/resources/remote-db.txt";
	static String logDBPath = 
			"/home/nickel/Desktop/test-system-1/spring-boot-microservice-patient-service/src/main/resources/log-db.txt";
	static String testDBPath = 
			"/home/nickel/Desktop/test-system-1/spring-boot-microservice-patient-service/src/main/resources/test-db.txt";

	
	@Autowired
	private Environment env;
	
	private RestClient restClinet = 
			new RestClient(new RestTemplateBuilder());
	
	private EngineCommunication ec;
	
	static String G = "loggedfunccall(X0,X1,X2,X3)"; // goal
	
	static String T = "(X0,X1,X2,X3)";	//response format
	
	static String LG = "lg(X0,X1,X2)"; 
	static String T_LG = "(X0, X1, X2)"; //(T0, T1, [P, U])
	
	//Negative Triggers:
	static String G1 = "funccall(X0, authorization-service, \"com.springboot.microservice.example.authorization.AuthorizationController.mendTheGlass\", X1)";
	static String T1 = "(X0)";
	private ArrayList<Long> mtgTimes = new ArrayList<>();
	
	//trigger aspects, if any

	//logging event aspects
	@Before("execution (* com.springboot.microservice.example.patient.PatientController.getPatientMedHistByName(..))")
	public void before0(JoinPoint joinPoint) throws Throwable	{
		// callEvent prefix realization		
		logger.info("Before aspect for {}", joinPoint);
		String precond = buildPreCond(joinPoint);
		appendFile(precond, localDBPath);
		logger.info(precond + " appended to local DB.");
		
		// send GET to triggers and collect the results
		// addPrecond prefix realization
		writeFile("", remoteDBPath);
		String content;
		content = restClinet.getPostsPlainJSON("http://localhost:8060/localdb");
		if (content != null) {
			appendFile(content, remoteDBPath);
		}
		logger.info(content + " appended to remote DB.");
				
		// initialize Prolog with LS, local and remote data
		// check if loggedfunccall is derivable and update log accordingly
		this.ec = new EngineCommunication();
		addLoggingSpecLG();
		addAssertionsFromDB(localDBPath);
		addAssertionsFromDB(remoteDBPath);
		
		ArrayList<Long> mtgTimes = ec.getTimes(G1, T1);
		System.out.println("\nFinal array:");
		for(Long x : mtgTimes)
			System.out.println(x);
		
		/* Query for MTG times and split String. Parse all String digits to Long and store in list */
//		ec.query(G1, T1); 
//		String result = ec.getQueryResult();
//		result = result.replaceAll("[^-?0-9]+", " ").trim(); // remove all non-digits and empty lines
//		String[] split = result.split(" ");
//		
//		long[] mtgTimes = new long[split.length - 1];
//		for(int i = 0; i<split.length-1; i++) { 
//			mtgTimes[i] = Long.parseLong(split[i+1]);
//		}
//		
//		
		/* Query for LG(T0, ..., Tn) and parse all Strings digits to Long and store in list */
		ec.query(LG, T_LG);
		writeFile(ec.getQueryResult(), testDBPath);	
		String result = ec.getQueryResult();
		result = result.replaceAll("[^-?0-9]+", " ").trim(); // remove all non-digits and empty lines
		String[] split_2 = result.split(" ");
		
		int counter = 1;
		long[][] lgTimes = new long[(split_2.length-1)/2][2]; // lgTimes[# of LG(T0, T1, [U,P])][# of positive triggers]	
		for(int i = 0; i<(split_2.length-1)/2; i++) {
			for(int j = 0; j<2; j++) {
				lgTimes[i][j] = Long.parseLong(split_2[counter++]);
			}
		}		
//		
		/** Check if Derivable */
		writeFile("", logDBPath);
		for(long[] _T : lgTimes) {
			Boolean derivable = true;	
			for(Long T2 : mtgTimes) { //if precondition of 1st negative trigger holds
				if(_T[1] < T2 && T2 < _T[0]) {
					derivable = false;
					break;
				}
			}
			
			if(derivable) { // If there was no breaks, log
				String goal = "loggedfunccall(" + String.valueOf(_T[0]) + ",X1,X2,X3)";
				System.out.println(goal);
				ec.query(goal, T);
				String queryResult = ec.getQueryResult();
				String log = queryResult.replaceAll("Var.",String.valueOf(_T[0]));
				System.out.println(log);
				appendFile(log, logDBPath);	
			}
		}

		ec.turnOff();
	}
	
	
	private void addLoggingSpecLG( ) {
		ec.addFact("dynamic lg/4 as incremental"); 
		ec.addFact("dynamic loggedfunccall/4 as incremental"); 
		ec.addFact("dynamic funccall/4 as incremental"); 
//		ec.addFact("dynamic funccall/4 as incremental"); 
		
		ec.addFact("assert((lg(T0, T1, [U, P])"
				+ " :- funccall(T0, patient-service, \"com.springboot.microservice.example.patient.PatientController.getPatientMedHistByName\", [U, P]),"
				+ " funccall(T1, authorization-service, \"com.springboot.microservice.example.authorization.AuthorizationController.breakTheGlass\", [U]),"
				+ " <(T1, T0), ==(U, user)))");
		
		ec.addFact("assert((loggedfunccall(T0, patient-service, \"com.springboot.microservice.example.patient.PatientController.getPatientMedHistByName\", [U, P])"
				+ " :- funccall(T0, patient-service, \"com.springboot.microservice.example.patient.PatientController.getPatientMedHistByName\", [U, P]),"
				+ " funccall(T1, authorization-service, \"com.springboot.microservice.example.authorization.AuthorizationController.breakTheGlass\", [U]),"
				+ " <(T1, T0), ==(U, user)))");
		
	}
	

//	private void addLoggingSpec( ) {
//		ec.addFact("dynamic loggedfunccall/4 as incremental"); 
//		ec.addFact("dynamic funccall/4 as incremental"); 
//		ec.addFact("dynamic funccall/4 as incremental"); 
//		
//		ec.addFact("assert((loggedfunccall(T0, patient-service, \"com.springboot.microservice.example.patient.PatientController.getPatientMedHistByName\", [U, P])"
//				+ " :- funccall(T0, patient-service, \"com.springboot.microservice.example.patient.PatientController.getPatientMedHistByName\", [U, P]),"
//				+ " funccall(T1, authorization-service, \"com.springboot.microservice.example.authorization.AuthorizationController.breakTheGlass\", [U]),"
//				+ " <(T1, T0), ==(U, user)))");
//		
//	}
	
	
	private void addAssertionsFromDB(String filePath) {
		
		BufferedReader reader;
		try {
			reader = new BufferedReader(new FileReader(filePath));
			String line = reader.readLine();
			while (line != null && line.length()>0) {
				logger.info("assert({})", line);
				ec.addFact("assert(" + line + ")");
				line = reader.readLine();
			}
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

	private String buildPreCond(JoinPoint joinPoint)  {
		String timestamp = ((Long) System.currentTimeMillis()).toString();
		String serviceName = 
				env.getRequiredProperty("spring.application.name");
		String methodName = "\"" + getMethodName( 
				joinPoint.getSignature().toString()) + "\"";
		String methodArg = arrayToString(joinPoint.getArgs());
		
		return "funccall(" + timestamp + 
				", " + serviceName +
				", "  + methodName + 
				", " + methodArg + ")";
	}
	
	private String arrayToString(Object[] array) {
		if (array.length == 0) return "[]";
		String str = "[" + array[0];
		for (int i = 1; i < array.length; i++) {
			str += ", " + array[i].toString().toLowerCase();
		}
		str += "]";
		return str;
	}
	
	private String getMethodName(String signature) {
		return (signature.split("\\s+")[1]).split("\\(")[0];
	}
	
	private void appendFile(String st, String filePath) 
			throws IOException {
		PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(filePath, true)));
		out.println(st);
		out.close();
	}
	
	private void writeFile(String st, String filePath) 
			throws IOException {
		PrintWriter out = new PrintWriter(filePath, "UTF-8");
		out.print(st);
		out.close();
	}


}
