package red.medusa.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import red.medusa.finddata.SystemDataInfo;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Bean {

    @SystemDataInfo(value = "张三", entityNumber = "本地系统数据源", requireMsg = true, requireMsgForLocalSystem = true)
    public String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}




