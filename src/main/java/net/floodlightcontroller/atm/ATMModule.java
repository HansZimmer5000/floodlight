package net.floodlightcontroller.atm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.projectfloodlight.openflow.protocol.OFType;

import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.internal.IOFSwitchService;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.restserver.IRestApiService;
import net.floodlightcontroller.restserver.RestletRoutable;

public class ATMModule implements IFloodlightModule {

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleServices() {
		Collection<Class<? extends IFloodlightService>> l = new ArrayList<>();
	    l.add(IACIDUpdaterService.class);
	    return l;
	}

	@Override
	public Map<Class<? extends IFloodlightService>, IFloodlightService> getServiceImpls() {
		Map<Class<? extends IFloodlightService>, IFloodlightService> m = new HashMap<>();
		
		ACIDUpdaterService updaterService = new ACIDUpdaterService();
		m.put(IACIDUpdaterService.class, updaterService);
		return m;
	}

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleDependencies() {
		Collection<Class<? extends IFloodlightService>> serviceList =
				new ArrayList<Class<? extends IFloodlightService>>();
		    serviceList.add(IFloodlightProviderService.class);
		    serviceList.add(IOFSwitchService.class);
		    serviceList.add(IRestApiService.class);
		    return serviceList;
	}

	@Override
	public void init(FloodlightModuleContext context)
			throws FloodlightModuleException {
		// Nothing to do.
	}

	@Override
	public void startUp(FloodlightModuleContext context)
			throws FloodlightModuleException {
		// Get Implementations
		IOFSwitchService switchService = context.getServiceImpl(IOFSwitchService.class);
		IRestApiService restAPIService = context.getServiceImpl(IRestApiService.class);
		IFloodlightProviderService provider = context.getServiceImpl(IFloodlightProviderService.class);
		IACIDUpdaterService updater = context.getServiceImpl(IACIDUpdaterService.class);
		
		// Add REST
		RestletRoutable routable = createRestAPI(switchService);
		restAPIService.addRestletRoutable(routable );
		
		// Add OpenFlow Listener
		List<OFType> messageTypes = new ArrayList<>();
		messageTypes.add(OFType.ERROR); //TODO Add all types
		for (Iterator<OFType> iterator = messageTypes.iterator(); iterator.hasNext();) {
			OFType ofType = (OFType) iterator.next();
			provider.addOFMessageListener(ofType, updater);
		}
	}
	
	private RestletRoutable createRestAPI(IOFSwitchService switchService){
		RestletRoutable ATMRouable = new ATMRoutable();
		return ATMRouable;
	}

}
