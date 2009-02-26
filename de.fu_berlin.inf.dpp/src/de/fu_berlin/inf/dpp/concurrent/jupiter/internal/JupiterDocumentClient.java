package de.fu_berlin.inf.dpp.concurrent.jupiter.internal;

import org.apache.log4j.Logger;
import org.eclipse.core.runtime.IPath;

import de.fu_berlin.inf.dpp.concurrent.jupiter.Algorithm;
import de.fu_berlin.inf.dpp.concurrent.jupiter.JupiterClient;
import de.fu_berlin.inf.dpp.concurrent.jupiter.Operation;
import de.fu_berlin.inf.dpp.concurrent.jupiter.Request;
import de.fu_berlin.inf.dpp.concurrent.jupiter.RequestForwarder;
import de.fu_berlin.inf.dpp.concurrent.jupiter.Timestamp;
import de.fu_berlin.inf.dpp.concurrent.jupiter.TransformationException;
import de.fu_berlin.inf.dpp.net.JID;

/**
 * The client-side handling for a single document
 */
public class JupiterDocumentClient implements JupiterClient {

    private static Logger logger = Logger.getLogger(JupiterDocumentClient.class
        .toString());

    /**
     * JID of remote client
     */
    private final JID jid;

    /**
     * jupiter sync algorithm.
     */
    private final Algorithm jupiter;

    private IPath editor;

    /** forwarder send request to server. */
    private final RequestForwarder forwarder;

    public JupiterDocumentClient(JID jid, RequestForwarder forwarder,
        IPath editor) {
        this.jid = jid;
        this.jupiter = new Jupiter(true);
        this.forwarder = forwarder;
        this.editor = editor;
    }

    public Request generateRequest(Operation op) {
        Request req = null;
        JupiterDocumentClient.logger.debug(this.jid.toString()
            + " client generate request for " + op);
        req = this.jupiter.generateRequest(op);
        req.setJID(this.jid);
        req.setEditorPath(this.editor);
        /* send request */
        this.forwarder.forwardOutgoingRequest(req);

        return req;
    }

    public Operation receiveRequest(Request req) throws TransformationException {

        JupiterDocumentClient.logger.debug(this.jid.toString()
            + " client receive request " + req.getOperation());

        Operation result = this.jupiter.receiveRequest(req);

        JupiterDocumentClient.logger.debug(this.jid.toString()
            + " client operation of IT: " + result);

        return result;
    }

    public JID getJID() {
        return this.jid;
    }

    public Timestamp getTimestamp() {
        return this.jupiter.getTimestamp();
    }

    public void updateVectorTime(Timestamp timestamp)
        throws TransformationException {
        this.jupiter.updateVectorTime(timestamp);
    }

}
