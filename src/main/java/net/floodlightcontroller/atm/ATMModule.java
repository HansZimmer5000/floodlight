package net.floodlightcontroller.atm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

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
		// This module does not implement any new services
		return null;
	}

	@Override
	public Map<Class<? extends IFloodlightService>, IFloodlightService> getServiceImpls() {
		// This module does not implement any new services
		return null;
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
		IOFSwitchService switchService = context.getServiceImpl(IOFSwitchService.class);
		IRestApiService restAPIService = context.getServiceImpl(IRestApiService.class);
		
		
		RestletRoutable routable = createRestAPI(switchService);
		restAPIService.addRestletRoutable(routable );
	}
	
	private RestletRoutable createRestAPI(IOFSwitchService switchService){
		RestletRoutable ATMRouable = new ATMRoutable();
		return ATMRouable;
	}

}
