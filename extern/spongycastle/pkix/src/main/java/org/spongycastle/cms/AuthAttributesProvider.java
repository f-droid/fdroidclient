package org.spongycastle.cms;

import org.spongycastle.asn1.ASN1Set;

interface AuthAttributesProvider
{
    ASN1Set getAuthAttributes();
}
