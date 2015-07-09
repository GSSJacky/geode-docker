package com.gemstone.gemfire.disttx;

import org.junit.experimental.categories.Category;

import com.gemstone.gemfire.cache.AttributesFactory;
import com.gemstone.gemfire.cache.Cache;
import com.gemstone.gemfire.cache.CacheTransactionManager;
import com.gemstone.gemfire.cache.DataPolicy;
import com.gemstone.gemfire.cache.DiskStore;
import com.gemstone.gemfire.cache.PartitionAttributesFactory;
import com.gemstone.gemfire.cache.Region;
import com.gemstone.gemfire.cache.RegionAttributes;
import com.gemstone.gemfire.internal.cache.TXManagerImpl;
import com.gemstone.gemfire.internal.cache.execute.data.CustId;
import com.gemstone.gemfire.internal.cache.execute.data.Customer;
import com.gemstone.gemfire.test.junit.categories.DistributedTransactionsTest;

import dunit.SerializableCallable;

@Category(DistributedTransactionsTest.class)
public class DistTXPersistentDebugDUnitTest extends DistTXDebugDUnitTest {

  public DistTXPersistentDebugDUnitTest(String name) {
    super(name);
  }
  
  @Override
  public void setUp() throws Exception {
    super.setUp();
    this.invokeInEveryVM(new SerializableCallable() {
      @Override
      public Object call() throws Exception {
        //System.setProperty("gemfire.ALLOW_PERSISTENT_TRANSACTIONS", "true");
        TXManagerImpl.ALLOW_PERSISTENT_TRANSACTIONS = true;
        return null;
      }
    }); 
  }
  
  @Override
  public void tearDown2() throws Exception {
    this.invokeInEveryVM(new SerializableCallable() {
      @Override
      public Object call() throws Exception {
        //System.setProperty("gemfire.ALLOW_PERSISTENT_TRANSACTIONS", "false");
        TXManagerImpl.ALLOW_PERSISTENT_TRANSACTIONS = false;
        return null;
      }
    }); 
    super.tearDown2();
  }
  
  protected void createPesistentPR(Object[] attributes) {
    dataStore1.invoke(DistTXPersistentDebugDUnitTest.class, "createPersistentPR", attributes);
    dataStore2.invoke(DistTXPersistentDebugDUnitTest.class, "createPersistentPR", attributes);
//    dataStore3.invoke(TxPersistentDebugDUnit.class, "createPR", attributes);
//    // make Local max memory = o for accessor
//    attributes[2] = new Integer(0);
//    accessor.invoke(TxPersistentDebugDUnit.class, "createPR", attributes);
  }
  
  public static void createPersistentPR(String regionName) {
    assertNotNull(cache);
    cache.createRegion(regionName, getPersistentPRAttributes(1, -1, cache, 113, true));
  }
  
  protected static RegionAttributes getPersistentPRAttributes(final int redundancy, final int recoveryDelay,
      Cache cache, int numBuckets, boolean synchronous) {
        DiskStore ds = cache.findDiskStore("disk");
        if(ds == null) {
          ds = cache.createDiskStoreFactory()
          .setDiskDirs(getDiskDirs()).create("disk");
        }
        AttributesFactory af = new AttributesFactory();
        PartitionAttributesFactory paf = new PartitionAttributesFactory();
        paf.setRedundantCopies(redundancy);
        paf.setRecoveryDelay(recoveryDelay);
        paf.setTotalNumBuckets(numBuckets);
        paf.setLocalMaxMemory(500);
        af.setPartitionAttributes(paf.create());
        af.setDataPolicy(DataPolicy.PERSISTENT_PARTITION);
        af.setDiskStoreName("disk");
        af.setDiskSynchronous(synchronous);
        RegionAttributes attr = af.create();
        return attr;
      }
  
  public void testBasicDistributedTX() throws Exception {
    createCacheInAllVms();
    final String regionName = "persistentCustomerPRRegion";
    Object[] attrs = new Object[] { regionName };
    createPesistentPR(attrs);
    SerializableCallable TxOps = new SerializableCallable() {
      @Override
      public Object call() throws Exception {
        CacheTransactionManager mgr = cache.getCacheTransactionManager();
        mgr.setDistributed(true);
        getLogWriter().fine("SJ:TX BEGIN");
        mgr.begin();
        Region<CustId, Customer> prRegion = cache.getRegion(regionName);

        CustId custIdOne = new CustId(1);
        Customer customerOne = new Customer("name1", "addr1");
        getLogWriter().fine("SJ:TX PUT 1");
        prRegion.put(custIdOne, customerOne);

        CustId custIdTwo = new CustId(2);
        Customer customerTwo = new Customer("name2", "addr2");
        getLogWriter().fine("SJ:TX PUT 2");
        prRegion.put(custIdTwo, customerTwo);

        getLogWriter().fine("SJ:TX COMMIT");
        mgr.commit();
        return null;
      }
    };

    dataStore2.invoke(TxOps);
  }
}