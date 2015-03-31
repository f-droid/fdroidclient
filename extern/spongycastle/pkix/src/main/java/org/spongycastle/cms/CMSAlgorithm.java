package org.spongycastle.cms;

import org.spongycastle.asn1.ASN1ObjectIdentifier;
import org.spongycastle.asn1.cryptopro.CryptoProObjectIdentifiers;
import org.spongycastle.asn1.kisa.KISAObjectIdentifiers;
import org.spongycastle.asn1.nist.NISTObjectIdentifiers;
import org.spongycastle.asn1.ntt.NTTObjectIdentifiers;
import org.spongycastle.asn1.oiw.OIWObjectIdentifiers;
import org.spongycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.spongycastle.asn1.teletrust.TeleTrusTObjectIdentifiers;
import org.spongycastle.asn1.x9.X9ObjectIdentifiers;

public class CMSAlgorithm
{
    public static final ASN1ObjectIdentifier  DES_CBC         = OIWObjectIdentifiers.desCBC;
    public static final ASN1ObjectIdentifier  DES_EDE3_CBC    = PKCSObjectIdentifiers.des_EDE3_CBC;
    public static final ASN1ObjectIdentifier  RC2_CBC         = PKCSObjectIdentifiers.RC2_CBC;
    public static final ASN1ObjectIdentifier  IDEA_CBC        = new ASN1ObjectIdentifier("1.3.6.1.4.1.188.7.1.1.2");
    public static final ASN1ObjectIdentifier  CAST5_CBC       = new ASN1ObjectIdentifier("1.2.840.113533.7.66.10");
    public static final ASN1ObjectIdentifier  AES128_CBC      = NISTObjectIdentifiers.id_aes128_CBC;
    public static final ASN1ObjectIdentifier  AES192_CBC      = NISTObjectIdentifiers.id_aes192_CBC;
    public static final ASN1ObjectIdentifier  AES256_CBC      = NISTObjectIdentifiers.id_aes256_CBC;
    public static final ASN1ObjectIdentifier  CAMELLIA128_CBC = NTTObjectIdentifiers.id_camellia128_cbc;
    public static final ASN1ObjectIdentifier  CAMELLIA192_CBC = NTTObjectIdentifiers.id_camellia192_cbc;
    public static final ASN1ObjectIdentifier  CAMELLIA256_CBC = NTTObjectIdentifiers.id_camellia256_cbc;
    public static final ASN1ObjectIdentifier  SEED_CBC        = KISAObjectIdentifiers.id_seedCBC;

    public static final ASN1ObjectIdentifier  DES_EDE3_WRAP   = PKCSObjectIdentifiers.id_alg_CMS3DESwrap;
    public static final ASN1ObjectIdentifier  AES128_WRAP     = NISTObjectIdentifiers.id_aes128_wrap;
    public static final ASN1ObjectIdentifier  AES192_WRAP     = NISTObjectIdentifiers.id_aes192_wrap;
    public static final ASN1ObjectIdentifier  AES256_WRAP     = NISTObjectIdentifiers.id_aes256_wrap;
    public static final ASN1ObjectIdentifier  CAMELLIA128_WRAP = NTTObjectIdentifiers.id_camellia128_wrap;
    public static final ASN1ObjectIdentifier  CAMELLIA192_WRAP = NTTObjectIdentifiers.id_camellia192_wrap;
    public static final ASN1ObjectIdentifier  CAMELLIA256_WRAP = NTTObjectIdentifiers.id_camellia256_wrap;
    public static final ASN1ObjectIdentifier  SEED_WRAP       = KISAObjectIdentifiers.id_npki_app_cmsSeed_wrap;

    public static final ASN1ObjectIdentifier  ECDH_SHA1KDF    = X9ObjectIdentifiers.dhSinglePass_stdDH_sha1kdf_scheme;
    public static final ASN1ObjectIdentifier  ECMQV_SHA1KDF   = X9ObjectIdentifiers.mqvSinglePass_sha1kdf_scheme;

    public static final ASN1ObjectIdentifier  SHA1 = OIWObjectIdentifiers.idSHA1;
    public static final ASN1ObjectIdentifier  SHA224 = NISTObjectIdentifiers.id_sha224;
    public static final ASN1ObjectIdentifier  SHA256 = NISTObjectIdentifiers.id_sha256;
    public static final ASN1ObjectIdentifier  SHA384 = NISTObjectIdentifiers.id_sha384;
    public static final ASN1ObjectIdentifier  SHA512 = NISTObjectIdentifiers.id_sha512;
    public static final ASN1ObjectIdentifier  MD5 = PKCSObjectIdentifiers.md5;
    public static final ASN1ObjectIdentifier  GOST3411 = CryptoProObjectIdentifiers.gostR3411;
    public static final ASN1ObjectIdentifier  RIPEMD128 = TeleTrusTObjectIdentifiers.ripemd128;
    public static final ASN1ObjectIdentifier  RIPEMD160 = TeleTrusTObjectIdentifiers.ripemd160;
    public static final ASN1ObjectIdentifier  RIPEMD256 = TeleTrusTObjectIdentifiers.ripemd256;

}
