/*
 * Copyright 2023 Viascom Ltd liab. Co
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

@file:JvmName("AlunaImageUtils")
@file:JvmMultifileClass

package io.viascom.discord.bot.aluna.util

import java.awt.AlphaComposite
import java.awt.Graphics
import java.awt.RenderingHints
import java.awt.Transparency
import java.awt.image.BufferedImage
import java.awt.image.Raster
import java.io.FileNotFoundException
import java.io.IOException
import java.io.OutputStream
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageWriteParam

/**
 * Rescale by a given amount.
 *
 * @param scale Scale to use. Where 1 is no scaling.
 * @return Rescaled BufferedImage
 */
@Throws(IOException::class)
fun BufferedImage.rescale(scale: Double): BufferedImage {
    val originalWidth = this.width
    val originalHeight = this.height
    val type = if (this.type == 0) BufferedImage.TYPE_INT_ARGB else this.type

    //rescale
    val resizedImage = BufferedImage((originalWidth * scale).toInt(), (originalHeight * scale).toInt(), type)
    val g = resizedImage.createGraphics()
    g.drawImage(this, 0, 0, (originalWidth * scale).toInt(), (originalHeight * scale).toInt(), null)
    g.dispose()
    g.composite = AlphaComposite.Src
    g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
    g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    return resizedImage
}

/**
 * Compress to a jpeg with a given compression quality
 *
 * @param compression Compression rate between 0 and 1
 * @param out OutputStream where to write the compressed image
 */
@Throws(FileNotFoundException::class, IOException::class)
fun BufferedImage.compress(compression: Float, out: OutputStream) {
    val jpegWriter = ImageIO.getImageWritersByFormatName("jpeg").next()

    // Set the compression quality
    val param = jpegWriter.defaultWriteParam
    param.compressionMode = ImageWriteParam.MODE_EXPLICIT
    param.compressionQuality = compression

    // Write the image to a Stream
    jpegWriter.output = out
    jpegWriter.write(null, IIOImage(this, null, null), param)
    jpegWriter.dispose()
    out.close()
}

/**
 * Apply a mask to an image. The mask should be black and white image where the brightness level defines the opacity of the pixel in the image.
 *
 * @param mask Mask to use
 * @return BufferedImage with the mask applied
 */
fun BufferedImage.applyMask(mask: BufferedImage): BufferedImage {
    val result = BufferedImage(this.getWidth(null), this.getHeight(null), Transparency.BITMASK)
    val temp = BufferedImage(this.getWidth(null), this.getHeight(null), Transparency.BITMASK)
    val raster = result.raster
    val maskData: Raster = mask.raster
    val tileData: Raster = this.raster
    val g: Graphics
    var pixel = IntArray(4)
    val width = this.getWidth(null)
    val height = this.getHeight(null)
    for (y in 0 until height) {
        for (x in 0 until width) {
            pixel = maskData.getPixel(x, y, pixel)
            if (pixel[0] == 255) {
                tileData.getPixel(x, y, pixel)
                pixel[3] = 255
                raster.setPixel(x, y, pixel)
                pixel = tileData.getPixel(x, y, pixel)
            }
        }
    }
    result.data = raster
    g = temp.createGraphics()
    g.drawImage(result, 0, 0, null)
    g.dispose()
    return temp
}
