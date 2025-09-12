package com.dc.esb.base.impls.service;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import com.dc.esb.container.adaptor.helper.AdaptorHelper;
import com.dc.esb.container.core.data.IServiceDataObject;
import com.dc.esb.container.core.protocol.IConnector;
import com.dc.esb.container.core.sclite.IBaseContext;
import com.dc.esb.container.core.sclite.IBaseService;
import com.dc.esb.container.core.sclite.ITransferableContext;
import com.dc.esb.container.core.sclite.InvokeException;
import com.dc.esb.container.core.sclite.ServiceMaintainException;
import com.dc.esb.container.core.sclite.trans.PropertyType;
import com.dc.esb.container.log.MyLog;
import com.dc.esb.container.log.MyLogHelper;
import com.dc.esb.container.protocol.http.HTTPTransfer;
import com.dc.esb.container.protocol.http.server.BufferServletRequest;
import com.dc.esb.container.protocol.http.server.HTTPServerConnector;
import com.dc.esb.container.protocol.ws.server.WSServerConnector;
import com.dc.esb.container.sclite.ContextConstants;

/**
 * 渠道适配流程中通过URL进行服务识别、系统识别、渠道识别
 * http://ip:port/esb/consumerId/providerId/services/servicesId
 * 
 * @author shijp 20220606
 * @E-Mail 
 */
public class CIdentifyByUrl implements IBaseService {

	private static final MyLog log = MyLogHelper.getFactory().getLog(CIdentifyByUrl.class);

	public IServiceDataObject invoke(IServiceDataObject data, IBaseContext context) throws InvokeException {

		Object conn = (IConnector) context.getLocalizedValue("connector");
		StringBuffer urlPath = null;
		String servletPath = null;
		String clientIp = null;
		String queryStr = null;		
		if ((conn instanceof WSServerConnector) || (conn instanceof HTTPServerConnector)) {
			Object obj = context.getLocalizedValue("httpRequest");
			if (obj instanceof BufferServletRequest) {
				BufferServletRequest httpRequest = (BufferServletRequest) obj;
				urlPath = httpRequest.getRequest().getRequestURL();
				servletPath = httpRequest.getRequest().getRequestURI();
				queryStr = httpRequest.getRequest().getQueryString();
				if (httpRequest.getHeader("x-forwarded-for") == null)
					clientIp = httpRequest.getRequest().getRemoteAddr();
				else {
					clientIp = httpRequest.getHeader("x-forwarded-for");
				}
				if (queryStr != null && !"".equals(queryStr)) {
					((ITransferableContext) context).setDelocalizedValue(
							"queryString", queryStr, PropertyType.RGLOBALVAR);
				}				
				String headerToString = null;
				Map<String, String> map = new HashMap<String, String>();
				Enumeration headerNames = httpRequest.getRequest().getHeaderNames();
				while (headerNames.hasMoreElements()) {
					String key = (String) headerNames.nextElement();
					if ("Content-Length".equalsIgnoreCase(key)) {
						continue;
					}
					if ("Host".equalsIgnoreCase(key)) {
						continue;
					}
					if ("X-Forwarded-For".equalsIgnoreCase(key)) {
						continue;
					}
					if ("Accept-Encoding".equalsIgnoreCase(key)) {
						continue;
					}
					String value = httpRequest.getHeader(key);
					map.put(key, value);
				}
				headerToString = HTTPTransfer.mapToString(map);

				((ITransferableContext) context).setDelocalizedValue(ContextConstants.HTTP_HEADERS, headerToString,
						PropertyType.RGLOBALVAR);
				log.debug(("HTTP Method: " + httpRequest.getRequest().getMethod() + "\n"));
				log.debug("Content Type: " + httpRequest.getRequest().getContentType() + "\n");
				log.debug("the request header is:" + headerToString);
			}
		}

		((ITransferableContext) context).setDelocalizedValue("FROMIP", clientIp, PropertyType.RGLOBALVAR);
		log.info("请求信息：请求方IP=" + clientIp + "，urlPath=" + urlPath);

		String[] str = servletPath.split("/");
		if (str.length >= 6) {
			String consumerId = str[2];
			String providerId = str[3];
			String servicesId = str[5];
			log.info("从请求中获取到调用信息，消费方ID=" + consumerId + "；提供方ID=" + providerId + "；服务ID=" + servicesId);
			context.removeDelocalizedValue(ContextConstants.REAL_CHANNEL);
			context.removeDelocalizedValue(ContextConstants.LOGIC_CHANNEL);
			context.removeDelocalizedValue(ContextConstants.IMMUNE_CHANNELID);

			if (consumerId != null) {
				((ITransferableContext) context).setDelocalizedValue(ContextConstants.REAL_CHANNEL, consumerId,
						PropertyType.RGLOBALVAR);
				((ITransferableContext) context).setDelocalizedValue(ContextConstants.IMMUNE_CHANNELID, consumerId,
						PropertyType.RGLOBALVAR);
				((ITransferableContext) context).setDelocalizedValue(ContextConstants.LOGIC_CHANNEL, consumerId,
						PropertyType.RGLOBALVAR);
			}
			if (providerId != null) {
				((ITransferableContext) context).setDelocalizedValue(ContextConstants.REAL_SYSTEM, providerId,
						PropertyType.RGLOBALVAR);
				((ITransferableContext) context).setDelocalizedValue(ContextConstants.LOGIC_SYSTEM, providerId,
						PropertyType.RGLOBALVAR);
			}
			if (servicesId != null) {
				((ITransferableContext) context).setDelocalizedValue(ContextConstants.IMMUNE_SERVICEID, servicesId,
						PropertyType.CONSTANT);
			}

		} else {
			log.error("前端消费方系统请求的URL地址不规范，请按规范地址调用！");
			AdaptorHelper.setError(context, "ESB-E-000077",
					"前端消费方系统请求的URL地址不规范，请按规范地址调用！");
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
