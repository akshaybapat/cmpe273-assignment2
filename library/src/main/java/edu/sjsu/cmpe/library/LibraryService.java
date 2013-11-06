package edu.sjsu.cmpe.library;

import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.fusesource.stomp.jms.StompJmsConnectionFactory;
import org.fusesource.stomp.jms.StompJmsDestination;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yammer.dropwizard.Service;
import com.yammer.dropwizard.assets.AssetsBundle;
import com.yammer.dropwizard.config.Bootstrap;
import com.yammer.dropwizard.config.Environment;
import com.yammer.dropwizard.views.ViewBundle;

import edu.sjsu.cmpe.library.api.resources.BookResource;
import edu.sjsu.cmpe.library.api.resources.RootResource;
import edu.sjsu.cmpe.library.config.LibraryServiceConfiguration;
import edu.sjsu.cmpe.library.domain.Book;
import edu.sjsu.cmpe.library.domain.Book.Status;
import edu.sjsu.cmpe.library.dto.BookDto;
import edu.sjsu.cmpe.library.repository.BookRepository;
import edu.sjsu.cmpe.library.repository.BookRepositoryInterface;
import edu.sjsu.cmpe.library.ui.resources.HomeResource;

import javax.jms.Connection;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;

public class LibraryService extends Service<LibraryServiceConfiguration> {

    private final Logger log = LoggerFactory.getLogger(getClass());
    
    static String queueName; 
	static String topicName ;
	static String user ;
	static String password;
	static String host;
	static String apolloPort ;
	static String instance;
	static BookRepository bookRepository;

    public static void main(String[] args) throws Exception {
	new LibraryService().run(args);
	
	 int numThreads = 2;
	 ExecutorService executor = Executors.newFixedThreadPool(numThreads);
	  
	 Runnable backgroundTask = new Runnable() {
	  
	 @Override
	 public void run() {
	 System.out.println("Hello World");
	 
	 try{
	 StompJmsConnectionFactory factory = new StompJmsConnectionFactory();
	    factory.setBrokerURI("tcp://" + host + ":" + apolloPort);
	    Connection connection = factory.createConnection(user, password);
	    connection.start();
	    Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
	    Destination dest = new StompJmsDestination(topicName);
	    MessageConsumer consumer = session.createConsumer(dest);
	    System.out.println("Waiting for messages from " + topicName + "...");
	    String msgBody = "";
	 
	   
	   
	   while(true) {
	     
	     Message msg = consumer.receive();
	     
	     if( msg instanceof TextMessage ) {
	    	
	    	    msgBody = ((TextMessage) msg).getText();
	            System.out.println("Received message = " + msgBody);
	            String [] contents=msgBody.split(":",4);
	            Book book=new Book();
                book.setIsbn(Long.parseLong(contents[0]));
                book.setTitle(contents[1]);
                book.setCategory(contents[2]);
                URL url=new URL(contents[3]);
                book.setCoverimage(url);
                Book book1 = bookRepository.getBookByISBN(book.getIsbn());
                //if(!bookRepository.find(book1.getTitle(), book1))
                bookRepository.saveBook(book);
                
                  if(bookRepository.getBookByISBN(book.getIsbn()).getStatus().toString()=="lost")
                  {
                          bookRepository.getBookByISBN(book.getIsbn()).setStatus(Status.available);;
                  }
                  BookDto bookResponse = new BookDto(book1);
	         	      
	     
	     } else {
	            System.out.println("Unexpected message type: "+msg.getClass());
	            }
	        
	        
	            }
	 }catch (Exception e){}
	 }
	 };
	 
	 executor.execute(backgroundTask);
	}

    @Override
    public void initialize(Bootstrap<LibraryServiceConfiguration> bootstrap) {
	bootstrap.setName("library-service");
	bootstrap.addBundle(new ViewBundle());
	bootstrap.addBundle(new AssetsBundle("/assets"));
    }

    @Override
    public void run(LibraryServiceConfiguration configuration,
	    Environment environment) throws Exception {

    	// This is how you pull the configurations from library_x_config.yml
    	queueName = configuration.getStompQueueName();
    	topicName = configuration.getStompTopicName();
    	user = configuration.getApolloUser();
    	password = configuration.getApolloPassword();
    	host = configuration.getApolloHost();
    	apolloPort = configuration.getApolloPort();
    	instance = configuration.getInstance();
    	//log.debug("Queue name is {}. Topic name is {}", queueName,
        		//topicName);
    	/** Root API */
    	environment.addResource(RootResource.class);
    	/** Books APIs */
    	 bookRepository = new BookRepository();
    	environment.addResource(new BookResource(bookRepository));

    	/** UI Resources */
    	environment.addResource(new HomeResource(bookRepository));
    }
    
          	
        public static void sendMessage(String data) throws Exception 
    {
        	
        String message = "";
        message = instance + ":" + data;
    	StompJmsConnectionFactory factory = new StompJmsConnectionFactory();
        factory.setBrokerURI("tcp://" + host + ":" + apolloPort);

        Connection connection = factory.createConnection(user, password);
        connection.start();
        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        Destination dest = new StompJmsDestination(queueName);
        MessageProducer producer = session.createProducer(dest);
        producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);

        System.out.println("Sending messages to " + queueName + "...");
        TextMessage msg = session.createTextMessage(message);
        msg.setLongProperty("id", System.currentTimeMillis());
        producer.send(msg);
        connection.close();


    }
}
