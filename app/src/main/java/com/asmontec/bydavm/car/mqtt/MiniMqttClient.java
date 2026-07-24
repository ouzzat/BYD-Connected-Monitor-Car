package com.asmontec.bydavm.car.mqtt;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;

/**
 * Client MQTT 3.1.1 minimal, écrit sans bibliothèque externe.
 * Ne prend en charge que ce qui est nécessaire à l'agent voiture :
 * CONNECT / PUBLISH (QoS 0, non retained) / PINGREQ / DISCONNECT.
 * Aucun abonnement, aucune réception de message : lecture seule côté véhicule.
 */
public final class MiniMqttClient {

    private static final byte TYPE_CONNECT = 1;
    private static final byte TYPE_CONNACK = 2;
    private static final byte TYPE_PUBLISH = 3;
    private static final byte TYPE_PINGREQ = 12;
    private static final byte TYPE_PINGRESP = 13;
    private static final byte TYPE_DISCONNECT = 14;

    private final String host;
    private final int port;
    private final int connectTimeoutMs;
    private final String certSha256Pin;

    private SSLSocket socket;
    private OutputStream out;
    private InputStream in;
    private final Object writeLock = new Object();

    private Thread readerThread;
    private Thread keepAliveThread;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private volatile long lastWriteAt;

    private int keepAliveSeconds = 30;

    /** Message testament (LWT) publié automatiquement par le broker en cas de coupure anormale. */
    public static final class Will {
        public final String topic;
        public final byte[] payload;
        public final boolean retain;

        public Will(String topic, byte[] payload, boolean retain) {
            this.topic = topic;
            this.payload = payload;
            this.retain = retain;
        }
    }

    public MiniMqttClient(String host, int port, int connectTimeoutMs, String certSha256Pin) {
        this.host = host;
        this.port = port;
        this.connectTimeoutMs = connectTimeoutMs;
        this.certSha256Pin = certSha256Pin;
    }

    /** Établit la connexion TLS puis effectue la poignée de main MQTT CONNECT/CONNACK. */
    public void connect(String clientId, String username, char[] password, int keepAliveSeconds,
                         MqttListener listener) throws IOException {
        connect(clientId, username, password, keepAliveSeconds, null, listener);
    }

    public void connect(String clientId, String username, char[] password, int keepAliveSeconds,
                         Will will, MqttListener listener) throws IOException {
        this.keepAliveSeconds = keepAliveSeconds;

        Socket plain = new Socket();
        plain.connect(new InetSocketAddress(host, port), connectTimeoutMs);
        plain.setSoTimeout(Math.max(connectTimeoutMs, (keepAliveSeconds + 10) * 1000));

        SSLContext context;
        try {
            context = SSLContext.getInstance("TLS");
            context.init(null, null, null);
        } catch (Exception e) {
            throw new IOException("Impossible d'initialiser TLS", e);
        }

        socket = (SSLSocket) context.getSocketFactory().createSocket(plain, host, port, true);
        String[] supported = socket.getSupportedProtocols();
        socket.setEnabledProtocols(intersectPreferred(supported, "TLSv1.3", "TLSv1.2"));

        SSLParameters params = socket.getSSLParameters();
        params.setEndpointIdentificationAlgorithm("HTTPS");
        socket.setSSLParameters(params);
        socket.setUseClientMode(true);
        socket.startHandshake();

        if (certSha256Pin != null && !certSha256Pin.trim().isEmpty()) {
            verifyPin(socket, certSha256Pin.trim());
        }

        out = socket.getOutputStream();
        in = socket.getInputStream();

        sendConnect(clientId, username, password, keepAliveSeconds, will);
        readConnAck();

        running.set(true);
        lastWriteAt = System.currentTimeMillis();

        readerThread = new Thread(() -> readLoop(listener), "mqtt-car-reader");
        readerThread.setDaemon(true);
        readerThread.start();

        keepAliveThread = new Thread(this::keepAliveLoop, "mqtt-car-keepalive");
        keepAliveThread.setDaemon(true);
        keepAliveThread.start();
    }

    private static String[] intersectPreferred(String[] supported, String... preferredOrder) {
        java.util.List<String> result = new java.util.ArrayList<>();
        for (String p : preferredOrder) {
            for (String s : supported) {
                if (s.equals(p)) {
                    result.add(p);
                    break;
                }
            }
        }
        if (result.isEmpty()) {
            return supported;
        }
        return result.toArray(new String[0]);
    }

    private static void verifyPin(SSLSocket socket, String expectedHexPin) throws IOException {
        try {
            Certificate[] chain = socket.getSession().getPeerCertificates();
            if (chain.length == 0 || !(chain[0] instanceof X509Certificate)) {
                throw new IOException("Certificat serveur inattendu");
            }
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] fingerprint = digest.digest(chain[0].getEncoded());
            StringBuilder hex = new StringBuilder();
            for (byte b : fingerprint) {
                hex.append(String.format(java.util.Locale.ROOT, "%02x", b));
            }
            if (!hex.toString().equalsIgnoreCase(expectedHexPin.replace(":", ""))) {
                throw new IOException("Empreinte du certificat non conforme (épinglage échoué)");
            }
        } catch (java.security.GeneralSecurityException e) {
            throw new IOException("Échec de la vérification de l'empreinte du certificat", e);
        }
    }

    public boolean isConnected() {
        return running.get() && socket != null && socket.isConnected() && !socket.isClosed();
    }

    public void publish(String topic, byte[] payload) throws IOException {
        publish(topic, payload, false);
    }

    public void publish(String topic, byte[] payload, boolean retain) throws IOException {
        byte[] topicBytes = topic.getBytes(StandardCharsets.UTF_8);
        ByteArrayOutputStream variableAndPayload = new ByteArrayOutputStream();
        variableAndPayload.write((topicBytes.length >> 8) & 0xFF);
        variableAndPayload.write(topicBytes.length & 0xFF);
        variableAndPayload.write(topicBytes);
        variableAndPayload.write(payload);

        byte flags = (byte) (retain ? 0x01 : 0x00);
        writeFrame(TYPE_PUBLISH, flags, variableAndPayload.toByteArray());
    }

    public void disconnectGracefully() {
        try {
            writeFrame(TYPE_DISCONNECT, (byte) 0x00, new byte[0]);
        } catch (IOException ignored) {
            // La connexion se ferme de toute façon.
        }
        close();
    }

    public void close() {
        running.set(false);
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException ignored) {
        }
        if (readerThread != null) {
            readerThread.interrupt();
        }
        if (keepAliveThread != null) {
            keepAliveThread.interrupt();
        }
    }

    private void sendConnect(String clientId, String username, char[] password, int keepAliveSeconds,
                              Will will) throws IOException {
        ByteArrayOutputStream variable = new ByteArrayOutputStream();
        writeUtf8(variable, "MQTT");
        variable.write(4); // niveau de protocole 3.1.1

        int connectFlags = 0x02; // clean session
        if (will != null) {
            connectFlags |= 0x04;
            if (will.retain) {
                connectFlags |= 0x20;
            }
        }
        if (username != null && !username.isEmpty()) {
            connectFlags |= 0x80;
        }
        if (password != null && password.length > 0) {
            connectFlags |= 0x40;
        }
        variable.write(connectFlags);
        variable.write((keepAliveSeconds >> 8) & 0xFF);
        variable.write(keepAliveSeconds & 0xFF);

        ByteArrayOutputStream payload = new ByteArrayOutputStream();
        writeUtf8(payload, clientId);
        if (will != null) {
            writeUtf8(payload, will.topic);
            payload.write((will.payload.length >> 8) & 0xFF);
            payload.write(will.payload.length & 0xFF);
            payload.write(will.payload);
        }
        if (username != null && !username.isEmpty()) {
            writeUtf8(payload, username);
        }
        if (password != null && password.length > 0) {
            byte[] pw = new String(password).getBytes(StandardCharsets.UTF_8);
            payload.write((pw.length >> 8) & 0xFF);
            payload.write(pw.length & 0xFF);
            payload.write(pw);
        }

        ByteArrayOutputStream full = new ByteArrayOutputStream();
        full.write(variable.toByteArray());
        full.write(payload.toByteArray());

        writeFrame(TYPE_CONNECT, (byte) 0x00, full.toByteArray());
    }

    private void readConnAck() throws IOException {
        int header = in.read();
        if (header < 0) {
            throw new IOException("Connexion fermée avant CONNACK");
        }
        int type = (header >> 4) & 0x0F;
        int remaining = readRemainingLength(in);
        byte[] body = readExactly(in, remaining);
        if (type != TYPE_CONNACK) {
            throw new IOException("Paquet inattendu, CONNACK requis, reçu type " + type);
        }
        int returnCode = body.length >= 2 ? (body[1] & 0xFF) : -1;
        if (returnCode != 0) {
            throw new IOException("CONNACK refusé, code=" + returnCode);
        }
    }

    private void readLoop(MqttListener listener) {
        try {
            while (running.get()) {
                int header = in.read();
                if (header < 0) {
                    throw new IOException("Flux fermé par le serveur");
                }
                int type = (header >> 4) & 0x0F;
                int remaining = readRemainingLength(in);
                byte[] body = readExactly(in, remaining);
                if (type == TYPE_PINGRESP) {
                    // rien à faire, la temporisation est réinitialisée par lastWriteAt
                    continue;
                }
                // Aucun autre type de paquet n'est attendu côté publication seule.
                if (body.length == 0 && type == 0) {
                    break;
                }
            }
        } catch (IOException e) {
            if (running.getAndSet(false)) {
                listener.onDisconnected(e);
            }
            return;
        }
        if (running.getAndSet(false)) {
            listener.onDisconnected(null);
        }
    }

    private void keepAliveLoop() {
        try {
            while (running.get()) {
                Thread.sleep(1000);
                long idleMs = System.currentTimeMillis() - lastWriteAt;
                if (idleMs >= (long) keepAliveSeconds * 1000L - 2000L) {
                    writeFrame(TYPE_PINGREQ, (byte) 0x00, new byte[0]);
                }
            }
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        } catch (IOException e) {
            close();
        }
    }

    private void writeFrame(byte type, byte flags, byte[] variableAndPayload) throws IOException {
        synchronized (writeLock) {
            if (out == null) {
                throw new IOException("Client MQTT non connecté");
            }
            out.write(((type & 0x0F) << 4) | (flags & 0x0F));
            out.write(encodeRemainingLength(variableAndPayload.length));
            out.write(variableAndPayload);
            out.flush();
            lastWriteAt = System.currentTimeMillis();
        }
    }

    static byte[] encodeRemainingLength(int length) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        int value = length;
        do {
            int digit = value % 128;
            value /= 128;
            if (value > 0) {
                digit |= 0x80;
            }
            bytes.write(digit);
        } while (value > 0);
        return bytes.toByteArray();
    }

    static int readRemainingLength(InputStream in) throws IOException {
        int multiplier = 1;
        int value = 0;
        int digit;
        int iterations = 0;
        do {
            digit = in.read();
            if (digit < 0) {
                throw new IOException("Flux fermé pendant la lecture de la longueur");
            }
            value += (digit & 0x7F) * multiplier;
            multiplier *= 128;
            iterations++;
            if (iterations > 4) {
                throw new IOException("Longueur restante MQTT invalide");
            }
        } while ((digit & 0x80) != 0);
        return value;
    }

    private static byte[] readExactly(InputStream in, int length) throws IOException {
        byte[] buffer = new byte[length];
        int read = 0;
        while (read < length) {
            int n = in.read(buffer, read, length - read);
            if (n < 0) {
                throw new IOException("Flux fermé, attendu " + length + " octets, reçu " + read);
            }
            read += n;
        }
        return buffer;
    }

    private static void writeUtf8(ByteArrayOutputStream stream, String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        stream.write((bytes.length >> 8) & 0xFF);
        stream.write(bytes.length & 0xFF);
        stream.write(bytes, 0, bytes.length);
    }
}
