/*
 * <p>Title: :ERPPmApi.java </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2014</p>
 * <p>Company: digitalchina.Ltd</p>
 * @author 
 * Created :2023-1-31 10:32:00
 * @version 1.0
 * ModifyList:
 * <Author> <Time(yyyy/mm/dd)>  <Description>  <Version>
 */


package com.dc.esb.busi.impls.erp;


import com.dc.esb.container.core.data.IServiceDataObject;
import com.dc.esb.container.core.sclite.IBaseContext;
import com.dc.esb.container.core.sclite.IBusinessService;
import com.dc.esb.container.core.sclite.InvokeException;
import com.dc.esb.container.core.sclite.ServiceMaintainException;

/**
 * <p>
 * <li>
 * </li>
 * </p>
 * 
 * @author 
 * @E-Mail 
 */
public class ERPPmApi implements IBusinessService {

	public IServiceDataObject invoke(IServiceDataObject arg0, IBaseContext arg1)
			throws InvokeException {
		// TODO Auto-generated method stub
		return null;
	}

	private boolean started = false;

	public boolean isStarted() {
		return started;
	}
	
	public void start() throws ServiceMaintainException {
		started = true;
	 }
		
	public void stop() throws ServiceMaintainException {
		started = false;
	}

}
