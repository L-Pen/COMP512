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
import java.util.concurrent.ThreadLocalRandom;
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
	int processId;
	String processName;
	Integer numberProcesses;
	int majority = 0;

	// impl stuff
	volatile double ballotId;
	volatile double maxBallotId = 0;
	volatile int moveCount = 0;
	volatile Deque<PlayerMoveData> deque = new ArrayDeque<>();
	volatile Queue<PlayerMoveData> deliveryQueue = new LinkedList<>();
	volatile List<Promise> promisesWithAcceptedRound;
	volatile int promiseCount;
	volatile int acceptAckCount;
	volatile boolean killThread;
	long initialBackoff = 10;
	long maxBackoff = 200;
	double multiplier = 2;
	long currentBackoff = initialBackoff;

	// locks
	volatile Object lock1;

	public Paxos(String myProcess, String[] allGroupProcesses, Logger logger, FailCheck failCheck)
			throws IOException, UnknownHostException {
		// Rember to call the failCheck.checkFailure(..) with appropriate arguments
		// throughout your Paxos code to force fail points if necessary.
		this.failCheck = failCheck;
		this.processId = Integer.parseInt(myProcess.split(":")[1]);
		this.processName = myProcess;
		this.numberProcesses = allGroupProcesses.length;
		this.acceptAckCount = 0;
		this.promisesWithAcceptedRound = new ArrayList<>();
		this.promiseCount = 0;
		this.killThread = false;
		this.lock1 = new Object();
		this.currentBackoff = Math.min((long) (currentBackoff * multiplier), maxBackoff);

		System.out.println("NUM PROCESSES: " + allGroupProcesses.length + " HI PROCESS ID: " + processId);

		// Initialize the GCL communication system as well as anything else you need to.
		this.gcl = new GCL(myProcess, allGroupProcesses, null, logger);
		this.majority = calculateMajority(allGroupProcesses.length);

		PaxosListener pl = new PaxosListener(this);
		Thread plThread = new Thread(pl);
		plThread.start();
	}

	private int calculateMajority(int numProcesses) {
		return (numProcesses / 2) + 1;
	}

	// This is what the application layer is going to call to send a message/value,
	// such as the player and the move
	public synchronized void broadcastTOMsg(Object val) throws InterruptedException {
		Object[] vals = (Object[]) val;
		synchronized (lock1) {
			PlayerMoveData playerMoveData = new PlayerMoveData((int) vals[0], (char) vals[1], moveCount);
			deque.addLast(playerMoveData);
			System.out.println("[broadcastTOMsg] Added move to dequeue in: " +
					deque.peek().toString() + " deque size: "
					+ deque.size());
		}

		PaxosLogger logger = new PaxosLogger(this);

		while (!deque.isEmpty()) {

			synchronized (lock1) {
				ballotId = Double.parseDouble((++moveCount) + "." + processId);
			}

			try {
				long randomizedDelay = ThreadLocalRandom.current().nextLong(initialBackoff, currentBackoff);
				Thread.sleep(Math.min(randomizedDelay, maxBackoff));
				currentBackoff = Math.min((long) (currentBackoff * multiplier), maxBackoff);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				continue;
			}

			try {
				logger.addBreadcrumb("PROPOSE");
				boolean suc = propose(logger);
				logger.removeBreadCrumb();
				if (!suc)
					continue;
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			try {
				logger.addBreadcrumb("ACCEPT");
				boolean suc = accept(logger);
				logger.removeBreadCrumb();
				if (!suc)
					continue;
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			try {
				logger.addBreadcrumb("CONFIRM");
				confirm(logger);
				logger.log("Broadcasting confirm");
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

	}

	// This is what the application layer is calling to figure out what is the next
	// message in the total order.
	// Messages delivered in ALL the processes in the group should deliver this in
	// the same order.
	public Object acceptTOMsg() throws InterruptedException {
		while (deliveryQueue.isEmpty()) {
		}
		synchronized (lock1) {
			PlayerMoveData pmd = deliveryQueue.remove();

			System.out.println("[acceptTOMsg] delivering move: " + pmd.toString());
			return new Object[] { pmd.player, pmd.move };
		}
	}

	public void shutdownPaxos() throws InterruptedException {
		this.killThread = true;
		gcl.shutdownGCL();
		Thread.sleep(10);
	}

	private boolean propose(PaxosLogger logger) throws InterruptedException {
		// propose to be leader, ie round value

		Proposal proposal = new Proposal(ballotId);
		gcl.broadcastMsg(proposal);

		failCheck.checkFailure(FailCheck.FailureType.AFTERSENDPROPOSE);

		logger.log("Broadcasting proposal");

		long start = System.currentTimeMillis();
		while (promiseCount < majority) {
			if (System.currentTimeMillis() - start > ThreadLocalRandom.current().nextInt(50, 200)) {
				logger.log("Didn't receive majority");
				synchronized (lock1) {
					promiseCount = 0;
				}
				return false;
			}
		}

		logger.log("Received majority promises");
		failCheck.checkFailure(FailCheck.FailureType.AFTERBECOMINGLEADER);

		synchronized (lock1) {

			logger.log("PHASE changed to ACCEPT_ACK");

			promisesWithAcceptedRound
					.sort((p1, p2) -> Double.compare(p1.ballotId, p2.ballotId));

			logger.log(
					"Promises received with accepted round size in propose: " + promisesWithAcceptedRound.size());
			for (int i = promisesWithAcceptedRound.size() - 1; i >= 0; i--) {
				deque.addFirst(promisesWithAcceptedRound.get(i).acceptedValue);
			}
			promisesWithAcceptedRound.clear();
		}
		logger.log("Number of moves left to send (deque Size): " + deque.size());

		return true;
	}

	private boolean accept(PaxosLogger logger) throws InterruptedException {

		PlayerMoveData pmd = deque.peekFirst();
		logger.log("Peeking first move from deque: " + pmd.toString());

		Accept accept = new Accept(ballotId, pmd);
		gcl.broadcastMsg(accept);
		logger.log("Broadcasting accept");

		long start = System.currentTimeMillis();
		while (acceptAckCount < majority) {
			if (System.currentTimeMillis() - start > ThreadLocalRandom.current().nextInt(50, 200)) {
				logger.log("Didn't receive majority");
				synchronized (lock1) {
					acceptAckCount = 0;
				}
				return false;
			}
		}
		failCheck.checkFailure(FailCheck.FailureType.AFTERVALUEACCEPT);
		synchronized (lock1) {
			deque.removeFirst();
		}
		logger.log("PMD accepted, remove move from deque: " + pmd.toString());

		return true;
	}

	private void confirm(PaxosLogger logger) throws InterruptedException {

		Confirm confirm = new Confirm(ballotId);
		gcl.broadcastMsg(confirm);

		logger.removeBreadCrumb();
		return;
	}
}

class PaxosListener implements Runnable {

	Paxos paxos;
	PlayerMoveData acceptedValue;
	double acceptedBallotId;
	ArrayList<String> duplicateCheck;

	public PaxosListener(Paxos paxos) {
		this.paxos = paxos;
		acceptedValue = null;
		acceptedBallotId = -1;
		duplicateCheck = new ArrayList<>();
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

				if (val instanceof Proposal) {
					paxos.failCheck.checkFailure(FailCheck.FailureType.RECEIVEPROPOSE);
					synchronized (paxos.lock1) {
						logger.addBreadcrumb("Proposal");
						Proposal p = (Proposal) val;
						logger.log("proposal object: " + p.toString());

						logger.log(String.format(
								"proposal ballotId %f > %f current max ballotId",
								p.ballotId, paxos.maxBallotId));
						if (p.ballotId >= paxos.maxBallotId) {
							Promise promise = new Promise(p.ballotId, acceptedValue, acceptedBallotId);
							logger.log("Sending promise: " + promise.toString()
									+ " to: " + gcmsg.senderProcess);
							paxos.gcl.sendMsg(promise, gcmsg.senderProcess);
							paxos.failCheck.checkFailure(FailCheck.FailureType.AFTERSENDVOTE);

							paxos.maxBallotId = p.ballotId;
						}
					}
				} else if (val instanceof Promise) {
					synchronized (paxos.lock1) {
						logger.addBreadcrumb("Promise");
						Promise promise = (Promise) gcmsg.val;
						logger.log("promise object: " + promise.toString());
						if (promise.ballotId == paxos.ballotId) {
							paxos.promiseCount++;
						}
						if (promise.acceptedValue != null && promise.acceptedBallotId != -1) {
							paxos.promisesWithAcceptedRound.add(promise);
						}
						logger.log("Promise count: " + paxos.promiseCount);
					}
				} else if (val instanceof Accept) {
					synchronized (paxos.lock1) {
						logger.addBreadcrumb("Accept");
						Accept acceptMessage = (Accept) val;
						logger.log("accept object: " + acceptMessage.toString());
						if (acceptMessage.ballotId >= paxos.maxBallotId) {
							paxos.maxBallotId = acceptMessage.ballotId;
							acceptedValue = acceptMessage.pmd;
							acceptedBallotId = acceptMessage.ballotId;
							AcceptAck acceptAck = new AcceptAck(acceptedBallotId);
							logger.log("Accept ack object: " + acceptAck.toString());
							paxos.gcl.sendMsg(acceptAck, gcmsg.senderProcess);
						}
					}
				} else if (val instanceof AcceptAck) {
					synchronized (paxos.lock1) {
						logger.addBreadcrumb("Accept Ack");
						AcceptAck acceptAck = (AcceptAck) gcmsg.val;
						logger.log("Accept ack object: " + acceptAck.toString());
						if (acceptAck.ballotId != paxos.ballotId)
							continue;
						paxos.acceptAckCount++;
						logger.log("Accept ack count: " + paxos.acceptAckCount);
					}
				} else if (val instanceof Confirm) {
					synchronized (paxos.lock1) {
						logger.addBreadcrumb("Confirm");
						Confirm confirm = (Confirm) val;
						logger.log("Confirm object: " + confirm.toString());
						logger.log(String.format("confirm round number %f == %f paxos accepted round number",
								confirm.ballotId, acceptedBallotId));
						if (confirm.ballotId == acceptedBallotId) {
							boolean duplicate = false;
							int duplicateCheckSize = duplicateCheck.size();
							for (int i = 0; i < duplicateCheckSize; i++) {
								duplicate = acceptedValue.getUid().equals(duplicateCheck.get(i)) || duplicate;
							}
							if (!duplicate) {
								paxos.deliveryQueue.add(acceptedValue);
								duplicateCheck.add(acceptedValue.getUid());
							}
						}
						acceptedValue = null;
						acceptedBallotId = -1;
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

		s += String.format("[%f] ", this.paxos.ballotId);

		for (String string : this.breadcrumbs) {
			s += String.format("[%s] ", string);
		}

		s += msg;

		System.out.println(s);
	}
}

class Promise implements Serializable {
	double ballotId;
	double acceptedBallotId;
	PlayerMoveData acceptedValue;

	public Promise(double ballotId, PlayerMoveData acceptedValue, double acceptedBallotId) {
		this.ballotId = ballotId;
		this.acceptedValue = acceptedValue;
		this.acceptedBallotId = acceptedBallotId;
	}

	@Override
	public String toString() {
		return "Promise{" +
				"ballotId=" + ballotId +
				", acceptedValue=" + acceptedValue +
				", acceptedBallotId=" + acceptedBallotId +
				'}';
	}
}

class Proposal implements Serializable {
	double ballotId;

	public Proposal(double ballotId) {
		this.ballotId = ballotId;
	}

	@Override
	public String toString() {
		return "Proposal{" +
				"ballotId=" + ballotId +
				'}';
	}
}

class PlayerMoveData implements Serializable {
	int player;
	char move;
	int moveCount;

	public PlayerMoveData(int player, char move, int moveCount) {
		this.player = player;
		this.move = move;
		this.moveCount = moveCount;
	}

	public String getUid() {
		return String.format("p%dm%d", player, moveCount);
	}

	@Override
	public String toString() {
		return "PlayerMoveData{" +
				"player=" + player +
				", move=" + move +
				", moveCount=" + moveCount +
				'}';
	}
}

class Accept implements Serializable {
	double ballotId;
	PlayerMoveData pmd;

	public Accept(double ballotId, PlayerMoveData pmd) {
		this.ballotId = ballotId;
		this.pmd = pmd;
	}

	@Override
	public String toString() {
		return "Accept{" +
				"ballotId=" + ballotId +
				", pmd=" + pmd +
				'}';
	}
}

class AcceptAck implements Serializable {
	double ballotId;

	public AcceptAck(double ballotId) {
		this.ballotId = ballotId;
	}

	@Override
	public String toString() {
		return "AcceptAck{" +
				"ballotId=" + ballotId +
				'}';
	}
}

class Confirm implements Serializable {
	double ballotId;

	public Confirm(double ballotId) {
		this.ballotId = ballotId;
	}

	@Override
	public String toString() {
		return "Confirm{" +
				"ballotId=" + ballotId +
				'}';
	}
}