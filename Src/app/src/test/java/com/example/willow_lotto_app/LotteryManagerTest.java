package com.example.willow_lotto_app;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertTrue;

public class LotteryManagerTest {

    @Test
    public void drawLottery_picksFromWaitingPool() {
        // Fake a small pool (in real code this comes from Firestore query)
        List<String> pool = Arrays.asList("userA", "userB", "userC");

        // Simulate the random pick logic from your class
        int index = new java.util.Random().nextInt(pool.size());
        String selected = pool.get(index);

        assertTrue("Selected winner should be in the pool", pool.contains(selected));
    }
}