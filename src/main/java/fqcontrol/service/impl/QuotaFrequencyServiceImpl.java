package fqcontrol.service.impl;

import fqcontrol.entity.FrequencyQuotaEntity;
import fqcontrol.service.QuotaFrequencyService;
import fqcontrol.util.JedisClusterUtils;
import fqcontrol.util.QuotaFrequencyConst;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

@Service
public class QuotaFrequencyServiceImpl implements QuotaFrequencyService {
    private static Logger logger = LoggerFactory.getLogger(QuotaFrequencyServiceImpl.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private RedisTemplate redisTemplate;

    private Map<String, List<FrequencyQuotaEntity>> configMap = new HashMap<>();

    @PostConstruct
    public void init() {
        logger.info("QuotaFrequencyServiceImpl begin init");
        try {
            reloadConfig();
        } catch (SQLException e) {
            logger.error("init error !", e);
        }
    }

    @Override
    public FrequencyResult incCheck(String identity, String quotaType) {
        try {
            if (!configMap.containsKey(quotaType)) {
                return new FrequencyResult(true, "");
            }

            List<FrequencyQuotaEntity> entities = configMap.get(quotaType);
            if(entities.size() > 1){
                FrequencyResult e = check(identity, entities);
                if (e != null) return e;
            }
            for (FrequencyQuotaEntity e : entities) {
                String redisKey = e.getRedisKey(identity);
                long currentValue = JedisClusterUtils.incr(redisKey);
                if (currentValue == 1)
                    JedisClusterUtils.expire(redisKey, e.getRedisExpire());

                if (currentValue > e.getQuota()) {
                    return new FrequencyResult(false, String.format("%s_%s", e.getQuotaType(), e.getInterval()));
                }
            }
            return new FrequencyResult(true, "");
        }catch (Exception ex){
            logger.error("incCheck error!!", ex);
            return new FrequencyResult(true, "");
        }
    }

    private FrequencyResult check(String identity, List<FrequencyQuotaEntity> entities) {
        for(FrequencyQuotaEntity e : entities){
            String redisKey = e.getRedisKey(identity);
            String strValue = JedisClusterUtils.getString(redisKey);
            long currentValue = strValue == null ? 0 : Long.valueOf(strValue);
            if (currentValue >= e.getQuota()) {
                return new FrequencyResult(false, String.format("%s_%s", e.getQuotaType(), e.getInterval()));
            }
        }
        return null;
    }

    @Override
    public void inc(String identity, String quotaType) {
        try {
            if (!configMap.containsKey(quotaType)) {
                return;
            }
            List<FrequencyQuotaEntity> entities = configMap.get(quotaType);
            for (FrequencyQuotaEntity e : entities) {
                String redisKey = e.getRedisKey(identity);
                long currentValue = JedisClusterUtils.incr(redisKey);
                if (currentValue == 1)
                    JedisClusterUtils.expire(redisKey, e.getRedisExpire());
            }
        }catch (Exception ex){
            logger.error("inc error!!", ex);
        }
    }

    @Override
    public void reset(String identity, String quotaType) {
        try {
            if (!configMap.containsKey(quotaType)) {
                return;
            }
            List<FrequencyQuotaEntity> entities = configMap.get(quotaType);
            for (FrequencyQuotaEntity e : entities) {
                String redisKey = e.getRedisKey(identity);
                JedisClusterUtils.delKey(redisKey);
            }
        }catch (Exception ex){
            logger.error("reset error!!", ex);
        }
    }

    @Override
    public void reloadConfig() throws SQLException {
        logger.info("begin load config");
        configMap.clear();
        List<FrequencyQuotaEntity> quotaEntities = loadFrequencyQuotaConfigs();
        quotaEntities.forEach(quotaEntity-> {
            try{
                int length = quotaEntity.getInterval().length();
                String intervalRule = quotaEntity.getInterval().substring(length-1, length);
                if(!intervalRule.equals(QuotaFrequencyConst.Minute) &&
                        !intervalRule.equals(QuotaFrequencyConst.Hour) &&
                        !intervalRule.equals(QuotaFrequencyConst.Day)){
                    logger.error("not support this config {} {}  {} intervalRule {}",quotaEntity.getQuotaType(),quotaEntity.getInterval(), quotaEntity.getQuota(), intervalRule);
                }
                int intervalCount = Integer.valueOf(quotaEntity.getInterval().substring(0, length-1));
                quotaEntity.setIntervalCount(intervalCount);
                quotaEntity.setIntervalRul(intervalRule);
                if(!configMap.containsKey(quotaEntity.getQuotaType())){
                    List<FrequencyQuotaEntity> arr = new ArrayList<>();
                    arr.add(quotaEntity);
                    configMap.put(quotaEntity.getQuotaType(), arr);
                }else {
                    configMap.get(quotaEntity.getQuotaType()).add(quotaEntity);
                }
            }catch (Exception ex){
                logger.error("error process config {} {}  {}",
                        quotaEntity.getQuotaType(),quotaEntity.getInterval(), quotaEntity.getQuota(), ex);
            }
        });
        configMap.values().forEach(list->
        {
            Collections.sort(list, new Comparator<FrequencyQuotaEntity>() {
                @Override
                public int compare(FrequencyQuotaEntity o1, FrequencyQuotaEntity o2) {
                    return o1.getQuota() - o2.getQuota();
                }
            });
        });
        logger.info("load config ok");
    }

    private List<FrequencyQuotaEntity> loadFrequencyQuotaConfigs() throws SQLException {
        try{
            return jdbcTemplate.query("select quota_type ,`interval`,quota from mss_quota_frequency", new RowMapper<FrequencyQuotaEntity>() {
                @Nullable
                @Override
                public FrequencyQuotaEntity mapRow(ResultSet resultSet, int i) throws SQLException {
                    FrequencyQuotaEntity entity = new FrequencyQuotaEntity();
                    entity.setInterval(resultSet.getString("interval"));
                    entity.setQuotaType(resultSet.getString("quota_type"));
                    entity.setQuota(resultSet.getInt("quota"));
                    return entity;
                }
            });
        }catch (Exception ex){
            logger.error("load configs from db error!", ex);
            throw  ex;
        }
    }
}
