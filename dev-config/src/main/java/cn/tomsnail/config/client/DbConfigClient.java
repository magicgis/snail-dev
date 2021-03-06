package cn.tomsnail.config.client;

import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;

import cn.tomsnail.starter.domain.spring.SpringBeanUtil;
import cn.tomsnail.util.configfile.ConfigHelp;
import cn.tomsnail.util.string.StringUtils;

public class DbConfigClient extends AConfigCilent{
	
	private JdbcTemplate jdbcTemplate;
	
	public DbConfigClient(AConfigCilent configCilent){
		this.configCilent = configCilent;
		try {
			if(isDo()){
				inited = true;
			}
		} catch (Exception e) {
		}
	}
	
	@Override
	public String getConfig(String key) {
		
		if(!isDo()){
			return null;
		}
		if(jdbcTemplate==null){
			init();
			if(jdbcTemplate==null){
				return null;
			}
		}
		
		if(StringUtils.isBlank(key)){
			return null;
		}
		
		String sql = "select value from sys_config where name = '"+key+"' and del_flag = '0' ";
		
		List<Map<String,Object>> maps =  jdbcTemplate.queryForList(sql);
		if(maps!=null&&maps.size()==1){
			Map m = maps.get(0);
			if(m!=null){
				Object v = m.get("value");
				if(v!=null){
					return v.toString();
				}
			}
		}
		
		return null;
	}

	@Override
	protected boolean isDo() {
		String iszkconfig = ConfigHelp.getInstance("config").getLocalConfig("system.dbconfig", "false");
		if(iszkconfig.equals("true")){
			return true;
		}
		return false;
	}
	
	private void init(){
		try {
			jdbcTemplate = (JdbcTemplate) SpringBeanUtil.getBean(JdbcTemplate.class);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}
