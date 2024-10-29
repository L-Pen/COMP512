package comp512st.paxos;

// Access to the GCL layer
import comp512.gcl.*;

import comp512.utils.*;

// Any other imports that you may need.
import java.io.*;
import java.util.ArrayDeque;
import java.util.ArrayList;
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
	boolean isLeader = false;
	int processId;
	String processName;

	public Paxos(String myProcess, String[] allGroupProcesses, Logger logger, FailCheck failCheck)
			throws IOException, UnknownHostException {
		// Rember to call the failCheck.checkFailure(..) with appropriate arguments
		// throughout your Paxos code to force fail points if necessary.
		this.failCheck = failCheck;
		this.processId = Integer.parseInt(myProcess.split(":")[1]);
		this.processName = myProcess;
		System.out.println("NUM PROCESSES: " + allGroupProcesses.length + " HI PROCESS ID: " + processId);

		// Initialize the GCL communication system as well as anything else you need to.
		this.gcl = new GCL(myProcess, allGroupProcesses, null, logger);
		this.majority = calculateMajority(allGroupProcesses.length);

		PaxosListener pl = new PaxosListener(this);
		Thread plThread = new Thread(pl);
		plThread.start();

		PaxosBroadcaster pb = new PaxosBroadcaster(this);
		Thread pbThread = new Thread(pb);
		pbThread.start();
	}

	private int calculateMajority(int numProcesses) {
		return (numProcesses / 2) + 1;
	}

	// This is what the application layer is going to call to send a message/value,
	// such as the player and the move
	public void broadcastTOMsg(Object val) throws InterruptedException {
		Object[] vals = (Object[]) val;
		PlayerMoveData playerMoveData = new PlayerMoveData((int) vals[0], (char) vals[1]);
		deque.addLast(playerMoveData);
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

	Paxos paxos;

	public PaxosListener(Paxos paxos) {
		this.paxos = paxos;
	}

	public void run() {
		while (true) {
			try {
				GCMessage gcmsg = paxos.gcl.readGCMessage();
				// hello liamo

				Object val = gcmsg.val;
				if (val instanceof Proposal) {
					Proposal p = (Proposal) val;
					int rountNumber = p.roundNumber;
					System.out.println(rountNumber);
					// if()
				}
				if (val instanceof LeaderElection) {
					LeaderElection le = (LeaderElection) val;
					if (paxos.processId > le.processId && !paxos.deque.isEmpty()) {

					}
				}

			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}

class LeaderElection implements Serializable {
	int processId;
	String processName;

	public LeaderElection(int processId, String processName) {
		this.processId = processId;
		this.processName = processName;
	}
}

class PaxosBroadcaster implements Runnable {
	Paxos paxos;
	boolean startedLeaderElection = false;

	public PaxosBroadcaster(Paxos paxos) {
		this.paxos = paxos;
	}

	public void run() {
		while (true) {
			// start new paxos instance
			if (paxos.deque.isEmpty())
				continue;

			// start leader election
			if (!startedLeaderElection) {
				LeaderElection le = new LeaderElection(paxos.processId, paxos.processName);
				paxos.gcl.broadcastMsg(le);
				startedLeaderElection = true;
			}

			if (!paxos.isLeader)
				continue;

			startedLeaderElection = false;
			paxos.roundNumber++;
			try {
				propose();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			// wait for majority to accept
			try {
				accept();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			// broadcast confirm
			try {
				confirm();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			paxos.isLeader = false;
		}
	}

	private void propose() throws InterruptedException {
		// propose to be leader, ie round value
		Proposal proposal = new Proposal(paxos.roundNumber);

		paxos.gcl.broadcastMsg(proposal);
		int count = 0;
		List<Promise> promisesWithAcceptedRound = new ArrayList<>();
		while (count < paxos.majority) {
			GCMessage gcmsg = paxos.gcl.readGCMessage();
			Promise promise = (Promise) gcmsg.val;
			if (promise.receivedRoundNumber != paxos.roundNumber)
				continue;
			if (promise.acceptedRoundNumber != -1) {
				promisesWithAcceptedRound.add(promise);
			}
			count++;
		}
		promisesWithAcceptedRound.sort((p1, p2) -> Integer.compare(p1.acceptedRoundNumber, p2.acceptedRoundNumber));

		for (int i = promisesWithAcceptedRound.size() - 1; i >= 0; i--) {
			paxos.deque.addFirst(promisesWithAcceptedRound.get(i).acceptedValue);
		}
		return;
	}

	private void accept() throws InterruptedException {
		PlayerMoveData pmd = paxos.deque.removeFirst();

		Accept accept = new Accept(paxos.roundNumber, pmd);
		paxos.gcl.broadcastMsg(accept);
		int count = 0;
		while (count < paxos.majority) {
			GCMessage gcmsg = paxos.gcl.readGCMessage();
			AcceptAck acceptAck = (AcceptAck) gcmsg.val;
			if (acceptAck.roundNumber != paxos.roundNumber)
				continue;
			count++;
		}
		return;
	}

	private void confirm() throws InterruptedException {
		Confirm confirm = new Confirm(paxos.roundNumber);
		paxos.gcl.broadcastMsg(confirm);
		return;
	}
}

class Promise implements Serializable {
	int receivedRoundNumber;
	int acceptedRoundNumber; // -1 for none
	PlayerMoveData acceptedValue;

	public Promise() {

	}
}

class Proposal implements Serializable {
	int roundNumber;

	public Proposal(int roundNumber) {
		this.roundNumber = roundNumber;
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

class Accept implements Serializable {
	int roundNumber;
	PlayerMoveData pmd;

	public Accept(int roundNumber, PlayerMoveData pmd) {
		this.roundNumber = roundNumber;
		this.pmd = pmd;
	}
}

class AcceptAck implements Serializable {
	int roundNumber;

	public AcceptAck(int roundNumber) {
		this.roundNumber = roundNumber;
	}
}

class Confirm implements Serializable {
	int roundNumber;

	public Confirm(int roundNumber) {
		this.roundNumber = roundNumber;
	}
}
