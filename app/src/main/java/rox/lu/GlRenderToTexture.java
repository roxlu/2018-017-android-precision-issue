/*
  Render To Texture                             
  =================

  Very basic OpenGL ES 2.0 render to texture helper. 
  We create an FBP with one color attachment; that's it.

 */
package rox.lu;

import android.opengl.GLES20;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

public class GlRenderToTexture {

  /* -------------------------------------------------------------- */

  private IntBuffer fbo_id = IntBuffer.allocate(1);
  private IntBuffer tex_id = IntBuffer.allocate(1);
  private int width = 0;
  private int height = 0;
  
  /* -------------------------------------------------------------- */

  /* 
     Creates the FBO and texture on color attachment 0 using our defaults
     for sampling and wrapping.
  */
  public void create(int texWidth, int texHeight) {

    if (0 != fbo_id.get(0)) {
      throw new RuntimeException("Already created the RenderToTexture fbo.");
    }

    if (0 != tex_id.get(0)) {
      throw new RuntimeException("Already created the RenderToTexture texture.");
    }

    if (texWidth <= 0) {
      throw new RuntimeException("Cannot create RenderToTexture, given width <= 0.");
    }

    if (texHeight <= 0) {
      throw new RuntimeException("Cannot create RenderToTexture, given height <= 0.");
    }

    width = texWidth;
    height = texHeight;

    /* create our storage buffer. */
    GLES20.glGenTextures(1, tex_id);
    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, tex_id.get(0));
    GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, width, height, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
    GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
    GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
    GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
    GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);

    /* create the framebuffer. */
    GLES20.glGenFramebuffers(1, fbo_id);
    GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fbo_id.get(0)); 
    GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, tex_id.get(0), 0);

    if (GLES20.GL_FRAMEBUFFER_COMPLETE != GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER)) {
      throw new RuntimeException("Failed to create the framebuffer for our decoded video frame.");
    }

    GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
  }

  public void beginCapture() {
    GLES20.glViewport(0, 0, width, height);
    GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fbo_id.get(0));
    GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
  }

  public void endCapture() {
    GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
  }

  public int getTextureId() {
    return tex_id.get(0);
  }

  public void bind() {
    GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fbo_id.get(0));
  }

};
