
/*
Copyright
All materials provided to the students as part of this course is the property of respective authors. Publishing them to third-party (including websites) is prohibited. Students may save it for their personal use, indefinitely, including personal cloud storage spaces. Further, no assessments published as part of this course may be shared with anyone else. Violators of this copyright infringement may face legal actions in addition to the University disciplinary proceedings.
©2022, Joseph D’Silva; ©2024, Bettina Kemme
*/
import java.io.*;

import java.util.*;

// To get the name of the host.
import java.net.*;

//To get the process id.
import java.lang.management.*;

import org.apache.zookeeper.*;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.KeeperException.*;
import org.apache.zookeeper.data.*;

import java.lang.Thread;

public class DistProcess implements Watcher, AsyncCallback.ChildrenCallback, AsyncCallback.DataCallback, Runnable {
	ZooKeeper zk;
	String zkServer, pinfo;
	volatile boolean isManager = false;
	boolean initialized = false;

	volatile private HashMap<String, String> workers = new HashMap<>();

	volatile private HashMap<String, Boolean> taskStatus = new HashMap<>();
	volatile private Queue<String> taskQueue = new LinkedList<>();

	volatile private DistTask taskObject = null;
	volatile private String workerTaskId = null;

	volatile private Object taskLock = new Object();

	DistProcess(String zkhost) {
		zkServer = zkhost;
		pinfo = ManagementFactory.getRuntimeMXBean().getName().split("@")[1];
		System.out.println("DISTAPP : ZK Connection information : " + zkServer);
		System.out.println("DISTAPP : Process information : " + pinfo);
	}

	void startProcess() throws IOException, UnknownHostException, KeeperException, InterruptedException {
		zk = new ZooKeeper(zkServer, 10000, this); // connect to ZK.
	}

	public void run() {
		while (true) {

			// manager stuff
			if (isManager) {
				while (taskQueue.isEmpty()) {
				}

				synchronized (workers) {
					Set<String> workerIds = workers.keySet();
					for (String workerId : workerIds) {
						if (workers.get(workerId) != null)
							continue;

						synchronized (taskLock) {
							String taskId = taskQueue.remove();
							System.out.println("==== Removed task from queue, length : " + taskQueue.size());
							workers.put(workerId, taskId);
							System.out.println(stringifyMap(workers));
							try {
								String path = "/dist30/workers/" + workerId;
								zk.setData(path, taskId.getBytes(), -1,
										null, null);
								zk.getData(path, true, null, null);
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
						break;
					}
				}
			}

			// worker stuff
			else {
				if (taskObject != null) {
					synchronized (taskLock) {
						try {
							taskObject.compute();

							// Serialize our Task object back to a byte array!
							ByteArrayOutputStream bos = new ByteArrayOutputStream();
							ObjectOutputStream oos = new ObjectOutputStream(bos);
							oos.writeObject(taskObject);
							oos.flush();
							byte[] taskSerial = bos.toByteArray();

							// Store it inside the result node.
							zk.create("/dist30/tasks/" + workerTaskId + "/result", taskSerial,
									Ids.OPEN_ACL_UNSAFE,
									CreateMode.PERSISTENT);

							String path = "/dist30/workers/" + pinfo;
							zk.setData(path, null, -1, null, null);
							taskObject = null;
							workerTaskId = null;
						} catch (NodeExistsException nee) {
							System.out.println(nee);
						} catch (KeeperException ke) {
							System.out.println(ke);
						} catch (InterruptedException ie) {
							System.out.println(ie);
						} catch (IOException io) {
							System.out.println(io);
						}
					}
				}
			}

		}
	}

	void initialize() {
		try {
			runForManager(); // See if you can become the manager (i.e, no other manager exists)
			isManager = true;
			getTasks(); // Install monitoring on any new tasks that will be created.
			getWorkers();
		} catch (NodeExistsException nee) {
			isManager = false;
			registerWorker();
			checkForTask();
		} catch (UnknownHostException uhe) {
			System.out.println(uhe);
		} catch (KeeperException ke) {
			System.out.println(ke);
		} catch (InterruptedException ie) {
			System.out.println(ie);
		}

		System.out.println("DISTAPP : Role : " + " I will be functioning as " + (isManager ? "manager" : "worker"));

	}

	// Manager fetching task znodes...
	void getTasks() {
		zk.getChildren("/dist30/tasks", this, this, null);
	}

	void getWorkers() {
		zk.getChildren("/dist30/workers", this, this, null);
	}

	// Try to become the manager.
	void runForManager() throws UnknownHostException, KeeperException, InterruptedException {
		// Try to create an ephemeral node to be the manager, put the hostname and pid
		// of this process as the data.
		// This is an example of Synchronous API invocation as the function waits for
		// the execution and no callback is involved..
		zk.create("/dist30/manager", pinfo.getBytes(), Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
	}

	void registerWorker() {
		try {
			zk.create("/dist30/workers/" + pinfo, null, Ids.OPEN_ACL_UNSAFE,
					CreateMode.EPHEMERAL);
		} catch (Exception e) {
			System.out.println("Node already registered to worker znode");
		}
	}

	void checkForTask() {
		zk.getData("/dist30/workers/" + pinfo, this, this, null);
	}

	public void process(WatchedEvent e) {
		// Get watcher notifications.

		System.out.println("DISTAPP : Event received : " + e);

		if (e.getType() == Watcher.Event.EventType.None) // This seems to be the event type associated with connections.
		{
			// Once we are connected, do our intialization stuff.
			if (e.getPath() == null && e.getState() == Watcher.Event.KeeperState.SyncConnected
					&& initialized == false) {
				initialize();
				initialized = true;
			}
		}

		// Manager should be notified if any new znodes are added to tasks.
		if (e.getType() == Watcher.Event.EventType.NodeChildrenChanged && e.getPath().equals("/dist30/tasks")) {
			getTasks();
		}

		// Manager should be notified if any new znodes are added to workers.
		if (e.getType() == Watcher.Event.EventType.NodeChildrenChanged && e.getPath().equals("/dist30/workers")) {
			getWorkers();
		}

		// Workers are notified when job is added to their znode
		if (e.getType() == Watcher.Event.EventType.NodeDataChanged
				&& e.getPath().equals("/dist30/workers/" + pinfo)) {
			checkForTask();
		} else if (e.getType() == Watcher.Event.EventType.NodeDataChanged) {
			String workerId = e.getPath().split("/")[3];
			synchronized (workers) {
				String taskId = workers.get(workerId);
				workers.put(workerId, null);
				System.out.println(stringifyMap(workers));
				synchronized (taskLock) {
					taskStatus.remove(taskId);
				}
			}
		}
	}

	// Asynchronous callback that is invoked by the zk.getChildren request.
	public void processResult(int rc, String path, Object ctx, List<String> children) {
		System.out.println("DISTAPP : processResult : " + rc + ":" + path + ":" +
				ctx);

		if (isManager && path.equals("/dist30/workers")) {
			synchronized (workers) {
				for (String c : children) {
					if (workers.containsKey(c))
						continue;
					workers.put(c, null);
				}
				System.out.println(stringifyMap(workers));
			}
		} else if (isManager && path.equals("/dist30/tasks")) {
			synchronized (taskLock) {
				for (String c : children) {
					if (taskStatus.containsKey(c))
						continue;
					taskStatus.put(c, false);
					taskQueue.add(c);
					System.out.println("==== Added task to queue, length : " + taskQueue.size());
				}
			}
		}
	}

	public void processResult(int rc, String path, Object ctx, byte[] data, Stat stat) {
		System.out.println("Data return - rc: " + rc + " path: " + path + " ctx: " +
				ctx);
		if (data == null)
			return;

		try {
			ByteArrayInputStream bis = new ByteArrayInputStream(data);
			ObjectInput in = new ObjectInputStream(bis);

			if (path.contains("workers")) {
				zk.getData("/dist30/tasks/" + (String) in.readObject(), false, this, null);
			} else if (path.contains("tasks")) {
				synchronized (taskLock) {
					taskObject = (DistTask) in.readObject();
				}
			}
		} catch (IOException io) {
			System.out.println(io);
		} catch (ClassNotFoundException cne) {
			System.out.println(cne);
		}
	}

	private String stringifyMap(HashMap<?, ?> map) {
		StringBuilder mapAsString = new StringBuilder("{");
		for (Object key : map.keySet()) {
			mapAsString.append(key + "=" + String.valueOf(map.get(key)) + ", ");
		}
		mapAsString.append("}");
		return mapAsString.toString();
	}

	public static void main(String args[]) throws Exception {
		// Create a new process
		// Read the ZooKeeper ensemble information from the environment variable.
		DistProcess dt = new DistProcess(System.getenv("ZKSERVER"));
		dt.startProcess();

		Thread dtThread = new Thread(dt);
		dtThread.start();
	}
}
