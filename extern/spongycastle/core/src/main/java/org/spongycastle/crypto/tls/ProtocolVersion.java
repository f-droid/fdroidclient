package org.spongycastle.crypto.tls;

import java.io.IOException;

public final class ProtocolVersion
{
    public static final ProtocolVersion SSLv3 = new ProtocolVersion(0x0300, "SSL 3.0");
    public static final ProtocolVersion TLSv10 = new ProtocolVersion(0x0301, "TLS 1.0");
    public static final ProtocolVersion TLSv11 = new ProtocolVersion(0x0302, "TLS 1.1");
    public static final ProtocolVersion TLSv12 = new ProtocolVersion(0x0303, "TLS 1.2");
    public static final ProtocolVersion DTLSv10 = new ProtocolVersion(0xFEFF, "DTLS 1.0");
    public static final ProtocolVersion DTLSv12 = new ProtocolVersion(0xFEFD, "DTLS 1.2");

    private int version;
    private String name;

    private ProtocolVersion(int v, String name)
    {
        this.version = v & 0xffff;
        this.name = name;
    }

    public int getFullVersion()
    {
        return version;
    }

    public int getMajorVersion()
    {
        return version >> 8;
    }

    public int getMinorVersion()
    {
        return version & 0xff;
    }

    public boolean isDTLS()
    {
        return getMajorVersion() == 0xFE;
    }

    public boolean isSSL()
    {
        return this == SSLv3;
    }

    public ProtocolVersion getEquivalentTLSVersion()
    {
        if (!isDTLS())
        {
            return this;
        }
        if (this == DTLSv10)
        {
            return TLSv11;
        }
        return TLSv12;
    }

    public boolean isEqualOrEarlierVersionOf(ProtocolVersion version)
    {
        if (getMajorVersion() != version.getMajorVersion())
        {
            return false;
        }
        int diffMinorVersion = version.getMinorVersion() - getMinorVersion();
        return isDTLS() ? diffMinorVersion <= 0 : diffMinorVersion >= 0;
    }

    public boolean isLaterVersionOf(ProtocolVersion version)
    {
        if (getMajorVersion() != version.getMajorVersion())
        {
            return false;
        }
        int diffMinorVersion = version.getMinorVersion() - getMinorVersion();
        return isDTLS() ? diffMinorVersion > 0 : diffMinorVersion < 0;
    }

    public boolean equals(Object obj)
    {
        return this == obj;
    }

    public int hashCode()
    {
        return version;
    }

    public static ProtocolVersion get(int major, int minor)
        throws IOException
    {
        switch (major)
        {
        case 0x03:
            switch (minor)
            {
            case 0x00:
                return SSLv3;
            case 0x01:
                return TLSv10;
            case 0x02:
                return TLSv11;
            case 0x03:
                return TLSv12;
            }
        case 0xFE:
            switch (minor)
            {
            case 0xFF:
                return DTLSv10;
            case 0xFD:
                return DTLSv12;
            }
        }

        throw new TlsFatalAlert(AlertDescription.illegal_parameter);
    }

    public String toString()
    {
        return name;
    }
}
