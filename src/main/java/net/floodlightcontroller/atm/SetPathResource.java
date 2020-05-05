package net.floodlightcontroller.atm;

import java.util.ArrayList;
import java.util.List;

import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.internal.IOFSwitchService;

import org.restlet.data.Status;
import org.restlet.resource.Put;
import org.restlet.resource.ServerResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SetPathResource extends ServerResource {
	protected static Logger log = LoggerFactory
			.getLogger(SetPathResource.class);
	static final long ASP_TIMEOUT_MS = 1;

	@Put
	public String SetPath(String jsonBody) {
		log.debug("SetPathReceived:" + jsonBody);

		// TODO Set status and message if error occurs somehwere during
		// execution
		Status status = Status.SUCCESS_NO_CONTENT;
		String message = "";

		// Get Services
		IACIDUpdaterService updateService = (IACIDUpdaterService) this
				.getContext().getAttributes()
				.get(IACIDUpdaterService.class.getCanonicalName());

		IOFSwitchService switchService = (IOFSwitchService) this.getContext()
				.getAttributes().get(IOFSwitchService.class.getCanonicalName());

		// Extract info from Request
		String hosts = extractHosts(jsonBody);
		String path = extractPath(jsonBody);

		// Prepare Update
		List<IOFSwitch> affectedSwitches = extractSwitches(switchService, path);
		List<String> updates = calculateNetworkUpdates();
		Byte[] updateID = updateService.createNewUpdateIDAndPrepareMessages();
		List<MessagePair> messages = updateService.getMessages(updateID);

		if (messages == null) {
			log.debug("Messages was null!");
		} else {
			// Update
			updateNetwork(updateService, updates, affectedSwitches, messages);
		}

		// Respond to user
		setStatus(status, message);
		return message; // Returning null will still send a response but it
						// seems broken / failure
	}

	private void updateNetwork(IACIDUpdaterService updateService,
			List<String> updates, List<IOFSwitch> affectedSwitches,
			List<MessagePair> messages) {

		try {
			// Vote Lock and wait for commits
			List<IOFSwitch> unconfirmedSwitches = executeFirstPhase(
					updateService, affectedSwitches, messages);

			// Rollback where necessary
			executeSecondPhase(updateService, affectedSwitches,
					unconfirmedSwitches);

			// Commit and wait for finishes
			List<IOFSwitch> unfinishedSwitches = executeThirdPhase(
					updateService, affectedSwitches, messages);

			if (unfinishedSwitches.size() == 0) {
				// Everything OK
			} else {
				// TODO Something did not work out!
			}
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private List<IOFSwitch> executeFirstPhase(
			IACIDUpdaterService updateService,
			List<IOFSwitch> affectedSwitches, List<MessagePair> messages)
			throws InterruptedException {
		updateService.voteLock(affectedSwitches);
		Thread.sleep(ASP_TIMEOUT_MS);

		// TODO is variable 'messages' still up-to-date (reference) or old
		// (copy)?
		List<IOFSwitch> unconfirmedSwitches = getUnconfirmedSwitches(messages,
				affectedSwitches);
		return unconfirmedSwitches;
	}

	private void executeSecondPhase(IACIDUpdaterService updateService,
			List<IOFSwitch> affectedSwitches,
			List<IOFSwitch> unconfirmedSwitches) {
		if (unconfirmedSwitches.size() > 0) {
			// TODO Calculate readySwitches (affectedSwitches -
			// unconfirmedSwitches)
			List<IOFSwitch> readySwitches = new ArrayList<>();
			updateService.rollback(readySwitches);
		}
	}

	private List<IOFSwitch> executeThirdPhase(
			IACIDUpdaterService updateService,
			List<IOFSwitch> affectedSwitches, List<MessagePair> messages)
			throws InterruptedException {
		updateService.commit(affectedSwitches);
		Thread.sleep(1000); // TODO how long to wait? Or actively check
							// whats inside messages?

		// TODO is variable 'messages' still up-to-date (reference) or old
		// (copy)?
		List<IOFSwitch> unfinishedSwitches = getUnfinishedSwitches(messages,
				affectedSwitches);
		return unfinishedSwitches;
	}

	private List<IOFSwitch> extractSwitches(IOFSwitchService switchService,
			String path) {
		// TODO Auto-generated method stub
		return new ArrayList<>();
	}

	private List<IOFSwitch> getUnfinishedSwitches(List<MessagePair> messages,
			List<IOFSwitch> affectedSwitches) {
		// TODO Auto-generated method stub
		return new ArrayList<>();
	}

	private List<IOFSwitch> getUnconfirmedSwitches(List<MessagePair> messages,
			List<IOFSwitch> affectedSwitches) {
		// TODO Auto-generated method stub
		return new ArrayList<>();
	}

	private String extractHosts(String json) {
		// TODO
		return "NOT IMPLEMENTED YET";
	}

	private String extractPath(String json) {
		// TODO
		return "NOT IMPLEMENTED YET";
	}

	private List<String> calculateNetworkUpdates() {
		List<String> result = new ArrayList<>();
		// TODO
		return result;
	}

	private Byte calculateXID() {
		// TODO
		return 49;
	}

}
