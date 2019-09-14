package org.apache.hadoop.yarn.server.nodemanager;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.yarn.conf.YarnConfiguration;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestSecureModeLocalUserAllocator {
  static Configuration conf;

  @BeforeClass
  public static void beforeAllTestMethods() {
    conf = new Configuration();
    conf.setBoolean(YarnConfiguration.NM_SECURE_MODE_USE_LOCAL_USER, true);
    conf.set(YarnConfiguration.NM_SECURE_MODE_LOCAL_USER_PREFIX, "smlu");
    conf.setBoolean(YarnConfiguration.NM_RECOVERY_ENABLED, false);
    conf.setInt(YarnConfiguration.NM_VCORES, 3);
  }

  @Test
  public void testSingleUserRefCounting() {
    SecureModeLocalUserAllocator allocator = new SecureModeLocalUserAllocator(
        conf);
    // non existing mapping won't hurt
    allocator.deallocate("user0", "app0");
    allocator.decrementFileOpCount("user0");
    allocator.incrementFileOpCount("user0");
    allocator.decrementLogHandlingCount("user0");
    allocator.incrementLogHandlingCount("user0");
    Assert.assertNull(allocator.getRunAsLocalUser("user0"));

    // as long as not all ref counts are 0, the mapping stays in memory
    allocator.allocate("user0", "app0");
    Assert.assertEquals("smlu0", allocator.getRunAsLocalUser("user0"));
    allocator.incrementFileOpCount("user0");
    allocator.deallocate("user0", "app0");
    Assert.assertEquals("smlu0", allocator.getRunAsLocalUser("user0"));
    allocator.incrementLogHandlingCount("user0");
    allocator.decrementFileOpCount("user0");
    Assert.assertEquals("smlu0", allocator.getRunAsLocalUser("user0"));
    allocator.decrementLogHandlingCount("user0");
    Assert.assertNull(allocator.getRunAsLocalUser("user0"));
  }

  @Test
  public void testMultiUserRefCounting() {
    SecureModeLocalUserAllocator allocator = new SecureModeLocalUserAllocator(
        conf);
    allocator.allocate("user0", "app0");
    allocator.allocate("user1", "app1");
    allocator.allocate("user2", "app2");

    allocator.incrementFileOpCount("user0");
    allocator.incrementLogHandlingCount("user1");

    Assert.assertEquals("smlu0", allocator.getRunAsLocalUser("user0"));
    Assert.assertEquals("smlu1", allocator.getRunAsLocalUser("user1"));
    Assert.assertEquals("smlu2", allocator.getRunAsLocalUser("user2"));

    allocator.deallocate("user0", "app0");
    allocator.deallocate("user1", "app1");
    allocator.deallocate("user2", "app2");

    Assert.assertEquals("smlu0", allocator.getRunAsLocalUser("user0"));
    Assert.assertEquals("smlu1", allocator.getRunAsLocalUser("user1"));
    Assert.assertNull(allocator.getRunAsLocalUser("user2"));

    allocator.decrementFileOpCount("user0");
    allocator.decrementLogHandlingCount("user1");
    Assert.assertNull(allocator.getRunAsLocalUser("user0"));
    Assert.assertNull(allocator.getRunAsLocalUser("user1"));
  }
}
