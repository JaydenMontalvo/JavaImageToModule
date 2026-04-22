// Main.java — Batch version: processes every PNG in a resources/ folder
//
// ─── QUICK START ──────────────────────────────────────────────────────────────
//   1. Put all your 64x64 indexed PNGs into a folder called: resources/
//      (in the same directory you run the command from)
//
//   2. Compile (once):
//        javac -d out src/main/java/com/jay/Main.java
//
//   3. Run:
//        java -cp out com.jay.Main
//
//      Optional — specify a custom folder:
//        java -cp out com.jay.Main path/to/my/images
//
//   4. All .lua files are created in an output/ folder.
//      Paste each into: ReplicatedStorage > ColorByNumber > Images > <ImageName>
//      Then add each name to the IMAGE_NAMES list in ImageRegistry.
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
        String inputFolder = (args.length >= 1) ? args[0] : "images";
        String outputFolder = "output";

        File inputDir = new File(inputFolder);
        if (!inputDir.exists() || !inputDir.isDirectory()) {
            System.err.println("Input folder not found: " + inputFolder);
            System.err.println("Create a folder named '" + inputFolder + "' and put your PNG images inside.");
            System.exit(1);
        }

        File outputDir = new File(outputFolder);
        if (!outputDir.exists()) outputDir.mkdirs();

        File[] pngFiles = inputDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".png"));
        if (pngFiles == null || pngFiles.length == 0) {
            System.err.println("No PNG files found in: " + inputFolder);
            System.exit(1);
        }

        System.out.println("Found " + pngFiles.length + " PNG(s) in '" + inputFolder + "' -> outputting to '" + outputFolder + "'\n");

        List<String> processedNames = new ArrayList<>();

        for (File file : pngFiles) {
            // Use the filename without extension as the image name
            String imageName = file.getName().replaceFirst("(?i)\\.png$", "");
            System.out.println("Processing: " + file.getName() + " -> " + imageName + ".lua");
            processImage(file, imageName, outputDir);
            processedNames.add(imageName);
        }

        System.out.println("\n✓ Done! Processed " + processedNames.size() + " image(s).");
        System.out.println("\nAdd these to IMAGE_NAMES in ImageRegistry:");
        for (String name : processedNames) {
            System.out.println("  \"" + name + "\",");
        }
    }

    static void processImage(File file, String imageName, File outputDir) throws Exception {
        BufferedImage img = ImageIO.read(file);
        if (img == null) {
            System.err.println("  SKIP: Could not read image: " + file.getName());
            return;
        }

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
            System.out.println("  WARNING: image has more than " + MAX_COLORS + " unique colors — extra colors snapped to nearest.");
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
        sb.append("\tprice     = 1048,\n");
        sb.append("\tthumbnail = \"rbxassetid://0\",  -- replace with real asset ID\n");
        sb.append("\twidth     = 64,\n");
        sb.append("\theight    = 64,\n");
        sb.append("\tpalette   = PALETTE,\n");
        sb.append("\tcolorMap  = colorMap,\n");
        sb.append("}\n");

        File outFile = new File(outputDir, imageName + ".lua");
        try (PrintWriter writer = new PrintWriter(new FileWriter(outFile))) {
            writer.print(sb);
        }

        System.out.println("  -> " + outFile.getPath());
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