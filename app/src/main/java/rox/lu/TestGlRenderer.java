package rox.lu;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.Environment;
import android.util.Log;
import android.view.Surface;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.*;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class TestGlRenderer implements GLSurfaceView.Renderer {

  /* -------------------------------------------------------------- */

  private static final String FILTER_VS = ""
    + "precision highp float;\n"
    + "attribute vec2 a_pos;\n"
    + "attribute highp vec2 a_tex;\n"
    + "varying highp vec2 v_tex;\n"
    + "void main() {\n"
    + "  gl_Position = vec4(a_pos.x, a_pos.y, 0.0, 1.0);\n"
    + "  v_tex = a_tex;\n"
    + "}\n";

  private static final String FILTER_FS = ""
    + "precision mediump float;\n"
    + "uniform sampler2D u_lookup_tex;\n"
    + "varying highp vec2 v_tex;\n"
    + "void main() {\n"
    + "  gl_FragColor = vec4(0.0, 0.0, 0.0, 1.0);\n"
    + "  vec3 lookup_col = texture2D(u_lookup_tex, v_tex).rgb;\n"
    + "  if (lookup_col.r == 1.0) {\n"
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
  private IntBuffer lookup_tex = IntBuffer.allocate(1);
  private int buffer_width = 3840;
  private int buffer_height = 2160;
  private boolean did_create_output_file = false;
  private Context context = null;
  
  /* -------------------------------------------------------------- */

  public void setContext(Context ctx) {
    context = ctx;
  }

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
      filter_prog.uniform1i("u_lookup_tex", 0);
    }

    if (null == filter_vbo) {
      
      float[] verts = {
        -1.0f,  1.0f, 0.0f, 1.0f,  /* top left */
        -1.0f, -1.0f, 0.0f, 0.0f,  /* bottom left */
        1.0f,  1.0f, 1.0f, 1.0f,   /* top right */
        1.0f, -1.0f, 1.0f, 0.0f    /* bottom right */
      };

      filter_vbo = new GlVbo();
      filter_vbo.create();
      filter_vbo.uploadStaticData(verts);
    }

    /* Create the lookup table that we use to try and solve the precision issue. */
    createLookupTexture(buffer_width);

    if (null == rtt) {
      rtt = new GlRenderToTexture();

      /* 
         When applying the shader/filter (see above, FILTER_FS), on a
         MiBox MDZ-16-AB we run into what I suspect to be a floating point
         precision issue. 
      */
      rtt.create(buffer_width, buffer_height);
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
  
  /* -------------------------------------------------------------- */
  
  public void onDrawFrame(GL10 unused) {

    rtt.beginCapture();
    {
      /* Bind our lookup texture with the black/white changing pixels. */
      GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
      GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, lookup_tex.get(0));

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

    /* 
       After we've rendered into our texture we download the
       result and save it into a uncompressed PNG file. We cannot
       simply render the result to a destination framebuffer with
       a resolution of 1920 x 1080 because the min/mag filters
       that are applied then, will distort the result.
    */
    if (false == did_create_output_file) {
      try {
        File sdcard = Environment.getExternalStorageDirectory();
        File file = new File(sdcard, "output.png");
        saveOutputTextureAsPng(buffer_width, buffer_height, file.toString());
      }
      catch (IOException ex) {
        ex.printStackTrace();
        Log.v("msg", "Failed to save the generated texture into output.png." + ex.getMessage());

      }
      did_create_output_file = true;      
    }
  }

  public void onSurfaceChanged(GL10 unused, int width, int height) {
    GLES20.glViewport(0, 0, width, height);

    Log.v("msg", String.format("> surface changed %d x %d", width, height));
  }

  /* -------------------------------------------------------------- */
  
  private void createLookupTexture(int width) {
        
    /* Create the buffer where each odd column is set to 0x00 and each even column is set to 0xFF. */
    byte[] pixels = new byte[width];
    for (int i = 0; i < width; ++i) {
      pixels[i] = (byte)(i % 2 == 0 ? 0x00 : 0xFF) ;
    }

    GLES20.glGenTextures(1, lookup_tex);
    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, lookup_tex.get(0));
    GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_LUMINANCE, width, 1, 0, GLES20.GL_LUMINANCE, GLES20.GL_UNSIGNED_BYTE, ByteBuffer.wrap(pixels));
    GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
    GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
    GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
    GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);

    Log.v("msg", "Update the lookup textures.");
  }

  /*
    See https://stackoverflow.com/questions/25348674/convert-opengl-es-2-0-rendered-texture-to-bitmap-and-back
  */
  public void saveOutputTextureAsPng(int sourceWidth, int sourceHeight, String filename) throws IOException {
    
    ByteBuffer pixel_buffer;
    pixel_buffer = ByteBuffer.allocateDirect(sourceWidth * sourceHeight * 4);
    pixel_buffer.order(ByteOrder.LITTLE_ENDIAN);

    /* 
       We bind the framebuffer into which we render our shader
       result. This allows us to download the generated texture
       data. The downloaded texture data holds the result of the
       shader that we defined above `FILTER_FS`.
    */
    rtt.bind();
    GLES20.glReadPixels(0, 0, sourceWidth, sourceHeight, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, pixel_buffer);

    BufferedOutputStream bos = null;
    try {
      bos = new BufferedOutputStream(new FileOutputStream(filename));
      Bitmap bmp = Bitmap.createBitmap(sourceWidth, sourceHeight, Bitmap.Config.ARGB_8888);
      pixel_buffer.rewind();
      bmp.copyPixelsFromBuffer(pixel_buffer);
      bmp.compress(Bitmap.CompressFormat.PNG, 100, bos);
      bmp.recycle();
    }
    finally {
      if (bos != null) {
        bos.close();
      }
    }
    
    Log.d("msg", "Saved " + sourceWidth + "x" + sourceHeight + " frame as '" + filename + "'");
  }
  
  /* -------------------------------------------------------------- */

};
