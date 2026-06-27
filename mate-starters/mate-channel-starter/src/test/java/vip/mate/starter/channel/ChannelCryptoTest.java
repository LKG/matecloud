/*
 * Copyright (c) 2024-2026 Beijing Daotiandi Technology Co., Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package vip.mate.starter.channel;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChannelCryptoTest {

    private final ChannelCrypto crypto = new ChannelCrypto("MateCloudDevEncryptKey0123456789");

    @Test
    void roundTrip() {
        String enc = crypto.encrypt("{\"accessKey\":\"AKID\",\"secretKey\":\"s3cr3t\"}");
        assertTrue(enc.startsWith("MGCM1:"));
        assertNotEquals("s3cr3t", enc);
        assertEquals("{\"accessKey\":\"AKID\",\"secretKey\":\"s3cr3t\"}", crypto.decrypt(enc));
    }

    @Test
    void legacyPlaintextPassesThrough() {
        assertEquals("{\"plain\":\"true\"}", crypto.decrypt("{\"plain\":\"true\"}"));
    }

    @Test
    void blankPassesThrough() {
        assertNull(crypto.encrypt(null));
        assertEquals("", crypto.encrypt(""));
    }
}
