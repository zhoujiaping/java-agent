def obj = new Expando()
obj.doGet = {
	req,resp->
	PrintWriter pw = resp.getWriter();
	pw.print("hello hello");
	pw.flush();
}
obj
