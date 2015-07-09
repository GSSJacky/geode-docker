/*=========================================================================
 * Copyright (c) 2002-2014 Pivotal Software, Inc. All Rights Reserved.
 * This product is protected by U.S. and international copyright
 * and intellectual property laws. Pivotal products are covered by
 * more patents listed at http://www.pivotal.io/patents.
 *=========================================================================
 */

package com.gemstone.gemfire.internal.cache.tier.sockets;

import com.gemstone.gemfire.internal.DataSerializableFixedID;
import com.gemstone.gemfire.internal.Version;
import com.gemstone.gemfire.internal.cache.versions.VersionTag;
import com.gemstone.gemfire.internal.logging.LogService;
import com.gemstone.gemfire.internal.logging.log4j.LogMarker;
import com.gemstone.gemfire.internal.offheap.OffHeapHelper;
import com.gemstone.gemfire.internal.offheap.Releasable;
import com.gemstone.gemfire.DataSerializer;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.logging.log4j.Logger;

/**
 * Encapsulates list containing objects, serialized objects, raw byte arrays, or
 * exceptions. It can optionally also hold the list of associated keys. Assumes
 * that keys are either provided for all entries or for none.
 * 
 * @since 5.7
 * @author swale
 */
public class ObjectPartList implements DataSerializableFixedID, Releasable {
  private static final Logger logger = LogService.getLogger();

  protected static final byte BYTES = 0;

  protected static final byte OBJECT = 1;

  protected static final byte EXCEPTION = 2;
  
  protected static final byte KEY_NOT_AT_SERVER = 3;

  protected byte[] objectTypeArray;

  protected boolean hasKeys;

  protected List keys;

  protected List objects;
  
  public void addPart(Object key, Object value, byte objectType, VersionTag versionTag) {
    int size = this.objects.size();
    int maxSize = this.objectTypeArray.length;
    if (size >= maxSize) {
      throw new IndexOutOfBoundsException("Cannot add object part beyond "
          + maxSize + " elements");
    }
    if (this.hasKeys) {
      if (key == null) {
        throw new IllegalArgumentException("Cannot add null key");
      }
      this.keys.add(key);
    }
    this.objectTypeArray[size] = objectType;
    this.objects.add(value);
  }

  // public methods

  public ObjectPartList() {
    this.objectTypeArray = null;
    this.hasKeys = false;
    this.keys = null;
    this.objects = new ArrayList();
  }

  public ObjectPartList(int maxSize, boolean hasKeys) {
    if (maxSize <= 0) {
      throw new IllegalArgumentException("Invalid size " + maxSize
          + " to ObjectPartList constructor");
    }
    this.objectTypeArray = new byte[maxSize];
    this.hasKeys = hasKeys;
    if (hasKeys) {
      this.keys = new ArrayList();
    }
    else {
      this.keys = null;
    }
    this.objects = new ArrayList();
  }

  public void addObjectPart(Object key, Object value, boolean isObject, VersionTag versionTag) {
    addPart(key, value, isObject ? OBJECT : BYTES, versionTag);
  }

  public void addExceptionPart(Object key, Exception ex) {
    addPart(key, ex, EXCEPTION, null);
  }
  
  public void addObjectPartForAbsentKey(Object key, Object value) {
    // ObjectPartList is for clients < version 6.5.0, which didn't support this setting
    throw new IllegalAccessError("inappropriate use of ObjectPartList");
  }


  
  public void addAll(ObjectPartList other) {
    if (logger.isTraceEnabled(LogMarker.OBJECT_PART_LIST)) {
      logger.trace(LogMarker.OBJECT_PART_LIST, "OPL.addAll: other={}\nthis={}", other, this);
    }
    
    if (this.hasKeys) {
      if (other.keys != null) {
        if (this.keys == null) {
          this.keys = new ArrayList(other.keys);
        } else {
          this.keys.addAll(other.keys);
        }
      }
    }
    else if (other.hasKeys) {
      this.hasKeys = true;
      this.keys = new ArrayList(other.keys);
    }
    this.objects.addAll(other.objects);
  }

  public List getKeys() {
    if (this.keys == null) {
      return Collections.emptyList();
    } else {
      return Collections.unmodifiableList(this.keys);
    }
  }
  
  /** unprotected access to the keys collection, which may be null */
  public List getKeysForTest() {
    return this.keys;
  }

  public List getObjects() {
    if (this.objects == null) {
      return Collections.emptyList();
    } else {
      return Collections.unmodifiableList(this.objects);
    }
  }
  
  /** unprotected access to the objects collection, which may be null */
  public List getObjectsForTest() {
    return this.objects;
  }
  
  public int size() {
    // some lists have only keys and some have only objects, so we need to choose
    // the correct collection to query
    if (this.hasKeys) {
      return this.keys.size();
    } else {
      return this.objects.size();
    }
  }

  public void reinit(int maxSize) {
    if (maxSize <= 0) {
      throw new IllegalArgumentException("Invalid size " + maxSize
          + " to ObjectPartList.reinit");
    }
    this.objectTypeArray = new byte[maxSize];
    this.objects.clear();
    this.keys.clear();
  }

  public void clear() {
    release();
    this.objects.clear();
    if (this.keys != null) {
      this.keys.clear();
    }
  }

  public void toData(DataOutput out) throws IOException {
    out.writeBoolean(this.hasKeys);
    if (this.objectTypeArray != null) {
      int numObjects = this.objects.size();
      out.writeInt(numObjects);
      for (int index = 0; index < numObjects; ++index) {
        Object value = this.objects.get(index);
        byte objectType = this.objectTypeArray[index];
        if (this.hasKeys) {
          DataSerializer.writeObject(this.keys.get(index), out);
        }
        out.writeBoolean(objectType == EXCEPTION);
        if (objectType == OBJECT && value instanceof byte[]) {
          out.write((byte[])value);
        }
        else if (objectType == EXCEPTION) {
          // write exception as byte array so native clients can skip it
          DataSerializer
              .writeByteArray(CacheServerHelper.serialize(value), out);
          // write the exception string for native clients
          DataSerializer.writeString(value.toString(), out);
        }
        else {
          DataSerializer.writeObject(value, out);
        }
      }
    }
    else {
      out.writeInt(0);
    }
  }

  public void fromData(DataInput in) throws IOException, ClassNotFoundException {
    this.hasKeys = in.readBoolean();
    if (this.hasKeys) {
      this.keys = new ArrayList();
    }
    int numObjects = in.readInt();
    if (numObjects > 0) {
      for (int index = 0; index < numObjects; ++index) {
        if (this.hasKeys) {
          Object key = DataSerializer.readObject(in);
          this.keys.add(key);
        }
        boolean isException = in.readBoolean();
        Object value;
        if (isException) {
          byte[] exBytes = DataSerializer.readByteArray(in);
          value = CacheServerHelper.deserialize(exBytes);
          // ignore the exception string meant for native clients
          DataSerializer.readString(in);
        }
        else {
          value = DataSerializer.readObject(in);
        }
        this.objects.add(value);
      }
    }
  }

  public int getDSFID() {
    return DataSerializableFixedID.OBJECT_PART_LIST;
  }

  @Override
  public Version[] getSerializationVersions() {
    return null;
  }

  @Override
  public void release() {
    for (Object v: this.objects) {
      OffHeapHelper.release(v);
    }
  }

}
