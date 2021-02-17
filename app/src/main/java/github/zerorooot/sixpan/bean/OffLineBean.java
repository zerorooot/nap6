package github.zerorooot.sixpan.bean;

import lombok.Data;


@Data
public class OffLineBean {
    private String name;
    private String savePath;
    private String accessPath;
    private String textLink;
    private String accessIdentity;
    private int progress;
    private long size;
    private String sizeString;
    private long createTime;
    private String time;
    private String fileMime;
    private boolean directory;
    private String taskIdentity;

    private boolean select = false;
}
