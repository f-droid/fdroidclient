package org.spongycastle.cms;

import org.spongycastle.operator.OperatorCreationException;

public interface SignerInformationVerifierProvider
{
    /**
     * Return a SignerInformationVerifierProvider suitable for the passed in SID.
     *
     * @param sid the SignerId we are trying to match for.
     * @return  a verifier if one is available, null otherwise.
     * @throws OperatorCreationException if creation of the verifier fails when it should suceed.
     */
    public SignerInformationVerifier get(SignerId sid)
          throws OperatorCreationException;
}
