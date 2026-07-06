package com.android.device.hardware;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.util.Size;
import android.util.SizeF;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class Camera {
    /**
     * 获取设备上的相机信息，并以JSON数组的形式返回。
     *
     * @param context 上下文对象，用于获取系统服务。
     * @return 包含相机信息的JSON数组。
     * @throws CameraAccessException 如果无法访问相机服务，则抛出此异常。
     * @throws JSONException         如果JSON操作失败，则抛出此异常。
     */
    public static JSONArray getCameraInfo(Context context) {
        JSONArray jsonArray = new JSONArray();
        try {
            CameraManager manager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
            String[] cameraIds = manager.getCameraIdList();
            if (cameraIds != null && cameraIds.length > 0) {
                for (String cameraId : cameraIds) {
                    if (cameraId.isEmpty()) {
                        continue;
                    }
                    JSONObject jsonObject = new JSONObject();
                    JSONArray itemList = new JSONArray();
                    try {
                        CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
                        StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                        JSONArray outpusize = getSizeInfo(map);

                        SizeF sensorSize = characteristics.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE);
                        float w = 0.5F * sensorSize.getWidth();
                        float h = 0.5F * sensorSize.getHeight();

                        float[] focalLengths = characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS);
                        for (int focusId = 0; focusId < focalLengths.length; ++focusId) {
                            float focalLength = focalLengths[focusId];
                            float horizonalAngle = (float) Math.toDegrees(2.0 * Math.atan(w / focalLength));
                            float verticalAngle = (float) Math.toDegrees(2.0 * Math.atan(h / focalLength));
                            JSONObject it = new JSONObject();
                            it.put("focusId", focusId);
                            it.put("focalLength", Float.toString(focalLength));
                            it.put("horizonalAngle", Float.toString(horizonalAngle));
                            it.put("verticalAngle", Float.toString(verticalAngle));
                            itemList.put(it);
                        }

                        jsonObject.put("info", itemList);
                        jsonObject.put("cameraId", cameraId);
                        jsonObject.put("sizeInfo", outpusize);
                        jsonObject.put("width", (double) (2.0F * w));
                        jsonObject.put("height", (double) (2.0F * h));
                        jsonObject.put("facing", characteristics.get(CameraCharacteristics.LENS_FACING));

                        jsonArray.put(jsonObject);
                    } catch (NullPointerException e) {
                        e.printStackTrace();
                    }
                }
            }

        } catch (CameraAccessException | JSONException e) {
            e.printStackTrace();
        }
        return jsonArray;
    }

    /**
     * 根据给定的StreamConfigurationMap获取支持的大小信息，并返回为JSONArray格式。
     *
     * @param map StreamConfigurationMap对象，包含输出流配置信息。
     * @return JSONArray格式的支持大小信息列表，每个元素为一个JSONObject，包含"width"和"height"两个字段。
     * @throws JSONException 如果在构建JSON对象时发生错误。
     */
    private static JSONArray getSizeInfo(StreamConfigurationMap map) throws JSONException {
        JSONArray outpusize = new JSONArray();
        if (map != null) {
            Size[] outputSizes = map.getOutputSizes(SurfaceTexture.class); // 可以改为ImageFormat等其他类型获取不同格式的支持大小
            for (Size size : outputSizes) {
                JSONObject sizejson = new JSONObject();
                sizejson.put("width", size.getWidth());
                sizejson.put("height", size.getHeight());
                outpusize.put(sizejson);
            }
        }
        return outpusize;
    }
}
