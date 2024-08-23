package org.kc7bfi.jflac.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;


class RingBufferTest {

    @Test
    void test1() {
        RingBuffer r = new RingBuffer(9);
        byte[] b = "ABCDEFG".getBytes();
        byte[] g = new byte[2];
        System.out.println("Start");
        r.put(b, 0, 3);
        r.get(g, 0, 2);
        System.out.println(new String(g));
        assertEquals("AB", new String(g));
        r.put(b, 0, 3);
        r.get(g, 0, 2);
        System.out.println(new String(g));
        assertEquals("CA", new String(g));
        r.put(b, 0, 3);
        r.get(g, 0, 2);
        System.out.println(new String(g));
        assertEquals("BC", new String(g));
        r.put(b, 0, 3);
        r.get(g, 0, 2);
        System.out.println(new String(g));
        assertEquals("AB", new String(g));
        r.put(b, 0, 3);
        r.get(g, 0, 2);
        System.out.println(new String(g));
        assertEquals("CA", new String(g));
        r.put(b, 0, 3);
        r.get(g, 0, 2);
        System.out.println(new String(g));
        assertEquals("BC", new String(g));
//        r.put(b, 0, 3);
//        r.get(g, 0, 2);
//        System.out.println(new String(g));
//        assertEquals("AB", new String(g));
//        r.put(b, 0, 3);
//        r.get(g, 0, 2);
//        System.out.println(new String(g));
//        assertEquals("AB", new String(g));
    }
}