/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.operator.transcript;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.channels.Channels;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.function.Predicate;

/**
 * A loopback proxy in front of a runtime's Unix socket that holds the requests one path matches
 * until it is released, and relays every other exchange untouched. A capture test points a console
 * at the proxy's machine root and so proves what the console does while a read is still running,
 * without asking the runtime to be slow.
 *
 * <p>It relays the bytes of each exchange, so an event stream stays a stream. The proxy is test
 * code: it neither reads nor rewrites a body.
 */
final class StallingSocketProxy implements AutoCloseable {

    /** The largest request head the proxy buffers before it gives up on the exchange. */
    private static final int MAX_HEAD = 64 * 1024;

    /** The end of a request head, as it arrives on the wire. */
    private static final int HEAD_END = 0x0D0A0D0A;

    private final ServerSocketChannel server;
    private final Path upstream;
    private final Path listenPath;
    private final Predicate<String> stalled;
    private final CountDownLatch released = new CountDownLatch(1);
    private final Set<SocketChannel> active = ConcurrentHashMap.newKeySet();
    private final Thread acceptor;
    private volatile boolean closed;

    private StallingSocketProxy(ServerSocketChannel server, Path upstream, Path listenPath,
                        Predicate<String> stalled) {
        this.server = server;
        this.upstream = upstream;
        this.listenPath = listenPath;
        this.stalled = stalled;
        this.acceptor = Thread.ofVirtual().name("stalling-proxy-acceptor").start(this::acceptLoop);
    }

    /**
     * @param upstream   the socket of the runtime the exchanges are relayed to
     * @param listenPath the socket the proxy listens on
     * @param stalled    decides, per request target, whether the exchange waits for
     *                   {@link #release()}
     * @return the started proxy
     * @throws IOException if the proxy cannot listen on the path
     */
    static StallingSocketProxy start(Path upstream, Path listenPath, Predicate<String> stalled)
                        throws IOException {
        Objects.requireNonNull(upstream, "upstream");
        Objects.requireNonNull(listenPath, "listenPath");
        Objects.requireNonNull(stalled, "stalled");
        Files.deleteIfExists(listenPath);
        var server = ServerSocketChannel.open(StandardProtocolFamily.UNIX);
        try {
            server.bind(UnixDomainSocketAddress.of(listenPath));
            return new StallingSocketProxy(server, upstream, listenPath, stalled);
        } catch (IOException e) {
            server.close();
            throw e;
        }
    }

    /** Lets every held exchange continue. */
    void release() {
        released.countDown();
    }

    /**
     * Copies a machine root so a console can be pointed at the proxy while the runtime keeps its
     * own, with the sockets of the copy left out.
     *
     * @param source the runtime's machine root
     * @param target the directory the copy is written to
     * @return the copy
     * @throws IOException if the tree cannot be copied
     */
    static Path copyMachineRoot(Path source, Path target) throws IOException {
        try (var paths = Files.walk(source)) {
            for (var path : paths.toList()) {
                var copy = target.resolve(source.relativize(path).toString());
                if (Files.isDirectory(path)) {
                    Files.createDirectories(copy);
                    // The machine root carries the modes the operator refuses to run without: a
                    // directory copied as 0755 is refused before the console draws anything.
                    Files.setPosixFilePermissions(copy, Files.getPosixFilePermissions(path));
                } else if (Files.isRegularFile(path)) {
                    Files.createDirectories(copy.getParent());
                    Files.copy(path, copy);
                    Files.setPosixFilePermissions(copy, Files.getPosixFilePermissions(path));
                }
            }
        }
        return target;
    }

    private void acceptLoop() {
        while (!closed && server.isOpen()) {
            try {
                var client = server.accept();
                Thread.ofVirtual().name("stalling-proxy-exchange").start(() -> serve(client));
            } catch (IOException _) {
                return;
            }
        }
    }

    private void serve(SocketChannel client) {
        active.add(client);
        try (client;
             var fromClient = new BufferedInputStream(Channels.newInputStream(client));
             var toClient = Channels.newOutputStream(client)) {
            var head = readHead(fromClient);
            if (head == null) {
                return;
            }
            if (stalled.test(targetOf(head))) {
                released.await();
            }
            try (var toRuntimeChannel = SocketChannel.open(StandardProtocolFamily.UNIX)) {
                toRuntimeChannel.connect(UnixDomainSocketAddress.of(upstream));
                var toRuntime = Channels.newOutputStream(toRuntimeChannel);
                toRuntime.write(head);
                toRuntime.flush();
                var fromRuntime = new BufferedInputStream(Channels.newInputStream(toRuntimeChannel));
                var requestPump = Thread.ofVirtual().name("stalling-proxy-request").start(() -> {
                    try {
                        fromClient.transferTo(toRuntime);
                        toRuntime.flush();
                    } catch (IOException _) {
                        // The side that stopped writing has ended the exchange.
                    }
                });
                fromRuntime.transferTo(toClient);
                toClient.flush();
                requestPump.interrupt();
            }
        } catch (IOException _) {
            // A client that leaves closes its exchange; the proxy holds no state of its own.
        } catch (InterruptedException _) {
            Thread.currentThread().interrupt();
        } finally {
            active.remove(client);
        }
    }

    private static byte[] readHead(InputStream in) throws IOException {
        var head = new ByteArrayOutputStream();
        var tail = 0;
        int read;
        while ((read = in.read()) >= 0) {
            head.write(read);
            tail = (tail << 8) | (read & 0xFF);
            if (tail == HEAD_END) {
                return head.toByteArray();
            }
            if (head.size() > MAX_HEAD) {
                throw new IOException("the request head is larger than " + MAX_HEAD + " bytes");
            }
        }
        return null;
    }

    private static String targetOf(byte[] head) {
        var requestLine = new String(head, StandardCharsets.UTF_8).split("\r\n", 2)[0];
        var parts = requestLine.split(" ");
        if (parts.length < 2) {
            return "";
        }
        var target = parts[1];
        var query = target.indexOf('?');
        return query < 0 ? target : target.substring(0, query);
    }

    /**
     * @param path the request path a caller stalls on
     * @return whether a request target names that path
     */
    static Predicate<String> isPath(String path) {
        var expected = Objects.requireNonNull(path, "path");
        return expected::equals;
    }

    @Override
    public synchronized void close() throws IOException {
        if (closed) {
            return;
        }
        closed = true;
        released.countDown();
        try {
            server.close();
        } finally {
            acceptor.interrupt();
            // An exchange the console still holds open would outlive the proxy otherwise.
            for (var channel : active) {
                try {
                    channel.close();
                } catch (IOException _) {
                    // A channel that is already gone needs no second close.
                }
            }
            active.clear();
            Files.deleteIfExists(listenPath);
        }
    }
}
