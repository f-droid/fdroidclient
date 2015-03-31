package org.spongycastle.cms;

import org.spongycastle.asn1.cms.RecipientInfo;
import org.spongycastle.operator.GenericKey;

public interface RecipientInfoGenerator
{
    RecipientInfo generate(GenericKey contentEncryptionKey)
        throws CMSException;
}
