// Copyright 2003-2005 Arthur van Hoff, Rick Blair
// Licensed under Apache License version 2.0
// Original license LGPL

package javax.jmdns.impl.tasks.state;

import java.io.IOException;
import java.util.Timer;
import java.util.logging.Logger;

import javax.jmdns.impl.DNSOutgoing;
import javax.jmdns.impl.DNSQuestion;
import javax.jmdns.impl.DNSRecord;
import javax.jmdns.impl.JmDNSImpl;
import javax.jmdns.impl.ServiceInfoImpl;
import javax.jmdns.impl.constants.DNSConstants;
import javax.jmdns.impl.constants.DNSRecordClass;
import javax.jmdns.impl.constants.DNSRecordType;
import javax.jmdns.impl.constants.DNSState;

/**
 * The Prober sends three consecutive probes for all service infos that needs probing as well as for the host name. The state of each service info of the host name is advanced, when a probe has been sent for it. When the prober has run three times,
 * it launches an Announcer.
 * <p/>
 * If a conflict during probes occurs, the affected service infos (and affected host name) are taken away from the prober. This eventually causes the prober to cancel itself.
 */
public class Prober extends DNSStateTask {
    static Logger logger = Logger.getLogger(Prober.class.getName());

    public Prober(JmDNSImpl jmDNSImpl) {
        super(jmDNSImpl, defaultTTL());

        this.setTaskState(DNSState.PROBING_1);
        this.associate(DNSState.PROBING_1);
    }

    /*
     * (non-Javadoc)
     * @see javax.jmdns.impl.tasks.DNSTask#getName()
     */
    @Override
    public String getName() {
        return "Prober(" + (this.getDns() != null ? this.getDns().getName() : "") + ")";
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return super.toString() + " state: " + this.getTaskState();
    }

    /*
     * (non-Javadoc)
     * @see javax.jmdns.impl.tasks.DNSTask#start(java.util.Timer)
     */
    @Override
    public void start(Timer timer) {
        long now = System.currentTimeMillis();
        if (now - this.getDns().getLastThrottleIncrement() < DNSConstants.PROBE_THROTTLE_COUNT_INTERVAL) {
            this.getDns().setThrottle(this.getDns().getThrottle() + 1);
        } else {
            this.getDns().setThrottle(1);
        }
        this.getDns().setLastThrottleIncrement(now);

        if (this.getDns().isAnnounced() && this.getDns().getThrottle() < DNSConstants.PROBE_THROTTLE_COUNT) {
            timer.schedule(this, JmDNSImpl.getRandom().nextInt(1 + DNSConstants.PROBE_WAIT_INTERVAL), DNSConstants.PROBE_WAIT_INTERVAL);
        } else if (!this.getDns().isCanceling() && !this.getDns().isCanceled()) {
            timer.schedule(this, DNSConstants.PROBE_CONFLICT_INTERVAL, DNSConstants.PROBE_CONFLICT_INTERVAL);
        }
    }

    @Override
    public boolean cancel() {
        this.removeAssociation();

        return super.cancel();
    }

    /*
     * (non-Javadoc)
     * @see javax.jmdns.impl.tasks.state.DNSStateTask#getTaskDescription()
     */
    @Override
    public String getTaskDescription() {
        return "probing";
    }

    /*
     * (non-Javadoc)
     * @see javax.jmdns.impl.tasks.state.DNSStateTask#checkRunCondition()
     */
    @Override
    protected boolean checkRunCondition() {
        return !this.getDns().isCanceling() && !this.getDns().isCanceled();
    }

    /*
     * (non-Javadoc)
     * @see javax.jmdns.impl.tasks.state.DNSStateTask#createOugoing()
     */
    @Override
    protected DNSOutgoing createOugoing() {
        return new DNSOutgoing(DNSConstants.FLAGS_QR_QUERY);
    }

    /*
     * (non-Javadoc)
     * @see javax.jmdns.impl.tasks.state.DNSStateTask#buildOutgoingForDNS(javax.jmdns.impl.DNSOutgoing)
     */
    @Override
    protected DNSOutgoing buildOutgoingForDNS(DNSOutgoing out) throws IOException {
        DNSOutgoing newOut = out;
        newOut.addQuestion(DNSQuestion.newQuestion(this.getDns().getLocalHost().getName(), DNSRecordType.TYPE_ANY, DNSRecordClass.CLASS_IN, DNSRecordClass.NOT_UNIQUE));
        for (DNSRecord answer : this.getDns().getLocalHost().answers(DNSRecordClass.CLASS_ANY, DNSRecordClass.NOT_UNIQUE, this.getTTL())) {
            newOut = this.addAuthoritativeAnswer(newOut, answer);
        }
        return newOut;
    }

    /*
     * (non-Javadoc)
     * @see javax.jmdns.impl.tasks.state.DNSStateTask#buildOutgoingForInfo(javax.jmdns.impl.ServiceInfoImpl, javax.jmdns.impl.DNSOutgoing)
     */
    @Override
    protected DNSOutgoing buildOutgoingForInfo(ServiceInfoImpl info, DNSOutgoing out) throws IOException {
        DNSOutgoing newOut = out;
        newOut = this.addQuestion(newOut, DNSQuestion.newQuestion(info.getQualifiedName(), DNSRecordType.TYPE_ANY, DNSRecordClass.CLASS_IN, DNSRecordClass.NOT_UNIQUE));
        // the "unique" flag should be not set here because these answers haven't been proven unique yet this means the record will not exactly match the announcement record
        newOut = this.addAuthoritativeAnswer(newOut, new DNSRecord.Service(info.getQualifiedName(), DNSRecordClass.CLASS_IN, DNSRecordClass.NOT_UNIQUE, this.getTTL(), info.getPriority(), info.getWeight(), info.getPort(), this.getDns().getLocalHost()
                .getName()));
        return newOut;
    }

    /*
     * (non-Javadoc)
     * @see javax.jmdns.impl.tasks.state.DNSStateTask#recoverTask(java.lang.Throwable)
     */
    @Override
    protected void recoverTask(Throwable e) {
        this.getDns().recover();
    }

    /*
     * (non-Javadoc)
     * @see javax.jmdns.impl.tasks.state.DNSStateTask#advanceTask()
     */
    @Override
    protected void advanceTask() {
        this.setTaskState(this.getTaskState().advance());
        if (!this.getTaskState().isProbing()) {
            cancel();

            this.getDns().startAnnouncer();
        }
    }

}