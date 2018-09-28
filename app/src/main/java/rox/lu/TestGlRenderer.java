package rox.lu;

import android.content.Context;
import android.opengl.GLES20;
import android.util.Log;
import android.opengl.GLSurfaceView;
import android.opengl.GLES11Ext;
import android.graphics.SurfaceTexture;
import android.view.Surface;
import java.nio.IntBuffer;
import java.util.*;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class TestGlRenderer implements GLSurfaceView.Renderer {

  /* -------------------------------------------------------------- */

  private static final String FULLSCREEN_VS = ""
    + "attribute vec2 a_pos;\n"
    + "attribute vec2 a_tex;\n"
    + "varying vec2 v_tex;\n"
    + "void main() {\n"
    + "  gl_Position = vec4(a_pos.x, a_pos.y, 0.0, 1.0);\n"
    + "  v_tex = a_tex;\n"
    + "}\n";

  private static final String FULLSCREEN_FS = ""
    + "precision mediump float;\n"
    + "varying vec2 v_tex;\n"
    + "void main() {\n"
    + "  gl_FragColor = vec4(v_tex.x, v_tex.y, 0.0, 1.0);\n"
    + "  if(mod(gl_FragCoord.x, 2.0) == 0.0) {\n"
    + "    gl_FragColor.rgb = vec3(1.0, 1.0, 1.0);\n"
    + "  }\n"
    + "}"
    + "";

  private GlShader fullscreen_vs;
  private GlShader fullscreen_fs;
  private GlProgram fullscreen_prog;
  private GlVbo fullscreen_vbo;

  /* -------------------------------------------------------------- */
  
  public void onSurfaceCreated(GL10 unused, EGLConfig config) {

    GLES20.glClearColor(0.0f, 1.0f, 0.0f, 1.0f);

    if (null == fullscreen_vs) {
      fullscreen_vs = new GlShader();
      fullscreen_vs.createVertexShader(FULLSCREEN_VS);
    }
    
    if (null == fullscreen_fs) {
      fullscreen_fs = new GlShader();
      fullscreen_fs.createFragmentShader(FULLSCREEN_FS);
    }

    if (null == fullscreen_prog) {
      fullscreen_prog = new GlProgram();
      fullscreen_prog.create();
      fullscreen_prog.attachShader(fullscreen_vs.getId());
      fullscreen_prog.attachShader(fullscreen_fs.getId());
      fullscreen_prog.bindAttribLocation("a_pos", 0);
      fullscreen_prog.bindAttribLocation("a_tex", 1);
      fullscreen_prog.link();
      fullscreen_prog.use();
      fullscreen_prog.uniform1i("u_tex", 0);
    }

    if (null == fullscreen_vbo) {
      
      float[] verts = {
       -1.0f,  1.0f, 0.0f, 1.0f,  /* top left */
       -1.0f, -1.0f, 0.0f, 0.0f,  /* bottom left */
        1.0f,  1.0f, 1.0f, 1.0f,   /* top right */
        1.0f, -1.0f, 1.0f, 0.0f    /* bottom right */
      };

      fullscreen_vbo = new GlVbo();
      fullscreen_vbo.create();
      fullscreen_vbo.uploadStaticData(verts);
    }

    Log.v("msg", "Vertex shader: " +fullscreen_vs.getId());
    Log.v("msg", "Fragment shader: " +fullscreen_fs.getId());
    Log.v("msg", "Program: " +fullscreen_prog.getId());
    Log.v("msg", "VBO: " +fullscreen_vbo.getId());
  }

  public void onDrawFrame(GL10 unused) {

    GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
    fullscreen_prog.use();
    fullscreen_vbo.bind();
    fullscreen_prog.enableAttrib(0);
    fullscreen_prog.enableAttrib(1);
    fullscreen_vbo.vertexAttribPointer(0, 2, GLES20.GL_FLOAT, true, 16, 0); /* pos */
    fullscreen_vbo.vertexAttribPointer(1, 2, GLES20.GL_FLOAT, true, 16, 8); /* tex */
    fullscreen_vbo.drawTriangleStrip(0, 4);
  }

  public void onSurfaceChanged(GL10 unused, int width, int height) {
    GLES20.glViewport(0, 0, width, height);

    Log.v("msg", String.format("> surface changed %d x %d", width, height));
  }
};
