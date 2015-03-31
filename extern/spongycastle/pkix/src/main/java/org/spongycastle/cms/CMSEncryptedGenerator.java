package org.spongycastle.cms;

/**
 * General class for generating a CMS encrypted-data message.
 */
public class CMSEncryptedGenerator
{
    protected CMSAttributeTableGenerator unprotectedAttributeGenerator = null;

    /**
     * base constructor
     */
    protected CMSEncryptedGenerator()
    {
    }

    public void setUnprotectedAttributeGenerator(CMSAttributeTableGenerator unprotectedAttributeGenerator)
    {
        this.unprotectedAttributeGenerator = unprotectedAttributeGenerator;
    }
}
