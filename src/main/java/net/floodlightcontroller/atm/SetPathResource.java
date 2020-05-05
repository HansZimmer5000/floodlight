package net.floodlightcontroller.atm;

import net.floodlightcontroller.core.internal.IOFSwitchService;

import org.restlet.data.Status;
import org.restlet.resource.Put;
import org.restlet.resource.ServerResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SetPathResource extends ServerResource {
	protected static Logger log = LoggerFactory.getLogger(SetPathResource.class);
	
	@Put
	public String SetPath(String jsonBody){
		log.debug("SetPathReceived:" + jsonBody);
		Status status = Status.CLIENT_ERROR_BAD_REQUEST;
		String message = "Error unkown";
		
		IOFSwitchService switchService =
                (IOFSwitchService)getContext().getAttributes().
                    get(IOFSwitchService.class.getCanonicalName());
		
		// For Parameters: String param = (String) getRequestAttributes().get("switch");
		
		//TODO Extract Info from JSON
		//rowValues = StaticEntries.jsonToStorageEntry(jsonBody);
		
		//TODO Set Path according to ASP
		
		setStatus(status, message);
		return null; // Returning null will still send a response but it seems broken / failure
	}
	
}
