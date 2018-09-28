/*

  Debug drawer for textures
  ==========================

  Very basic helper that allows you to render the contents of a 
  texture into the current viewport area. We create a VBO and the 
  necessary shader. 

 */
package rox.lu;

import android.opengl.GLES20;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

public class GlTextureRenderer {

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
    + "uniform sampler2D u_tex;\n"
    + "void main() {\n"
    + "  vec4 tc = texture2D(u_tex, v_tex);\n"
    + "  gl_FragColor = tc;\n"
    + "}"
    + "";

  /* -------------------------------------------------------------- */
  
  private GlShader fullscreen_vs;
  private GlShader fullscreen_fs;
  private GlProgram fullscreen_prog;
  private GlVbo fullscreen_vbo;

  /* -------------------------------------------------------------- */
  
  public void create() {

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
       -1.0f,  1.0f, 0.0f, 1.0f,   /* top left */
       -1.0f, -1.0f, 0.0f, 0.0f,   /* bottom left */
        1.0f,  1.0f, 1.0f, 1.0f,   /* top right */
        1.0f, -1.0f, 1.0f, 0.0f    /* bottom right */
      };

      fullscreen_vbo = new GlVbo();
      fullscreen_vbo.create();
      fullscreen_vbo.uploadStaticData(verts);
    }
  }

  public void draw(int texId) {
    
    GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texId);
    
    fullscreen_prog.use();
    fullscreen_vbo.bind();
    fullscreen_prog.enableAttrib(0);
    fullscreen_prog.enableAttrib(1);
    fullscreen_vbo.vertexAttribPointer(0, 2, GLES20.GL_FLOAT, true, 16, 0); /* pos */
    fullscreen_vbo.vertexAttribPointer(1, 2, GLES20.GL_FLOAT, true, 16, 8); /* tex */
    fullscreen_vbo.drawTriangleStrip(0, 4);
  }
  
  public void draw(int texId, int x, int y, int w, int h) {
    GLES20.glViewport(x, y, w, h);
    draw(texId);
  }
};

