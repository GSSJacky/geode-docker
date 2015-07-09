/*=========================================================================
 * Copyright (c) 2010-2014 Pivotal Software, Inc. All Rights Reserved.
 * This product is protected by U.S. and international copyright
 * and intellectual property laws. Pivotal products are covered by
 * one or more patents listed at http://www.pivotal.io/patents.
 *=========================================================================
 */
/**
 * 
 */
package com.gemstone.gemfire.internal.cache.execute.data;

import com.gemstone.gemfire.DataSerializable;
import com.gemstone.gemfire.DataSerializer;
import com.gemstone.gemfire.internal.cache.execute.PRColocationDUnitTest;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class Order implements DataSerializable {
  String orderName;

  public Order() {

  }

  public Order(String orderName) {
    this.orderName = orderName + PRColocationDUnitTest.getDefaultAddOnString();
  }

  public void fromData(DataInput in) throws IOException, ClassNotFoundException {
    this.orderName = DataSerializer.readString(in);
  }

  public void toData(DataOutput out) throws IOException {
    DataSerializer.writeString(this.orderName, out);
  }

  public String toString() {
    return this.orderName;
  }

  public boolean equals(Object obj) {
    if(this == obj)
      return true;
    
    if(obj instanceof Order){
      Order other = (Order)obj;
      if(other.orderName != null && other.orderName.equals(this.orderName)){
        return true;
      }
    }
    return false;
  }
}
