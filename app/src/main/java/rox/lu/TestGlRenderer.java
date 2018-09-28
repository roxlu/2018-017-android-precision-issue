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

  private static final String FILTER_VS = ""
    + "attribute vec2 a_pos;\n"
    + "attribute vec2 a_tex;\n"
    + "varying vec2 v_tex;\n"
    + "void main() {\n"
    + "  gl_Position = vec4(a_pos.x, a_pos.y, 0.0, 1.0);\n"
    + "  v_tex = a_tex;\n"
    + "}\n";

  private static final String FILTER_FS = ""
    + "precision mediump float;\n"
    + "varying vec2 v_tex;\n"
    + "void main() {\n"
    + "  gl_FragColor = vec4(v_tex.x, v_tex.y, 0.0, 1.0);\n"
    + "  gl_FragColor = vec4(0.0, 0.0, 0.0, 1.0);\n"
    + "  if(mod(gl_FragCoord.x, 2.0) < 1.0) {\n"
    + "    gl_FragColor.rgb = vec3(1.0, 0.0, 0.0);\n"
    + "  }\n"
    + "}"
    + "";

  private GlShader filter_vs;
  private GlShader filter_fs;
  private GlProgram filter_prog;
  private GlVbo filter_vbo;
  private GlRenderToTexture rtt;
  private GlTextureRenderer texture_renderer;

  /* -------------------------------------------------------------- */
  
  public void onSurfaceCreated(GL10 unused, EGLConfig config) {

    GLES20.glClearColor(0.0f, 1.0f, 0.0f, 1.0f);

    if (null == filter_vs) {
      filter_vs = new GlShader();
      filter_vs.createVertexShader(FILTER_VS);
    }
    
    if (null == filter_fs) {
      filter_fs = new GlShader();
      filter_fs.createFragmentShader(FILTER_FS);
    }

    if (null == filter_prog) {
      filter_prog = new GlProgram();
      filter_prog.create();
      filter_prog.attachShader(filter_vs.getId());
      filter_prog.attachShader(filter_fs.getId());
      filter_prog.bindAttribLocation("a_pos", 0);
      filter_prog.bindAttribLocation("a_tex", 1);
      filter_prog.link();
      filter_prog.use();
      filter_prog.uniform1i("u_tex", 0);
    }

    if (null == filter_vbo) {
      
      float[] verts = {
       -1.0f,  1.0f, 0.0f, 1.0f,   /* top left */
       -1.0f, -1.0f, 0.0f, 0.0f,   /* bottom left */
        1.0f,  1.0f, 1.0f, 1.0f,   /* top right */
        1.0f, -1.0f, 1.0f, 0.0f    /* bottom right */
      };

      filter_vbo = new GlVbo();
      filter_vbo.create();
      filter_vbo.uploadStaticData(verts);
    }

    if (null == rtt) {
      rtt = new GlRenderToTexture();

      /* 
         When applying the shader/filter (see above, FILTER_FS), on a
         MiBox MDZ-16-AB we run into what I suspect to be a floating point
         precision issue. See this image for the result that we get:
         https://imgur.com/a/JeAZq6t
       */
      rtt.create(3840, 2160);

      /* 
         Using a resolution of 1920 x 1080, gives "reasonable"
         results on MiBox MDZ-16-AB. See https://imgur.com/0LfUBP5
      */
      //rtt.create(1920, 1080);
    }

    if (null == texture_renderer) {
      texture_renderer = new GlTextureRenderer();
      texture_renderer.create();
    }

    Log.v("msg", "Vertex shader: " +filter_vs.getId());
    Log.v("msg", "Fragment shader: " +filter_fs.getId());
    Log.v("msg", "Program: " +filter_prog.getId());
    Log.v("msg", "VBO: " +filter_vbo.getId());
  }

  public void onDrawFrame(GL10 unused) {

    rtt.beginCapture();
    {
      filter_prog.use();
      filter_vbo.bind();
      filter_prog.enableAttrib(0);
      filter_prog.enableAttrib(1);
      filter_vbo.vertexAttribPointer(0, 2, GLES20.GL_FLOAT, true, 16, 0); /* pos */
      filter_vbo.vertexAttribPointer(1, 2, GLES20.GL_FLOAT, true, 16, 8); /* tex */
      filter_vbo.drawTriangleStrip(0, 4);
    }
    rtt.endCapture();

    texture_renderer.draw(rtt.getTextureId(), 0, 0, 1920, 1080);
  }

  public void onSurfaceChanged(GL10 unused, int width, int height) {
    GLES20.glViewport(0, 0, width, height);

    Log.v("msg", String.format("> surface changed %d x %d", width, height));
  }
};
