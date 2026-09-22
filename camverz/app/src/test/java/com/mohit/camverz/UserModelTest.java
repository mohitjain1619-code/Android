package com.mohit.camverz;

import org.junit.Test;
import static org.junit.Assert.*;

public class UserModelTest {

    @Test
    public void testUserDefaultConstructorAndNullSafety() {
        User user = new User();
        assertNull(user.getUid());
        assertNull(user.getName());
        assertEquals(0, user.getFollowersCount());
        assertEquals(0, user.getFollowingCount());
        assertFalse(user.isFollowedByMe());
    }

    @Test
    public void testUserSettersAndGetters() {
        User user = new User();
        user.setFollowersCount(42);
        user.setFollowingCount(10);
        user.setFollowedByMe(true);

        assertEquals(42, user.getFollowersCount());
        assertEquals(10, user.getFollowingCount());
        assertTrue(user.isFollowedByMe());
    }
}
