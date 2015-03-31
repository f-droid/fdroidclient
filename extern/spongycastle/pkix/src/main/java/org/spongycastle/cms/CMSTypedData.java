package org.spongycastle.cms;

import org.spongycastle.asn1.ASN1ObjectIdentifier;

public interface CMSTypedData
    extends CMSProcessable
{
    ASN1ObjectIdentifier getContentType();
}
