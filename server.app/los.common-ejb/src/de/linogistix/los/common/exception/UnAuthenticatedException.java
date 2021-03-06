/*
 * Copyright (c) 2006 - 2010 LinogistiX GmbH
 * 
 *  www.linogistix.com
 *  
 *  Project myWMS-LOS
 */
package de.linogistix.los.common.exception;

import javax.ejb.ApplicationException;

import org.mywms.service.ServiceException;

import de.linogistix.los.res.BundleResolver;

/**
 * This Exception will not cause a rollback of the transaction.
 * 
 * @author Jordan
 *
 */
@ApplicationException(rollback=false)
public class UnAuthenticatedException extends ServiceException {

	private static final long serialVersionUID = 1L;
	
	public UnAuthenticatedException(){
		super("UnAuthenticated", "UNAUTHENTICATED", new Object[0]);
		setBundleResolver(BundleResolver.class);
	}
}
