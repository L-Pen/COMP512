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
		System.out.println("Entered broadcastTOMsg");
		Object[] vals = (Object[]) val;
		PlayerMoveData playerMoveData = new PlayerMoveData((int) vals[0], (char) vals[1]);
		deque.addLast(playerMoveData);
		System.out.println("Deque size in broadcastTO: " + deque.peek().toString());
		System.out.println("Added to deque in broadcastTOMsg");
	}

	// This is what the application layer is calling to figure out what is the next
	// message in the total order.
	// Messages delivered in ALL the processes in the group should deliver this in
	// the same order.
	public Object acceptTOMsg() throws InterruptedException {
		// This is just a place holder.
		System.out.println("Entered acceptTOMsg");
		while (deliveryQueue.isEmpty()) {
		}
		PlayerMoveData pmd = deliveryQueue.remove();
		System.out.println("TO STRING IN ACCEPT TO MESSAGE: " + pmd.toString());
		return new Object[] { pmd.player, pmd.move };
	}

	// Add any of your own shutdown code into this method.
	public void shutdownPaxos() {
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
		while (true) {
			try {
				GCMessage gcmsg = paxos.gcl.readGCMessage();

				Object val = gcmsg.val;

				System.out.println("Received payload: " + val.getClass().getName());

				if (val instanceof LeaderElection) {
					System.out.println("In leader election in paxos listener");
					paxos.acceptedRoundNumber = -1;
					paxos.acceptedValue = null;
					LeaderElection le = (LeaderElection) val;
					paxos.paxosInstanceRunning = false;
					// if im not the leader or my idis less than propoper or my id is bigger but i
					// have nothing to send
					boolean elect = (paxos.processId == le.processId) || (paxos.processId < le.processId)
							|| (paxos.processId > le.processId && paxos.deque.isEmpty());
					System.out.println("Elect result: " + elect);
					paxos.paxosInstanceRunning = elect;
					LeaderElectionAck leaderElectionMessage = new LeaderElectionAck(elect);
					paxos.gcl.sendMsg(leaderElectionMessage, gcmsg.senderProcess);
					paxos.failCheck.checkFailure(FailCheck.FailureType.AFTERSENDVOTE);
					System.out.println("Exiting leader election in paxos listener");
				} else if (val instanceof LeaderElectionAck && paxos.phase == PaxosPhase.LEADER_ELECTION_ACK) {
					System.out.println("In leader election ack in paxos listener");
					receivedLeAcks.add((LeaderElectionAck) val);

					if (receivedLeAcks.size() == paxos.numberProcesses) {
						System.out.println("Received all leader election acks");
						boolean electLeader = true;
						for (LeaderElectionAck lea : receivedLeAcks) {
							if (!lea.electLeader) { // if not elected the leader break
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
						System.out.println("Process ID: " + paxos.processId + " Leader: " + paxos.isLeader);
					}
				} else if (val instanceof Proposal) {
					System.out.println(paxos.processId + " In Proposal in paxos listener");
					paxos.failCheck.checkFailure(FailCheck.FailureType.RECEIVEPROPOSE);
					Proposal p = (Proposal) val;
					Promise promise = new Promise(p.roundNumber, paxos.acceptedRoundNumber, paxos.acceptedValue);

					if (p.roundNumber >= paxos.roundNumber) {
						System.out.println("Inside >= round number in proposal in paxos listener");
						paxos.roundNumber = p.roundNumber;
						System.out.println("Paxos Round number in propsal in paxos listener :" + paxos.roundNumber);
						System.out.println("Sending promise to: " + gcmsg.senderProcess);

						paxos.gcl.sendMsg(promise, gcmsg.senderProcess);
					} else {
						System.out.println("Refused your proposal loser");
					}

					System.out.println("FINISHED PROPOSAL: " + p.roundNumber);
				} else if (val instanceof Promise && paxos.phase == PaxosPhase.PROMISE) {
					Promise promise = (Promise) gcmsg.val;
					System.out.println("Received promise from: " + gcmsg.senderProcess);
					if (promise.receivedRoundNumber != paxos.roundNumber)
						continue;
					if (promise.acceptedRoundNumber != -1) {
						paxos.promisesWithAcceptedRound.add(promise);
					}
					paxos.promiseCount++;
					System.out.println("Promise count: " + paxos.promiseCount);
				} else if (val instanceof Accept) {
					System.out.println("In Accept in paxos listener");
					Accept acceptMessage = (Accept) val;
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

	public PaxosBroadcaster(Paxos paxos) {
		this.paxos = paxos;
	}

	public void run() {
		while (true) {
			// System.out.println("Deque size in THREAD: " + paxos.deque.size());

			// start new paxos instance
			if (paxos.deque.isEmpty())
				continue;

			// start leader election
			if (!paxos.startedLeaderElection && !paxos.paxosInstanceRunning) {
				System.out.println("Beginning Leader Election in Paxos Broadcaster");
				LeaderElection le = new LeaderElection(paxos.processId, paxos.processName);
				System.out.println(le);
				paxos.gcl.broadcastMsg(le);
				paxos.failCheck.checkFailure(FailCheck.FailureType.AFTERSENDPROPOSE);
				paxos.startedLeaderElection = true;
			}

			if (!paxos.isLeader)
				continue;

			System.out.println("In the middle of Paxos Broadcaster");
			paxos.startedLeaderElection = false;
			paxos.roundNumber++;
			try {
				System.out.println("Proposing");
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
		System.out.println("Inside Propose message");
		Proposal proposal = new Proposal(paxos.roundNumber);

		paxos.gcl.broadcastMsg(proposal);
		System.out.println("Broadcasted proposal");
		while (paxos.promiseCount < paxos.majority) {
		}
		paxos.phase = PaxosPhase.ACCEPT_ACK;
		paxos.promisesWithAcceptedRound
				.sort((p1, p2) -> Integer.compare(p1.acceptedRoundNumber, p2.acceptedRoundNumber));

		System.out.println("Promises with accepted round size in propose: " + paxos.promisesWithAcceptedRound.size());
		for (int i = paxos.promisesWithAcceptedRound.size() - 1; i >= 0; i--) {
			paxos.deque.addFirst(paxos.promisesWithAcceptedRound.get(i).acceptedValue);
		}
		paxos.promisesWithAcceptedRound.clear();
		System.out.println("Deque size in propose: " + paxos.deque.size());
		return;
	}

	private void accept() throws InterruptedException {
		System.out.println("Inside Accept message");
		PlayerMoveData pmd = paxos.deque.peekFirst();
		System.out.println("PRINTING PMD IN ACCEPT: " + pmd.toString());

		paxos.acceptAckCount = 0;
		Accept accept = new Accept(paxos.roundNumber, pmd);
		paxos.gcl.broadcastMsg(accept);
		while (paxos.acceptAckCount < paxos.majority) {
		}
		paxos.failCheck.checkFailure(FailCheck.FailureType.AFTERVALUEACCEPT);
		paxos.deque.removeFirst();
		System.out.println("FINISHED ACCEPT");
		return;
	}

	private void confirm() throws InterruptedException {
		System.out.println("Inside Confirm message");
		Confirm confirm = new Confirm(paxos.roundNumber);
		paxos.gcl.broadcastMsg(confirm);
		System.out.println("FINISHED CONFIRM");
		return;
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
		return "Player: " + player + " Move: " + move;
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

class LeaderElectionAck implements Serializable {
	boolean electLeader;

	public LeaderElectionAck(boolean electLeader) {
		this.electLeader = electLeader;
	}
}

enum PaxosPhase {
	LEADER_ELECTION_ACK,
	PROMISE,
	ACCEPT_ACK,
}
