package dev.resteasy.example;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

@Path("test")
public class SampleServer {

	@Path("echo")
	@POST
	public String echo(String s) {
		return s;
	}
}
