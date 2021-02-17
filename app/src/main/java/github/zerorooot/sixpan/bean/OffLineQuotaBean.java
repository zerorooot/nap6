package github.zerorooot.sixpan.bean;


import lombok.Data;

@Data
public class OffLineQuotaBean {
    private int dailyUsed;
    private int dailyQuota;
    private int available;
}
