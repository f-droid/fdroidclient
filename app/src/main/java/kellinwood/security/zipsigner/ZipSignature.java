/*
 * Copyright (C) 2010 Ken Ellinwood.
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

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.PrivateKey;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;

@SuppressWarnings("restriction")
public class ZipSignature {

    byte[] beforeAlgorithmIdBytes =  { 0x30, 0x21 };

    //      byte[] algorithmIdBytes;    
    //		algorithmIdBytes =  sun.security.x509.AlgorithmId.get("SHA1").encode();    
    byte[] algorithmIdBytes = {0x30, 0x09, 0x06, 0x05, 0x2B, 0x0E, 0x03, 0x02, 0x1A, 0x05, 0x00 }; 

    byte[] afterAlgorithmIdBytes = { 0x04, 0x14 };

    Cipher cipher;

    MessageDigest md;


    public ZipSignature() throws IOException, GeneralSecurityException
    {
        md = MessageDigest.getInstance("SHA1");
        cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
    }

    public void initSign( PrivateKey privateKey) throws InvalidKeyException 
    {
        cipher.init(Cipher.ENCRYPT_MODE, privateKey);
    }

    public void update( byte[] data) {
        md.update( data);
    }

    public void update( byte[] data, int offset, int count) {
        md.update( data, offset, count);
    }

    public byte[] sign() throws BadPaddingException, IllegalBlockSizeException 
    {
        cipher.update( beforeAlgorithmIdBytes);
        cipher.update( algorithmIdBytes);
        cipher.update( afterAlgorithmIdBytes);
        cipher.update( md.digest());		
        return cipher.doFinal();
    }
}
