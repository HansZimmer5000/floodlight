package net.floodlightcontroller.atm;

import org.restlet.Context;
import org.restlet.Restlet;
import org.restlet.data.Status;
import org.restlet.resource.Put;
import org.restlet.resource.ServerResource;
import org.restlet.routing.Router;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.floodlightcontroller.core.internal.IOFSwitchService;
import net.floodlightcontroller.restserver.RestletRoutable;

public class ATMRoutable implements RestletRoutable {
	private Logger log = LoggerFactory.getLogger(ATMRoutable.class);
	
	@Override
	public Restlet getRestlet(Context context) {
        Router router = new Router(context);
        
        router.attach("/path", SetPathResource.class);
        
        return router;
	}

	@Override
	public String basePath() {
		return "/atm";
	}
	
	public class SetPathResource extends ServerResource {
		private Logger log = LoggerFactory.getLogger(SetPathResource.class);
		
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
			return null;
		}
		
	}

}
