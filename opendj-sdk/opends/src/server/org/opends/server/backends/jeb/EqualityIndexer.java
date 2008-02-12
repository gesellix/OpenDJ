/*
 * CDDL HEADER START
 *
 * The contents of this file are subject to the terms of the
 * Common Development and Distribution License, Version 1.0 only
 * (the "License").  You may not use this file except in compliance
 * with the License.
 *
 * You can obtain a copy of the license at
 * trunk/opends/resource/legal-notices/OpenDS.LICENSE
 * or https://OpenDS.dev.java.net/OpenDS.LICENSE.
 * See the License for the specific language governing permissions
 * and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL HEADER in each
 * file and include the License file at
 * trunk/opends/resource/legal-notices/OpenDS.LICENSE.  If applicable,
 * add the following below this CDDL HEADER, with the fields enclosed
 * by brackets "[]" replaced with your own identifying information:
 *      Portions Copyright [yyyy] [name of copyright owner]
 *
 * CDDL HEADER END
 *
 *
 *      Copyright 2006-2008 Sun Microsystems, Inc.
 */
package org.opends.server.backends.jeb;

import static org.opends.server.loggers.debug.DebugLogger.*;
import org.opends.server.loggers.debug.DebugTracer;
import org.opends.server.types.DebugLogLevel;

import com.sleepycat.je.Transaction;
import com.sleepycat.je.DatabaseException;

import org.opends.server.types.Attribute;
import org.opends.server.types.AttributeType;
import org.opends.server.types.AttributeValue;
import org.opends.server.types.DirectoryException;
import org.opends.server.types.Entry;
import org.opends.server.types.Modification;

import java.util.*;

/**
 * An implementation of an Indexer for attribute equality.
 */
public class EqualityIndexer extends Indexer
{
  /**
   * The tracer object for the debug logger.
   */
  private static final DebugTracer TRACER = getTracer();



  /**
   * The comparator for index keys generated by this class.
   */
  private static final Comparator<byte[]> comparator =
       new AttributeIndex.KeyComparator();

  /**
   * The attribute type for which this instance will
   * generate index keys.
   */
  private AttributeType attributeType;


  /**
   * Create a new attribute equality indexer for the given attribute type.
   * @param attributeType The attribute type for which an indexer is
   * required.
   */
  public EqualityIndexer(AttributeType attributeType)
  {
    this.attributeType = attributeType;
  }

  /**
   * Get a string representation of this object.  The returned value is
   * used to name an index created using this object.
   * @return A string representation of this object.
   */
  public String toString()
  {
    return attributeType.getNameOrOID() + ".equality";
  }

  /**
   * Get the comparator that must be used to compare index keys
   * generated by this class.
   *
   * @return A byte array comparator.
   */
  public Comparator<byte[]> getComparator()
  {
    return comparator;
  }



  /**
   * Generate the set of index keys for an entry.
   *
   * @param txn A database transaction to be used if the database need to be
   * accessed in the course of generating the index keys.
   * @param entry The entry.
   * @param keys The set into which the generated keys will be inserted.
   * @throws DatabaseException If an error occurs in the JE database.
   */
  public void indexEntry(Transaction txn, Entry entry,
                         Set<byte[]> keys) throws DatabaseException
  {
    List<Attribute> attrList =
         entry.getAttribute(attributeType);
    if (attrList != null)
    {
      indexAttribute(attrList, keys);
    }
  }



  /**
   * Generate the set of index keys to be added and the set of index keys
   * to be deleted for an entry that has been replaced.
   *
   * @param txn A database transaction to be used if the database need to be
   * accessed in the course of generating the index keys.
   * @param oldEntry The original entry contents.
   * @param newEntry The new entry contents.
   * @param addKeys The set into which the keys to be added will be inserted.
   * @param delKeys The set into which the keys to be deleted will be inserted.
   * @throws DatabaseException If an error occurs in the JE database.
   */
  public void replaceEntry(Transaction txn, Entry oldEntry, Entry newEntry,
                           Set<byte[]> addKeys,
                           Set<byte[]> delKeys)
      throws DatabaseException
  {
    List<Attribute> newAttributes = newEntry.getAttribute(attributeType, true);
    List<Attribute> oldAttributes = oldEntry.getAttribute(attributeType, true);

    if(newAttributes == null)
    {
      indexAttribute(oldAttributes, delKeys);
    }
    else
    {
      if(oldAttributes == null)
      {
        indexAttribute(newAttributes, addKeys);
      }
      else
      {
        TreeSet<byte[]> newKeys =
            new TreeSet<byte[]>(comparator);
        TreeSet<byte[]> oldKeys =
            new TreeSet<byte[]>(comparator);
        indexAttribute(newAttributes, newKeys);
        indexAttribute(oldAttributes, oldKeys);

        addKeys.addAll(newKeys);
        addKeys.removeAll(oldKeys);

        delKeys.addAll(oldKeys);
        delKeys.removeAll(newKeys);
      }
    }
  }




  /**
   * Generate the set of index keys to be added and the set of index keys
   * to be deleted for an entry that was modified.
   *
   * @param txn A database transaction to be used if the database need to be
   * accessed in the course of generating the index keys.
   * @param oldEntry The original entry contents.
   * @param newEntry The new entry contents.
   * @param mods The set of modifications that were applied to the entry.
   * @param addKeys The set into which the keys to be added will be inserted.
   * @param delKeys The set into which the keys to be deleted will be inserted.
   * @throws DatabaseException If an error occurs in the JE database.
   */
  public void modifyEntry(Transaction txn, Entry oldEntry, Entry newEntry,
                          List<Modification> mods,
                          Set<byte[]> addKeys,
                          Set<byte[]> delKeys)
      throws DatabaseException
  {
    List<Attribute> newAttributes = newEntry.getAttribute(attributeType, true);
    List<Attribute> oldAttributes = oldEntry.getAttribute(attributeType, true);

    if(newAttributes == null)
    {
      indexAttribute(oldAttributes, delKeys);
    }
    else
    {
      if(oldAttributes == null)
      {
        indexAttribute(newAttributes, addKeys);
      }
      else
      {
        TreeSet<byte[]> newKeys =
            new TreeSet<byte[]>(comparator);
        TreeSet<byte[]> oldKeys =
            new TreeSet<byte[]>(comparator);
        indexAttribute(newAttributes, newKeys);
        indexAttribute(oldAttributes, oldKeys);

        addKeys.addAll(newKeys);
        addKeys.removeAll(oldKeys);

        delKeys.addAll(oldKeys);
        delKeys.removeAll(newKeys);
      }
    }
  }

  /**
   * Generate the set of index keys for a set of attribute values.
   * @param values The set of attribute values to be indexed.
   * @param keys The set into which the keys will be inserted.
   */
  private void indexValues(Set<AttributeValue> values,
                           Set<byte[]> keys)
  {
    if (values == null) return;

    for (AttributeValue value : values)
    {
      try
      {
        byte[] keyBytes = value.getNormalizedValue().value();

        keys.add(keyBytes);
      }
      catch (DirectoryException e)
      {
        if (debugEnabled())
        {
          TRACER.debugCaught(DebugLogLevel.ERROR, e);
        }
      }
    }
  }

  /**
   * Generate the set of index keys for an attribute.
   * @param attrList The attribute to be indexed.
   * @param keys The set into which the keys will be inserted.
   */
  private void indexAttribute(List<Attribute> attrList,
                              Set<byte[]> keys)
  {
    if (attrList == null) return;

    for (Attribute attr : attrList)
    {
      indexValues(attr.getValues(), keys);
    }
  }

}
