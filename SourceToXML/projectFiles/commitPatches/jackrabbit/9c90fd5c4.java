From 9c90fd5c4c971664fc78656791946f0e8eef37f0 Mon Sep 17 00:00:00 2001
From: Jukka Zitting <jukka@apache.org>
Date: Sat, 1 Jul 2006 07:14:14 +0000
Subject: [PATCH] JCR-367: Use the XMLChar class from Xerces instead of from
 JDK.

git-svn-id: https://svn.apache.org/repos/asf/jackrabbit/trunk@418445 13f79535-47bb-0310-9956-ffa450edef68
--
 .../org/apache/jackrabbit/util/XMLChar.java   | 959 ++++++++++++------
 1 file changed, 651 insertions(+), 308 deletions(-)

diff --git a/jackrabbit/src/main/java/org/apache/jackrabbit/util/XMLChar.java b/jackrabbit/src/main/java/org/apache/jackrabbit/util/XMLChar.java
index 12cdaeb25..9802fec75 100644
-- a/jackrabbit/src/main/java/org/apache/jackrabbit/util/XMLChar.java
++ b/jackrabbit/src/main/java/org/apache/jackrabbit/util/XMLChar.java
@@ -1,62 +1,25 @@
 /*
 * The Apache Software License, Version 1.1
 *
 *
 * Copyright (c) 1999-2002 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "Xerces" and "Apache Software Foundation" must
 *    not be used to endorse or promote products derived from this
 *    software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache",
 *    nor may "Apache" appear in their name, without prior written
 *    permission of the Apache Software Foundation.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation and was
 * originally based on software copyright (c) 1999, International
 * Business Machines, Inc., http://www.apache.org.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 * Copyright 1999-2004 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
  */

 package org.apache.jackrabbit.util;
 
// Note: This file is a copy from org.apache.xerces.util.
// See http://issues.apache.org/jira/browse/JCR-367

import java.util.Arrays;

 /**
  * This class defines the basic XML character properties. The data
  * in this class can be used to verify that a character is a valid
@@ -76,6 +39,7 @@ package org.apache.jackrabbit.util;
  * @author Andy Clark, IBM
  * @author Eric Ye, IBM
  * @author Arnaud  Le Hors, IBM
 * @author Michael Glavassevich, IBM
  * @author Rahul Srivastava, Sun Microsystems Inc.
  *
  * @version $Id$
@@ -87,7 +51,7 @@ public class XMLChar {
     //
 
     /** Character flags. */
    public static final byte[] CHARS = new byte[1 << 16];
    private static final byte[] CHARS = new byte[1 << 16];
 
     /** Valid character mask. */
     public static final int MASK_VALID = 0x01;
@@ -103,10 +67,10 @@ public class XMLChar {
 
     /** Pubid character mask. */
     public static final int MASK_PUBID = 0x10;

    /**
    
    /** 
      * Content character mask. Special characters are those that can
     * be considered the start of markup, such as '&lt;' and '&amp;'.
     * be considered the start of markup, such as '&lt;' and '&amp;'. 
      * The various newline characters are considered special as well.
      * All other valid XML characters can be considered content.
      * <p>
@@ -125,251 +89,629 @@ public class XMLChar {
     //
 
     static {

        //
        // [2] Char ::= #x9 | #xA | #xD | [#x20-#xD7FF] |
        //              [#xE000-#xFFFD] | [#x10000-#x10FFFF]
        //

        int charRange[] = {
            0x0009, 0x000A, 0x000D, 0x000D, 0x0020, 0xD7FF, 0xE000, 0xFFFD,
        };

        //
        // [3] S ::= (#x20 | #x9 | #xD | #xA)+
        //

        int spaceChar[] = {
            0x0020, 0x0009, 0x000D, 0x000A,
        };

        //
        // [4] NameChar ::= Letter | Digit | '.' | '-' | '_' | ':' |
        //                  CombiningChar | Extender
        //

        int nameChar[] = {
            0x002D, 0x002E, // '-' and '.'
        };

        //
        // [5] Name ::= (Letter | '_' | ':') (NameChar)*
        //

        int nameStartChar[] = {
            0x003A, 0x005F, // ':' and '_'
        };

        //
        // [13] PubidChar ::= #x20 | 0xD | 0xA | [a-zA-Z0-9] | [-'()+,./:=?;!*#@$_%]
        //

        int pubidChar[] = {
            0x000A, 0x000D, 0x0020, 0x0021, 0x0023, 0x0024, 0x0025, 0x003D,
            0x005F
        };

        int pubidRange[] = {
            0x0027, 0x003B, 0x003F, 0x005A, 0x0061, 0x007A
        };

        //
        // [84] Letter ::= BaseChar | Ideographic
        //

        int letterRange[] = {
            // BaseChar
            0x0041, 0x005A, 0x0061, 0x007A, 0x00C0, 0x00D6, 0x00D8, 0x00F6,
            0x00F8, 0x0131, 0x0134, 0x013E, 0x0141, 0x0148, 0x014A, 0x017E,
            0x0180, 0x01C3, 0x01CD, 0x01F0, 0x01F4, 0x01F5, 0x01FA, 0x0217,
            0x0250, 0x02A8, 0x02BB, 0x02C1, 0x0388, 0x038A, 0x038E, 0x03A1,
            0x03A3, 0x03CE, 0x03D0, 0x03D6, 0x03E2, 0x03F3, 0x0401, 0x040C,
            0x040E, 0x044F, 0x0451, 0x045C, 0x045E, 0x0481, 0x0490, 0x04C4,
            0x04C7, 0x04C8, 0x04CB, 0x04CC, 0x04D0, 0x04EB, 0x04EE, 0x04F5,
            0x04F8, 0x04F9, 0x0531, 0x0556, 0x0561, 0x0586, 0x05D0, 0x05EA,
            0x05F0, 0x05F2, 0x0621, 0x063A, 0x0641, 0x064A, 0x0671, 0x06B7,
            0x06BA, 0x06BE, 0x06C0, 0x06CE, 0x06D0, 0x06D3, 0x06E5, 0x06E6,
            0x0905, 0x0939, 0x0958, 0x0961, 0x0985, 0x098C, 0x098F, 0x0990,
            0x0993, 0x09A8, 0x09AA, 0x09B0, 0x09B6, 0x09B9, 0x09DC, 0x09DD,
            0x09DF, 0x09E1, 0x09F0, 0x09F1, 0x0A05, 0x0A0A, 0x0A0F, 0x0A10,
            0x0A13, 0x0A28, 0x0A2A, 0x0A30, 0x0A32, 0x0A33, 0x0A35, 0x0A36,
            0x0A38, 0x0A39, 0x0A59, 0x0A5C, 0x0A72, 0x0A74, 0x0A85, 0x0A8B,
            0x0A8F, 0x0A91, 0x0A93, 0x0AA8, 0x0AAA, 0x0AB0, 0x0AB2, 0x0AB3,
            0x0AB5, 0x0AB9, 0x0B05, 0x0B0C, 0x0B0F, 0x0B10, 0x0B13, 0x0B28,
            0x0B2A, 0x0B30, 0x0B32, 0x0B33, 0x0B36, 0x0B39, 0x0B5C, 0x0B5D,
            0x0B5F, 0x0B61, 0x0B85, 0x0B8A, 0x0B8E, 0x0B90, 0x0B92, 0x0B95,
            0x0B99, 0x0B9A, 0x0B9E, 0x0B9F, 0x0BA3, 0x0BA4, 0x0BA8, 0x0BAA,
            0x0BAE, 0x0BB5, 0x0BB7, 0x0BB9, 0x0C05, 0x0C0C, 0x0C0E, 0x0C10,
            0x0C12, 0x0C28, 0x0C2A, 0x0C33, 0x0C35, 0x0C39, 0x0C60, 0x0C61,
            0x0C85, 0x0C8C, 0x0C8E, 0x0C90, 0x0C92, 0x0CA8, 0x0CAA, 0x0CB3,
            0x0CB5, 0x0CB9, 0x0CE0, 0x0CE1, 0x0D05, 0x0D0C, 0x0D0E, 0x0D10,
            0x0D12, 0x0D28, 0x0D2A, 0x0D39, 0x0D60, 0x0D61, 0x0E01, 0x0E2E,
            0x0E32, 0x0E33, 0x0E40, 0x0E45, 0x0E81, 0x0E82, 0x0E87, 0x0E88,
            0x0E94, 0x0E97, 0x0E99, 0x0E9F, 0x0EA1, 0x0EA3, 0x0EAA, 0x0EAB,
            0x0EAD, 0x0EAE, 0x0EB2, 0x0EB3, 0x0EC0, 0x0EC4, 0x0F40, 0x0F47,
            0x0F49, 0x0F69, 0x10A0, 0x10C5, 0x10D0, 0x10F6, 0x1102, 0x1103,
            0x1105, 0x1107, 0x110B, 0x110C, 0x110E, 0x1112, 0x1154, 0x1155,
            0x115F, 0x1161, 0x116D, 0x116E, 0x1172, 0x1173, 0x11AE, 0x11AF,
            0x11B7, 0x11B8, 0x11BC, 0x11C2, 0x1E00, 0x1E9B, 0x1EA0, 0x1EF9,
            0x1F00, 0x1F15, 0x1F18, 0x1F1D, 0x1F20, 0x1F45, 0x1F48, 0x1F4D,
            0x1F50, 0x1F57, 0x1F5F, 0x1F7D, 0x1F80, 0x1FB4, 0x1FB6, 0x1FBC,
            0x1FC2, 0x1FC4, 0x1FC6, 0x1FCC, 0x1FD0, 0x1FD3, 0x1FD6, 0x1FDB,
            0x1FE0, 0x1FEC, 0x1FF2, 0x1FF4, 0x1FF6, 0x1FFC, 0x212A, 0x212B,
            0x2180, 0x2182, 0x3041, 0x3094, 0x30A1, 0x30FA, 0x3105, 0x312C,
            0xAC00, 0xD7A3,
            // Ideographic
            0x3021, 0x3029, 0x4E00, 0x9FA5,
        };
        int letterChar[] = {
            // BaseChar
            0x0386, 0x038C, 0x03DA, 0x03DC, 0x03DE, 0x03E0, 0x0559, 0x06D5,
            0x093D, 0x09B2, 0x0A5E, 0x0A8D, 0x0ABD, 0x0AE0, 0x0B3D, 0x0B9C,
            0x0CDE, 0x0E30, 0x0E84, 0x0E8A, 0x0E8D, 0x0EA5, 0x0EA7, 0x0EB0,
            0x0EBD, 0x1100, 0x1109, 0x113C, 0x113E, 0x1140, 0x114C, 0x114E,
            0x1150, 0x1159, 0x1163, 0x1165, 0x1167, 0x1169, 0x1175, 0x119E,
            0x11A8, 0x11AB, 0x11BA, 0x11EB, 0x11F0, 0x11F9, 0x1F59, 0x1F5B,
            0x1F5D, 0x1FBE, 0x2126, 0x212E,
            // Ideographic
            0x3007,
        };

        //
        // [87] CombiningChar ::= ...
        //

        int combiningCharRange[] = {
            0x0300, 0x0345, 0x0360, 0x0361, 0x0483, 0x0486, 0x0591, 0x05A1,
            0x05A3, 0x05B9, 0x05BB, 0x05BD, 0x05C1, 0x05C2, 0x064B, 0x0652,
            0x06D6, 0x06DC, 0x06DD, 0x06DF, 0x06E0, 0x06E4, 0x06E7, 0x06E8,
            0x06EA, 0x06ED, 0x0901, 0x0903, 0x093E, 0x094C, 0x0951, 0x0954,
            0x0962, 0x0963, 0x0981, 0x0983, 0x09C0, 0x09C4, 0x09C7, 0x09C8,
            0x09CB, 0x09CD, 0x09E2, 0x09E3, 0x0A40, 0x0A42, 0x0A47, 0x0A48,
            0x0A4B, 0x0A4D, 0x0A70, 0x0A71, 0x0A81, 0x0A83, 0x0ABE, 0x0AC5,
            0x0AC7, 0x0AC9, 0x0ACB, 0x0ACD, 0x0B01, 0x0B03, 0x0B3E, 0x0B43,
            0x0B47, 0x0B48, 0x0B4B, 0x0B4D, 0x0B56, 0x0B57, 0x0B82, 0x0B83,
            0x0BBE, 0x0BC2, 0x0BC6, 0x0BC8, 0x0BCA, 0x0BCD, 0x0C01, 0x0C03,
            0x0C3E, 0x0C44, 0x0C46, 0x0C48, 0x0C4A, 0x0C4D, 0x0C55, 0x0C56,
            0x0C82, 0x0C83, 0x0CBE, 0x0CC4, 0x0CC6, 0x0CC8, 0x0CCA, 0x0CCD,
            0x0CD5, 0x0CD6, 0x0D02, 0x0D03, 0x0D3E, 0x0D43, 0x0D46, 0x0D48,
            0x0D4A, 0x0D4D, 0x0E34, 0x0E3A, 0x0E47, 0x0E4E, 0x0EB4, 0x0EB9,
            0x0EBB, 0x0EBC, 0x0EC8, 0x0ECD, 0x0F18, 0x0F19, 0x0F71, 0x0F84,
            0x0F86, 0x0F8B, 0x0F90, 0x0F95, 0x0F99, 0x0FAD, 0x0FB1, 0x0FB7,
            0x20D0, 0x20DC, 0x302A, 0x302F,
        };

        int combiningCharChar[] = {
            0x05BF, 0x05C4, 0x0670, 0x093C, 0x094D, 0x09BC, 0x09BE, 0x09BF,
            0x09D7, 0x0A02, 0x0A3C, 0x0A3E, 0x0A3F, 0x0ABC, 0x0B3C, 0x0BD7,
            0x0D57, 0x0E31, 0x0EB1, 0x0F35, 0x0F37, 0x0F39, 0x0F3E, 0x0F3F,
            0x0F97, 0x0FB9, 0x20E1, 0x3099, 0x309A,
        };

        //
        // [88] Digit ::= ...
        //

        int digitRange[] = {
            0x0030, 0x0039, 0x0660, 0x0669, 0x06F0, 0x06F9, 0x0966, 0x096F,
            0x09E6, 0x09EF, 0x0A66, 0x0A6F, 0x0AE6, 0x0AEF, 0x0B66, 0x0B6F,
            0x0BE7, 0x0BEF, 0x0C66, 0x0C6F, 0x0CE6, 0x0CEF, 0x0D66, 0x0D6F,
            0x0E50, 0x0E59, 0x0ED0, 0x0ED9, 0x0F20, 0x0F29,
        };

        //
        // [89] Extender ::= ...
        //

        int extenderRange[] = {
            0x3031, 0x3035, 0x309D, 0x309E, 0x30FC, 0x30FE,
        };

        int extenderChar[] = {
            0x00B7, 0x02D0, 0x02D1, 0x0387, 0x0640, 0x0E46, 0x0EC6, 0x3005,
        };

        //
        // SpecialChar ::= '<', '&', '\n', '\r', ']'
        //

        int specialChar[] = {
            '<', '&', '\n', '\r', ']',
        };

        //
        // Initialize
        //

        // set valid characters
        for (int i = 0; i < charRange.length; i += 2) {
            for (int j = charRange[i]; j <= charRange[i + 1]; j++) {
                CHARS[j] |= MASK_VALID | MASK_CONTENT;
            }
        }

        // remove special characters
        for (int i = 0; i < specialChar.length; i++) {
            CHARS[specialChar[i]] = (byte)(CHARS[specialChar[i]] & ~MASK_CONTENT);
        }

        // set space characters
        for (int i = 0; i < spaceChar.length; i++) {
            CHARS[spaceChar[i]] |= MASK_SPACE;
        }

        // set name start characters
        for (int i = 0; i < nameStartChar.length; i++) {
            CHARS[nameStartChar[i]] |= MASK_NAME_START | MASK_NAME |
                                       MASK_NCNAME_START | MASK_NCNAME;
        }
        for (int i = 0; i < letterRange.length; i += 2) {
            for (int j = letterRange[i]; j <= letterRange[i + 1]; j++) {
                CHARS[j] |= MASK_NAME_START | MASK_NAME |
                            MASK_NCNAME_START | MASK_NCNAME;
            }
        }
        for (int i = 0; i < letterChar.length; i++) {
            CHARS[letterChar[i]] |= MASK_NAME_START | MASK_NAME |
                                    MASK_NCNAME_START | MASK_NCNAME;
        }

        // set name characters
        for (int i = 0; i < nameChar.length; i++) {
            CHARS[nameChar[i]] |= MASK_NAME | MASK_NCNAME;
        }
        for (int i = 0; i < digitRange.length; i += 2) {
            for (int j = digitRange[i]; j <= digitRange[i + 1]; j++) {
                CHARS[j] |= MASK_NAME | MASK_NCNAME;
            }
        }
        for (int i = 0; i < combiningCharRange.length; i += 2) {
            for (int j = combiningCharRange[i]; j <= combiningCharRange[i + 1]; j++) {
                CHARS[j] |= MASK_NAME | MASK_NCNAME;
            }
        }
        for (int i = 0; i < combiningCharChar.length; i++) {
            CHARS[combiningCharChar[i]] |= MASK_NAME | MASK_NCNAME;
        }
        for (int i = 0; i < extenderRange.length; i += 2) {
            for (int j = extenderRange[i]; j <= extenderRange[i + 1]; j++) {
                CHARS[j] |= MASK_NAME | MASK_NCNAME;
            }
        }
        for (int i = 0; i < extenderChar.length; i++) {
            CHARS[extenderChar[i]] |= MASK_NAME | MASK_NCNAME;
        }

        // remove ':' from allowable MASK_NCNAME_START and MASK_NCNAME chars
        CHARS[':'] &= ~(MASK_NCNAME_START | MASK_NCNAME);

        // set Pubid characters
        for (int i = 0; i < pubidChar.length; i++) {
            CHARS[pubidChar[i]] |= MASK_PUBID;
        }
        for (int i = 0; i < pubidRange.length; i += 2) {
            for (int j = pubidRange[i]; j <= pubidRange[i + 1]; j++) {
                CHARS[j] |= MASK_PUBID;
            }
        }
        
        // Initializing the Character Flag Array
        // Code generated by: XMLCharGenerator.
        
        CHARS[9] = 35;
        CHARS[10] = 19;
        CHARS[13] = 19;
        CHARS[32] = 51;
        CHARS[33] = 49;
        CHARS[34] = 33;
        Arrays.fill(CHARS, 35, 38, (byte) 49 ); // Fill 3 of value (byte) 49
        CHARS[38] = 1;
        Arrays.fill(CHARS, 39, 45, (byte) 49 ); // Fill 6 of value (byte) 49
        Arrays.fill(CHARS, 45, 47, (byte) -71 ); // Fill 2 of value (byte) -71
        CHARS[47] = 49;
        Arrays.fill(CHARS, 48, 58, (byte) -71 ); // Fill 10 of value (byte) -71
        CHARS[58] = 61;
        CHARS[59] = 49;
        CHARS[60] = 1;
        CHARS[61] = 49;
        CHARS[62] = 33;
        Arrays.fill(CHARS, 63, 65, (byte) 49 ); // Fill 2 of value (byte) 49
        Arrays.fill(CHARS, 65, 91, (byte) -3 ); // Fill 26 of value (byte) -3
        Arrays.fill(CHARS, 91, 93, (byte) 33 ); // Fill 2 of value (byte) 33
        CHARS[93] = 1;
        CHARS[94] = 33;
        CHARS[95] = -3;
        CHARS[96] = 33;
        Arrays.fill(CHARS, 97, 123, (byte) -3 ); // Fill 26 of value (byte) -3
        Arrays.fill(CHARS, 123, 183, (byte) 33 ); // Fill 60 of value (byte) 33
        CHARS[183] = -87;
        Arrays.fill(CHARS, 184, 192, (byte) 33 ); // Fill 8 of value (byte) 33
        Arrays.fill(CHARS, 192, 215, (byte) -19 ); // Fill 23 of value (byte) -19
        CHARS[215] = 33;
        Arrays.fill(CHARS, 216, 247, (byte) -19 ); // Fill 31 of value (byte) -19
        CHARS[247] = 33;
        Arrays.fill(CHARS, 248, 306, (byte) -19 ); // Fill 58 of value (byte) -19
        Arrays.fill(CHARS, 306, 308, (byte) 33 ); // Fill 2 of value (byte) 33
        Arrays.fill(CHARS, 308, 319, (byte) -19 ); // Fill 11 of value (byte) -19
        Arrays.fill(CHARS, 319, 321, (byte) 33 ); // Fill 2 of value (byte) 33
        Arrays.fill(CHARS, 321, 329, (byte) -19 ); // Fill 8 of value (byte) -19
        CHARS[329] = 33;
        Arrays.fill(CHARS, 330, 383, (byte) -19 ); // Fill 53 of value (byte) -19
        CHARS[383] = 33;
        Arrays.fill(CHARS, 384, 452, (byte) -19 ); // Fill 68 of value (byte) -19
        Arrays.fill(CHARS, 452, 461, (byte) 33 ); // Fill 9 of value (byte) 33
        Arrays.fill(CHARS, 461, 497, (byte) -19 ); // Fill 36 of value (byte) -19
        Arrays.fill(CHARS, 497, 500, (byte) 33 ); // Fill 3 of value (byte) 33
        Arrays.fill(CHARS, 500, 502, (byte) -19 ); // Fill 2 of value (byte) -19
        Arrays.fill(CHARS, 502, 506, (byte) 33 ); // Fill 4 of value (byte) 33
        Arrays.fill(CHARS, 506, 536, (byte) -19 ); // Fill 30 of value (byte) -19
        Arrays.fill(CHARS, 536, 592, (byte) 33 ); // Fill 56 of value (byte) 33
        Arrays.fill(CHARS, 592, 681, (byte) -19 ); // Fill 89 of value (byte) -19
        Arrays.fill(CHARS, 681, 699, (byte) 33 ); // Fill 18 of value (byte) 33
        Arrays.fill(CHARS, 699, 706, (byte) -19 ); // Fill 7 of value (byte) -19
        Arrays.fill(CHARS, 706, 720, (byte) 33 ); // Fill 14 of value (byte) 33
        Arrays.fill(CHARS, 720, 722, (byte) -87 ); // Fill 2 of value (byte) -87
        Arrays.fill(CHARS, 722, 768, (byte) 33 ); // Fill 46 of value (byte) 33
        Arrays.fill(CHARS, 768, 838, (byte) -87 ); // Fill 70 of value (byte) -87
        Arrays.fill(CHARS, 838, 864, (byte) 33 ); // Fill 26 of value (byte) 33
        Arrays.fill(CHARS, 864, 866, (byte) -87 ); // Fill 2 of value (byte) -87
        Arrays.fill(CHARS, 866, 902, (byte) 33 ); // Fill 36 of value (byte) 33
        CHARS[902] = -19;
        CHARS[903] = -87;
        Arrays.fill(CHARS, 904, 907, (byte) -19 ); // Fill 3 of value (byte) -19
        CHARS[907] = 33;
        CHARS[908] = -19;
        CHARS[909] = 33;
        Arrays.fill(CHARS, 910, 930, (byte) -19 ); // Fill 20 of value (byte) -19
        CHARS[930] = 33;
        Arrays.fill(CHARS, 931, 975, (byte) -19 ); // Fill 44 of value (byte) -19
        CHARS[975] = 33;
        Arrays.fill(CHARS, 976, 983, (byte) -19 ); // Fill 7 of value (byte) -19
        Arrays.fill(CHARS, 983, 986, (byte) 33 ); // Fill 3 of value (byte) 33
        CHARS[986] = -19;
        CHARS[987] = 33;
        CHARS[988] = -19;
        CHARS[989] = 33;
        CHARS[990] = -19;
        CHARS[991] = 33;
        CHARS[992] = -19;
        CHARS[993] = 33;
        Arrays.fill(CHARS, 994, 1012, (byte) -19 ); // Fill 18 of value (byte) -19
        Arrays.fill(CHARS, 1012, 1025, (byte) 33 ); // Fill 13 of value (byte) 33
        Arrays.fill(CHARS, 1025, 1037, (byte) -19 ); // Fill 12 of value (byte) -19
        CHARS[1037] = 33;
        Arrays.fill(CHARS, 1038, 1104, (byte) -19 ); // Fill 66 of value (byte) -19
        CHARS[1104] = 33;
        Arrays.fill(CHARS, 1105, 1117, (byte) -19 ); // Fill 12 of value (byte) -19
        CHARS[1117] = 33;
        Arrays.fill(CHARS, 1118, 1154, (byte) -19 ); // Fill 36 of value (byte) -19
        CHARS[1154] = 33;
        Arrays.fill(CHARS, 1155, 1159, (byte) -87 ); // Fill 4 of value (byte) -87
        Arrays.fill(CHARS, 1159, 1168, (byte) 33 ); // Fill 9 of value (byte) 33
        Arrays.fill(CHARS, 1168, 1221, (byte) -19 ); // Fill 53 of value (byte) -19
        Arrays.fill(CHARS, 1221, 1223, (byte) 33 ); // Fill 2 of value (byte) 33
        Arrays.fill(CHARS, 1223, 1225, (byte) -19 ); // Fill 2 of value (byte) -19
        Arrays.fill(CHARS, 1225, 1227, (byte) 33 ); // Fill 2 of value (byte) 33
        Arrays.fill(CHARS, 1227, 1229, (byte) -19 ); // Fill 2 of value (byte) -19
        Arrays.fill(CHARS, 1229, 1232, (byte) 33 ); // Fill 3 of value (byte) 33
        Arrays.fill(CHARS, 1232, 1260, (byte) -19 ); // Fill 28 of value (byte) -19
        Arrays.fill(CHARS, 1260, 1262, (byte) 33 ); // Fill 2 of value (byte) 33
        Arrays.fill(CHARS, 1262, 1270, (byte) -19 ); // Fill 8 of value (byte) -19
        Arrays.fill(CHARS, 1270, 1272, (byte) 33 ); // Fill 2 of value (byte) 33
        Arrays.fill(CHARS, 1272, 1274, (byte) -19 ); // Fill 2 of value (byte) -19
        Arrays.fill(CHARS, 1274, 1329, (byte) 33 ); // Fill 55 of value (byte) 33
        Arrays.fill(CHARS, 1329, 1367, (byte) -19 ); // Fill 38 of value (byte) -19
        Arrays.fill(CHARS, 1367, 1369, (byte) 33 ); // Fill 2 of value (byte) 33
        CHARS[1369] = -19;
        Arrays.fill(CHARS, 1370, 1377, (byte) 33 ); // Fill 7 of value (byte) 33
        Arrays.fill(CHARS, 1377, 1415, (byte) -19 ); // Fill 38 of value (byte) -19
        Arrays.fill(CHARS, 1415, 1425, (byte) 33 ); // Fill 10 of value (byte) 33
        Arrays.fill(CHARS, 1425, 1442, (byte) -87 ); // Fill 17 of value (byte) -87
        CHARS[1442] = 33;
        Arrays.fill(CHARS, 1443, 1466, (byte) -87 ); // Fill 23 of value (byte) -87
        CHARS[1466] = 33;
        Arrays.fill(CHARS, 1467, 1470, (byte) -87 ); // Fill 3 of value (byte) -87
        CHARS[1470] = 33;
        CHARS[1471] = -87;
        CHARS[1472] = 33;
        Arrays.fill(CHARS, 1473, 1475, (byte) -87 ); // Fill 2 of value (byte) -87
        CHARS[1475] = 33;
        CHARS[1476] = -87;
        Arrays.fill(CHARS, 1477, 1488, (byte) 33 ); // Fill 11 of value (byte) 33
        Arrays.fill(CHARS, 1488, 1515, (byte) -19 ); // Fill 27 of value (byte) -19
        Arrays.fill(CHARS, 1515, 1520, (byte) 33 ); // Fill 5 of value (byte) 33
        Arrays.fill(CHARS, 1520, 1523, (byte) -19 ); // Fill 3 of value (byte) -19
        Arrays.fill(CHARS, 1523, 1569, (byte) 33 ); // Fill 46 of value (byte) 33
        Arrays.fill(CHARS, 1569, 1595, (byte) -19 ); // Fill 26 of value (byte) -19
        Arrays.fill(CHARS, 1595, 1600, (byte) 33 ); // Fill 5 of value (byte) 33
        CHARS[1600] = -87;
        Arrays.fill(CHARS, 1601, 1611, (byte) -19 ); // Fill 10 of value (byte) -19
        Arrays.fill(CHARS, 1611, 1619, (byte) -87 ); // Fill 8 of value (byte) -87
        Arrays.fill(CHARS, 1619, 1632, (byte) 33 ); // Fill 13 of value (byte) 33
        Arrays.fill(CHARS, 1632, 1642, (byte) -87 ); // Fill 10 of value (byte) -87
        Arrays.fill(CHARS, 1642, 1648, (byte) 33 ); // Fill 6 of value (byte) 33
        CHARS[1648] = -87;
        Arrays.fill(CHARS, 1649, 1720, (byte) -19 ); // Fill 71 of value (byte) -19
        Arrays.fill(CHARS, 1720, 1722, (byte) 33 ); // Fill 2 of value (byte) 33
        Arrays.fill(CHARS, 1722, 1727, (byte) -19 ); // Fill 5 of value (byte) -19
        CHARS[1727] = 33;
        Arrays.fill(CHARS, 1728, 1743, (byte) -19 ); // Fill 15 of value (byte) -19
        CHARS[1743] = 33;
        Arrays.fill(CHARS, 1744, 1748, (byte) -19 ); // Fill 4 of value (byte) -19
        CHARS[1748] = 33;
        CHARS[1749] = -19;
        Arrays.fill(CHARS, 1750, 1765, (byte) -87 ); // Fill 15 of value (byte) -87
        Arrays.fill(CHARS, 1765, 1767, (byte) -19 ); // Fill 2 of value (byte) -19
        Arrays.fill(CHARS, 1767, 1769, (byte) -87 ); // Fill 2 of value (byte) -87
        CHARS[1769] = 33;
        Arrays.fill(CHARS, 1770, 1774, (byte) -87 ); // Fill 4 of value (byte) -87
        Arrays.fill(CHARS, 1774, 1776, (byte) 33 ); // Fill 2 of value (byte) 33
        Arrays.fill(CHARS, 1776, 1786, (byte) -87 ); // Fill 10 of value (byte) -87
        Arrays.fill(CHARS, 1786, 2305, (byte) 33 ); // Fill 519 of value (byte) 33
        Arrays.fill(CHARS, 2305, 2308, (byte) -87 ); // Fill 3 of value (byte) -87
        CHARS[2308] = 33;
        Arrays.fill(CHARS, 2309, 2362, (byte) -19 ); // Fill 53 of value (byte) -19
        Arrays.fill(CHARS, 2362, 2364, (byte) 33 ); // Fill 2 of value (byte) 33
        CHARS[2364] = -87;
        CHARS[2365] = -19;
        Arrays.fill(CHARS, 2366, 2382, (byte) -87 ); // Fill 16 of value (byte) -87
        Arrays.fill(CHARS, 2382, 2385, (byte) 33 ); // Fill 3 of value (byte) 33
        Arrays.fill(CHARS, 2385, 2389, (byte) -87 ); // Fill 4 of value (byte) -87
        Arrays.fill(CHARS, 2389, 2392, (byte) 33 ); // Fill 3 of value (byte) 33
        Arrays.fill(CHARS, 2392, 2402, (byte) -19 ); // Fill 10 of value (byte) -19
        Arrays.fill(CHARS, 2402, 2404, (byte) -87 ); // Fill 2 of value (byte) -87
        Arrays.fill(CHARS, 2404, 2406, (byte) 33 ); // Fill 2 of value (byte) 33
        Arrays.fill(CHARS, 2406, 2416, (byte) -87 ); // Fill 10 of value (byte) -87
        Arrays.fill(CHARS, 2416, 2433, (byte) 33 ); // Fill 17 of value (byte) 33
        Arrays.fill(CHARS, 2433, 2436, (byte) -87 ); // Fill 3 of value (byte) -87
        CHARS[2436] = 33;
        Arrays.fill(CHARS, 2437, 2445, (byte) -19 ); // Fill 8 of value (byte) -19
        Arrays.fill(CHARS, 2445, 2447, (byte) 33 ); // Fill 2 of value (byte) 33
        Arrays.fill(CHARS, 2447, 2449, (byte) -19 ); // Fill 2 of value (byte) -19
        Arrays.fill(CHARS, 2449, 2451, (byte) 33 ); // Fill 2 of value (byte) 33
        Arrays.fill(CHARS, 2451, 2473, (byte) -19 ); // Fill 22 of value (byte) -19
        CHARS[2473] = 33;
        Arrays.fill(CHARS, 2474, 2481, (byte) -19 ); // Fill 7 of value (byte) -19
        CHARS[2481] = 33;
        CHARS[2482] = -19;
        Arrays.fill(CHARS, 2483, 2486, (byte) 33 ); // Fill 3 of value (byte) 33
        Arrays.fill(CHARS, 2486, 2490, (byte) -19 ); // Fill 4 of value (byte) -19
        Arrays.fill(CHARS, 2490, 2492, (byte) 33 ); // Fill 2 of value (byte) 33
        CHARS[2492] = -87;
        CHARS[2493] = 33;
        Arrays.fill(CHARS, 2494, 2501, (byte) -87 ); // Fill 7 of value (byte) -87
        Arrays.fill(CHARS, 2501, 2503, (byte) 33 ); // Fill 2 of value (byte) 33
        Arrays.fill(CHARS, 2503, 2505, (byte) -87 ); // Fill 2 of value (byte) -87
        Arrays.fill(CHARS, 2505, 2507, (byte) 33 ); // Fill 2 of value (byte) 33
        Arrays.fill(CHARS, 2507, 2510, (byte) -87 ); // Fill 3 of value (byte) -87
        Arrays.fill(CHARS, 2510, 2519, (byte) 33 ); // Fill 9 of value (byte) 33
        CHARS[2519] = -87;
        Arrays.fill(CHARS, 2520, 2524, (byte) 33 ); // Fill 4 of value (byte) 33
        Arrays.fill(CHARS, 2524, 2526, (byte) -19 ); // Fill 2 of value (byte) -19
        CHARS[2526] = 33;
        Arrays.fill(CHARS, 2527, 2530, (byte) -19 ); // Fill 3 of value (byte) -19
        Arrays.fill(CHARS, 2530, 2532, (byte) -87 ); // Fill 2 of value (byte) -87
        Arrays.fill(CHARS, 2532, 2534, (byte) 33 ); // Fill 2 of value (byte) 33
        Arrays.fill(CHARS, 2534, 2544, (byte) -87 ); // Fill 10 of value (byte) -87
        Arrays.fill(CHARS, 2544, 2546, (byte) -19 ); // Fill 2 of value (byte) -19
        Arrays.fill(CHARS, 2546, 2562, (byte) 33 ); // Fill 16 of value (byte) 33
        CHARS[2562] = -87;
        Arrays.fill(CHARS, 2563, 2565, (byte) 33 ); // Fill 2 of value (byte) 33
        Arrays.fill(CHARS, 2565, 2571, (byte) -19 ); // Fill 6 of value (byte) -19
        Arrays.fill(CHARS, 2571, 2575, (byte) 33 ); // Fill 4 of value (byte) 33
        Arrays.fill(CHARS, 2575, 2577, (byte) -19 ); // Fill 2 of value (byte) -19
        Arrays.fill(CHARS, 2577, 2579, (byte) 33 ); // Fill 2 of value (byte) 33
        Arrays.fill(CHARS, 2579, 2601, (byte) -19 ); // Fill 22 of value (byte) -19
        CHARS[2601] = 33;
        Arrays.fill(CHARS, 2602, 2609, (byte) -19 ); // Fill 7 of value (byte) -19
        CHARS[2609] = 33;
        Arrays.fill(CHARS, 2610, 2612, (byte) -19 ); // Fill 2 of value (byte) -19
        CHARS[2612] = 33;
        Arrays.fill(CHARS, 2613, 2615, (byte) -19 ); // Fill 2 of value (byte) -19
        CHARS[2615] = 33;
        Arrays.fill(CHARS, 2616, 2618, (byte) -19 ); // Fill 2 of value (byte) -19
        Arrays.fill(CHARS, 2618, 2620, (byte) 33 ); // Fill 2 of value (byte) 33
        CHARS[2620] = -87;
        CHARS[2621] = 33;
        Arrays.fill(CHARS, 2622, 2627, (byte) -87 ); // Fill 5 of value (byte) -87
        Arrays.fill(CHARS, 2627, 2631, (byte) 33 ); // Fill 4 of value (byte) 33
        Arrays.fill(CHARS, 2631, 2633, (byte) -87 ); // Fill 2 of value (byte) -87
        Arrays.fill(CHARS, 2633, 2635, (byte) 33 ); // Fill 2 of value (byte) 33
        Arrays.fill(CHARS, 2635, 2638, (byte) -87 ); // Fill 3 of value (byte) -87
        Arrays.fill(CHARS, 2638, 2649, (byte) 33 ); // Fill 11 of value (byte) 33
        Arrays.fill(CHARS, 2649, 2653, (byte) -19 ); // Fill 4 of value (byte) -19
        CHARS[2653] = 33;
        CHARS[2654] = -19;
        Arrays.fill(CHARS, 2655, 2662, (byte) 33 ); // Fill 7 of value (byte) 33
        Arrays.fill(CHARS, 2662, 2674, (byte) -87 ); // Fill 12 of value (byte) -87
        Arrays.fill(CHARS, 2674, 2677, (byte) -19 ); // Fill 3 of value (byte) -19
        Arrays.fill(CHARS, 2677, 2689, (byte) 33 ); // Fill 12 of value (byte) 33
        Arrays.fill(CHARS, 2689, 2692, (byte) -87 ); // Fill 3 of value (byte) -87
        CHARS[2692] = 33;
        Arrays.fill(CHARS, 2693, 2700, (byte) -19 ); // Fill 7 of value (byte) -19
        CHARS[2700] = 33;
        CHARS[2701] = -19;
        CHARS[2702] = 33;
        Arrays.fill(CHARS, 2703, 2706, (byte) -19 ); // Fill 3 of value (byte) -19
        CHARS[2706] = 33;
        Arrays.fill(CHARS, 2707, 2729, (byte) -19 ); // Fill 22 of value (byte) -19
        CHARS[2729] = 33;
        Arrays.fill(CHARS, 2730, 2737, (byte) -19 ); // Fill 7 of value (byte) -19
        CHARS[2737] = 33;
        Arrays.fill(CHARS, 2738, 2740, (byte) -19 ); // Fill 2 of value (byte) -19
        CHARS[2740] = 33;
        Arrays.fill(CHARS, 2741, 2746, (byte) -19 ); // Fill 5 of value (byte) -19
        Arrays.fill(CHARS, 2746, 2748, (byte) 33 ); // Fill 2 of value (byte) 33
        CHARS[2748] = -87;
        CHARS[2749] = -19;
        Arrays.fill(CHARS, 2750, 2758, (byte) -87 ); // Fill 8 of value (byte) -87
        CHARS[2758] = 33;
        Arrays.fill(CHARS, 2759, 2762, (byte) -87 ); // Fill 3 of value (byte) -87
        CHARS[2762] = 33;
        Arrays.fill(CHARS, 2763, 2766, (byte) -87 ); // Fill 3 of value (byte) -87
        Arrays.fill(CHARS, 2766, 2784, (byte) 33 ); // Fill 18 of value (byte) 33
        CHARS[2784] = -19;
        Arrays.fill(CHARS, 2785, 2790, (byte) 33 ); // Fill 5 of value (byte) 33
        Arrays.fill(CHARS, 2790, 2800, (byte) -87 ); // Fill 10 of value (byte) -87
        Arrays.fill(CHARS, 2800, 2817, (byte) 33 ); // Fill 17 of value (byte) 33
        Arrays.fill(CHARS, 2817, 2820, (byte) -87 ); // Fill 3 of value (byte) -87
        CHARS[2820] = 33;
        Arrays.fill(CHARS, 2821, 2829, (byte) -19 ); // Fill 8 of value (byte) -19
        Arrays.fill(CHARS, 2829, 2831, (byte) 33 ); // Fill 2 of value (byte) 33
        Arrays.fill(CHARS, 2831, 2833, (byte) -19 ); // Fill 2 of value (byte) -19
        Arrays.fill(CHARS, 2833, 2835, (byte) 33 ); // Fill 2 of value (byte) 33
        Arrays.fill(CHARS, 2835, 2857, (byte) -19 ); // Fill 22 of value (byte) -19
        CHARS[2857] = 33;
        Arrays.fill(CHARS, 2858, 2865, (byte) -19 ); // Fill 7 of value (byte) -19
        CHARS[2865] = 33;
        Arrays.fill(CHARS, 2866, 2868, (byte) -19 ); // Fill 2 of value (byte) -19
        Arrays.fill(CHARS, 2868, 2870, (byte) 33 ); // Fill 2 of value (byte) 33
        Arrays.fill(CHARS, 2870, 2874, (byte) -19 ); // Fill 4 of value (byte) -19
        Arrays.fill(CHARS, 2874, 2876, (byte) 33 ); // Fill 2 of value (byte) 33
        CHARS[2876] = -87;
        CHARS[2877] = -19;
        Arrays.fill(CHARS, 2878, 2884, (byte) -87 ); // Fill 6 of value (byte) -87
        Arrays.fill(CHARS, 2884, 2887, (byte) 33 ); // Fill 3 of value (byte) 33
        Arrays.fill(CHARS, 2887, 2889, (byte) -87 ); // Fill 2 of value (byte) -87
        Arrays.fill(CHARS, 2889, 2891, (byte) 33 ); // Fill 2 of value (byte) 33
        Arrays.fill(CHARS, 2891, 2894, (byte) -87 ); // Fill 3 of value (byte) -87
        Arrays.fill(CHARS, 2894, 2902, (byte) 33 ); // Fill 8 of value (byte) 33
        Arrays.fill(CHARS, 2902, 2904, (byte) -87 ); // Fill 2 of value (byte) -87
        Arrays.fill(CHARS, 2904, 2908, (byte) 33 ); // Fill 4 of value (byte) 33
        Arrays.fill(CHARS, 2908, 2910, (byte) -19 ); // Fill 2 of value (byte) -19
        CHARS[2910] = 33;
        Arrays.fill(CHARS, 2911, 2914, (byte) -19 ); // Fill 3 of value (byte) -19
        Arrays.fill(CHARS, 2914, 2918, (byte) 33 ); // Fill 4 of value (byte) 33
        Arrays.fill(CHARS, 2918, 2928, (byte) -87 ); // Fill 10 of value (byte) -87
        Arrays.fill(CHARS, 2928, 2946, (byte) 33 ); // Fill 18 of value (byte) 33
        Arrays.fill(CHARS, 2946, 2948, (byte) -87 ); // Fill 2 of value (byte) -87
        CHARS[2948] = 33;
        Arrays.fill(CHARS, 2949, 2955, (byte) -19 ); // Fill 6 of value (byte) -19
        Arrays.fill(CHARS, 2955, 2958, (byte) 33 ); // Fill 3 of value (byte) 33
        Arrays.fill(CHARS, 2958, 2961, (byte) -19 ); // Fill 3 of value (byte) -19
        CHARS[2961] = 33;
        Arrays.fill(CHARS, 2962, 2966, (byte) -19 ); // Fill 4 of value (byte) -19
        Arrays.fill(CHARS, 2966, 2969, (byte) 33 ); // Fill 3 of value (byte) 33
        Arrays.fill(CHARS, 2969, 2971, (byte) -19 ); // Fill 2 of value (byte) -19
        CHARS[2971] = 33;
        CHARS[2972] = -19;
        CHARS[2973] = 33;
        Arrays.fill(CHARS, 2974, 2976, (byte) -19 ); // Fill 2 of value (byte) -19
        Arrays.fill(CHARS, 2976, 2979, (byte) 33 ); // Fill 3 of value (byte) 33
        Arrays.fill(CHARS, 2979, 2981, (byte) -19 ); // Fill 2 of value (byte) -19
        Arrays.fill(CHARS, 2981, 2984, (byte) 33 ); // Fill 3 of value (byte) 33
        Arrays.fill(CHARS, 2984, 2987, (byte) -19 ); // Fill 3 of value (byte) -19
        Arrays.fill(CHARS, 2987, 2990, (byte) 33 ); // Fill 3 of value (byte) 33
        Arrays.fill(CHARS, 2990, 2998, (byte) -19 ); // Fill 8 of value (byte) -19
        CHARS[2998] = 33;
        Arrays.fill(CHARS, 2999, 3002, (byte) -19 ); // Fill 3 of value (byte) -19
        Arrays.fill(CHARS, 3002, 3006, (byte) 33 ); // Fill 4 of value (byte) 33
        Arrays.fill(CHARS, 3006, 3011, (byte) -87 ); // Fill 5 of value (byte) -87
        Arrays.fill(CHARS, 3011, 3014, (byte) 33 ); // Fill 3 of value (byte) 33
        Arrays.fill(CHARS, 3014, 3017, (byte) -87 ); // Fill 3 of value (byte) -87
        CHARS[3017] = 33;
        Arrays.fill(CHARS, 3018, 3022, (byte) -87 ); // Fill 4 of value (byte) -87
        Arrays.fill(CHARS, 3022, 3031, (byte) 33 ); // Fill 9 of value (byte) 33
        CHARS[3031] = -87;
        Arrays.fill(CHARS, 3032, 3047, (byte) 33 ); // Fill 15 of value (byte) 33
        Arrays.fill(CHARS, 3047, 3056, (byte) -87 ); // Fill 9 of value (byte) -87
        Arrays.fill(CHARS, 3056, 3073, (byte) 33 ); // Fill 17 of value (byte) 33
        Arrays.fill(CHARS, 3073, 3076, (byte) -87 ); // Fill 3 of value (byte) -87
        CHARS[3076] = 33;
        Arrays.fill(CHARS, 3077, 3085, (byte) -19 ); // Fill 8 of value (byte) -19
        CHARS[3085] = 33;
        Arrays.fill(CHARS, 3086, 3089, (byte) -19 ); // Fill 3 of value (byte) -19
        CHARS[3089] = 33;
        Arrays.fill(CHARS, 3090, 3113, (byte) -19 ); // Fill 23 of value (byte) -19
        CHARS[3113] = 33;
        Arrays.fill(CHARS, 3114, 3124, (byte) -19 ); // Fill 10 of value (byte) -19
        CHARS[3124] = 33;
        Arrays.fill(CHARS, 3125, 3130, (byte) -19 ); // Fill 5 of value (byte) -19
        Arrays.fill(CHARS, 3130, 3134, (byte) 33 ); // Fill 4 of value (byte) 33
        Arrays.fill(CHARS, 3134, 3141, (byte) -87 ); // Fill 7 of value (byte) -87
        CHARS[3141] = 33;
        Arrays.fill(CHARS, 3142, 3145, (byte) -87 ); // Fill 3 of value (byte) -87
        CHARS[3145] = 33;
        Arrays.fill(CHARS, 3146, 3150, (byte) -87 ); // Fill 4 of value (byte) -87
        Arrays.fill(CHARS, 3150, 3157, (byte) 33 ); // Fill 7 of value (byte) 33
        Arrays.fill(CHARS, 3157, 3159, (byte) -87 ); // Fill 2 of value (byte) -87
        Arrays.fill(CHARS, 3159, 3168, (byte) 33 ); // Fill 9 of value (byte) 33
        Arrays.fill(CHARS, 3168, 3170, (byte) -19 ); // Fill 2 of value (byte) -19
        Arrays.fill(CHARS, 3170, 3174, (byte) 33 ); // Fill 4 of value (byte) 33
        Arrays.fill(CHARS, 3174, 3184, (byte) -87 ); // Fill 10 of value (byte) -87
        Arrays.fill(CHARS, 3184, 3202, (byte) 33 ); // Fill 18 of value (byte) 33
        Arrays.fill(CHARS, 3202, 3204, (byte) -87 ); // Fill 2 of value (byte) -87
        CHARS[3204] = 33;
        Arrays.fill(CHARS, 3205, 3213, (byte) -19 ); // Fill 8 of value (byte) -19
        CHARS[3213] = 33;
        Arrays.fill(CHARS, 3214, 3217, (byte) -19 ); // Fill 3 of value (byte) -19
        CHARS[3217] = 33;
        Arrays.fill(CHARS, 3218, 3241, (byte) -19 ); // Fill 23 of value (byte) -19
        CHARS[3241] = 33;
        Arrays.fill(CHARS, 3242, 3252, (byte) -19 ); // Fill 10 of value (byte) -19
        CHARS[3252] = 33;
        Arrays.fill(CHARS, 3253, 3258, (byte) -19 ); // Fill 5 of value (byte) -19
        Arrays.fill(CHARS, 3258, 3262, (byte) 33 ); // Fill 4 of value (byte) 33
        Arrays.fill(CHARS, 3262, 3269, (byte) -87 ); // Fill 7 of value (byte) -87
        CHARS[3269] = 33;
        Arrays.fill(CHARS, 3270, 3273, (byte) -87 ); // Fill 3 of value (byte) -87
        CHARS[3273] = 33;
        Arrays.fill(CHARS, 3274, 3278, (byte) -87 ); // Fill 4 of value (byte) -87
        Arrays.fill(CHARS, 3278, 3285, (byte) 33 ); // Fill 7 of value (byte) 33
        Arrays.fill(CHARS, 3285, 3287, (byte) -87 ); // Fill 2 of value (byte) -87
        Arrays.fill(CHARS, 3287, 3294, (byte) 33 ); // Fill 7 of value (byte) 33
        CHARS[3294] = -19;
        CHARS[3295] = 33;
        Arrays.fill(CHARS, 3296, 3298, (byte) -19 ); // Fill 2 of value (byte) -19
        Arrays.fill(CHARS, 3298, 3302, (byte) 33 ); // Fill 4 of value (byte) 33
        Arrays.fill(CHARS, 3302, 3312, (byte) -87 ); // Fill 10 of value (byte) -87
        Arrays.fill(CHARS, 3312, 3330, (byte) 33 ); // Fill 18 of value (byte) 33
        Arrays.fill(CHARS, 3330, 3332, (byte) -87 ); // Fill 2 of value (byte) -87
        CHARS[3332] = 33;
        Arrays.fill(CHARS, 3333, 3341, (byte) -19 ); // Fill 8 of value (byte) -19
        CHARS[3341] = 33;
        Arrays.fill(CHARS, 3342, 3345, (byte) -19 ); // Fill 3 of value (byte) -19
        CHARS[3345] = 33;
        Arrays.fill(CHARS, 3346, 3369, (byte) -19 ); // Fill 23 of value (byte) -19
        CHARS[3369] = 33;
        Arrays.fill(CHARS, 3370, 3386, (byte) -19 ); // Fill 16 of value (byte) -19
        Arrays.fill(CHARS, 3386, 3390, (byte) 33 ); // Fill 4 of value (byte) 33
        Arrays.fill(CHARS, 3390, 3396, (byte) -87 ); // Fill 6 of value (byte) -87
        Arrays.fill(CHARS, 3396, 3398, (byte) 33 ); // Fill 2 of value (byte) 33
        Arrays.fill(CHARS, 3398, 3401, (byte) -87 ); // Fill 3 of value (byte) -87
        CHARS[3401] = 33;
        Arrays.fill(CHARS, 3402, 3406, (byte) -87 ); // Fill 4 of value (byte) -87
        Arrays.fill(CHARS, 3406, 3415, (byte) 33 ); // Fill 9 of value (byte) 33
        CHARS[3415] = -87;
        Arrays.fill(CHARS, 3416, 3424, (byte) 33 ); // Fill 8 of value (byte) 33
        Arrays.fill(CHARS, 3424, 3426, (byte) -19 ); // Fill 2 of value (byte) -19
        Arrays.fill(CHARS, 3426, 3430, (byte) 33 ); // Fill 4 of value (byte) 33
        Arrays.fill(CHARS, 3430, 3440, (byte) -87 ); // Fill 10 of value (byte) -87
        Arrays.fill(CHARS, 3440, 3585, (byte) 33 ); // Fill 145 of value (byte) 33
        Arrays.fill(CHARS, 3585, 3631, (byte) -19 ); // Fill 46 of value (byte) -19
        CHARS[3631] = 33;
        CHARS[3632] = -19;
        CHARS[3633] = -87;
        Arrays.fill(CHARS, 3634, 3636, (byte) -19 ); // Fill 2 of value (byte) -19
        Arrays.fill(CHARS, 3636, 3643, (byte) -87 ); // Fill 7 of value (byte) -87
        Arrays.fill(CHARS, 3643, 3648, (byte) 33 ); // Fill 5 of value (byte) 33
        Arrays.fill(CHARS, 3648, 3654, (byte) -19 ); // Fill 6 of value (byte) -19
        Arrays.fill(CHARS, 3654, 3663, (byte) -87 ); // Fill 9 of value (byte) -87
        CHARS[3663] = 33;
        Arrays.fill(CHARS, 3664, 3674, (byte) -87 ); // Fill 10 of value (byte) -87
        Arrays.fill(CHARS, 3674, 3713, (byte) 33 ); // Fill 39 of value (byte) 33
        Arrays.fill(CHARS, 3713, 3715, (byte) -19 ); // Fill 2 of value (byte) -19
        CHARS[3715] = 33;
        CHARS[3716] = -19;
        Arrays.fill(CHARS, 3717, 3719, (byte) 33 ); // Fill 2 of value (byte) 33
        Arrays.fill(CHARS, 3719, 3721, (byte) -19 ); // Fill 2 of value (byte) -19
        CHARS[3721] = 33;
        CHARS[3722] = -19;
        Arrays.fill(CHARS, 3723, 3725, (byte) 33 ); // Fill 2 of value (byte) 33
        CHARS[3725] = -19;
        Arrays.fill(CHARS, 3726, 3732, (byte) 33 ); // Fill 6 of value (byte) 33
        Arrays.fill(CHARS, 3732, 3736, (byte) -19 ); // Fill 4 of value (byte) -19
        CHARS[3736] = 33;
        Arrays.fill(CHARS, 3737, 3744, (byte) -19 ); // Fill 7 of value (byte) -19
        CHARS[3744] = 33;
        Arrays.fill(CHARS, 3745, 3748, (byte) -19 ); // Fill 3 of value (byte) -19
        CHARS[3748] = 33;
        CHARS[3749] = -19;
        CHARS[3750] = 33;
        CHARS[3751] = -19;
        Arrays.fill(CHARS, 3752, 3754, (byte) 33 ); // Fill 2 of value (byte) 33
        Arrays.fill(CHARS, 3754, 3756, (byte) -19 ); // Fill 2 of value (byte) -19
        CHARS[3756] = 33;
        Arrays.fill(CHARS, 3757, 3759, (byte) -19 ); // Fill 2 of value (byte) -19
        CHARS[3759] = 33;
        CHARS[3760] = -19;
        CHARS[3761] = -87;
        Arrays.fill(CHARS, 3762, 3764, (byte) -19 ); // Fill 2 of value (byte) -19
        Arrays.fill(CHARS, 3764, 3770, (byte) -87 ); // Fill 6 of value (byte) -87
        CHARS[3770] = 33;
        Arrays.fill(CHARS, 3771, 3773, (byte) -87 ); // Fill 2 of value (byte) -87
        CHARS[3773] = -19;
        Arrays.fill(CHARS, 3774, 3776, (byte) 33 ); // Fill 2 of value (byte) 33
        Arrays.fill(CHARS, 3776, 3781, (byte) -19 ); // Fill 5 of value (byte) -19
        CHARS[3781] = 33;
        CHARS[3782] = -87;
        CHARS[3783] = 33;
        Arrays.fill(CHARS, 3784, 3790, (byte) -87 ); // Fill 6 of value (byte) -87
        Arrays.fill(CHARS, 3790, 3792, (byte) 33 ); // Fill 2 of value (byte) 33
        Arrays.fill(CHARS, 3792, 3802, (byte) -87 ); // Fill 10 of value (byte) -87
        Arrays.fill(CHARS, 3802, 3864, (byte) 33 ); // Fill 62 of value (byte) 33
        Arrays.fill(CHARS, 3864, 3866, (byte) -87 ); // Fill 2 of value (byte) -87
        Arrays.fill(CHARS, 3866, 3872, (byte) 33 ); // Fill 6 of value (byte) 33
        Arrays.fill(CHARS, 3872, 3882, (byte) -87 ); // Fill 10 of value (byte) -87
        Arrays.fill(CHARS, 3882, 3893, (byte) 33 ); // Fill 11 of value (byte) 33
        CHARS[3893] = -87;
        CHARS[3894] = 33;
        CHARS[3895] = -87;
        CHARS[3896] = 33;
        CHARS[3897] = -87;
        Arrays.fill(CHARS, 3898, 3902, (byte) 33 ); // Fill 4 of value (byte) 33
        Arrays.fill(CHARS, 3902, 3904, (byte) -87 ); // Fill 2 of value (byte) -87
        Arrays.fill(CHARS, 3904, 3912, (byte) -19 ); // Fill 8 of value (byte) -19
        CHARS[3912] = 33;
        Arrays.fill(CHARS, 3913, 3946, (byte) -19 ); // Fill 33 of value (byte) -19
        Arrays.fill(CHARS, 3946, 3953, (byte) 33 ); // Fill 7 of value (byte) 33
        Arrays.fill(CHARS, 3953, 3973, (byte) -87 ); // Fill 20 of value (byte) -87
        CHARS[3973] = 33;
        Arrays.fill(CHARS, 3974, 3980, (byte) -87 ); // Fill 6 of value (byte) -87
        Arrays.fill(CHARS, 3980, 3984, (byte) 33 ); // Fill 4 of value (byte) 33
        Arrays.fill(CHARS, 3984, 3990, (byte) -87 ); // Fill 6 of value (byte) -87
        CHARS[3990] = 33;
        CHARS[3991] = -87;
        CHARS[3992] = 33;
        Arrays.fill(CHARS, 3993, 4014, (byte) -87 ); // Fill 21 of value (byte) -87
        Arrays.fill(CHARS, 4014, 4017, (byte) 33 ); // Fill 3 of value (byte) 33
        Arrays.fill(CHARS, 4017, 4024, (byte) -87 ); // Fill 7 of value (byte) -87
        CHARS[4024] = 33;
        CHARS[4025] = -87;
        Arrays.fill(CHARS, 4026, 4256, (byte) 33 ); // Fill 230 of value (byte) 33
        Arrays.fill(CHARS, 4256, 4294, (byte) -19 ); // Fill 38 of value (byte) -19
        Arrays.fill(CHARS, 4294, 4304, (byte) 33 ); // Fill 10 of value (byte) 33
        Arrays.fill(CHARS, 4304, 4343, (byte) -19 ); // Fill 39 of value (byte) -19
        Arrays.fill(CHARS, 4343, 4352, (byte) 33 ); // Fill 9 of value (byte) 33
        CHARS[4352] = -19;
        CHARS[4353] = 33;
        Arrays.fill(CHARS, 4354, 4356, (byte) -19 ); // Fill 2 of value (byte) -19
        CHARS[4356] = 33;
        Arrays.fill(CHARS, 4357, 4360, (byte) -19 ); // Fill 3 of value (byte) -19
        CHARS[4360] = 33;
        CHARS[4361] = -19;
        CHARS[4362] = 33;
        Arrays.fill(CHARS, 4363, 4365, (byte) -19 ); // Fill 2 of value (byte) -19
        CHARS[4365] = 33;
        Arrays.fill(CHARS, 4366, 4371, (byte) -19 ); // Fill 5 of value (byte) -19
        Arrays.fill(CHARS, 4371, 4412, (byte) 33 ); // Fill 41 of value (byte) 33
        CHARS[4412] = -19;
        CHARS[4413] = 33;
        CHARS[4414] = -19;
        CHARS[4415] = 33;
        CHARS[4416] = -19;
        Arrays.fill(CHARS, 4417, 4428, (byte) 33 ); // Fill 11 of value (byte) 33
        CHARS[4428] = -19;
        CHARS[4429] = 33;
        CHARS[4430] = -19;
        CHARS[4431] = 33;
        CHARS[4432] = -19;
        Arrays.fill(CHARS, 4433, 4436, (byte) 33 ); // Fill 3 of value (byte) 33
        Arrays.fill(CHARS, 4436, 4438, (byte) -19 ); // Fill 2 of value (byte) -19
        Arrays.fill(CHARS, 4438, 4441, (byte) 33 ); // Fill 3 of value (byte) 33
        CHARS[4441] = -19;
        Arrays.fill(CHARS, 4442, 4447, (byte) 33 ); // Fill 5 of value (byte) 33
        Arrays.fill(CHARS, 4447, 4450, (byte) -19 ); // Fill 3 of value (byte) -19
        CHARS[4450] = 33;
        CHARS[4451] = -19;
        CHARS[4452] = 33;
        CHARS[4453] = -19;
        CHARS[4454] = 33;
        CHARS[4455] = -19;
        CHARS[4456] = 33;
        CHARS[4457] = -19;
        Arrays.fill(CHARS, 4458, 4461, (byte) 33 ); // Fill 3 of value (byte) 33
        Arrays.fill(CHARS, 4461, 4463, (byte) -19 ); // Fill 2 of value (byte) -19
        Arrays.fill(CHARS, 4463, 4466, (byte) 33 ); // Fill 3 of value (byte) 33
        Arrays.fill(CHARS, 4466, 4468, (byte) -19 ); // Fill 2 of value (byte) -19
        CHARS[4468] = 33;
        CHARS[4469] = -19;
        Arrays.fill(CHARS, 4470, 4510, (byte) 33 ); // Fill 40 of value (byte) 33
        CHARS[4510] = -19;
        Arrays.fill(CHARS, 4511, 4520, (byte) 33 ); // Fill 9 of value (byte) 33
        CHARS[4520] = -19;
        Arrays.fill(CHARS, 4521, 4523, (byte) 33 ); // Fill 2 of value (byte) 33
        CHARS[4523] = -19;
        Arrays.fill(CHARS, 4524, 4526, (byte) 33 ); // Fill 2 of value (byte) 33
        Arrays.fill(CHARS, 4526, 4528, (byte) -19 ); // Fill 2 of value (byte) -19
        Arrays.fill(CHARS, 4528, 4535, (byte) 33 ); // Fill 7 of value (byte) 33
        Arrays.fill(CHARS, 4535, 4537, (byte) -19 ); // Fill 2 of value (byte) -19
        CHARS[4537] = 33;
        CHARS[4538] = -19;
        CHARS[4539] = 33;
        Arrays.fill(CHARS, 4540, 4547, (byte) -19 ); // Fill 7 of value (byte) -19
        Arrays.fill(CHARS, 4547, 4587, (byte) 33 ); // Fill 40 of value (byte) 33
        CHARS[4587] = -19;
        Arrays.fill(CHARS, 4588, 4592, (byte) 33 ); // Fill 4 of value (byte) 33
        CHARS[4592] = -19;
        Arrays.fill(CHARS, 4593, 4601, (byte) 33 ); // Fill 8 of value (byte) 33
        CHARS[4601] = -19;
        Arrays.fill(CHARS, 4602, 7680, (byte) 33 ); // Fill 3078 of value (byte) 33
        Arrays.fill(CHARS, 7680, 7836, (byte) -19 ); // Fill 156 of value (byte) -19
        Arrays.fill(CHARS, 7836, 7840, (byte) 33 ); // Fill 4 of value (byte) 33
        Arrays.fill(CHARS, 7840, 7930, (byte) -19 ); // Fill 90 of value (byte) -19
        Arrays.fill(CHARS, 7930, 7936, (byte) 33 ); // Fill 6 of value (byte) 33
        Arrays.fill(CHARS, 7936, 7958, (byte) -19 ); // Fill 22 of value (byte) -19
        Arrays.fill(CHARS, 7958, 7960, (byte) 33 ); // Fill 2 of value (byte) 33
        Arrays.fill(CHARS, 7960, 7966, (byte) -19 ); // Fill 6 of value (byte) -19
        Arrays.fill(CHARS, 7966, 7968, (byte) 33 ); // Fill 2 of value (byte) 33
        Arrays.fill(CHARS, 7968, 8006, (byte) -19 ); // Fill 38 of value (byte) -19
        Arrays.fill(CHARS, 8006, 8008, (byte) 33 ); // Fill 2 of value (byte) 33
        Arrays.fill(CHARS, 8008, 8014, (byte) -19 ); // Fill 6 of value (byte) -19
        Arrays.fill(CHARS, 8014, 8016, (byte) 33 ); // Fill 2 of value (byte) 33
        Arrays.fill(CHARS, 8016, 8024, (byte) -19 ); // Fill 8 of value (byte) -19
        CHARS[8024] = 33;
        CHARS[8025] = -19;
        CHARS[8026] = 33;
        CHARS[8027] = -19;
        CHARS[8028] = 33;
        CHARS[8029] = -19;
        CHARS[8030] = 33;
        Arrays.fill(CHARS, 8031, 8062, (byte) -19 ); // Fill 31 of value (byte) -19
        Arrays.fill(CHARS, 8062, 8064, (byte) 33 ); // Fill 2 of value (byte) 33
        Arrays.fill(CHARS, 8064, 8117, (byte) -19 ); // Fill 53 of value (byte) -19
        CHARS[8117] = 33;
        Arrays.fill(CHARS, 8118, 8125, (byte) -19 ); // Fill 7 of value (byte) -19
        CHARS[8125] = 33;
        CHARS[8126] = -19;
        Arrays.fill(CHARS, 8127, 8130, (byte) 33 ); // Fill 3 of value (byte) 33
        Arrays.fill(CHARS, 8130, 8133, (byte) -19 ); // Fill 3 of value (byte) -19
        CHARS[8133] = 33;
        Arrays.fill(CHARS, 8134, 8141, (byte) -19 ); // Fill 7 of value (byte) -19
        Arrays.fill(CHARS, 8141, 8144, (byte) 33 ); // Fill 3 of value (byte) 33
        Arrays.fill(CHARS, 8144, 8148, (byte) -19 ); // Fill 4 of value (byte) -19
        Arrays.fill(CHARS, 8148, 8150, (byte) 33 ); // Fill 2 of value (byte) 33
        Arrays.fill(CHARS, 8150, 8156, (byte) -19 ); // Fill 6 of value (byte) -19
        Arrays.fill(CHARS, 8156, 8160, (byte) 33 ); // Fill 4 of value (byte) 33
        Arrays.fill(CHARS, 8160, 8173, (byte) -19 ); // Fill 13 of value (byte) -19
        Arrays.fill(CHARS, 8173, 8178, (byte) 33 ); // Fill 5 of value (byte) 33
        Arrays.fill(CHARS, 8178, 8181, (byte) -19 ); // Fill 3 of value (byte) -19
        CHARS[8181] = 33;
        Arrays.fill(CHARS, 8182, 8189, (byte) -19 ); // Fill 7 of value (byte) -19
        Arrays.fill(CHARS, 8189, 8400, (byte) 33 ); // Fill 211 of value (byte) 33
        Arrays.fill(CHARS, 8400, 8413, (byte) -87 ); // Fill 13 of value (byte) -87
        Arrays.fill(CHARS, 8413, 8417, (byte) 33 ); // Fill 4 of value (byte) 33
        CHARS[8417] = -87;
        Arrays.fill(CHARS, 8418, 8486, (byte) 33 ); // Fill 68 of value (byte) 33
        CHARS[8486] = -19;
        Arrays.fill(CHARS, 8487, 8490, (byte) 33 ); // Fill 3 of value (byte) 33
        Arrays.fill(CHARS, 8490, 8492, (byte) -19 ); // Fill 2 of value (byte) -19
        Arrays.fill(CHARS, 8492, 8494, (byte) 33 ); // Fill 2 of value (byte) 33
        CHARS[8494] = -19;
        Arrays.fill(CHARS, 8495, 8576, (byte) 33 ); // Fill 81 of value (byte) 33
        Arrays.fill(CHARS, 8576, 8579, (byte) -19 ); // Fill 3 of value (byte) -19
        Arrays.fill(CHARS, 8579, 12293, (byte) 33 ); // Fill 3714 of value (byte) 33
        CHARS[12293] = -87;
        CHARS[12294] = 33;
        CHARS[12295] = -19;
        Arrays.fill(CHARS, 12296, 12321, (byte) 33 ); // Fill 25 of value (byte) 33
        Arrays.fill(CHARS, 12321, 12330, (byte) -19 ); // Fill 9 of value (byte) -19
        Arrays.fill(CHARS, 12330, 12336, (byte) -87 ); // Fill 6 of value (byte) -87
        CHARS[12336] = 33;
        Arrays.fill(CHARS, 12337, 12342, (byte) -87 ); // Fill 5 of value (byte) -87
        Arrays.fill(CHARS, 12342, 12353, (byte) 33 ); // Fill 11 of value (byte) 33
        Arrays.fill(CHARS, 12353, 12437, (byte) -19 ); // Fill 84 of value (byte) -19
        Arrays.fill(CHARS, 12437, 12441, (byte) 33 ); // Fill 4 of value (byte) 33
        Arrays.fill(CHARS, 12441, 12443, (byte) -87 ); // Fill 2 of value (byte) -87
        Arrays.fill(CHARS, 12443, 12445, (byte) 33 ); // Fill 2 of value (byte) 33
        Arrays.fill(CHARS, 12445, 12447, (byte) -87 ); // Fill 2 of value (byte) -87
        Arrays.fill(CHARS, 12447, 12449, (byte) 33 ); // Fill 2 of value (byte) 33
        Arrays.fill(CHARS, 12449, 12539, (byte) -19 ); // Fill 90 of value (byte) -19
        CHARS[12539] = 33;
        Arrays.fill(CHARS, 12540, 12543, (byte) -87 ); // Fill 3 of value (byte) -87
        Arrays.fill(CHARS, 12543, 12549, (byte) 33 ); // Fill 6 of value (byte) 33
        Arrays.fill(CHARS, 12549, 12589, (byte) -19 ); // Fill 40 of value (byte) -19
        Arrays.fill(CHARS, 12589, 19968, (byte) 33 ); // Fill 7379 of value (byte) 33
        Arrays.fill(CHARS, 19968, 40870, (byte) -19 ); // Fill 20902 of value (byte) -19
        Arrays.fill(CHARS, 40870, 44032, (byte) 33 ); // Fill 3162 of value (byte) 33
        Arrays.fill(CHARS, 44032, 55204, (byte) -19 ); // Fill 11172 of value (byte) -19
        Arrays.fill(CHARS, 55204, 55296, (byte) 33 ); // Fill 92 of value (byte) 33
        Arrays.fill(CHARS, 57344, 65534, (byte) 33 ); // Fill 8190 of value (byte) 33
 
     } // <clinit>()
 
@@ -485,7 +827,7 @@ public class XMLChar {
      * @param c The character to check.
      */
     public static boolean isSpace(int c) {
        return c < 0x10000 && (CHARS[c] & MASK_SPACE) != 0;
        return c <= 0x20 && (CHARS[c] & MASK_SPACE) != 0;
     } // isSpace(int):boolean
 
     /**
@@ -567,7 +909,7 @@ public class XMLChar {
         }
         return true;
     } // isValidName(String):boolean

    
 
     /*
      * from the namespace rec
@@ -577,7 +919,7 @@ public class XMLChar {
      * Check to see if a string is a valid NCName according to [4]
      * from the XML Namespaces 1.0 Recommendation
      *
     * @param name string to check
     * @param ncName string to check
      * @return true if name is a valid NCName
      */
     public static boolean isValidNCName(String ncName) {
@@ -603,7 +945,7 @@ public class XMLChar {
      * in the XML 1.0 Recommendation
      *
      * @param nmtoken string to check
     * @return true if nmtoken is a valid Nmtoken
     * @return true if nmtoken is a valid Nmtoken 
      */
     public static boolean isValidNmtoken(String nmtoken) {
         if (nmtoken.length() == 0)
@@ -678,4 +1020,5 @@ public class XMLChar {
         return false;
     } // isValidIANAEncoding(String):boolean
 

 } // class XMLChar
- 
2.19.1.windows.1

