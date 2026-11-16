package com.novacut.videoeditor.graphics

import android.graphics.SurfaceTexture
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.os.SystemClock
import android.view.Surface
import com.novacut.videoeditor.model.ColorAdjustment
import com.novacut.videoeditor.model.FilterPreset
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class VideoGLRenderer(
    private val onSurfaceCreatedListener: ((Surface) -> Unit)? = null
) : GLSurfaceView.Renderer, SurfaceTexture.OnFrameAvailableListener {

    private val triangleVerticesData = floatArrayOf(
        // X, Y, Z, U, V
        -1.0f, -1.0f, 0.0f, 0.0f, 0.0f,
         1.0f, -1.0f, 0.0f, 1.0f, 0.0f,
        -1.0f,  1.0f, 0.0f, 0.0f, 1.0f,
         1.0f,  1.0f, 0.0f, 1.0f, 1.0f
    )

    private val triangleVertices: FloatBuffer = ByteBuffer
        .allocateDirect(triangleVerticesData.size * 4)
        .order(ByteOrder.nativeOrder())
        .asFloatBuffer()
        .apply {
            put(triangleVerticesData)
            position(0)
        }

    private val mvpMatrix = FloatArray(16)
    private val stMatrix = FloatArray(16)

    private var textureId: Int = 0
    private var surfaceTexture: SurfaceTexture? = null
    private var updateSurface = false

    // Compiled GL Programs
    private var programOes = 0
    private var programGrayscale = 0
    private var programColorAdjust = 0
    private var programBlur = 0
    private var programRetroVHS = 0

    // Filter and Color Parameters
    var currentFilter: FilterPreset = FilterPreset.ORIGINAL
    var colorAdjustment = ColorAdjustment()

    private var viewportWidth: Int = 1080
    private var viewportHeight: Int = 1920

    init {
        Matrix.setIdentityM(stMatrix, 0)
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        // Compile Shader Pipeline
        programOes = ShaderPrograms.createProgram(ShaderPrograms.VERTEX_SHADER, ShaderPrograms.FRAGMENT_SHADER_OES)
        programGrayscale = ShaderPrograms.createProgram(ShaderPrograms.VERTEX_SHADER, ShaderPrograms.FRAGMENT_SHADER_GRAYSCALE)
        programColorAdjust = ShaderPrograms.createProgram(ShaderPrograms.VERTEX_SHADER, ShaderPrograms.FRAGMENT_SHADER_COLOR_ADJUST)
        programBlur = ShaderPrograms.createProgram(ShaderPrograms.VERTEX_SHADER, ShaderPrograms.FRAGMENT_SHADER_BLUR)
        programRetroVHS = ShaderPrograms.createProgram(ShaderPrograms.VERTEX_SHADER, ShaderPrograms.FRAGMENT_SHADER_RETRO_VHS)

        // Generate OES External Texture
        val textures = IntArray(1)
        GLES20.glGenTextures(1, textures, 0)
        textureId = textures[0]
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId)
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR.toFloat())
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR.toFloat())
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)

        // Create SurfaceTexture and notify caller
        surfaceTexture = SurfaceTexture(textureId).apply {
            setOnFrameAvailableListener(this@VideoGLRenderer)
        }
        val surface = Surface(surfaceTexture)
        onSurfaceCreatedListener?.invoke(surface)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        viewportWidth = width
        viewportHeight = height
        GLES20.glViewport(0, 0, width, height)
        Matrix.setIdentityM(mvpMatrix, 0)
    }

    override fun onDrawFrame(gl: GL10?) {
        synchronized(this) {
            if (updateSurface) {
                surfaceTexture?.updateTexImage()
                surfaceTexture?.getTransformMatrix(stMatrix)
                updateSurface = false
            }
        }

        GLES20.glClearColor(0.05f, 0.05f, 0.07f, 1.0f)
        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT or GLES20.GL_COLOR_BUFFER_BIT)

        // Select Shader Program based on active filter
        val activeProgram = when (currentFilter) {
            FilterPreset.MONO_NOIR -> programGrayscale
            FilterPreset.CYBERPUNK -> programRetroVHS
            FilterPreset.CINEMATIC, FilterPreset.WARM_GOLD, FilterPreset.COLD_BLUE, FilterPreset.EMERALD, FilterPreset.VINTAGE -> programColorAdjust
            else -> programOes
        }

        GLES20.glUseProgram(activeProgram)

        // Bind OES Texture
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId)
        val uTextureLoc = GLES20.glGetUniformLocation(activeProgram, "sTexture")
        GLES20.glUniform1i(uTextureLoc, 0)

        // Set Vertex and Texture Coordinates
        triangleVertices.position(0)
        val aPositionHandle = GLES20.glGetAttribLocation(activeProgram, "aPosition")
        GLES20.glEnableVertexAttribArray(aPositionHandle)
        GLES20.glVertexAttribPointer(aPositionHandle, 3, GLES20.GL_FLOAT, false, 5 * 4, triangleVertices)

        triangleVertices.position(3)
        val aTextureCoordHandle = GLES20.glGetAttribLocation(activeProgram, "aTextureCoord")
        GLES20.glEnableVertexAttribArray(aTextureCoordHandle)
        GLES20.glVertexAttribPointer(aTextureCoordHandle, 2, GLES20.GL_FLOAT, false, 5 * 4, triangleVertices)

        // Uniforms for Matrix
        val uMVPMatrixHandle = GLES20.glGetUniformLocation(activeProgram, "uMVPMatrix")
        GLES20.glUniformMatrix4fv(uMVPMatrixHandle, 1, false, mvpMatrix, 0)
        val uSTMatrixHandle = GLES20.glGetUniformLocation(activeProgram, "uSTMatrix")
        GLES20.glUniformMatrix4fv(uSTMatrixHandle, 1, false, stMatrix, 0)

        // Bind Specific Filter Uniforms
        if (activeProgram == programColorAdjust) {
            val uBrightness = GLES20.glGetUniformLocation(activeProgram, "uBrightness")
            val uContrast = GLES20.glGetUniformLocation(activeProgram, "uContrast")
            val uSaturation = GLES20.glGetUniformLocation(activeProgram, "uSaturation")
            val uTemperature = GLES20.glGetUniformLocation(activeProgram, "uTemperature")

            val brightness = colorAdjustment.brightness + when (currentFilter) {
                FilterPreset.CINEMATIC -> 0.05f
                FilterPreset.WARM_GOLD -> 0.08f
                else -> 0.0f
            }
            val contrast = colorAdjustment.contrast * when (currentFilter) {
                FilterPreset.CINEMATIC -> 1.25f
                FilterPreset.VINTAGE -> 0.85f
                else -> 1.0f
            }
            val saturation = colorAdjustment.saturation * when (currentFilter) {
                FilterPreset.CINEMATIC -> 1.15f
                FilterPreset.VINTAGE -> 0.7f
                FilterPreset.EMERALD -> 1.3f
                else -> 1.0f
            }
            val temperature = colorAdjustment.temperature + when (currentFilter) {
                FilterPreset.WARM_GOLD -> 0.4f
                FilterPreset.COLD_BLUE -> -0.4f
                else -> 0.0f
            }

            GLES20.glUniform1f(uBrightness, brightness)
            GLES20.glUniform1f(uContrast, contrast)
            GLES20.glUniform1f(uSaturation, saturation)
            GLES20.glUniform1f(uTemperature, temperature)
        } else if (activeProgram == programRetroVHS) {
            val uTimeLoc = GLES20.glGetUniformLocation(activeProgram, "uTime")
            val time = (SystemClock.uptimeMillis() % 10000) / 1000.0f
            GLES20.glUniform1f(uTimeLoc, time)
        } else if (activeProgram == programBlur) {
            val uTexelOffset = GLES20.glGetUniformLocation(activeProgram, "uTexelOffset")
            GLES20.glUniform2f(uTexelOffset, 1.0f / viewportWidth, 1.0f / viewportHeight)
        }

        // Draw quad
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)

        GLES20.glDisableVertexAttribArray(aPositionHandle)
        GLES20.glDisableVertexAttribArray(aTextureCoordHandle)
    }

    override fun onFrameAvailable(surfaceTexture: SurfaceTexture?) {
        synchronized(this) {
            updateSurface = true
        }
    }
}
