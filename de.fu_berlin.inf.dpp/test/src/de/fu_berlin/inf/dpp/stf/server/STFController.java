package de.fu_berlin.inf.dpp.stf.server;

import java.rmi.AccessException;
import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

import org.apache.log4j.Logger;

import de.fu_berlin.inf.dpp.Saros;
import de.fu_berlin.inf.dpp.accountManagement.XMPPAccountStore;
import de.fu_berlin.inf.dpp.editor.EditorManager;
import de.fu_berlin.inf.dpp.feedback.FeedbackManager;
import de.fu_berlin.inf.dpp.net.internal.DataTransferManager;
import de.fu_berlin.inf.dpp.project.SarosSessionManager;
import de.fu_berlin.inf.dpp.stf.STF;
import de.fu_berlin.inf.dpp.stf.client.AbstractTester;
import de.fu_berlin.inf.dpp.stf.server.rmiSarosSWTBot.finder.STFBotImp;
import de.fu_berlin.inf.dpp.stf.server.rmiSarosSWTBot.finder.STFWorkbenchBotImp;
import de.fu_berlin.inf.dpp.stf.server.rmiSarosSWTBot.finder.remoteWidgets.STFBotButtonImp;
import de.fu_berlin.inf.dpp.stf.server.rmiSarosSWTBot.finder.remoteWidgets.STFBotComboImp;
import de.fu_berlin.inf.dpp.stf.server.rmiSarosSWTBot.finder.remoteWidgets.STFBotEditorImp;
import de.fu_berlin.inf.dpp.stf.server.rmiSarosSWTBot.finder.remoteWidgets.STFBotLabelImp;
import de.fu_berlin.inf.dpp.stf.server.rmiSarosSWTBot.finder.remoteWidgets.STFBotListImp;
import de.fu_berlin.inf.dpp.stf.server.rmiSarosSWTBot.finder.remoteWidgets.STFBotMenuImp;
import de.fu_berlin.inf.dpp.stf.server.rmiSarosSWTBot.finder.remoteWidgets.STFBotShellImp;
import de.fu_berlin.inf.dpp.stf.server.rmiSarosSWTBot.finder.remoteWidgets.STFBotTableImp;
import de.fu_berlin.inf.dpp.stf.server.rmiSarosSWTBot.finder.remoteWidgets.STFBotTableItemImp;
import de.fu_berlin.inf.dpp.stf.server.rmiSarosSWTBot.finder.remoteWidgets.STFBotTextImp;
import de.fu_berlin.inf.dpp.stf.server.rmiSarosSWTBot.finder.remoteWidgets.STFBotToolbarButtonImp;
import de.fu_berlin.inf.dpp.stf.server.rmiSarosSWTBot.finder.remoteWidgets.STFBotTreeImp;
import de.fu_berlin.inf.dpp.stf.server.rmiSarosSWTBot.finder.remoteWidgets.STFBotTreeItemImp;
import de.fu_berlin.inf.dpp.stf.server.rmiSarosSWTBot.finder.remoteWidgets.STFBotViewImp;
import de.fu_berlin.inf.dpp.stf.server.rmiSarosSWTBot.noFinder.NoBotImp;
import de.fu_berlin.inf.dpp.stf.server.rmiSarosSWTBot.superFinder.SuperBotImp;
import de.fu_berlin.inf.dpp.stf.server.rmiSarosSWTBot.superFinder.StateImp;
import de.fu_berlin.inf.dpp.stf.server.rmiSarosSWTBot.superFinder.WaitImp;
import de.fu_berlin.inf.dpp.stf.server.rmiSarosSWTBot.superFinder.remoteComponents.Component;
import de.fu_berlin.inf.dpp.stf.server.rmiSarosSWTBot.superFinder.remoteComponents.contextMenu.ContextMenuWrapperImp;
import de.fu_berlin.inf.dpp.stf.server.rmiSarosSWTBot.superFinder.remoteComponents.contextMenu.NewCImp;
import de.fu_berlin.inf.dpp.stf.server.rmiSarosSWTBot.superFinder.remoteComponents.contextMenu.RefactorCImp;
import de.fu_berlin.inf.dpp.stf.server.rmiSarosSWTBot.superFinder.remoteComponents.contextMenu.SarosContextMenuWrapperImp;
import de.fu_berlin.inf.dpp.stf.server.rmiSarosSWTBot.superFinder.remoteComponents.contextMenu.ShareWithCImp;
import de.fu_berlin.inf.dpp.stf.server.rmiSarosSWTBot.superFinder.remoteComponents.contextMenu.TeamCImp;
import de.fu_berlin.inf.dpp.stf.server.rmiSarosSWTBot.superFinder.remoteComponents.menuBar.SarosMImp;
import de.fu_berlin.inf.dpp.stf.server.rmiSarosSWTBot.superFinder.remoteComponents.menuBar.SarosPreferencesImp;
import de.fu_berlin.inf.dpp.stf.server.rmiSarosSWTBot.superFinder.remoteComponents.menuBar.WindowMImp;
import de.fu_berlin.inf.dpp.stf.server.rmiSarosSWTBot.superFinder.remoteComponents.views.ConsoleViewImp;
import de.fu_berlin.inf.dpp.stf.server.rmiSarosSWTBot.superFinder.remoteComponents.views.PEViewImp;
import de.fu_berlin.inf.dpp.stf.server.rmiSarosSWTBot.superFinder.remoteComponents.views.ProgressViewImp;
import de.fu_berlin.inf.dpp.stf.server.rmiSarosSWTBot.superFinder.remoteComponents.views.ViewsImp;
import de.fu_berlin.inf.dpp.stf.server.rmiSarosSWTBot.superFinder.remoteComponents.views.sarosViews.BuddiesViewImp;
import de.fu_berlin.inf.dpp.stf.server.rmiSarosSWTBot.superFinder.remoteComponents.views.sarosViews.ChatViewImp;
import de.fu_berlin.inf.dpp.stf.server.rmiSarosSWTBot.superFinder.remoteComponents.views.sarosViews.RSViewImp;
import de.fu_berlin.inf.dpp.stf.server.rmiSarosSWTBot.superFinder.remoteComponents.views.sarosViews.SessionViewImp;

/**
 * SarosRmiSWTWorkbenchBot controls Eclipse Saros from the GUI perspective. It
 * exports {@link SarosState} via RMI. You should not use this within tests.
 * Have a look at {@link AbstractTester} if you want to write tests.
 * 
 */
public class STFController {

    private static final transient Logger log = Logger
        .getLogger(STFController.class);

    public static int sleepTime = 750;

    /** The RMI registry used, is not exported */
    private static transient Registry registry;

    /*
     * sometimes when connecting to a server i'm getting error:
     * java.rmi.NoSuchObjectException:no Such object in table. This happens when
     * the remote object the stub refers to has been DGC'd and GC's locally. My
     * solution is keeping a static references "classVariable" to the object in
     * the object in the server JVM.
     */
    public static void exportedObjects(int port, Saros saros,
        SarosSessionManager sessionManager,
        DataTransferManager dataTransferManager, EditorManager editorManager,
        XMPPAccountStore xmppAccountStore, FeedbackManager feedbackManager)
        throws RemoteException {

        STF.saros = saros;
        Component.sessionManager = sessionManager;
        Component.dataTransferManager = dataTransferManager;
        Component.editorManager = editorManager;
        Component.xmppAccountStore = xmppAccountStore;
        Component.feedbackManager = feedbackManager;
        try {
            registry = LocateRegistry.createRegistry(port);
        } catch (RemoteException e) {
            registry = LocateRegistry.getRegistry(port);
        }

        exportObject(STFBotImp.getInstance(), "stfBot");
        exportObject(STFWorkbenchBotImp.getInstance(), "bot");
        exportObject(NoBotImp.getInstance(), "noBot");
        exportObject(SuperBotImp.getInstance(), "sarosBot");
        exportObject(STFBotShellImp.getInstance(), "shell");
        exportObject(STFBotTableImp.getInstance(), "table");
        exportObject(STFBotTableItemImp.getInstance(), "tableItem");
        exportObject(STFBotTreeImp.getInstance(), "tree");
        exportObject(STFBotTreeItemImp.getInstance(), "treeItem");
        exportObject(STFBotButtonImp.getInstance(), "button");
        exportObject(STFBotToolbarButtonImp.getInstance(), "toolbarButton");
        exportObject(STFBotViewImp.getInstance(), "view");
        exportObject(STFBotMenuImp.getInstance(), "menu");
        exportObject(STFBotLabelImp.getInstance(), "label");
        exportObject(STFBotTextImp.getInstance(), "text");
        exportObject(STFBotListImp.getInstance(), "list");
        exportObject(PEViewImp.getInstance(), "packageExplorerView");
        exportObject(ProgressViewImp.getInstance(), "progressView");
        exportObject(STFBotEditorImp.getInstance(), "eclipseEditor");
        exportObject(BuddiesViewImp.getInstance(), "rosterView");
        exportObject(SessionViewImp.getInstance(), "sessionView");
        exportObject(RSViewImp.getInstance(), "remoteScreenView");
        exportObject(ChatViewImp.getInstance(), "chatView");
        exportObject(ConsoleViewImp.getInstance(), "consoleView");
        exportObject(NewCImp.getInstance(), "fileM");
        exportObject(STFBotComboImp.getInstance(), "combo");
        exportObject(RefactorCImp.getInstance(), "refactorM");
        exportObject(WindowMImp.getInstance(), "windowM");
        exportObject(SarosMImp.getInstance(), "sarosM");
        exportObject(TeamCImp.getInstance(), "team");
        exportObject(ShareWithCImp.getInstance(), "saros");

        exportObject(ContextMenuWrapperImp.getInstance(), "contextMenu");
        exportObject(SarosContextMenuWrapperImp.getInstance(),
            "sarosContextMenu");
        exportObject(SarosPreferencesImp.getInstance(), "sarosPreferences");
        exportObject(StateImp.getInstance(), "state");
        exportObject(WaitImp.getInstance(), "wait");
        exportObject(ViewsImp.getInstance(), "views");
    }

    /**
     * Add a shutdown hook to unbind exported Object from registry.
     */
    private static void addShutdownHook(final String name) {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                try {
                    if (registry != null && name != null)
                        registry.unbind(name);
                } catch (RemoteException e) {
                    log.warn("Failed to unbind: " + name, e);
                } catch (NotBoundException e) {
                    log.warn("Failed to unbind: " + name, e);
                }
            }
        });
    }

    /**
     * Export object by given name on our local RMI Registry.
     */
    private static Remote exportObject(Remote exportedObject, String exportName) {
        try {
            Remote remoteObject = UnicastRemoteObject.exportObject(
                exportedObject, 0);
            addShutdownHook(exportName);
            registry.bind(exportName, remoteObject);
            return remoteObject;
        } catch (RemoteException e) {
            log.error("Could not export the object " + exportName, e);
        } catch (AlreadyBoundException e) {
            log.error("Could not bind the object " + exportName
                + ", because it is bound already.", e);
        }
        return null;
    }

    public static void listRmiObjects() {
        try {
            for (String s : registry.list())
                log.debug("registered Object: " + s);
        } catch (AccessException e) {
            log.error("Failed on access", e);
        } catch (RemoteException e) {
            log.error("Failed", e);
        }
    }
}
