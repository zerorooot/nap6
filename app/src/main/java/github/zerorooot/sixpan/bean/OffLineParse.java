package github.zerorooot.sixpan.bean;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OffLineParse {
    private String hash;
    private String textLink;
    private boolean ready = false;


}
