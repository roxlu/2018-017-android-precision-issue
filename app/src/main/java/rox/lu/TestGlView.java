package rox.lu;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.util.Log;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class TestGlView extends GLSurfaceView {
  
  /* ---------------------------------------------- */
  
  private TestGlRenderer renderer;

  /* ---------------------------------------------- */
  
  public TestGlView(Context context) {
    this(context, null);
  }

  public TestGlView(Context context,  AttributeSet attrs) {
    super(context, attrs);
    setEGLContextClientVersion(2);
    renderer = new TestGlRenderer();
    setRenderer(renderer);

    Log.v("msg", "Create TestGlView");
  }


  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec);

    int measuredWidth = getMeasuredWidth();
    int measuredHeight = getMeasuredHeight();

    setMeasuredDimension(measuredWidth, 100);

    Log.v("msg", "onMeasure: " +measuredWidth +", " +measuredHeight);
  }
};
