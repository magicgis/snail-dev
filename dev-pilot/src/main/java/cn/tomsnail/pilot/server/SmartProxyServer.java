package cn.tomsnail.pilot.server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import cn.tomsnail.pilot.model.CommandType;
import cn.tomsnail.pilot.model.Consts;
import cn.tomsnail.pilot.model.ProxyInfo;
import cn.tomsnail.pilot.model.ServiceProcess;
import cn.tomsnail.pilot.model.ServiceStatus;
import cn.tomsnail.pilot.model.SmartProxyInfo;
import cn.tomsnail.pilot.server.listener.IListenerHandler;
import cn.tomsnail.pilot.server.listener.ProxyListenerHandler;
import cn.tomsnail.pilot.server.source.ConfigSourceFactory;
import cn.tomsnail.pilot.server.source.ISource;


/**
 *        
 * @author yangsong
 * @version 0.0.1
 * @status 正常
 * @date 2016年7月6日 下午1:50:59
 * @see 
 */
public class SmartProxyServer extends ZkServer{
	
	private Map<String,ProxyInfo> proxySet = new ConcurrentHashMap<String, ProxyInfo>();
	
	private IServer serviceServer;
	
	private IListenerHandler handler;
	
	private ISource source;
	
	private IServer serviceKeeper;

	public SmartProxyServer(IServer serviceServer,IServer serviceKeeper){
		this.serviceServer = serviceServer;
		this.serviceKeeper = serviceKeeper;
	}
	
	@Override
	public void init() {
		this.handler = new ProxyListenerHandler(this);
		this.source = ConfigSourceFactory.instance().getDefaultSource();
		listenerHandler(Consts.ZK_PROXY, this.handler);
		initNodeInfo();
	}

	private void initNodeInfo(){
		Map<String,ProxyInfo> allProryInfo = this.source.getSourceData();
		if(allProryInfo!=null&&allProryInfo.size()>0){
			Iterator<String> it = allProryInfo.keySet().iterator();
			while(it.hasNext()){
				String _proxy = it.next();
				ProxyInfo proxyInfo = allProryInfo.get(_proxy);
				SmartProxyInfo _proxyInfo = new SmartProxyInfo();
				_proxyInfo.setConfigList(proxyInfo.getCommandInfo());
				_proxyInfo.setSelfInfo(proxyInfo.getSelfInfo());
				writeData(_proxy,_proxyInfo);
			}
		}
	}
	
	private synchronized void  change(String proxy,String commandType) {
		List<String> proxys = getPath(Consts.ZK_PROXY);
		if(proxys==null||proxys.size()==0) return;
		Map<String,SmartProxyInfo> _proxySet = new HashMap<String, SmartProxyInfo>();
		for(String _proxy:proxys){
			if(isSpeProxy(proxy, _proxy)){
				continue;
			}
			SmartProxyInfo _proxyInfo = getNewProxyInfo(commandType, _proxy);
			if(_proxyInfo==null){
				delete(Consts.ZK_PROXY+"/"+_proxy);
				continue;
			}
			writeData(_proxy, _proxyInfo);
			_proxySet.put(_proxy, _proxyInfo);
		}
		proxySet.clear();
		proxySet.putAll(_proxySet);
	}

	private void writeData(String _proxy, SmartProxyInfo _proxyInfo) {
		writeObject(Consts.ZK_ROOT+"/"+_proxy, _proxyInfo);

	}
	
	private SmartProxyInfo getNewProxyInfo(String commandType, String _proxy) {
		String proxyPath = Consts.ZK_ROOT+"/"+_proxy;
		SmartProxyInfo _proxyInfo = (SmartProxyInfo) readObject(proxyPath);
		ProxyInfo config_proxyInfo = this.source.getSourceData().get(_proxy);
		if(config_proxyInfo==null){
			return null;
		}
		List<ServiceProcess> _processes = this.source.getSourceData().get(_proxy).getCommandInfo();
		if(_processes!=null&&_processes.size()>0){
			List<ServiceProcess> processes = getProcessesWithCStatus(commandType, _proxy, _processes);
			_proxyInfo.setCommandInfo(processes);
		}
		return _proxyInfo;
	}

	private List<ServiceProcess> getProcessesWithCStatus(String commandType,
			String _proxy, List<ServiceProcess> _processes) {
		List<ServiceProcess> processes = new ArrayList<ServiceProcess>();
		for(ServiceProcess _process:_processes){
			try {
				ServiceProcess process = (ServiceProcess) _process.clone();
				processes.add(process);
				if(!process.getStatus().equals(ServiceStatus.DEBUG)){
					List<ServiceProcess> processInfos = getService(_proxy, process.getName());
					if(processInfos!=null){
						process.setStatus(ServiceStatus.ACTIVE);
					}else{
						process.setStatus(ServiceStatus.DEAD);
					}
				}
				if(commandType!=null){
					process.setCommand(commandType);
				}
			} catch (CloneNotSupportedException e) {
				e.printStackTrace();
			}
		}
		return processes;
	}

	private boolean isSpeProxy(String proxy, String _proxy) {
		return (proxy!=null&&!_proxy.equals(proxy))||_proxy.equals("servernode");
	}

	@Override
	public void start() {
		change(null,null);
		if(serviceKeeper!=null) serviceKeeper.start();
	}

	@Override
	public void stop() {
		
	}

	@Override
	public void notifly(String path, int type) {
		if(type==NotifType.NOTHING) return;
		change(null,CommandType.START);
	}

	@Override
	public List<ServiceProcess> getService(String node, String service) {
		return serviceServer.getService(node, service);
	}

}
