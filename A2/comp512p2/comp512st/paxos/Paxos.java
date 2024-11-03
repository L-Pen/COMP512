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
	volatile int roundNumber = 0;
	int majority = 0;
	volatile Deque<PlayerMoveData> deque = new ArrayDeque<>();
	volatile Queue<PlayerMoveData> deliveryQueue = new LinkedList<>();
	volatile boolean isLeader = false;
	int processId;
	String processName;
	Integer numberProcesses;
	volatile boolean startedLeaderElection = false;
	volatile boolean paxosInstanceRunning = false;
	volatile int acceptedRoundNumber;
	volatile PlayerMoveData acceptedValue;
	volatile int acceptAckCount;
	volatile List<Promise> promisesWithAcceptedRound;
	volatile int promiseCount;
	volatile PaxosPhase phase;
	volatile boolean killThread;
	volatile ArrayList<Long> avg = new ArrayList<>();
	volatile long start;

	public Paxos(String myProcess, String[] allGroupProcesses, Logger logger, FailCheck failCheck)
			throws IOException, UnknownHostException {
		// Rember to call the failCheck.checkFailure(..) with appropriate arguments
		// throughout your Paxos code to force fail points if necessary.
		this.failCheck = failCheck;
		this.processId = Integer.parseInt(myProcess.split(":")[1]);
		this.processName = myProcess;
		this.numberProcesses = allGroupProcesses.length;
		this.startedLeaderElection = false;
		this.paxosInstanceRunning = false;
		this.acceptedRoundNumber = -1;
		this.acceptedValue = null;
		this.acceptAckCount = 0;
		this.promisesWithAcceptedRound = new ArrayList<>();
		this.promiseCount = 0;
		this.phase = PaxosPhase.LEADER_ELECTION_ACK;
		this.killThread = false;
		this.start = 0;

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
		PlayerMoveData playerMoveData = new PlayerMoveData((int) vals[0], (char) vals[1], System.nanoTime());
		synchronized (this) {
			deque.addLast(playerMoveData);
		}
		System.out.println("[broadcastTOMsg] Added move to dequeue in: " +
				deque.peek().toString() + " deque size: "
				+ deque.size());
	}

	// This is what the application layer is calling to figure out what is the next
	// message in the total order.
	// Messages delivered in ALL the processes in the group should deliver this in
	// the same order.
	public Object acceptTOMsg() throws InterruptedException {
		// This is just a place holder.
		while (deliveryQueue.isEmpty()) {
		}
		PlayerMoveData pmd = null;
		synchronized (this) {
			pmd = deliveryQueue.remove();
		}
		System.out.println("[acceptTOMsg] delivering move: " + pmd.toString());
		return new Object[] { pmd.player, pmd.move };
	}

	// Add any of your own shutdown code into this method.
	public void shutdownPaxos() throws InterruptedException {
		this.killThread = true;
		gcl.shutdownGCL();
		Thread.sleep(10);
		int sum = 0;
		int size = avg.size();
		for (int i = 0; i < size; i++) {
			long x = avg.get(i);
			sum += x;
			System.out.println("Paxos runtime: " + x);
		}
		System.out.println("Paxos avg runtime: " + sum / size);
		System.out.println("Total Paxos runtime: " + sum);
	}
}

class PaxosListener implements Runnable {

	Paxos paxos;
	ArrayList<LeaderElectionAck> receivedLeAcks = new ArrayList<>();

	public PaxosListener(Paxos paxos) {
		this.paxos = paxos;
	}

	public void run() {
		while (!paxos.killThread) {
			try {
				GCMessage gcmsg = paxos.gcl.readGCMessage();

				Object val = gcmsg.val;

				PaxosLogger logger = new PaxosLogger(paxos);
				logger.addBreadcrumb("Paxos Listener");

				logger.log("Received payload: " + val.getClass().getName() + " from: "
						+ gcmsg.senderProcess);

				if (val instanceof LeaderElection) {
					logger.addBreadcrumb("Leader Election");
					if (paxos.paxosInstanceRunning) {
						logger.log("Paxos instance still running... send auto decline");
						LeaderElectionAck leaderElectionMessage = new LeaderElectionAck(false);
						paxos.gcl.sendMsg(leaderElectionMessage, gcmsg.senderProcess);
					} else {
						LeaderElection le = (LeaderElection) val;
						logger.log("leader election object: " + le.toString());
						PlayerMoveData pmd = paxos.deque.peekFirst();
						boolean pids = paxos.processId == le.processId;
						boolean pmdNull = pmd == null;
						boolean timestamps = !pmdNull && le.moveTimestamp < pmd.timestamp;
						boolean sameTimestamps = !pmdNull && le.moveTimestamp == pmd.timestamp
								&& le.processId > paxos.processId;
						logger.log(String.format(
								"Elect condition = pids: %b or pmd null: %b or timestamps: %b or sameTimestamps: %b",
								pids,
								pmdNull, timestamps, sameTimestamps));
						boolean elect = pids || pmdNull || timestamps || sameTimestamps;
						logger.log(String.format("Elect %d to be leader: %b",
								le.processId, elect));
						LeaderElectionAck leaderElectionMessage = new LeaderElectionAck(elect);
						paxos.gcl.sendMsg(leaderElectionMessage, gcmsg.senderProcess);
						paxos.failCheck.checkFailure(FailCheck.FailureType.AFTERSENDVOTE);
						logger.log(String.format(
								"Started leader election: %b, Paxos instance running: %b, deque empty: %b",
								paxos.startedLeaderElection, paxos.paxosInstanceRunning, paxos.deque.isEmpty()));
					}
				} else if (val instanceof LeaderElectionAck && paxos.phase == PaxosPhase.LEADER_ELECTION_ACK) {
					logger.addBreadcrumb("Leader Election Ack");
					LeaderElectionAck lea = (LeaderElectionAck) val;
					logger.log("leader election ack object: " + lea.toString());
					receivedLeAcks.add(lea);

					if (receivedLeAcks.size() == paxos.numberProcesses) {
						logger.log("Received all acks");
						boolean electLeader = true;
						for (LeaderElectionAck lea1 : receivedLeAcks) {
							if (!lea1.electLeader) { // if not elected the leader break
								electLeader = false;
								break;
							}
						}
						synchronized (paxos) {
							if (electLeader) {
								paxos.isLeader = true;
								paxos.failCheck.checkFailure(FailCheck.FailureType.AFTERBECOMINGLEADER);
								paxos.phase = PaxosPhase.PROMISE;
								paxos.paxosInstanceRunning = true;
							}
							paxos.startedLeaderElection = false;
						}
						receivedLeAcks.clear();
						logger.log("Result of my leader election: " + electLeader);
					}
				} else if (val instanceof Proposal) {
					logger.addBreadcrumb("Proposal");
					paxos.failCheck.checkFailure(FailCheck.FailureType.RECEIVEPROPOSE);
					synchronized (paxos) {
						paxos.paxosInstanceRunning = true;
					}
					Proposal p = (Proposal) val;
					logger.log("proposal object: " + p.toString());
					Promise promise = new Promise(p.roundNumber, paxos.acceptedRoundNumber, paxos.acceptedValue);

					logger.log(String.format(
							"proposal round number %d >= %d current paxos round number",
							p.roundNumber, paxos.roundNumber));
					if (p.roundNumber >= paxos.roundNumber) {
						synchronized (paxos) {
							paxos.roundNumber = p.roundNumber;
						}
						logger.log("Sending promise: " + promise.toString()
								+ " to: " + gcmsg.senderProcess);
						paxos.gcl.sendMsg(promise, gcmsg.senderProcess);
					}
				} else if (val instanceof Promise && paxos.phase == PaxosPhase.PROMISE) {
					logger.addBreadcrumb("Promise");
					Promise promise = (Promise) gcmsg.val;
					logger.log("promise object: " + promise.toString());
					if (promise.receivedRoundNumber != paxos.roundNumber)
						continue;
					synchronized (paxos) {
						if (promise.acceptedRoundNumber != -1) {
							paxos.promisesWithAcceptedRound.add(promise);
						}
						paxos.promiseCount++;
					}
					logger.log("Promise count: " + paxos.promiseCount);
				} else if (val instanceof Accept) {
					logger.addBreadcrumb("Accept");
					Accept acceptMessage = (Accept) val;
					logger.log("accept object: " + acceptMessage.toString());
					synchronized (paxos) {
						paxos.acceptedValue = acceptMessage.pmd;
						paxos.acceptedRoundNumber = acceptMessage.roundNumber;
					}
					AcceptAck acceptAck = new AcceptAck(acceptMessage.roundNumber);
					logger.log("Accept ack object: " + acceptAck.toString());
					paxos.gcl.sendMsg(acceptAck, gcmsg.senderProcess);
				} else if (val instanceof AcceptAck && paxos.phase == PaxosPhase.ACCEPT_ACK) {
					logger.addBreadcrumb("Accept Ack");
					AcceptAck acceptAck = (AcceptAck) gcmsg.val;
					logger.log("Accept ack object: " + acceptAck.toString());
					if (acceptAck.roundNumber != paxos.roundNumber)
						continue;
					synchronized (paxos) {
						paxos.acceptAckCount++;
					}
					logger.log("Accept ack count: " + paxos.acceptAckCount);
				} else if (val instanceof Confirm) {
					logger.addBreadcrumb("Confirm");
					Confirm confirm = (Confirm) val;
					logger.log("Confirm object: " + confirm.toString());
					logger.log(String.format("confirm round number %d == %d paxos accepted round number",
							confirm.roundNumber, paxos.acceptedRoundNumber));
					synchronized (paxos) {
						if (confirm.roundNumber == paxos.acceptedRoundNumber) {
							paxos.deliveryQueue.add(paxos.acceptedValue);
						}
						paxos.acceptedRoundNumber = -1;
						paxos.acceptedValue = null;
						paxos.paxosInstanceRunning = false;
						paxos.acceptAckCount = 0;
						paxos.promiseCount = 0;
					}
				}

			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}

class PaxosBroadcaster implements Runnable {
	Paxos paxos;

	public PaxosBroadcaster(Paxos paxos) {
		this.paxos = paxos;
	}

	public void run() {

		while (!paxos.killThread) {

			PaxosLogger logger = new PaxosLogger(paxos);
			logger.addBreadcrumb("PaxosBroadcaster");

			if (paxos.deque.isEmpty())
				continue;

			// start leader election
			if (!paxos.startedLeaderElection && !paxos.paxosInstanceRunning) {
				logger.log("Starting Leader Election");
				long timestamp = paxos.deque.peekFirst().timestamp;
				LeaderElection le = new LeaderElection(paxos.processId, timestamp);
				paxos.gcl.broadcastMsg(le);
				paxos.failCheck.checkFailure(FailCheck.FailureType.AFTERSENDPROPOSE);
				synchronized (paxos) {
					paxos.startedLeaderElection = true;
					paxos.start = System.currentTimeMillis();
				}
			}

			if (!paxos.isLeader)
				continue;

			logger.log("Process" + paxos.processId + " is the leader");
			synchronized (paxos) {
				paxos.roundNumber++;
			}
			try {
				propose(logger);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			// wait for majority to accept
			try {
				accept(logger);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			// broadcast confirm
			try {
				confirm(logger);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			long end = System.currentTimeMillis();

			synchronized (paxos) {
				paxos.avg.add((end - paxos.start));
				paxos.phase = PaxosPhase.LEADER_ELECTION_ACK;
				paxos.isLeader = false;
			}
		}
	}

	private void propose(PaxosLogger logger) throws InterruptedException {
		// propose to be leader, ie round value

		logger.addBreadcrumb("PROPOSE");

		Proposal proposal = new Proposal(paxos.roundNumber);
		paxos.gcl.broadcastMsg(proposal);

		logger.log("Broadcasting proposal");

		while (paxos.promiseCount < paxos.majority) {
		}

		logger.log("Received majority promises");

		synchronized (paxos) {
			paxos.phase = PaxosPhase.ACCEPT_ACK;

			logger.log("PHASE changed to ACCEPT_ACK");

			paxos.promisesWithAcceptedRound
					.sort((p1, p2) -> Integer.compare(p1.acceptedRoundNumber, p2.acceptedRoundNumber));

			logger.log(
					"Promises received with accepted round size in propose: " + paxos.promisesWithAcceptedRound.size());
			for (int i = paxos.promisesWithAcceptedRound.size() - 1; i >= 0; i--) {
				paxos.deque.addFirst(paxos.promisesWithAcceptedRound.get(i).acceptedValue);
			}
			paxos.promisesWithAcceptedRound.clear();
		}
		logger.log("Number of moves left to send (deque Size): " + paxos.deque.size());

		logger.removeBreadCrumb();
		return;
	}

	private void accept(PaxosLogger logger) throws InterruptedException {

		logger.addBreadcrumb("ACCEPT");

		PlayerMoveData pmd = paxos.deque.peekFirst();
		logger.log("Peeking first move from deque: " + pmd.toString());

		Accept accept = new Accept(paxos.roundNumber, pmd);
		paxos.gcl.broadcastMsg(accept);
		logger.log("Broadcasting accept");
		while (paxos.acceptAckCount < paxos.majority) {
		}
		paxos.failCheck.checkFailure(FailCheck.FailureType.AFTERVALUEACCEPT);
		synchronized (paxos) {
			paxos.deque.removeFirst();
		}
		logger.log("PMD accepted, remove move from deque: " + pmd.toString());

		logger.removeBreadCrumb();
		return;
	}

	private void confirm(PaxosLogger logger) throws InterruptedException {

		logger.addBreadcrumb("CONFIRM");

		Confirm confirm = new Confirm(paxos.roundNumber);
		logger.log("Broadcasting confirm");
		paxos.gcl.broadcastMsg(confirm);

		logger.removeBreadCrumb();
		return;
	}
}

class LeaderElection implements Serializable {
	int processId;
	long moveTimestamp;

	public LeaderElection(int processId, long timestamp) {
		this.processId = processId;
		this.moveTimestamp = timestamp;
	}

	@Override
	public String toString() {
		return "LeaderElection{" +
				"moveTimestamp=" + moveTimestamp +
				'}';
	}
}

class PaxosLogger {

	Paxos paxos;
	ArrayList<String> breadcrumbs;
	boolean doLog = true;

	public PaxosLogger(Paxos paxos) {
		this.paxos = paxos;
		this.breadcrumbs = new ArrayList<>();
	}

	public void addBreadcrumb(String b) {
		this.breadcrumbs.add(b);
	}

	public void removeBreadCrumb() {
		this.breadcrumbs.removeLast();
	}

	public void log(String msg) {
		if (!doLog)
			return;

		String s = "";

		s += String.format("[%d] ", this.paxos.roundNumber);

		for (String string : this.breadcrumbs) {
			s += String.format("[%s] ", string);
		}

		s += msg;

		System.out.println(s);
	}
}

class Promise implements Serializable {
	int receivedRoundNumber;
	int acceptedRoundNumber; // -1 for none
	PlayerMoveData acceptedValue;

	public Promise(int receivedRoundNumber, int acceptedRoundNumber, PlayerMoveData acceptedValue) {
		this.receivedRoundNumber = receivedRoundNumber;
		this.acceptedRoundNumber = acceptedRoundNumber;
		this.acceptedValue = acceptedValue;
	}

	@Override
	public String toString() {
		return "Promise{" +
				"receivedRoundNumber=" + receivedRoundNumber +
				", acceptedRoundNumber=" + acceptedRoundNumber +
				", acceptedValue=" + acceptedValue +
				'}';
	}
}

class Proposal implements Serializable {
	int roundNumber;

	public Proposal(int roundNumber) {
		this.roundNumber = roundNumber;
	}

	@Override
	public String toString() {
		return "Proposal{" +
				"roundNumber=" + roundNumber +
				'}';
	}
}

class PlayerMoveData implements Serializable {
	int player;
	char move;
	long timestamp;

	public PlayerMoveData(int player, char move, long timestamp) {
		this.player = player;
		this.move = move;
		this.timestamp = timestamp;
	}

	@Override
	public String toString() {
		return "PlayerMoveData{" +
				"player=" + player +
				", move=" + move +
				", timestamp=" + timestamp +
				'}';
	}
}

class Accept implements Serializable {
	int roundNumber;
	PlayerMoveData pmd;

	public Accept(int roundNumber, PlayerMoveData pmd) {
		this.roundNumber = roundNumber;
		this.pmd = pmd;
	}

	@Override
	public String toString() {
		return "Accept{" +
				"roundNumber=" + roundNumber +
				", pmd=" + pmd +
				'}';
	}
}

class AcceptAck implements Serializable {
	int roundNumber;

	public AcceptAck(int roundNumber) {
		this.roundNumber = roundNumber;
	}

	@Override
	public String toString() {
		return "AcceptAck{" +
				"roundNumber=" + roundNumber +
				'}';
	}
}

class Confirm implements Serializable {
	int roundNumber;

	public Confirm(int roundNumber) {
		this.roundNumber = roundNumber;
	}

	@Override
	public String toString() {
		return "Confirm{" +
				"roundNumber=" + roundNumber +
				'}';
	}
}

class LeaderElectionAck implements Serializable {
	boolean electLeader;

	public LeaderElectionAck(boolean electLeader) {
		this.electLeader = electLeader;
	}

	@Override
	public String toString() {
		return "LeaderElectionAck{" +
				"electLeader=" + electLeader +
				'}';
	}
}

enum PaxosPhase {
	LEADER_ELECTION_ACK,
	PROMISE,
	ACCEPT_ACK,
}
