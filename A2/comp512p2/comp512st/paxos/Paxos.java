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

import javax.sound.midi.SysexMessage;

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
		System.out.println("[broadcastTOMsg] Added move to dequeue in: " + deque.peek().toString());
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
		System.out.println("[acceptTOMsg] delivering move: " + pmd.toString());
		return new Object[] { pmd.player, pmd.move };
	}

	// Add any of your own shutdown code into this method.
	public void shutdownPaxos() {
		this.killThread = true;
		gcl.shutdownGCL();
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

				System.out.println(
						"[Paxos Listener] Received payload: " + val.getClass().getName() + " from: "
								+ gcmsg.senderProcess);

				if (val instanceof LeaderElection) {
					paxos.acceptedRoundNumber = -1;
					paxos.acceptedValue = null;
					LeaderElection le = (LeaderElection) val;
					System.out.println("[Paxos Listener] [Leader Election] leader election object: " + le.toString());
					paxos.paxosInstanceRunning = false;
					// if im not the leader or my idis less than propoper or my id is bigger but i
					// have nothing to send
					boolean elect = (paxos.processId == le.processId) || (paxos.processId < le.processId)
							|| (paxos.processId > le.processId && paxos.deque.isEmpty());
					System.out.println(String.format("[Paxos Listener] [Leader Election] Elect %d to be leader: %b",
							le.processId, elect));
					paxos.paxosInstanceRunning = elect;
					LeaderElectionAck leaderElectionMessage = new LeaderElectionAck(elect);
					paxos.gcl.sendMsg(leaderElectionMessage, gcmsg.senderProcess);
					paxos.failCheck.checkFailure(FailCheck.FailureType.AFTERSENDVOTE);
				} else if (val instanceof LeaderElectionAck && paxos.phase == PaxosPhase.LEADER_ELECTION_ACK) {
					LeaderElectionAck lea = (LeaderElectionAck) val;
					System.out.println(
							"[Paxos Listener] [Leader Election Ack] leader election ack object: " + lea.toString());
					receivedLeAcks.add(lea);

					if (receivedLeAcks.size() == paxos.numberProcesses) {
						System.out.println("[Paxos Listener] [Leader Election Ack] received all acks");
						boolean electLeader = true;
						for (LeaderElectionAck lea1 : receivedLeAcks) {
							if (!lea1.electLeader) { // if not elected the leader break
								electLeader = false;
								break;
							}
						}
						if (electLeader) {
							// dont set started leader election false here because we dont want to start
							// another one
							paxos.isLeader = true;
							paxos.failCheck.checkFailure(FailCheck.FailureType.AFTERBECOMINGLEADER);
							paxos.phase = PaxosPhase.PROMISE;
						} else {
							// if you get rejected, we want you to be able to run for leader again
							paxos.startedLeaderElection = false;
						}
						receivedLeAcks.clear();
						System.out.println(
								"[Paxos Listener] [Leader Election Ack] Result of my leader election: " + electLeader);
					}
				} else if (val instanceof Proposal) {
					paxos.failCheck.checkFailure(FailCheck.FailureType.RECEIVEPROPOSE);
					Proposal p = (Proposal) val;
					System.out.println(
							"[Paxos Listener] [Proposal] proposal object: " + p.toString());
					Promise promise = new Promise(p.roundNumber, paxos.acceptedRoundNumber, paxos.acceptedValue);

					System.out.println(String.format(
							"[Paxos Listener] [Proposal] proposal round number %d >= %d current paxos round number",
							p.roundNumber, paxos.roundNumber));
					if (p.roundNumber >= paxos.roundNumber) {
						paxos.roundNumber = p.roundNumber;
						System.out.println("[Paxos Listener] [Proposal] Sending promise: " + promise.toString()
								+ " to: " + gcmsg.senderProcess);
						paxos.gcl.sendMsg(promise, gcmsg.senderProcess);
					}
				} else if (val instanceof Promise && paxos.phase == PaxosPhase.PROMISE) {
					Promise promise = (Promise) gcmsg.val;
					System.out.println("[Paxos Listener] [Promise] promise object: " + promise.toString()
							+ " | Current paxos round: " + paxos.roundNumber);
					if (promise.receivedRoundNumber != paxos.roundNumber)
						continue;
					if (promise.acceptedRoundNumber != -1) {
						paxos.promisesWithAcceptedRound.add(promise);
					}
					paxos.promiseCount++;
					System.out.println("[Paxos Listener] [Promise] Promise count: " + paxos.promiseCount);
				} else if (val instanceof Accept) {
					Accept acceptMessage = (Accept) val;
					System.out.println("[Paxos Listener] [Accept] promise object: " + acceptMessage.toString());
					paxos.acceptedValue = acceptMessage.pmd;
					paxos.acceptedRoundNumber = acceptMessage.roundNumber;
					System.out.println("Accepted Round Number: " + paxos.acceptedRoundNumber);
					System.out.println("Accepted Value: " + paxos.acceptedValue.toString());
					AcceptAck acceptAck = new AcceptAck(acceptMessage.roundNumber);
					paxos.gcl.sendMsg(acceptAck, gcmsg.senderProcess);
					System.out.println("Exiting Accept in paxos listener");
				} else if (val instanceof AcceptAck && paxos.phase == PaxosPhase.ACCEPT_ACK) {
					AcceptAck acceptAck = (AcceptAck) gcmsg.val;
					if (acceptAck.roundNumber != paxos.roundNumber)
						continue;
					paxos.acceptAckCount++;
					System.out.println("Accept ack count: " + paxos.acceptAckCount);
				} else if (val instanceof Confirm) {
					System.out.println("In Confirm in paxos listener");
					Confirm confirm = (Confirm) val;
					if (confirm.roundNumber == paxos.acceptedRoundNumber) {
						System.out.println("Were you false?");
						paxos.deliveryQueue.add(paxos.acceptedValue);
					}
					paxos.paxosInstanceRunning = false;
				}
			} catch (InterruptedException e) {
				// e.printStackTrace();
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

			PaxosLogger logger = new PaxosLogger(paxos.roundNumber);
			logger.addBreadcrumb("PaxosBroadcaster");
			
			if (paxos.deque.isEmpty())
				continue;

			// start leader election
			if (!paxos.startedLeaderElection && !paxos.paxosInstanceRunning) {
				logger.log("Starting Leader Election");
				LeaderElection le = new LeaderElection(paxos.processId, paxos.processName);
				System.out.println(le);
				logger.log("Sending Leader Election");
				paxos.gcl.broadcastMsg(le);
				paxos.failCheck.checkFailure(FailCheck.FailureType.AFTERSENDPROPOSE);
				paxos.startedLeaderElection = true;
			}

			if (!paxos.isLeader)
				continue;

			logger.log("Process" + paxos.processId + " is the leader");
			paxos.startedLeaderElection = false;
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

			paxos.phase = PaxosPhase.LEADER_ELECTION_ACK;
			paxos.isLeader = false;
		}
	}

	private void propose() throws InterruptedException {
		// propose to be leader, ie round value

		PaxosLogger logger = new PaxosLogger(paxos.roundNumber);
		logger.addBreadcrumb("[PROPOSE]");
		
		Proposal proposal = new Proposal(paxos.roundNumber);
		paxos.gcl.broadcastMsg(proposal);

		logger.log("Broadcasting proposal");

		while (paxos.promiseCount < paxos.majority) {
		}

		logger.log("Received majority promises");

		paxos.phase = PaxosPhase.ACCEPT_ACK;

		logger.log("PHASE changed to ACCEPT_ACK");

		paxos.promisesWithAcceptedRound
				.sort((p1, p2) -> Integer.compare(p1.acceptedRoundNumber, p2.acceptedRoundNumber));

		logger.log("Promises received with accepted round size in propose: " + paxos.promisesWithAcceptedRound.size());
		for (int i = paxos.promisesWithAcceptedRound.size() - 1; i >= 0; i--) {
			paxos.deque.addFirst(paxos.promisesWithAcceptedRound.get(i).acceptedValue);
		}
		paxos.promisesWithAcceptedRound.clear();
		logger.log("Number of moves left to send (deque Size): " + paxos.deque.size());
		return;
	}

	private void accept() throws InterruptedException {

		PaxosLogger logger = new PaxosLogger(paxos.roundNumber);
		logger.addBreadcrumb("[ACCEPT]");

		PlayerMoveData pmd = paxos.deque.peekFirst();
		logger.log("Peeking first move from deque: " + pmd.toString());

		paxos.acceptAckCount = 0;
		Accept accept = new Accept(paxos.roundNumber, pmd);
		paxos.gcl.broadcastMsg(accept);
		logger.log("Broadcasting accept");
		while (paxos.acceptAckCount < paxos.majority) {
		}
		paxos.failCheck.checkFailure(FailCheck.FailureType.AFTERVALUEACCEPT);
		paxos.deque.removeFirst();
		logger.log("PMD accepted, remove move from deque: " + pmd.toString());
		return;
	}

	private void confirm() throws InterruptedException {

		PaxosLogger logger = new PaxosLogger(paxos.roundNumber);
		logger.addBreadcrumb("[CONFIRM]");

		Confirm confirm = new Confirm(paxos.roundNumber);
		logger.log("Broadcasting confirm");
		paxos.gcl.broadcastMsg(confirm);
		return;
	}
}

class LeaderElection implements Serializable {
	int processId;
	String processName;

	public LeaderElection(int processId, String processName) {
		this.processId = processId;
		this.processName = processName;
	}

	@Override
	public String toString() {
		return "LeaderElection{" +
				"processId=" + processId +
				", processName='" + processName + '\'' +
				'}';
	}
}

class PaxosLogger {

	int round;
	ArrayList<String> breadcrumbs;

	public PaxosLogger(int round) {
		this.round = round;
	}

	public void addBreadcrumb(String b) {
		this.breadcrumbs.add(b);
	}

	public void log(String msg) {

		String s = "";

		s += String.format("[%d] ", this.round);

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

	public PlayerMoveData(int player, char move) {
		this.player = player;
		this.move = move;
	}

	@Override
	public String toString() {
		return "PlayerMoveData{" +
				"player=" + player +
				", move=" + move +
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
