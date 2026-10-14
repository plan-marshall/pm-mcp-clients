/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.mock.http;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.channels.Channels;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Minimal HTTP/1.1 and Server-Sent Events server over JDK {@link ServerSocketChannel}. Supports
 * both Unix domain sockets and loopback TCP addresses.
 */
public final class MockHttpServer implements AutoCloseable {

    private final ServerSocketChannel server;
    private final MockHttpHandler handler;
    private final Path unixSocketPath;
    private final List<MockHttpRequest> requests = new CopyOnWriteArrayList<>();
    private final BlockingQueue<MockHttpRequest> arrivals = new LinkedBlockingQueue<>();
    private final Thread acceptor;
    private volatile boolean closed;

    private MockHttpServer(ServerSocketChannel server, MockHttpHandler handler, Path unixSocketPath) {
        this.server = server;
        this.handler = handler;
        this.unixSocketPath = unixSocketPath;
        this.acceptor = Thread.ofPlatform().daemon().name("mock-http-acceptor-" + server).start(this::acceptLoop);
    }

    /**
     * Starts an HTTP server on a Unix domain socket.
     *
     * @param socketPath the path of the Unix domain socket
     * @param handler    the request handler
     * @return the running server
     * @throws IOException on socket bind failure
     */
    public static MockHttpServer startUnix(Path socketPath, MockHttpHandler handler) throws IOException {
        Objects.requireNonNull(socketPath, "socketPath");
        Objects.requireNonNull(handler, "handler");
        Files.deleteIfExists(socketPath);
        var server = ServerSocketChannel.open(StandardProtocolFamily.UNIX);
        try {
            server.bind(UnixDomainSocketAddress.of(socketPath));
            return new MockHttpServer(server, handler, socketPath);
        } catch (IOException | RuntimeException e) {
            server.close();
            throw e;
        }
    }

    /**
     * Starts an HTTP server on a loopback TCP port.
     *
     * @param port    the TCP port (0 to let the OS assign an ephemeral port)
     * @param handler the request handler
     * @return the running server
     * @throws IOException on socket bind failure
     */
    public static MockHttpServer startTcp(int port, MockHttpHandler handler) throws IOException {
        Objects.requireNonNull(handler, "handler");
        var server = ServerSocketChannel.open();
        try {
            server.bind(new InetSocketAddress("127.0.0.1", port));
            return new MockHttpServer(server, handler, null);
        } catch (IOException | RuntimeException e) {
            server.close();
            throw e;
        }
    }

    /**
     * @return the local socket address
     */
    public SocketAddress localAddress() {
        try {
            return server.getLocalAddress();
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * @return the bound TCP port if listening on TCP, or -1 if listening on a Unix domain socket
     */
    public int port() {
        var addr = localAddress();
        return addr instanceof InetSocketAddress inet ? inet.getPort() : -1;
    }

    /**
     * @return the list of all requests received so far in arrival order
     */
    public List<MockHttpRequest> requests() {
        return List.copyOf(requests);
    }

    /**
     * Waits for the next request to arrive.
     *
     * @param timeout the maximum duration to wait
     * @return the next request, or {@code null} if timed out
     * @throws InterruptedException if interrupted while waiting
     */
    public MockHttpRequest awaitRequest(Duration timeout) throws InterruptedException {
        return arrivals.poll(timeout.toMillis(), TimeUnit.MILLISECONDS);
    }

    private void acceptLoop() {
        while (!closed && server.isOpen()) {
            try {
                var channel = server.accept();
                Thread.ofVirtual().name("mock-http-worker").start(() -> serve(channel));
            } catch (IOException e) {
                if (closed || !server.isOpen()) {
                    return;
                }
            }
        }
    }

    private void serve(SocketChannel channel) {
        try (channel;
             var in = new BufferedInputStream(Channels.newInputStream(channel));
             var out = Channels.newOutputStream(channel)) {
            var request = readRequest(in);
            if (request == null) {
                return;
            }
            requests.add(request);
            arrivals.add(request);
            var response = new MockHttpResponse(out);
            invokeHandler(request, response);
        } catch (IOException e) {
            // Client closed connection
        }
    }

    private void invokeHandler(MockHttpRequest request, MockHttpResponse response) throws IOException {
        try {
            handler.handle(request, response);
            if (!response.isCommitted()) {
                response.send(500, "application/json", "{\"error\":\"unhandled_request\"}");
            }
            // cui-rewrite:disable InvalidExceptionUsageRecipe
        } catch (IOException | RuntimeException e) {
            if (!response.isCommitted()) {
                response.send(500, "application/json", "{\"error\":\"internal_error\",\"message\":\"" + escapeJson(e.getMessage()) + "\"}");
            }
        }
    }

    private static MockHttpRequest readRequest(InputStream in) throws IOException {
        var requestLine = readLine(in);
        if (requestLine == null || requestLine.isBlank()) {
            return null;
        }
        var receivedNanos = System.nanoTime();
        var parts = requestLine.split(" ");
        if (parts.length < 2) {
            return null;
        }
        var method = parts[0];
        var target = parts[1];

        var headers = new LinkedHashMap<String, String>();
        String line;
        while ((line = readLine(in)) != null && !line.isEmpty()) {
            var colon = line.indexOf(':');
            if (colon > 0) {
                var name = line.substring(0, colon).trim().toLowerCase(Locale.ROOT);
                var value = line.substring(colon + 1).trim();
                headers.put(name, value);
            }
        }

        var length = 0;
        var lengthHeader = headers.get("content-length");
        if (lengthHeader != null) {
            try {
                length = Integer.parseInt(lengthHeader);
            } catch (NumberFormatException ignored) {
                // Ignore malformed content-length header and treat as 0
                length = 0;
            }
        }

        var body = length > 0 ? in.readNBytes(length) : new byte[0];
        return new MockHttpRequest(method, target, headers, body, receivedNanos);
    }

    private static String readLine(InputStream in) throws IOException {
        var bytes = new ByteArrayOutputStream();
        int b;
        while ((b = in.read()) >= 0 && b != '\n') {
            bytes.write(b);
        }
        if (b < 0 && bytes.size() == 0) {
            return null;
        }
        var text = bytes.toString(StandardCharsets.UTF_8);
        return text.endsWith("\r") ? text.substring(0, text.length() - 1) : text;
    }

    private static String escapeJson(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("\"", "\\\"").replace("\n", "\\n");
    }

    @Override
    public synchronized void close() throws IOException {
        if (!closed) {
            closed = true;
            try {
                server.close();
            } finally {
                acceptor.interrupt();
                if (unixSocketPath != null) {
                    Files.deleteIfExists(unixSocketPath);
                }
            }
        }
    }
}
