package fqcontrol.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisZSetCommands.Tuple;
import org.springframework.data.redis.core.*;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;


@Component
public class JedisClusterUtils {  
  
	@Autowired
    private RedisTemplate<String, String> redisTemplate;
  
    private static JedisClusterUtils cacheUtils;
  
    @PostConstruct  
    public void init() {  
        cacheUtils = this;  
        cacheUtils.redisTemplate = this.redisTemplate;  
    }  
  

    /** 
     * 从缓存中取得字符串数据 
     * 
     * @param key 
     * @return 数据 
     */  
    public static String getString(String key) {  
        //cacheUtils.redisTemplate.opsForValue().get(key);
  
        return cacheUtils.redisTemplate.opsForValue().get(key);  
    }

    /**
     * 设置超时时间
     *
     * @param key
     * @param seconds
     */
    public static void expire(String key, int seconds) {
        cacheUtils.redisTemplate
                .execute((RedisCallback<Boolean>) connection -> connection.expire(key.getBytes(), seconds));

    }

    /**
     * 从缓存中删除数据
     *
     * @param key
     * @return
     */
    public static void delKey(String key) {

        cacheUtils.redisTemplate.execute((RedisCallback<Long>) connection -> connection.del(key.getBytes()));
    }

    /** 
     * 
     * 根据key增长 ，计数器 
     * 
     * @param key 
     * @return 
     */  
    public static long incr(String key) {  
  
        return cacheUtils.redisTemplate.execute((RedisCallback<Long>) connection -> {
            return connection.incr(key.getBytes());  
        });  
    }  
  

}  
