package org.spongycastle.crypto.tls;

import java.io.IOException;
import javax.microedition.io.DatagramConnection;
import javax.microedition.io.Datagram;

public class UDPTransport
    implements DatagramTransport
{

    protected final static int MIN_IP_OVERHEAD = 20;
    protected final static int MAX_IP_OVERHEAD = MIN_IP_OVERHEAD + 64;
    protected final static int UDP_OVERHEAD = 8;

    protected final DatagramConnection socket;
    protected final int receiveLimit, sendLimit;

    public UDPTransport(DatagramConnection socket, int mtu)
        throws IOException
    {
        //
        // In 1.3 and earlier sockets were bound and connected during creation
        //
        //if (!socket.isBound() || !socket.isConnected())
        //{
        //    throw new IllegalArgumentException("'socket' must be bound and connected");
        //}

        this.socket = socket;

        // NOTE: As of JDK 1.6, can use NetworkInterface.getMTU

        this.receiveLimit = mtu - MIN_IP_OVERHEAD - UDP_OVERHEAD;
        this.sendLimit = mtu - MAX_IP_OVERHEAD - UDP_OVERHEAD;
    }

    public int getReceiveLimit()
    {
        return receiveLimit;
    }

    public int getSendLimit()
    {
        // TODO[DTLS] Implement Path-MTU discovery?
        return sendLimit;
    }

    public int receive(byte[] buf, int off, int len, int waitMillis)
        throws IOException
    {
        //socket.setSoTimeout(waitMillis); -- not applicable

        if (off == 0)
        {
            Datagram packet = socket.newDatagram(buf, len);
            socket.receive(packet);

            return packet.getLength();
        }
        else
        {
            byte[] rv = new byte[len];

            Datagram packet = socket.newDatagram(rv, len);
            socket.receive(packet);

            System.arraycopy(rv, 0, buf, off, packet.getLength());

            return packet.getLength();
        }
    }

    public void send(byte[] buf, int off, int len)
        throws IOException
    {
        if (len > getSendLimit())
        {
            /*
             * RFC 4347 4.1.1. "If the application attempts to send a record larger than the MTU,
             * the DTLS implementation SHOULD generate an error, thus avoiding sending a packet
             * which will be fragmented."
             */
            // TODO Exception
        }

        if (off == 0)
        {
            Datagram packet = socket.newDatagram(buf, len);
            socket.send(packet);
        }
        else
        {
            byte[] data = new byte[len];

            System.arraycopy(buf, off, data, 0, len);

            Datagram packet = socket.newDatagram(data, len);
            socket.send(packet);
        }
    }

    public void close()
        throws IOException
    {
        socket.close();
    }
}
