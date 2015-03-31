package org.spongycastle.jce.provider;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.PublicKey;
import java.security.cert.*;
import org.spongycastle.jce.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Implements the PKIX CertPathBuilding algorithem for BouncyCastle.
 * <br />
 * <b>MAYBE: implement more CertPath validation whil build path to omit invalid pathes</b>
 *
 * @see CertPathBuilderSpi
 **/
public class PKIXCertPathBuilderSpi
    extends CertPathBuilderSpi
{
    /**
     * Build and validate a CertPath using the given parameter.
     *
     * @param params PKIXBuilderParameters object containing all
     * information to build the CertPath
     **/
    public CertPathBuilderResult engineBuild(
        CertPathParameters params)
        throws CertPathBuilderException, InvalidAlgorithmParameterException 
    {
        if (!(params instanceof PKIXBuilderParameters))
        {
            throw new InvalidAlgorithmParameterException("params must be a PKIXBuilderParameters instance");
        }

        PKIXBuilderParameters pkixParams = (PKIXBuilderParameters)params;

        Collection targets;
        Iterator targetIter;
        List certPathList = new ArrayList();
        X509Certificate cert;
        Collection      certs;
        CertPath        certPath = null;
        Exception       certPathException = null;

        // search target certificates
        CertSelector certSelect = pkixParams.getTargetCertConstraints();
        if (certSelect == null)
        {
            throw new CertPathBuilderException("targetCertConstraints must be non-null for CertPath building");
        }

        try
        {
            targets = findCertificates(certSelect, pkixParams.getCertStores());
        }
        catch (CertStoreException e)
        {
            throw new CertPathBuilderException(e);
        }

        if (targets.isEmpty())
        {
            throw new CertPathBuilderException("no certificate found matching targetCertContraints");
        }

        CertificateFactory  cFact;
        CertPathValidator   validator;

        try
        {
            cFact = CertificateFactory.getInstance("X.509", "SC");
            validator = CertPathValidator.getInstance("PKIX", "SC");
        }
        catch (Exception e)
        {
            throw new CertPathBuilderException("exception creating support classes: " + e);
        }

        //
        // check all potential target certificates
        targetIter = targets.iterator();
        while (targetIter.hasNext())
        {
            cert = (X509Certificate)targetIter.next();
            certPathList.clear();
            while (cert != null)
            {
                // add cert to the certpath
                certPathList.add(cert);

                // check wether the issuer of <cert> is a TrustAnchor 
                if (findTrustAnchor(cert, pkixParams.getTrustAnchors()) != null)
                {
                    try
                    {
                        certPath = cFact.generateCertPath(certPathList);

                        PKIXCertPathValidatorResult result = (PKIXCertPathValidatorResult)validator.validate(certPath, pkixParams);

                        return new PKIXCertPathBuilderResult(certPath,
                                     result.getTrustAnchor(),
                                     result.getPolicyTree(),
                                     result.getPublicKey());
                    }
                    catch (CertificateException ex)
                    {
                        certPathException = ex;
                    }
                    catch (CertPathValidatorException ex)
                    {
                        certPathException = ex;
                    }
                    // if validation failed go to next certificate
                    cert = null;
                }
                else
                {
                    // try to get the issuer certificate from one
                    // of the CertStores
                    try
                    {
                        X509Certificate issuer = findIssuer(cert, pkixParams.getCertStores());
                        if (issuer.equals(cert))
                        {
                            cert = null;
                        }
                        else
                        {
                            cert = issuer;
                        }
                    }
                    catch (CertPathValidatorException ex)
                    {
                        certPathException = ex;
                        cert = null;
                    }
                }
            }
        }

        if (certPath != null)
        {
            throw new CertPathBuilderException("found certificate chain, but could not be validated", certPathException);
        }

        throw new CertPathBuilderException("unable to find certificate chain");
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
     * found but the signature verificytion on the given certificate
     * has thrown an exception. This Exception can be obtainted with
     * <code>getCause()</code> method.
     **/
    final TrustAnchor findTrustAnchor(
        X509Certificate cert,
        Set             trustAnchors) 
        throws CertPathBuilderException
    {
        Iterator iter = trustAnchors.iterator();
        TrustAnchor trust = null;
        PublicKey trustPublicKey = null;
        Exception invalidKeyEx = null;

        X509CertSelector certSelectX509 = new X509CertSelector();

        try
        {
            certSelectX509.setSubject(PrincipalUtil.getIssuerX509Principal(cert).getEncoded());
        }
        catch (CertificateEncodingException ex)
        {
            throw new CertPathBuilderException("can't get trust anchor principal",null);
        }
        catch (IOException ex)
        {
            throw new CertPathBuilderException("can't get trust anchor principal",null);
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
                    X509Principal certIssuer = PrincipalUtil.getIssuerX509Principal(cert);
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
                catch (CertificateEncodingException ex)
                {
                    trust = null;
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
            throw new CertPathBuilderException("TrustAnchor found put certificate validation failed",invalidKeyEx);
        }

        return trust;
    }

    /**
     * Return a Collection of all certificates found in the
     * CertStore's that are matching the certSelect criteriums.
     *
     * @param certSelector a {@link CertSelector CertSelector}
     * object that will be used to select the certificates
     * @param certStores a List containing only {@link CertStore
     * CertStore} objects. These are used to search for
     * certificates
     *
     * @return a Collection of all found {@link Certificate Certificate}
     * objects. May be empty but never <code>null</code>.
     **/
    private Collection findCertificates(
        CertSelector    certSelect,
        List            certStores) 
        throws CertStoreException
    {
        Set certs = new HashSet();
        Iterator iter = certStores.iterator();

        while (iter.hasNext())
        {
            CertStore   certStore = (CertStore)iter.next();

            certs.addAll(certStore.getCertificates(certSelect));
        }

        return certs;
    }
    
    /**
     * Find the issuer certificate of the given certificate.
     *
     * @param cert the certificate hows issuer certificate should
     * be found.
     * @param certStores a list of <code>CertStore</code> object
     * that will be searched
     *
     * @return then <code>X509Certificate</code> object containing
     * the issuer certificate or <code>null</code> if not found
     *
     * @exception CertPathValidatorException if a TrustAnchor  was
     * found but the signature verificytion on the given certificate
     * has thrown an exception. This Exception can be obtainted with
     * <code>getCause()</code> method.
     **/
    private X509Certificate findIssuer(
        X509Certificate cert,
        List certStores)
        throws CertPathValidatorException
    {
        Exception invalidKeyEx = null;
        X509CertSelector certSelect = new X509CertSelector();
        try
        {
            certSelect.setSubject(PrincipalUtil.getIssuerX509Principal(cert).getEncoded());
        }
        catch (CertificateEncodingException ex)
        {
            throw new CertPathValidatorException("Issuer not found", null, null, -1);
        }
        catch (IOException ex)
        {
            throw new CertPathValidatorException("Issuer not found", null, null, -1);
        }

        Iterator iter;
        try
        {
            iter = findCertificates(certSelect, certStores).iterator();
        }
        catch (CertStoreException e)
        {
            throw new CertPathValidatorException(e);
        }
        
        X509Certificate issuer = null;
        while (iter.hasNext() && issuer == null)
        {
            issuer = (X509Certificate)iter.next();
            try
            {
                cert.verify(issuer.getPublicKey());
            }
            catch (Exception ex)
            {
                invalidKeyEx = ex;
                issuer = null;
            }
        }

        if (issuer == null && invalidKeyEx == null)
        {
           throw new CertPathValidatorException("Issuer not found", null, null, -1);
        }

        if (issuer == null && invalidKeyEx != null)
        {
            throw new CertPathValidatorException("issuer found but certificate validation failed",invalidKeyEx,null,-1);
        }

        return issuer;
    }
}
