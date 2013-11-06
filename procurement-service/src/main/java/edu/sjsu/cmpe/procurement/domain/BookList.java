package edu.sjsu.cmpe.procurement.domain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class BookList {

HashMap shipped_books;

public BookList()
{
	this.shipped_books = new HashMap();
}

public void displayBooks()
{
	for (int i=0;i<shipped_books.size();i++)
	System.out.println("Book"+i+" :"+shipped_books.get(i));
}

}
