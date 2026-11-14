/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.entry;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import java.io.IOException;
import java.io.StringWriter;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import de.cuioss.test.generator.Generators;
import de.cuioss.test.generator.junit.EnableGeneratorController;
import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

@DisplayName("Entry record contracts")
@EnableGeneratorController
class EntryRecordsTest {

    private static final JsonFactory FACTORY = new JsonFactory();
    private static final Instant PAIRED_AT = Instant.parse("2026-05-12T10:00:00Z");
    private static final Instant USED_AT = Instant.parse("2026-10-16T14:02:00Z");

    @Nested
    @DisplayName("RuntimeEntryResource read from the legacy shape")
    class RuntimeEntryLegacyShape {

        @Test
        @DisplayName("should derive both listeners, the host, and onboarding from the legacy members")
        void shouldDeriveFromLegacyMembers() throws Exception {
            var port = Generators.integers(1024, 65535).next();
            var lanPort = Generators.integers(1024, 65535).next();
            var fingerprint = Generators.nonBlankStrings().next();
            var host = Generators.nonBlankStrings().next();
            var paired = Generators.integers(0, 20).next();
            var json = JsonTree.write(Map.of(
                    "web", Map.of("enabled", true, "port", port, "lan", true, "lan_port", lanPort,
                            "lan_fingerprint", fingerprint, "paired_devices_count", paired),
                    "machine", Map.of("host", host, "onboarded", true)));

            var entry = RuntimeEntryResource.fromJson(json);

            assertEquals(List.of(
                            new ListenerResource(ListenerKind.LOOPBACK, ListenerState.OPEN, port,
                                    "http://127.0.0.1:" + port, null),
                            new ListenerResource(ListenerKind.LAN, ListenerState.OPEN, lanPort, null, fingerprint)),
                    entry.listeners());
            assertEquals(host, entry.host());
            assertTrue(entry.onboarded());
            assertEquals(OnboardingProgress.complete(), entry.onboarding());
            assertEquals(paired, entry.pairedDevicesCount());
        }

        @Test
        @DisplayName("should give a closed LAN listener no fingerprint and an open loopback without port no URL")
        void shouldKeepClosedListenerWithoutFingerprint() throws Exception {
            var fingerprint = Generators.nonBlankStrings().next();
            var json = JsonTree.write(Map.of("web", Map.of("open", true, "lan", false,
                    "lan_fingerprint", fingerprint)));

            var listeners = RuntimeEntryResource.fromJson(json).listeners();

            assertEquals(List.of(
                    new ListenerResource(ListenerKind.LOOPBACK, ListenerState.OPEN, null, null, null),
                    new ListenerResource(ListenerKind.LAN, ListenerState.CLOSED, null, null, null)), listeners);
        }

        @Test
        @DisplayName("should refuse a legacy port out of range as a malformed member")
        void shouldRefuseLegacyPortOutOfRange() {
            var port = Generators.integers(65536, 1_000_000).next();
            var json = JsonTree.write(Map.of("web", Map.of("open", true, "port", port)));

            assertThrows(JsonFormatException.class, () -> RuntimeEntryResource.fromJson(json));
        }

        @Test
        @DisplayName("should read an empty status as not onboarded, without listeners, host, or devices")
        void shouldReadEmptyStatus() throws Exception {
            var entry = RuntimeEntryResource.fromJson("{}");

            assertTrue(entry.listeners().isEmpty());
            assertNull(entry.host());
            assertFalse(entry.onboarded());
            assertEquals(OnboardingProgress.notStarted(), entry.onboarding());
            assertEquals(0, entry.pairedDevicesCount());
            assertEquals(Set.of("onboarded", "onboard", "listeners", "paired_devices_count"),
                    entry.toMap().keySet());
        }

        @Test
        @DisplayName("should take onboarding as completed from the onboard object's completed flag or state")
        void shouldReadOnboardedFromOnboardObject() throws Exception {
            var byFlag = RuntimeEntryResource.fromMap(Map.of("onboard",
                    Map.of("completed", true)));
            var byState = RuntimeEntryResource.fromMap(Map.of("onboard",
                    Map.of("state", "onboard.completed")));
            var running = RuntimeEntryResource.fromMap(Map.of("onboard",
                    Map.of("state", Generators.nonBlankStrings().next()), "machine",
                    Map.of("onboarded", false)));

            assertTrue(byFlag.onboarded());
            assertTrue(byState.onboarded());
            assertFalse(running.onboarded());
        }

        @Test
        @DisplayName("should take a negative device count of the status as none")
        void shouldReadNegativeDeviceCountAsNone() throws Exception {
            var count = Generators.integers(-100, -1).next();

            var entry = RuntimeEntryResource.fromMap(Map.of("paired_devices_count", count));

            assertEquals(0, entry.pairedDevicesCount());
        }

        @Test
        @DisplayName("should refuse listeners that are no array of objects and a status that is no object")
        void shouldRefuseMalformedShapes() throws Exception {
            var text = Generators.nonBlankStrings().next();
            var notArray = Map.<String, Object>of("listeners", text);
            var notObjects = Map.<String, Object>of("listeners", List.of(text));

            assertThrows(JsonFormatException.class, () -> RuntimeEntryResource.fromMap(notArray));
            assertThrows(JsonFormatException.class, () -> RuntimeEntryResource.fromMap(notObjects));
            assertThrows(JsonFormatException.class, () -> RuntimeEntryResource.fromJson("[]"));
            try (JsonParser parser = FACTORY.createParser("[]")) {
                assertThrows(JsonFormatException.class, () -> RuntimeEntryResource.readFrom(parser));
            }
        }

        @Test
        @DisplayName("should replace absent listeners of a constructed entry and refuse a negative device count")
        void shouldValidateConstructedEntry() {
            var count = Generators.integers(-100, -1).next();
            var progress = OnboardingProgress.notStarted();

            var entry = new RuntimeEntryResource(null, null, null, null, false, progress, null, 0);

            assertTrue(entry.listeners().isEmpty());
            assertThrows(IllegalArgumentException.class,
                    () -> new RuntimeEntryResource(null, null, null, null, false, progress, null, count));
        }
    }

    @Nested
    @DisplayName("ListenerKind")
    class ListenerKindTest {

        @Test
        @DisplayName("is the closed list of the two listeners and resolves its wire values")
        void closedListAndWireValues() {
            var wireValues = Stream.of(ListenerKind.values())
                    .map(ListenerKind::wireValue)
                    .collect(Collectors.toCollection(TreeSet::new));

            assertEquals(Set.of("loopback", "lan"), wireValues);
            assertEquals(ListenerKind.LOOPBACK, ListenerKind.fromWire("loopback"));
            assertEquals(ListenerKind.LAN, ListenerKind.fromWire("LAN"));
            assertNull(ListenerKind.fromWireOrNull("bogus"));
        }

        @Test
        @DisplayName("refuses a value that names no listener")
        void refusesUnknownValue() {
            assertThrows(IllegalArgumentException.class, () -> ListenerKind.fromWire("bogus"));
        }
    }

    @Nested
    @DisplayName("PairingCode")
    class PairingCodeTest {

        @Test
        @DisplayName("normalizes an operator's input and shows it with the dash")
        void normalizesAndDisplays() {
            var code = new PairingCode("kxqb-mrtn");

            assertEquals("KXQBMRTN", code.value());
            assertEquals("KXQB-MRTN", code.display());
            assertTrue(code.matches("kxqb mrtn"));
            assertTrue(code.matches("KXQB-MRTN"));
        }

        @Test
        @DisplayName("refuses a code outside the alphabet or of the wrong length")
        void refusesInvalidCodes() {
            assertThrows(IllegalArgumentException.class, () -> new PairingCode("KXQB-MRT"));
            assertThrows(IllegalArgumentException.class, () -> new PairingCode("KXQB-MRTNA"));
            assertThrows(IllegalArgumentException.class, () -> new PairingCode("KXQB-MRTO"));
            assertThrows(IllegalArgumentException.class, () -> new PairingCode("KXQB-MRT0"));
            assertFalse(PairingCode.isValid("KXQB-MRTO"));
            assertTrue(PairingCode.isValid("bcdf-ghjk"));
        }

        @Test
        @DisplayName("generates a valid code")
        void generatesAValidCode() {
            var code = PairingCode.generate();

            assertTrue(PairingCode.isValid(code.display()));
        }
    }

    @Test
    @DisplayName("PairingToken round-trips and keeps its expiry")
    void pairingTokenRoundTrips() throws Exception {
        var expiresInSeconds = Generators.integers(1, 86_400).next();
        var token = new PairingToken("KXQB-MRTN", "http://127.0.0.1:7420/pair#code=KXQB-MRTN",
                expiresInSeconds);

        var roundTrip = PairingToken.fromJson(token.toJson());

        assertEquals(token, roundTrip);
        assertEquals(token, streamingRoundTrip(token, PairingToken::writeTo, PairingToken::readFrom));
    }

    @Test
    @DisplayName("PairingToken defaults the expiry when it is absent")
    void pairingTokenExpiryDefault() throws Exception {
        var parsed = PairingToken.fromJson("{\"code\":\"KXQB-MRTN\",\"url\":\"http://127.0.0.1:7420/pair#code=KXQB-MRTN\"}");

        assertEquals(PairingToken.DEFAULT_LIFETIME_SECONDS, parsed.expiresInSeconds());
    }

    @Test
    @DisplayName("PairingResource round-trips the code, URL and lifetime")
    void pairingResourceRoundTrips() throws Exception {
        var resource = PairingResource.of(new PairingCode("KXQBMRTN"),
                "http://127.0.0.1:7420/pair#code=KXQB-MRTN");

        assertEquals(resource, PairingResource.fromJson(resource.toJson()));
        assertEquals(resource, streamingRoundTrip(resource, PairingResource::writeTo, PairingResource::readFrom));
        assertEquals("KXQB-MRTN", resource.toMap().get("code"));
    }

    @Test
    @DisplayName("PairingRedemptionRequest round-trips and omits an absent label")
    void pairingRedemptionRequestRoundTrips() throws Exception {
        var label = Generators.nonBlankStrings().next();
        var full = new PairingRedemptionRequest("KXQB-MRTN", label, "web");

        assertEquals(full, PairingRedemptionRequest.fromJson(full.toJson()));
        assertEquals(full, streamingRoundTrip(full,
                PairingRedemptionRequest::writeTo, PairingRedemptionRequest::readFrom));

        var minimal = new PairingRedemptionRequest("KXQB-MRTN", null, null);
        assertFalse(minimal.toMap().containsKey("label"));
        var parsed = PairingRedemptionRequest.fromJson(minimal.toJson());
        assertNull(parsed.label());
        assertNull(parsed.clientType());
    }

    @Test
    @DisplayName("PairingRedemptionResponse round-trips with its paired_from origin")
    void pairingRedemptionResponseRoundTrips() throws Exception {
        var label = Generators.nonBlankStrings().next();
        var response = new PairingRedemptionResponse("pmw_secret", "dev-1", label, "lan");

        assertEquals(response, PairingRedemptionResponse.fromJson(response.toJson()));
    }

    @Test
    @DisplayName("LanListenerConfig round-trips with its fingerprint")
    void lanListenerConfigRoundTrips() throws Exception {
        var config = new LanListenerConfig(true, 7421, "mbp-oliver.local", "4F:1A:9C");

        assertEquals(config, LanListenerConfig.fromJson(config.toJson()));
        assertEquals(config, streamingRoundTrip(config,
                LanListenerConfig::writeTo, LanListenerConfig::readFrom));
    }

    @Test
    @DisplayName("ConnectionStatus round-trips with both listeners and the device count")
    void connectionStatusRoundTrips() throws Exception {
        var pairedDevicesCount = Generators.integers(0, 10).next();
        var status = new ConnectionStatus("http://127.0.0.1:7420", true, true,
                "https://mbp-oliver.local:7421", "4F:1A:9C", pairedDevicesCount, true);

        assertEquals(status, ConnectionStatus.fromJson(status.toJson()));
        assertEquals(status, streamingRoundTrip(status,
                ConnectionStatus::writeTo, ConnectionStatus::readFrom));
    }

    @Test
    @DisplayName("DevicePairedEvent round-trips with its paired_from origin")
    void devicePairedEventRoundTrips() throws Exception {
        var label = Generators.nonBlankStrings().next();
        var event = new DevicePairedEvent("dev-1", label, "loopback");

        assertEquals("device.paired", DevicePairedEvent.EVENT_NAME);
        assertEquals(event, DevicePairedEvent.fromJson(event.toJson()));
        assertEquals(event, streamingRoundTrip(event,
                DevicePairedEvent::writeTo, DevicePairedEvent::readFrom));
    }

    @Test
    @DisplayName("DeviceRenamedEvent and DeviceRevokedEvent round-trip with their paired_from origin")
    void deviceLifecycleEventsRoundTrip() throws Exception {
        var label = Generators.nonBlankStrings().next();
        var renamed = new DeviceRenamedEvent("dev-2", label, "lan");
        var revoked = new DeviceRevokedEvent("dev-2", label, "lan");

        assertEquals("device.renamed", DeviceRenamedEvent.EVENT_NAME);
        assertEquals("device.revoked", DeviceRevokedEvent.EVENT_NAME);
        assertEquals(renamed, DeviceRenamedEvent.fromJson(renamed.toJson()));
        assertEquals(revoked, DeviceRevokedEvent.fromJson(revoked.toJson()));
        assertEquals(renamed, streamingRoundTrip(renamed,
                DeviceRenamedEvent::writeTo, DeviceRenamedEvent::readFrom));
        assertEquals(revoked, streamingRoundTrip(revoked,
                DeviceRevokedEvent::writeTo, DeviceRevokedEvent::readFrom));
    }

    @Test
    @DisplayName("ListenerChangedEvent round-trips with its listener kind and state")
    void listenerChangedEventRoundTrips() throws Exception {
        var event = new ListenerChangedEvent(ListenerKind.LAN, ListenerState.OPEN, 7421);

        assertEquals("listener.changed", ListenerChangedEvent.EVENT_NAME);
        var roundTrip = ListenerChangedEvent.fromJson(event.toJson());
        assertEquals(event, roundTrip);
        assertEquals("lan", roundTrip.toMap().get("listener"));
        assertEquals("open", roundTrip.toMap().get("state"));
        assertEquals(7421, roundTrip.toMap().get("port"));
        assertTrue(roundTrip.isOpen());
        assertEquals(event, streamingRoundTrip(event,
                ListenerChangedEvent::writeTo, ListenerChangedEvent::readFrom));
    }

    @Test
    @DisplayName("ListenerResource round-trips open and closed listeners")
    void listenerResourceRoundTrips() throws Exception {
        var open = new ListenerResource(ListenerKind.LAN, ListenerState.OPEN, 7421,
                "https://mbp-oliver.local:7421", "4F:1A:9C");
        var closed = new ListenerResource(ListenerKind.LOOPBACK, ListenerState.CLOSED, 7420, null, null);

        assertEquals(open, ListenerResource.fromJson(open.toJson()));
        assertEquals(closed, ListenerResource.fromJson(closed.toJson()));
        assertEquals(open, streamingRoundTrip(open, ListenerResource::writeTo, ListenerResource::readFrom));
        assertTrue(open.isOpen());
        assertFalse(closed.isOpen());
    }

    @Test
    @DisplayName("OnboardingProgress round-trips its state, step and completion")
    void onboardingProgressRoundTrips() throws Exception {
        var step = Generators.integers(0, 6).next();
        var progress = new OnboardingProgress("harness.choose", step, 7, false);

        assertEquals(progress, OnboardingProgress.fromJson(progress.toJson()));
        assertEquals(OnboardingProgress.complete(), OnboardingProgress.fromJson(
                "{\"state\":\"onboard.completed\",\"step\":7,\"steps\":7,\"completed\":true}"));
        assertEquals(OnboardingProgress.DEFAULT_STEPS, OnboardingProgress.notStarted().steps());
    }

    @Test
    @DisplayName("OnboardingProgress refuses a steps value that is no exact integer rather than truncating it")
    void onboardingProgressRefusesATruncatingStepsValue() {
        assertAll(
                () -> assertEquals(OnboardingProgress.DEFAULT_STEPS,
                        OnboardingProgress.fromJson(
                                "{\"state\":\"harness.choose\",\"step\":1,\"steps\":3.9}")
                                .steps()),
                () -> assertEquals(OnboardingProgress.DEFAULT_STEPS,
                        OnboardingProgress.fromJson(
                                "{\"state\":\"harness.choose\",\"step\":1,\"steps\":\"12\"}")
                                .steps()),
                () -> assertEquals(OnboardingProgress.DEFAULT_STEPS,
                        OnboardingProgress.fromJson(
                                "{\"state\":\"harness.choose\",\"step\":1,\"steps\":1e1}")
                                .steps()),
                () -> assertEquals(9, OnboardingProgress.fromJson(
                        "{\"state\":\"harness.choose\",\"step\":1,\"steps\":9}").steps()));
    }

    @Test
    @DisplayName("RuntimeEntryResource round-trips the status the entry reads")
    void runtimeEntryResourceRoundTrips() throws Exception {
        var resource = new RuntimeEntryResource("0.9.0", "ready", "running", "mbp-oliver", false,
                new OnboardingProgress("harness.choose", 2, 7, false),
                List.of(
                        new ListenerResource(ListenerKind.LOOPBACK, ListenerState.OPEN, 7420,
                                "http://127.0.0.1:7420", null),
                        new ListenerResource(ListenerKind.LAN, ListenerState.CLOSED, 7421, null, null)),
                1);

        assertEquals(resource, RuntimeEntryResource.fromJson(resource.toJson()));
        assertEquals(resource, streamingRoundTrip(resource,
                RuntimeEntryResource::writeTo, RuntimeEntryResource::readFrom));
    }

    @Test
    @DisplayName("RuntimeEntryResource derives the listeners from the legacy web object")
    void runtimeEntryResourceDerivesListeners() throws Exception {
        var resource = RuntimeEntryResource.fromJson("""
                {"status":"ready","machine":"mbp-oliver","onboarded":true,
                 "web":{"enabled":true,"lan":true,"port":7420,"lan_port":7421,
                        "lan_fingerprint":"4F:1A:9C"},"paired_devices_count":2}""");

        assertEquals("mbp-oliver", resource.host());
        assertTrue(resource.onboarded());
        assertEquals(OnboardingProgress.complete(), resource.onboarding());
        assertEquals(2, resource.pairedDevicesCount());
        assertEquals(2, resource.listeners().size());
        assertEquals(ListenerKind.LOOPBACK, resource.listeners().get(0).listener());
        assertEquals(ListenerState.OPEN, resource.listeners().get(0).state());
        assertEquals(7420, resource.listeners().get(0).port());
        assertEquals(ListenerKind.LAN, resource.listeners().get(1).listener());
        assertEquals(ListenerState.OPEN, resource.listeners().get(1).state());
        assertEquals(7421, resource.listeners().get(1).port());
        assertEquals("4F:1A:9C", resource.listeners().get(1).fingerprint());
    }

    @ParameterizedTest(name = "the {0} of the legacy web object")
    @ValueSource(strings = {"port", "lan_port"})
    @DisplayName("RuntimeEntryResource refuses a legacy port beyond the range, a fraction, a text, and one that wraps to a valid port")
    void runtimeEntryResourceRefusesAMalformedLegacyPort(String member) {
        var wrapsToAValidPort = (1L << 32) + Generators.integers(1024, 65_535).next();
        var beyond = 65_536 + Generators.integers(0, 1_000).next();
        var fraction = Generators.integers(1024, 65_535).next() + 0.5;
        var text = Generators.nonBlankStrings().next();

        assertAll(
                () -> assertThrows(JsonFormatException.class, () -> legacyWeb(member, wrapsToAValidPort)),
                () -> assertThrows(JsonFormatException.class, () -> legacyWeb(member, beyond)),
                () -> assertThrows(JsonFormatException.class, () -> legacyWeb(member, fraction)),
                () -> assertThrows(JsonFormatException.class, () -> legacyWeb(member, text)));
    }

    private static RuntimeEntryResource legacyWeb(String member, Object value) throws IOException {
        var web = new LinkedHashMap<String, Object>(Map.of("enabled", true, "lan", true, "port", 7420,
                "lan_port", 7421));
        web.put(member, value);
        return RuntimeEntryResource.fromJson(JsonTree.write(Map.of("status", "ready", "web", web)));
    }

    @Test
    @DisplayName("PairedDeviceResource round-trips every field the list carries")
    void pairedDeviceResourceRoundTrips() throws Exception {
        var device = new PairedDeviceResource("dev-1", "Firefox on mbp-oliver",
                PAIRED_AT, USED_AT, "open", "loopback");

        assertEquals(device, PairedDeviceResource.fromJson(device.toJson()));
        assertEquals(device, streamingRoundTrip(device,
                PairedDeviceResource::writeTo, PairedDeviceResource::readFrom));
        assertEquals("pair", PairedDeviceResource
                .fromJson("{\"device_id\":\"dev-2\",\"label\":\"iPad\"}").pairedBy());
        assertEquals("loopback", PairedDeviceResource
                .fromJson("{\"device_id\":\"dev-2\",\"label\":\"iPad\"}").pairedFrom());
    }

    @Test
    @DisplayName("DeviceListResource reads the wire array and the object shape")
    void deviceListResourceRoundTrips() throws Exception {
        var firstLabel = Generators.nonBlankStrings().next();
        var secondLabel = Generators.nonBlankStrings().next();
        var devices = new DeviceListResource(List.of(
                new PairedDeviceResource("dev-1", firstLabel, PAIRED_AT, USED_AT, "pair", "loopback"),
                new PairedDeviceResource("dev-2", secondLabel, PAIRED_AT, USED_AT, "entry", "lan")));

        assertTrue(devices.toJson().startsWith("["));
        assertEquals(devices, DeviceListResource.fromJson(devices.toJson()));
        assertEquals(devices, DeviceListResource.fromTree(devices.toMap()));
        assertEquals(devices, streamingRoundTrip(devices, DeviceListResource::writeTo, DeviceListResource::readFrom));
        assertEquals(2, devices.size());
        assertEquals(0, DeviceListResource.fromJson("[]").size());
    }

    @Test
    @DisplayName("WebCredentialResource round-trips the entry status without a secret")
    void webCredentialResourceRoundTrips() throws Exception {
        var resource = new WebCredentialResource("github", WebCredentialResource.Status.PRESENT,
                WebCredentialResource.Store.KEYCHAIN, true);

        assertEquals(resource, WebCredentialResource.fromJson(resource.toJson()));
        assertEquals(resource, streamingRoundTrip(resource,
                WebCredentialResource::writeTo, WebCredentialResource::readFrom));
        assertEquals(WebCredentialResource.Status.STORED,
                WebCredentialResource.stored("github").status());
        assertFalse(resource.toMap().containsKey("secret"));
        assertTrue(WebCredentialResource.fromJson(
                "{\"credential_key\":\"github\",\"status\":\"not_found\"}").loopbackOnly());
    }

    @Nested
    @DisplayName("LoopbackHost")
    class LoopbackHostTest {

        @Test
        @DisplayName("accepts the loopback listener's own origin with any port or none")
        void acceptsLoopbackOrigins() {
            assertTrue(LoopbackHost.isLoopback("127.0.0.1"));
            assertTrue(LoopbackHost.isLoopback("127.0.0.1:7420"));
            assertTrue(LoopbackHost.isLoopback("LOCALHOST:8000"));
            assertTrue(LoopbackHost.isLoopback("[::1]"));
            assertTrue(LoopbackHost.isLoopback("[::1]:7420"));
            assertEquals("localhost", LoopbackHost.parse("localhost:7420").orElseThrow().host());
        }

        @Test
        @DisplayName("refuses a foreign host, an IPv6 zone, an IPv4-mapped address, and malformed values")
        void refusesForeignAndMalformedOrigins() {
            assertFalse(LoopbackHost.isLoopback("evil.example.com"));
            assertFalse(LoopbackHost.isLoopback("192.168.1.20:7420"));
            assertFalse(LoopbackHost.isLoopback("[fe80::1%en0]:7420"));
            assertFalse(LoopbackHost.isLoopback("[::ffff:127.0.0.1]"));
            assertFalse(LoopbackHost.isLoopback("localhost:port"));
            assertFalse(LoopbackHost.isLoopback("127.0.0.1:"));
            assertFalse(LoopbackHost.isLoopback(null));
            assertFalse(LoopbackHost.isLoopback("   "));
        }
    }

    @Nested
    @DisplayName("Negative parsing")
    class NegativeParsing {

        @Test
        @DisplayName("refuses a JSON value that is no object")
        void refusesNonObjectJson() {
            assertThrows(JsonFormatException.class, () -> PairingToken.fromJson("[]"));
            assertThrows(JsonFormatException.class, () -> PairingResource.fromJson("[]"));
            assertThrows(JsonFormatException.class, () -> PairingRedemptionRequest.fromJson("[]"));
            assertThrows(JsonFormatException.class, () -> PairingRedemptionResponse.fromJson("[]"));
            assertThrows(JsonFormatException.class, () -> LanListenerConfig.fromJson("[]"));
            assertThrows(JsonFormatException.class, () -> ConnectionStatus.fromJson("[]"));
            assertThrows(JsonFormatException.class, () -> DevicePairedEvent.fromJson("[]"));
            assertThrows(JsonFormatException.class, () -> DeviceRenamedEvent.fromJson("[]"));
            assertThrows(JsonFormatException.class, () -> DeviceRevokedEvent.fromJson("[]"));
            assertThrows(JsonFormatException.class, () -> ListenerChangedEvent.fromJson("[]"));
            assertThrows(JsonFormatException.class, () -> ListenerResource.fromJson("[]"));
            assertThrows(JsonFormatException.class, () -> OnboardingProgress.fromJson("[]"));
            assertThrows(JsonFormatException.class, () -> RuntimeEntryResource.fromJson("[]"));
            assertThrows(JsonFormatException.class, () -> PairedDeviceResource.fromJson("[]"));
            assertThrows(JsonFormatException.class, () -> WebCredentialResource.fromJson("[]"));
        }

        @Test
        @DisplayName("refuses an object without the required fields")
        void refusesMissingFields() {
            assertThrows(JsonFormatException.class, () -> PairingToken.fromJson("{}"));
            assertThrows(JsonFormatException.class, () -> PairingResource.fromJson("{}"));
            assertThrows(JsonFormatException.class, () -> PairingRedemptionRequest.fromJson("{}"));
            assertThrows(JsonFormatException.class, () -> PairingRedemptionResponse.fromJson("{}"));
            assertThrows(JsonFormatException.class, () -> LanListenerConfig.fromJson("{}"));
            assertThrows(JsonFormatException.class, () -> DevicePairedEvent.fromJson("{}"));
            assertThrows(JsonFormatException.class, () -> DeviceRenamedEvent.fromJson("{}"));
            assertThrows(JsonFormatException.class, () -> DeviceRevokedEvent.fromJson("{}"));
            assertThrows(JsonFormatException.class, () -> ListenerChangedEvent.fromJson("{}"));
            assertThrows(JsonFormatException.class, () -> ListenerResource.fromJson("{}"));
            assertThrows(JsonFormatException.class, () -> PairedDeviceResource.fromJson("{}"));
            assertThrows(JsonFormatException.class, () -> WebCredentialResource.fromJson("{}"));
        }

        @Test
        @DisplayName("refuses an unknown listener state, store, or instant")
        void refusesUnknownClosedValues() {
            assertThrows(JsonFormatException.class, () -> ListenerChangedEvent.fromJson(
                    "{\"listener\":\"lan\",\"state\":\"maybe\",\"port\":7421}"));
            assertThrows(JsonFormatException.class, () -> ListenerResource.fromJson(
                    "{\"listener\":\"lan\",\"state\":\"maybe\"}"));
            assertThrows(JsonFormatException.class, () -> WebCredentialResource.fromJson(
                    "{\"credential_key\":\"github\",\"status\":\"present\",\"store\":\"vault\"}"));
            assertThrows(JsonFormatException.class, () -> PairedDeviceResource.fromJson(
                    "{\"device_id\":\"dev-1\",\"label\":\"Firefox\",\"created_at\":\"yesterday\"}"));
        }

        @Test
        @DisplayName("defaults the optional connection fields")
        void defaultsOptionalConnectionFields() throws Exception {
            var status = ConnectionStatus.fromJson("{}");

            assertEquals(0, status.pairedDevicesCount());
            assertFalse(status.loopbackOpen());
            assertFalse(status.lanEnabled());
        }

        @Test
        @DisplayName("defaults the optional runtime entry fields")
        void defaultsOptionalRuntimeEntryFields() throws Exception {
            var entry = RuntimeEntryResource.fromJson("{}");

            assertFalse(entry.onboarded());
            assertEquals(0, entry.listeners().size());
            assertEquals(0, entry.pairedDevicesCount());
            assertNotNull(entry.onboarding());
        }
    }

    private static <T> T streamingRoundTrip(T value, Writer<T> writer, Reader<T> reader) throws IOException {
        var out = new StringWriter();
        try (JsonGenerator generator = FACTORY.createGenerator(out)) {
            writer.write(value, generator);
        }
        var json = out.toString();
        assertFalse(json.isBlank());
        assertEquals(JsonTree.write(JsonTree.parse(json)), json);
        try (JsonParser parser = FACTORY.createParser(json)) {
            return reader.read(parser);
        }
    }

    @FunctionalInterface
    private interface Writer<T> {
        void write(T value, JsonGenerator generator) throws IOException;
    }

    @FunctionalInterface
    private interface Reader<T> {
        T read(JsonParser parser) throws IOException;
    }
}
