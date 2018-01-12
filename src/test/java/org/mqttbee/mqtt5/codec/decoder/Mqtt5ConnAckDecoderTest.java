package org.mqttbee.mqtt5.codec.decoder;

import com.google.common.collect.ImmutableList;
import io.netty.buffer.ByteBuf;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mqttbee.annotations.Nullable;
import org.mqttbee.api.mqtt5.message.Mqtt5ConnAck;
import org.mqttbee.api.mqtt5.message.Mqtt5Disconnect;
import org.mqttbee.mqtt5.message.Mqtt5MessageType;
import org.mqttbee.mqtt5.message.Mqtt5UTF8String;
import org.mqttbee.mqtt5.message.Mqtt5UserProperty;
import org.mqttbee.mqtt5.message.connack.Mqtt5ConnAckReasonCode;
import org.mqttbee.mqtt5.message.disconnect.Mqtt5DisconnectReasonCode;

import static org.junit.Assert.*;

/**
 * @author Silvio Giebl
 */
public class Mqtt5ConnAckDecoderTest {

    private EmbeddedChannel channel;

    @Before
    public void setUp() {
        channel = new EmbeddedChannel(new Mqtt5Decoder(new Mqtt5ConnAckTestMessageDecoders()));
    }

    @After
    public void tearDown() throws Exception {
        channel.close();
    }

    @Test
    public void test_example() {
        final byte[] encoded = {
                // fixed header
                //   type, flags
                0b0010_0000,
                //   remaining length
                122,
                // variable header
                //   connack flags
                0b0000_0001,
                //   reason code (success)
                0x00,
                //   properties
                119,
                //     session expiry interval
                0x11, 0, 0, 0, 10,
                //     receive maximum
                0x21, 0, 100,
                //     maximum qos
                0x24, 1,
                //     retain available
                0x25, 0,
                //     maximum packet size
                0x27, 0, 0, 0, 100,
                //     assigned client identifier
                0x12, 0, 4, 't', 'e', 's', 't',
                //     topic alias maximum
                0x22, 0, 5,
                //     reason string
                0x1F, 0, 7, 's', 'u', 'c', 'c', 'e', 's', 's',
                //     user properties
                0x26, 0, 4, 't', 'e', 's', 't', 0, 5, 'v', 'a', 'l', 'u', 'e',
                0x26, 0, 4, 't', 'e', 's', 't', 0, 6, 'v', 'a', 'l', 'u', 'e', '2',
                //     wildcard subscription available
                0x28, 0,
                //     subscription identifiers available
                0x29, 1,
                //     shared subscription available
                0x2A, 0,
                //     server keep alive
                0x13, 0, 10,
                //     response information
                0x1A, 0, 8, 'r', 'e', 's', 'p', 'o', 'n', 's', 'e',
                //     server reference
                0x1C, 0, 6, 's', 'e', 'r', 'v', 'e', 'r',
                //     auth method
                0x15, 0, 8, 'G', 'S', '2', '-', 'K', 'R', 'B', '5',
                //     auth data
                0x16, 0, 10, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10
        };

        final ByteBuf byteBuf = channel.alloc().buffer();
        byteBuf.writeBytes(encoded);
        channel.writeInbound(byteBuf);
        final Mqtt5ConnAck connAck = channel.readInbound();

        assertNotNull(connAck);

        assertEquals(Mqtt5ConnAckReasonCode.SUCCESS, connAck.getReasonCode());
        assertTrue(connAck.getSessionExpiryInterval().isPresent());
        assertEquals(10, (long) connAck.getSessionExpiryInterval().get());
        assertTrue(connAck.getAssignedClientIdentifier().isPresent());
        assertEquals("test", connAck.getAssignedClientIdentifier().get().toString());
        assertTrue(connAck.getReasonString().isPresent());
        assertEquals("success", connAck.getReasonString().get().toString());
        assertTrue(connAck.getServerKeepAlive().isPresent());
        assertEquals(10, (long) connAck.getServerKeepAlive().get());
        assertTrue(connAck.getResponseInformation().isPresent());
        assertEquals("response", connAck.getResponseInformation().get().toString());
        assertTrue(connAck.getServerReference().isPresent());
        assertEquals("server", connAck.getServerReference().get().toString());

        final ImmutableList<Mqtt5UserProperty> userProperties = connAck.getUserProperties();
        assertEquals(2, userProperties.size());
        final Mqtt5UTF8String test = Mqtt5UTF8String.from("test");
        final Mqtt5UTF8String value = Mqtt5UTF8String.from("value");
        final Mqtt5UTF8String value2 = Mqtt5UTF8String.from("value2");
        assertNotNull(test);
        assertNotNull(value);
        assertNotNull(value2);
        assertTrue(userProperties.contains(new Mqtt5UserProperty(test, value)));
        assertTrue(userProperties.contains(new Mqtt5UserProperty(test, value2)));

        final Mqtt5ConnAck.Restrictions restrictions = connAck.getRestrictions();
        assertEquals(100, restrictions.getReceiveMaximum());
        assertEquals(1, restrictions.getMaximumQoS());
        assertEquals(false, restrictions.isRetainAvailable());
        assertEquals(100, restrictions.getMaximumPacketSize());
        assertEquals(5, restrictions.getTopicAliasMaximum());
        assertEquals(false, restrictions.isWildcardSubscriptionAvailable());
        assertEquals(true, restrictions.isSubscriptionIdentifierAvailable());
        assertEquals(false, restrictions.isSharedSubscriptionAvailable());

        assertTrue(connAck.getAuth().isPresent());
        final Mqtt5ConnAck.Auth auth = connAck.getAuth().get();
        assertTrue(auth.getMethod().isPresent());
        assertEquals("GS2-KRB5", auth.getMethod().get().toString());
        assertTrue(auth.getData().isPresent());
        assertArrayEquals(new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10}, auth.getData().get());
    }

    @Test
    public void decode_not_enough_bytes() {
        final ByteBuf byteBuf = channel.alloc().buffer();
        // fixed header
        //   type, flags
        byteBuf.writeByte(0b0010_0000);
        //   remaining length
        byteBuf.writeByte(122);
        // variable header
        //   connack flags
        byteBuf.writeByte(0b0000_0001);
        //   reason code (success)
        byteBuf.writeByte(0x00);
        //   properties
        byteBuf.writeByte(PROPERTIES_VALID_LENGTH);
        byteBuf.writeBytes(PROPERTIES_VALID, 0, 10);

        channel.writeInbound(byteBuf);
        final Mqtt5ConnAck connAck = channel.readInbound();

        assertNull(connAck);

        final Mqtt5Disconnect disconnect = channel.readOutbound();

        assertNull(disconnect);
    }

    @Test
    public void decode_wrong_flags() {
        final ByteBuf byteBuf = channel.alloc().buffer();
        // fixed header
        //   type, flags
        byteBuf.writeByte(0b0010_0100);
        //   remaining length
        byteBuf.writeByte(122);
        // variable header
        //   connack flags
        byteBuf.writeByte(0b0000_0001);
        //   reason code (success)
        byteBuf.writeByte(0x00);
        //   properties
        byteBuf.writeByte(PROPERTIES_VALID_LENGTH);
        byteBuf.writeBytes(PROPERTIES_VALID);

        channel.writeInbound(byteBuf);

        testDisconnect(Mqtt5DisconnectReasonCode.MALFORMED_PACKET);
    }

    @Test
    public void decode_remaining_length_too_short() {
        final ByteBuf byteBuf = channel.alloc().buffer();
        // fixed header
        //   type, flags
        byteBuf.writeByte(0b0010_0000);
        //   remaining length
        byteBuf.writeByte(121);
        // variable header
        //   connack flags
        byteBuf.writeByte(0b0000_0001);
        //   reason code (success)
        byteBuf.writeByte(0x00);
        //   properties
        byteBuf.writeByte(PROPERTIES_VALID_LENGTH);
        byteBuf.writeBytes(PROPERTIES_VALID);

        channel.writeInbound(byteBuf);

        testDisconnect(Mqtt5DisconnectReasonCode.MALFORMED_PACKET);
    }

    @Test
    public void decode_remaining_length_too_long() {
        final ByteBuf byteBuf = channel.alloc().buffer();
        // fixed header
        //   type, flags
        byteBuf.writeByte(0b0010_0000);
        //   remaining length
        byteBuf.writeByte(123);
        // variable header
        //   connack flags
        byteBuf.writeByte(0b0000_0001);
        //   reason code (success)
        byteBuf.writeByte(0x00);
        //   properties
        byteBuf.writeByte(PROPERTIES_VALID_LENGTH);
        byteBuf.writeBytes(PROPERTIES_VALID);
        // padding, e.g. next message
        byteBuf.writeByte(0b0010_0000);

        channel.writeInbound(byteBuf);

        testDisconnect(Mqtt5DisconnectReasonCode.MALFORMED_PACKET);
    }

    @Test
    public void decode_wrong_connack_flags() {
        final ByteBuf byteBuf = channel.alloc().buffer();
        // fixed header
        //   type, flags
        byteBuf.writeByte(0b0010_0000);
        //   remaining length
        byteBuf.writeByte(122);
        // variable header
        //   connack flags
        byteBuf.writeByte(0b0000_1001);
        //   reason code (success)
        byteBuf.writeByte(0x00);
        //   properties
        byteBuf.writeByte(PROPERTIES_VALID_LENGTH);
        byteBuf.writeBytes(PROPERTIES_VALID);

        channel.writeInbound(byteBuf);

        testDisconnect(Mqtt5DisconnectReasonCode.MALFORMED_PACKET);
    }

    @Test
    public void decode_wrong_reason_code() {
        final ByteBuf byteBuf = channel.alloc().buffer();
        // fixed header
        //   type, flags
        byteBuf.writeByte(0b0010_0000);
        //   remaining length
        byteBuf.writeByte(122);
        // variable header
        //   connack flags
        byteBuf.writeByte(0b0000_0001);
        //   reason code
        byteBuf.writeByte(0x10);
        //   properties
        byteBuf.writeByte(PROPERTIES_VALID_LENGTH);
        byteBuf.writeBytes(PROPERTIES_VALID);

        channel.writeInbound(byteBuf);

        testDisconnect(Mqtt5DisconnectReasonCode.MALFORMED_PACKET);
    }

    @Test
    public void decode_property_length_too_short() {
        final ByteBuf byteBuf = channel.alloc().buffer();
        // fixed header
        //   type, flags
        byteBuf.writeByte(0b0010_0000);
        //   remaining length
        byteBuf.writeByte(122);
        // variable header
        //   connack flags
        byteBuf.writeByte(0b0000_0001);
        //   reason code (success)
        byteBuf.writeByte(0x00);
        //   properties
        byteBuf.writeByte(PROPERTIES_VALID_LENGTH - 1);
        byteBuf.writeBytes(PROPERTIES_VALID);

        channel.writeInbound(byteBuf);

        testDisconnect(Mqtt5DisconnectReasonCode.MALFORMED_PACKET);
    }

    @Test
    public void decode_property_length_too_long() {
        final ByteBuf byteBuf = channel.alloc().buffer();
        // fixed header
        //   type, flags
        byteBuf.writeByte(0b0010_0000);
        //   remaining length
        byteBuf.writeByte(122);
        // variable header
        //   connack flags
        byteBuf.writeByte(0b0000_0001);
        //   reason code (success)
        byteBuf.writeByte(0x00);
        //   properties
        byteBuf.writeByte(PROPERTIES_VALID_LENGTH + 1);
        byteBuf.writeBytes(PROPERTIES_VALID);

        channel.writeInbound(byteBuf);

        testDisconnect(Mqtt5DisconnectReasonCode.MALFORMED_PACKET);
    }

    @Test
    public void decode_reason_code_not_0_session_present_must_be_0() {
        final ByteBuf byteBuf = channel.alloc().buffer();
        // fixed header
        //   type, flags
        byteBuf.writeByte(0b0010_0000);
        //   remaining length
        byteBuf.writeByte(122);
        // variable header
        //   connack flags
        byteBuf.writeByte(0b0000_0001);
        //   reason code (protocol error)
        byteBuf.writeByte(0x82);
        //   properties
        byteBuf.writeByte(PROPERTIES_VALID_LENGTH);
        byteBuf.writeBytes(PROPERTIES_VALID);

        channel.writeInbound(byteBuf);

        testDisconnect(Mqtt5DisconnectReasonCode.MALFORMED_PACKET);
    }

    @Test
    public void decode_receive_maximum_0() {
        final ByteBuf byteBuf = channel.alloc().buffer();
        // fixed header
        //   type, flags
        byteBuf.writeByte(0b0010_0000);
        //   remaining length
        byteBuf.writeByte(6);
        // variable header
        //   connack flags
        byteBuf.writeByte(0b0000_0001);
        //   reason code (success)
        byteBuf.writeByte(0x00);
        //   properties
        byteBuf.writeByte(3);
        //     receive maximum
        byteBuf.writeBytes(new byte[]{0x21, 0, 0});

        channel.writeInbound(byteBuf);

        testDisconnect(Mqtt5DisconnectReasonCode.PROTOCOL_ERROR);
    }

    @Test
    public void decode_maximum_qos_not_0_or_1() {
        final ByteBuf byteBuf = channel.alloc().buffer();
        // fixed header
        //   type, flags
        byteBuf.writeByte(0b0010_0000);
        //   remaining length
        byteBuf.writeByte(5);
        // variable header
        //   connack flags
        byteBuf.writeByte(0b0000_0001);
        //   reason code (success)
        byteBuf.writeByte(0x00);
        //   properties
        byteBuf.writeByte(2);
        //     receive maximum
        byteBuf.writeBytes(new byte[]{0x24, 2});

        channel.writeInbound(byteBuf);

        testDisconnect(Mqtt5DisconnectReasonCode.PROTOCOL_ERROR);
    }

    @Test
    public void decode_retain_available_not_0_or_1() {
        final ByteBuf byteBuf = channel.alloc().buffer();
        // fixed header
        //   type, flags
        byteBuf.writeByte(0b0010_0000);
        //   remaining length
        byteBuf.writeByte(5);
        // variable header
        //   connack flags
        byteBuf.writeByte(0b0000_0001);
        //   reason code (success)
        byteBuf.writeByte(0x00);
        //   properties
        byteBuf.writeByte(2);
        //     receive maximum
        byteBuf.writeBytes(new byte[]{0x25, 2});

        channel.writeInbound(byteBuf);

        testDisconnect(Mqtt5DisconnectReasonCode.PROTOCOL_ERROR);
    }

    @Test
    public void decode_maximum_packet_size_0() {
        final ByteBuf byteBuf = channel.alloc().buffer();
        // fixed header
        //   type, flags
        byteBuf.writeByte(0b0010_0000);
        //   remaining length
        byteBuf.writeByte(8);
        // variable header
        //   connack flags
        byteBuf.writeByte(0b0000_0001);
        //   reason code (success)
        byteBuf.writeByte(0x00);
        //   properties
        byteBuf.writeByte(5);
        //     receive maximum
        byteBuf.writeBytes(new byte[]{0x27, 0, 0, 0, 0});

        channel.writeInbound(byteBuf);

        testDisconnect(Mqtt5DisconnectReasonCode.PROTOCOL_ERROR);
    }

    @Test
    public void decode_wildcard_subscription_available_not_0_or_1() {
        final ByteBuf byteBuf = channel.alloc().buffer();
        // fixed header
        //   type, flags
        byteBuf.writeByte(0b0010_0000);
        //   remaining length
        byteBuf.writeByte(5);
        // variable header
        //   connack flags
        byteBuf.writeByte(0b0000_0001);
        //   reason code (success)
        byteBuf.writeByte(0x00);
        //   properties
        byteBuf.writeByte(2);
        //     wildcard subscription available
        byteBuf.writeBytes(new byte[]{0x28, 2});

        channel.writeInbound(byteBuf);

        testDisconnect(Mqtt5DisconnectReasonCode.PROTOCOL_ERROR);
    }

    @Test
    public void decode_subscription_identifiers_available_not_0_or_1() {
        final ByteBuf byteBuf = channel.alloc().buffer();
        // fixed header
        //   type, flags
        byteBuf.writeByte(0b0010_0000);
        //   remaining length
        byteBuf.writeByte(5);
        // variable header
        //   connack flags
        byteBuf.writeByte(0b0000_0001);
        //   reason code (success)
        byteBuf.writeByte(0x00);
        //   properties
        byteBuf.writeByte(2);
        //     subscription identifiers available
        byteBuf.writeBytes(new byte[]{0x29, 2});

        channel.writeInbound(byteBuf);

        testDisconnect(Mqtt5DisconnectReasonCode.PROTOCOL_ERROR);
    }

    @Test
    public void decode_shared_subscription_available_not_0_or_1() {
        final ByteBuf byteBuf = channel.alloc().buffer();
        // fixed header
        //   type, flags
        byteBuf.writeByte(0b0010_0000);
        //   remaining length
        byteBuf.writeByte(5);
        // variable header
        //   connack flags
        byteBuf.writeByte(0b0000_0001);
        //   reason code (success)
        byteBuf.writeByte(0x00);
        //   properties
        byteBuf.writeByte(2);
        //     shared subscription available
        byteBuf.writeBytes(new byte[]{0x2A, 2});

        channel.writeInbound(byteBuf);

        testDisconnect(Mqtt5DisconnectReasonCode.PROTOCOL_ERROR);
    }

    @Test
    public void decode_must_not_multiple_session_expiry_interval() {
        final ByteBuf byteBuf = channel.alloc().buffer();
        // fixed header
        //   type, flags
        byteBuf.writeByte(0b0010_0000);
        //   remaining length
        byteBuf.writeByte(13);
        // variable header
        //   connack flags
        byteBuf.writeByte(0b0000_0001);
        //   reason code (success)
        byteBuf.writeByte(0x00);
        //   properties
        byteBuf.writeByte(10);
        //     session expiry interval
        byteBuf.writeBytes(new byte[]{0x11, 0, 0, 0, 10});
        //     session expiry interval
        byteBuf.writeBytes(new byte[]{0x11, 0, 0, 0, 10});

        channel.writeInbound(byteBuf);

        testDisconnect(Mqtt5DisconnectReasonCode.PROTOCOL_ERROR);
    }

    @Test
    public void decode_must_not_multiple_receive_maximum() {
        final ByteBuf byteBuf = channel.alloc().buffer();
        // fixed header
        //   type, flags
        byteBuf.writeByte(0b0010_0000);
        //   remaining length
        byteBuf.writeByte(9);
        // variable header
        //   connack flags
        byteBuf.writeByte(0b0000_0001);
        //   reason code (success)
        byteBuf.writeByte(0x00);
        //   properties
        byteBuf.writeByte(6);
        //     receive maximum
        byteBuf.writeBytes(new byte[]{0x21, 0, 100});
        //     receive maximum
        byteBuf.writeBytes(new byte[]{0x21, 0, 100});

        channel.writeInbound(byteBuf);

        testDisconnect(Mqtt5DisconnectReasonCode.PROTOCOL_ERROR);
    }

    @Test
    public void decode_must_not_multiple_maximum_qos() {
        final ByteBuf byteBuf = channel.alloc().buffer();
        // fixed header
        //   type, flags
        byteBuf.writeByte(0b0010_0000);
        //   remaining length
        byteBuf.writeByte(7);
        // variable header
        //   connack flags
        byteBuf.writeByte(0b0000_0001);
        //   reason code (success)
        byteBuf.writeByte(0x00);
        //   properties
        byteBuf.writeByte(4);
        //     receive maximum
        byteBuf.writeBytes(new byte[]{0x24, 1});
        //     receive maximum
        byteBuf.writeBytes(new byte[]{0x24, 1});

        channel.writeInbound(byteBuf);

        testDisconnect(Mqtt5DisconnectReasonCode.PROTOCOL_ERROR);
    }

    @Test
    public void decode_must_not_multiple_retain_available() {
        final ByteBuf byteBuf = channel.alloc().buffer();
        // fixed header
        //   type, flags
        byteBuf.writeByte(0b0010_0000);
        //   remaining length
        byteBuf.writeByte(7);
        // variable header
        //   connack flags
        byteBuf.writeByte(0b0000_0001);
        //   reason code (success)
        byteBuf.writeByte(0x00);
        //   properties
        byteBuf.writeByte(4);
        //     retain available
        byteBuf.writeBytes(new byte[]{0x25, 0});
        //     retain available
        byteBuf.writeBytes(new byte[]{0x25, 0});

        channel.writeInbound(byteBuf);

        testDisconnect(Mqtt5DisconnectReasonCode.PROTOCOL_ERROR);
    }

    @Test
    public void decode_must_not_multiple_maximum_packet_size() {
        final ByteBuf byteBuf = channel.alloc().buffer();
        // fixed header
        //   type, flags
        byteBuf.writeByte(0b0010_0000);
        //   remaining length
        byteBuf.writeByte(13);
        // variable header
        //   connack flags
        byteBuf.writeByte(0b0000_0001);
        //   reason code (success)
        byteBuf.writeByte(0x00);
        //   properties
        byteBuf.writeByte(10);
        //     maximum packet size
        byteBuf.writeBytes(new byte[]{0x27, 0, 0, 0, 100});
        //     maximum packet size
        byteBuf.writeBytes(new byte[]{0x27, 0, 0, 0, 100});

        channel.writeInbound(byteBuf);

        testDisconnect(Mqtt5DisconnectReasonCode.PROTOCOL_ERROR);
    }

    @Test
    public void decode_must_not_multiple_assigned_client_identifier() {
        final ByteBuf byteBuf = channel.alloc().buffer();
        // fixed header
        //   type, flags
        byteBuf.writeByte(0b0010_0000);
        //   remaining length
        byteBuf.writeByte(17);
        // variable header
        //   connack flags
        byteBuf.writeByte(0b0000_0001);
        //   reason code (success)
        byteBuf.writeByte(0x00);
        //   properties
        byteBuf.writeByte(14);
        //     assigned client identifier
        byteBuf.writeBytes(new byte[]{0x12, 0, 4, 't', 'e', 's', 't'});
        //     assigned client identifier
        byteBuf.writeBytes(new byte[]{0x12, 0, 4, 't', 'e', 's', 't'});

        channel.writeInbound(byteBuf);

        testDisconnect(Mqtt5DisconnectReasonCode.PROTOCOL_ERROR);
    }

    @Test
    public void decode_must_not_multiple_topic_alias_maximum() {
        final ByteBuf byteBuf = channel.alloc().buffer();
        // fixed header
        //   type, flags
        byteBuf.writeByte(0b0010_0000);
        //   remaining length
        byteBuf.writeByte(9);
        // variable header
        //   connack flags
        byteBuf.writeByte(0b0000_0001);
        //   reason code (success)
        byteBuf.writeByte(0x00);
        //   properties
        byteBuf.writeByte(6);
        //     topic alias maximum
        byteBuf.writeBytes(new byte[]{0x22, 0, 5});
        //     topic alias maximum
        byteBuf.writeBytes(new byte[]{0x22, 0, 5});

        channel.writeInbound(byteBuf);

        testDisconnect(Mqtt5DisconnectReasonCode.PROTOCOL_ERROR);
    }

    @Test
    public void decode_must_not_multiple_reason_string() {
        final ByteBuf byteBuf = channel.alloc().buffer();
        // fixed header
        //   type, flags
        byteBuf.writeByte(0b0010_0000);
        //   remaining length
        byteBuf.writeByte(23);
        // variable header
        //   connack flags
        byteBuf.writeByte(0b0000_0001);
        //   reason code (success)
        byteBuf.writeByte(0x00);
        //   properties
        byteBuf.writeByte(20);
        //     reason string
        byteBuf.writeBytes(new byte[]{0x1F, 0, 7, 's', 'u', 'c', 'c', 'e', 's', 's'});
        //     reason string
        byteBuf.writeBytes(new byte[]{0x1F, 0, 7, 's', 'u', 'c', 'c', 'e', 's', 's'});

        channel.writeInbound(byteBuf);

        testDisconnect(Mqtt5DisconnectReasonCode.PROTOCOL_ERROR);
    }

    @Test
    public void decode_must_not_multipe_wildcard_subscription_available() {
        final ByteBuf byteBuf = channel.alloc().buffer();
        // fixed header
        //   type, flags
        byteBuf.writeByte(0b0010_0000);
        //   remaining length
        byteBuf.writeByte(7);
        // variable header
        //   connack flags
        byteBuf.writeByte(0b0000_0001);
        //   reason code (success)
        byteBuf.writeByte(0x00);
        //   properties
        byteBuf.writeByte(4);
        //     wildcard subscription available
        byteBuf.writeBytes(new byte[]{0x28, 0});
        //     wildcard subscription available
        byteBuf.writeBytes(new byte[]{0x28, 0});

        channel.writeInbound(byteBuf);

        testDisconnect(Mqtt5DisconnectReasonCode.PROTOCOL_ERROR);
    }

    @Test
    public void decode_must_not_multiple_subscription_identifiers_available() {
        final ByteBuf byteBuf = channel.alloc().buffer();
        // fixed header
        //   type, flags
        byteBuf.writeByte(0b0010_0000);
        //   remaining length
        byteBuf.writeByte(7);
        // variable header
        //   connack flags
        byteBuf.writeByte(0b0000_0001);
        //   reason code (success)
        byteBuf.writeByte(0x00);
        //   properties
        byteBuf.writeByte(4);
        //     subscription identifiers available
        byteBuf.writeBytes(new byte[]{0x29, 1});
        //     subscription identifiers available
        byteBuf.writeBytes(new byte[]{0x29, 1});

        channel.writeInbound(byteBuf);

        testDisconnect(Mqtt5DisconnectReasonCode.PROTOCOL_ERROR);
    }

    @Test
    public void decode_must_not_multiple_shared_subscription_available() {
        final ByteBuf byteBuf = channel.alloc().buffer();
        // fixed header
        //   type, flags
        byteBuf.writeByte(0b0010_0000);
        //   remaining length
        byteBuf.writeByte(7);
        // variable header
        //   connack flags
        byteBuf.writeByte(0b0000_0001);
        //   reason code (success)
        byteBuf.writeByte(0x00);
        //   properties
        byteBuf.writeByte(4);
        //     shared subscription available
        byteBuf.writeBytes(new byte[]{0x2A, 0});
        //     shared subscription available
        byteBuf.writeBytes(new byte[]{0x2A, 0});

        channel.writeInbound(byteBuf);

        testDisconnect(Mqtt5DisconnectReasonCode.PROTOCOL_ERROR);
    }

    @Test
    public void decode_must_not_multiple_server_keep_alive() {
        final ByteBuf byteBuf = channel.alloc().buffer();
        // fixed header
        //   type, flags
        byteBuf.writeByte(0b0010_0000);
        //   remaining length
        byteBuf.writeByte(9);
        // variable header
        //   connack flags
        byteBuf.writeByte(0b0000_0001);
        //   reason code (success)
        byteBuf.writeByte(0x00);
        //   properties
        byteBuf.writeByte(6);
        //     server keep alive
        byteBuf.writeBytes(new byte[]{0x13, 0, 10});
        //     server keep alive
        byteBuf.writeBytes(new byte[]{0x13, 0, 10});

        channel.writeInbound(byteBuf);

        testDisconnect(Mqtt5DisconnectReasonCode.PROTOCOL_ERROR);
    }

    @Test
    public void decode_must_not_multiple_response_information() {
        final ByteBuf byteBuf = channel.alloc().buffer();
        // fixed header
        //   type, flags
        byteBuf.writeByte(0b0010_0000);
        //   remaining length
        byteBuf.writeByte(25);
        // variable header
        //   connack flags
        byteBuf.writeByte(0b0000_0001);
        //   reason code (success)
        byteBuf.writeByte(0x00);
        //   properties
        byteBuf.writeByte(22);
        //     response information
        byteBuf.writeBytes(new byte[]{0x1A, 0, 8, 'r', 'e', 's', 'p', 'o', 'n', 's', 'e'});
        //     response information
        byteBuf.writeBytes(new byte[]{0x1A, 0, 8, 'r', 'e', 's', 'p', 'o', 'n', 's', 'e'});

        channel.writeInbound(byteBuf);

        testDisconnect(Mqtt5DisconnectReasonCode.PROTOCOL_ERROR);
    }

    @Test
    public void decode_must_not_multiple_server_reference() {
        final ByteBuf byteBuf = channel.alloc().buffer();
        // fixed header
        //   type, flags
        byteBuf.writeByte(0b0010_0000);
        //   remaining length
        byteBuf.writeByte(21);
        // variable header
        //   connack flags
        byteBuf.writeByte(0b0000_0001);
        //   reason code (success)
        byteBuf.writeByte(0x00);
        //   properties
        byteBuf.writeByte(18);
        //     server reference
        byteBuf.writeBytes(new byte[]{0x1C, 0, 6, 's', 'e', 'r', 'v', 'e', 'r'});
        //     server reference
        byteBuf.writeBytes(new byte[]{0x1C, 0, 6, 's', 'e', 'r', 'v', 'e', 'r'});

        channel.writeInbound(byteBuf);

        testDisconnect(Mqtt5DisconnectReasonCode.PROTOCOL_ERROR);
    }

    @Test
    public void decode_must_not_multiple_auth_method() {
        final ByteBuf byteBuf = channel.alloc().buffer();
        // fixed header
        //   type, flags
        byteBuf.writeByte(0b0010_0000);
        //   remaining length
        byteBuf.writeByte(25);
        // variable header
        //   connack flags
        byteBuf.writeByte(0b0000_0001);
        //   reason code (success)
        byteBuf.writeByte(0x00);
        //   properties
        byteBuf.writeByte(22);
        //     auth method
        byteBuf.writeBytes(new byte[]{0x15, 0, 8, 'G', 'S', '2', '-', 'K', 'R', 'B', '5'});
        //     auth method
        byteBuf.writeBytes(new byte[]{0x15, 0, 8, 'G', 'S', '2', '-', 'K', 'R', 'B', '5'});

        channel.writeInbound(byteBuf);

        testDisconnect(Mqtt5DisconnectReasonCode.PROTOCOL_ERROR);
    }

    @Test
    public void decode_must_not_multiple_auth_data() {
        final ByteBuf byteBuf = channel.alloc().buffer();
        // fixed header
        //   type, flags
        byteBuf.writeByte(0b0010_0000);
        //   remaining length
        byteBuf.writeByte(29);
        // variable header
        //   connack flags
        byteBuf.writeByte(0b0000_0001);
        //   reason code (success)
        byteBuf.writeByte(0x00);
        //   properties
        byteBuf.writeByte(26);
        //     auth data
        byteBuf.writeBytes(new byte[]{0x16, 0, 10, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10});
        //     auth data
        byteBuf.writeBytes(new byte[]{0x16, 0, 10, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10});

        channel.writeInbound(byteBuf);

        testDisconnect(Mqtt5DisconnectReasonCode.PROTOCOL_ERROR);
    }

    private void testDisconnect(final Mqtt5DisconnectReasonCode reasonCode) {
        final Mqtt5ConnAck connAck = channel.readInbound();
        assertNull(connAck);

        final Mqtt5Disconnect disconnect = channel.readOutbound();
        assertNotNull(disconnect);
        assertEquals(reasonCode, disconnect.getReasonCode());
    }

    private static final byte PROPERTIES_VALID_LENGTH = 119;
    private static final byte[] PROPERTIES_VALID = {
            //     session expiry interval
            0x11, 0, 0, 0, 10,
            //     receive maximum
            0x21, 0, 100,
            //     maximum qos
            0x24, 1,
            //     retain available
            0x25, 0,
            //     maximum packet size
            0x27, 0, 0, 0, 100,
            //     assigned client identifier
            0x12, 0, 4, 't', 'e', 's', 't',
            //     topic alias maximum
            0x22, 0, 5,
            //     reason string
            0x1F, 0, 7, 's', 'u', 'c', 'c', 'e', 's', 's',
            //     user properties
            0x26, 0, 4, 't', 'e', 's', 't', 0, 5, 'v', 'a', 'l', 'u', 'e',
            0x26, 0, 4, 't', 'e', 's', 't', 0, 6, 'v', 'a', 'l', 'u', 'e', '2',
            //     wildcard subscription available
            0x28, 0,
            //     subscription identifiers available
            0x29, 1,
            //     shared subscription available
            0x2A, 0,
            //     server keep alive
            0x13, 0, 10,
            //     response information
            0x1A, 0, 8, 'r', 'e', 's', 'p', 'o', 'n', 's', 'e',
            //     server reference
            0x1C, 0, 6, 's', 'e', 'r', 'v', 'e', 'r',
            //     auth method
            0x15, 0, 8, 'G', 'S', '2', '-', 'K', 'R', 'B', '5',
            //     auth data
            0x16, 0, 10, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10,
    };

    private static class Mqtt5ConnAckTestMessageDecoders implements Mqtt5MessageDecoders {
        @Nullable
        @Override
        public Mqtt5MessageDecoder get(final int code) {
            if (code == Mqtt5MessageType.CONNACK.getCode()) {
                return new Mqtt5ConnAckDecoder();
            }
            return null;
        }
    }

}