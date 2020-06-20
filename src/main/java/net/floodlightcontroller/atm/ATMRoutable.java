package net.floodlightcontroller.atm;

import org.restlet.Context;
import org.restlet.Restlet;
import org.restlet.routing.Router;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.floodlightcontroller.restserver.RestletRoutable;

public class ATMRoutable implements RestletRoutable {
	private Logger log = LoggerFactory.getLogger(ATMRoutable.class);
	
	@Override
	public Restlet getRestlet(Context context) {
        Router router = new Router(context);
        
        router.attach("/atm/flows", SetPathResource.class);
        router.attach("/healthz", HealthzResource.class);
        
        return router;
	}
	
	

	@Override
	public String basePath() {
		return "";
	}
}
