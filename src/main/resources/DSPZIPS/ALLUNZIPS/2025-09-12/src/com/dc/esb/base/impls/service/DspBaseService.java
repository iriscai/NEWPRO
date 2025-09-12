package com.dc.esb.base.impls.service;

import java.util.Map;

import com.dc.esb.container.core.data.IServiceDataObject;
import com.dc.esb.container.core.sclite.IBaseContext;
import com.dc.esb.container.core.sclite.IBaseService;
import com.dc.esb.container.core.sclite.ITransferableContext;
import com.dc.esb.container.core.sclite.InvokeException;
import com.dc.esb.container.core.sclite.ServiceMaintainException;
import com.dc.esb.container.core.sclite.trans.PropertyType;
import com.dc.esb.container.log.MyLog;
import com.dc.esb.container.log.MyLogHelper;
import com.dc.esb.container.protocol.http.HTTPTransfer;
import com.dc.esb.container.sclite.ContextConstants;

/**
 * 根据DSP需求，将渠道ID添加到http头中,dsp用于识别消息来源
 * 
 * @author shijp 20221011
 * @E-Mail 
 */
public class DspBaseService implements IBaseService {
	private static final MyLog log = MyLogHelper.getFactory().getLog(DspBaseService.class);

	public IServiceDataObject invoke(IServiceDataObject sdo, IBaseContext context) throws InvokeException {
		// TODO Auto-generated method stub
		String channel = context.getDelocalizedValue(ContextConstants.IMMUNE_CHANNELID);
		String headers = context.getDelocalizedValue("HTTP_HEADERS");
		String headerToString;

		if (headers != null) {
			Map<String, String> header = HTTPTransfer.stringToMap(headers);
			header.put("channelSystem", channel);
			headerToString = HTTPTransfer.mapToString(header);
			((ITransferableContext) context).setDelocalizedValue(ContextConstants.HTTP_HEADERS, headerToString,
					PropertyType.RGLOBALVAR);

		} else
			log.error("http头为空，请检查流程配置。");
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
