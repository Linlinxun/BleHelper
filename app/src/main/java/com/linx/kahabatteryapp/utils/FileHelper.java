package com.linx.kahabatteryapp.utils;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import com.linx.kahabatteryapp.App;
import com.linx.kahabatteryapp.bean.BatteryInfoBean;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import jxl.Workbook;
import jxl.WorkbookSettings;
import jxl.format.Colour;
import jxl.write.Label;
import jxl.write.Number;
import jxl.write.WritableCellFormat;
import jxl.write.WritableFont;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;
import jxl.write.WriteException;

/**
 * author : linxun
 * create on 2022/9/6
 * explain:保存数据为excel表格
 */
public class FileHelper {

    private static FileHelper INSTANCE = new FileHelper();

    private WritableCellFormat arial10format = null;

    private WritableFont arial10font = null;

    private WritableFont arial12font = null;

    private WritableCellFormat arial12format = null;

    private static String UTF8_ENCODING = "UTF-8";

    private String timeName = "";

    public static FileHelper getInstance() {
        return INSTANCE;
    }

    public void resetTimeName() {
        this.timeName = "";
    }

    public FileHelper() {
        try {
            arial10font = new WritableFont(WritableFont.ARIAL, 10, WritableFont.BOLD);
            arial10format = new WritableCellFormat(arial10font);
            arial10format.setAlignment(jxl.format.Alignment.CENTRE);
            arial10format.setBorder(jxl.format.Border.ALL, jxl.format.BorderLineStyle.THIN);
            arial10format.setBackground(Colour.GRAY_25);

            arial12font = new WritableFont(WritableFont.ARIAL, 10);
            arial12format = new WritableCellFormat(arial12font);
            arial12format.setAlignment(jxl.format.Alignment.CENTRE);
            arial12format.setBorder(jxl.format.Border.ALL, jxl.format.BorderLineStyle.THIN);
        } catch (WriteException e) {
            e.printStackTrace();
        }

    }

    public void saveBatteryInfoExcel(Context context, String deviceAddress, List<BatteryInfoBean> dataList) throws Exception {
        String excelDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath() + File.separator + "KaHa Tool" + File.separator + "Historical data" + File.separator;
        String[] title = {"Time", "Electricity(%)", "Voltage(V)"};
        String sheetName = "BatteryInfo_data";
        String address = deviceAddress.replace(":", "");
        if (Objects.equals(timeName, "")) {
            timeName = Utils.INSTANCE.getCountCurrentDate();
        }
        String fileName = "Battery_" + address.substring(address.length() - 4) + "_" + timeName + ".xls";
        String excelPath = Environment.DIRECTORY_DOWNLOADS + File.separator + "KaHa Tool" + File.separator + "Historical data" + File.separator;
        String filePath = excelDir + fileName;
        OutputStream outputStream = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Uri uri = MediaStore.Files.getContentUri("external");
            ContentValues value = new ContentValues();
            value.put(MediaStore.Downloads.RELATIVE_PATH, excelPath);
            value.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
            Uri fileUri = context.getContentResolver().insert(uri, value);
            outputStream = context.getContentResolver().openOutputStream(fileUri);
        } else {
            File directory = new File(excelDir);
            if (!directory.exists()) {
                directory.mkdirs();
            }
            outputStream = new FileOutputStream(filePath);
        }
        writeBatteryExcel(outputStream, sheetName, title, dataList, filePath);
    }

    private void writeBatteryExcel(OutputStream os, String sheetName, String[] colName, List<BatteryInfoBean> objList, String filePath) throws Exception {
        WritableWorkbook workbook = null;
        workbook = Workbook.createWorkbook(os);
        //设置表格的名字
        WritableSheet sheet = workbook.createSheet(sheetName, 0);
        for (int col = 0; col < colName.length; col++) {
            sheet.addCell(new Label(col, 0, colName[col], arial10format));
        }
        //设置行高
        sheet.setRowView(0, 340);
        if (objList != null && objList.size() > 0) {
            WorkbookSettings setEncode = new WorkbookSettings();
            setEncode.setEncoding(UTF8_ENCODING);
            for (int j = 0; j < objList.size(); j++) {
                BatteryInfoBean bean = objList.get(j);
                List<String> list = new ArrayList<>();
                list.add(bean.getTime());
                list.add("" + bean.getElectricity());
                list.add("" + bean.getVoltage());

                for (int i = 0; i < list.size(); i++) {
                    if (i == 1) {
                        sheet.addCell(new Number(i, j + 1, Integer.parseInt(list.get(i)), arial12format));
                    } else if (i == 2) {
                        sheet.addCell(new Number(i, j + 1, Double.parseDouble(list.get(i)), arial12format));
                    } else {
                        sheet.addCell(new Label(i, j + 1, list.get(i), arial12format));
                    }

                    if (list.get(i).length() <= 3) {
                        //设置列宽
                        sheet.setColumnView(i, list.get(i).length() + 8);
                    } else {
                        //设置列宽
                        sheet.setColumnView(i, list.get(i).length() + 5);
                    }
                }
                //设置行高
                sheet.setRowView(j + 1, 350);
            }
            Toast.makeText(App.Companion.getInstance(), "The file save in:" + filePath, Toast.LENGTH_SHORT).show();
            workbook.write();
            workbook.close();
            os.close();
        }
    }

}
