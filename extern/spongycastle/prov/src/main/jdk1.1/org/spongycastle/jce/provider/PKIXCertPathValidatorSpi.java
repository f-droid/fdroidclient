package org.spongycastle.jce.provider;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.PublicKey;
import java.security.cert.CRLException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertPath;
import java.security.cert.CertPathParameters;
import java.security.cert.CertPathValidatorSpi;
import java.security.cert.CertPathValidatorException;
import java.security.cert.CertPathValidatorResult;
import java.security.cert.PolicyQualifierInfo;
import java.security.cert.X509Certificate;
import java.security.cert.X509CRL;
import java.security.cert.X509CRLEntry;
import java.security.cert.X509CRLSelector;
import java.security.cert.X509CertSelector;
import java.security.cert.PKIXParameters;
import java.security.cert.PKIXCertPathChecker;
import java.security.cert.PKIXCertPathValidatorResult;
import java.security.cert.TrustAnchor;
import java.security.cert.PKIXParameters;
import java.security.cert.CertStore;
import java.security.cert.CertStoreException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import org.spongycastle.jce.X509Principal;
import org.spongycastle.jce.PrincipalUtil;
import org.spongycastle.asn1.ASN1InputStream;
import org.spongycastle.asn1.ASN1OctetString;
import org.spongycastle.asn1.ASN1OutputStream;
import org.spongycastle.asn1.ASN1Sequence;
import org.spongycastle.asn1.ASN1TaggedObject;
import org.spongycastle.asn1.BERConstructedOctetString;
import org.spongycastle.asn1.ASN1Encodable;
import org.spongycastle.asn1.DEREnumerated;
import org.spongycastle.asn1.DERIA5String;
import org.spongycastle.asn1.DERInteger;
import org.spongycastle.asn1.ASN1Primitive;
import org.spongycastle.asn1.DERObjectIdentifier;
import org.spongycastle.asn1.x509.AlgorithmIdentifier;
import org.spongycastle.asn1.x509.BasicConstraints;
import org.spongycastle.asn1.x509.GeneralName;
import org.spongycastle.asn1.x509.GeneralSubtree;
import org.spongycastle.asn1.x509.IssuingDistributionPoint;
import org.spongycastle.asn1.x509.NameConstraints;
import org.spongycastle.asn1.x509.PolicyInformation;
import org.spongycastle.asn1.x509.SubjectPublicKeyInfo;
import org.spongycastle.asn1.x509.X509Extensions;

/**
 * CertPathValidatorSpi implemenation for X.509 Certificate validation ala rfc 3280<br />
 **/
public class PKIXCertPathValidatorSpi extends CertPathValidatorSpi
{
    private static final String CERTIFICATE_POLICIES = X509Extensions.CertificatePolicies.getId();
    private static final String POLICY_MAPPINGS = X509Extensions.PolicyMappings.getId();
    private static final String INHIBIT_ANY_POLICY = X509Extensions.InhibitAnyPolicy.getId();
    private static final String ISSUING_DISTRIBUTION_POINT = X509Extensions.IssuingDistributionPoint.getId();
    private static final String DELTA_CRL_INDICATOR = X509Extensions.DeltaCRLIndicator.getId();
    private static final String POLICY_CONSTRAINTS = X509Extensions.PolicyConstraints.getId();
    private static final String BASIC_CONSTRAINTS = X509Extensions.BasicConstraints.getId();
    private static final String SUBJECT_ALTERNATIVE_NAME = X509Extensions.SubjectAlternativeName.getId();
    private static final String NAME_CONSTRAINTS = X509Extensions.NameConstraints.getId();
    private static final String KEY_USAGE = X509Extensions.KeyUsage.getId();

    private static final String CRL_NUMBER = X509Extensions.CRLNumber.getId();

    private static final String ANY_POLICY = "2.5.29.32.0";


    /*
     * key usage bits
     */
    private static final int    KEY_CERT_SIGN = 5;
    private static final int    CRL_SIGN = 6;

    private static final String[] crlReasons = new String[] {
                                        "unspecified",
                                        "keyCompromise",
                                        "cACompromise",
                                        "affiliationChanged",
                                        "superseded",
                                        "cessationOfOperation",
                                        "certificateHold",
                                        "unknown",
                                        "removeFromCRL",
                                        "privilegeWithdrawn",
                                        "aACompromise" };
    
    /**
     * extract the value of the given extension, if it exists.
     */
    private ASN1Primitive getExtensionValue(
        java.security.cert.X509Extension    ext,
        String                              oid)
        throws AnnotatedException
    {
        byte[]  bytes = ext.getExtensionValue(oid);
        if (bytes == null)
        {
            return null;
        }

        return getObject(oid, bytes);
    }

    private ASN1Primitive getObject(
        String oid,
        byte[] ext)
        throws AnnotatedException
    {
        try
        {
            ASN1InputStream aIn = new ASN1InputStream(ext);
            ASN1OctetString octs = (ASN1OctetString)aIn.readObject();

            aIn = new ASN1InputStream(octs.getOctets());
            return aIn.readObject();
        }
        catch (IOException e)
        {
            throw new AnnotatedException("exception processing extension " + oid, e);
        }
    }
    
    private boolean withinDNSubtree(
        ASN1Sequence    dns,
        ASN1Sequence    subtree)
    {
        if (subtree.size() < 1)
        {
            return false;
        }

        if (subtree.size() > dns.size())
        {
            return false;
        }

        for (int j = subtree.size() - 1; j >= 0; j--)
        {
            if (!subtree.getObjectAt(j).equals(dns.getObjectAt(j)))
            {
                return false;
            }
        }

        return true;
    }

    private void checkPermittedDN(
        Set             permitted,
        ASN1Sequence    dns)
    throws CertPathValidatorException
    {
        if (permitted.isEmpty())
        {
            return;
        }
        
        Iterator        it = permitted.iterator();

        while (it.hasNext())
        {
            ASN1Sequence subtree = (ASN1Sequence)it.next();
            
            if (withinDNSubtree(dns, subtree))
            {
                return;
            }
        }

        throw new CertPathValidatorException("Subject distinguished name is not from a permitted subtree");
    }
    
    private void checkExcludedDN(
        Set             excluded,
        ASN1Sequence    dns)
        throws CertPathValidatorException
    {
        if (excluded.isEmpty())
        {
            return;
        }

        Iterator        it = excluded.iterator();

        while (it.hasNext())
        {
            ASN1Sequence subtree = (ASN1Sequence)it.next();
            
            if (withinDNSubtree(dns, subtree))
            {
                throw new CertPathValidatorException("Subject distinguished name is from an excluded subtree");
            }
        }
    }

    private Set intersectDN(
        Set             permitted,
        ASN1Sequence    dn)
    {
        if (permitted.isEmpty())
        {
            permitted.add(dn);

            return permitted;
        }
        else
        {
            Set     intersect = new HashSet();
            
            Iterator _iter = permitted.iterator();
            while (_iter.hasNext())
            {
                ASN1Sequence subtree = (ASN1Sequence)_iter.next();

                if (withinDNSubtree(dn, subtree))
                {
                    intersect.add(dn);
                }
                else if (withinDNSubtree(subtree, dn))
                {
                    intersect.add(subtree);
                }
            }
            
            return intersect;
        }
    }
    
    private Set unionDN(
        Set             excluded,
        ASN1Sequence    dn)
    {
        if (excluded.isEmpty())
        {
            excluded.add(dn);

            return excluded;
        }
        else
        {
            Set         intersect = new HashSet();

            Iterator _iter = excluded.iterator();
            while (_iter.hasNext())
            {
                ASN1Sequence subtree = (ASN1Sequence)_iter.next();

                if (withinDNSubtree(dn, subtree))
                {
                    intersect.add(subtree);
                }
                else if (withinDNSubtree(subtree, dn))
                {
                    intersect.add(dn);
                }
                else
                {
                    intersect.add(subtree);
                    intersect.add(dn);
                }
            }
            
            return intersect;
        }
    }
    
    private Set intersectEmail(
        Set     permitted,
        String  email)
    {
        String _sub = email.substring(email.indexOf('@') + 1);
        
        if (permitted.isEmpty())
        {
            permitted.add(_sub);

            return permitted;
        }
        else
        {
            Set      intersect = new HashSet();

            Iterator _iter = permitted.iterator();
            while (_iter.hasNext())
            {
                String _permitted = (String)_iter.next();

                if (_sub.endsWith(_permitted))
                {
                    intersect.add(_sub);
                }
                else if (_permitted.endsWith(_sub))
                {
                    intersect.add(_permitted);
                }
            }
            
            return intersect;
        }
    }

    private Set unionEmail(
        Set     excluded,
        String  email)
    {
        String _sub = email.substring(email.indexOf('@') + 1);
        
        if (excluded.isEmpty())
        {
            excluded.add(_sub);
            return excluded;
        }
        else
        {
            Set     intersect = new HashSet();

            Iterator _iter = excluded.iterator();
            while (_iter.hasNext())
            {
                String _excluded = (String)_iter.next();

                if (_sub.endsWith(_excluded))
                {
                    intersect.add(_excluded);
                }
                else if (_excluded.endsWith(_sub))
                {
                    intersect.add(_sub);
                }
                else
                {
                    intersect.add(_excluded);
                    intersect.add(_sub);
                }
            }
            
            return intersect;
        }
    }
    
    private Set intersectIP(
        Set     permitted,
        byte[]  ip)
    {
        // TBD
        return permitted;
    }
    
    private Set unionIP(
        Set     excluded,
        byte[]  ip)
    {
        // TBD
        return excluded;
    }

    private void checkPermittedEmail(
        Set     permitted,
        String email) 
        throws CertPathValidatorException
    {
        if (permitted.isEmpty())
        {
            return;
        }
        
        String      sub = email.substring(email.indexOf('@') + 1);
        Iterator    it = permitted.iterator();

        while (it.hasNext())
        {
            String str = (String)it.next();

            if (sub.endsWith(str))
            {
                return;
            }
        }

        throw new CertPathValidatorException("Subject email address is not from a permitted subtree");
    }
    
    private void checkExcludedEmail(
        Set     excluded,
        String  email) 
        throws CertPathValidatorException
    {
        if (excluded.isEmpty())
        {
            return;
        }
        
        String      sub = email.substring(email.indexOf('@') + 1);
        Iterator    it = excluded.iterator();

        while (it.hasNext())
        {
            String str = (String)it.next();
            if (sub.endsWith(str))
            {
                throw new CertPathValidatorException("Subject email address is from an excluded subtree");
            }
        }
    }
    
    private void checkPermittedIP(
        Set     permitted,
        byte[]  ip) 
        throws CertPathValidatorException
    {
        if (permitted.isEmpty())
        {
            return;
        }

        // TODO: ??? Something here
    }
    
    private void checkExcludedIP(
        Set     excluded,
        byte[]  ip) 
        throws CertPathValidatorException
    {
        if (excluded.isEmpty())
        {
            return;
        }
        
        // TODO, check RFC791 and RFC1883 for IP bytes definition.
    }

    private PKIXPolicyNode removePolicyNode(
        PKIXPolicyNode  validPolicyTree,
        List     []        policyNodes,
        PKIXPolicyNode _node)
    {
        PKIXPolicyNode _parent = (PKIXPolicyNode)_node.getParent();
        
        if (validPolicyTree == null)
        {
            return null;
        }

        if (_parent == null)
        {
            for (int j = 0; j < policyNodes.length; j++)
            {
                policyNodes[j] = new ArrayList();
            }

            return null;
        }
        else
        {
            _parent.removeChild(_node);
            removePolicyNodeRecurse(policyNodes, _node);

            return validPolicyTree;
        }
    }
    
    private void removePolicyNodeRecurse(
        List     []        policyNodes,
        PKIXPolicyNode  _node)
    {
        policyNodes[_node.getDepth()].remove(_node);

        if (_node.hasChildren())
        {
            Iterator _iter = _node.getChildren();
            while (_iter.hasNext())
            {
                PKIXPolicyNode _child = (PKIXPolicyNode)_iter.next();
                removePolicyNodeRecurse(policyNodes, _child);
            }
        }
    }

    private boolean isSelfIssued(
        X509Certificate cert)
    {
        return cert.getSubjectDN().equals(cert.getIssuerDN());
    }

    private boolean isAnyPolicy(
        Set policySet)
    {
        return policySet == null || policySet.contains(ANY_POLICY) || policySet.isEmpty();
    }

    private AlgorithmIdentifier getAlgorithmIdentifier(
        PublicKey key)
        throws CertPathValidatorException
    {
        try
        {
            ASN1InputStream      aIn = new ASN1InputStream(
                                    new ByteArrayInputStream(key.getEncoded()));

            SubjectPublicKeyInfo info = SubjectPublicKeyInfo.getInstance(aIn.readObject());

            return info.getAlgorithmId();
        }
        catch (IOException e)
        {
            throw new CertPathValidatorException("exception processing public key");
        }
    }

    private Set getQualifierSet(ASN1Sequence qualifiers) 
        throws CertPathValidatorException
    {
        Set             pq   = new HashSet();
        
        if (qualifiers == null)
        {
            return pq;
        }
        
        ByteArrayOutputStream   bOut = new ByteArrayOutputStream();
        ASN1OutputStream        aOut = new ASN1OutputStream(bOut);

        Enumeration e = qualifiers.getObjects();

        while (e.hasMoreElements())
        {
            try
            {
                aOut.writeObject((ASN1Encodable)e.nextElement());

                pq.add(new PolicyQualifierInfo(bOut.toByteArray()));
            }
            catch (IOException ex)
            {
                throw new CertPathValidatorException("exception building qualifier set: " + ex);
            }

            bOut.reset();
        }
        
        return pq;
    }

    private boolean processCertD1i(
        int                 index,
        List     []            policyNodes,
        DERObjectIdentifier pOid,
        Set                 pq)
    {
        List       policyNodeVec = policyNodes[index - 1];

        for (int j = 0; j < policyNodeVec.size(); j++)
        {
            PKIXPolicyNode node = (PKIXPolicyNode)policyNodeVec.get(j);
            Set            expectedPolicies = node.getExpectedPolicies();
            
            if (expectedPolicies.contains(pOid.getId()))
            {
                Set childExpectedPolicies = new HashSet();
                childExpectedPolicies.add(pOid.getId());
                
                PKIXPolicyNode child = new PKIXPolicyNode(new ArrayList(),
                                                           index,
                                                           childExpectedPolicies,
                                                           node,
                                                           pq,
                                                           pOid.getId(),
                                                           false);
                node.addChild(child);
                policyNodes[index].add(child);
                
                return true;
            }
        }
        
        return false;
    }

    private void processCertD1ii(
        int                 index,
        List     []            policyNodes,
        DERObjectIdentifier _poid,
        Set _pq)
    {
        List       policyNodeVec = policyNodes[index - 1];

        for (int j = 0; j < policyNodeVec.size(); j++)
        {
            PKIXPolicyNode _node = (PKIXPolicyNode)policyNodeVec.get(j);
            Set            _expectedPolicies = _node.getExpectedPolicies();
            
            if (ANY_POLICY.equals(_node.getValidPolicy()))
            {
                Set _childExpectedPolicies = new HashSet();
                _childExpectedPolicies.add(_poid.getId());
                
                PKIXPolicyNode _child = new PKIXPolicyNode(new ArrayList(),
                                                           index,
                                                           _childExpectedPolicies,
                                                           _node,
                                                           _pq,
                                                           _poid.getId(),
                                                           false);
                _node.addChild(_child);
                policyNodes[index].add(_child);
                return;
            }
        }
    }

    public CertPathValidatorResult engineValidate(
        CertPath certPath,
        CertPathParameters params)
        throws CertPathValidatorException, InvalidAlgorithmParameterException
    {
        if (!(params instanceof PKIXParameters))
        {
            throw new InvalidAlgorithmParameterException("params must be a PKIXParameters instance");
        }

        PKIXParameters paramsPKIX = (PKIXParameters)params;
        if (paramsPKIX.getTrustAnchors() == null)
        {
            throw new InvalidAlgorithmParameterException("trustAnchors is null, this is not allowed for path validation");
        }

        //
        // 6.1.1 - inputs
        //

        //
        // (a)
        //
        List    certs = certPath.getCertificates();
        int     n = certs.size();
        
        if (certs.isEmpty())
        {
            throw new CertPathValidatorException("CertPath is empty", null, certPath, 0);
        }

        //
        // (b)
        //
        Date validDate = getValidDate(paramsPKIX);

        //
        // (c)
        //
        Set userInitialPolicySet = paramsPKIX.getInitialPolicies();

        //
        // (d)
        // 
        TrustAnchor trust = findTrustAnchor((X509Certificate)certs.get(certs.size() - 1), certPath, certs.size() - 1, paramsPKIX.getTrustAnchors());

        if (trust == null)
        {
            throw new CertPathValidatorException("TrustAnchor for CertPath not found.", null, certPath, -1);
        }
        
        //
        // (e), (f), (g) are part of the paramsPKIX object.
        //

        Iterator certIter;
        int index = 0;
        int i;
        //Certificate for each interation of the validation loop
        //Signature information for each iteration of the validation loop
        Set subTreeContraints = new HashSet();
        Set subTreeExcludes = new HashSet();

        //
        // 6.1.2 - setup
        //

        //
        // (a)
        //
        List     []  policyNodes = new ArrayList[n + 1];
        for (int j = 0; j < policyNodes.length; j++)
        {
            policyNodes[j] = new ArrayList();
        }

        Set policySet = new HashSet();

        policySet.add(ANY_POLICY);

        PKIXPolicyNode  validPolicyTree = new PKIXPolicyNode(new ArrayList(), 0, policySet, null, new HashSet(), ANY_POLICY, false);

        policyNodes[0].add(validPolicyTree);

        //
        // (b)
        //
        Set     permittedSubtreesDN = new HashSet();
        Set     permittedSubtreesEmail = new HashSet();
        Set     permittedSubtreesIP = new HashSet();
    
        //
        // (c)
        //
        Set     excludedSubtreesDN = new HashSet();
        Set     excludedSubtreesEmail = new HashSet();
        Set     excludedSubtreesIP = new HashSet();
    
        //
        // (d)
        //
        int explicitPolicy;
        Set acceptablePolicies = null;

        if (paramsPKIX.isExplicitPolicyRequired())
        {
            explicitPolicy = 0;
        }
        else
        {
            explicitPolicy = n + 1;
        }

        //
        // (e)
        //
        int inhibitAnyPolicy;

        if (paramsPKIX.isAnyPolicyInhibited())
        {
            inhibitAnyPolicy = 0;
        }
        else
        {
            inhibitAnyPolicy = n + 1;
        }
    
        //
        // (f)
        //
        int policyMapping;

        if (paramsPKIX.isPolicyMappingInhibited())
        {
            policyMapping = 0;
        }
        else
        {
            policyMapping = n + 1;
        }
    
        //
        // (g), (h), (i), (j)
        //
        PublicKey workingPublicKey;
        X509Principal workingIssuerName;

        X509Certificate sign = trust.getTrustedCert();
        try
        {
            if (sign != null)
            {
                workingIssuerName = getSubjectPrincipal(sign);
                workingPublicKey = sign.getPublicKey();
            }
            else
            {
                workingIssuerName = new X509Principal(trust.getCAName());
                workingPublicKey = trust.getCAPublicKey();
            }
        }
        catch (IllegalArgumentException ex)
        {
            throw new CertPathValidatorException("TrustAnchor subjectDN: " + ex.toString());
        }
        catch (AnnotatedException ex)
        {
            throw new CertPathValidatorException(ex.getMessage(), ex.getUnderlyingException(), certPath, index);
        }

        AlgorithmIdentifier workingAlgId = getAlgorithmIdentifier(workingPublicKey);
        DERObjectIdentifier workingPublicKeyAlgorithm = workingAlgId.getObjectId();
        ASN1Encodable        workingPublicKeyParameters = workingAlgId.getParameters();
    
        //
        // (k)
        //
        int maxPathLength = n;

        //
        // 6.1.3
        //
        Iterator tmpIter;
        int tmpInt;

        if (paramsPKIX.getTargetCertConstraints() != null
            && !paramsPKIX.getTargetCertConstraints().match((X509Certificate)certs.get(0)))
        {
            throw new CertPathValidatorException("target certificate in certpath does not match targetcertconstraints", null, certPath, 0);
        }


        // 
        // initialise CertPathChecker's
        //
        List  pathCheckers = paramsPKIX.getCertPathCheckers();
        certIter = pathCheckers.iterator();
        while (certIter.hasNext())
        {
            ((PKIXCertPathChecker)certIter.next()).init(false);
        }

        X509Certificate cert = null;

        for (index = certs.size() - 1; index >= 0 ; index--)
        {
            try
            {
                //
                // i as defined in the algorithm description
                //
                i = n - index;
    
                //
                // set certificate to be checked in this round
                // sign and workingPublicKey and workingIssuerName are set
                // at the end of the for loop and initialied the
                // first time from the TrustAnchor
                //
                cert = (X509Certificate)certs.get(index);
    
                //
                // 6.1.3
                //
    
                //
                // (a) verify
                //
                try
                {
                    // (a) (1)
                    //
                    cert.verify(workingPublicKey, "SC");
                }
                catch (Exception e)
                {
                    throw new CertPathValidatorException("Could not validate certificate signature.", e, certPath, index);
                }
    
                try
                {
                    // (a) (2)
                    //
                    cert.checkValidity(validDate);
                }
                catch (CertificateExpiredException e)
                {
                    throw new CertPathValidatorException("Could not validate certificate: " + e.getMessage(), e, certPath, index);
                }
                catch (CertificateNotYetValidException e)
                {
                    throw new CertPathValidatorException("Could not validate certificate: " + e.getMessage(), e, certPath, index);
                }
    
                //
                // (a) (3)
                //
                if (paramsPKIX.isRevocationEnabled())
                {
                    checkCRLs(paramsPKIX, cert, validDate, sign, workingPublicKey);
                }
    
                //
                // (a) (4) name chaining
                //
                if (!getEncodedIssuerPrincipal(cert).equals(workingIssuerName))
                {
                    throw new CertPathValidatorException(
                                "IssuerName(" + getEncodedIssuerPrincipal(cert) +
                                ") does not match SubjectName(" + workingIssuerName +
                                ") of signing certificate", null, certPath, index);
                }
    
                //
                // (b), (c) permitted and excluded subtree checking.
                //
                if (!(isSelfIssued(cert) && (i < n)))
                {
                    X509Principal principal = getSubjectPrincipal(cert);
                    ASN1InputStream aIn = new ASN1InputStream(new ByteArrayInputStream(principal.getEncoded()));
                    ASN1Sequence    dns;
    
                    try
                    {
                        dns = (ASN1Sequence)aIn.readObject();
                    }
                    catch (IOException e)
                    {
                        throw new CertPathValidatorException("exception extracting subject name when checking subtrees");
                    }
    
                    checkPermittedDN(permittedSubtreesDN, dns);
    
                    checkExcludedDN(excludedSubtreesDN, dns);
            
                    ASN1Sequence   altName = (ASN1Sequence)getExtensionValue(cert, SUBJECT_ALTERNATIVE_NAME);
                    if (altName != null)
                    {
                        for (int j = 0; j < altName.size(); j++)
                        {
                            ASN1TaggedObject o = (ASN1TaggedObject)altName.getObjectAt(j);
    
                            switch(o.getTagNo())
                            {
                            case 1:
                                String email = DERIA5String.getInstance(o, true).getString();
    
                                checkPermittedEmail(permittedSubtreesEmail, email);
                                checkExcludedEmail(excludedSubtreesEmail, email);
                                break;
                            case 4:
                                ASN1Sequence altDN = ASN1Sequence.getInstance(o, true);
    
                                checkPermittedDN(permittedSubtreesDN, altDN);
                                checkExcludedDN(excludedSubtreesDN, altDN);
                                break;
                            case 7:
                                byte[] ip = ASN1OctetString.getInstance(o, true).getOctets();
    
                                checkPermittedIP(permittedSubtreesIP, ip);
                                checkExcludedIP(excludedSubtreesIP, ip);
                            }
                        }
                    }
                }
    
                //
                // (d) policy Information checking against initial policy and
                // policy mapping
                //
                ASN1Sequence   certPolicies = (ASN1Sequence)getExtensionValue(cert, CERTIFICATE_POLICIES);
                if (certPolicies != null && validPolicyTree != null)
                {
                    //
                    // (d) (1)
                    //
                    Enumeration e = certPolicies.getObjects();
                    Set         pols = new HashSet();
                        
                    while (e.hasMoreElements())
                    {
                        PolicyInformation   pInfo = PolicyInformation.getInstance(e.nextElement());
                        DERObjectIdentifier pOid = pInfo.getPolicyIdentifier();
                        
                        pols.add(pOid.getId());
    
                        if (!ANY_POLICY.equals(pOid.getId()))
                        {
                            Set pq = getQualifierSet(pInfo.getPolicyQualifiers());
                            
                            boolean match = processCertD1i(i, policyNodes, pOid, pq);
                            
                            if (!match)
                            {
                                processCertD1ii(i, policyNodes, pOid, pq);
                            }
                        }
                    }
    
                    if (acceptablePolicies == null || acceptablePolicies.contains(ANY_POLICY))
                    {
                        acceptablePolicies = pols;
                    }
                    else
                    {
                        Iterator    it = acceptablePolicies.iterator();
                        Set         t1 = new HashSet();
    
                        while (it.hasNext())
                        {
                            Object  o = it.next();
    
                            if (pols.contains(o))
                            {
                                t1.add(o);
                            }
                        }
    
                        acceptablePolicies = t1;
                    }
    
                    //
                    // (d) (2)
                    //
                    if ((inhibitAnyPolicy > 0) || ((i < n) && isSelfIssued(cert)))
                    {
                        e = certPolicies.getObjects();
    
                        while (e.hasMoreElements())
                        {
                            PolicyInformation   pInfo = PolicyInformation.getInstance(e.nextElement());
    
                            if (ANY_POLICY.equals(pInfo.getPolicyIdentifier().getId()))
                            {
                                Set    _apq   = getQualifierSet(pInfo.getPolicyQualifiers());
                                List      _nodes = policyNodes[i - 1];
                                
                                for (int k = 0; k < _nodes.size(); k++)
                                {
                                    PKIXPolicyNode _node = (PKIXPolicyNode)_nodes.get(k);
                                    
                                    Iterator _policySetIter = _node.getExpectedPolicies().iterator();
                                    while (_policySetIter.hasNext())
                                    {
                                        Object _tmp = _policySetIter.next();
                                        
                                        String _policy;
                                        if (_tmp instanceof String)
                                        {
                                            _policy = (String)_tmp;
                                        }
                                        else if (_tmp instanceof DERObjectIdentifier)
                                        {
                                            _policy = ((DERObjectIdentifier)_tmp).getId();
                                        }
                                        else
                                        {
                                            continue;
                                        }
                                        
                                        boolean  _found        = false;
                                        Iterator _childrenIter = _node.getChildren();
    
                                        while (_childrenIter.hasNext())
                                        {
                                            PKIXPolicyNode _child = (PKIXPolicyNode)_childrenIter.next();
    
                                            if (_policy.equals(_child.getValidPolicy()))
                                            {
                                                _found = true;
                                            }
                                        }
    
                                        if (!_found)
                                        {
                                            Set _newChildExpectedPolicies = new HashSet();
                                            _newChildExpectedPolicies.add(_policy);
    
                                            PKIXPolicyNode _newChild = new PKIXPolicyNode(new ArrayList(),
                                                                                          i,
                                                                                          _newChildExpectedPolicies,
                                                                                          _node,
                                                                                          _apq,
                                                                                          _policy,
                                                                                          false);
                                            _node.addChild(_newChild);
                                            policyNodes[i].add(_newChild);
                                        }
                                    }
                                }
                                break;
                            }
                        }
                    }
                
                    //
                    // (d) (3)
                    //
                    for (int j = (i - 1); j >= 0; j--)
                    {
                        List      nodes = policyNodes[j];
                        
                        for (int k = 0; k < nodes.size(); k++)
                        {
                            PKIXPolicyNode node = (PKIXPolicyNode)nodes.get(k);
                            if (!node.hasChildren())
                            {
                                validPolicyTree = removePolicyNode(validPolicyTree, policyNodes, node);
                                if (validPolicyTree == null)
                                {
                                    break;
                                }
                            }
                        }
                    }
                
                    //
                    // d (4)
                    //
                    Set criticalExtensionOids = cert.getCriticalExtensionOIDs();
                    
                    if (criticalExtensionOids != null)
                    {
                        boolean critical = criticalExtensionOids.contains(CERTIFICATE_POLICIES);
                    
                        List      nodes = policyNodes[i];
                        for (int j = 0; j < nodes.size(); j++)
                        {
                            PKIXPolicyNode node = (PKIXPolicyNode)nodes.get(j);
                            node.setCritical(critical);
                        }
                    }
                }
    
                // 
                // (e)
                //
                if (certPolicies == null)
                {
                    validPolicyTree = null;
                }
    
                //
                // (f)
                //
                if (explicitPolicy <= 0 && validPolicyTree == null)
                {
                    throw new CertPathValidatorException("No valid policy tree found when one expected.");
                }
    
                //
                // 6.1.4
                //
    
                if (i != n)
                {
                    if (cert != null && cert.getVersion() == 1)
                    {
                        throw new CertPathValidatorException(
                                "Version 1 certs can't be used as CA ones");
                    }
    
                    //
                    //
                    // (a) check the policy mappings
                    //
                    ASN1Primitive   pm = getExtensionValue(cert, POLICY_MAPPINGS);
                    if (pm != null)
                    {
                        ASN1Sequence mappings = (ASN1Sequence)pm;
                    
                        for (int j = 0; j < mappings.size(); j++)
                        {
                            ASN1Sequence    mapping = (ASN1Sequence)mappings.getObjectAt(j);
    
                            DERObjectIdentifier issuerDomainPolicy = (DERObjectIdentifier)mapping.getObjectAt(0);
                            DERObjectIdentifier subjectDomainPolicy = (DERObjectIdentifier)mapping.getObjectAt(1);
    
                            if (ANY_POLICY.equals(issuerDomainPolicy.getId()))
                            {
                            
                                throw new CertPathValidatorException("IssuerDomainPolicy is anyPolicy");
                            }
                        
                            if (ANY_POLICY.equals(subjectDomainPolicy.getId()))
                            {
                            
                                throw new CertPathValidatorException("SubjectDomainPolicy is anyPolicy");
                            }
                        }
                    }
                  
                    // (b)
                    //
                    if (pm != null)
                    {
                        ASN1Sequence mappings = (ASN1Sequence)pm;
                        Map m_idp = new HashMap();
                        Set s_idp = new HashSet();
                        
                        for (int j = 0; j < mappings.size(); j++)
                        {
                            ASN1Sequence mapping = (ASN1Sequence)mappings.getObjectAt(j);
                            String id_p = ((DERObjectIdentifier)mapping.getObjectAt(0)).getId();
                            String sd_p = ((DERObjectIdentifier)mapping.getObjectAt(1)).getId();
                            Set tmp;
                            
                            if (!m_idp.containsKey(id_p))
                            {
                                tmp = new HashSet();
                                tmp.add(sd_p);
                                m_idp.put(id_p, tmp);
                                s_idp.add(id_p);
                            }
                            else
                            {
                                tmp = (Set)m_idp.get(id_p);
                                tmp.add(sd_p);
                            }
                        }
    
                        Iterator it_idp = s_idp.iterator();
                        while (it_idp.hasNext())
                        {
                            String id_p = (String)it_idp.next();
    
                            //
                            // (1)
                            //
                            if (policyMapping > 0)
                            {
                                boolean idp_found = false;
                                Iterator nodes_i = policyNodes[i].iterator();
                                while (nodes_i.hasNext())
                                {
                                    PKIXPolicyNode node = (PKIXPolicyNode)nodes_i.next();
                                    if (node.getValidPolicy().equals(id_p))
                                    {
                                        idp_found = true;
                                        node.expectedPolicies = (Set)m_idp.get(id_p);
                                        break;
                                    }
                                }
    
                                if (!idp_found)
                                {
                                    nodes_i = policyNodes[i].iterator();
                                    while (nodes_i.hasNext())
                                    {
                                        PKIXPolicyNode node = (PKIXPolicyNode)nodes_i.next();
                                        if (ANY_POLICY.equals(node.getValidPolicy()))
                                        {
                                            Set pq = null;
                                            ASN1Sequence policies = (ASN1Sequence)getExtensionValue(
                                                    cert, CERTIFICATE_POLICIES);
                                            Enumeration e = policies.getObjects();
                                            while (e.hasMoreElements())
                                            {
                                                PolicyInformation pinfo = PolicyInformation.getInstance(e.nextElement());
                                                if (ANY_POLICY.equals(pinfo.getPolicyIdentifier().getId()))
                                                {
                                                    pq = getQualifierSet(pinfo.getPolicyQualifiers());
                                                    break;
                                                }
                                            }
                                            boolean ci = false;
                                            if (cert.getCriticalExtensionOIDs() != null)
                                            {
                                                ci = cert.getCriticalExtensionOIDs().contains(CERTIFICATE_POLICIES);
                                            }
    
                                            PKIXPolicyNode p_node = (PKIXPolicyNode)node.getParent();
                                            if (ANY_POLICY.equals(p_node.getValidPolicy()))
                                            {
                                                PKIXPolicyNode c_node = new PKIXPolicyNode(
                                                        new ArrayList(), i,
                                                        (Set)m_idp.get(id_p),
                                                        p_node, pq, id_p, ci);
                                                p_node.addChild(c_node);
                                                policyNodes[i].add(c_node);
                                            }
                                            break;
                                        }
                                    }
                                }
    
                            //
                            // (2)
                            //
                            }
                            else if (policyMapping <= 0)
                            {
                                Iterator nodes_i = policyNodes[i].iterator();
                                while (nodes_i.hasNext())
                                {
                                    PKIXPolicyNode node = (PKIXPolicyNode)nodes_i.next();
                                    if (node.getValidPolicy().equals(id_p))
                                    {
                                        PKIXPolicyNode p_node = (PKIXPolicyNode)node.getParent();
                                        p_node.removeChild(node);
                                        nodes_i.remove();
                                        for (int k = (i - 1); k >= 0; k--)
                                        {
                                            List nodes = policyNodes[k];
                                            for (int l = 0; l < nodes.size(); l++)
                                            {
                                                PKIXPolicyNode node2 = (PKIXPolicyNode)nodes.get(l);
                                                if (!node2.hasChildren())
                                                {
                                                    validPolicyTree = removePolicyNode(validPolicyTree, policyNodes, node2);
                                                    if (validPolicyTree == null)
                                                    {
                                                        break;
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    //
                    // (g) handle the name constraints extension
                    //
                    ASN1Sequence ncSeq = (ASN1Sequence)getExtensionValue(cert, NAME_CONSTRAINTS);
                    if (ncSeq != null)
                    {
                        NameConstraints nc = NameConstraints.getInstance(ncSeq);
    
                        //
                        // (g) (1) permitted subtrees
                        //
                        GeneralSubtree[] permitted = nc.getPermittedSubtrees();
                        if (permitted != null)
                        {
                            for (int indx = 0; indx != permitted.length; indx++)
                            {
                                GeneralSubtree  subtree = permitted[indx];
                                GeneralName     base = subtree.getBase();
    
                                switch(base.getTagNo())
                                {
                                    case 1:
                                        permittedSubtreesEmail = intersectEmail(permittedSubtreesEmail, DERIA5String.getInstance(base.getName()).getString());
                                        break;
                                    case 4:
                                        permittedSubtreesDN = intersectDN(permittedSubtreesDN, (ASN1Sequence)base.getName());
                                        break;
                                    case 7:
                                        permittedSubtreesIP = intersectIP(permittedSubtreesIP, BERConstructedOctetString.fromSequence((ASN1Sequence)base.getName()).getOctets());
                                        break;
                                }
                            }
                        }
                    
                        //
                        // (g) (2) excluded subtrees
                        //
                        GeneralSubtree[] excluded = nc.getExcludedSubtrees();
                        if (excluded != null)
                        {
                            for (int indx = 0; indx != excluded.length; indx++)
                            {
                                GeneralSubtree  subtree = excluded[indx];
                                GeneralName     base = subtree.getBase();
    
                                switch(base.getTagNo())
                                {
                                case 1:
                                    excludedSubtreesEmail = unionEmail(excludedSubtreesEmail, DERIA5String.getInstance(base.getName()).getString());
                                    break;
                                case 4:
                                    excludedSubtreesDN = unionDN(excludedSubtreesDN, (ASN1Sequence)base.getName());
                                    break;
                                case 7:
                                    excludedSubtreesIP = unionIP(excludedSubtreesIP, BERConstructedOctetString.fromSequence((ASN1Sequence)base.getName()).getOctets());
                                    break;
                                }
                            }
                        }
                    }
    
                    //
                    // (h)
                    //
                    if (!isSelfIssued(cert))
                    {
                        //
                        // (1)
                        //
                        if (explicitPolicy != 0)
                        {
                            explicitPolicy--;
                        }
                    
                        //
                        // (2)
                        //
                        if (policyMapping != 0)
                        {
                            policyMapping--;
                        }
                    
                        //
                        // (3)
                        //
                        if (inhibitAnyPolicy != 0)
                        {
                            inhibitAnyPolicy--;
                        }
                    }
            
                    //
                    // (i)
                    //
                    ASN1Sequence pc = (ASN1Sequence)getExtensionValue(cert, POLICY_CONSTRAINTS);
                
                    if (pc != null)
                    {
                        Enumeration policyConstraints = pc.getObjects();
    
                        while (policyConstraints.hasMoreElements())
                        {
                            ASN1TaggedObject    constraint = (ASN1TaggedObject)policyConstraints.nextElement();
                            switch (constraint.getTagNo())
                            {
                            case 0:
                                tmpInt = DERInteger.getInstance(constraint).getValue().intValue();
                                if (tmpInt < explicitPolicy)
                                {
                                    explicitPolicy = tmpInt;
                                }
                                break;
                            case 1:
                                tmpInt = DERInteger.getInstance(constraint).getValue().intValue();
                                if (tmpInt < policyMapping)
                                {
                                    policyMapping = tmpInt;
                                }
                            break;
                            }
                        }
                    }
            
                    //
                    // (j)
                    //
                    DERInteger iap = (DERInteger)getExtensionValue(cert, INHIBIT_ANY_POLICY);
                
                    if (iap != null)
                    {
                        int _inhibitAnyPolicy = iap.getValue().intValue();
                    
                        if (_inhibitAnyPolicy < inhibitAnyPolicy)
                        {
                            inhibitAnyPolicy = _inhibitAnyPolicy;
                        }
                    }
            
                    //
                    // (k)
                    //
                    BasicConstraints    bc = BasicConstraints.getInstance(
                                                getExtensionValue(cert, BASIC_CONSTRAINTS));
                    if (bc != null)
                    {
                        if (!(bc.isCA()))
                        {
                            throw new CertPathValidatorException("Not a CA certificate");
                        }
                    }
                    else
                    {
                        throw new CertPathValidatorException("Intermediate certificate lacks BasicConstraints");
                    }
                
                    //
                    // (l)
                    //
                    if (!isSelfIssued(cert))
                    {
                        if (maxPathLength <= 0)
                        {
                            throw new CertPathValidatorException("Max path length not greater than zero");
                        }
                    
                        maxPathLength--;
                    }
            
                    //
                    // (m)
                    //
                    if (bc != null)
                    {
                        BigInteger          _pathLengthConstraint = bc.getPathLenConstraint();
                
                        if (_pathLengthConstraint != null)
                        {
                            int _plc = _pathLengthConstraint.intValue();
    
                            if (_plc < maxPathLength)
                            {
                                maxPathLength = _plc;
                            }
                        }
                    }
            
                    //
                    // (n)
                    //
                    boolean[] _usage = cert.getKeyUsage();
                
                    if ((_usage != null) && !_usage[5])
                    {
                        throw new CertPathValidatorException(
                                    "Issuer certificate keyusage extension is critical an does not permit key signing.\n",
                                    null, certPath, index);
                    }
    
                    //
                    // (o)
                    //
                    Set criticalExtensions = new HashSet(cert.getCriticalExtensionOIDs());
                    // these extensions are handle by the algorithem
                    criticalExtensions.remove(KEY_USAGE);
                    criticalExtensions.remove(CERTIFICATE_POLICIES);
                    criticalExtensions.remove(POLICY_MAPPINGS);
                    criticalExtensions.remove(INHIBIT_ANY_POLICY);
                    criticalExtensions.remove(ISSUING_DISTRIBUTION_POINT);
                    criticalExtensions.remove(DELTA_CRL_INDICATOR);
                    criticalExtensions.remove(POLICY_CONSTRAINTS);
                    criticalExtensions.remove(BASIC_CONSTRAINTS);
                    criticalExtensions.remove(SUBJECT_ALTERNATIVE_NAME);
                    criticalExtensions.remove(NAME_CONSTRAINTS);
    
                    tmpIter = pathCheckers.iterator();
                    while (tmpIter.hasNext())
                    {
                        try
                        {
                            ((PKIXCertPathChecker)tmpIter.next()).check(cert, criticalExtensions);
                        }
                        catch (CertPathValidatorException e)
                        {
                            throw new CertPathValidatorException(e.getMessage(), e.getCause(), certPath, index);
                        }
                    }
                    if (!criticalExtensions.isEmpty())
                    {
                        throw new CertPathValidatorException(
                            "Certificate has unsupported critical extension", null, certPath, index);
                    }
                }
    
                    // set signing certificate for next round
                sign = cert;
                workingPublicKey = sign.getPublicKey();
                try
                {
                    workingIssuerName = getSubjectPrincipal(sign);
                }
                catch (IllegalArgumentException ex)
                {
                    throw new CertPathValidatorException(sign.getSubjectDN().getName() + " :" + ex.toString());
                }
                workingAlgId = getAlgorithmIdentifier(workingPublicKey);
                workingPublicKeyAlgorithm = workingAlgId.getObjectId();
                workingPublicKeyParameters = workingAlgId.getParameters();
            }
            catch (AnnotatedException e)
            {
                throw new CertPathValidatorException(e.getMessage(), e.getUnderlyingException(), certPath, index);
            }
        }

        //
        // 6.1.5 Wrap-up procedure
        //

        //
        // (a)
        //
        if (!isSelfIssued(cert) && (explicitPolicy != 0))
        {
            explicitPolicy--;
        }
    
        //
        // (b)
        //
        try
        {
            ASN1Sequence pc = (ASN1Sequence)getExtensionValue(cert, POLICY_CONSTRAINTS);
            if (pc != null)
            {
                Enumeration policyConstraints = pc.getObjects();
    
                while (policyConstraints.hasMoreElements())
                {
                    ASN1TaggedObject    constraint = (ASN1TaggedObject)policyConstraints.nextElement();
                    switch (constraint.getTagNo())
                    {
                    case 0:
                        tmpInt = DERInteger.getInstance(constraint).getValue().intValue();
                        if (tmpInt == 0)
                        {
                            explicitPolicy = 0;
                        }
                        break;
                    }
                }
            }
        }
        catch (AnnotatedException e)
        {
            throw new CertPathValidatorException(e.getMessage(), e.getUnderlyingException(), certPath, index);
        }
    
        //
        // (c) (d) and (e) are already done
        //
    
        //
        // (f) 
        //
        Set criticalExtensions = cert.getCriticalExtensionOIDs();
        
        if (criticalExtensions != null)
        {
            criticalExtensions = new HashSet(criticalExtensions);
            // these extensions are handle by the algorithm
            criticalExtensions.remove(KEY_USAGE);
            criticalExtensions.remove(CERTIFICATE_POLICIES);
            criticalExtensions.remove(POLICY_MAPPINGS);
            criticalExtensions.remove(INHIBIT_ANY_POLICY);
            criticalExtensions.remove(ISSUING_DISTRIBUTION_POINT);
            criticalExtensions.remove(DELTA_CRL_INDICATOR);
            criticalExtensions.remove(POLICY_CONSTRAINTS);
            criticalExtensions.remove(BASIC_CONSTRAINTS);
            criticalExtensions.remove(SUBJECT_ALTERNATIVE_NAME);
            criticalExtensions.remove(NAME_CONSTRAINTS);
        }
        else
        {
            criticalExtensions = new HashSet();
        }
        
        tmpIter = pathCheckers.iterator();
        while (tmpIter.hasNext())
        {
            try
            {
                ((PKIXCertPathChecker)tmpIter.next()).check(cert, criticalExtensions);
            }
            catch (CertPathValidatorException e)
            {
                throw new CertPathValidatorException(e.getMessage(), e.getCause(), certPath, index);
            }
        }
        
        if (!criticalExtensions.isEmpty())
        {
            throw new CertPathValidatorException(
                "Certificate has unsupported critical extension", null, certPath, index);
        }

        //
        // (g)
        //
        PKIXPolicyNode intersection;
        

        //
        // (g) (i)
        //
        if (validPolicyTree == null)
        { 
            if (paramsPKIX.isExplicitPolicyRequired())
            {
                throw new CertPathValidatorException("Explicit policy requested but none available.");
            }
            intersection = null;
        }
        else if (isAnyPolicy(userInitialPolicySet)) // (g) (ii)
        {
            if (paramsPKIX.isExplicitPolicyRequired())
            {
                if (acceptablePolicies.isEmpty())
                {
                    throw new CertPathValidatorException("Explicit policy requested but none available.");
                }
                else
                {
                    Set _validPolicyNodeSet = new HashSet();
                    
                    for (int j = 0; j < policyNodes.length; j++)
                    {
                        List      _nodeDepth = policyNodes[j];
                        
                        for (int k = 0; k < _nodeDepth.size(); k++)
                        {
                            PKIXPolicyNode _node = (PKIXPolicyNode)_nodeDepth.get(k);
                            
                            if (ANY_POLICY.equals(_node.getValidPolicy()))
                            {
                                Iterator _iter = _node.getChildren();
                                while (_iter.hasNext())
                                {
                                    _validPolicyNodeSet.add(_iter.next());
                                }
                            }
                        }
                    }
                    
                    Iterator _vpnsIter = _validPolicyNodeSet.iterator();
                    while (_vpnsIter.hasNext())
                    {
                        PKIXPolicyNode _node = (PKIXPolicyNode)_vpnsIter.next();
                        String _validPolicy = _node.getValidPolicy();
                        
                        if (!acceptablePolicies.contains(_validPolicy))
                        {
                            //validPolicyTree = removePolicyNode(validPolicyTree, policyNodes, _node);
                        }
                    }
                    if (validPolicyTree != null)
                    {
                        for (int j = (n - 1); j >= 0; j--)
                        {
                            List      nodes = policyNodes[j];
                            
                            for (int k = 0; k < nodes.size(); k++)
                            {
                                PKIXPolicyNode node = (PKIXPolicyNode)nodes.get(k);
                                if (!node.hasChildren())
                                {
                                    validPolicyTree = removePolicyNode(validPolicyTree, policyNodes, node);
                                }
                            }
                        }
                    }
                }
            }

            intersection = validPolicyTree;
        }
        else
        {
            //
            // (g) (iii)
            //
            // This implementation is not exactly same as the one described in RFC3280.
            // However, as far as the validation result is concerned, both produce 
            // adequate result. The only difference is whether AnyPolicy is remain 
            // in the policy tree or not. 
            //
            // (g) (iii) 1
            //
            Set _validPolicyNodeSet = new HashSet();
            
            for (int j = 0; j < policyNodes.length; j++)
            {
                List      _nodeDepth = policyNodes[j];
                
                for (int k = 0; k < _nodeDepth.size(); k++)
                {
                    PKIXPolicyNode _node = (PKIXPolicyNode)_nodeDepth.get(k);
                    
                    if (ANY_POLICY.equals(_node.getValidPolicy()))
                    {
                        Iterator _iter = _node.getChildren();
                        while (_iter.hasNext())
                        {
                            PKIXPolicyNode _c_node = (PKIXPolicyNode)_iter.next();
                            if (!ANY_POLICY.equals(_c_node.getValidPolicy()))
                            {
                                _validPolicyNodeSet.add(_c_node);
                            }
                        }
                    }
                }
            }
            
            //
            // (g) (iii) 2
            //
            Iterator _vpnsIter = _validPolicyNodeSet.iterator();
            while (_vpnsIter.hasNext())
            {
                PKIXPolicyNode _node = (PKIXPolicyNode)_vpnsIter.next();
                String _validPolicy = _node.getValidPolicy();

                if (!userInitialPolicySet.contains(_validPolicy))
                {
                    validPolicyTree = removePolicyNode(validPolicyTree, policyNodes, _node);
                }
            }
            
            //
            // (g) (iii) 4
            //
            if (validPolicyTree != null)
            {
                for (int j = (n - 1); j >= 0; j--)
                {
                    List      nodes = policyNodes[j];
                    
                    for (int k = 0; k < nodes.size(); k++)
                    {
                        PKIXPolicyNode node = (PKIXPolicyNode)nodes.get(k);
                        if (!node.hasChildren())
                        {
                            validPolicyTree = removePolicyNode(validPolicyTree, policyNodes, node);
                        }
                    }
                }
            }
            
            intersection = validPolicyTree;
        }
 
        if ((explicitPolicy > 0) || (intersection != null))
        {
            return new PKIXCertPathValidatorResult(trust, intersection, workingPublicKey);
        }

        throw new CertPathValidatorException("Path processing failed on policy.", null, certPath, index);
    }

    private Date getValidDate(
        PKIXParameters paramsPKIX)
    {
        Date validDate = paramsPKIX.getDate();

        if (validDate == null)
        {
            validDate = new Date();
        }
        
        return validDate;
    }

    private void checkCRLs(PKIXParameters paramsPKIX, X509Certificate cert, Date validDate, X509Certificate sign, PublicKey workingPublicKey) 
        throws AnnotatedException 
    {
        X509CRLSelector crlselect;
        crlselect = new X509CRLSelector();

        try
        {
            crlselect.addIssuerName(getEncodedIssuerPrincipal(cert).getEncoded());
        }
        catch (IOException e)
        {
            throw new AnnotatedException("Cannot extract issuer from certificate: " + e, e);
        }

        crlselect.setCertificateChecking(cert);

        Iterator crl_iter = findCRLs(crlselect, paramsPKIX.getCertStores()).iterator();
        boolean validCrlFound = false;
        X509CRLEntry crl_entry;
        while (crl_iter.hasNext())
        {
            X509CRL crl = (X509CRL)crl_iter.next();

            if (cert.getNotAfter().after(crl.getThisUpdate()))
            {
                if (crl.getNextUpdate() == null
                    || validDate.before(crl.getNextUpdate())) 
                {
                    validCrlFound = true;
                }

                if (sign != null)
                {
                    boolean[] keyusage = sign.getKeyUsage();

                    if (keyusage != null
                        && (keyusage.length < 7 || !keyusage[CRL_SIGN]))
                    {
                        throw new AnnotatedException(
                            "Issuer certificate keyusage extension does not permit crl signing.\n" + sign);
                    }
                }

                try
                {
                    crl.verify(workingPublicKey, "SC");
                }
                catch (Exception e)
                {
                    throw new AnnotatedException("can't verify CRL: " + e, e);
                }

                crl_entry = crl.getRevokedCertificate(cert.getSerialNumber());
                if (crl_entry != null
                    && !validDate.before(crl_entry.getRevocationDate()))
                {
                    String reason = null;
                    
                    if (crl_entry.hasExtensions())
                    {
                        DEREnumerated reasonCode = DEREnumerated.getInstance(getExtensionValue(crl_entry, X509Extensions.ReasonCode.getId()));
                        if (reasonCode != null)
                        {
                            reason = crlReasons[reasonCode.getValue().intValue()];
                        }
                    }

                    SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z");
                    df.setTimeZone(TimeZone.getTimeZone("UTC"));
                    String message = "Certificate revocation after " + df.format(crl_entry.getRevocationDate());
                    
                    if (reason != null)
                    {
                        message += ", reason: " + reason;
                    }
                    
                    throw new AnnotatedException(message);
                }

                //
                // check the DeltaCRL indicator, base point and the issuing distribution point
                //
                ASN1Primitive idp = getExtensionValue(crl, ISSUING_DISTRIBUTION_POINT);
                ASN1Primitive dci = getExtensionValue(crl, DELTA_CRL_INDICATOR);

                if (dci != null)
                {
                    X509CRLSelector baseSelect = new X509CRLSelector();

                    try
                    {
                        baseSelect.addIssuerName(getIssuerPrincipal(crl).getEncoded());
                    }
                    catch (IOException e)
                    {
                        throw new AnnotatedException("can't extract issuer from certificate: " + e, e);
                    }

                    baseSelect.setMinCRLNumber(((DERInteger)dci).getPositiveValue());
                    baseSelect.setMaxCRLNumber(((DERInteger)getExtensionValue(crl, CRL_NUMBER)).getPositiveValue().subtract(BigInteger.valueOf(1)));
                    
                    boolean  foundBase = false;
                    Iterator it  = findCRLs(baseSelect, paramsPKIX.getCertStores()).iterator();
                    while (it.hasNext())
                    {
                        X509CRL base = (X509CRL)it.next();

                        ASN1Primitive baseIdp = getExtensionValue(base, ISSUING_DISTRIBUTION_POINT);
                        
                        if (idp == null)
                        {
                            if (baseIdp == null)
                            {
                                foundBase = true;
                                break;
                            }
                        }
                        else
                        {
                            if (idp.equals(baseIdp))
                            {
                                foundBase = true;
                                break;
                            }
                        }
                    }
                    
                    if (!foundBase)
                    {
                        throw new AnnotatedException("No base CRL for delta CRL");
                    }
                }

                if (idp != null)
                {
                    IssuingDistributionPoint    p = IssuingDistributionPoint.getInstance(idp);
                    BasicConstraints    bc = BasicConstraints.getInstance(getExtensionValue(cert, BASIC_CONSTRAINTS));
                    
                    if (p.onlyContainsUserCerts() && (bc != null && bc.isCA()))
                    {
                        throw new AnnotatedException("CA Cert CRL only contains user certificates");
                    }
                    
                    if (p.onlyContainsCACerts() && (bc == null || !bc.isCA()))
                    {
                        throw new AnnotatedException("End CRL only contains CA certificates");
                    }
                    
                    if (p.onlyContainsAttributeCerts())
                    {
                        throw new AnnotatedException("onlyContainsAttributeCerts boolean is asserted");
                    }
                }
            }
        }

        if (!validCrlFound)
        {
            throw new AnnotatedException("no valid CRL found");
        }
    }

    /**
     * Return a Collection of all CRLs found in the
     * CertStore's that are matching the crlSelect criteriums.
     *
     * @param certSelector a {@link CertSelector CertSelector}
     * object that will be used to select the certificates
     * @param certStores a List containing only {@link CertStore
     * CertStore} objects. These are used to search for
     * CRLs
     *
     * @return a Collection of all found {@link CRL CRL}
     * objects. May be empty but never <code>null</code>.
     */
    private Collection findCRLs(
        X509CRLSelector crlSelect,
        List            crlStores)
        throws AnnotatedException
    {
        Set crls = new HashSet();
        Iterator iter = crlStores.iterator();

        while (iter.hasNext())
        {
            CertStore   certStore = (CertStore)iter.next();

            try
            {
                crls.addAll(certStore.getCRLs(crlSelect));
            }
            catch (CertStoreException e)
            {
                throw new AnnotatedException("cannot extract crl: " + e, e);
            }
        }

        return crls;
    }

    /**
     * Search the given Set of TrustAnchor's for one that is the
     * issuer of the fiven X509 certificate.
     *
     * @param cert the X509 certificate
     * @param trustAnchors a Set of TrustAnchor's
     *
     * @return the <code>TrustAnchor</code> object if found or
     * <code>null</code> if not.
     *
     * @exception CertPathValidatorException if a TrustAnchor  was
     * found but the signature verification on the given certificate
     * has thrown an exception. This Exception can be obtainted with
     * <code>getCause()</code> method.
     **/
    final TrustAnchor findTrustAnchor(
        X509Certificate cert,
        CertPath        certPath,
        int             index,
        Set             trustAnchors) 
        throws CertPathValidatorException
    {
        Iterator iter = trustAnchors.iterator();
        TrustAnchor trust = null;
        PublicKey trustPublicKey = null;
        Exception invalidKeyEx = null;

        X509CertSelector certSelectX509 = new X509CertSelector();

        try
        {
            certSelectX509.setSubject(getEncodedIssuerPrincipal(cert).getEncoded());
        }
        catch (IOException ex)
        {
            throw new CertPathValidatorException(ex);
        }
        catch (AnnotatedException ex)
        {
            throw new CertPathValidatorException(ex.getUnderlyingException());
        }

        while (iter.hasNext() && trust == null)
        {
            trust = (TrustAnchor)iter.next();
            if (trust.getTrustedCert() != null)
            {
                if (certSelectX509.match(trust.getTrustedCert()))
                {
                    trustPublicKey = trust.getTrustedCert().getPublicKey();
                }
                else
                {
                    trust = null;
                }
            }
            else if (trust.getCAName() != null
                        && trust.getCAPublicKey() != null)
            {
                try
                {
                    X509Principal certIssuer = getEncodedIssuerPrincipal(cert);
                    X509Principal caName = new X509Principal(trust.getCAName());
                    if (certIssuer.equals(caName))
                    {
                        trustPublicKey = trust.getCAPublicKey();
                    }
                    else
                    {
                        trust = null;
                    }
                }
                catch (AnnotatedException ex)
                {
                    throw new CertPathValidatorException(ex.getMessage(), ex.getUnderlyingException(), certPath, index);
                }
                catch (IllegalArgumentException ex)
                {
                    trust = null;
                }
            }
            else
            {
                trust = null;
            }
            
            if (trustPublicKey != null)
            {
                try
                {
                    cert.verify(trustPublicKey);
                }
                catch (Exception ex)
                {
                    invalidKeyEx = ex;
                    trust = null;
                }
            }
        }
    
        if (trust == null && invalidKeyEx != null)
        {
            throw new CertPathValidatorException("TrustAnchor found but certificate validation failed.", invalidKeyEx, certPath, index);
        }

        return trust;
    }
    
    private X509Principal getIssuerPrincipal(X509CRL crl)
        throws AnnotatedException
    {
        try
        {
            return PrincipalUtil.getIssuerX509Principal(crl);
        }
        catch (CRLException e)
        {
            throw new AnnotatedException("can't get CRL issuer principal", e);
        }
    }

    private X509Principal getEncodedIssuerPrincipal(X509Certificate cert)
        throws AnnotatedException
    {
        try
        {
            return PrincipalUtil.getIssuerX509Principal(cert);
        }
        catch (CertificateEncodingException e)
        {
            throw new AnnotatedException("can't get issuer principal.", e);
        }
    }

    private X509Principal getSubjectPrincipal(X509Certificate cert)
        throws AnnotatedException
    {
        try
        {
            return PrincipalUtil.getSubjectX509Principal(cert);
        }
        catch (CertificateEncodingException e)
        {
            throw new AnnotatedException("can't get subject principal.", e);
        }
    }
}
