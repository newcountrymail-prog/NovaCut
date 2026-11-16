package com.novacut.videoeditor.graphics

import android.opengl.GLES20
import android.util.Log

object ShaderPrograms {
    private const val TAG = "ShaderPrograms"

    // Standard Vertex Shader with Matrix Transformation
    const val VERTEX_SHADER = """
        uniform mat4 uMVPMatrix;
        uniform mat4 uSTMatrix;
        attribute vec4 aPosition;
        attribute vec4 aTextureCoord;
        varying vec2 vTextureCoord;
        void main() {
            gl_Position = uMVPMatrix * aPosition;
            vTextureCoord = (uSTMatrix * aTextureCoord).xy;
        }
    """

    // 1. Base OES Texture Fragment Shader
    const val FRAGMENT_SHADER_OES = """
        #extension GL_OES_EGL_image_external : require
        precision mediump float;
        varying vec2 vTextureCoord;
        uniform samplerExternalOES sTexture;
        void main() {
            gl_FragColor = texture2D(sTexture, vTextureCoord);
        }
    """

    // 2. Grayscale Fragment Shader
    const val FRAGMENT_SHADER_GRAYSCALE = """
        #extension GL_OES_EGL_image_external : require
        precision mediump float;
        varying vec2 vTextureCoord;
        uniform samplerExternalOES sTexture;
        void main() {
            vec4 color = texture2D(sTexture, vTextureCoord);
            float gray = dot(color.rgb, vec3(0.299, 0.587, 0.114));
            gl_FragColor = vec4(vec3(gray), color.a);
        }
    """

    // 3. Brightness, Contrast, Saturation, Temperature Shader
    const val FRAGMENT_SHADER_COLOR_ADJUST = """
        #extension GL_OES_EGL_image_external : require
        precision mediump float;
        varying vec2 vTextureCoord;
        uniform samplerExternalOES sTexture;
        uniform float uBrightness;  // -1.0 to 1.0 (default 0.0)
        uniform float uContrast;    // 0.0 to 2.0 (default 1.0)
        uniform float uSaturation;  // 0.0 to 2.0 (default 1.0)
        uniform float uTemperature; // -1.0 to 1.0 (default 0.0)

        void main() {
            vec4 textureColor = texture2D(sTexture, vTextureCoord);
            vec3 rgb = textureColor.rgb;

            // 1. Brightness
            rgb += uBrightness;

            // 2. Contrast
            rgb = (rgb - 0.5) * uContrast + 0.5;

            // 3. Saturation
            float luminance = dot(rgb, vec3(0.2126, 0.7152, 0.0722));
            rgb = mix(vec3(luminance), rgb, uSaturation);

            // 4. Color Temperature (Warm/Cool)
            rgb.r += uTemperature * 0.1;
            rgb.b -= uTemperature * 0.1;

            gl_FragColor = vec4(clamp(rgb, 0.0, 1.0), textureColor.a);
        }
    """

    // 4. Fast Gaussian 9-Tap Blur Shader
    const val FRAGMENT_SHADER_BLUR = """
        #extension GL_OES_EGL_image_external : require
        precision mediump float;
        varying vec2 vTextureCoord;
        uniform samplerExternalOES sTexture;
        uniform vec2 uTexelOffset; // (1.0/width, 1.0/height)

        void main() {
            vec4 sum = vec4(0.0);
            sum += texture2D(sTexture, vTextureCoord + uTexelOffset * vec2(-1.0, -1.0)) * 0.0625;
            sum += texture2D(sTexture, vTextureCoord + uTexelOffset * vec2( 0.0, -1.0)) * 0.1250;
            sum += texture2D(sTexture, vTextureCoord + uTexelOffset * vec2( 1.0, -1.0)) * 0.0625;
            sum += texture2D(sTexture, vTextureCoord + uTexelOffset * vec2(-1.0,  0.0)) * 0.1250;
            sum += texture2D(sTexture, vTextureCoord) * 0.2500;
            sum += texture2D(sTexture, vTextureCoord + uTexelOffset * vec2( 1.0,  0.0)) * 0.1250;
            sum += texture2D(sTexture, vTextureCoord + uTexelOffset * vec2(-1.0,  1.0)) * 0.0625;
            sum += texture2D(sTexture, vTextureCoord + uTexelOffset * vec2( 0.0,  1.0)) * 0.1250;
            sum += texture2D(sTexture, vTextureCoord + uTexelOffset * vec2( 1.0,  1.0)) * 0.0625;
            gl_FragColor = sum;
        }
    """

    // 5. Retro VHS / CRT Artifacts Shader (Scanlines, Chromatic Aberration, Noise)
    const val FRAGMENT_SHADER_RETRO_VHS = """
        #extension GL_OES_EGL_image_external : require
        precision mediump float;
        varying vec2 vTextureCoord;
        uniform samplerExternalOES sTexture;
        uniform float uTime; // Dynamic noise animation tick

        void main() {
            vec2 uv = vTextureCoord;
            
            // Chromatic aberration (RGB channel shift)
            float shift = 0.004;
            float r = texture2D(sTexture, uv + vec2(shift, 0.0)).r;
            float g = texture2D(sTexture, uv).g;
            float b = texture2D(sTexture, uv - vec2(shift, 0.0)).b;
            vec3 color = vec3(r, g, b);

            // Horizontal VHS Scanlines
            float scanline = sin(uv.y * 600.0) * 0.04;
            color -= scanline;

            // Analog Noise emulation
            float noise = fract(sin(dot(uv + uTime, vec2(12.9898, 78.233))) * 43758.5453) * 0.06;
            color += noise;

            gl_FragColor = vec4(clamp(color, 0.0, 1.0), 1.0);
        }
    """

    /**
     * Compiles GLSL shader code and verifies compile status.
     */
    fun loadShader(shaderType: Int, source: String): Int {
        var shader = GLES20.glCreateShader(shaderType)
        if (shader != 0) {
            GLES20.glShaderSource(shader, source)
            GLES20.glCompileShader(shader)
            val compiled = IntArray(1)
            GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0)
            if (compiled[0] == 0) {
                Log.e(TAG, "Could not compile shader $shaderType: ${GLES20.glGetShaderInfoLog(shader)}")
                GLES20.glDeleteShader(shader)
                shader = 0
            }
        }
        return shader
    }

    /**
     * Creates program and links vertex & fragment shaders.
     */
    fun createProgram(vertexSource: String, fragmentSource: String): Int {
        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexSource)
        if (vertexShader == 0) return 0
        val pixelShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource)
        if (pixelShader == 0) return 0

        var program = GLES20.glCreateProgram()
        if (program != 0) {
            GLES20.glAttachShader(program, vertexShader)
            GLES20.glAttachShader(program, pixelShader)
            GLES20.glLinkProgram(program)
            val linkStatus = IntArray(1)
            GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0)
            if (linkStatus[0] != GLES20.GL_TRUE) {
                Log.e(TAG, "Could not link program: ${GLES20.glGetProgramInfoLog(program)}")
                GLES20.glDeleteProgram(program)
                program = 0
            }
        }
        return program
    }
}
