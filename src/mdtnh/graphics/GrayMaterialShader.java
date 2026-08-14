package mdtnh.graphics;

import arc.graphics.Color;
import arc.graphics.Gl;
import arc.graphics.g2d.TextureRegion;
import arc.graphics.gl.Shader;

/**
 * 灰度材质混合 Shader。
 *
 * 支持：
 *
 * 1. 灰度图 + 纯颜色
 * 2. 灰度图 + 材质贴图
 * 3. 材质贴图额外乘颜色
 *
 * 灰度图负责保留明暗细节，
 * 材质颜色或材质贴图负责最终颜色。
 */
public class GrayMaterialShader extends Shader {

    /**
     * 当前作为灰度模板使用的区域。
     */
    private TextureRegion grayRegion;

    /**
     * 当前使用的材质贴图。
     *
     * useMaterialTexture == false 时可为空。
     */
    private TextureRegion materialRegion;

    /**
     * 额外颜色。
     *
     * 纯颜色模式：
     * 直接作为材质颜色。
     *
     * 贴图模式：
     * 与材质贴图相乘，可用于整体染色。
     */
    private final Color tint = new Color(Color.white);

    /**
     * 是否使用材质贴图。
     */
    private boolean useMaterialTexture;


    /**
     * SpriteBatch 使用的顶点 Shader。
     *
     * 属性名称必须与 Arc SpriteBatch 保持一致。
     */
    private static final String vertexShader =
            """
            attribute vec4 a_position;
            attribute vec4 a_color;
            attribute vec2 a_texCoord0;
            attribute vec4 a_mix_color;

            uniform mat4 u_projTrans;

            varying vec4 v_color;
            varying vec4 v_mix_color;
            varying vec2 v_texCoords;

            void main(){
                v_color = a_color;
                v_color.a *= (255.0 / 254.0);

                v_mix_color = a_mix_color;
                v_mix_color.a *= (255.0 / 254.0);

                v_texCoords = a_texCoord0;

                gl_Position = u_projTrans * a_position;
            }
            """;


    /**
     * 实际执行灰度图与材质混合。
     */
    private static final String fragmentShader =
            """
            varying lowp vec4 v_color;
            varying lowp vec4 v_mix_color;
            varying highp vec2 v_texCoords;

            // 当前 Draw.rect() 绘制的灰度图。
            uniform sampler2D u_texture;

            // 第二张材质贴图。
            uniform sampler2D u_materialTexture;

            // 材质颜色。
            uniform vec4 u_tint;

            // 0 = 使用纯颜色
            // 1 = 使用材质贴图
            uniform float u_useMaterialTexture;

            // 灰度图在其 Texture 中的 UV 范围。
            //
            // xy = 左下
            // zw = 右上
            uniform vec4 u_grayUv;

            // 材质贴图在其 Texture 中的 UV 范围。
            uniform vec4 u_materialUv;


            void main(){

                /*
                 * ==============================
                 * 读取灰度模板
                 * ==============================
                 */

                vec4 graySample =
                    texture2D(
                        u_texture,
                        v_texCoords
                    );


                /*
                 * TextureRegion 很可能处于 TextureAtlas
                 * 中，因此 v_texCoords 并不是 0~1。
                 *
                 * 这里首先转换成当前灰度 Region
                 * 内部的局部坐标。
                 */
                vec2 localUv =
                    (v_texCoords - u_grayUv.xy)
                    /
                    (u_grayUv.zw - u_grayUv.xy);


                /*
                 * 再把 0~1 的局部 UV
                 * 映射到材质 TextureRegion。
                 */
                vec2 materialUv =
                    u_materialUv.xy
                    +
                    localUv
                    *
                    (u_materialUv.zw -
                     u_materialUv.xy);


                /*
                 * ==============================
                 * 读取材质贴图
                 * ==============================
                 */

                vec4 textureMaterial =
                    texture2D(
                        u_materialTexture,
                        materialUv
                    );


                /*
                 * 材质贴图仍然可以被 tint 染色。
                 *
                 * 白色：
                 * 保持原贴图颜色。
                 *
                 * 其他颜色：
                 * 给贴图附加颜色。
                 */
                textureMaterial *= u_tint;


                /*
                 * ==============================
                 * 选择材质来源
                 * ==============================
                 *
                 * useMaterialTexture = 0：
                 *
                 *     material = tint
                 *
                 * useMaterialTexture = 1：
                 *
                 *     material = textureMaterial
                 */

                vec4 material =
                    mix(
                        u_tint,
                        textureMaterial,
                        u_useMaterialTexture
                    );


                /*
                 * ==============================
                 * 计算灰度
                 * ==============================
                 *
                 * 不直接只读 .r，
                 * 因此即使输入图不完全是标准灰度图，
                 * 也能够正常取得亮度。
                 */

                float gray =
                    dot(
                        graySample.rgb,
                        vec3(
                            0.299,
                            0.587,
                            0.114
                        )
                    );


                /*
                 * ==============================
                 * 最终材质
                 * ==============================
                 *
                 * 灰度图：
                 * 控制明暗。
                 *
                 * material：
                 * 控制颜色和纹理。
                 */

                vec4 result;

                result.rgb =
                    material.rgb * gray;

                result.a =
                    material.a *
                    graySample.a;


                /*
                 * 保留 Arc Draw.color()
                 * 与 Draw.mixcol() 的行为。
                 */

                result =
                    mix(
                        result,
                        vec4(
                            v_mix_color.rgb,
                            result.a
                        ),
                        v_mix_color.a
                    );

                gl_FragColor =
                    result * v_color;
            }
            """;


    public GrayMaterialShader() {
        super(vertexShader, fragmentShader);
    }


    /**
     * 设置：
     *
     * 灰度图 + 纯颜色。
     */
    public void set(
            TextureRegion grayRegion,
            Color color
    ) {

        this.grayRegion = grayRegion;

        this.materialRegion = null;

        this.tint.set(color);

        this.useMaterialTexture = false;
    }


    /**
     * 设置：
     *
     * 灰度图 + 材质贴图。
     */
    public void set(
            TextureRegion grayRegion,
            TextureRegion materialRegion
    ) {

        set(
                grayRegion,
                materialRegion,
                Color.white
        );
    }


    /**
     * 设置：
     *
     * 灰度图 + 材质贴图 + 染色。
     */
    public void set(
            TextureRegion grayRegion,
            TextureRegion materialRegion,
            Color color
    ) {

        this.grayRegion = grayRegion;

        this.materialRegion =
                materialRegion;

        this.tint.set(color);

        this.useMaterialTexture = true;
    }


    /**
     * Shader 每次真正开始绘制前，
     * SpriteBatch 会调用 apply()。
     */
    @Override
    public void apply() {

        /*
         * 灰度图由 SpriteBatch 自动绑定到
         * Texture Unit 0。
         */
        setUniformi(
                "u_texture",
                0
        );


        /*
         * 第二张材质贴图使用
         * Texture Unit 1。
         */
        setUniformi(
                "u_materialTexture",
                1
        );


        /*
         * 即便现在使用的是纯颜色，
         * 也给第二纹理槽绑定一个合法 Texture。
         */
        TextureRegion actualMaterial =
                useMaterialTexture
                        ? materialRegion
                        : grayRegion;


        actualMaterial.texture.bind(1);


        /*
         * 非常重要：
         *
         * material.texture.bind(1)
         * 会把当前活动 Texture Unit 改成 1。
         *
         * SpriteBatch 随后还需要把它自己的
         * u_texture 绑定到 Texture Unit 0，
         * 所以这里必须恢复。
         */
        Gl.activeTexture(
                Gl.texture0
        );


        /*
         * ==============================
         * 普通参数
         * ==============================
         */

        setUniformf(
                "u_tint",
                tint
        );

        setUniformf(
                "u_useMaterialTexture",
                useMaterialTexture
                        ? 1f
                        : 0f
        );


        /*
         * Arc 绘制 TextureRegion 时实际使用：
         *
         * u  = region.u
         * v  = region.v2
         * u2 = region.u2
         * v2 = region.v
         *
         * 所以这里必须使用相同方向。
         */
        setUniformf(
                "u_grayUv",

                grayRegion.u,
                grayRegion.v2,

                grayRegion.u2,
                grayRegion.v
        );


        setUniformf(
                "u_materialUv",

                actualMaterial.u,
                actualMaterial.v2,

                actualMaterial.u2,
                actualMaterial.v
        );
    }
}
