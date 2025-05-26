package core;

import java.awt.Image;
import java.io.File;
import java.io.IOException;
import java.awt.image.PixelGrabber;

import javax.imageio.ImageIO;

public class Texture {
    //用于存储纹理像素RGB颜色的数组
    public int[] texture;

    //纹理的尺寸
    public int width, height, widthBits;

    //为简化计算，图片宽应为2^n
    public Texture(String file) {
        String imageFolder = "/image/";
        Image img = null;
        try {
            img = ImageIO.read(getClass().getResourceAsStream(imageFolder + file));
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        width = img.getWidth(null);
        height = img.getHeight(null);

        widthBits = 0;
        for (int i = width - 1; i != 0; i >>= 1)
            widthBits++;

        texture = new int[width * height];

        //把图片转换为像素
        PixelGrabber pg = new PixelGrabber(img, 0, 0, width, height, texture, 0, width);
        try {
            pg.grabPixels();
        } catch (Exception e) {
            System.out.println(e);
        }
    }
}
