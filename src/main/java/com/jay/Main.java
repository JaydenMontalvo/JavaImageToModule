// Main.java — Roblox Color-by-Number Lua module generator
//
// ─── QUICK START ──────────────────────────────────────────────────────────────
//   1. Prepare your image in Photopea:
//        a. Image > Mode > Indexed Color  (Colors = 24, Dither = None)
//        b. Image > Image Size            (64 x 64, Resample = Nearest Neighbor)
//        c. File > Export As > PNG
//
//   2. Compile (once):
//        javac -d out src/main/java/com/jay/Main.java
//
//   3. Run:
//        java -cp out com.jay.Main <path/to/image.png> <ImageName>
//
//      Example:
//        java -cp out com.jay.Main C:/Users/Jay/Pictures/sunset.png Sunset
//
//   4. A file named <ImageName>.lua is created in your working directory.
//      Paste it into:  ReplicatedStorage > ColorByNumber > Images > <ImageName>
//      Then add '<ImageName>' to the IMAGE_NAMES list in ImageRegistry.
//
// ─── NOTES ────────────────────────────────────────────────────────────────────
//   • Images are automatically resized to 64×64 with Nearest Neighbor if needed.
//   • Only the first 24 unique colors are kept; extras snap to the nearest match.
//   • Run with no arguments to see this usage summary.
// ──────────────────────────────────────────────────────────────────────────────

package com.jay;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;

public class Main {

    static final int MAX_COLORS = 24;
    static final int GRID = 64;

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("Usage:  java -cp out com.jay.Main <path/to/image.png> <ImageName>");
            System.out.println("Example: java -cp out com.jay.Main C:/Users/Jay/Pictures/sunset.png Sunset");
            System.exit(0);
        }

        String imagePath = args[0];
        String imageName = args[1];

        File file = new File(imagePath);
        if (!file.exists()) {
            System.err.println("File not found: " + imagePath);
            System.exit(1);
        }

        System.out.println("Processing " + imagePath + " as '" + imageName + "'...");

        BufferedImage img = ImageIO.read(file);

        if (img.getWidth() != GRID || img.getHeight() != GRID) {
            System.out.printf("  Resizing (%dx%d) -> (%dx%d) with Nearest Neighbor%n",
                    img.getWidth(), img.getHeight(), GRID, GRID);
            BufferedImage resized = new BufferedImage(GRID, GRID, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = resized.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            g.drawImage(img, 0, 0, GRID, GRID, null);
            g.dispose();
            img = resized;
        }

        int[][] pixels = new int[GRID][GRID];
        for (int y = 0; y < GRID; y++)
            for (int x = 0; x < GRID; x++)
                pixels[y][x] = img.getRGB(x, y) & 0xFFFFFF;

        LinkedHashMap<Integer, Integer> colorToIndex = new LinkedHashMap<>();
        List<int[]> palette = new ArrayList<>();
        boolean remapped = false;

        for (int y = 0; y < GRID; y++) {
            for (int x = 0; x < GRID; x++) {
                int rgb = pixels[y][x];
                if (!colorToIndex.containsKey(rgb)) {
                    if (palette.size() < MAX_COLORS) {
                        colorToIndex.put(rgb, palette.size());
                        palette.add(new int[]{(rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF});
                    } else {
                        remapped = true;
                    }
                }
            }
        }

        if (remapped) {
            System.out.println("  WARNING: image has more than " + MAX_COLORS + " unique colors.");
            System.out.println("  Extra colors snapped to nearest palette entry.");
            System.out.println("  Re-export from Photopea with Colors=" + MAX_COLORS + ", Dither=None for clean results.");
        }

        int[][] colorMap = new int[GRID][GRID];
        for (int y = 0; y < GRID; y++) {
            for (int x = 0; x < GRID; x++) {
                int rgb = pixels[y][x];
                colorMap[y][x] = colorToIndex.containsKey(rgb)
                        ? colorToIndex.get(rgb) + 1
                        : nearestColor(rgb, palette) + 1;
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("-- ReplicatedStorage/ColorByNumber/Images/").append(imageName).append(".lua\n");
        sb.append("-- ").append(palette.size()).append(" colors, 64x64 grid\n\n");

        sb.append("local PALETTE = {\n");
        for (int i = 0; i < palette.size(); i++) {
            int[] c = palette.get(i);
            sb.append(String.format("\tColor3.fromRGB(%3d, %3d, %3d),  -- %d%n", c[0], c[1], c[2], i + 1));
        }
        sb.append("}\n\n");

        sb.append("local colorMap = {\n");
        for (int y = 0; y < GRID; y++) {
            sb.append("\t{");
            for (int x = 0; x < GRID; x++) {
                sb.append(colorMap[y][x]);
                if (x < GRID - 1) sb.append(", ");
            }
            sb.append("},\n");
        }
        sb.append("}\n\n");

        sb.append("return {\n");
        sb.append("\tname      = \"").append(imageName).append("\",\n");
        sb.append("\tthumbnail = \"rbxassetid://0\",  -- replace with real asset ID\n");
        sb.append("\twidth     = 64,\n");
        sb.append("\theight    = 64,\n");
        sb.append("\tpalette   = PALETTE,\n");
        sb.append("\tcolorMap  = colorMap,\n");
        sb.append("}\n");

        String outPath = imageName + ".lua";
        try (PrintWriter writer = new PrintWriter(new FileWriter(outPath))) {
            writer.print(sb);
        }

        System.out.println("  Done -> " + outPath);
        System.out.println("  Paste into: ReplicatedStorage.ColorByNumber.Images." + imageName);
        System.out.println("  Add '" + imageName + "' to ImageRegistry IMAGE_NAMES list");
    }

    static int nearestColor(int rgb, List<int[]> palette) {
        int r = (rgb >> 16) & 0xFF, g = (rgb >> 8) & 0xFF, b = rgb & 0xFF;
        int best = 0;
        long bestDist = Long.MAX_VALUE;
        for (int i = 0; i < palette.size(); i++) {
            int[] c = palette.get(i);
            long d = (long)(r-c[0])*(r-c[0]) + (long)(g-c[1])*(g-c[1]) + (long)(b-c[2])*(b-c[2]);
            if (d < bestDist) { bestDist = d; best = i; }
        }
        return best;
    }
}
