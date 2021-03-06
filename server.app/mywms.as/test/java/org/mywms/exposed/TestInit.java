/*
 * Copyright (c) 2006 by Fraunhofer IML, Dortmund.
 * All rights reserved.
 *
 * Project: myWMS
 */
package org.mywms.exposed;

import java.io.FileInputStream;
import java.util.Properties;

import junit.framework.TestCase;

import org.mywms.ejb.BeanLocator;
import org.mywms.facade.SanityCheck;


/**
 * Implements a basic setUp() method, operating the authentication stuff.
 * 
 * @author Olaf Krause
 * @version $Revision: 734 $ provided by $Author: dgrys $
 */
public class TestInit 
	extends TestCase 
{

	public static final String LOGIN = "admin";
	public static final String PASSWD = "admin";
	
	/** Contains the locator for admin access to the myWMS app. */
	protected BeanLocator beanLocator;

	/**
	 * Sets up a bean locator with admin privileges.
	 *  
	 * @see TestCase#setUp()
	 */
	protected void setUp() 
		throws Exception 
	{
		super.setUp();
		//
		Properties props = new Properties();
		props.load(new FileInputStream("../../config/wf8-context.properties"));

		Properties appServerProps = new Properties();
		appServerProps.load(new FileInputStream("../../config/appserver.properties"));

		beanLocator = new BeanLocator(LOGIN, PASSWD, props, appServerProps);

		SanityCheck sanityCheck = beanLocator.getStateless(SanityCheck.class);
		sanityCheck.check();
		
	}
}
