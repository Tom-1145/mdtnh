package mdtnh.graphics;

import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.TextureRegion;

/**
 * MDTNH 材质绘制工具。
 *
 * 对 GrayMaterialShader 做统一封装，
 * 避免其他 Block / Unit / Effect
 * 直接操作 Shader 状态。
 */
public class MdtMaterialDraw {

    private static GrayMaterialShader shader;


    private MdtMaterialDraw() {
    }


    /**
     * 必须在客户端 GL 环境已经创建之后调用。
     */
    public static void load() {

        if (shader != null) {
            return;
        }

        shader =
                new GrayMaterialShader();
    }


    /**
     * 灰度图 + 颜色。
     */
    public static void draw(
            TextureRegion gray,
            Color color,
            float x,
            float y,
            float width,
            float height,
            float rotation
    ) {

        if (shader == null) {
            return;
        }

        /*
         * 先提交之前仍在 Batch 中等待绘制的内容，
         * 避免它们错误地使用我们的 Shader 参数。
         */
        Draw.flush();

        shader.set(
                gray,
                color
        );

        Draw.shader(shader);

        Draw.rect(
                gray,
                x,
                y,
                width,
                height,
                rotation
        );

        /*
         * 当前材质必须在参数改变前真正提交。
         */
        Draw.flush();

        /*
         * 恢复 Mindustry 默认 Shader。
         */
        Draw.shader();
    }


    /**
     * 灰度图 + 材质贴图。
     */
    public static void draw(
            TextureRegion gray,
            TextureRegion material,
            float x,
            float y,
            float width,
            float height,
            float rotation
    ) {

        draw(
                gray,
                material,
                Color.white,
                x,
                y,
                width,
                height,
                rotation
        );
    }


    /**
     * 灰度图 + 材质贴图 + 额外染色。
     */
    public static void draw(
            TextureRegion gray,
            TextureRegion material,
            Color tint,
            float x,
            float y,
            float width,
            float height,
            float rotation
    ) {

        if (shader == null) {
            return;
        }

        Draw.flush();

        shader.set(
                gray,
                material,
                tint
        );

        Draw.shader(shader);

        Draw.rect(
                gray,
                x,
                y,
                width,
                height,
                rotation
        );

        Draw.flush();

        Draw.shader();
    }


    /**
     * 游戏退出或资源释放时可以调用。
     */
    public static void dispose() {

        if (shader != null) {

            shader.dispose();

            shader = null;
        }
    }
}
