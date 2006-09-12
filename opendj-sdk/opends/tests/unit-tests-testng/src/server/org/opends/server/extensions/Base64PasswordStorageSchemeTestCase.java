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
 * by brackets "[]" replaced with your own identifying * information:
 *      Portions Copyright [yyyy] [name of copyright owner]
 *
 * CDDL HEADER END
 *
 *
 *      Portions Copyright 2006 Sun Microsystems, Inc.
 */
package org.opends.server.extensions;



import org.opends.server.api.PasswordStorageScheme;



/**
 * A set of test cases for the base64 password storage scheme.
 */
public class Base64PasswordStorageSchemeTestCase
       extends PasswordStorageSchemeTestCase
{
  /**
   * Creates a new instance of this storage scheme test case.
   */
  public Base64PasswordStorageSchemeTestCase()
  {
    super("cn=Base64,cn=Password Storage Schemes,cn=config");
  }



  /**
   * Retrieves an initialized instance of this password storage scheme.
   *
   * @param  configEntry  The configuration entry for the password storage
   *                      scheme, or <CODE>null</CODE> if none is available.
   *
   * @return  An initialized instance of this password storage scheme.
   *
   * @throws  Exception  If an unexpected problem occurs.
   */
  public PasswordStorageScheme getScheme()
         throws Exception
  {
    Base64PasswordStorageScheme scheme = new Base64PasswordStorageScheme();
    scheme.initializePasswordStorageScheme(configEntry);
    return scheme;
  }
}

