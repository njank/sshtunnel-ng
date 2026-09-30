package org.programmerplanet.sshtunnel.ui;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.swt.widgets.Display;
import org.programmerplanet.sshtunnel.model.ConnectionManager;
import org.programmerplanet.sshtunnel.model.Session;

/**
 * 
 * @author <a href="agungm@outlook.com">Mulya Agung</a>
 */

public class SessionConnectionMonitor implements Runnable {

	private static final Log log = LogFactory.getLog(SessionConnectionMonitor.class);

	private static final int DEF_MONITOR_INTERVAL = 10000;
	private static final SessionConnectionMonitor INSTANCE = new SessionConnectionMonitor();

	private final Object lock = new Object();
	private Thread thread;
	private volatile boolean threadStopped;
	private int monitorInterval;
	private SshTunnelComposite sshTunnelComposite;

	public SessionConnectionMonitor() {
		this(DEF_MONITOR_INTERVAL);
	}

	public SessionConnectionMonitor(int monitorInterval) {
		threadStopped = false;
		this.monitorInterval = monitorInterval;
	}

	public void run() {
		if (log.isWarnEnabled()) {
			log.warn("Connection monitor is now running..");
		}
		while (!threadStopped) {
			try {
				// Checks every open connection, whether it was opened by Connect, Connect All or the tray menu
				final List<Session> lostSessions = ConnectionManager.getInstance().closeDeadConnections();
				if (sshTunnelComposite != null && !lostSessions.isEmpty()) {
					Display.getDefault().asyncExec(new Runnable() {
						public void run() {
							for (Session s : lostSessions) {
								sshTunnelComposite.showDisconnectedMessage(s);
							}
							sshTunnelComposite.connectionStatusChanged();
						}
					});
				}
			} catch (RuntimeException e) {
				// Keep the monitor running
				log.error("Error while checking connections", e);
			}
			try {
				Thread.sleep(monitorInterval);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void setSshTunnelComposite(SshTunnelComposite sshTunnelComposite) {
		this.sshTunnelComposite = sshTunnelComposite;
	}

	public void startMonitor() {
		synchronized (lock) {
			if (thread == null) {
				thread = new Thread(this);
				threadStopped = false;
				thread.start();
			}
		}
	}

	public void setThreadStopped(boolean stop) {
		threadStopped = stop;
	}

	public void stopMonitor() {
		synchronized (lock) {
			if (thread != null) {
				threadStopped = true;
				thread = null;
				
				if (log.isWarnEnabled()) {
					log.warn("Connection monitor is stopped.");
				}
			}
		}
	}

	public static SessionConnectionMonitor getInstance() {
		return INSTANCE;
	}

}
