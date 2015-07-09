/*=========================================================================
 * Copyright (c) 2010-2014 Pivotal Software, Inc. All Rights Reserved.
 * This product is protected by U.S. and international copyright
 * and intellectual property laws. Pivotal products are covered by
 * one or more patents listed at http://www.pivotal.io/patents.
 *=========================================================================
 */
package com.gemstone.gemfire;

import static org.junit.Assert.*;

import com.gemstone.gemfire.distributed.internal.InternalDistributedSystem;
import com.gemstone.gemfire.distributed.internal.LonerDistributionManager;
import com.gemstone.gemfire.internal.*;
import com.gemstone.gemfire.distributed.*;
import com.gemstone.gemfire.distributed.internal.DistributionConfig;
import com.gemstone.gemfire.cache.*;
import com.gemstone.gemfire.test.junit.categories.IntegrationTest;

import java.util.Properties;
import java.net.*;

import org.junit.After;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * This class makes sure an isolated "loner" distribution manager
 * can be created and do some cache functions.
 */
@SuppressWarnings("deprecation")
@Category(IntegrationTest.class)
public class LonerDMJUnitTest {

  @After
  public void tearDown() {
    DistributedSystem ds = InternalDistributedSystem.getAnyInstance();
    if(ds != null) {
      ds.disconnect();
    }
  }

  @Test
  public void testLoner() throws CacheException {
    long start;
    long end;
    DistributedSystem ds = null;
    Cache c = null;
    Properties cfg = new Properties();
    cfg.setProperty("mcast-port", "0");
    cfg.setProperty("locators", "");
    cfg.setProperty("statistic-sampling-enabled", "false");

    for (int i=0; i < 2; i++) {
      start = System.currentTimeMillis();
      ds = DistributedSystem.connect(cfg);
      end = System.currentTimeMillis();
      System.out.println("ds.connect took    " + (end -start) + " ms");
      try {

        start = System.currentTimeMillis();
        c = CacheFactory.create(ds);
        end = System.currentTimeMillis();
        System.out.println("Cache create took " + (end -start) + " ms");

        try {
          AttributesFactory af = new AttributesFactory();
          af.setScope(Scope.GLOBAL);
          Region r = c.createRegion("loner", af.create());
          r.put("key1", "value1");
          r.get("key1");
          r.get("key2");
          r.invalidate("key1");
          r.destroy("key1");
          r.destroyRegion();
        } finally {
          {
            start = System.currentTimeMillis();
            c.close();
            end = System.currentTimeMillis();
            System.out.println("Cache close took " + (end -start) + " ms");
          }
        }
      } finally {
        if (ds != null) {
          start = System.currentTimeMillis();
          ds.disconnect();
          end = System.currentTimeMillis();
          System.out.println("ds.disconnect took " + (end -start) + " ms");
        }
        ds = null;
      }
    }
  }
  
  @Test
  public void testLonerWithStats() throws CacheException {
    long start;
    long end;
    DistributedSystem ds = null;
    Cache c = null;
    Properties cfg = new Properties();
    cfg.setProperty("mcast-port", "0");
    cfg.setProperty("locators", "");
    cfg.setProperty("statistic-sampling-enabled", "true");
    cfg.setProperty("statistic-archive-file", "lonerStats.gfs");

    for (int i=0; i < 1; i++) {
      start = System.currentTimeMillis();
      ds = DistributedSystem.connect(cfg);
      end = System.currentTimeMillis();
      System.out.println("ds.connect took    " + (end -start) + " ms");
      try {

        start = System.currentTimeMillis();
        c = CacheFactory.create(ds);
        end = System.currentTimeMillis();
        System.out.println("Cache create took " + (end -start) + " ms");

        try {
          AttributesFactory af = new AttributesFactory();
          af.setScope(Scope.GLOBAL);
          Region r = c.createRegion("loner", af.create());
          r.put("key1", "value1");
          r.get("key1");
          r.get("key2");
          r.invalidate("key1");
          r.destroy("key1");
          r.destroyRegion();
        } finally {
          {
            start = System.currentTimeMillis();
            c.close();
            end = System.currentTimeMillis();
            System.out.println("Cache close took " + (end -start) + " ms");
          }
        }
      } finally {
        if (ds != null) {
          start = System.currentTimeMillis();
          ds.disconnect();
          end = System.currentTimeMillis();
          System.out.println("ds.disconnect took " + (end -start) + " ms");
        }
        ds = null;
      }
    }
  }
  
  @Test
  public void testMemberId() throws UnknownHostException {
    String host = InetAddress.getLocalHost().getCanonicalHostName();
    String name = "Foo";

    Properties cfg = new Properties();
    cfg.setProperty(DistributionConfig.MCAST_PORT_NAME, "0");
    cfg.setProperty(DistributionConfig.LOCATORS_NAME, "");
    cfg.setProperty(DistributionConfig.ROLES_NAME, "lonelyOne");
    cfg.setProperty(DistributionConfig.NAME_NAME, name);
    DistributedSystem ds = DistributedSystem.connect(cfg);
    System.out.println("MemberId = " + ds.getMemberId());
    assertEquals(host.toString(), ds.getDistributedMember().getHost());
    assertEquals(OSProcess.getId(), ds.getDistributedMember().getProcessId());
    if(!PureJavaMode.isPure()) {
      String pid = String.valueOf(OSProcess.getId());
      assertTrue(ds.getMemberId().indexOf(pid) > -1);
    }
    assertTrue(ds.getMemberId().indexOf(name) > -1);
    String memberid = ds.getMemberId();
    String shortname = shortName(host);
    assertTrue("'" + memberid + "' does not contain '" + shortname + "'",
               memberid.indexOf(shortname) > -1);
    // make sure the loner port can be updated
    ((LonerDistributionManager)((InternalDistributedSystem)ds).getDM()).updateLonerPort(100);
  }

  private String shortName(String hostname) {
    assertNotNull(hostname);
    int index = hostname.indexOf('.');

    if (index > 0 && !Character.isDigit(hostname.charAt(0)))
      return hostname.substring(0, index);
    else
      return hostname;
  }
  
}
