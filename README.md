# Pixel Art to Roblox Module Converter

## Overview

This program converts a 24-color pixel art image into a Lua module that can be used in a Roblox game.

## Features

* Supports up to 24 colors
* Outputs a Lua ModuleScript
* Preserves pixel data and color mapping
* Simple conversion process

## Input

* Pixel art image (.png or .jpg)
* Maximum of 24 unique colors

## Output

A Lua module containing image data.

Example:

```lua
return {
    Width = 16,
    Height = 16,
    Pixels = {
        {1,1,2,2,3},
        {1,2,2,3,3}
    },
    Palette = {
        [1] = Color3.fromRGB(255,255,255),
        [2] = Color3.fromRGB(0,0,0),
        [3] = Color3.fromRGB(255,0,0)
    }
}
```

## Usage

1. Run the converter on your image
2. Copy the generated Lua module
3. Paste it into a ModuleScript in Roblox
4. Require the module in your game

## Notes

* Keep images small for performance
* Best used for pixel art or low-resolution images
