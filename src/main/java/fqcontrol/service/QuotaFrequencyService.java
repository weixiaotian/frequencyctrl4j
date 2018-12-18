package fqcontrol.service;

import java.sql.SQLException;

public interface QuotaFrequencyService {
    FrequencyResult incCheck(String identity, String quotaType);
    void inc(String identity, String quotaType);
    void reset(String identity, String quotaType);
    void reloadConfig() throws SQLException;

    public static class FrequencyResult{
        public FrequencyResult(boolean checkOk, String errorType){
            this.checkOk = checkOk;
            this.errorType = errorType;
        }
        private boolean checkOk;
        private String errorType;

        public boolean isCheckOk() {
            return checkOk;
        }

        public void setCheckOk(boolean checkOk) {
            this.checkOk = checkOk;
        }

        public String getErrorType() {
            return errorType;
        }

        public void setErrorType(String errorType) {
            this.errorType = errorType;
        }
    }
}
