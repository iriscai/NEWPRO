package com.dc.esb.base.impls.service;

import com.dc.esb.container.adaptor.helper.AdaptorHelper;
import com.dc.esb.container.core.data.IServiceDataObject;
import com.dc.esb.container.core.sclite.IBaseContext;
import com.dc.esb.container.core.sclite.IBaseService;
import com.dc.esb.container.core.sclite.InvokeException;
import com.dc.esb.container.core.sclite.ServiceMaintainException;
import com.dc.esb.container.exception.ESBCodes;
import com.dc.esb.container.log.MyLog;
import com.dc.esb.container.log.MyLogHelper;
import com.dc.esb.container.sclite.ContextConstants;

/**
 * <p>
 * 
 * <li></li>
 * </p>
 * 南钢ESB异常处理定制开发
 * 
 * @author shijp 20220606
 * @E-Mail 
 */
public class ExceptionPackService implements IBaseService {

	private final MyLog log = MyLogHelper.getFactory().getLog(ExceptionPackService.class);

	public IServiceDataObject invoke(IServiceDataObject data, IBaseContext context) throws InvokeException {
		if (!"on".equals(context.getLocalizedValue("processed"))) {
			try {
				String serviceid = context.getDelocalizedValue(ContextConstants.IMMUNE_SERVICEID);
				String sysid = context.getDelocalizedValue(ContextConstants.REAL_SYSTEM);
				String statusCode = context.getDelocalizedValue(ContextConstants.HTTP_STATUS_CODE);
				String channelid = context.getDelocalizedValue(ContextConstants.IMMUNE_CHANNELID);
				String esbflowno = context.getDelocalizedValue(ContextConstants.UNIQUEFLOWNO);
				String url = context.getDelocalizedValue("HTTP_SERVICE_URL");
				String esbmessage = null;
				String esbcode = context.getDelocalizedValue(ContextConstants.CODE);

				if ("200".equals(statusCode) && esbcode == null) {
					return data;
				}

				if (null != statusCode) {
					int httpcode = Integer.parseInt(statusCode);
					switch (httpcode) {
					case 404:
						esbmessage = "ESB调用" + sysid + "服务" + serviceid + "异常[HTTP_STATUS_CODE:" + statusCode
								+ "],业务接口地址[" + url + "]";
						AdaptorHelper.setError(context, ESBCodes.CODE_E000404, esbmessage);
						break;
					case 497:
						esbmessage = "ESB调用" + sysid + "服务" + serviceid + "异常[HTTP_STATUS_CODE:" + statusCode
								+ "]建链超时,业务接口地址[" + url + "]";
						AdaptorHelper.setError(context, ESBCodes.CODE_E000497, esbmessage);
						break;
					case 498:
						esbmessage = "ESB调用" + sysid + "服务" + serviceid + "异常[HTTP_STATUS_CODE:" + statusCode
								+ "]拒绝连接,业务接口地址[" + url + "]";
						AdaptorHelper.setError(context, ESBCodes.CODE_E000498, esbmessage);
						break;
					case 499:
						esbmessage = "ESB调用" + sysid + "服务" + serviceid + "异常[HTTP_STATUS_CODE:" + statusCode + "]，"
								+ sysid + "系统内部处理超时，后端接口地址[" + url + "]";
						AdaptorHelper.setError(context, ESBCodes.CODE_E000499, esbmessage);
						break;
					case 500:
						esbmessage = "ESB调用" + sysid + "服务" + serviceid + "异常[HTTP_STATUS_CODE:" + statusCode + "],"
								+ sysid + "系统内部异常，后端接口地址[" + url + "]";
						AdaptorHelper.setError(context, ESBCodes.CODE_E000500, esbmessage);
						break;
					default:
						if (log.isWarnEnabled()) {
							log.warn("ESB调用服务" + "未知的错误类型,statusCode  = " + statusCode);
						}
						break;
					}

				} else if (null != esbcode) {
					if (esbcode.equals("ESB-E-000002")) {
						context.removeDelocalizedValue(ContextConstants.MESSAGE);

						esbmessage = "ESB调用" + sysid + "的服务" + serviceid + "异常,接出地址拒绝连接或超时";

					} else if (esbcode.equals("ESB-E-000024") || esbcode.equals("ESB-E-000001")) {
						context.removeDelocalizedValue(ContextConstants.MESSAGE);
						esbmessage = "ESB调用" + sysid + "的服务" + serviceid + "内部处理超时";

					}

					else if (esbcode.equals("ESB-E-000008")) {
						context.removeDelocalizedValue(ContextConstants.MESSAGE);
						esbmessage = "请求方调用地址中请求方系统名:" + channelid + "未在ESB注册成功，请核实";

					} else if (esbcode.equals("ESB-E-000009")) {

						context.removeDelocalizedValue(ContextConstants.MESSAGE);
						esbmessage = "请求方调用地址中服务名:" + serviceid + "校验失败，请核实";

					} else if (esbcode.equals("ESB-E-000053")) {
						context.removeDelocalizedValue(ContextConstants.MESSAGE);

						esbmessage = "请求方调用地址中服务名:" + serviceid + "不存在，请核实";

					} else if (esbcode.equals("ESB-E-000048") || esbcode.equals("ESB-E-000047")) {
						context.removeDelocalizedValue(ContextConstants.MESSAGE);
						esbmessage = "ESB解析报文失败,请核实报文内容、请求系统ID、服务名是否正确";

					} else if (esbcode.startsWith("ESB-E")) {
						context.setDelocalizedValue(ContextConstants.MESSAGE,
								context.getDelocalizedValue(ContextConstants.MESSAGE));
					}
					context.setDelocalizedValue(ContextConstants.MESSAGE, esbmessage);
				}

				if (!ESBCodes.isSuccessful(context)) {
					if (null == channelid) {
						packmsg(context, "json");
					} else {
						switch (channelid) {
						case "OA":
							packmsg(context, "json");
							break;
						case "ERP":
							packmsg(context, "json");
							break;
						default:
							packmsg(context, "json");
							break;
						}
					}

				}

			} catch (Exception e) {
				log.error("ESB封装异常报文失败");
				e.printStackTrace();
			}
			return data;
		}

		return data;
	}

	public void packmsg(IBaseContext context, String type) {
		String esbCode = context.getDelocalizedValue(ContextConstants.CODE);
		String emessage = context.getDelocalizedValue(ContextConstants.MESSAGE).trim();
		String esbflowno = context.getDelocalizedValue("UniqueFlowNo");
		if (type.equals("json")) {
			String message = "{\n  \"SHEAD\":{\n      \"retType\": \"E\",\n      " + "\"retCode\": \"" + esbCode
					+ "\",\n      " + "\"retMessage\": \"" + emessage + "\",\n      " + "\"esbFlowNo\": \"" + esbflowno
					+ "\"\n  }\n}";
			context.removeDelocalizedValue(ContextConstants.ORIGINALDATA);
			context.removeDelocalizedValue(ContextConstants.MESSAGE);
			context.setLocalizedValue(ContextConstants.ORIGINALDATA, message);
			context.setLocalizedValue("processed", "on");
		} else if (type.equals("soap")) {
			String messgae = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
					+ "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">\n" + "  <soap:Body>\n"
					+ "  <soap:Fault>\n" + "     <faultcode>" + esbCode + "</faultcode>\n" + "     <faultstring>"
					+ transfer(emessage) + "</faultstring>\n" + "  </soap:Fault>\n" + "  </soap:Body>\n"
					+ "</soap:Envelope>";
			context.removeDelocalizedValue(ContextConstants.ORIGINALDATA);
			// context.removeDelocalizedValue(ContextConstants.CODE);
			context.removeDelocalizedValue(ContextConstants.MESSAGE);
			context.setLocalizedValue(ContextConstants.ORIGINALDATA, messgae);
			context.setLocalizedValue("processed", "on");
		} else if (type.equals("xml")) {
			String messgae = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + "<response>\n" + "   <retType>" + "E"
					+ "</retType>\n" + "   <retCode>" + esbCode + "</retCode>\n" + "   <retMessage>"
					+ transfer(emessage) + "</retMessage>\n" + "   <esbflowno>" + esbflowno + "</esbflowno>\n"
					+ "</response>";
			context.removeDelocalizedValue(ContextConstants.ORIGINALDATA);
			// context.removeDelocalizedValue(ContextConstants.CODE);
			context.removeDelocalizedValue(ContextConstants.MESSAGE);
			context.setLocalizedValue(ContextConstants.ORIGINALDATA, messgae);
			context.setLocalizedValue("processed", "on");
		}
	}

	public void setErroMessage() {

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

	private String transfer(String emessage) {
		return emessage.replace("<", "[").replace(">", "]");
	}

	/**
	 * 判断是否是成功的
	 * 
	 * @param code
	 * @return
	 */
	/*
	 * public static boolean isSuccessful(IBaseContext context) { return
	 * context.getDelocalizedValue(ContextConstants.CODE) == null; }
	 */
	public static void main(String args[]) {
		// String message ="{\n \"retType\": \"E\",\n " + "\"retCode\": \"" +
		// "ESB-E-000060" + "\",\n " + "\"retMessage\":
		// \"请求方系统IP10.7.29.190不是ESB渠道[IDMS]信任的IP，不允许访问ESB\"\n}";

		// String messgae = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
		// + "<response>\n"
		// + " <retType>" + "E" + "</retType>\n"
		// + " <retCode>" + "E" + "</retCode>\n"
		// + " <retMessage>" + "E" + "</retMessage>\n"
		// + " <esbflowno>" + "E" + "</esbflowno>\n"
		// + "</response>";
		String messgae = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
				+ "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">\n" + "  <soap:Body>\n"
				+ "  <soap:Fault>\n" + "     <faultcode>" + "00000" + "</faultcode>\n" + "     <faultstring>" + "00000"
				+ "</faultstring>\n" + "  </soap:Fault>\n" + "  </soap:Body>\n" + "</soap:Envelope>";
		System.out.println(messgae);
	}
}
