package com.dc.esb.base.impls.service;

import org.json.JSONException;
import com.dc.esb.container.protocol.http.HTTPTransfer;
import com.dc.esb.container.sclite.ContextConstants;
import org.json.JSONObject;

import com.dc.esb.container.adaptor.helper.AdaptorHelper;
import com.dc.esb.container.core.data.IServiceDataObject;
import com.dc.esb.container.core.sclite.IBaseContext;
import com.dc.esb.container.core.sclite.IBaseService;
import com.dc.esb.container.core.sclite.ITransferableContext;
import com.dc.esb.container.core.sclite.InvokeException;
import com.dc.esb.container.core.sclite.ServiceMaintainException;
import com.dc.esb.container.core.sclite.trans.PropertyType;
import com.dc.esb.container.log.MyLog;
import com.dc.esb.container.log.MyLogHelper;
import javax.servlet.http.HttpServletResponse;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * 南钢集团ESB针对DSP场景进行异常拦截处理
 * 
 * 
 * @author shijp 20221020
 * @E-Mail
 */
public class DspExceptionProcess implements IBaseService {
	private static final MyLog log = MyLogHelper.getFactory().getLog(DspExceptionProcess.class);

	public IServiceDataObject invoke(IServiceDataObject sdo, IBaseContext context) throws InvokeException {

		HttpServletResponse response = (HttpServletResponse) context.getLocalizedValue(ContextConstants.HTTPRESP);
		String headers = context.getDelocalizedValue("HTTP_HEADERS");
		Map<String, String> header = HTTPTransfer.stringToMap(headers);
		String dspFlowNo = header.get("uniqueFlowNo");
		String statusCode = null;

		if (dspFlowNo != null) {
			((ITransferableContext) context).setDelocalizedValue("dspFlowNo", dspFlowNo, PropertyType.RGLOBALVAR);
			log.debug("DSP流水号为：" + dspFlowNo);
		} else
			log.error("DSP流水号为空");

		String oldmsg = null;
		String newmsg = null;
		Object oobj = context.getLocalizedValue("originalData");
		if (oobj instanceof String) {
			oldmsg = (String) oobj;
		} else if (oobj instanceof byte[]) {
			byte[] obj = (byte[]) oobj;
			oldmsg = new String(obj);
		}

		if (null != oldmsg && oldmsg.contains("DSP-E")) {

			if ("200".equals(statusCode) || statusCode == null || statusCode.endsWith("")) {
				statusCode = "500";
				context.setDelocalizedValue(ContextConstants.HTTP_STATUS_CODE, statusCode);
			}

			newmsg = oldmsg.replaceAll("\n", "");

			JSONObject jsonObject;
			try {
				jsonObject = new JSONObject(newmsg.toString());
				String code = jsonObject.getString("stateCode");
				String message = jsonObject.getString("message");
				String[] str = message.split("at");
				if (str.length > 1) {
					message = str[0];
				}
				log.error("DSP交易失败，请联系DSP运维人员！");
				log.info("DSP错误原因：" + oldmsg);

				AdaptorHelper.setError(context, code, message);

			} catch (JSONException e) {

				// TODO Auto-generated catch block
				e.printStackTrace();
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

	public static void main(String msg[]) {
		String msg1 = "{\"stateCode\": \"DSP-E-000002\",\"message\": \"\nInvoking business at service DbSqlQuery_3 failed\"}";

		msg1 = msg1.replaceAll("\n", "");
		System.out.print(msg1);
		JSONObject jsonObject;
		try {
			jsonObject = new JSONObject(msg1.toString());
			String code = jsonObject.getString("stateCode");
			String message = jsonObject.getString("message");
			if (code.startsWith("DSP-E")) {

				String[] str = message.split("at");
				System.out.println(str.length);
				if (str.length > 1) {
					message = str[0];
					System.out.println(message);
				}
				System.out.println(message);

			}
		} catch (JSONException e) {
			e.printStackTrace();
		}

	}

}
