/*
 * DPP - Serious Distributed Pair Programming
 * (c) Freie Universitaet Berlin - Fachbereich Mathematik und Informatik - 2006
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
package de.fu_berlin.inf.dpp.ui.actions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.actions.SelectionProviderAction;
import org.jivesoftware.smack.RosterEntry;

import de.fu_berlin.inf.dpp.Saros;
import de.fu_berlin.inf.dpp.invitation.IIncomingInvitationProcess;
import de.fu_berlin.inf.dpp.net.JID;
import de.fu_berlin.inf.dpp.project.ISessionListener;
import de.fu_berlin.inf.dpp.project.ISharedProject;
import de.fu_berlin.inf.dpp.ui.SarosUI;

/**
 * Action to start an Invitation for the currently selected RosterEntries.
 * 
 * @author rdjemili
 * @author oezbek
 */
public class InviteAction extends SelectionProviderAction {

    private static final Logger log = Logger.getLogger(InviteAction.class
        .getName());

    public InviteAction(ISelectionProvider provider) {
        super(provider, "Invite user to shared project..");
        setToolTipText("Invite user to shared project..");

        setImageDescriptor(SarosUI
            .getImageDescriptor("icons/transmit_blue.png"));

        Saros.getDefault().getSessionManager().addSessionListener(
            new ISessionListener() {
                public void sessionStarted(ISharedProject session) {
                    updateEnablement();
                }

                public void sessionEnded(ISharedProject session) {
                    updateEnablement();
                }

                public void invitationReceived(
                    IIncomingInvitationProcess process) {
                    // does not affect us (because we are not host if we receive
                    // an invitation)
                }
            });

        updateEnablement();
    }

    @Override
    public void run() {
        try {
            ISharedProject project = Saros.getDefault().getSessionManager()
                .getSharedProject();

            project.startInvitation(getSelected());
        } catch (RuntimeException e) {
            log.error("Internal Error in InviteAction:", e);
        }
    }

    @Override
    public void selectionChanged(IStructuredSelection selection) {
        updateEnablement();
    }

    public void updateEnablement() {
        setEnabled(canInviteSelected());
    }

    public List<JID> getSelected() {
        ArrayList<JID> selected = new ArrayList<JID>();

        for (Object o : getStructuredSelection().toList()) {
            if (!(o instanceof RosterEntry)) {
                return Collections.emptyList();
            } else {
                selected.add(new JID(((RosterEntry) o).getUser()));
            }
        }

        return selected;
    }

    public boolean canInviteSelected() {

        try {
            ISharedProject project = Saros.getDefault().getSessionManager()
                .getSharedProject();

            List<JID> selected = getSelected();

            if (project == null || !project.isHost() || selected.isEmpty()) {
                return false;
            }

            for (JID jid : selected) {

                // Participant needs to be...
                // ...available
                if (!Saros.getDefault().getRoster().getPresence(jid.toString())
                    .isAvailable())
                    return false;

                // ...not in a session already
                if (project.getParticipant(jid) != null)
                    return false;

                // ...use a Saros enabled client
                if (!Saros.getDefault().hasSarosSupport(jid.toString()))
                    return false;
            }
        } catch (RuntimeException e) {
            log.error("Internal Error while updating InviteAction:", e);
            return false;
        }
        return true;
    }
}
