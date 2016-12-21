/*
 * Copyright 2016 Real Logic Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.net.*;

public final class MultiCastReceiver
    implements Runnable
{
    public static final int BUFFER_SIZE = 1000;

    private static volatile long messageCounter = 0L;
    private static long lastMessageCounter = 0L;
    private static long lastTimestamp = System.currentTimeMillis();

    public static void main(final String[] args)
        throws Exception
    {
        init(args);

        final int multiCastPort = 4447;
        final String address = "230.0.0.1";
        final NetworkInterface networkInterface = NetworkInterface.getByName(args[0]);
        final MulticastSocket receiveSocket = new MulticastSocket(multiCastPort);
        final SocketAddress socketAddress = new InetSocketAddress(address, multiCastPort);
        receiveSocket.joinGroup(socketAddress, networkInterface);

        final byte[] buffer = new byte[BUFFER_SIZE];
        final DatagramPacket packet = new DatagramPacket(buffer, BUFFER_SIZE);

        new Thread(new MultiCastReceiver()).start();

        while (true)
        {
            packet.setLength(BUFFER_SIZE);
            receiveSocket.receive(packet);
            ++messageCounter;
        }
    }

    private static void init(final String[] args)
    {
        System.setProperty("java.net.preferIPv4Stack", "true");

        if (1 != args.length)
        {
            System.out.println("Usage: java MultiCastReceiver <interface name>");
            System.exit(1);
        }
    }

    public void run()
    {
        while (true)
        {
            try
            {
                Thread.sleep(1000L);
            }
            catch (final InterruptedException ex)
            {
                break;
            }

            final long newTimestamp = System.currentTimeMillis();
            final long duration = newTimestamp - lastTimestamp;
            final long newMessageCounter = messageCounter;
            final long numberOfMessages = newMessageCounter - lastMessageCounter;

            System.out.format("Received %d messages in %dms%n", numberOfMessages, duration);

            lastTimestamp = newTimestamp;
            lastMessageCounter = newMessageCounter;
        }
    }
}
