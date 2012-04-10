package de.fu_berlin.inf.dpp.activities.serializable;

import org.eclipse.jface.text.source.ILineRange;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;

import de.fu_berlin.inf.dpp.activities.SPathDataObject;
import de.fu_berlin.inf.dpp.activities.business.IActivity;
import de.fu_berlin.inf.dpp.activities.business.ViewportActivity;
import de.fu_berlin.inf.dpp.net.JID;
import de.fu_berlin.inf.dpp.project.ISarosSession;

@XStreamAlias("viewportActivity")
public class ViewportActivityDataObject extends
    AbstractProjectActivityDataObject {
    @XStreamAlias("t")
    @XStreamAsAttribute
    protected final int topIndex;

    @XStreamAlias("b")
    @XStreamAsAttribute
    protected final int bottomIndex;

    public ViewportActivityDataObject(JID source, int topIndex,
        int bottomIndex, SPathDataObject path) {
        super(source, path);

        if (path == null) {
            throw new IllegalArgumentException("editor must not be null");
        }

        assert topIndex <= bottomIndex : "Top == " + topIndex + ", Bottom == "
            + bottomIndex;

        this.topIndex = topIndex;
        this.bottomIndex = bottomIndex;
    }

    public ViewportActivityDataObject(JID source, ILineRange viewport,
        SPathDataObject editor) {

        this(source, Math.max(0, viewport.getStartLine()), Math.max(0,
            viewport.getStartLine())
            + Math.max(0, viewport.getNumberOfLines()), editor);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + bottomIndex;
        result = prime * result + ((path == null) ? 0 : path.hashCode());
        result = prime * result + topIndex;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (!(obj instanceof ViewportActivityDataObject))
            return false;
        ViewportActivityDataObject other = (ViewportActivityDataObject) obj;
        if (bottomIndex != other.bottomIndex)
            return false;
        if (path == null) {
            if (other.path != null)
                return false;
        } else if (!path.equals(other.path))
            return false;
        if (topIndex != other.topIndex)
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "ViewportActivityDataObject(path:" + this.path + ",range:("
            + this.topIndex + "," + this.bottomIndex + "))";
    }

    public IActivity getActivity(ISarosSession sarosSession) {
        return new ViewportActivity(sarosSession.getUser(source), topIndex,
            bottomIndex, path.toSPath(sarosSession));
    }
}
