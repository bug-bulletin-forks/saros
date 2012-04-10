package de.fu_berlin.inf.dpp.activities.serializable;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamConverter;

import de.fu_berlin.inf.dpp.activities.business.IActivity;
import de.fu_berlin.inf.dpp.activities.business.StopActivity;
import de.fu_berlin.inf.dpp.activities.business.StopActivity.State;
import de.fu_berlin.inf.dpp.activities.business.StopActivity.Type;
import de.fu_berlin.inf.dpp.net.JID;
import de.fu_berlin.inf.dpp.project.ISarosSession;
import de.fu_berlin.inf.dpp.util.xstream.JIDConverter;

/**
 * A StopActivityDataObject is used for signaling to a user that he should be
 * stopped or started (meaning that no more activityDataObjects should be
 * generated by this user).
 */
@XStreamAlias("stopActivity")
public class StopActivityDataObject extends AbstractActivityDataObject {

    @XStreamAsAttribute
    @XStreamConverter(JIDConverter.class)
    protected JID initiator;

    // the user who has to be locked / unlocked
    @XStreamAsAttribute
    @XStreamConverter(JIDConverter.class)
    protected JID user;

    @XStreamAsAttribute
    protected Type type;

    @XStreamAsAttribute
    protected State state;

    // a stop activityDataObject has a unique id
    @XStreamAsAttribute
    protected String stopActivityID;

    public StopActivityDataObject(JID source, JID initiator, JID user,
        Type type, State state, String stopActivityID) {

        super(source);

        this.initiator = initiator;
        this.user = user;
        this.state = state;
        this.type = type;
        this.stopActivityID = stopActivityID;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result
            + ((initiator == null) ? 0 : initiator.hashCode());
        result = prime * result + ((state == null) ? 0 : state.hashCode());
        result = prime * result
            + ((stopActivityID == null) ? 0 : stopActivityID.hashCode());
        result = prime * result + ((type == null) ? 0 : type.hashCode());
        result = prime * result + ((user == null) ? 0 : user.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        StopActivityDataObject other = (StopActivityDataObject) obj;
        if (initiator == null) {
            if (other.initiator != null)
                return false;
        } else if (!initiator.equals(other.initiator))
            return false;
        if (state == null) {
            if (other.state != null)
                return false;
        } else if (!state.equals(other.state))
            return false;
        if (stopActivityID == null) {
            if (other.stopActivityID != null)
                return false;
        } else if (!stopActivityID.equals(other.stopActivityID))
            return false;
        if (type == null) {
            if (other.type != null)
                return false;
        } else if (!type.equals(other.type))
            return false;
        if (user == null) {
            if (other.user != null)
                return false;
        } else if (!user.equals(other.user))
            return false;
        return true;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("StopActivityDataObject (id: " + stopActivityID);
        sb.append(", type: " + type);
        sb.append(", state: " + state);
        sb.append(", initiator: " + initiator.toString());
        sb.append(", affected user: " + user.toString());
        sb.append(", src: " + getSource() + ")");
        return sb.toString();
    }

    public IActivity getActivity(ISarosSession sarosSession) {
        return new StopActivity(sarosSession.getUser(source),
            sarosSession.getUser(initiator), sarosSession.getUser(user), type,
            state, stopActivityID);
    }
}