package net.floodlightcontroller.atm;


import org.restlet.data.Status;
import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;

public class HealthzResource extends ServerResource {

	@Get
	public String GetHealthz() {
		setStatus(Status.SUCCESS_OK);
		return "OK";
	}
}
