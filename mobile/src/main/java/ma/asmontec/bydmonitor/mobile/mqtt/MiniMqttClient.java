package ma.asmontec.bydmonitor.mobile.mqtt;

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
import java.util.concurrent.atomic.AtomicInteger;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;

/**
 * Client MQTT 3.1.1 minimal, écrit sans bibliothèque externe : CONNECT,
 * SUBSCRIBE (QoS 0), réception PUBLISH, PINGREQ, DISCONNECT. Utilisé par
 * l'application mobile pour s'abonner en lecture seule au préfixe du
 * véhicule ; aucune publication n'est effectuée par ce client.
 */
public final class MiniMqttClient {

    private static final byte TYPE_CONNECT = 1;
    private static final byte TYPE_CONNACK = 2;
    private static final byte TYPE_PUBLISH = 3;
    private static final byte TYPE_SUBSCRIBE = 8;
    private static final byte TYPE_SUBACK = 9;
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
    private final AtomicInteger packetIdCounter = new AtomicInteger(1);

    private Thread readerThread;
    private Thread keepAliveThread;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private volatile long lastWriteAt;
    private int keepAliveSeconds = 30;

    public MiniMqttClient(String host, int port, int connectTimeoutMs, String certSha256Pin) {
        this.host = host;
        this.port = port;
        this.connectTimeoutMs = connectTimeoutMs;
        this.certSha256Pin = certSha256Pin;
    }

    public void connect(String clientId, String username, char[] password, int keepAliveSeconds,
                         MqttListener listener) throws IOException {
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
        socket.setEnabledProtocols(intersectPreferred(socket.getSupportedProtocols(), "TLSv1.3", "TLSv1.2"));

        socket.setUseClientMode(true);
        socket.startHandshake();

        // setEndpointIdentificationAlgorithm() nécessite l'API 24 ; minSdk est 23,
        // donc la vérification du nom d'hôte est faite manuellement après la
        // poignée de main, avec le vérificateur HTTPS standard d'Android.
        if (!HttpsURLConnection.getDefaultHostnameVerifier().verify(host, socket.getSession())) {
            throw new IOException("Le nom d'hôte ne correspond pas au certificat présenté (" + host + ")");
        }

        if (certSha256Pin != null && !certSha256Pin.trim().isEmpty()) {
            verifyPin(socket, certSha256Pin.trim());
        }

        out = socket.getOutputStream();
        in = socket.getInputStream();

        sendConnect(clientId, username, password, keepAliveSeconds);
        readConnAck();

        running.set(true);
        lastWriteAt = System.currentTimeMillis();

        readerThread = new Thread(() -> readLoop(listener), "mqtt-mobile-reader");
        readerThread.setDaemon(true);
        readerThread.start();

        keepAliveThread = new Thread(this::keepAliveLoop, "mqtt-mobile-keepalive");
        keepAliveThread.setDaemon(true);
        keepAliveThread.start();

        listener.onConnected();
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
        return result.isEmpty() ? supported : result.toArray(new String[0]);
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

    public void subscribe(String topicFilter) throws IOException {
        int packetId = packetIdCounter.getAndIncrement() & 0xFFFF;
        if (packetId == 0) {
            packetId = packetIdCounter.getAndIncrement();
        }
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        body.write((packetId >> 8) & 0xFF);
        body.write(packetId & 0xFF);
        writeUtf8(body, topicFilter);
        body.write(0x00); // QoS demandé 0

        writeFrame(TYPE_SUBSCRIBE, (byte) 0x02, body.toByteArray());
    }

    public void disconnectGracefully() {
        try {
            writeFrame(TYPE_DISCONNECT, (byte) 0x00, new byte[0]);
        } catch (IOException ignored) {
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

    private void sendConnect(String clientId, String username, char[] password, int keepAliveSeconds) throws IOException {
        ByteArrayOutputStream variable = new ByteArrayOutputStream();
        writeUtf8(variable, "MQTT");
        variable.write(4);

        int connectFlags = 0x02;
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

                if (type == TYPE_PUBLISH) {
                    dispatchPublish(body, listener);
                } else if (type == TYPE_PINGRESP || type == TYPE_SUBACK) {
                    // aucune action nécessaire pour ce client minimal
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

    private void dispatchPublish(byte[] body, MqttListener listener) {
        if (body.length < 2) {
            return;
        }
        int topicLen = ((body[0] & 0xFF) << 8) | (body[1] & 0xFF);
        if (2 + topicLen > body.length) {
            return;
        }
        String topic = new String(body, 2, topicLen, StandardCharsets.UTF_8);
        int payloadOffset = 2 + topicLen;
        byte[] payload = new byte[body.length - payloadOffset];
        System.arraycopy(body, payloadOffset, payload, 0, payload.length);
        listener.onMessage(topic, payload);
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
