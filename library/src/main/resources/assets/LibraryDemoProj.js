$(":button").click(function() {
	var isbn = this.id;
   $.ajax({
            url: "v1/books/"+isbn+"?status=lost",
            type: 'PUT',    
            contentType: 'application/json',
            success: function(result) {
                alert("Book status has been updated");
		document.getElementById(isbn+":status").innerHTML="lost";
		document.getElementById(isbn).disabled = true;
		
		
            }
        });
});