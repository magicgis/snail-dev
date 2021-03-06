package cn.tomsnail.lock.zookeeper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;

/**
 *        zk简单分布式锁
 * @author yangsong
 * @version 0.0.1
 * @status 正常
 * @date 2016年8月19日 下午1:52:02
 * @see 
 */
public class ZKDistributedSimpleLock implements Lock, Watcher {
	private ZooKeeper zk;
	private String root = "/slocks";// 根
	private String lockName;// 竞争资源的标志
	private String waitNode;// 等待前一个锁
	private String myZnode;// 当前锁
	private CountDownLatch latch;// 计数器
	private int sessionTimeout = 30000;
	private List<Exception> exception = new ArrayList<Exception>();

	/**
	 * 创建分布式锁,使用前请确认config配置的zookeeper服务可用
	 * 
	 * @param config
	 *            127.0.0.1:2181
	 * @param lockName
	 *            竞争资源标志,lockName中不能包含单词lock
	 */
	public ZKDistributedSimpleLock(String config, String lockName) {
		this.lockName = lockName;
		// 创建一个与服务器的连接
		try {
			zk = new ZooKeeper(config, sessionTimeout, this);
			Stat stat = zk.exists(root, false);
			if (stat == null) {
				// 创建根节点
				zk.create(root, new byte[0], ZooDefs.Ids.OPEN_ACL_UNSAFE,
						CreateMode.PERSISTENT);
			}
		} catch (IOException e) {
			exception.add(e);
		} catch (KeeperException e) {
			exception.add(e);
		} catch (InterruptedException e) {
			exception.add(e);
		}
	}

	/**
	 * zookeeper节点的监视器
	 */
	public void process(WatchedEvent event) {
		if (this.latch != null) {
			this.latch.countDown();
		}
	}

	public void lock() {
		if (exception.size() > 0) {
			throw new LockException(exception.get(0));
		}
		try {
			if (this.tryLock()) {
				System.out.println("Thread " + Thread.currentThread().getId()
						+ " " + myZnode + " get lock true");
				return;
			} else {
				waitForLock(waitNode, sessionTimeout);// 等待锁
			}
		} catch (KeeperException e) {
			throw new LockException(e);
		} catch (InterruptedException e) {
			throw new LockException(e);
		}
	}

	public boolean tryLock() {
		try {
			String splitStr = "_lock_";
			if (lockName.contains(splitStr))
				throw new LockException("lockName can not contains \\u000B");
			// 创建临时子节点
			zk.create(root + "/" + lockName + splitStr, new byte[0],
					ZooDefs.Ids.OPEN_ACL_UNSAFE,
					CreateMode.EPHEMERAL);
			myZnode = waitNode = lockName;
			System.out.println(myZnode + " is created ");
			return true;
		} catch (KeeperException e) {
			//throw new LockException(e);
		} catch (InterruptedException e) {
			//throw new LockException(e);
		}
		return false;
	}

	public boolean tryLock(long time, TimeUnit unit) {
		try {
			if (this.tryLock()) {
				return true;
			}
			return waitForLock(waitNode, time);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	private boolean waitForLock(String lower, long waitTime)
			throws InterruptedException, KeeperException {
		Stat stat = zk.exists(root + "/" + lower,true);
		// 判断自己的节点是否存在,如果不存在则无需等待锁,同时注册监听
		if (stat != null) {
			System.out.println("Thread " + Thread.currentThread().getId()
					+ " waiting for " + root + "/" + lower);
			this.latch = new CountDownLatch(1);
			this.latch.await(waitTime, TimeUnit.MILLISECONDS);
			this.latch = null;
		}
		return tryLock();
	}

	public void unlock() {
		try {
			System.out.println("unlock " + myZnode);
			zk.delete(myZnode, -1);
			myZnode = null;
			zk.close();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (KeeperException e) {
			e.printStackTrace();
		}
	}

	public void lockInterruptibly() throws InterruptedException {
		this.lock();
	}

	public Condition newCondition() {
		return null;
	}

	public class LockException extends RuntimeException {
		private static final long serialVersionUID = 1L;

		public LockException(String e) {
			super(e);
		}

		public LockException(Exception e) {
			super(e);
		}
	}

}
