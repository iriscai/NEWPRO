package com.dc.esb.base.impls.service;


import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletResponse;
import com.dc.esb.container.core.data.IServiceDataObject;
import com.dc.esb.container.core.sclite.IBaseContext;
import com.dc.esb.container.core.sclite.IBaseService;
import com.dc.esb.container.core.sclite.InvokeException;
import com.dc.esb.container.core.sclite.ServiceMaintainException;
import com.dc.esb.container.log.MyLog;
import com.dc.esb.container.log.MyLogHelper;
import com.dc.esb.container.protocol.http.HTTPTransfer;
import com.dc.esb.container.sclite.ContextConstants;

/**
 * 克隆http响应头
 * 
 * 
 * @author shijp 202207027
 * @E-Mail 
 */
public class CloneResHeader implements IBaseService {

private static final MyLog log = MyLogHelper.getFactory().getLog(
			CloneResHeader.class);
	public String signature=null;
	public IServiceDataObject invoke(IServiceDataObject data, IBaseContext context)
			throws InvokeException {
		HttpServletResponse response= (HttpServletResponse) context.getLocalizedValue(ContextConstants.HTTPRESP);
		
		//如果接出协议是http，则将接出协议的http头信息放到接入协议上
		 String headers = context.getDelocalizedValue("HTTP_HEADERS");
			
		 if (headers != null)
		    {
		      Map header = HTTPTransfer.stringToMap(headers);
		      Set set = header.entrySet();
		      Iterator iter = set.iterator();
		      while (iter.hasNext()) {
		        Map.Entry entry = (Map.Entry)iter.next();
		        String key = (String)entry.getKey();
//		         if ("Content-Encoding".equalsIgnoreCase(key)) {
//				 continue;
//				 }
		        String value = (String)entry.getValue();
		        response.addHeader(key, value);
		      }
		    }
		
		String statusCode = context.getDelocalizedValue(ContextConstants.HTTP_STATUS_CODE);
		if(statusCode !=null && statusCode.trim().length()>0)
			response.setStatus(Integer.valueOf(statusCode));
		//日志输出
		if(log.isDebugEnabled()){
			Collection<String> httpR = response.getHeaderNames();
			Iterator it = httpR.iterator();
			StringBuilder builder = new StringBuilder();
			builder.append("http响应头：\n");
			while(it.hasNext()){
				String key = (String)it.next();
				String value = response.getHeader(key);
				builder.append(key).append("=").append(value).append("\n");
			}
			log.debug(builder.toString()) ;
		}
		return data;
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
