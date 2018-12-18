package fqcontrol.entity;


import fqcontrol.util.QuotaFrequencyConst;

public class FrequencyQuotaEntity {
    private String quotaType;
    private String interval;
    private int quota;

    private int intervalCount;
    private String intervalRul;

    public String getQuotaType() {
        return quotaType;
    }

    public void setQuotaType(String quotaType) {
        this.quotaType = quotaType;
    }

    public String getInterval() {
        return interval;
    }

    public void setInterval(String interval) {
        this.interval = interval;
    }

    public int getQuota() {
        return quota;
    }

    public void setQuota(int quota) {
        this.quota = quota;
    }

    public int getIntervalCount() {
        return intervalCount;
    }

    public void setIntervalCount(int intervalCount) {
        this.intervalCount = intervalCount;
    }

    public String getIntervalRul() {
        return intervalRul;
    }

    public void setIntervalRul(String intervalRul) {
        this.intervalRul = intervalRul;
    }


    public String getRedisKey(String identity){
        return String.format("FQ_%s_%s_%s", identity, quotaType, interval);
    }

    int expire = 0;
    public int getRedisExpire(){
        if(expire == 0){
            if(intervalRul.equals(QuotaFrequencyConst.Minute)){
                expire = intervalCount * 60;
            }else if(intervalRul.equals(QuotaFrequencyConst.Hour)){
                expire = intervalCount * 60 * 60;
            }else if(intervalRul.equals(QuotaFrequencyConst.Day)){
                expire = intervalCount * 60 * 60 * 24;
            }
        }
        return expire;
    }
}
