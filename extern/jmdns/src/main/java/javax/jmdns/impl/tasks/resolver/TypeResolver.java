// Copyright 2003-2005 Arthur van Hoff, Rick Blair
// Licensed under Apache License version 2.0
// Original license LGPL

package javax.jmdns.impl.tasks.resolver;

import java.io.IOException;

import javax.jmdns.impl.DNSOutgoing;
import javax.jmdns.impl.DNSQuestion;
import javax.jmdns.impl.DNSRecord;
import javax.jmdns.impl.JmDNSImpl;
import javax.jmdns.impl.JmDNSImpl.ServiceTypeEntry;
import javax.jmdns.impl.constants.DNSConstants;
import javax.jmdns.impl.constants.DNSRecordClass;
import javax.jmdns.impl.constants.DNSRecordType;

/**
 * Helper class to resolve service types.
 * <p/>
 * The TypeResolver queries three times consecutively for service types, and then removes itself from the timer.
 * <p/>
 * The TypeResolver will run only if JmDNS is in state ANNOUNCED.
 */
public class TypeResolver extends DNSResolverTask {

    /**
     * @param jmDNSImpl
     */
    public TypeResolver(JmDNSImpl jmDNSImpl) {
        super(jmDNSImpl);
    }

    /*
     * (non-Javadoc)
     * @see javax.jmdns.impl.tasks.DNSTask#getName()
     */
    @Override
    public String getName() {
        return "TypeResolver(" + (this.getDns() != null ? this.getDns().getName() : "") + ")";
    }

    /*
     * (non-Javadoc)
     * @see javax.jmdns.impl.tasks.Resolver#addAnswers(javax.jmdns.impl.DNSOutgoing)
     */
    @Override
    protected DNSOutgoing addAnswers(DNSOutgoing out) throws IOException {
        DNSOutgoing newOut = out;
        long now = System.currentTimeMillis();
        for (String type : this.getDns().getServiceTypes().keySet()) {
            ServiceTypeEntry typeEntry = this.getDns().getServiceTypes().get(type);
            newOut = this.addAnswer(newOut, new DNSRecord.Pointer("_services._dns-sd._udp.local.", DNSRecordClass.CLASS_IN, DNSRecordClass.NOT_UNIQUE, DNSConstants.DNS_TTL, typeEntry.getType()), now);
        }
        return newOut;
    }

    /*
     * (non-Javadoc)
     * @see javax.jmdns.impl.tasks.Resolver#addQuestions(javax.jmdns.impl.DNSOutgoing)
     */
    @Override
    protected DNSOutgoing addQuestions(DNSOutgoing out) throws IOException {
        return this.addQuestion(out, DNSQuestion.newQuestion("_services._dns-sd._udp.local.", DNSRecordType.TYPE_PTR, DNSRecordClass.CLASS_IN, DNSRecordClass.NOT_UNIQUE));
    }

    /*
     * (non-Javadoc)
     * @see javax.jmdns.impl.tasks.Resolver#description()
     */
    @Override
    protected String description() {
        return "querying type";
    }
}