/*
 * CDDL HEADER START
 *
 * The contents of this file are subject to the terms of the
 * Common Development and Distribution License, Version 1.0 only
 * (the "License").  You may not use this file except in compliance
 * with the License.
 *
 * You can obtain a copy of the license at legal-notices/CDDLv1_0.txt
 * or http://forgerock.org/license/CDDLv1.0.html.
 * See the License for the specific language governing permissions
 * and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL HEADER in each
 * file and include the License file at legal-notices/CDDLv1_0.txt.
 * If applicable, add the following below this CDDL HEADER, with the
 * fields enclosed by brackets "[]" replaced with your own identifying
 * information:
 *      Portions Copyright [yyyy] [name of copyright owner]
 *
 * CDDL HEADER END
 *
 *
 *      Copyright 2006-2010 Sun Microsystems, Inc.
 *      Portions Copyright 2014-2016 ForgeRock AS
 */
package org.opends.server.monitors;

import static org.opends.server.util.ServerConstants.*;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.forgerock.i18n.slf4j.LocalizedLogger;
import org.opends.server.admin.std.server.MonitorProviderCfg;
import org.opends.server.api.Backend;
import org.opends.server.api.MonitorProvider;
import org.opends.server.core.DirectoryServer;
import org.opends.server.schema.BooleanSyntax;
import org.forgerock.opendj.ldap.schema.AttributeType;
import org.opends.server.types.*;

/**
 * This class implements a monitor provider that will report generic information
 * for an enabled Directory Server backend, including its backend ID, base DNs,
 * writability mode, and the number of entries it contains.
 */
public class BackendMonitor
       extends MonitorProvider<MonitorProviderCfg>
{
  /** The attribute type that will be used to report the backend ID. */
  private AttributeType backendIDType;
  /** The attribute type that will be used to report the set of base DNs. */
  private AttributeType baseDNType;
  /** The attribute type that will be used to report the number of entries. */
  private AttributeType entryCountType;
  /** The attribute type that will be used to report the number of entries per base DN. */
  private AttributeType baseDNEntryCountType;
  /** The attribute type that will be used to indicate if a backend is private. */
  private AttributeType isPrivateType;
  /** The attribute type that will be used to report the writability mode. */
  private AttributeType writabilityModeType;

  /** The backend with which this monitor is associated. */
  private Backend<?> backend;

  /** The name for this monitor. */
  private String monitorName;
  private static final LocalizedLogger logger = LocalizedLogger.getLoggerForThisClass();

  /**
   * Creates a new instance of this backend monitor provider that will work with
   * the provided backend.  Most of the initialization should be handled in the
   * {@code initializeMonitorProvider} method.
   *
   * @param  backend  The backend with which this monitor is associated.
   */
  public BackendMonitor(Backend<?> backend)
  {
    this.backend = backend;
  }

  @Override
  public void initializeMonitorProvider(MonitorProviderCfg configuration)
  {
    monitorName = backend.getBackendID() + " Backend";

    backendIDType = DirectoryServer.getAttributeTypeOrDefault(ATTR_MONITOR_BACKEND_ID);
    baseDNType = DirectoryServer.getAttributeTypeOrDefault(ATTR_MONITOR_BACKEND_BASE_DN);
    entryCountType = DirectoryServer.getAttributeTypeOrDefault(ATTR_MONITOR_BACKEND_ENTRY_COUNT);
    baseDNEntryCountType = DirectoryServer.getAttributeTypeOrDefault(ATTR_MONITOR_BASE_DN_ENTRY_COUNT);
    isPrivateType = DirectoryServer.getAttributeTypeOrDefault(ATTR_MONITOR_BACKEND_IS_PRIVATE);
    writabilityModeType = DirectoryServer.getAttributeTypeOrDefault(ATTR_MONITOR_BACKEND_WRITABILITY_MODE);
  }

  @Override
  public String getMonitorInstanceName()
  {
    return monitorName;
  }

  /**
   * Retrieves the objectclass that should be included in the monitor entry
   * created from this monitor provider.
   *
   * @return  The objectclass that should be included in the monitor entry
   *          created from this monitor provider.
   */
  @Override
  public ObjectClass getMonitorObjectClass()
  {
    return DirectoryConfig.getObjectClass(OC_MONITOR_BACKEND, true);
  }

  @Override
  public List<Attribute> getMonitorData()
  {
    LinkedList<Attribute> attrs = new LinkedList<>();

    attrs.add(Attributes.create(backendIDType, backend.getBackendID()));

    DN[] baseDNs = backend.getBaseDNs();

    AttributeBuilder builder = new AttributeBuilder(baseDNType);
    builder.addAllStrings(Arrays.asList(baseDNs));
    attrs.add(builder.toAttribute());

    attrs.add(Attributes.create(isPrivateType, BooleanSyntax
        .createBooleanValue(backend.isPrivateBackend())));

    long backendCount = backend.getEntryCount();
    attrs.add(Attributes.create(entryCountType, String
        .valueOf(backendCount)));

    builder = new AttributeBuilder(baseDNEntryCountType);
    if (baseDNs.length != 1)
    {
      for (DN dn : baseDNs)
      {
        long entryCount = -1;
        try
        {
          entryCount = backend.getNumberOfEntriesInBaseDN(dn);
        }
        catch (Exception ex)
        {
          logger.traceException(ex);
        }
        builder.add(entryCount + " " + dn);
      }
    }
    else
    {
      // This is done to avoid recalculating the number of entries
      // using the numSubordinates method in the case where the
      // backend has a single base DN.
      builder.add(backendCount + " " + baseDNs[0]);
    }
    attrs.add(builder.toAttribute());

    attrs.add(Attributes.create(writabilityModeType, String
        .valueOf(backend.getWritabilityMode())));

    return attrs;
  }
}

