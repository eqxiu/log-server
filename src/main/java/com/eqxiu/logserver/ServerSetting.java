package com.eqxiu.logserver;

import cn.hutool.core.lang.Singleton;
import cn.hutool.core.util.StrUtil;
import com.eqxiu.logserver.action.Action;
import com.eqxiu.logserver.action.ErrorAction;
import com.eqxiu.logserver.annotation.Route;
import com.eqxiu.logserver.conf.SysConfigManager;
import com.eqxiu.logserver.filter.Filter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 全局设定文件
 *
 */
public class ServerSetting {
	private static final Logger log = LoggerFactory.getLogger(ServerSetting.class);
	
	//-------------------------------------------------------- Default value start
	/** 默认的字符集编码 */
	public final static String DEFAULT_CHARSET = "utf-8";
	
	public final static String MAPPING_ALL = "/*";
	
	public final static String MAPPING_ERROR = "/_error";
	//-------------------------------------------------------- Default value end
	
	/** 字符编码 */
	private static String charset = DEFAULT_CHARSET;
	/** 端口 */
//	private static int port = SysConfigManager.getInteger("server.port");
	private static int port = 8888;

	/** Filter映射表 */
	private static Map<String, Filter> filterMap;
	/** Action映射表 */
	private static Map<String, Action> actionMap;
	
	static{
		filterMap = new ConcurrentHashMap<String, Filter>();
		
		actionMap = new ConcurrentHashMap<String, Action>();
		actionMap.put(StrUtil.SLASH, new ErrorAction());
		actionMap.put(MAPPING_ERROR, new ErrorAction());
		actionMap.put(MAPPING_ALL, new ErrorAction());
	}
	
	/**
	 * @return 获取编码
	 */
	public static String getCharset() {
		return charset;
	}
	/**
	 * @return 字符集
	 */
	public static Charset charset() {
		return Charset.forName(getCharset());
	}
	
	/**
	 * 设置编码
	 * @param charset 编码
	 */
	public static void setCharset(String charset) {
		ServerSetting.charset = charset;
	}
	
	/**
	 * @return 监听端口
	 */
	public static int getPort() {
		return port;
	}
	/**
	 * 设置监听端口
	 * @param port 端口
	 */
	public static void setPort(int port) {
		ServerSetting.port = port;
	}
	
	/**
	 * @return 获取FilterMap
	 */
	public static Map<String, Filter> getFilterMap() {
		return filterMap;
	}
	/**
	 * 获得路径对应的Filter
	 * @param path 路径，为空时将获得 根目录对应的Action
	 * @return Filter
	 */
	public static Filter getFilter(String path){
		if(StrUtil.isBlank(path)){
			path = StrUtil.SLASH;
		}
		return getFilterMap().get(path.trim().replaceAll("/{2,}", "/"));
	}
	/**
	 * 设置FilterMap
	 * @param filterMap FilterMap
	 */
	public static void setFilterMap(Map<String, Filter> filterMap) {
		ServerSetting.filterMap = filterMap;
	}
	
	/**
	 * 设置Filter类，已有的Filter类将被覆盖
	 * @param path 拦截路径（必须以"/"开头）
	 * @param filter Action类
	 */
	public static void setFilter(String path, Filter filter) {
		if(StrUtil.isBlank(path)){
			path = StrUtil.SLASH;
		}
		
		if(null == filter) {
			log.warn("Added blank action, pass it.");
			return;
		}
		//所有路径必须以 "/" 开头，如果没有则补全之
		if(false == path.startsWith(StrUtil.SLASH)) {
			path = StrUtil.SLASH + path;
		}
		
		ServerSetting.filterMap.put(SysConfigManager.getProperty("url.prefix") + path, filter);
	}
	
	/**
	 * 设置Filter类，已有的Filter类将被覆盖
	 * @param path 拦截路径（必须以"/"开头）
	 * @param filterClass Filter类
	 */
	public static void setFilter(String path, Class<? extends Filter> filterClass) {
		setFilter(path, (Filter)Singleton.get(filterClass));
	}
	//----------------------------------------------------------------------------------------------- Filter end
	
	//----------------------------------------------------------------------------------------------- Action start
	/**
	 * @return 获取ActionMap
	 */
	public static Map<String, Action> getActionMap() {
		return actionMap;
	}
	/**
	 * 获得路径对应的Action
	 * @param path 路径，为空时将获得 根目录对应的Action
	 * @return Action
	 */
	public static Action getAction(String path){
		if(StrUtil.isBlank(path)){
			path = StrUtil.SLASH;
		}
		return getActionMap().get(path.trim().replaceAll("/{2,}", "/"));
	}
	/**
	 * 设置ActionMap
	 * @param actionMap ActionMap
	 */
	public static void setActionMap(Map<String, Action> actionMap) {
		ServerSetting.actionMap = actionMap;
	}
	
	/**
	 * 设置Action类，已有的Action类将被覆盖
	 * @param path 拦截路径（必须以"/"开头）
	 * @param action Action类
	 */
	public static void setAction(String path, Action action) {
		if(StrUtil.isBlank(path)){
			path = StrUtil.SLASH;
		}
		
		if(null == action) {
			log.warn("Added blank action, pass it.");
			return;
		}
		//所有路径必须以 "/" 开头，如果没有则补全之
		if(false == path.startsWith(StrUtil.SLASH)) {
			path = StrUtil.SLASH + path;
		}
		log.info("add action "+path+" ["+action.getClass()+"]");
		ServerSetting.actionMap.put(SysConfigManager.getProperty("url.prefix")+path, action);
	}
	
	/**
	 * 增加Action类，已有的Action类将被覆盖<br>
	 * 所有Action都是以单例模式存在的！
	 * @param path 拦截路径（必须以"/"开头）
	 * @param actionClass Action类
	 */
	public static void setAction(String path, Class<? extends Action> actionClass) {
		setAction(path, (Action) Singleton.get(actionClass));
	}
	
	/**
	 * 增加Action类，已有的Action类将被覆盖<br>
	 * 自动读取Route的注解来获得Path路径
	 * @param action 带注解的Action对象
	 */
	public static void setAction(Action action) {
		final Route route = action.getClass().getAnnotation(Route.class);
		if(route != null){
			final String path = route.value();
			
			if(StrUtil.isNotBlank(path)){
				setAction(path, action);
				return;
			}
		}
//		throw new ServerSettingException("Can not find Route annotation,please add annotation to Action class!");
	}
	
	/**
	 * 增加Action类，已有的Action类将被覆盖<br>
	 * 所有Action都是以单例模式存在的！
	 * @param actionClass 带注解的Action类
	 */
	public static void setAction(Class<? extends Action> actionClass) {
		setAction((Action)Singleton.get(actionClass));
	}
	//----------------------------------------------------------------------------------------------- Action start
	
}
