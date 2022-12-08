package red.medusa.pojo;

import red.medusa.finddata.FindSystemDataProxy;

import java.util.HashMap;
import java.util.Map;

public class FindSystemDataTest {

    public static Map<String, String> CACHE = new HashMap<String, String>() {
        {
            this.put("1", "A");
            this.put("2", "B");
        }
    };

    public static void main(String[] args) throws Exception {
        FindSystemDataProxy findSystemDataProxy1 = new FindSystemDataProxy(Bean.class);
        FindSystemDataProxy findSystemDataProxy2 = new FindSystemDataProxy(Bean.class);
        Bean proxy1 = findSystemDataProxy1.from(new Bean("1"));
        Bean proxy2 = findSystemDataProxy2.newInstance("2");

        System.out.println(proxy1.getName());

        findSystemDataProxy1.findObjectByNumber(proxy1::getName)
                .with(System.out::println);
        findSystemDataProxy2.findObjectByNumber(proxy2::getName)
                .withNotNull(System.out::println);

        try {
            proxy2.setName("");
            findSystemDataProxy2.findObjectByNumber(proxy2::getName)
                    .withNotNull(System.out::println);
        } catch (Exception e) {
            System.out.println(e.getClass());   // java.lang.IllegalArgumentException
            System.out.println(e.getMessage()); // 张三是必录参数
        }

        try {
            proxy2.setName("3");
            findSystemDataProxy2.findObjectByNumber(proxy2::getName)
                    .withNotNull(System.out::println);
        } catch (Exception e) {
            System.out.println(e.getClass());   // java.lang.IllegalArgumentException
            System.out.println(e.getMessage()); // 张三在系统未匹配到对应数据
        }
    }
}
