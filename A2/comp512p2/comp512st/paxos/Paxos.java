package comp512st.paxos;

// Access to the GCL layer
import comp512.gcl.*;

import comp512.utils.*;

// Any other imports that you may need.
import java.io.*;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.logging.*;
import java.net.UnknownHostException;
import java.lang.Thread;

// ANY OTHER classes, etc., that you add must be private to this package and not visible to the application layer.

// extend / implement whatever interface, etc. as required.
// NO OTHER public members / methods allowed. broadcastTOMsg, acceptTOMsg, and shutdownPaxos must be the only visible methods to the application layer.
//		You should also not change the signature of these methods (arguments and return value) other aspects maybe changed with reasonable design needs.
public class Paxos {
	GCL gcl;
	FailCheck failCheck;
	int roundNumber = 0;
	int majority = 0;
	Deque<PlayerMoveData> deque = new ArrayDeque<>();
	Queue<PlayerMoveData> deliveryQueue = new LinkedList<>();

	public Paxos(String myProcess, String[] allGroupProcesses, Logger logger, FailCheck failCheck)
			throws IOException, UnknownHostException {
		// Rember to call the failCheck.checkFailure(..) with appropriate arguments
		// throughout your Paxos code to force fail points if necessary.
		this.failCheck = failCheck;
		System.out.println("NUM PROCESSES: " + allGroupProcesses.length);

		// Initialize the GCL communication system as well as anything else you need to.
		this.gcl = new GCL(myProcess, allGroupProcesses, null, logger);
		this.majority = calculateMajority(allGroupProcesses.length);

		PaxosListener pl = new PaxosListener(deque, gcl);
		Thread plThread = new Thread(pl);
		plThread.start();
	}

	private int calculateMajority(int numProcesses) {
		return (numProcesses / 2) + 1;
	}

	// This is what the application layer is going to call to send a message/value,
	// such as the player and the move
	public void broadcastTOMsg(Object val) throws InterruptedException {
		// This is just a place holder.
		// Extend this to build whatever Paxos logic you need to make sure the messaging
		// system is total order.
		// Here you will have to ensure that the CALL BLOCKS, and is returned ONLY when
		// a majority (and immediately upon majority) of processes have accepted the
		// value.
		Object[] vals = (Object[]) val;
		PlayerMoveData playerMoveData = new PlayerMoveData((int) vals[0], (char) vals[1]);
		deque.addLast(playerMoveData);

		// start new paxos instance
		while (!deque.isEmpty()) {
			roundNumber++;
			propose();

			// wait for majority to accept
			accept();

			// broadcast confirm
			confirm();
		}
	}

	private void propose() throws InterruptedException {
		// propose to be leader, ie round value
		Proposal proposal = new Proposal(roundNumber);

		gcl.broadcastMsg(proposal);
		int count = 0;
		List<Promise> promisesWithAcceptedRound = new ArrayList<>();
		while (count < this.majority) {
			GCMessage gcmsg = gcl.readGCMessage();
			Promise promise = (Promise) gcmsg.val;
			if (promise.receivedRoundNumber != roundNumber)
				continue;
			if (promise.acceptedRoundNumber != -1) {
				promisesWithAcceptedRound.add(promise);
			}
			count++;
		}
		promisesWithAcceptedRound.sort((p1, p2) -> Integer.compare(p1.acceptedRoundNumber, p2.acceptedRoundNumber));

		for (int i = promisesWithAcceptedRound.size() - 1; i >= 0; i--) {
			deque.addFirst(promisesWithAcceptedRound.get(i).acceptedValue);
		}
		return;
	}

	private void accept() throws InterruptedException {
		PlayerMoveData pmd = deque.removeFirst();

		Accept accept = new Accept(roundNumber, pmd);
		gcl.broadcastMsg(accept);
		int count = 0;
		while (count < this.majority) {
			GCMessage gcmsg = gcl.readGCMessage();
			AcceptAck acceptAck = (AcceptAck) gcmsg.val;
			if (acceptAck.roundNumber != roundNumber)
				continue;
			count++;
		}
		return;
	}

	private void confirm() throws InterruptedException {
		Confirm confirm = new Confirm(roundNumber);
		gcl.broadcastMsg(confirm);
		return;
	}

	// This is what the application layer is calling to figure out what is the next
	// message in the total order.
	// Messages delivered in ALL the processes in the group should deliver this in
	// the same order.
	public Object acceptTOMsg() throws InterruptedException {
		// This is just a place holder.
		while (deliveryQueue.isEmpty()) {
		}
		PlayerMoveData pmd = deliveryQueue.remove();
		return new Object[] { pmd.player, pmd.move };
	}

	// Add any of your own shutdown code into this method.
	public void shutdownPaxos() {
		gcl.shutdownGCL();
	}
}

class PaxosListener implements Runnable {

	Deque<PlayerMoveData> deque;
	GCL gcl;

	public PaxosListener(Deque<PlayerMoveData> deque, GCL gcl) {
		this.deque = deque;
		this.gcl = gcl;
	}

	public void run() {
		while (true) {
			try {
				GCMessage gcmsg = gcl.readGCMessage();

				PaxosPhase val = (PaxosPhase) gcmsg.val;
				System.out.println(val + " " + val.type);
				// switch (val.type) {
				// case "PROPOSAL": {

				// Proposal p = (Proposal) val;
				// System.out.println("rn " + p.roundNumber);
				// break;
				// }
				// case "ACCEPT": {

				// break;
				// }
				// case "CONFIRM": {

				// break;
				// }
				// default:
				// break;
				// }

			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

}

abstract class PaxosPhase implements Serializable {
	String type;
}

class Promise {
	int receivedRoundNumber;
	int acceptedRoundNumber; // -1 for none
	PlayerMoveData acceptedValue;

	public Promise() {

	}
}

class Proposal extends PaxosPhase {
	int roundNumber;
	String type;

	public Proposal(int roundNumber) {
		this.roundNumber = roundNumber;
		this.type = "PROPOSAL";
	}
}

class PlayerMoveData {
	int player;
	char move;

	public PlayerMoveData(int player, char move) {
		this.player = player;
		this.move = move;
	}
}

class Accept extends PaxosPhase {
	int roundNumber;
	PlayerMoveData pmd;
	String type;

	public Accept(int roundNumber, PlayerMoveData pmd) {
		this.roundNumber = roundNumber;
		this.pmd = pmd;
		this.type = "ACCEPT";
	}
}

class AcceptAck {
	int roundNumber;

	public AcceptAck(int roundNumber) {
		this.roundNumber = roundNumber;
	}
}

class Confirm extends PaxosPhase {
	int roundNumber;
	String type;

	public Confirm(int roundNumber) {
		this.roundNumber = roundNumber;
		this.type = "CONFIRM";
	}
}
