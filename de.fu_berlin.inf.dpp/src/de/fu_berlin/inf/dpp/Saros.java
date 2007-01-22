/*
 * DPP - Serious Distributed Pair Programming
 * (c) Freie Universitšt Berlin - Fachbereich Mathematik und Informatik - 2006
 * (c) Riad Djemili - 2006
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 1, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package de.fu_berlin.inf.dpp;

import java.awt.Toolkit;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import javax.sound.midi.Transmitter;
import javax.swing.text.DefaultEditorKit.BeepAction;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.osgi.framework.BundleContext;

import de.fu_berlin.inf.dpp.net.IConnectionListener;
import de.fu_berlin.inf.dpp.net.JID;
import de.fu_berlin.inf.dpp.net.internal.PacketExtensions;
import de.fu_berlin.inf.dpp.project.ActivityRegistry;
import de.fu_berlin.inf.dpp.project.SessionManager;
import de.fu_berlin.inf.dpp.project.internal.SharedProject;
import de.fu_berlin.inf.dpp.ui.SarosUI;
import de.fu_berlin.inf.dpp.ui.wizards.ConfigurationWizard;

/**
 * The main plug-in of Saros.
 * 
 * @author rdjemili
 * @author coezbek
 */
public class Saros extends AbstractUIPlugin {
	public static enum ConnectionState {
		NOT_CONNECTED, CONNECTING, CONNECTED, DISCONNECTING, ERROR
	};

	// The shared instance.
	private static Saros plugin;

	public static final String SAROS = "de.fu_berlin.inf.dpp"; //$NON-NLS-1$

	private static SarosUI uiInstance;

	private XMPPConnection connection;

	private ConnectionState connectionState = ConnectionState.NOT_CONNECTED;
	
	private String connectionError;

	private MessagingManager messagingManager;

	private SessionManager sessionManager;

	// TODO use ListenerList instead
	private List<IConnectionListener> listeners = new CopyOnWriteArrayList<IConnectionListener>();

	// Smack (XMPP) connection listener 
	private ConnectionListener smackConnectionListener = new ConnectionListener() {
		public void connectionClosed() {
			// self inflicted, controlled disconnect
		}
		
		public void connectionClosedOnError(Exception e) { 
			Toolkit.getDefaultToolkit().beep();
			System.out.println("XMPP Connection Error: "+e.toString());
			
			// attempt reconnection
			connect(true);
			
			if (connection.isConnected()) {
				// successfull
				sessionManager.OnReconnect();
				System.out.println("XMPP reconnected (quick)");
				
				
			} else {
				// failed - tell the user
				setConnectionState(ConnectionState.ERROR, "XMPP Connection Error");
				
				Display.getDefault().syncExec(new Runnable() {
					public void run() {
						boolean retry=true;
						while (retry) {
						
							retry=MessageDialog.openQuestion(Display.getDefault().getActiveShell(),
									"Network error", "Network error occured. Do you want to reconnect?\nIf not, the session will be disconnected.");
						
							if (retry) {
								connect(true);
								if (connection.isConnected()) {
									sessionManager.OnReconnect();
									System.out.println("XMPP reconnected (requested)");
									break;
								}
							}
							else {
								disconnect("XMPP connection error");
							}
						
						}
					}
				});

				
//				if( Saros.getDefault().getSessionManager().getSharedProject().isDriver())
			
			}
		} 
	}; 
	
	static {
		PacketExtensions.hookExtensionProviders();
	}

	/**
	 * Create the shared instance.
	 */
	public Saros() {
		plugin = this;
	}

	/**
	 * This method is called upon plug-in activation
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);

		XMPPConnection.DEBUG_ENABLED = getPreferenceStore().getBoolean(PreferenceConstants.DEBUG);

		setupLoggers();

		messagingManager = new MessagingManager();
		sessionManager = new SessionManager();

		ActivityRegistry.getDefault();
		SkypeManager.getDefault();

		uiInstance = new SarosUI(sessionManager);

		boolean hasUserName = getPreferenceStore().getString(PreferenceConstants.USERNAME).length() > 0;
		
		if (getPreferenceStore().getBoolean(PreferenceConstants.AUTO_CONNECT) && hasUserName) {
			asyncConnect();
		}
		
		if (!hasUserName){
			
			Display.getDefault().asyncExec(new Runnable() {
				public void run() {
					try {
						Shell shell = Display.getDefault().getActiveShell();
						new WizardDialog(shell, new ConfigurationWizard()).open();
					} catch (Exception e) {
						Saros.getDefault().getLog().log(
							new Status(IStatus.ERROR, Saros.SAROS, IStatus.ERROR,
								"Error while running configuration wizard", e));
					}
				}
			});
		}
		
	}

	/**
	 * This method is called when the plug-in is stopped
	 */
	public void stop(BundleContext context) throws Exception {
		super.stop(context);

		sessionManager.leaveSession();
		disconnect(null);

		plugin = null;
	}

	/**
	 * Returns the shared instance.
	 * 
	 * @return the shared instance.
	 */
	public static Saros getDefault() {
		return plugin;
	}

	public JID getMyJID() {
		return isConnected() ? new JID(connection.getUser()) : null;
	}

	public SarosUI getUI() { // HACK
		return uiInstance;
	}

	public Roster getRoster() {
		if (!isConnected())
			return null;

		return connection.getRoster();
	}

	/**
	 * @return the MessagingManager which is responsible for handling instant
	 *         messaging. Is never <code>null</code>.
	 */
	public MessagingManager getMessagingManager() {
		return messagingManager;
	}

	/**
	 * @return the SessionManager. Is never <code>null</code>.
	 */
	public SessionManager getSessionManager() {
		return sessionManager;
	}

	public void asyncConnect() {
		new Thread(new Runnable() {
			public void run() {
				connect(false);
			}
		}).start();
	}

	/**
	 * Connects according to the preferences. This is a blocking method.
	 * 
	 * If there is already a established connection when calling this method, it
	 * disconnects before connecting.
	 */
	public void connect(boolean reconnect) {

		IPreferenceStore prefStore = getPreferenceStore();
		final String server = prefStore.getString(PreferenceConstants.SERVER);
		final String username = prefStore.getString(PreferenceConstants.USERNAME);
		String password = prefStore.getString(PreferenceConstants.PASSWORD);

		try {
			if (!reconnect) {
				if (isConnected())
					disconnect(null);

				setConnectionState(ConnectionState.CONNECTING, null);
			} else  if (isConnected()) {
				connection.close();
				connection.removeConnectionListener(smackConnectionListener);
			}

			connection = new XMPPConnection(server);
			connection.login(username, password);
			
			connection.addConnectionListener(smackConnectionListener);
			
			setConnectionState(ConnectionState.CONNECTED, null);
			

		} catch (final Exception e) {
			//disconnect(e.getMessage());

			if (!reconnect){
				setConnectionState(ConnectionState.NOT_CONNECTED, null);				
				Display.getDefault().syncExec(new Runnable() {
					public void run() {
						MessageDialog.openError(Display.getDefault().getActiveShell(),
							"Error Connecting", "Could not connect to server '" + server
								+ "' as user '" + username + "'.\nErrorMessage was: " + e.getMessage());
					}
				});
			}
		}
	}

	/**
	 * Disconnects. This is a blocking method.
	 * 
	 * @param reason
	 *            the error why the connection was closed. If the connection was
	 *            not closed due to an error <code>null</code> should be
	 *            passed.
	 */
	public void disconnect(String error) {
		setConnectionState(ConnectionState.DISCONNECTING, error);

		if (connection != null) {
			connection.removeConnectionListener(smackConnectionListener);
			connection.close();
			connection = null;
		}

		setConnectionState(error == null ? ConnectionState.NOT_CONNECTED : ConnectionState.ERROR,
			error);

	}

	/**
	 * Creates the given account on the given Jabber server. This is a blocking
	 * method.
	 * 
	 * @param server
	 *            the server on which to create the account.
	 * @param username
	 *            the username for the new account.
	 * @param password
	 *            the password for the new account.
	 * @param monitor
	 *            the progressmonitor for the operation.
	 * @throws XMPPException
	 *             exception that occcurs while registering.
	 */
	public void createAccount(String server, String username, String password,
		IProgressMonitor monitor) throws XMPPException {

		monitor.beginTask("Registering account", 3);

		XMPPConnection connection = new XMPPConnection(server);
		monitor.worked(1);

		connection.getAccountManager().createAccount(username, password);
		monitor.worked(1);

		connection.close();
		monitor.done();
	}

	/**
	 * Adds given contact to the roster. This is a blocking method.
	 * 
	 * @param jid
	 *            the Jabber ID of the contact.
	 * @param nickname
	 *            the nickname under which the new contact should appear in the
	 *            roster.
	 * @param groups
	 *            the groups to which the new contact should belong to. This
	 *            information will be saved on the server.
	 * @throws XMPPException
	 *             is thrown if no connection is establised.
	 */
	public void addContact(JID jid, String nickname, String[] groups) throws XMPPException {
		assertConnection();
		connection.getRoster().createEntry(jid.toString(), nickname, groups);
	}

	/**
	 * Removes given contact from the roster. This is a blocking method.
	 * 
	 * @param rosterEntry
	 *            the contact that is to be removed
	 * @throws XMPPException
	 *             is thrown if no connection is establised.
	 */
	public void removeContact(RosterEntry rosterEntry) throws XMPPException {
		assertConnection();
		connection.getRoster().removeEntry(rosterEntry);
	}

	public boolean isConnected() {
		return connection != null && connection.isConnected();
	}

	/**
	 * @return the current state of the connection.
	 */
	public ConnectionState getConnectionState() {
		return connectionState;
	}

	/**
	 * @return an error string that contains the error message for the current
	 *         connection error if the state is {@link ConnectionState.ERROR} or
	 *         <code>null</code> if there is another state set.
	 */
	public String getConnectionError() {
		return connectionError;
	}

	/**
	 * @return the currently established connection or <code>null</code> if
	 *         there is none.
	 */
	public XMPPConnection getConnection() {
		return connection;
	}

	public void addListener(IConnectionListener listener) {
		if (!listeners.contains(listener)) {
			listeners.add(listener);
		}
	}

	public void removeListener(IConnectionListener listener) {
		listeners.remove(listener);
	}

	private void assertConnection() throws XMPPException {
		if (!isConnected())
			throw new XMPPException("No connection");
	}

	/**
	 * Sets a new connection state and notifies all connection listeners.
	 */
	private void setConnectionState(ConnectionState state, String error) {
		connectionState = state;
		connectionError = error;

		for (IConnectionListener listener : listeners) {
			listener.connectionStateChanged(connection, state);
		}
	}

	private void setupLoggers() {
		try {
			Logger sarosRootLogger = Logger.getLogger("de.fu_berlin.inf.dpp");
			sarosRootLogger.setLevel(Level.ALL);

			Handler handler = new FileHandler("saros.log", 10 * 1024 * 1024, 1, true);
			handler.setFormatter(new SimpleFormatter());
			sarosRootLogger.addHandler(handler);

			// handler = new ConsoleHandler();
			// sarosRootLogger.addHandler(handler);

		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Log a message to the Eclipse ErrorLog. This method should be used to log
	 * all errors that occur in the plugin that cannot be corrected by the user
	 * and seem to be errors within the plug-in or the used libraries.
	 * 
	 * @param message
	 *            A meaningful description of during which operation the error
	 *            occurred
	 * @param e
	 *            The exception associated with the error (may be null)
	 */
	public static void log(String message, Exception e) {
		Saros.getDefault().getLog().log(
			new Status(IStatus.ERROR, Saros.SAROS, IStatus.ERROR, message, e));
	}

}
