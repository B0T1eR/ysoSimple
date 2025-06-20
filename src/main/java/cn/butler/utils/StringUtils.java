package cn.butler.utils;

import java.util.Random;

public class StringUtils {
    public static String getRandStr(int length){
        String dicts = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random random = new Random();
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < length; i ++) {
            int index = random.nextInt(dicts.length());
            sb.append(dicts.charAt(index));
        }

        return "Exploit_" + sb;
    }

    public static String getRandUppercaseStr(int length){
        String dicts = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        Random random = new Random();
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < length; i ++) {
            int index = random.nextInt(dicts.length());
            sb.append(dicts.charAt(index));
        }

        return sb.toString();
    }
}
