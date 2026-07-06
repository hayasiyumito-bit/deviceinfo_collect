package com.android.device.hardware;

import android.content.Context;
import android.hardware.input.InputManager;
import android.view.InputDevice;

import org.json.JSONArray;

public class InputDevices {
    /**
     * 获取输入设备名称列表
     *
     * @param context 上下文对象
     * @return 包含输入设备名称的JSONArray对象
     */
    public static JSONArray getInputDevices(Context context){
        JSONArray array = new JSONArray();
        try {
            InputManager manager = (InputManager) context.getSystemService(Context.INPUT_SERVICE);
            int[] deviceIds = manager.getInputDeviceIds();
            for (int deviceId : deviceIds) {
                InputDevice device = manager.getInputDevice(deviceId);
                array.put(device.getName());
            }
        }catch (Exception e){
            //nothing
        }
        return array;
    }
}
