package com.dc.esb.base.impls.service;

import java.io.UnsupportedEncodingException;

import org.json.JSONException;
import org.json.JSONObject;

import com.dc.esb.container.adaptor.helper.AdaptorHelper;
import com.dc.esb.container.core.data.IServiceDataObject;
import com.dc.esb.container.core.sclite.IBaseContext;
import com.dc.esb.container.core.sclite.IBaseService;
import com.dc.esb.container.core.sclite.InvokeException;
import com.dc.esb.container.core.sclite.ServiceMaintainException;
import com.dc.esb.container.log.MyLog;
import com.dc.esb.container.log.MyLogHelper;
import com.dc.esb.container.sclite.ContextConstants;
import com.dc.esb.container.core.sclite.IBaseContext;
import com.dc.esb.container.core.sclite.IBaseService;
import com.dc.esb.container.core.sclite.InvokeException;
import com.dc.esb.container.core.sclite.ServiceMaintainException;


/**
 * <p>
 * 
 * <li>
 * </li>
 * </p>
 * 中核ESB连通性探测服务识别基础服务
 * @author shijp 20220725
 * @E-Mail  
 */
public class DetectBaseService implements IBaseService {
private static final MyLog log = MyLogHelper.getFactory().getLog(
			DetectBaseService.class);
	public IServiceDataObject invoke(IServiceDataObject sdo, IBaseContext context)
			throws InvokeException {
		Object data = context.getLocalizedValue(ContextConstants.ORIGINALDATA);
		String originalData = "";
		if (data instanceof byte[]) {
			try {
				originalData = new String((byte[]) data, "utf-8");
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		} else {
			originalData = (String) data;
		}
		try {
			JSONObject json = new JSONObject(originalData);
			String serviceId = json.getString("invokeService");
			AdaptorHelper.setServiceid(context, serviceId);
			if (log.isDebugEnabled()) {
				log.debug("ESB连通性探测服务识别成功，识别到的服务为:" + serviceId);
			}
		} catch (JSONException e) {
			if(log.isErrorEnabled()){
				log.error("ESB连通性探测服务识别失败",e);
			}
		}
		return sdo;
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
