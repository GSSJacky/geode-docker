package com.gemstone.gemfire.disttx;

import java.util.Properties;

import org.junit.experimental.categories.Category;

import com.gemstone.gemfire.cache.CacheFactory;
import com.gemstone.gemfire.cache.CacheTransactionManager;
import com.gemstone.gemfire.cache.RegionShortcut;
import com.gemstone.gemfire.internal.cache.TXManagerImplJUnitTest;
import com.gemstone.gemfire.test.junit.categories.DistributedTransactionsTest;
import com.gemstone.gemfire.test.junit.categories.IntegrationTest;


/**
 * Same tests as that of {@link TXManagerImplJUnitTest} after setting
 * "distributed-transactions" property to true
 *
 */
@Category({IntegrationTest.class, DistributedTransactionsTest.class })
public class DistTXManagerImplJUnitTest extends TXManagerImplJUnitTest {

  public DistTXManagerImplJUnitTest() {
  }
  
  @Override
  protected void createCache() {
    Properties props = new Properties();
    props.put("mcast-port", "0");
    props.put("locators", "");
    props.put("distributed-transactions", "true");
    cache = new CacheFactory(props).create();
    region = cache.createRegionFactory(RegionShortcut.REPLICATE).create("testRegion");
    CacheTransactionManager txmgr = cache.getCacheTransactionManager();
    assert(txmgr.isDistributed());
  }

}
