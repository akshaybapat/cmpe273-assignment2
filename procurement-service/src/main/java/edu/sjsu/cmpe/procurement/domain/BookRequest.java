package edu.sjsu.cmpe.procurement.domain;

public class BookRequest {

	public String id;
	public String order_book_isbns[];
	
	public BookRequest(String id, String[] bookList)
	{
		this.id = id;
		this.order_book_isbns = bookList;
	}
	
}
