package com.dc.esb.busi.impls.service;


import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;

import com.dc.esb.container.core.data.IServiceDataObject;
import com.dc.esb.container.core.sclite.IBaseContext;
import com.dc.esb.container.core.sclite.IBusinessService;
import com.dc.esb.container.core.sclite.InvokeException;
import com.dc.esb.container.core.sclite.ServiceMaintainException;
import com.dc.esb.container.log.MyLog;
import com.dc.esb.container.log.MyLogHelper;
import com.dc.esb.container.protocol.ContextDataHelper;
import com.dcfs.impls.esb.ESBConfig;

/**
 * <p>
 * 
 * <li>
 * </li>
 * </p>
 * 南钢ESB连通性探测服务
 * @author shijp 20220810
 * @E-Mail
 */
public class DetectService_esb implements IBusinessService {
private static final MyLog log = MyLogHelper.getFactory().getLog(DetectService_esb.class);

	public IServiceDataObject invoke(IServiceDataObject data, IBaseContext context)
			throws InvokeException {
		String loopip=null;
		try {
			loopip = getIp();
		} catch (Exception e1) {
			log.error(e1.getMessage(), e1);
		}
		String nodename = ESBConfig.getConfig().getProperty("identify.node");
		if(loopip!=null&&loopip.length()>0){
			if(nodename!=null&&nodename.length()>0){
				StringBuffer sb = new StringBuffer();
				sb.append("{\"code\":\"0\",\"message\":\"message\",\"nodename\":\"");
				sb.append(nodename);
				sb.append("\",\"loopip\":\"");
				sb.append(loopip);
				sb.append("\"}");
				try {
					ContextDataHelper.injectOriginalData(context, sb.toString().getBytes("utf-8"));
				} catch (UnsupportedEncodingException e) {
					log.error("ESB连通测试交易中测试服务编排响应报文失败",e);
				}
			}else{
				if(log.isErrorEnabled()){
					log.error("ESB连通测试交易中测试服务获取本路所在节点的名称失败!");
				}
			}
		}else{
			if(log.isErrorEnabled()){
				log.error("ESB连通测试交易中测试服务获取本路IP失败!");
			}
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
	

	/**
	 * 获取本机IP Creates a new instance of getIp . getIp
	 * 
	 * @return
	 * @throws UnknownHostException
	 * @throws SocketException
	 */
	public static String getIp() throws UnknownHostException, SocketException {
		if (isWindowsOS()) {
			return InetAddress.getLocalHost().getHostAddress();
		} else {
			return getLinuxLocalIp();
		}
	}

	/**
	 * 判断操作系统是否是Windows
	 * 
	 * @return
	 */
	private static boolean isWindowsOS() {
		boolean isWindowsOS = false;
		String osName = System.getProperty("os.name");
		if (osName.toLowerCase().indexOf("windows") > -1) {
			isWindowsOS = true;
		}
		return isWindowsOS;
	}

	/**
	 * 获取Linux下的IP地址
	 * 
	 * @return IP地址
	 * @throws SocketException
	 */
	private static String getLinuxLocalIp() throws SocketException {
		String ip = "";
		try {
			for (Enumeration<NetworkInterface> en = NetworkInterface
					.getNetworkInterfaces(); en.hasMoreElements();) {
				NetworkInterface intf = en.nextElement();
				String name = intf.getName();
				if (!name.contains("docker") && !name.contains("lo")) {
					for (Enumeration<InetAddress> enumIpAddr = intf
							.getInetAddresses(); enumIpAddr.hasMoreElements();) {
						InetAddress inetAddress = enumIpAddr.nextElement();
						if (!inetAddress.isLoopbackAddress()) {
							String ipaddress = inetAddress.getHostAddress()
									.toString();
							if (!ipaddress.contains("::")
									&& !ipaddress.contains("0:0:")
									&& !ipaddress.contains("fe80")) {
								ip = ipaddress;
							}
						}
					}
				}
			}
		} catch (SocketException ex) {
			System.out.println("获取ip地址异常");
			ip = "127.0.0.1";
			ex.printStackTrace();
		}
		return ip;
	}

	

}

