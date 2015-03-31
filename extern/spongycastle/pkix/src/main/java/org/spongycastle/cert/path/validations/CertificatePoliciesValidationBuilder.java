package org.spongycastle.cert.path.validations;

import org.spongycastle.cert.path.CertPath;

public class CertificatePoliciesValidationBuilder
{
    private boolean isExplicitPolicyRequired;
    private boolean isAnyPolicyInhibited;
    private boolean isPolicyMappingInhibited;

    public void setAnyPolicyInhibited(boolean anyPolicyInhibited)
    {
        isAnyPolicyInhibited = anyPolicyInhibited;
    }

    public void setExplicitPolicyRequired(boolean explicitPolicyRequired)
    {
        isExplicitPolicyRequired = explicitPolicyRequired;
    }

    public void setPolicyMappingInhibited(boolean policyMappingInhibited)
    {
        isPolicyMappingInhibited = policyMappingInhibited;
    }

    public CertificatePoliciesValidation build(int pathLen)
    {
        return new CertificatePoliciesValidation(pathLen, isExplicitPolicyRequired, isAnyPolicyInhibited, isPolicyMappingInhibited);
    }

    public CertificatePoliciesValidation build(CertPath path)
    {
        return build(path.length());
    }
}
