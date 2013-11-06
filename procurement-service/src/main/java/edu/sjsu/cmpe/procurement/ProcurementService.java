package edu.sjsu.cmpe.procurement;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;





import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.GenericType;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.yammer.dropwizard.Service;
import com.yammer.dropwizard.client.HttpClientBuilder;
import com.yammer.dropwizard.client.JerseyClientBuilder;
import com.yammer.dropwizard.config.Bootstrap;
import com.yammer.dropwizard.config.Environment;



import de.spinscale.dropwizard.jobs.Job;
import de.spinscale.dropwizard.jobs.JobsBundle;
import de.spinscale.dropwizard.jobs.annotations.Every;
import edu.sjsu.cmpe.procurement.config.ProcurementServiceConfiguration;
import edu.sjsu.cmpe.library.domain.Book;
import edu.sjsu.cmpe.procurement.domain.BookList;
import edu.sjsu.cmpe.procurement.domain.BookRequest;

import javax.jms.Connection;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.http.client.HttpClient;
import org.eclipse.jetty.util.ajax.JSON;
import org.fusesource.stomp.jms.StompJmsConnectionFactory;
import org.fusesource.stomp.jms.StompJmsDestination;
import org.fusesource.stomp.jms.message.StompJmsMessage;

public class ProcurementService extends Service<ProcurementServiceConfiguration>  {

    private final Logger log = LoggerFactory.getLogger(getClass());
    
    static String queueName; 
	static String topicName ;
	static String user ;
	static String password;
	static String host;
	static String apolloPort ;
					 

    public static void main(String[] args) throws Exception {
	new ProcurementService().run(args);
    }

    @Override
    public void initialize(Bootstrap<ProcurementServiceConfiguration> bootstrap) {
	bootstrap.setName("procurement-service");
	bootstrap.addBundle(new JobsBundle("edu.sjsu.cmpe.procurement"));
    }

    @Override
    public void run(ProcurementServiceConfiguration configuration,
	    Environment environment) throws Exception {
    	
    	Client client = new JerseyClientBuilder().using(configuration.getJerseyClientConfiguration())
                .using(environment)
                .build();
   // environment.addResource(new ExternalServiceResource(client));
	queueName = configuration.getStompQueueName();
	topicName = configuration.getStompTopicName();
	user = "admin";
	password = "password";
	host = "54.215.210.214";
	apolloPort = "61613";
	log.debug("Queue name is {}. Topic is {}. User is {}. Password is {}. Host is {}. Port is {}.", queueName, topicName,user,password, host,apolloPort);
	//checkMessage();
	    }
    
    public static String checkMessage() throws Exception
    {
    	StompJmsConnectionFactory factory = new StompJmsConnectionFactory();
	    factory.setBrokerURI("tcp://" + host + ":" + apolloPort);
	    Connection connection = factory.createConnection(user, password);
	    connection.start();
	    Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
	    Destination dest = new StompJmsDestination(queueName);
	    MessageConsumer consumer = session.createConsumer(dest);
	    System.out.println("Waiting for messages from " + queueName + "...");
	    String msgBody = "";
	    
	   long waitUntil = 5000;
	   while(true) {
	
	     Message msg = consumer.receive(waitUntil);
	     if( msg instanceof TextMessage ) {
	    	
	    	    msgBody = msgBody + ((TextMessage) msg).getText() + "#";
	            System.out.println("Received message = " + msgBody);
	         	      
	     } else if (msg == null) {
	    	 System.out.println("No new messages. Existing due to timeout - " + waitUntil / 1000 + " sec");
	    	 break;

	     } else {
	            System.out.println("Unexpected message type: "+msg.getClass());
	            }
	   
	        }
	    connection.close();
	    return msgBody;
	    
	    	         	
	    }

    public static void processmsg(String msgBody) throws Exception {
   
    if(!msgBody.isEmpty()){
    int counter = msgBody.split("#").length ;
    System.out.println("You are here. Message body is " + msgBody +". Counter is "+counter);
    String bookList[] = new String[counter + 1];
    String token [] = msgBody.split("#");
    
   for(int i =0; i < token.length; i++)
    {
    	//System.out.println(token[i]);
	    String temp[] = token[i].split(":");
    	bookList[i] = temp[1];
    	//System.out.println(bookList[i]);
		
	    
    }
    postOrder(bookList);
    }	
    }
    
	public static void postOrder(String[] bookList) throws Exception
   	{
		Client client = Client.create();
		 
		WebResource webResource = client
		   .resource("http://54.215.210.214:9000/orders");
				
		int n = bookList.length;
		if (n>0) n = n-1;
		String list = "[" ;
				for (int i=0;i<=n;i++)
					if(i == n) { list = list + bookList[i]; }
					else list = list + bookList[i] + ",";
				list = list + "]";
 
		System.out.println(list);
		
		BookRequest bookRequest = new BookRequest("20571",bookList);
 
		ClientResponse response = webResource.accept("application/json")
				                             .type("application/json")
				                             .entity(bookRequest, "application/json")
				                             .post(ClientResponse.class);
		
		 
		if (response.getStatus() != 200) {
			throw new RuntimeException("Failed : HTTP error code : "
			     + response.getStatus());
		}
 		
		System.out.println("Output from Server .... \n");
		String output = response.getEntity(String.class);
		System.out.println(output);
   	}
   	
	public static String[] checkArrival() throws Exception
   	{
	    
		Client client = Client.create();
		WebResource webResource = client
		   .resource("http://54.215.210.214:9000/orders/20571");
				
		Map<String,Book[]> collector;
		ClientResponse response = webResource.accept("application/json")
										     .type("application/json")
				                             .get(ClientResponse.class);
		
		 
		if (response.getStatus() != 200) {
			throw new RuntimeException("Failed : HTTP error code : "
			     + response.getStatus());
		}
 		
		System.out.println("Output from Publisher Server .... \n");
		collector = response.getEntity(new GenericType<Map<String, Book[]>>(){}) ;
		Book[] shipped_books = collector.get("shipped_books");
		String[] bookMessage = new String[shipped_books.length];
		
		for (int i=0;i<shipped_books.length;i++)
			{
			System.out.println("Book "+i+" : "+shipped_books[i].getTitle());
			bookMessage[i]= shipped_books[i].getIsbn() + ":" + shipped_books[i].getTitle() + ":" + shipped_books[i].getCategory() + ":" + shipped_books[i].getCoverimage() ;
			System.out.println(bookMessage[i]);
			}
		return bookMessage;
   	}
	
    public static void sendMessagebyTopic(String bookTopic[]) throws Exception 
 {
     	
     StompJmsConnectionFactory factory = new StompJmsConnectionFactory();
     factory.setBrokerURI("tcp://" + host + ":" + apolloPort);

     Connection connection = factory.createConnection(user, password);
     connection.start();
     Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
     for(int i=0;i<bookTopic.length;i++)
     {
     int noOfTokens = bookTopic[i].split(":").length;
     if(noOfTokens > 4){
     String token [] = bookTopic[i].split(":");
     Destination dest = new StompJmsDestination("/topic/20571.book."+token[2]);
     MessageProducer producer = session.createProducer(dest);
     producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT); 

     System.out.println("Sending messages to topic 20571.book."+token[2]);
    
     TextMessage msg = session.createTextMessage(bookTopic[i]);
     msg.setLongProperty("id", System.currentTimeMillis());
     producer.send(msg);
     }
     }
    connection.close();


 }
}
