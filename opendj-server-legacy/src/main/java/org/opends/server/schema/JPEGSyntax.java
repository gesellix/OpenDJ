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
 *      Copyright 2006-2008 Sun Microsystems, Inc.
 *      Portions Copyright 2012-2016 ForgeRock AS
 *
 */
package org.opends.server.schema;

import static org.opends.server.schema.SchemaConstants.*;

import java.util.List;

import org.forgerock.i18n.LocalizableMessage;
import org.forgerock.opendj.config.server.ConfigChangeResult;
import org.forgerock.opendj.config.server.ConfigException;
import org.forgerock.opendj.ldap.schema.Schema;
import org.forgerock.opendj.ldap.schema.SchemaOptions;
import org.forgerock.opendj.ldap.schema.Syntax;
import org.opends.server.admin.server.ConfigurationChangeListener;
import org.opends.server.admin.std.server.JPEGAttributeSyntaxCfg;
import org.opends.server.api.AttributeSyntax;
import org.opends.server.core.ServerContext;
import org.opends.server.types.DirectoryException;

/**
 * This class implements the JPEG attribute syntax.  This is actually
 * two specifications - JPEG and JFIF. As an extension we allow JPEG
 * and Exif, which is what most digital cameras use. We only check for
 * valid JFIF and Exif headers.
 */
public class JPEGSyntax
       extends AttributeSyntax<JPEGAttributeSyntaxCfg>
       implements ConfigurationChangeListener<JPEGAttributeSyntaxCfg>
{

  /** The current configuration for this JPEG syntax. */
  private volatile JPEGAttributeSyntaxCfg config;

  private ServerContext serverContext;

  /**
   * Creates a new instance of this syntax.  Note that the only thing that
   * should be done here is to invoke the default constructor for the
   * superclass.  All initialization should be performed in the
   * <CODE>initializeSyntax</CODE> method.
   */
  public JPEGSyntax()
  {
    super();
  }

  @Override
  public void initializeSyntax(JPEGAttributeSyntaxCfg configuration, ServerContext serverContext)
      throws ConfigException, DirectoryException
  {
    this.config = configuration;
    this.serverContext = serverContext;
    serverContext.getSchema().updateSchemaOption(SchemaOptions.ALLOW_MALFORMED_JPEG_PHOTOS, !config.isStrictFormat());
    config.addJPEGChangeListener(this);
  }

  @Override
  public Syntax getSDKSyntax(Schema schema)
  {
    return schema.getSyntax(SchemaConstants.SYNTAX_JPEG_OID);
  }

  @Override
  public String getName()
  {
    return SYNTAX_JPEG_NAME;
  }

  @Override
  public String getOID()
  {
    return SYNTAX_JPEG_OID;
  }

  @Override
  public String getDescription()
  {
    return SYNTAX_JPEG_DESCRIPTION;
  }

  @Override
  public boolean isConfigurationChangeAcceptable(
                      JPEGAttributeSyntaxCfg configuration,
                      List<LocalizableMessage> unacceptableReasons)
  {
    // The configuration will always be acceptable.
    return true;
  }

  @Override
  public ConfigChangeResult applyConfigurationChange(
              JPEGAttributeSyntaxCfg configuration)
  {
    this.config = configuration;
    final ConfigChangeResult ccr = new ConfigChangeResult();
    try
    {
      serverContext.getSchema().updateSchemaOption(SchemaOptions.ALLOW_MALFORMED_JPEG_PHOTOS, !config.isStrictFormat());
    }
    catch (DirectoryException e)
    {
      ccr.setResultCode(e.getResultCode());
      ccr.addMessage(e.getMessageObject());
    }
    return ccr;
  }
}

