package cn.tomsnail.pilot.server;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.I0Itec.zkclient.IZkChildListener;

import cn.tomsnail.pilot.model.Consts;
import cn.tomsnail.pilot.model.ServiceProcess;
import cn.tomsnail.pilot.server.listener.IListenerHandler;
import cn.tomsnail.pilot.server.listener.NodeListenerHandler;
import cn.tomsnail.pilot.server.listener.ServiceListenerHandler;

/**
 *        
 * @author yangsong
 * @version 0.0.1
 * @status 正常
 * @date 2016年7月6日 下午1:54:39
 * @see 
 */
public class NodeServer  extends ZkServer {
		
	private IServer serviceServer ;
	
	private IListenerHandler handler;

	private Map<String,ServiceListenerHandler> nodeSet;
	
	private Map<String,IZkChildListener> childListenerSet;
	
	public NodeServer(IServer serviceServer){
		this.handler = new NodeListenerHandler(this);
		this.serviceServer = serviceServer;
	}

	@Override
	public void init() {
		nodeSet = new ConcurrentHashMap<String, ServiceListenerHandler>();
		childListenerSet = new ConcurrentHashMap<String, IZkChildListener>();
		listenerHandler(Consts.ZK_ROOT, this.handler);
		
	}

	@Override
	public void start() {
		List<String> nodes = getPath(Consts.ZK_ROOT);
		if(nodes==null||nodes.size()==0) return;
		for(String node:nodes){
			initService(node);
		}
	}

	private void initService(String node) {
		String nodePath =Consts.ZK_ROOT+"/"+node;
		final ServiceListenerHandler handler = new ServiceListenerHandler(serviceServer);
		nodeSet.put(node, handler);
		serviceServer.notifly(node, NotifType.SERVICE_CREATE);
		childListenerSet.put(node, registerChildListener(nodePath,handler));
	}

	@Override
	public void stop() {
		Iterator<String> it = nodeSet.keySet().iterator();
		while(it.hasNext()){
			String node = it.next();
			String nodePath =Consts.ZK_ROOT+"/"+node;
			IZkChildListener childListener =  childListenerSet.remove(node);
			unsubscribeChildListener(nodePath,childListener);
			ServiceListenerHandler handler = nodeSet.get(node);
			handler.close();
			handler = null;
			childListener = null;
		}
		nodeSet.clear();
		childListenerSet.clear();
	}

	@Override
	public void notifly(String path, int type) {
		if(type==NotifType.NOTHING) return;
		List<String> nodes = getPath(Consts.ZK_ROOT);
		if(nodes==null||nodes.size()==0) return;
		for(String node:nodes){
			if(nodeSet.containsKey(node)){
			}else{
				initService(node);
			}
		}
	}

	@Override
	public List<ServiceProcess> getService(String node, String service) {
		return serviceServer.getService(node, service);
	}
}
