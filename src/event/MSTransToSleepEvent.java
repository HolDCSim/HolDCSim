package event;

import java.util.Vector;

import debug.Sim;
import infrastructure.AbstractSleepController;
import infrastructure.Core;
import infrastructure.Core.PowerState;
import infrastructure.ShallowDeepServer;
import infrastructure.GeneralSleepController;
import experiment.ShallowDeepExperiment;
import experiment.SingletonJobExperiment.IdleRecord;

/**
 * @author fanyao event for transit one server from active to sleep mode in
 *         MultiServerExperiment
 */
public class MSTransToSleepEvent extends AbstractEvent {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private ShallowDeepServer server;
	private int shallowSleepState;
	private int deepSleepState;
	private ShallowDeepExperiment mulExp;

	public MSTransToSleepEvent(double theTime,
			ShallowDeepExperiment anExperiment, ShallowDeepServer server,
			int shallowState, int deepState) {
		super(theTime, anExperiment);
		this.server = server;
		this.shallowSleepState = shallowState;
		this.deepSleepState = deepState;
		this.mulExp = anExperiment;
		// TODO Auto-generated constructor stub
	}

	@Override
	public void process() {
		// TODO Auto-generated method stub

		// server is in shallow sleep state
		/*modified for multicore scenario*/
		//if (!server.isServerBusy()) {
		if (! server.isServerBusy()) {
			StartSleepEvent startSleepEvent = new StartSleepEvent(
					time, mulExp, server, deepSleepState);
			AbstractSleepController sleepable = server.getSleepController();

			Vector<IdleRecord> idleDistribution = sleepable
					.getIdleDistribution();
			// //when there is no record, this means the server is in the intial
			// shallow sleep state
			// if(idleDistribution.size() == 0){
			// IdleRecord firstRecord = new IdleRecord();
			// firstRecord.s
			// }
			IdleRecord record = idleDistribution.lastElement();

			// updating the last sleep state record, which should be shallow
			// sleep state

			if (record.duration != IdleRecord.INITIAL
					|| record.sleepstate != shallowSleepState) {
				Sim.fatalError("attempt to update the wrong record");
			}
			// if (record.duration != 0.0
			// || record.sleepstate != shallowSleepState) {
			// Sim.fatalError("attempt to update the wrong record");
			// }
			record.sleepstate = sleepable.getCurrentSleepState();
			
			record.duration = time - record.startIdleTime;

			// wrap up the sleep time for original shallow sleep state
			// FIXME: only works when shallow sleep state is 1, check
			// implementation of accumulateSSTimes()
			sleepable.accumulateSSTimes(shallowSleepState,
					time - sleepable.getLastEnterSleepStateTime());
			// begin count the new sleepstate durations
			sleepable.setLastEnterSleepStateTime(0.0);

			/**
			 * this is not necessary since coreEnteredParkEvent would create the
			 * new record
			 */
			// begin new record
			// IdleRecord newRecord = new IdleRecord();
			// newRecord.startIdleTime = time;
			// idleDistribution.add(newRecord);

			// add park event to experiment
			mulExp.addEvent(startSleepEvent);
		}

		else {
			// if server is busy,it could be active or transition_to_active
			// when transtion_to_active, it means there is a MSTransToActive
			// event

			// active/wakeup --> sleep
			// MSTransToActiveEvent toActiveEvent = server.getToActiveEvent();
			// if (toActiveEvent != null)
			// mulExp.cancelEvent(toActiveEvent);
			// server.setToActiveEvent(null);

			// FIXME: if the shallow sleepstate is not 1, the server may be
			// currently
			// transitioning out of shallow sleep state, then we need to
			// distinguish
			// from which sleepstate it is transitioning from

			// if the active server just finished a job and called
			// updateThresholds
			server.setNextFixedSleepState(deepSleepState);
			// EnergyAwareServer eeServer = (EnergyAwareServer) server;
			// CoreStartSleepEvent event = eeServer.getSingelSleepEvent();
			// if (event != null) {
			// if (event.getSleepState() != 6) {
			// Sim.fatalError("fatal error, try to put a deep sleep server to deepsleeparrays");
			// }
			//
			// }
			if (server.isTransitionToActive()) {

				server.setToSleepEvent(true);
			}
		}

	}
}
