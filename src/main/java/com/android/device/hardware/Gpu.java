package com.android.device.hardware;

import android.content.Context;
import android.opengl.EGL14;
import android.opengl.GLSurfaceView;

import org.json.JSONObject;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

class DemoGLSurfaceView extends GLSurfaceView {

    DemoRenderer mRenderer;

    public DemoGLSurfaceView(Context context) {
        super(context);
        setEGLConfigChooser(8, 8, 8, 8, 0, 0);
        mRenderer = new DemoRenderer();
        setRenderer(mRenderer);

        //setVisibility(GLSurfaceView.GONE);
    }
}

class DemoRenderer implements GLSurfaceView.Renderer {

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
//        Log.d("SystemInfo", "GL_RENDERER = " +gl.glGetString(GL10.GL_RENDERER));
//        Log.d("SystemInfo", "GL_VENDOR = " + gl.glGetString(GL10.GL_VENDOR));
//        Log.d("SystemInfo", "GL_VERSION = " + gl.glGetString(GL10.GL_VERSION));
//        Log.i("SystemInfo", "GL_EXTENSIONS = " + gl.glGetString(GL10.GL_EXTENSIONS));
    }

    @Override
    public void onDrawFrame(GL10 arg0) {
        // TODO Auto-generated method stub

    }


    @Override
    public void onSurfaceChanged(GL10 arg0, int arg1, int arg2) {
        // TODO Auto-generated method stub

    }

}

public class Gpu {
    public static JSONObject getGpuInfo(Context context) {
//        GLSurfaceView glView = new DemoGLSurfaceView(context);
        try {
//            SurfaceView surfaceView = new SurfaceView(context);
//            Surface virtualSurface = surfaceView.getHolder().getSurface();
            JSONObject object = new JSONObject();
            // 获取EGL Display
            android.opengl.EGLDisplay eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);

            // 获取GPU厂商信息
            String vendor = EGL14.eglQueryString(eglDisplay, EGL14.EGL_VENDOR);

            // 获取GPU版本信息
            String version = EGL14.eglQueryString(eglDisplay, EGL14.EGL_VERSION);

            // 获取GPU扩展信息
            String extensions = EGL14.eglQueryString(eglDisplay, EGL14.EGL_EXTENSIONS);

            //
//            String renderer = EGL14.eglQueryString(eglDisplay, EGL14.EGL_RENDER_BUFFER);
                    //+ " " + EGL14.eglQueryString(eglDisplay, EGL14.EGL_RENDERABLE_TYPE);

//            String vendor = GLES20.glGetString(GLES20.GL_VENDOR);
//            String renderer = GLES20.glGetString(GLES20.GL_RENDERER);
//            String version = GLES20.glGetString(GLES20.GL_VERSION);
//            String extensions = GLES20.glGetString(GLES20.GL_EXTENSIONS);

            object.put("GL_VENDOR", vendor);
            object.put("GL_VERSION", version);
            object.put("GL_EXTENSIONS", extensions);
//            object.put("GL_RENDERER", renderer);
            //Log.d("getGpuInfo", "GL_RENDERER = " + EGL14.EGL_RENDER_BUFFER + " " + EGL14.EGL_RENDERABLE_TYPE);
//            Log.d("getGpuInfo", "GL_VENDOR = " + vendor);
//            Log.d("getGpuInfo", "GL_VERSION = " + version);
//            Log.i("getGpuInfo", "GL_EXTENSIONS = " + extensions);

            return object;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
        //(Activity)context.setContentView(glView);
    }

    public static String gpu(Context context) {
        return "";
    }
}
