package org.spongycastle.jce.provider;

import java.io.IOException;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.PublicKey;
import java.security.cert.CertPath;
import java.security.cert.CertPathBuilder;
import java.security.cert.CertPathBuilderException;
import java.security.cert.CertPathValidatorException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.PKIXCertPathChecker;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.security.cert.X509Extension;
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
import java.util.Vector;

import org.spongycastle.asn1.ASN1EncodableVector;
import org.spongycastle.asn1.ASN1InputStream;
import org.spongycastle.asn1.ASN1Sequence;
import org.spongycastle.asn1.ASN1TaggedObject;
import org.spongycastle.asn1.x509.BasicConstraints;
import org.spongycastle.asn1.x509.CRLDistPoint;
import org.spongycastle.asn1.x509.CRLReason;
import org.spongycastle.asn1.x509.DistributionPoint;
import org.spongycastle.asn1.x509.DistributionPointName;
import org.spongycastle.asn1.x509.GeneralName;
import org.spongycastle.asn1.x509.GeneralNames;
import org.spongycastle.asn1.x509.GeneralSubtree;
import org.spongycastle.asn1.x509.IssuingDistributionPoint;
import org.spongycastle.asn1.x509.NameConstraints;
import org.spongycastle.asn1.x509.PolicyInformation;
import org.spongycastle.asn1.x509.X509Extensions;
import org.spongycastle.asn1.x509.X509Name;
import org.spongycastle.util.Arrays;

public class RFC3280CertPathUtilities
{
    public static final String CERTIFICATE_POLICIES = X509Extensions.CertificatePolicies.getId();

    public static final String POLICY_MAPPINGS = X509Extensions.PolicyMappings.getId();

    public static final String INHIBIT_ANY_POLICY = X509Extensions.InhibitAnyPolicy.getId();

    public static final String ISSUING_DISTRIBUTION_POINT = X509Extensions.IssuingDistributionPoint.getId();

    public static final String FRESHEST_CRL = X509Extensions.FreshestCRL.getId();

    public static final String DELTA_CRL_INDICATOR = X509Extensions.DeltaCRLIndicator.getId();

    public static final String POLICY_CONSTRAINTS = X509Extensions.PolicyConstraints.getId();

    public static final String BASIC_CONSTRAINTS = X509Extensions.BasicConstraints.getId();

    public static final String CRL_DISTRIBUTION_POINTS = X509Extensions.CRLDistributionPoints.getId();

    public static final String SUBJECT_ALTERNATIVE_NAME = X509Extensions.SubjectAlternativeName.getId();

    public static final String NAME_CONSTRAINTS = X509Extensions.NameConstraints.getId();

    public static final String AUTHORITY_KEY_IDENTIFIER = X509Extensions.AuthorityKeyIdentifier.getId();

    public static final String KEY_USAGE = X509Extensions.KeyUsage.getId();

    public static final String CRL_NUMBER = X509Extensions.CRLNumber.getId();

    public static final String ANY_POLICY = "2.5.29.32.0";

    /*
     * key usage bits
     */
    public static final int KEY_CERT_SIGN = 5;

    public static final int CRL_SIGN = 6;

}
