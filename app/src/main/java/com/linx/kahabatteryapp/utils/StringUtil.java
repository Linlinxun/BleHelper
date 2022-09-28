package com.linx.kahabatteryapp.utils;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class StringUtil {
    private static final String TAG = StringUtil.class.getName();

    public static List<Byte> stringToByte(String s) {
        List<Byte> list = new ArrayList();
        try {
            for (int i = 0; i < s.length() - 1; i = i + 2) {
                String str = s.substring(i, i + 2);
                list.add((byte) Integer.parseInt(str, 16));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public static List<String> toList(String[] dataList) {
        if (dataList.length == 0) {
            return null;
        }
        List<String> list = new ArrayList<>();
        for (int i = 0; i < dataList.length; i++) {
            list.add(dataList[i]);
        }
        return list;
    }

    public static String[] subArray(String[] datalist, int start) {
        return subArray(datalist, start, datalist.length);
    }

    public static String[] subArray(String[] datalist, int start, int end) {
        if (start >= end) {
            Log.d(TAG, "subArray: 开始下标需小于结束下标");
            return null;
        }
        String[] array = new String[end - start];
        int j = start;
        for (int i = 0; i < end - start; i++) {
            array[i] = datalist[j];
            j++;
        }
        return array;
    }


    public static byte[] hexStringToBytes(String hex) {
        if ((hex == null) || (hex.equals(""))) {
            return null;
        } else {
            if (hex.length() % 2 != 0){
                hex=0+hex;
            }
            hex = hex.toUpperCase();
            int len = hex.length() / 2;
            byte[] b = new byte[len];
            char[] hc = hex.toCharArray();
            for (int i = 0; i < len; i++) {
                int p = 2 * i;
                b[i] = (byte) (charToByte(hc[p]) << 4 | charToByte(hc[p + 1]));
            }
            return b;
        }
    }

    private static byte charToByte(char c) {
        return (byte) "0123456789ABCDEF".indexOf(c);
    }



    // 转化十六进制编码为字符串
    public static String toStringHex2(String s) {
        byte[] baKeyword = new byte[s.length() / 2];
        for (int i = 0; i < baKeyword.length; i++) {
            try {
                baKeyword[i] = (byte) (0xff & Integer.parseInt(s.substring(
                        i * 2, i * 2 + 2), 16));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        try {
            s = new String(baKeyword, "utf-8");// UTF-16le:Not
        } catch (Exception e1) {
            e1.printStackTrace();
        }
        return s;
    }
}
