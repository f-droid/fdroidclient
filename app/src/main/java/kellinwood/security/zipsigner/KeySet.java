

/*
 * Copyright (C) 2010 Ken Ellinwood
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package kellinwood.security.zipsigner;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;

public class KeySet {

    String name;
    
    // certificate
    X509Certificate publicKey = null;
    
    // private key
    PrivateKey privateKey = null; 

    // signature block template
    byte[] sigBlockTemplate = null;

    String signatureAlgorithm = "SHA1withRSA";
    
    public KeySet() {
    }
    
    public KeySet( String name, X509Certificate publicKey, PrivateKey privateKey, byte[] sigBlockTemplate)
    {
        this.name = name;
        this.publicKey = publicKey;
        this.privateKey = privateKey;
        this.sigBlockTemplate = sigBlockTemplate;
    }

    public KeySet( String name, X509Certificate publicKey, PrivateKey privateKey, String signatureAlgorithm, byte[] sigBlockTemplate)
    {
        this.name = name;
        this.publicKey = publicKey;
        this.privateKey = privateKey;
        if (signatureAlgorithm != null) this.signatureAlgorithm = signatureAlgorithm;
        this.sigBlockTemplate = sigBlockTemplate;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public X509Certificate getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(X509Certificate publicKey) {
        this.publicKey = publicKey;
    }

    public PrivateKey getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(PrivateKey privateKey) {
        this.privateKey = privateKey;
    }

    public byte[] getSigBlockTemplate() {
        return sigBlockTemplate;
    }

    public void setSigBlockTemplate(byte[] sigBlockTemplate) {
        this.sigBlockTemplate = sigBlockTemplate;
    }

    public String getSignatureAlgorithm() {
        return signatureAlgorithm;
    }

    public void setSignatureAlgorithm(String signatureAlgorithm) {
        if (signatureAlgorithm == null) signatureAlgorithm = "SHA1withRSA";
        else this.signatureAlgorithm = signatureAlgorithm;
    }
}
