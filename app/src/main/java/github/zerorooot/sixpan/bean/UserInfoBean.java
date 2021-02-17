package github.zerorooot.sixpan.bean;

import lombok.Data;

@Data
public class UserInfoBean {
    private String name;
    private String icon;
    private long spaceCapacity;
    private String spaceCapacitySize;
    private long spaceUsed;
    private String spaceUsedSize;
    private long vipExpireTime;
    private String vipExpireTimeString;
}
