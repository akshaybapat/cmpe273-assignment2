package edu.sjsu.cmpe.procurement;

import de.spinscale.dropwizard.jobs.Job;
import de.spinscale.dropwizard.jobs.annotations.Every;

@Every("50s")
public class Reciever extends Job
{

@Override
public void doJob()  {
	
	try {
		String message = ProcurementService.checkMessage();
		ProcurementService.processmsg(message);
		String[] bookTopic = ProcurementService.checkArrival();
		ProcurementService.sendMessagebyTopic(bookTopic);
	} catch (Exception e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
}
 
 }